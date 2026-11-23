package com.greenstage.data.source.mock

import com.greenstage.data.model.AccountType
import com.greenstage.data.repository.CommunityRepository

internal class MockCommunityRepository(
    private val dataSource: MockDataSource
) : CommunityRepository {

    override suspend fun getOwnedCommunity() = runCatching {
        synchronized(dataSource.lock) {
            val userId = dataSource.currentUserId
                ?: error("No authenticated user.")

            val user = dataSource.users[userId]
                ?: error("User profile not found.")

            check(user.accountType == AccountType.ADMIN) {
                "Only administrators can access their community."
            }

            dataSource.communities.values.firstOrNull {
                it.createdBy == userId
            } ?: error("Owned community not found.")
        }
    }

    override suspend fun updateCommunity(
        communityId: String,
        name: String,
        description: String,
        category: String
    ) = runCatching {
        require(name.isNotBlank()) {
            "Community name cannot be empty."
        }

        require(description.isNotBlank()) {
            "Community description cannot be empty."
        }

        require(category.isNotBlank()) {
            "Community category cannot be empty."
        }

        synchronized(dataSource.lock) {
            val userId = dataSource.currentUserId
                ?: error("No authenticated user.")

            val user = dataSource.users[userId]
                ?: error("User profile not found.")

            check(user.accountType == AccountType.ADMIN) {
                "Only administrators can update communities."
            }

            val community = dataSource.communities[communityId]
                ?: error("Community not found.")

            check(community.createdBy == userId) {
                "You do not own this community."
            }

            val updatedCommunity = community.copy(
                name = name.trim(),
                description = description.trim(),
                category = category.trim(),
                updatedAt = System.currentTimeMillis()
            )

            dataSource.communities[communityId] = updatedCommunity
        }
    }
}
