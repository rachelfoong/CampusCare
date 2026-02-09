package com.university.campuscare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
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
import com.university.campuscare.viewmodel.StaffViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacilitiesTeamScreen(
    onNavigateBack: () -> Unit,
    viewModel: StaffViewModel = viewModel()
) {
    // Collect state from ViewModel
    val staffList by viewModel.staffList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val issueCounts by viewModel.staffIssueCounts.collectAsState()

    // Local UI State for Search & Filter
    var searchQuery by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("All") }

    // Extract unique Departments
    val departments = remember(staffList) {
        listOf("All") + staffList
            .map { it.department } // Map to department
            .filter { it.isNotBlank() } // Remove empty ones
            .distinct()
            .sorted()
    }

    // Filter Logic using Department
    val filteredStaff = remember(staffList, searchQuery, selectedDept) {
        staffList.filter { user ->
            val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.department.contains(searchQuery, ignoreCase = true)

            val matchesDept = selectedDept == "All" || user.department == selectedDept

            matchesSearch && matchesDept
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadStaff()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Facilities Team", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            // --- Search & Filter Section ---
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by name or email") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF0000),
                        cursorColor = Color(0xFFFF0000)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips (Horizontal Scroll)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    departments.forEach { dept ->
                        FilterChip(
                            selected = selectedDept == dept,
                            onClick = { selectedDept = dept },
                            label = { Text(dept) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFEBEB),
                                selectedLabelColor = Color(0xFFFF0000)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedDept == dept,
                                borderColor = if (selectedDept == dept) Color(0xFFFF0000) else Color.LightGray
                            )
                        )
                    }
                }
            }

            // --- Staff List ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF0000))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredStaff.isEmpty()) {
                        item {
                            Text(
                                text = "No team members found.",
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        items(filteredStaff) { user ->
                            val count = issueCounts[user.userId] ?: 0
                            TeamMemberCard(user, count)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamMemberCard(user: User, issueCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Icon / Avatar
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = Color(0xFFFFEBEB)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initials = remember(user.name) {
                        val parts = user.name.trim().split("\\s+".toRegex())
                        if (parts.size >= 2) {
                            // Two or more words: Take first letter of first two words (e.g., "John Doe" -> "JD")
                            "${parts[0].first()}${parts[1].first()}"
                        } else {
                            // Single word: Take first two letters (e.g., "Admin" -> "AD")
                            user.name.take(2)
                        }
                    }

                    Text(
                        text = initials.uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF0000)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.department.ifBlank { "Staff Member" },
                    fontSize = 14.sp,
                    color = Color(0xFFFF0000),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Contact Info Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = user.email, fontSize = 12.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (issueCount == 1) "$issueCount Active Issue" else "$issueCount Active Issues",
                        fontSize = 12.sp,
                        color = if (issueCount > 0) Color(0xFFFF0000) else Color.Gray,
                        fontWeight = if (issueCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}