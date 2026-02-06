package com.university.campuscare.data.repository

import com.university.campuscare.data.model.User
import com.university.campuscare.utils.DataResult

interface UserProfileRepository {
    suspend fun getUserProfile(userId: String): DataResult<User>
    suspend fun updateUserProfile(user: User): DataResult<Unit>
}
