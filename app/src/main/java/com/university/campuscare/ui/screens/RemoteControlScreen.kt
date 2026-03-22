package com.university.campuscare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import android.provider.Settings
import com.university.campuscare.remote.FeaturePreferences
import com.university.campuscare.remote.RemoteAccessService
import com.university.campuscare.remote.RemoteControlUtils
import com.university.campuscare.remote.ScreenRecorder
import com.university.campuscare.remote.TouchAccessibilityService
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    onNavigateBack: () -> Unit,
    onRequestServiceStart: () -> Unit = {}
) {
    val context = LocalContext.current

    // State that refreshes every second
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick++
        }
    }

    // Read live state (re-evaluated every tick)
    val serviceRunning = remember(tick) { RemoteAccessService.isRunning }
    val isRecording = remember(tick) { RemoteAccessService.isRecording }
    val isClientConnected = remember(tick) { RemoteAccessService.isClientConnected }
    val recordingStartTime = remember(tick) { RemoteAccessService.recordingStartTime }
    val nextRecordingTime = remember(tick) { RemoteAccessService.nextRecordingTime }
    val localIp = remember(tick) { RemoteControlUtils.getLocalIpAddress(context) }
    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) }
    val isAccessibilityEnabled = remember(tick) { TouchAccessibilityService.isEnabled(context) }
    val recordingFeatureEnabled = remember(tick) { FeaturePreferences.isRecordingEnabled(context) }
    val remoteAccessFeatureEnabled = remember(tick) { FeaturePreferences.isRemoteAccessEnabled(context) }

    // Get recordings list
    val recordings = remember(tick) {
        val dir = ScreenRecorder.getRecordingsDir(context)
        if (dir.exists()) {
            dir.listFiles()
                ?.filter { it.extension == "mp4" }
                ?.sortedByDescending { it.lastModified() }
                ?.take(5)
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote Control") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!serviceRunning && (recordingFeatureEnabled || remoteAccessFeatureEnabled)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Service Starting...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The service will start automatically when you grant screen capture permission. " +
                                    "Reopen the app if the permission dialog was dismissed.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!serviceRunning && !recordingFeatureEnabled && !remoteAccessFeatureEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Both features are disabled. Enable at least one to start the service.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── SECTION 1: Screen Recording ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle(icon = Icons.Default.Videocam, title = "Screen Recording")
                Switch(
                    checked = recordingFeatureEnabled,
                    onCheckedChange = { enabled ->
                        FeaturePreferences.setRecordingEnabled(context, enabled)
                        if (enabled && !RemoteAccessService.isRunning) {
                            onRequestServiceStart()
                        } else if (RemoteAccessService.isRunning) {
                            RemoteAccessService.sendToggleRecording(context, enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = Color.White
                    )
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (recordingFeatureEnabled)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                if (recordingFeatureEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Recording status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) Color(0xFFF44336) else Color(0xFF9E9E9E))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (isRecording) {
                                val elapsed = System.currentTimeMillis() - recordingStartTime
                                val remaining = (60 * 60 * 1000L) - elapsed
                                Text(
                                    text = "Recording (${formatDuration(remaining)} remaining)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF44336)
                                )
                            } else {
                                Text(
                                    text = "Idle",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Next recording
                        if (!isRecording && nextRecordingTime > 0) {
                            val timeUntil = nextRecordingTime - System.currentTimeMillis()
                            if (timeUntil > 0) {
                                StatusRow(
                                    icon = Icons.Default.Schedule,
                                    label = "Next recording",
                                    value = "in ${formatDuration(timeUntil)}"
                                )
                            }
                        }

                        StatusRow(
                            icon = Icons.Default.FiberManualRecord,
                            label = "Schedule",
                            value = "10 seconds every hour"
                        )

                        StatusRow(
                            icon = Icons.Default.Folder,
                            label = "Storage",
                            value = "${recordings.size} recording(s)"
                        )

                        // Recent recordings
                        if (recordings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Recent Recordings",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            recordings.forEach { file ->
                                RecordingItem(file)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Screen recording is disabled",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── SECTION 2: Remote Access ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionTitle(icon = Icons.Default.Cast, title = "Remote Access")
                Switch(
                    checked = remoteAccessFeatureEnabled,
                    onCheckedChange = { enabled ->
                        FeaturePreferences.setRemoteAccessEnabled(context, enabled)
                        if (enabled && !RemoteAccessService.isRunning) {
                            onRequestServiceStart()
                        } else if (RemoteAccessService.isRunning) {
                            RemoteAccessService.sendToggleRemoteAccess(context, enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = Color.White
                    )
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (remoteAccessFeatureEnabled)
                        MaterialTheme.colorScheme.surface
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                if (remoteAccessFeatureEnabled) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Connection status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isClientConnected -> Color(0xFF4CAF50)
                                            serviceRunning -> Color(0xFFFFC107)
                                            else -> Color(0xFF9E9E9E)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when {
                                    isClientConnected -> "PC Client Connected"
                                    serviceRunning -> "Listening for connections"
                                    else -> "Service not running"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isClientConnected -> Color(0xFF4CAF50)
                                    serviceRunning -> Color(0xFFFFC107)
                                    else -> Color(0xFF9E9E9E)
                                }
                            )
                        }

                        // Device ID
                        if (serviceRunning) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Connect your PC client using device ID:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = deviceId,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        StatusRow(
                            icon = Icons.Default.Computer,
                            label = "Device ID",
                            value = deviceId
                        )

                        StatusRow(
                            icon = Icons.Default.Wifi,
                            label = "Relay",
                            value = if (serviceRunning) "34.169.113.109:9000" else "Not connected"
                        )

                        StatusRow(
                            icon = Icons.Default.Accessibility,
                            label = "Touch Control",
                            value = if (isAccessibilityEnabled) "Enabled" else "Disabled",
                            valueColor = if (isAccessibilityEnabled) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                } else {
                    Text(
                        text = "Remote access is disabled",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun RecordingItem(file: File) {
    val sizeMb = file.length() / (1024.0 * 1024.0)
    val name = file.nameWithoutExtension
        .removePrefix("recording_")
        .replace("_", " at ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format("%.1f MB", sizeMb),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
