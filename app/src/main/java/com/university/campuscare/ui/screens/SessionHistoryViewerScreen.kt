package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.campuscare.data.model.ClientProfile
import com.university.campuscare.data.repository.ClientProfileRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryViewerScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<ClientProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Collect sessions flow
    LaunchedEffect(userId) {
        try {
            ClientProfileRepository.getSessions(userId).collect { sessionList ->
                sessions = sessionList
                isLoading = false
            }
        } catch (e: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session History - Internal Demo", fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Clear all sessions button
                    if (sessions.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Clear all")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                sessions.isEmpty() -> {
                    Text(
                        "No session history found",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "Total Sessions: ${sessions.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(sessions) { session ->
                            SessionHistoryCard(
                                session = session,
                                onDelete = {
                                    coroutineScope.launch {
                                        ClientProfileRepository.deleteSession(userId, session.sessionId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete all confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Clear All Session History") },
            text = { Text("Are you sure you want to delete all ${sessions.size} session records? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            ClientProfileRepository.clearHistory(userId)
                            showDeleteAllDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SessionHistoryCard(
    session: ClientProfile,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Session ID and timestamp header
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Session ID: ${session.sessionId.take(12)}...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        formatTimestamp(session.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Device info grid
            DeviceInfoGrid(session)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Session") },
            text = { Text("Delete this session record?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DeviceInfoGrid(session: ClientProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Device hardware
        DeviceInfoRow("Device:", "${session.manufacturer} ${session.deviceModel}")
        DeviceInfoRow("OS Version:", "Android ${session.osVersion} (SDK ${session.sdkInt})")

        // App info
        DeviceInfoRow("App Version:", "${session.appVersion} (Build: ${session.appBuild})")

        // Network
        DeviceInfoRow("Network:", session.networkType)
        if (session.carrier.isNotEmpty()) {
            DeviceInfoRow("Carrier:", session.carrier)
        }

        // Display
        DeviceInfoRow(
            "Screen:",
            "${session.screenWidthPx}x${session.screenHeightPx} @ ${session.screenDpi} dpi"
        )

        // Locale
        DeviceInfoRow("Locale:", session.locale)
        DeviceInfoRow("Timezone:", session.timezone)
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            modifier = Modifier.weight(0.65f)
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return if (timestamp > 0) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
        sdf.format(Date(timestamp))
    } else {
        "No timestamp"
    }
}
