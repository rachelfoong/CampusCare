package com.university.campuscare.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.university.campuscare.data.repository.DirectMessage
import com.university.campuscare.viewmodel.DirectMessageViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    adminId: String,
    adminName: String,
    currentUserId: String,
    currentUserName: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    viewModel: DirectMessageViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Unique conversation id (stable)
    val conversationId = remember { listOf(currentUserId, adminId).sorted().joinToString("_") }

    // ---- Location sharing state ----
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    var isRequestingLocation by remember { mutableStateOf(false) }
    var locationToShare by remember { mutableStateOf<Location?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val combinedError = error ?: localError

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
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
            localError = "Location permission denied."
        }
    }

    // Load messages
    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
        viewModel.startConversationInsights(
            conversationId = conversationId,
            currentUserId = currentUserId,
            otherUserId = adminId
        )
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { onNavigateToProfile(adminId) }) {
                        Text(
                            text = adminName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to view profile",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF0000),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Column {
                    // Error banner (viewModel OR local)
                    if (combinedError != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = combinedError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Input row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF0000),
                                cursorColor = Color(0xFFFF0000)
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 📍 Location share button
                        Box {
                            IconButton(
                                onClick = {
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
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFFF0000), CircleShape)
                            ) {
                                Text("📍", fontSize = 16.sp, color = Color.White)
                            }

                            if (isRequestingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .align(Alignment.Center),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send button
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    val newMessage = DirectMessage(
                                        senderId = currentUserId,
                                        senderName = currentUserName,
                                        receiverId = adminId,
                                        message = messageText,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    viewModel.sendMessage(conversationId, newMessage)
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (messageText.isNotBlank()) Color(0xFFFF0000) else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            when {
                isLoading && messages.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF0000))
                    }
                }

                messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No messages yet", fontSize = 16.sp, color = Color.Gray)
                        Text(
                            "Start a conversation with $adminName",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            DirectMessageBubble(
                                message = msg,
                                isCurrentUser = msg.senderId == currentUserId
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ Confirm dialog for location sharing
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
                    Spacer(Modifier.height(8.dp))
                    Text("A map link will be sent in chat.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val mapsLink = "https://maps.google.com/?q=$lat,$lon"
                    val newMessage = DirectMessage(
                        senderId = currentUserId,
                        senderName = currentUserName,
                        receiverId = adminId,
                        message = "📍 Shared location: $mapsLink",
                        timestamp = System.currentTimeMillis()
                    )
                    viewModel.sendMessage(conversationId, newMessage)

                    showConfirmDialog = false
                    locationToShare = null
                }) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    locationToShare = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DirectMessageBubble(
    message: DirectMessage,
    isCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isCurrentUser) 12.dp else 4.dp,
                topEnd = if (isCurrentUser) 4.dp else 12.dp,
                bottomStart = 12.dp,
                bottomEnd = 12.dp
            ),
            color = if (isCurrentUser) Color(0xFFFF0000) else Color(0xFFF5F5F5),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 11.sp,
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> {
            val minutes = diff / (60 * 1000)
            "$minutes ${if (minutes == 1L) "min" else "mins"} ago"
        }
        diff < 24 * 60 * 60 * 1000 -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

// Helper: fetch fresh location safely (checks permission)
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
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(null) }
        } catch (_: SecurityException) {
            cont.resume(null)
        } catch (_: Exception) {
            cont.resume(null)
        }
    }
}