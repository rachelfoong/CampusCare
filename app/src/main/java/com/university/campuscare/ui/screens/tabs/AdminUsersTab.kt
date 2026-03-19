package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.campuscare.viewmodel.AdminViewModel

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
            IconButton(onClick = {/* TODO: Handle add user button click */}) {
                Icon(Icons.Default.Add, contentDescription = "Add User", tint = Color(0xFFFF0000))
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
fun UserCard(name: String, email: String, reportsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                tint = Color(0xFFFF0000)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold)
                Text(
                    text = email,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$reportsCount",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFFF0000)
                )
                Text(
                    text = "reports",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
