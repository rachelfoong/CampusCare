package com.university.campuscare.ui.screens.tabs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.campuscare.ui.components.IssueCard
import com.university.campuscare.viewmodel.IssuesState
import com.university.campuscare.viewmodel.IssuesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IssuesTab(
    userId: String,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToIssueDetails: (String) -> Unit,
    viewModel: IssuesViewModel = viewModel()
) {
    val issues by viewModel.issues.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val issuesState by viewModel.issuesState.collectAsState()

    LaunchedEffect(userId) {
        Log.d("IssuesTab", "LaunchedEffect triggered with userId: $userId")
        viewModel.loadIssues(userId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Issues",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                Text(
                    text = "Loading...",

                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (issuesState is IssuesState.Error) {
                Text(
                    text = (issuesState as IssuesState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (issues.isEmpty()) {
                Text(
                    text = "No issues found for user: $userId",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(issues) { issue ->
                        IssueCard(
                            title = issue.title,
                            status = issue.status.name,
                            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(issue.createdAt)),
                            location = issue.location.block,
                            urgency = issue.urgency.name,
                            onClick = { onNavigateToIssueDetails(issue.id) },
                            onChatClick = if (issue.status == com.university.campuscare.data.model.IssueStatus.IN_PROGRESS) {
                                { onNavigateToChat(issue.id, issue.title) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}
