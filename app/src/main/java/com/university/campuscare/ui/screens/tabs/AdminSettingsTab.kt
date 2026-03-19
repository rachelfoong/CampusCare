package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.university.campuscare.ui.Screen

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
            tint = Color(0xFFFF0000)
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
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Settings options (Wired to shared User/Staff screens)
        SettingsOption(Icons.Default.Person, "Edit Profile") {
            navController.navigate(Screen.UserProfile.route)
        }
        SettingsOption(Icons.Default.Notifications, "Settings") {
            navController.navigate(Screen.Settings.route)
        }
        SettingsOption(Icons.Default.Info, "Help & Support") {
            navController.navigate(Screen.HelpSupport.route)
        }
        SettingsOption(icon = Icons.Default.Groups, "Facilities Team") {
            navController.navigate(Screen.FacilitiesTeam.route)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

@Composable
fun SettingsOption(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFFF0000))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 16.sp)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}
