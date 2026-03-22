package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.university.campuscare.viewmodel.AdminViewModel
import com.university.campuscare.viewmodel.AuthViewModel
import com.university.campuscare.ui.screens.tabs.*
import com.university.campuscare.viewmodel.AuthState

sealed class AdminBottomNavItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    object Dashboard : AdminBottomNavItem("Dashboard", Icons.Default.Dashboard)
    object AllReports : AdminBottomNavItem("Reports", Icons.Default.CheckCircle)
    object Analytics : AdminBottomNavItem("Analytics", Icons.Default.BarChart)
    object Users : AdminBottomNavItem("Users", Icons.Default.Group)
    object StaffMgmt : AdminBottomNavItem("Staff", Icons.Default.Engineering)
    object Recordings : AdminBottomNavItem("Recordings", Icons.Default.Videocam)
    object Settings : AdminBottomNavItem("Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavController,
    onLogout: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    authViewModel: AuthViewModel,
    viewModel: AdminViewModel = viewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val bottomNavItems = listOf(
        AdminBottomNavItem.Dashboard,
        AdminBottomNavItem.AllReports,
        AdminBottomNavItem.Analytics,
        AdminBottomNavItem.Users,
        AdminBottomNavItem.StaffMgmt,
        AdminBottomNavItem.Recordings, 
        AdminBottomNavItem.Settings
    )

    val authState by authViewModel.authState.collectAsState()
    val userName = if (authState is AuthState.Authenticated) (authState as AuthState.Authenticated).user.name else "Admin"

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        label = { Text(item.title, fontSize = 11.sp) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFFF0000),
                            selectedTextColor = Color(0xFFFF0000),
                            indicatorColor = Color(0xFFFFEBEB)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> AdminDashboardScreen(navController, userName, onNavigateToChat, viewModel)
                1 -> AdminReportsTab(viewModel)
                2 -> AdminAnalyticsTab(viewModel)
                3 -> AdminUsersTab(viewModel)
                4 -> StaffManagementTab(viewModel, authViewModel)
                5 -> AdminRecordingsTab(viewModel)
                6 -> AdminSettingsTab(userName, onLogout, navController)
            }
        }
    }
}

@Composable
fun AdminReportsTab(viewModel: AdminViewModel) {
    val filteredIssues = viewModel.getFilteredIssues()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All Reports",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips
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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredIssues.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No reports found", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredIssues) { issue ->
                    AdminReportCard(
                        title = issue.title,
                        description = issue.description,
                        reporter = issue.reporterName,
                        status = issue.status.name,
                        date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(issue.createdAt))
                    )
                }
            }
        }
    }
}

@Composable
fun AdminAnalyticsTab(viewModel: AdminViewModel) {
    val analyticsData by viewModel.analyticsData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Analytics",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Report Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Report Statistics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                AnalyticsRow("Total Reports This Month", analyticsData.totalReportsThisMonth.toString())
                AnalyticsRow("Average Resolution Time", analyticsData.averageResolutionTime)
                AnalyticsRow("Most Reported Issue", analyticsData.mostReportedIssue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Category Breakdown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (analyticsData.categoryBreakdown.isEmpty()) {
                    Text(
                        text = "No data available",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    val categories = analyticsData.categoryBreakdown.entries.sortedByDescending { it.value }
                    val colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.error
                    )

                    categories.forEachIndexed { index, (category, percentage) ->
                        val color = colors.getOrElse(index) { MaterialTheme.colorScheme.primary }
                        CategoryBar(category, percentage, color)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab(viewModel: AdminViewModel) {
    val allUsers by viewModel.allUsers.collectAsState()
    val allIssues by viewModel.allIssues.collectAsState()

    // Specifically filter to only show Student users (excludes Staff and Admin)
    val studentUsers = allUsers.filter { it.role == "STUDENT" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Users",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(studentUsers) { user ->
                val reportsCount = allIssues.count { it.reportedBy == user.userId }
                UserCard(
                    name = user.name,
                    email = user.email,
                    reportsCount = reportsCount
                )
            }
        }
    }
}

@Composable
fun AdminRecordingsTab(viewModel: AdminViewModel) {
    val recordings by viewModel.recordings.collectAsState()
    val isLoading by viewModel.isLoadingRecordings.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Screen Recordings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                viewModel.loadRecordings()
                viewModel.loadDevices()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        // ── Connected Devices ──
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Devices",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (devices.isEmpty()) {
            Text(
                text = "No devices seen yet",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            devices.forEach { device ->
                DeviceCard(device = device, onCopyIp = {
                    clipboardManager.setText(
                        androidx.compose.ui.text.AnnotatedString(device.deviceId)
                    )
                })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Recordings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            recordings.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No recordings yet",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recordings) { recording ->
                        RecordingCard(
                            recording = recording,
                            onView = {
                                if (recording.downloadUrl.isNotBlank()) {
                                    uriHandler.openUri(recording.downloadUrl)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    recording: com.university.campuscare.data.model.Recording,
    onView: () -> Unit
) {
    val dateStr = try {
        val filename = recording.downloadUrl
            .substringAfterLast("%2F")
            .substringBefore("?")
            .removeSuffix(".mp4")
            .removePrefix("recording_")
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .parse(filename)
            ?.let { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(it) }
            ?: SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(recording.timestamp))
    } catch (e: Exception) {
        SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(recording.timestamp))
    }
    val sizeMb = recording.fileSizeBytes / (1024.0 * 1024.0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = dateStr,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                TextButton(onClick = onView) {
                    Text("View")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabelValue("Device", recording.deviceId.takeLast(8))
                LabelValue("Size", String.format("%.1f MB", sizeMb))
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DeviceCard(
    device: com.university.campuscare.viewmodel.DeviceInfo,
    onCopyIp: () -> Unit
) {
    val lastSeenStr = if (device.lastSeen > 0L) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(device.lastSeen))
    } else {
        "Unknown"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceId,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
                Text(
                    text = "Last active: $lastSeenStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = onCopyIp) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy IP",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AdminSettingsTab(userName: String, onLogout: () -> Unit, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile header
        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = userName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Administrator",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Settings options
        SettingsOption(Icons.Default.Notifications, "Notification Settings") { /* TODO */ }
        SettingsOption(Icons.Default.Lock, "Security") { /* TODO */ }
        SettingsOption(Icons.Default.Build, "System Configuration") { /* TODO */ }
        SettingsOption(Icons.Default.Info, "About") { /* TODO */ }
        
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
fun AdminReportCard(
    title: String,
    description: String,
    reporter: String,
    status: String,
    date: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "By: $reporter",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = date,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "PENDING" -> MaterialTheme.colorScheme.error
        "IN_PROGRESS" -> MaterialTheme.colorScheme.tertiary
        "RESOLVED" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.replace("_", " "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AdminFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    )
}

@Composable
fun AnalyticsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CategoryBar(label: String, percentage: Int, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 14.sp)
            Text(text = "$percentage%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color
        )
    }
}

@Composable
fun UserCard(name: String, email: String, reportsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold)
                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$reportsCount",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "reports",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun SettingsOption(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
