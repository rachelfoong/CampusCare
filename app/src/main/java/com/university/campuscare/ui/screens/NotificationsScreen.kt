package com.university.campuscare.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.campuscare.data.model.NotificationType
import com.university.campuscare.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { viewModel.markAllAsRead("") }) {
                Icon(
                    Icons.Default.DoneAll,
                    contentDescription = "Mark all read",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFFF0000)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Mark all read",
                    color = Color(0xFFFF0000)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Notifications List
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        type = notification.type,
                        title = notification.title,
                        message = notification.message,
                        timestamp = notification.timestamp,
                        isRead = notification.isRead,
                        onClick = {
                            if (!notification.isRead) {
                                viewModel.markAsRead("", notification.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    type: NotificationType,
    title: String,
    message: String,
    timestamp: Long,
    isRead: Boolean,
    onClick: () -> Unit
) {
    val (icon, backgroundColor) = when (type) {
        NotificationType.ISSUE_RESOLVED -> Icons.Default.CheckCircle to Color(0xFFE8F5E9)
        NotificationType.STATUS_UPDATE -> Icons.Default.Update to Color(0xFFE3F2FD)
        NotificationType.NEW_MESSAGE -> Icons.Default.Message to Color(0xFFFFEBEE)
        NotificationType.MAINTENANCE_SCHEDULE -> Icons.Default.Schedule to Color(0xFFFFF3E0)
    }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) Color.White else Color(0xFFFFF9F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = backgroundColor,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = when (type) {
                            NotificationType.ISSUE_RESOLVED -> Color(0xFF4CAF50)
                            NotificationType.STATUS_UPDATE -> Color(0xFF2196F3)
                            NotificationType.NEW_MESSAGE -> Color(0xFFFF0000)
                            NotificationType.MAINTENANCE_SCHEDULE -> Color(0xFFFF9800)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getTimeAgo(timestamp),
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
            
            if (!isRead) {
                Surface(
                    color = Color(0xFFFF0000),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} ${if (diff / (60 * 1000) == 1L) "minute" else "minutes"} ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} ${if (diff / (60 * 60 * 1000) == 1L) "hour" else "hours"} ago"
        else -> "${diff / (24 * 60 * 60 * 1000)} ${if (diff / (24 * 60 * 60 * 1000) == 1L) "day" else "days"} ago"
    }
}
