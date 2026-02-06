package com.university.campuscare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.university.campuscare.utils.DataResult
import com.university.campuscare.viewmodel.UserProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: UserProfileViewModel = viewModel()
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val updateProfileResult by viewModel.updateProfileResult.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        viewModel.getUserProfile(userId)
    }

    LaunchedEffect(updateProfileResult) {
        if (updateProfileResult is DataResult.Success) {
            scope.launch {
                snackbarHostState.showSnackbar("Profile updated successfully")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF0000),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val result = userProfile) {
                is DataResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF0000))
                    }
                }
                is DataResult.Success -> {
                    UserProfileContent(
                        user = result.data,
                        onUpdateProfile = { updatedUser ->
                            viewModel.updateUserProfile(updatedUser)
                        },
                        isUpdating = updateProfileResult is DataResult.Loading
                    )
                }
                is DataResult.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Error: ${result.error.peekContent()}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.getUserProfile(userId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF0000)
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun UserProfileContent(
    user: User,
    onUpdateProfile: (User) -> Unit,
    isUpdating: Boolean
) {
    var name by remember { mutableStateOf(user.name) }
    var department by remember { mutableStateOf(user.department) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Edit Your Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF0000)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isUpdating,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF0000),
                focusedLabelColor = Color(0xFFFF0000),
                cursorColor = Color(0xFFFF0000)
            )
        )
        
        // Email field (read-only)
        OutlinedTextField(
            value = user.email,
            onValueChange = {},
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.Gray,
                disabledTextColor = Color.DarkGray
            )
        )
        
        // Department field
        OutlinedTextField(
            value = department,
            onValueChange = { department = it },
            label = { Text("Department") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isUpdating,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF0000),
                focusedLabelColor = Color(0xFFFF0000),
                cursorColor = Color(0xFFFF0000)
            )
        )
        
        // Role field (read-only)
        OutlinedTextField(
            value = user.role,
            onValueChange = {},
            label = { Text("Role") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.Gray,
                disabledTextColor = Color.DarkGray
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Save button
        Button(
            onClick = { 
                val updatedUser = user.copy(name = name, department = department)
                onUpdateProfile(updatedUser)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isUpdating && (name != user.name || department != user.department),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000),
                disabledContainerColor = Color.Gray
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Save Changes", fontSize = 16.sp)
            }
        }
    }
}
