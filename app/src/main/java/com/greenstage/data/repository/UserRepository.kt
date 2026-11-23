package com.greenstage.data.repository

import com.greenstage.data.model.User

interface UserRepository {

    suspend fun getCurrentUser(): Result<User>

    suspend fun updateProfile(
        name: String,
        interests: List<String>
    ): Result<Unit>
}
