package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.campuscare.ui.components.ProfileOption

@Composable
fun ProfileTab(
    userName: String,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelpSupport: () -> Unit = {},
    onNavigateToFacilitiesTeam: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Profile Icon
        Icon(
            Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Color(0xFFFF0000)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = userName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Profile Options
        ProfileOption(
            icon = Icons.Default.Person,
            title = "Edit Profile",
            onClick = { /* TODO */ }
        )
        ProfileOption(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = onNavigateToSettings
        )
        ProfileOption(
            icon = Icons.Default.Info,
            title = "Help & Support",
            onClick = onNavigateToHelpSupport
        )
        ProfileOption(
            icon = Icons.Default.Groups,
            title = "Facilities Team",
            onClick = onNavigateToFacilitiesTeam
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", fontSize = 16.sp)
        }
    }
}
