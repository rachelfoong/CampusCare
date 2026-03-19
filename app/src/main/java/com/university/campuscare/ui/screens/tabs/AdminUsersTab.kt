package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.campuscare.viewmodel.AdminViewModel
import com.university.campuscare.viewmodel.AuthState
import com.university.campuscare.viewmodel.AuthViewModel

@Composable
fun AdminUsersTab(
    viewModel: AdminViewModel,
    authViewModel: AuthViewModel
) {
    val allUsers by viewModel.allUsers.collectAsState()
    val allIssues by viewModel.allIssues.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

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

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFFFF0000),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (studentUsers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No users found",
                        color = Color.Gray
                    )
                }
            }
        } else {
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

    if (showCreateDialog) {
        CreateUserDialog(
            authViewModel = authViewModel,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                viewModel.loadAllUsers()
            }
        )
    }
}

@Composable
fun UserCard(name: String, email: String, reportsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val authState by authViewModel.authState.collectAsState()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFFF0000),
        focusedLabelColor = Color(0xFFFF0000),
        cursorColor = Color(0xFFFF0000)
    )

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                if (isCreating) {
                    authViewModel.clearError()
                    onSuccess()
                    isCreating = false
                }
            }
            is AuthState.Error -> {
                if (isCreating) {
                    errorMessage = (authState as AuthState.Error).message
                    isCreating = false
                }
            }
            else -> {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isCreating) {
                authViewModel.clearError()
                onDismiss()
            }
        },
        containerColor = Color.White,
        title = { Text("Create Student Account") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = if (passwordVisible) Color(0xFFFF0000) else Color.Gray
                            )
                        }
                    }
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF0000),
                        fontSize = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorMessage = ""
                    isCreating = true
                    // Role is strictly STUDENT for the Users tab
                    authViewModel.register(
                        name = name,
                        email = email,
                        password = password,
                        confirmPassword = password,
                        department = "", // Students don't have a department in this context
                        role = "STUDENT"
                    )
                },
                enabled = !isCreating && name.isNotEmpty() && email.isNotEmpty() && password.length >= 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    authViewModel.clearError()
                    onDismiss()
                },
                enabled = !isCreating,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Cancel")
            }
        }
    )
}