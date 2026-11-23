package com.greenstage.data.repository

import com.greenstage.data.model.Community

interface CommunityRepository {

    suspend fun getOwnedCommunity(): Result<Community>

    suspend fun updateCommunity(
        communityId: String,
        name: String,
        description: String,
        category: String
    ): Result<Unit>
}
