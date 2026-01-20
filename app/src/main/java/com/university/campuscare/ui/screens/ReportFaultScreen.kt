package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.university.campuscare.viewmodel.ReportViewModel

@Composable
fun ReportFaultScreen(
    viewModel: ReportViewModel = viewModel(),
    onSubmitSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") } // [cite: 12] Location sharing

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Report a Facility Issue", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Issue Title (e.g. Broken Lift)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Location Input (Eventually replace with GPS logic)
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location (e.g. Block A, Level 2)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Triggers the ViewModel function which calls ImageHandler
                viewModel.submitReport(title, location)
                onSubmitSuccess()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit Report")
        }
    }
}