package com.greenstage.data.source.mock

import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Community
import com.greenstage.data.model.User
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.AuthState
import com.greenstage.data.repository.CommunityRegistrationResult
import com.greenstage.data.source.mock.MockDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

internal class MockAuthRepository(
    private val dataSource: MockDataSource
) : AuthRepository {

    override suspend fun registerStudent(
        name: String,
        email: String,
        password: String
    ): Result<User> = runCatching {

        validateCredentials(name, email, password)

        synchronized(dataSource.lock) {
            val normalizedEmail = normalizeEmail(email)

            check(!dataSource.passwordsByEmail.containsKey(normalizedEmail)) {
                "An account with this email already exists."
            }

            val now = System.currentTimeMillis()

            val user = User(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                email = normalizedEmail,
                accountType = AccountType.STUDENT,
                interests = emptyList(),
                createdAt = now,
                updatedAt = now
            )

            dataSource.users[user.id] = user
            dataSource.passwordsByEmail[normalizedEmail] = password

            dataSource.currentUserId = user.id
            dataSource.authState.value = AuthState.Authenticated(user)

            user
        }
    }

    override suspend fun registerCommunityAdmin(
        name: String,
        email: String,
        password: String,
        communityName: String,
        communityDescription: String,
        communityCategory: String
    ): Result<CommunityRegistrationResult> = runCatching {

        validateCredentials(name, email, password)

        require(communityName.isNotBlank()) {
            "Community name cannot be empty."
        }

        require(communityDescription.isNotBlank()) {
            "Community description cannot be empty."
        }

        require(communityCategory.isNotBlank()) {
            "Community category cannot be empty."
        }

        synchronized(dataSource.lock) {
            val normalizedEmail = normalizeEmail(email)

            check(!dataSource.passwordsByEmail.containsKey(normalizedEmail)) {
                "An account with this email already exists."
            }

            val now = System.currentTimeMillis()

            val user = User(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                email = normalizedEmail,
                accountType = AccountType.ADMIN,
                interests = emptyList(),
                createdAt = now,
                updatedAt = now
            )

            val community = Community(
                id = UUID.randomUUID().toString(),
                name = communityName.trim(),
                description = communityDescription.trim(),
                category = communityCategory.trim(),
                createdBy = user.id,
                createdAt = now,
                updatedAt = now
            )

            // Store both records together.
            dataSource.users[user.id] = user
            dataSource.communities[community.id] = community
            dataSource.passwordsByEmail[normalizedEmail] = password

            dataSource.currentUserId = user.id
            dataSource.authState.value = AuthState.Authenticated(user)

            CommunityRegistrationResult(
                user = user,
                community = community
            )
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<User> = runCatching {

        require(email.isNotBlank()) {
            "Email cannot be empty."
        }

        require(password.isNotBlank()) {
            "Password cannot be empty."
        }

        synchronized(dataSource.lock) {
            val normalizedEmail = normalizeEmail(email)

            val storedPassword =
                dataSource.passwordsByEmail[normalizedEmail]

            check(storedPassword == password) {
                "Invalid email or password."
            }

            val user = dataSource.users.values.firstOrNull {
                it.email == normalizedEmail
            } ?: error("User profile not found.")

            dataSource.currentUserId = user.id
            dataSource.authState.value = AuthState.Authenticated(user)

            user
        }
    }

    override suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit> = runCatching {

        require(email.isNotBlank()) {
            "Email cannot be empty."
        }

        synchronized(dataSource.lock) {
            val normalizedEmail = normalizeEmail(email)

            check(dataSource.passwordsByEmail.containsKey(normalizedEmail)) {
                "No account exists with this email."
            }
        }

        // Mock implementation: no actual email is sent.
        Unit
    }

    override suspend fun logout(): Result<Unit> = runCatching {

        synchronized(dataSource.lock) {
            dataSource.currentUserId = null
            dataSource.authState.value = AuthState.SignedOut
        }
    }

    override fun observeAuthState(): Flow<AuthState> {
        return dataSource.authState.asStateFlow()
    }

    private fun validateCredentials(
        name: String,
        email: String,
        password: String
    ) {
        require(name.isNotBlank()) {
            "Name cannot be empty."
        }

        require(email.isNotBlank() && email.contains("@")) {
            "Enter a valid email address."
        }

        require(password.length >= 6) {
            "Password must contain at least 6 characters."
        }
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }
}
