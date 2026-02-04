package com.university.campuscare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.university.campuscare.data.model.IssueStatus
import com.university.campuscare.data.model.IssueCategory
import com.university.campuscare.ui.components.StatusChip
import com.university.campuscare.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.automirrored.filled.Chat
import com.university.campuscare.ui.components.AssignStaffDialog
import androidx.navigation.NavController
import com.university.campuscare.ui.Screen

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    viewModel: AdminViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filteredIssues = viewModel.getFilteredIssues()

    var showFilterMenu by remember { mutableStateOf(false) }

// Assign dialog state
    var showAssignDialog by remember { mutableStateOf(false) }
    var issueToAssign by remember { mutableStateOf<com.university.campuscare.data.model.Issue?>(null) }
    val staffList by viewModel.staffList.collectAsState()
    val isLoadingStaff by viewModel.isLoadingStaff.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Red Header with Stats
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF0000))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Admin Dashboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminStatCard(
                        value = stats.total.toString(),
                        label = "Total",
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        value = stats.pending.toString(),
                        label = "Pending",
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        value = stats.active.toString(),
                        label = "Active",
                        modifier = Modifier.weight(1f)
                    )
                    AdminStatCard(
                        value = stats.resolved.toString(),
                        label = "Resolved",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search reports...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5)
                    )
                )
                Box {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedCategory != null) Color(0xFFD32F2F) else Color(0xFFFF0000)
                        )
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        Text(
                            "Filter by Category",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                viewModel.setCategoryFilter(null)
                                showFilterMenu = false
                            },
                            leadingIcon = {
                                if (selectedCategory == null) Icon(Icons.Default.Check, contentDescription = null)
                            }
                        )
                        //IssueCategory.values().forEach { category ->
                        IssueCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.setCategoryFilter(category)
                                    showFilterMenu = false
                                },
                                leadingIcon = {
                                    if (selectedCategory == category) Icon(Icons.Default.Check, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            if (selectedCategory != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Category: ${selectedCategory?.name}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    IconButton(
                        onClick = { viewModel.setCategoryFilter(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Category", modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Issues List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFF0000))
                }
            } else if (filteredIssues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reports found",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredIssues) { issue ->
                        AdminIssueCard(
                            issue = issue,
                            onAccept = { viewModel.acceptIssue(issue.id) },
                            onAssign = { issueToAssign = issue
                                showAssignDialog = true },
                            onOpenChat = { onNavigateToChat(issue.id, issue.title) },
                            onMarkResolved = { viewModel.resolveIssue(issue.id) },
                            onDelete = { viewModel.deleteIssue(issue.id) },
                            onClick = { navController.navigate(Screen.IssueDetail.createRoute(issue.id))}
                        )
                    }
                }
            }
        }
        // Assign Staff Dialog
        if (showAssignDialog && issueToAssign != null) {
            AssignStaffDialog(
                staffList = staffList,
                isLoading = isLoadingStaff,
                onDismiss = { showAssignDialog = false },
                onAssignStaff = { selectedStaff ->
                    viewModel.assignIssueToStaff(
                        issueId = issueToAssign!!.id,
                        staffUserId = selectedStaff.userId,
                        staffName = selectedStaff.name
                    )
                    showAssignDialog = false
                }
            )
        }
    }
}

@Composable
private fun AdminStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD32F2F)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AdminIssueCard(
    issue: com.university.campuscare.data.model.Issue,
    onAccept: () -> Unit,
    onAssign: () -> Unit,
    onOpenChat: () -> Unit,
    onMarkResolved: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = {}
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = issue.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Reported by ${issue.reporterName}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Box {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false }
                    ) {
                    // Add things in dropdownmenu if needed
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(status = issue.status.name.replace("_", " "))
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = issue.category,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 ${issue.location.block}, ${issue.location.level}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(Date(issue.createdAt)),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }


            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Action Buttons Section - Differentiated by Status
                when (issue.status) {
                    IssueStatus.PENDING -> {
                        // For PENDING issues: Show Accept and Assign buttons
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF0000)
                                )
                            ) {
                                Text("Accept")
                            }
                            OutlinedButton(
                                onClick = onAssign,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF0000)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFFF0000)
                                )
                            ) {
                                Text("Assign")
                            }
                        }
                    }

                    IssueStatus.IN_PROGRESS -> {
                        // For IN_PROGRESS issues: Show "Go to Issue Chat" and "Mark as Resolved" buttons
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenChat,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF0000)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFFF0000)
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Chat", fontSize = 13.sp)
                            }
                            Button(
                                onClick = onMarkResolved,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resolved", fontSize = 13.sp)
                            }
                        }
                    }

                    IssueStatus.RESOLVED -> {
                        // For IN_PROGRESS issues: Show "Go to Issue Chat" and "Mark as Resolved" buttons
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenChat,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF0000)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    Color(0xFFFF0000)
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Chat", fontSize = 13.sp)
                            }
                            Button(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
