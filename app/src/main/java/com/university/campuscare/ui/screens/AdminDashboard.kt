package com.university.campuscare.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AdminDashboard() {
    // Admin Privileges: View all open tickets
    LazyColumn {
        item {
            Text("Admin Dashboard: Open Tickets")
        }
        // List items here...
    }
}