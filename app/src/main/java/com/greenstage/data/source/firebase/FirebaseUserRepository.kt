package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.User
import com.greenstage.data.repository.UserRepository
import com.greenstage.data.source.firebase.FirestoreMapper.toTimestamp
import com.greenstage.data.source.firebase.FirestoreMapper.toUser
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserRepository {

    private fun currentUid(): String {
        return firebaseAuth.currentUser?.uid
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: error("No authenticated user.")
    }

    override suspend fun getCurrentUser(): Result<User> = runCatching {
        val uid = currentUid()
        val doc = firestore.collection("users").document(uid).get().await()
        val user = doc.toUser(uid) ?: error("User profile not found.")
        user
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun updateProfile(
        name: String,
        interests: List<String>
    ): Result<Unit> = runCatching {
        require(name.isNotBlank()) { "Name cannot be empty." }

        val uid = currentUid()
        val docRef = firestore.collection("users").document(uid)
        val doc = docRef.get().await()
        if (!doc.exists()) {
            error("User profile not found.")
        }

        val updates = mapOf(
            "name" to name.trim(),
            "interests" to interests.map { it.trim() }.filter { it.isNotBlank() },
            "updatedAt" to System.currentTimeMillis().toTimestamp()
        )

        docRef.update(updates).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }
}
