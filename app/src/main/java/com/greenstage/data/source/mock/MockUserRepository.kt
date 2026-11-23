package com.greenstage.data.source.mock

import com.greenstage.data.repository.UserRepository

internal class MockUserRepository(
    private val dataSource: MockDataSource
) : UserRepository {

    override suspend fun getCurrentUser() = runCatching {
        synchronized(dataSource.lock) {
            val userId = dataSource.currentUserId
                ?: error("No authenticated user.")

            dataSource.users[userId]
                ?: error("User profile not found.")
        }
    }

    override suspend fun updateProfile(
        name: String,
        interests: List<String>
    ) = runCatching {
        require(name.isNotBlank()) {
            "Name cannot be empty."
        }

        synchronized(dataSource.lock) {
            val userId = dataSource.currentUserId
                ?: error("No authenticated user.")

            val user = dataSource.users[userId]
                ?: error("User profile not found.")

            val updatedUser = user.copy(
                name = name.trim(),
                interests = interests
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct(),
                updatedAt = System.currentTimeMillis()
            )

            dataSource.users[userId] = updatedUser

            // Keep the observable authentication state in sync.
            dataSource.authState.value =
                com.greenstage.data.repository.AuthState.Authenticated(
                    updatedUser
                )
        }
    }
}
