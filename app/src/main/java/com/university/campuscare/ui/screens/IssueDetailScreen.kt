package com.university.campuscare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.viewmodel.IssuesViewModel
import com.university.campuscare.ui.components.StatusChip
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    issueId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: IssuesViewModel = viewModel()
) {
    val issues by viewModel.issues.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Find the issue by ID
    val issue = remember(issues, issueId) {
        issues.find { it.id == issueId }
    }
    
    LaunchedEffect(issueId) {
        if (issue == null) {
            viewModel.getIssueById(issueId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue Details") },
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
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        color = Color(0xFFFF0000)
                    )
                }
                issue == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Issue not found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    IssueDetailContent(
                        issue = issue,
                        onNavigateToChat = onNavigateToChat
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueDetailContent(
    issue: Issue,
    onNavigateToChat: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header Card with Status and ID
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEB)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Issue #${issue.id.take(8)}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    StatusChip(status = issue.status.name.replace("_", " "))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category badge
                Surface(
                    color = Color(0xFFFF0000).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = issue.category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF0000),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        // Title Section
        DetailSection(
            title = "Title",
            icon = Icons.Default.Title
        ) {
            Text(
                text = issue.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        
        // Description Section
        DetailSection(
            title = "Description",
            icon = Icons.Default.Description
        ) {
            Text(
                text = issue.description.ifBlank { "No description provided" },
                fontSize = 15.sp,
                color = Color.Black,
                lineHeight = 22.sp
            )
        }
        
        // Location Section
        DetailSection(
            title = "Location",
            icon = Icons.Default.LocationOn
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocationInfoRow(label = "Block", value = issue.location.block)
                LocationInfoRow(label = "Level", value = issue.location.level)
                if (issue.location.room.isNotBlank()) {
                    LocationInfoRow(label = "Room", value = issue.location.room)
                }
            }
        }
        
        // Reporter Information
        DetailSection(
            title = "Reported By",
            icon = Icons.Default.Person
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = issue.reporterName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "Reporter ID: ${issue.reportedBy}...",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        
        // Timestamps
        DetailSection(
            title = "Timeline",
            icon = Icons.Default.Schedule
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeInfoRow(
                    label = "Reported",
                    timestamp = issue.createdAt
                )
                if (issue.createdAt != issue.updatedAt) {
                    TimeInfoRow(
                        label = "Last Updated",
                        timestamp = issue.updatedAt
                    )
                }
            }
        }
        
        // Photo Section
        if (!issue.photoUrl.isNullOrBlank()) {
            DetailSection(
                title = "Photo Evidence",
                icon = Icons.Default.Image
            ) {
                AsyncImage(
                    model = issue.photoUrl,
                    contentDescription = "Issue Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
        
        // Assigned To (if assigned)
        if (!issue.assignedTo.isNullOrBlank()) {
            DetailSection(
                title = "Assigned To",
                icon = Icons.Default.AssignmentInd
            ) {
                Text(
                    text = issue.assignedToName ?: "Staff Member",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = "Staff ID: ${issue.assignedTo}...",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
        
        // Action Buttons
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                // Chat button (always available for assigned issues)
                if (issue.status != IssueStatus.PENDING) {
                    Button(
                        onClick = { onNavigateToChat(issue.id, issue.title) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Chat")
                    }
                } else {
                    Text("Chat available once issue is accepted.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DetailSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFFF0000)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF0000)
                )
            }
            
            content()
        }
    }
}

@Composable
private fun LocationInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value.ifBlank { "Not specified" },
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun TimeInfoRow(label: String, timestamp: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)),
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp)),
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
