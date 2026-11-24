package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.Community
import com.greenstage.data.repository.CommunityRepository
import kotlinx.coroutines.tasks.await

class FirebaseCommunityRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CommunityRepository {

    override suspend fun getOwnedCommunity(): Result<Community> = runCatching {
        val uid = firebaseAuth.currentUser?.uid
            ?: error("No authenticated user.")

        val snapshot = firestore.collection("communities").document(uid).get().await()
        if (!snapshot.exists()) {
            error("Owned community not found.")
        }

        val name = snapshot.getString("name") ?: error("Community name missing.")
        val description = snapshot.getString("description") ?: error("Community description missing.")
        val category = snapshot.getString("category") ?: error("Community category missing.")
        val createdBy = snapshot.getString("createdBy") ?: error("Community creator ID missing.")

        check(createdBy == uid) { "Community owner mismatch." }

        val createdAt = snapshot.getLong("createdAt")
            ?: snapshot.getTimestamp("createdAt")?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()
        val updatedAt = snapshot.getLong("updatedAt")
            ?: snapshot.getTimestamp("updatedAt")?.toInstant()?.toEpochMilli()
            ?: System.currentTimeMillis()

        Community(
            id = uid,
            name = name,
            description = description,
            category = category,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun updateCommunity(
        communityId: String,
        name: String,
        description: String,
        category: String
    ): Result<Unit> = runCatching {
        require(name.isNotBlank()) { "Community name cannot be empty." }
        require(description.isNotBlank()) { "Community description cannot be empty." }
        require(category.isNotBlank()) { "Community category cannot be empty." }

        val uid = firebaseAuth.currentUser?.uid
            ?: error("No authenticated user.")

        check(communityId == uid) { "You can only update your owned community." }

        val docRef = firestore.collection("communities").document(uid)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            error("Community not found.")
        }

        val createdBy = snapshot.getString("createdBy")
        check(createdBy == uid) { "You do not own this community." }

        val updates = mapOf(
            "name" to name.trim(),
            "description" to description.trim(),
            "category" to category.trim(),
            "updatedAt" to System.currentTimeMillis()
        )

        docRef.update(updates).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }
}
