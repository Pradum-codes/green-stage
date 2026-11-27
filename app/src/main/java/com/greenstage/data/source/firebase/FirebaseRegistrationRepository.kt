package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.Registration
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.data.source.firebase.FirestoreMapper.toRegistration
import com.greenstage.data.source.firebase.FirestoreMapper.toTimestamp
import kotlinx.coroutines.tasks.await

class FirebaseRegistrationRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : RegistrationRepository {

    private fun currentUid(): String {
        return firebaseAuth.currentUser?.uid
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: error("No authenticated user.")
    }

    override suspend fun registerForEvent(
        eventId: String
    ): Result<Unit> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }
        val uid = currentUid()

        val regRef = firestore.collection("users")
            .document(uid)
            .collection("registrations")
            .document(eventId)

        val regData = mapOf(
            "eventId" to eventId,
            "registeredAt" to System.currentTimeMillis().toTimestamp()
        )

        regRef.set(regData).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun cancelRegistration(
        eventId: String
    ): Result<Unit> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }
        val uid = currentUid()

        val regRef = firestore.collection("users")
            .document(uid)
            .collection("registrations")
            .document(eventId)

        regRef.delete().await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun isRegistered(
        eventId: String
    ): Result<Boolean> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }
        val uid = currentUid()

        val doc = firestore.collection("users")
            .document(uid)
            .collection("registrations")
            .document(eventId)
            .get()
            .await()

        doc.exists()
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun getCurrentUserRegistrations():
            Result<List<Registration>> = runCatching {
        val uid = currentUid()

        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("registrations")
            .get()
            .await()

        snapshot.documents.mapNotNull { it.toRegistration(it.id) }
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun getRegistrationCount(
        eventId: String
    ): Result<Long> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }

        val aggregateQuery = firestore.collectionGroup("registrations")
            .whereEqualTo("eventId", eventId)
            .count()

        val snapshot = aggregateQuery.get(AggregateSource.SERVER).await()
        snapshot.count
    }.recoverCatching {
        0L
    }

    override suspend fun getEventParticipants(
        eventId: String
    ): Result<List<com.greenstage.ui.events.ParticipantInfo>> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }

        val snapshot = firestore.collectionGroup("registrations")
            .whereEqualTo("eventId", eventId)
            .get()
            .await()

        val list = mutableListOf<com.greenstage.ui.events.ParticipantInfo>()
        for (doc in snapshot.documents) {
            val parentUserDoc = doc.reference.parent.parent?.get()?.await()
            val name = parentUserDoc?.getString("name") ?: "Student"
            val email = parentUserDoc?.getString("email") ?: ""
            val regAt = doc.getTimestamp("registeredAt")?.toDate()?.time ?: System.currentTimeMillis()
            list.add(
                com.greenstage.ui.events.ParticipantInfo(
                    userId = parentUserDoc?.id ?: doc.id,
                    name = name,
                    email = email,
                    registeredAt = regAt
                )
            )
        }
        list
    }.recoverCatching {
        emptyList()
    }
}
