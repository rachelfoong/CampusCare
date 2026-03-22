"""
CampusCare Relay Server
=======================
Phones and PC clients both connect here.
Phones register with their device ID and wait.
PC clients request a device ID and get bridged to that phone.

Usage:
    python3 relay_server.py [port]   (default: 9000)

Protocol:
    Phone:  PHONE:<device_id>\n  →  OK\n
    PC:     PC:<device_id>\n     →  OK\n  (then binary stream begins)
    Debug:  LIST\n               →  DEVICES:<id1>,<id2>,...\n
"""

import socket
import threading
import sys
import time

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 9000
BUFFER_SIZE = 65536

# Registry: device_id -> { 'sock': socket, 'session_active': Event, 'done': Event }
phones: dict = {}
phones_lock = threading.Lock()


def log(msg: str):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def recv_line(sock: socket.socket, timeout: float = 10.0) -> str | None:
    """Read a newline-terminated line byte by byte — no buffering."""
    sock.settimeout(timeout)
    buf = b""
    try:
        while b"\n" not in buf:
            chunk = sock.recv(1)
            if not chunk:
                return None
            buf += chunk
        return buf.decode("utf-8").strip()
    except Exception:
        return None
    finally:
        sock.settimeout(None)


def send_line(sock: socket.socket, msg: str):
    try:
        sock.sendall((msg + "\n").encode("utf-8"))
    except Exception:
        pass


def forward(src: socket.socket, dst: socket.socket, label: str):
    """Forward all bytes from src to dst until either side closes."""
    try:
        while True:
            data = src.recv(BUFFER_SIZE)
            if not data:
                break
            dst.sendall(data)
    except Exception:
        pass
    log(f"{label} stopped")


def handle_phone(sock: socket.socket, device_id: str, addr):
    """
    Register the phone and block until a session ends.
    IMPORTANT: We never read from the socket here.
    The forwarding thread gets exclusive read access, eliminating race conditions.
    """
    session_active = threading.Event()
    done = threading.Event()

    with phones_lock:
        # If same device reconnects, clean up old entry
        old = phones.pop(device_id, None)
        if old:
            try:
                old['sock'].close()
            except Exception:
                pass
            old['done'].set()  # Unblock the old handle_phone thread

        phones[device_id] = {
            'sock': sock,
            'session_active': session_active,
            'done': done,
        }

    # Enable OS-level TCP keepalive to detect dead connections
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
        # Tune keepalive: start after 60s idle, probe every 10s, give up after 6 failures
        sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPIDLE, 60)
        sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPINTVL, 10)
        sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_KEEPCNT, 6)
    except Exception:
        pass

    send_line(sock, "OK")
    log(f"Phone registered: {device_id} from {addr[0]}")

    # Block until a session ends or the phone reconnects (which sets done)
    # We do NOT read from the socket — forwarding thread owns it exclusively
    done.wait()

    with phones_lock:
        if phones.get(device_id, {}).get('sock') is sock:
            del phones[device_id]
    try:
        sock.close()
    except Exception:
        pass
    log(f"Phone {device_id} cleaned up")


def handle_pc(sock: socket.socket, device_id: str, addr):
    """Find the requested phone and bridge the PC to it."""
    log(f"PC requested device: {device_id} from {addr[0]}")

    with phones_lock:
        info = phones.get(device_id)

    if info is None:
        send_line(sock, f"ERROR:Device '{device_id}' is not connected")
        sock.close()
        log(f"PC rejected: {device_id} not found")
        return

    phone_sock = info['sock']
    session_active: threading.Event = info['session_active']
    done: threading.Event = info['done']

    # Acknowledge the PC
    send_line(sock, "OK")

    # Mark session as active so handle_phone knows not to touch the socket
    session_active.set()

    # Tell the phone a PC has connected — it will start streaming immediately
    send_line(phone_sock, "CLIENT_CONNECTED")

    log(f"Bridging PC {addr[0]} <-> Phone {device_id}")

    # Bidirectional forwarding — handle_phone is blocked on done.wait() and
    # never reads from phone_sock, so these threads have exclusive socket access
    t1 = threading.Thread(
        target=forward, args=(sock, phone_sock, f"PC→{device_id}"), daemon=True
    )
    t2 = threading.Thread(
        target=forward, args=(phone_sock, sock, f"{device_id}→PC"), daemon=True
    )
    t1.start()
    t2.start()
    t1.join()
    t2.join()

    log(f"Session ended: PC {addr[0]} <-> Phone {device_id}")

    # Signal handle_phone to clean up
    done.set()

    try:
        sock.close()
    except Exception:
        pass


def handle_connection(sock: socket.socket, addr):
    log(f"New connection from {addr[0]}:{addr[1]}")

    handshake = recv_line(sock)
    if not handshake:
        log(f"No handshake from {addr[0]}, closing")
        sock.close()
        return

    if handshake.startswith("PHONE:"):
        device_id = handshake[len("PHONE:"):].strip()
        if not device_id:
            send_line(sock, "ERROR:Missing device ID")
            sock.close()
            return
        handle_phone(sock, device_id, addr)

    elif handshake.startswith("PC:"):
        device_id = handshake[len("PC:"):].strip()
        if not device_id:
            send_line(sock, "ERROR:Missing device ID")
            sock.close()
            return
        handle_pc(sock, device_id, addr)

    elif handshake == "LIST":
        with phones_lock:
            ids = list(phones.keys())
        send_line(sock, "DEVICES:" + ",".join(ids) if ids else "DEVICES:")
        sock.close()

    else:
        log(f"Unknown handshake from {addr[0]}: {handshake!r}")
        send_line(sock, "ERROR:Unknown handshake")
        sock.close()


def heartbeat_loop():
    """
    Sends PING to every idle (no active session) phone every 30 seconds.
    Keeps NAT/firewall mappings alive in the relay→phone direction.
    If a send fails, the phone is dead — set done so handle_phone cleans up.
    The phone's own heartbeat keeps the phone→relay direction alive.
    """
    while True:
        time.sleep(9 * 60)  # 9 minutes — relay→phone direction (WiFi-safe)
        with phones_lock:
            entries = list(phones.items())
        for device_id, info in entries:
            if info['session_active'].is_set():
                continue  # Session active — forwarding threads own the socket
            try:
                send_line(info['sock'], "PING")
            except Exception:
                log(f"Heartbeat failed for {device_id} — marking as dead")
                info['done'].set()


def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server.bind(("0.0.0.0", PORT))
    server.listen(50)
    log(f"Relay server listening on port {PORT}")

    threading.Thread(target=heartbeat_loop, daemon=True).start()

    try:
        while True:
            try:
                sock, addr = server.accept()
                sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                t = threading.Thread(
                    target=handle_connection, args=(sock, addr), daemon=True
                )
                t.start()
            except Exception as e:
                log(f"Accept error: {e}")
    except KeyboardInterrupt:
        log("Shutting down")
    finally:
        server.close()


if __name__ == "__main__":
    main()
