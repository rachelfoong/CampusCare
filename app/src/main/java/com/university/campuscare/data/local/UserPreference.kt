package com.university.campuscare.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "campus_care_preferences")

class UserPreference(private val context: Context) {

    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
        private val USER_DEPARTMENT_KEY = stringPreferencesKey("user_department")
        private val PROFILE_PHOTO_URL_KEY = stringPreferencesKey("profile_photo_url")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
    }

    // Save user session
    suspend fun saveUserSession(
        userId: String,
        userName: String,
        userEmail: String,
        userRole: String,
        department: String = "",
        profilePhotoUrl: String = ""
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_NAME_KEY] = userName
            preferences[USER_EMAIL_KEY] = userEmail
            preferences[USER_ROLE_KEY] = userRole
            preferences[USER_DEPARTMENT_KEY] = department
            preferences[PROFILE_PHOTO_URL_KEY] = profilePhotoUrl
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }

    // Individual setters
    suspend fun setUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    val userIdPreference: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ID_KEY]
        }

    suspend fun setUserName(userName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = userName
        }
    }

    val userNamePreference: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY]
        }

    suspend fun setUserEmail(userEmail: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = userEmail
        }
    }

    val userEmailPreference: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_EMAIL_KEY]
        }

    suspend fun setUserRole(userRole: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ROLE_KEY] = userRole
        }
    }

    val userRolePreference: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_ROLE_KEY]
        }

    suspend fun setUserDepartment(department: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_DEPARTMENT_KEY] = department
        }
    }

    val userDepartmentPreference: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_DEPARTMENT_KEY]
        }

    suspend fun setProfilePhotoUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[PROFILE_PHOTO_URL_KEY] = url
        }
    }

    val profilePhotoUrlPreference: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PROFILE_PHOTO_URL_KEY]
        }

    val isLoggedInPreference: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_LOGGED_IN_KEY] ?: false
        }

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = isFirstLaunch
        }
    }

    val isFirstLaunchPreference: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[FIRST_LAUNCH_KEY] ?: true
        }

    // Logout - clear all user data
    suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = ""
            preferences[USER_NAME_KEY] = ""
            preferences[USER_EMAIL_KEY] = ""
            preferences[USER_ROLE_KEY] = ""
            preferences[USER_DEPARTMENT_KEY] = ""
            preferences[PROFILE_PHOTO_URL_KEY] = ""
            preferences[IS_LOGGED_IN_KEY] = false
        }
    }
}
