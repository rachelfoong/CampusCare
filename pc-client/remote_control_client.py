"""
CampusCare Remote Control - PC Client
======================================
Connects to the CampusCare relay server and requests a specific phone
by device ID. Displays the phone screen and allows touch/keyboard control.

Requirements:
    pip install Pillow

Usage:
    python remote_control_client.py [relay_ip] [device_id]

Controls:
    Mouse click     -> Tap
    Mouse drag      -> Swipe
    Escape          -> Back
    H               -> Home
    R               -> Recents
    Q / Ctrl+C      -> Quit
"""

import io
import socket
import struct
import sys
import threading
import time
import tkinter as tk
from tkinter import messagebox, simpledialog
from collections import deque

try:
    from PIL import Image, ImageTk
except ImportError:
    print("ERROR: Pillow is required. Install it with: pip install Pillow")
    sys.exit(1)


RELAY_HOST = "34.169.113.109"
RELAY_PORT = 9000


class RemoteControlClient:
    """Main client application."""

    def __init__(self, host: str, port: int, device_id: str):
        self.host = host
        self.port = port
        self.device_id = device_id
        self.sock: socket.socket | None = None
        self.connected = False
        self.running = True

        # Frame state
        self.latest_frame: Image.Image | None = None
        self.frame_lock = threading.Lock()
        self.frame_count = 0
        self.fps = 0.0
        self.fps_timer = time.time()
        self.native_width = 0
        self.native_height = 0

        # Mouse drag tracking
        self.drag_start_x = 0
        self.drag_start_y = 0
        self.is_dragging = False
        self.drag_threshold = 10  # pixels before a click becomes a swipe

        # Display state
        self.display_width = 0
        self.display_height = 0
        self.image_offset_x = 0
        self.image_offset_y = 0
        self.scale_factor = 1.0

        # Tkinter
        self.root: tk.Tk | None = None
        self.canvas: tk.Canvas | None = None
        self.status_var: tk.StringVar | None = None
        self.photo_image: ImageTk.PhotoImage | None = None
        self.canvas_image_id = None

    def connect(self) -> bool:
        """Connect to the relay server and request the target device."""
        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.settimeout(5.0)
            self.sock.connect((self.host, self.port))
            self.sock.settimeout(None)

            # Send PC handshake with device ID
            self.sock.sendall(f"PC:{self.device_id}\n".encode("utf-8"))

            # Read response line
            response = b""
            while b"\n" not in response:
                chunk = self.sock.recv(1)
                if not chunk:
                    raise OSError("Relay closed connection during handshake")
                response += chunk
            response = response.decode("utf-8").strip()

            if response != "OK":
                print(f"Relay error: {response}")
                self.sock.close()
                self.sock = None
                return False

            self.connected = True
            print(f"Connected to relay — bridged to device {self.device_id}")
            return True
        except (socket.error, OSError) as e:
            print(f"Connection failed: {e}")
            self.sock = None
            self.connected = False
            return False

    def recv_exact(self, n: int) -> bytes | None:
        """Read exactly n bytes from the socket."""
        data = bytearray()
        while len(data) < n:
            try:
                chunk = self.sock.recv(n - len(data))
                if not chunk:
                    return None
                data.extend(chunk)
            except (socket.error, OSError):
                return None
        return bytes(data)

    def receiver_loop(self):
        """Background thread: continuously receives frames from the server."""
        while self.running and self.connected:
            try:
                # Read 4-byte frame size (big-endian)
                size_data = self.recv_exact(4)
                if size_data is None:
                    break
                frame_size = struct.unpack(">I", size_data)[0]

                if frame_size <= 0 or frame_size > 10_000_000:  # sanity check: max 10MB
                    print(f"Invalid frame size: {frame_size}")
                    break

                # Read frame JPEG data
                jpeg_data = self.recv_exact(frame_size)
                if jpeg_data is None:
                    break

                # Decode JPEG
                image = Image.open(io.BytesIO(jpeg_data))

                with self.frame_lock:
                    self.latest_frame = image
                    if self.native_width == 0:
                        self.native_width = image.width
                        self.native_height = image.height

                # FPS tracking
                self.frame_count += 1
                now = time.time()
                elapsed = now - self.fps_timer
                if elapsed >= 1.0:
                    self.fps = self.frame_count / elapsed
                    self.frame_count = 0
                    self.fps_timer = now

            except Exception as e:
                if self.running:
                    print(f"Receiver error: {e}")
                break

        self.connected = False
        print("Receiver thread stopped")

    def send_command(self, command: str):
        """Send a text command to the server (newline-terminated)."""
        if not self.connected or self.sock is None:
            return
        try:
            self.sock.sendall((command + "\n").encode("utf-8"))
        except (socket.error, OSError) as e:
            print(f"Send error: {e}")
            self.connected = False

    def screen_to_native(self, screen_x: int, screen_y: int) -> tuple[float, float] | None:
        """Convert window canvas coordinates to native capture coordinates."""
        if self.native_width == 0 or self.display_width == 0:
            return None

        # Subtract the offset (letterboxing)
        rel_x = screen_x - self.image_offset_x
        rel_y = screen_y - self.image_offset_y

        # Check bounds
        if rel_x < 0 or rel_y < 0 or rel_x > self.display_width or rel_y > self.display_height:
            return None

        # Scale back to native resolution
        native_x = rel_x / self.scale_factor
        native_y = rel_y / self.scale_factor

        return (native_x, native_y)

    # ── Mouse event handlers ──

    def on_mouse_down(self, event):
        self.drag_start_x = event.x
        self.drag_start_y = event.y
        self.is_dragging = False

    def on_mouse_move(self, event):
        dx = abs(event.x - self.drag_start_x)
        dy = abs(event.y - self.drag_start_y)
        if dx > self.drag_threshold or dy > self.drag_threshold:
            self.is_dragging = True

    def on_mouse_up(self, event):
        if self.is_dragging:
            # Swipe gesture
            start = self.screen_to_native(self.drag_start_x, self.drag_start_y)
            end = self.screen_to_native(event.x, event.y)
            if start and end:
                cmd = f"SWIPE:{start[0]:.0f},{start[1]:.0f},{end[0]:.0f},{end[1]:.0f}"
                self.send_command(cmd)
        else:
            # Single tap
            coords = self.screen_to_native(event.x, event.y)
            if coords:
                cmd = f"TOUCH:{coords[0]:.0f},{coords[1]:.0f}"
                self.send_command(cmd)

        self.is_dragging = False

    # ── Keyboard event handlers ──

    def on_key_press(self, event):
        key = event.keysym.lower()
        if key == "escape":
            self.send_command("BACK")
        elif key == "h":
            self.send_command("HOME")
        elif key == "r":
            self.send_command("RECENTS")
        elif key == "w":
            self.send_command("WAKE")
        elif key == "s":
            self.send_command("SLEEP")
        elif key == "q":
            self.shutdown()

    # ── Rendering ──

    def update_frame(self):
        """Called periodically by tkinter to render the latest frame."""
        if not self.running:
            return

        frame = None
        with self.frame_lock:
            if self.latest_frame is not None:
                frame = self.latest_frame
                self.latest_frame = None

        if frame is not None and self.canvas is not None:
            # Get current canvas size
            canvas_w = self.canvas.winfo_width()
            canvas_h = self.canvas.winfo_height()

            if canvas_w > 1 and canvas_h > 1:
                # Calculate scale to fit while keeping aspect ratio
                scale_x = canvas_w / frame.width
                scale_y = canvas_h / frame.height
                self.scale_factor = min(scale_x, scale_y)

                self.display_width = int(frame.width * self.scale_factor)
                self.display_height = int(frame.height * self.scale_factor)

                # Center the image
                self.image_offset_x = (canvas_w - self.display_width) // 2
                self.image_offset_y = (canvas_h - self.display_height) // 2

                # Resize and display
                resized = frame.resize(
                    (self.display_width, self.display_height),
                    Image.Resampling.LANCZOS
                )
                self.photo_image = ImageTk.PhotoImage(resized)

                if self.canvas_image_id is None:
                    self.canvas_image_id = self.canvas.create_image(
                        self.image_offset_x, self.image_offset_y,
                        anchor=tk.NW,
                        image=self.photo_image
                    )
                else:
                    self.canvas.coords(self.canvas_image_id, self.image_offset_x, self.image_offset_y)
                    self.canvas.itemconfig(self.canvas_image_id, image=self.photo_image)

        # Update status bar
        if self.status_var:
            if self.connected:
                self.status_var.set(
                    f"  Connected to {self.host}:{self.port}  |  "
                    f"{self.native_width}x{self.native_height}  |  "
                    f"{self.fps:.1f} FPS  |  "
                    f"Esc=Back  H=Home  R=Recents  W=Wake  S=Sleep  Q=Quit"
                )
            else:
                self.status_var.set("  Disconnected")

        # Check for disconnection
        if not self.connected and self.running:
            self.running = False
            if self.root:
                messagebox.showinfo("Disconnected", "Connection to device lost.")
                self.root.destroy()
            return

        # Schedule next update (~30 FPS render rate)
        if self.root and self.running:
            self.root.after(33, self.update_frame)

    def shutdown(self):
        """Clean shutdown."""
        self.running = False
        self.connected = False
        if self.sock:
            try:
                self.sock.close()
            except Exception:
                pass
            self.sock = None
        if self.root:
            try:
                self.root.destroy()
            except Exception:
                pass

    def run(self):
        """Start the GUI and receiver thread."""
        # Connect
        if not self.connect():
            print(f"Could not connect to {self.host}:{self.port}")
            return

        # Start receiver thread
        receiver = threading.Thread(target=self.receiver_loop, daemon=True)
        receiver.start()

        # Wait briefly for the first frame to get native resolution
        time.sleep(0.5)

        # Build tkinter UI
        self.root = tk.Tk()
        self.root.title(f"Remote Control - {self.device_id}")
        self.root.configure(bg="black")

        # Set initial window size (phone aspect ratio, reasonable desktop size)
        win_h = 800
        if self.native_width > 0 and self.native_height > 0:
            aspect = self.native_width / self.native_height
            win_w = int(win_h * aspect)
        else:
            win_w = 400

        self.root.geometry(f"{win_w}x{win_h + 30}")
        self.root.minsize(300, 400)

        # Status bar
        self.status_var = tk.StringVar(value="  Connecting...")
        status_bar = tk.Label(
            self.root,
            textvariable=self.status_var,
            anchor=tk.W,
            bg="#1a1a2e",
            fg="#e0e0e0",
            font=("Consolas", 9),
            padx=5,
            pady=3
        )
        status_bar.pack(side=tk.BOTTOM, fill=tk.X)

        # Screen control buttons
        btn_frame = tk.Frame(self.root, bg="#1a1a2e", pady=4)
        btn_frame.pack(side=tk.BOTTOM, fill=tk.X)
        tk.Button(
            btn_frame, text="Wake Screen (W)",
            command=lambda: self.send_command("WAKE"),
            bg="#2d5a27", fg="white", font=("Consolas", 9),
            relief=tk.FLAT, padx=10, cursor="hand2"
        ).pack(side=tk.LEFT, padx=(8, 4))
        tk.Button(
            btn_frame, text="Sleep Screen (S)",
            command=lambda: self.send_command("SLEEP"),
            bg="#5a2727", fg="white", font=("Consolas", 9),
            relief=tk.FLAT, padx=10, cursor="hand2"
        ).pack(side=tk.LEFT, padx=4)

        # Canvas for frame display
        self.canvas = tk.Canvas(self.root, bg="black", highlightthickness=0)
        self.canvas.pack(fill=tk.BOTH, expand=True)

        # Bind events
        self.canvas.bind("<ButtonPress-1>", self.on_mouse_down)
        self.canvas.bind("<B1-Motion>", self.on_mouse_move)
        self.canvas.bind("<ButtonRelease-1>", self.on_mouse_up)
        self.root.bind("<KeyPress>", self.on_key_press)
        self.root.protocol("WM_DELETE_WINDOW", self.shutdown)

        # Start render loop
        self.root.after(100, self.update_frame)

        # Run tkinter mainloop
        self.root.mainloop()

        # Cleanup
        self.running = False
        self.connected = False
        if self.sock:
            try:
                self.sock.close()
            except Exception:
                pass
        receiver.join(timeout=2.0)
        print("Client shut down.")


def main():
    # Parse command-line args or prompt
    relay_host = RELAY_HOST
    relay_port = RELAY_PORT
    device_id = None

    if len(sys.argv) >= 2:
        relay_host = sys.argv[1]
    if len(sys.argv) >= 3:
        device_id = sys.argv[2]

    if device_id is None:
        # Show a simple dialog to get the device ID
        temp_root = tk.Tk()
        temp_root.withdraw()
        device_id = simpledialog.askstring(
            "Connect to Device",
            "Enter the Device ID\n(shown on the Remote Control screen of the phone):",
            parent=temp_root
        )
        temp_root.destroy()

        if not device_id:
            print("No device ID entered. Exiting.")
            sys.exit(0)

    device_id = device_id.strip()
    print(f"Connecting to relay {relay_host}:{relay_port} → device {device_id}...")
    client = RemoteControlClient(relay_host, relay_port, device_id)
    client.run()


if __name__ == "__main__":
    main()
