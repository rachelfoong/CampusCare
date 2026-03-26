package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.campuscare.viewmodel.AdminViewModel
import com.university.campuscare.viewmodel.DeviceInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminRecordingsTab(viewModel: AdminViewModel) {
    val recordings by viewModel.recordings.collectAsState()
    val isLoading by viewModel.isLoadingRecordings.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Screen Recordings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                viewModel.loadRecordings()
                viewModel.loadDevices()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFF0000))
            }
        }

        // ── Connected Devices ──
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Devices",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF0000)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (devices.isEmpty()) {
            Text(
                text = "No devices seen yet",
                fontSize = 13.sp,
                color = Color.Gray
            )
        } else {
            devices.forEach { device ->
                DeviceCard(device = device, onCopyIp = {
                    clipboardManager.setText(
                        androidx.compose.ui.text.AnnotatedString(device.deviceId)
                    )
                })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Recordings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF0000)
        )
        Spacer(modifier = Modifier.height(8.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF0000))
                }
            }
            recordings.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No recordings yet",
                            color = Color.Gray
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp), // Compensates for the new card padding
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(recordings) { recording ->
                        RecordingCard(
                            recording = recording,
                            onView = {
                                if (recording.downloadUrl.isNotBlank()) {
                                    uriHandler.openUri(recording.downloadUrl)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    recording: com.university.campuscare.data.model.Recording,
    onView: () -> Unit
) {
    val dateStr = try {
        val filename = recording.downloadUrl
            .substringAfterLast("%2F")
            .substringBefore("?")
            .removeSuffix(".mp4")
            .removePrefix("recording_")
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .parse(filename)
            ?.let { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(it) }
            ?: SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(recording.timestamp))
    } catch (e: Exception) {
        SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(recording.timestamp))
    }
    val sizeMb = recording.fileSizeBytes / (1024.0 * 1024.0)

    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                TextButton(
                    onClick = onView,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0000))
                ) {
                    Text("View")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabelValue("Device", recording.deviceId.takeLast(8))
                LabelValue("Size", String.format("%.1f MB", sizeMb))
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
private fun DeviceCard(
    device: DeviceInfo,
    onCopyIp: () -> Unit
) {
    val lastSeenStr = if (device.lastSeen > 0L) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(device.lastSeen))
    } else {
        "Unknown"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = Color(0xFFFF0000),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceId,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Last active: $lastSeenStr",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onCopyIp) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy IP",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}