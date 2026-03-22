package com.university.campuscare.ui.screens.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.university.campuscare.viewmodel.AuthState
import com.university.campuscare.viewmodel.AuthViewModel
import com.university.campuscare.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementTab(
    adminViewModel: AdminViewModel,
    authViewModel: AuthViewModel
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val staffList by adminViewModel.staffList.collectAsState()

    LaunchedEffect(Unit) {
        adminViewModel.loadStaffMembers()
    }

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
                text = "Staff Management",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFFFF0000),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Staff")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (staffList.isEmpty()) {
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
                        "No staff members yet",
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(staffList) { staff ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = staff.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = staff.email,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                                if (staff.department.isNotEmpty()) {
                                    Text(
                                        text = "Department: ${staff.department}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Surface(
                                color = if (staff.role == "ADMIN") {
                                    Color(0xFFFFEBEB)
                                } else {
                                    Color(0xFFF5F5F5)
                                },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = staff.role,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (staff.role == "ADMIN") {
                                        Color(0xFFFF0000)
                                    } else {
                                        Color.DarkGray
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateStaffDialog(
            authViewModel = authViewModel,
            onDismiss = { showCreateDialog = false },
            onSuccess = {
                showCreateDialog = false
                adminViewModel.loadStaffMembers()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStaffDialog(
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var deptDropdownExpanded by remember { mutableStateOf(false) }
    val departments = listOf("Security", "Facilities", "IT", "Administration", "Housing")

    var selectedRole by remember { mutableStateOf("STAFF") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }
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
        title = { Text("Create Staff Account") },
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
                                contentDescription = if (passwordVisible)
                                    "Hide password"
                                else
                                    "Show password",
                                tint = if (passwordVisible) Color(0xFFFF0000) else Color.Gray
                            )
                        }
                    }
                )

                ExposedDropdownMenuBox(
                    expanded = deptDropdownExpanded,
                    onExpandedChange = { deptDropdownExpanded = !deptDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = department.ifEmpty { "Select Department" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = deptDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = textFieldColors
                    )

                    ExposedDropdownMenu(
                        expanded = deptDropdownExpanded,
                        onDismissRequest = { deptDropdownExpanded = false },
                        containerColor = Color.White
                    ) {
                        departments.forEach { deptOption ->
                            DropdownMenuItem(
                                text = { Text(deptOption) },
                                onClick = {
                                    department = deptOption
                                    deptDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = !roleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        colors = textFieldColors,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false },
                        containerColor = Color.White
                    ) {
                        DropdownMenuItem(
                            text = { Text("STAFF - Can view and respond") },
                            onClick = {
                                selectedRole = "STAFF"
                                roleDropdownExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ADMIN - Full privileges") },
                            onClick = {
                                selectedRole = "ADMIN"
                                roleDropdownExpanded = false
                            }
                        )
                    }
                }

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
                    authViewModel.register(
                        name = name,
                        email = email,
                        password = password,
                        confirmPassword = password,
                        department = department,
                        role = selectedRole
                    )
                },
                enabled = !isCreating &&
                        name.isNotEmpty() &&
                        email.isNotEmpty() &&
                        password.length >= 6 &&
                        department.isNotEmpty(),
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