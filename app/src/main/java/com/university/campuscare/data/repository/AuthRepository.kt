package com.university.campuscare.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.university.campuscare.data.local.UserPreference
import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult
import com.university.campuscare.utils.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface AuthRepository {
    suspend fun register(
        name: String,
        email: String,
        password: String,
        department: String
    ): Flow<DataResult<User>>

    suspend fun login(
        email: String,
        password: String
    ): Flow<DataResult<User>>

    suspend fun logout(): Flow<DataResult<Unit>>

    suspend fun resetPassword(email: String): Flow<DataResult<Unit>>

    suspend fun getCurrentUser(): Flow<DataResult<User>>
}

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userPreference: UserPreference
) : AuthRepository {

    // Demo mode flag - set to true to bypass Firebase for testing
    private val isDemoMode = false

    // Demo accounts for testing
    private val demoAccounts = mapOf(
        "student@campus.edu" to DemoUser("student123", "Student User", "STUDENT", "Computer Science"),
        "admin@campus.edu" to DemoUser("admin123", "Admin User", "ADMIN", "Administration"),
        "cc_admin@gmail.com" to DemoUser("admin123", "Admin User", "ADMIN", "Administration"),
        "staff@campus.edu" to DemoUser("staff123", "Staff Member", "STAFF", "Facilities")
    )

    private data class DemoUser(
        val password: String,
        val name: String,
        val role: String,
        val department: String
    )

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        department: String
    ): Flow<DataResult<User>> = flow {
        if (name.isBlank()) {
            emit(DataResult.Error(Event("Name cannot be empty")))
            return@flow
        }
        if (email.isBlank() || !email.contains("@")) {
            emit(DataResult.Error(Event("Invalid email address")))
            return@flow
        }
        if (password.length < 6) {
            emit(DataResult.Error(Event("Password must be at least 6 characters")))
            return@flow
        }

        emit(DataResult.Loading)

        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw Exception("Failed to create user account")

            val user = User(
                userId = firebaseUser.uid,
                name = name,
                email = email,
                role = "STUDENT",
                department = department,
                profilePhotoUrl = ""
            )

            // Save to Firestore
            val userMap = hashMapOf(
                "userId" to user.userId,
                "name" to user.name,
                "email" to user.email,
                "role" to user.role,
                "department" to user.department,
                "profilePhotoUrl" to user.profilePhotoUrl
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(userMap)
                .await()

            userPreference.saveUserSession(
                userId = user.userId,
                userName = user.name,
                userEmail = user.email,
                userRole = user.role,
                department = user.department,
                profilePhotoUrl = user.profilePhotoUrl
            )

            emit(DataResult.Success(user))

        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("API key not valid") == true ||
                e.message?.contains("INVALID_API_KEY") == true ->
                    "Firebase not configured. Please add valid google-services.json file. See FIREBASE_SETUP.md"
                e.message?.contains("email address is already in use") == true ->
                    "This email is already registered"
                e.message?.contains("network") == true ->
                    "Network error. Please check your connection"
                else -> "Registration failed: ${e.message}"
            }
            emit(DataResult.Error(Event(errorMessage)))
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Flow<DataResult<User>> = flow {
        if (email.isBlank() || password.isBlank()) {
            emit(DataResult.Error(Event("Email and password cannot be empty")))
            return@flow
        }

        emit(DataResult.Loading)

        try {
            if (isDemoMode) {
                kotlinx.coroutines.delay(1000)

                val demoUser = demoAccounts[email.lowercase()]
                if (demoUser == null || demoUser.password != password) {
                    emit(DataResult.Error(Event("Invalid email or password")))
                    return@flow
                }

                val user = User(
                    userId = "demo_${demoUser.role.lowercase()}_001",
                    name = demoUser.name,
                    email = email,
                    role = demoUser.role,
                    department = demoUser.department,
                    profilePhotoUrl = ""
                )

                userPreference.saveUserSession(
                    userId = user.userId,
                    userName = user.name,
                    userEmail = user.email,
                    userRole = user.role,
                    department = user.department,
                    profilePhotoUrl = user.profilePhotoUrl
                )

                emit(DataResult.Success(user))
                return@flow
            }

            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw Exception("Login failed")

            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                throw Exception("User profile not found")
            }

            val user = User(
                userId = userDoc.getString("userId") ?: "",
                name = userDoc.getString("name") ?: "",
                email = userDoc.getString("email") ?: "",
                role = userDoc.getString("role") ?: "STUDENT",
                department = userDoc.getString("department") ?: "",
                profilePhotoUrl = userDoc.getString("profilePhotoUrl") ?: ""
            )

            userPreference.saveUserSession(
                userId = user.userId,
                userName = user.name,
                userEmail = user.email,
                userRole = user.role,
                department = user.department,
                profilePhotoUrl = user.profilePhotoUrl
            )

            emit(DataResult.Success(user))

        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("API key not valid") == true ||
                e.message?.contains("INVALID_API_KEY") == true ->
                    "Firebase not configured. Please add valid google-services.json file. See FIREBASE_SETUP.md"
                e.message?.contains("no user record") == true ||
                e.message?.contains("password is invalid") == true ->
                    "Invalid email or password"
                e.message?.contains("network") == true ->
                    "Network error. Please check your connection"
                else -> "Login failed: ${e.message}"
            }
            emit(DataResult.Error(Event(errorMessage)))
        }
    }

    override suspend fun logout(): Flow<DataResult<Unit>> = flow {
        try {
            firebaseAuth.signOut()
            userPreference.logout()
            emit(DataResult.Success(Unit))
        } catch (e: Exception) {
            emit(DataResult.Error(Event("Logout failed: ${e.message}")))
        }
    }

    override suspend fun resetPassword(email: String): Flow<DataResult<Unit>> = flow {
        if (email.isBlank() || !email.contains("@")) {
            emit(DataResult.Error(Event("Invalid email address")))
            return@flow
        }

        emit(DataResult.Loading)

        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(DataResult.Success(Unit))
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("no user record") == true ->
                    "No account found with this email"
                e.message?.contains("network") == true ->
                    "Network error. Please check your connection"
                else -> "Failed to send reset email: ${e.message}"
            }
            emit(DataResult.Error(Event(errorMessage)))
        }
    }

    override suspend fun getCurrentUser(): Flow<DataResult<User>> = flow {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser == null) {
            emit(DataResult.Error(Event("No user logged in")))
            return@flow
        }

        try {
            val userDoc = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                emit(DataResult.Error(Event("User profile not found")))
                return@flow
            }

            val user = User(
                userId = userDoc.getString("userId") ?: "",
                name = userDoc.getString("name") ?: "",
                email = userDoc.getString("email") ?: "",
                role = userDoc.getString("role") ?: "STUDENT",
                department = userDoc.getString("department") ?: "",
                profilePhotoUrl = userDoc.getString("profilePhotoUrl") ?: ""
            )

            emit(DataResult.Success(user))

        } catch (e: Exception) {
            emit(DataResult.Error(Event("Failed to get user: ${e.message}")))
        }
    }
}