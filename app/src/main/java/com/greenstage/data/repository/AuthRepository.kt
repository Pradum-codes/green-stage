package com.greenstage.data.repository

import com.greenstage.data.model.Community
import com.greenstage.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun registerStudent(
        name: String,
        email: String,
        password: String
    ): Result<User>

    suspend fun registerCommunityAdmin(
        name: String,
        email: String,
        password: String,
        communityName: String,
        communityDescription: String,
        communityCategory: String
    ): Result<CommunityRegistrationResult>

    suspend fun login(
        email: String,
        password: String
    ): Result<User>

    suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit>

    suspend fun logout(): Result<Unit>

    fun observeAuthState(): Flow<AuthState>
}

data class CommunityRegistrationResult(
    val user: User,
    val community: Community
)
