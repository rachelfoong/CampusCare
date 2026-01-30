package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.ui.components.IssueCard
import com.university.campuscare.viewmodel.IssuesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyIssuesScreen(
    onIssueClick: (String) -> Unit,
    userId: String,
    viewModel: IssuesViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filteredIssues = viewModel.getFilteredIssues()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "My Issues",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search issues...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedContainerColor = Color(0xFFF5F5F5)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IssueFilterChip(
                text = "All",
                isSelected = selectedFilter == null,
                onClick = { viewModel.setFilter(null) }
            )
            IssueFilterChip(
                text = "Pending",
                isSelected = selectedFilter == IssueStatus.PENDING,
                onClick = { viewModel.setFilter(IssueStatus.PENDING) }
            )
            IssueFilterChip(
                text = "In Progress",
                isSelected = selectedFilter == IssueStatus.IN_PROGRESS,
                onClick = { viewModel.setFilter(IssueStatus.IN_PROGRESS) }
            )
            IssueFilterChip(
                text = "Resolved",
                isSelected = selectedFilter == IssueStatus.RESOLVED,
                onClick = { viewModel.setFilter(IssueStatus.RESOLVED) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Issues List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF0000))
            }
        } else if (filteredIssues.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "No issues found",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredIssues) { issue ->
                    IssueCard(
                        title = issue.title,
                        status = issue.status.name.replace("_", " "),
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(Date(issue.createdAt)),
                        location = "${issue.location.block}, ${issue.location.level}"
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFFEBEB),
            selectedLabelColor = Color(0xFFFF0000),
            containerColor = Color.White,
            labelColor = Color.Gray
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = if (isSelected) Color(0xFFFF0000) else Color.LightGray,
            selectedBorderColor = Color(0xFFFF0000),
            enabled = true,
            selected = isSelected
        )
    )
}
