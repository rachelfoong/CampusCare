package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import com.university.campuscare.viewmodel.DirectMessageViewModel

@Composable
fun MessagesTab(
    currentUserId: String,
    onNavigateToDirectChat: (String, String) -> Unit,
    viewModel: DirectMessageViewModel = viewModel()
) {
    val adminUsersState by viewModel.adminUsers.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAdminUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Messages",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF0000),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Contact Campus Admins",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            adminUsersState is DataResult.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF0000))
                }
            }
            adminUsersState is DataResult.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading admins",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = (adminUsersState as DataResult.Error).error.peekContent(),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            adminUsersState is DataResult.Success -> {
                val adminUsers = (adminUsersState as DataResult.Success).data
                if (adminUsers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No admins available",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(adminUsers) { admin ->
                            AdminContactCard(
                                admin = admin,
                                onClick = {
                                    onNavigateToDirectChat(admin.userId, admin.name)
                                }
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@Composable
fun AdminContactCard(
    admin: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile icon/picture
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color(0xFFFFEBEB)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Admin details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = admin.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Admin",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Text(
                    text = "Campus Admin",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                if (admin.department.isNotEmpty()) {
                    Text(
                        text = admin.department,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Message indicator
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Send Message",
                tint = Color(0xFFFF0000),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
