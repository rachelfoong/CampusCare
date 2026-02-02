//kotlin
package com.university.campuscare.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.university.campuscare.data.model.IssueCategory
import com.university.campuscare.viewmodel.ReportState
import com.university.campuscare.viewmodel.ReportViewModel
import kotlinx.coroutines.launch
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFaultScreen(
    onNavigateBack: () -> Unit,
    userId: String,
    userName: String,
    viewModel: ReportViewModel = viewModel()
) {
    // UI state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var block by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val reportState by viewModel.reportState.collectAsState()

    // Helpers (declare before launcher)
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION

    // Helper function to actively fetch location
    fun fetchAndSetLocation(onErrorMessage: String = "Failed to get location") {
        if (ContextCompat.checkSelfPermission(context, locationPermission) != PackageManager.PERMISSION_GRANTED) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Location permission required") }
            return
        }

        val cts = CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        coroutineScope.launch {
                            try {
                                val addresses = withContext(Dispatchers.IO) {
                                    Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                                }
                                val place = if (!addresses.isNullOrEmpty()) {
                                    addresses[0].getAddressLine(0) ?: "${loc.latitude}, ${loc.longitude}"
                                } else {
                                    "${loc.latitude}, ${loc.longitude}"
                                }
                                room = place
                                snackbarHostState.showSnackbar("Location added")
                            } catch (e: IOException) {
                                // network or service error during geocoding
                                room = "${loc.latitude}, ${loc.longitude}"
                                snackbarHostState.showSnackbar("Reverse geocoding failed")
                            }
                        }
                    } else {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Location unavailable") }
                    }
                }
                .addOnFailureListener {
                    coroutineScope.launch { snackbarHostState.showSnackbar(onErrorMessage) }
                }
        } catch (e: SecurityException) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Location permission missing") }
        }
    }


    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchAndSetLocation()
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Location permission denied") }
        }
    }

    LaunchedEffect(reportState) {
        if (reportState is ReportState.Success) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Issue", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Issue Category", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryButton("Lift", selectedCategory == IssueCategory.LIFT, { viewModel.selectCategory(IssueCategory.LIFT) }, Modifier.weight(1f))
                    CategoryButton("Toilet", selectedCategory == IssueCategory.TOILET, { viewModel.selectCategory(IssueCategory.TOILET) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryButton("Wi-Fi", selectedCategory == IssueCategory.WIFI, { viewModel.selectCategory(IssueCategory.WIFI) }, Modifier.weight(1f))
                    CategoryButton("Classroom", selectedCategory == IssueCategory.CLASSROOM, { viewModel.selectCategory(IssueCategory.CLASSROOM) }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                CategoryButton("Other", selectedCategory == IssueCategory.OTHER, { viewModel.selectCategory(IssueCategory.OTHER) }, Modifier.fillMaxWidth(0.5f))
            }

            item {
                OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Brief description of the problem") }, shape = RoundedCornerShape(8.dp))
            }

            item {
                OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("Provide more details about the issue") }, shape = RoundedCornerShape(8.dp), maxLines = 5)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = block, onValueChange = { block = it }, modifier = Modifier.weight(1f), placeholder = { Text("e.g., Block A") }, shape = RoundedCornerShape(8.dp))
                    OutlinedTextField(value = level, onValueChange = { level = it }, modifier = Modifier.weight(1f), placeholder = { Text("e.g., Level 3") }, shape = RoundedCornerShape(8.dp))
                }
            }

            item {
                OutlinedTextField(value = room, onValueChange = { room = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Room number or area description") }, shape = RoundedCornerShape(8.dp))
            }

            item {
                Text("Photo & Location", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhotoLocationCard(Icons.Default.CameraAlt, "Take Photo", onClick = { /* implement camera */ }, Modifier.weight(1f))
                    PhotoLocationCard(
                        Icons.Default.LocationOn,
                        "Add Location",
                        onClick = {
                            // Check permission or request, then actively fetch and set `room`
                            if (ContextCompat.checkSelfPermission(context, locationPermission) == PackageManager.PERMISSION_GRANTED) {
                                fetchAndSetLocation()
                            } else {
                                permissionLauncher.launch(locationPermission)
                            }
                        },
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (room.isNotBlank()) {
                    Text("Location added: $room", fontSize = 14.sp, color = Color(0xFF007700))
                }
            }

            if (reportState is ReportState.Error) {
                item {
                    Text((reportState as ReportState.Error).message, color = Color(0xFFFF0000), fontSize = 14.sp)
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.submitReport(title, description, block, level, room, userId, userName)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                    enabled = reportState !is ReportState.Loading,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (reportState is ReportState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Submit Report", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFFFFEBEB) else Color.White,
            contentColor = if (isSelected) Color(0xFFFF0000) else Color.Gray
        ),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF0000) else Color.LightGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun PhotoLocationCard(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = text, tint = Color(0xFFFF0000), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(text, fontSize = 14.sp, color = Color.Gray)
        }
    }
}
