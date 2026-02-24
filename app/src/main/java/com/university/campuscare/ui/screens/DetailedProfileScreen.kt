package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import com.university.campuscare.viewmodel.ProfileSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDirectChat: (String, String) -> Unit = { _, _ -> },
    viewModel: ProfileSearchViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isLoading by viewModel.isLoadingProfile.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
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
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF0000))
                }
            }
            userProfile is DataResult.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error loading profile",
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = (userProfile as DataResult.Error).error.peekContent(),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadUserProfile(userId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF0000)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            userProfile is DataResult.Success -> {
                val user = (userProfile as DataResult.Success<User>).data
                DetailedProfileContent(
                    user = user,
                    onNavigateToDirectChat = onNavigateToDirectChat,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                // Idle or Loading state
            }
        }
    }
}

@Composable
fun DetailedProfileContent(
    user: User,
    onNavigateToDirectChat: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture
        if (user.profilePhotoUrl.isNotEmpty()) {
            AsyncImage(
                model = user.profilePhotoUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color(0xFFFF0000)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = user.name,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Role badge
        Surface(
            color = when (user.role) {
                "ADMIN" -> Color(0xFFFF0000)
                "STAFF" -> Color(0xFF4CAF50)
                else -> Color(0xFF2196F3)
            },
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = user.role,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Contact Information Section
        DetailSection(title = "Contact Information") {
            ProfileInfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = user.email
            )
            if (user.department.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                ProfileInfoRow(
                    icon = Icons.Default.Business,
                    label = "Department",
                    value = user.department
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Expertise/Role Section
        DetailSection(title = "Role & Expertise") {
            ProfileInfoRow(
                icon = Icons.Default.Work,
                label = "Role",
                value = when (user.role) {
                    "ADMIN" -> "Campus Administrator"
                    "STAFF" -> "Maintenance Staff"
                    else -> "Student"
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action buttons
        if (user.role == "ADMIN" || user.role == "STAFF") {
            Button(
                onClick = { onNavigateToDirectChat(user.userId, user.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0000)
                )
            ) {
                Icon(Icons.Default.Chat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Message")
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF0000)
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFFF0000),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.Black
            )
        }
    }
}
