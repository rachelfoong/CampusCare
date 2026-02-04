package com.university.campuscare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
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
import com.university.campuscare.data.model.Issue
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.ui.components.StatusChip
import com.university.campuscare.viewmodel.IssuesState
import com.university.campuscare.viewmodel.IssuesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailsScreen(
    issueId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: IssuesViewModel = viewModel()
) {
    LaunchedEffect(issueId) {
        viewModel.getIssueById(issueId)
    }

    // Find the issue from the ViewModel's list
    val issues by viewModel.issues.collectAsState()
    val issuesState by viewModel.issuesState.collectAsState()

//    val issue = remember(issues, issueId) { issues.find { it.id == issueId } }
    val issue = issues.firstOrNull { it.id == issueId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue Details") },
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
        if (issuesState is IssuesState.Loading || issue == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF0000))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF5F5F5))
            ) {
                // Image Section (if photos exist)
//                if (issue.photos.isNotEmpty()) {
//                    AsyncImage(
//                        model = issue.photos.first(),
//                        contentDescription = "Issue Photo",
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(250.dp),
//                        contentScale = ContentScale.Crop
//                    )
//                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Header: Title and Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = issue.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusChip(status = issue.status.name)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Chip
                    Surface(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = issue.category,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF0000))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${issue.location.block}, ${issue.location.level}", fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFFFF0000))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Reported On", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(issue.createdAt)),
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Description
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = issue.description,
                        fontSize = 16.sp,
                        color = Color.Gray,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Chat Button
                    Button(
                        onClick = { onNavigateToChat(issue.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}