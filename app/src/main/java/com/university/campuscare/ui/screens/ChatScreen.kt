package com.university.campuscare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.campuscare.data.model.Message
import com.university.campuscare.viewmodel.AuthState
import com.university.campuscare.viewmodel.AuthViewModel
import com.university.campuscare.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.automirrored.filled.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    issueId: String,
    issueTitle: String,
    currentUserId: String,
    currentUserName: String,
    isAdmin: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val messageText = remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    LaunchedEffect(issueId) {
        viewModel.loadMessages(issueId)
    }
    
    // Auto-scroll to bottom when new messages arrive
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
                            text = "Issue Chat",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = issueTitle,
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
                    titleContentColor = Color.White
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
                    // Error message
                    if (error != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF0000),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error ?: "",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF0000)
                                )
                            }
                        }
                    }
                    
                    // Message input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageText.value,
                            onValueChange = { messageText.value = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color(0xFFF5F5F5),
                                focusedContainerColor = Color(0xFFF5F5F5)
                            ),
                            maxLines = 4
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                if (messageText.value.isNotBlank()) {
                                    viewModel.sendMessage(
                                        issueId = issueId,
                                        senderId = currentUserId,
                                        senderName = currentUserName,
                                        text = messageText.value,
                                        isAdmin = isAdmin
                                    )
                                    messageText.value = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (messageText.value.isBlank()) 
                                        Color.Gray 
                                    else 
                                        Color(0xFFFF0000),
                                    shape = CircleShape
                                )
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading && messages.isEmpty() -> {
                    // Loading state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF0000),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading messages...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
                messages.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No messages yet",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start the conversation about this issue",
                            fontSize = 14.sp,
                            color = Color.Gray.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    // Messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.senderId == currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender name (only for other users' messages)
        if (!isCurrentUser) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.senderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                if (message.isFromAdmin) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        color = Color(0xFFFF0000),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Admin",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
        
        // Message bubble
        Surface(
            color = if (isCurrentUser) Color(0xFFFF0000) else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    fontSize = 15.sp,
                    color = if (isCurrentUser) Color.White else Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 11.sp,
                    color = if (isCurrentUser) 
                        Color.White.copy(alpha = 0.7f) 
                    else 
                        Color.Gray
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
