
package com.university.campuscare.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.university.campuscare.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    issueId: String,
    onNavigateBack: () -> Unit,
    currentUserId: String,
    currentUserName: String,
    isAdmin: Boolean,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()

    // Local UI states for location flow
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var isRequestingLocation by remember { mutableStateOf(false) }
    var locationToShare by remember { mutableStateOf<Location?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Fetch a fresh location once permission is granted
            coroutineScope.launch {
                isRequestingLocation = true
                val loc = fetchCurrentLocation(context,fusedClient)
                isRequestingLocation = false
                if (loc != null) {
                    locationToShare = loc
                    showConfirmDialog = true
                } else {
                    localError = "Unable to fetch location. Try again."
                }
            }
        } else {
            localError = "Location permission denied."
        }
    }

    val combinedError = error ?: localError

    LaunchedEffect(issueId) {
        viewModel.loadMessages(issueId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isAdmin) "Chat (Issue #$issueId)" else "Chat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isLoading) {
                            Text("Connecting...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF0000),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            if (combinedError != null) {
                Surface(color = Color.Red, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = combinedError,
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                ) {
                    items(messages) { message ->
                        val isMe = message.senderId == currentUserId
                        MessageBubble(
                            message = message.message,
                            senderName = message.senderName,
                            timestamp = message.timestamp,
                            isMe = isMe,
                            isAdmin = message.isFromAdmin
                        )
                    }
                }

                if (isLoading && messages.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFFF0000)
                    )
                }
            }

            // Input Area with location button
            Surface(
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // Location button (explicit intent)
                    Box {
                        IconButton(
                            onClick = {
                                // Always fetch fresh location on tap
                                localError = null
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    coroutineScope.launch {
                                        isRequestingLocation = true
                                        val loc = fetchCurrentLocation(context, fusedClient)
                                        isRequestingLocation = false
                                        if (loc != null) {
                                            locationToShare = loc
                                            showConfirmDialog = true
                                        } else {
                                            localError = "Unable to fetch location. Try again."
                                        }
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFF0000), CircleShape)
                        ) {
                            // simple emoji avoids icon dependency issues
                            Text("📍", fontSize = 18.sp)
                        }

                        if (isRequestingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(
                                    issueId = issueId,
                                    senderId = currentUserId,
                                    senderName = currentUserName,
                                    text = messageText,
                                    isAdmin = isAdmin
                                )
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFFF0000), CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog: shows fetched coords and requires explicit send
    if (showConfirmDialog && locationToShare != null) {
        val lat = locationToShare!!.latitude
        val lon = locationToShare!!.longitude
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false; locationToShare = null },
            title = { Text("Share location?") },
            text = {
                Column {
                    Text("Latitude: $lat")
                    Text("Longitude: $lon")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("A link will be sent to the chat. This will not be sent automatically.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Format a Google Maps link and send as message
                    val mapsLink = "https://maps.google.com/?q=$lat,$lon"
                    viewModel.sendMessage(
                        issueId = issueId,
                        senderId = currentUserId,
                        senderName = currentUserName,
                        text = "Shared location: $mapsLink",
                        isAdmin = isAdmin
                    )
                    showConfirmDialog = false
                    locationToShare = null
                }) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    locationToShare = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper to fetch a fresh location exactly when requested
private suspend fun fetchCurrentLocation(
    context: android.content.Context,
    fusedClient: FusedLocationProviderClient
): Location? {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!granted) return null

    return suspendCancellableCoroutine { cont ->
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { cont.resume(null) }
        } catch (_: SecurityException) {
            cont.resume(null)
        } catch (_: Exception) {
            cont.resume(null)
        }
    }
}


@Composable
fun MessageBubble(
    message: String,
    senderName: String,
    timestamp: Long,
    isMe: Boolean,
    isAdmin: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = senderName + if (isAdmin) " (Admin)" else "",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
            )
        }
        Surface(
            color = if (isMe) Color(0xFFFF0000) else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Text(
                text = message,
                color = if (isMe) Color.White else Color.Black,
                modifier = Modifier.padding(12.dp),
                fontSize = 16.sp
            )
        }
        Text(
            text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp)),
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}
