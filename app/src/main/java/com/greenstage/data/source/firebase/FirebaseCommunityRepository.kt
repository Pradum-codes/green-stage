package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.Community
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.source.firebase.FirestoreMapper.toCommunity
import com.greenstage.data.source.firebase.FirestoreMapper.toTimestamp
import kotlinx.coroutines.tasks.await

class FirebaseCommunityRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : CommunityRepository {

    private fun currentUid(): String {
        return firebaseAuth.currentUser?.uid
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: error("No authenticated user.")
    }

    override suspend fun getOwnedCommunity(): Result<Community> = runCatching {
        val uid = currentUid()

        val snapshot = firestore.collection("communities").document(uid).get().await()
        val community = snapshot.toCommunity(uid) ?: error("Owned community not found.")

        check(community.createdBy == uid) { "Community owner mismatch." }

        community
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

        val uid = currentUid()

        check(communityId == uid) { "You can only update your owned community." }

        val docRef = firestore.collection("communities").document(uid)
        val snapshot = docRef.get().await()

        val community = snapshot.toCommunity(uid) ?: error("Community not found.")

        check(community.createdBy == uid) { "You do not own this community." }

        val updates = mapOf(
            "name" to name.trim(),
            "description" to description.trim(),
            "category" to category.trim(),
            "updatedAt" to System.currentTimeMillis().toTimestamp()
        )

        docRef.update(updates).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }
}
