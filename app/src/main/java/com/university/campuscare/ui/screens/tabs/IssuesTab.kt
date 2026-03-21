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
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.ui.components.AdminFilterChip
import com.university.campuscare.ui.components.IssueCard
import com.university.campuscare.viewmodel.IssuesState
import com.university.campuscare.viewmodel.IssuesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IssuesTab(
    userId: String,
    userRole: String,
    onNavigateToChat: (String, String) -> Unit,
    onNavigateToIssueDetails: (String) -> Unit,
    viewModel: IssuesViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val issuesState by viewModel.issuesState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val filteredIssues = viewModel.getFilteredIssues()

    LaunchedEffect(userId, userRole) {
        Log.d("IssuesTab", "LaunchedEffect triggered with userId: $userId, role: $userRole")
        viewModel.loadIssues(userId, userRole)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminFilterChip(
                text = "All",
                isSelected = selectedFilter == null,
                onClick = { viewModel.setFilter(null) }
            )
            AdminFilterChip(
                text = "Pending",
                isSelected = selectedFilter == IssueStatus.PENDING,
                onClick = { viewModel.setFilter(IssueStatus.PENDING) }
            )
            AdminFilterChip(
                text = "In Progress",
                isSelected = selectedFilter == IssueStatus.IN_PROGRESS,
                onClick = { viewModel.setFilter(IssueStatus.IN_PROGRESS) }
            )
            AdminFilterChip(
                text = "Resolved",
                isSelected = selectedFilter == IssueStatus.RESOLVED,
                onClick = { viewModel.setFilter(IssueStatus.RESOLVED) }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFFFF0000),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (issuesState is IssuesState.Error) {
                Text(
                    text = (issuesState as IssuesState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (filteredIssues.isEmpty()) {
                Text(
                    text = if (selectedFilter == null) "No issues found" else "No ${selectedFilter?.name?.lowercase()?.replace("_", " ")} issues",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredIssues) { issue ->
                        IssueCard(
                            title = issue.title,
                            status = issue.status.name,
                            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(issue.createdAt)),
                            location = issue.location.block,
                            urgency = issue.urgency.name,
                            onClick = { onNavigateToIssueDetails(issue.id) },
                            onChatClick = if (issue.status == IssueStatus.IN_PROGRESS) {
                                { onNavigateToChat(issue.id, issue.title) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}