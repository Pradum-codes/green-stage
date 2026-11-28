package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.greenstage.data.model.Registration
import com.greenstage.data.model.ParticipantInfo
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
            ?: error("No authenticated user.")
    }

    private suspend fun verifyEventOwnership(
        eventId: String
    ): String {
        val uid = currentUid()

        val event = firestore.collection("events")
            .document(eventId)
            .get(Source.SERVER)
            .await()

        require(event.exists()) {
            "Event does not exist."
        }

        require(
            event.getString("communityId") == uid &&
            event.getString("createdBy") == uid
        ) {
            "You do not own this event."
        }

        return uid
    }

    override suspend fun registerForEvent(
        eventId: String
    ): Result<Unit> = runCatching {

        require(eventId.isNotBlank()) {
            "Event ID cannot be empty."
        }

        val uid = currentUid()

        val userRef = firestore.collection("users")
            .document(uid)

        val eventRef = firestore.collection("events")
            .document(eventId)

        val regRef = userRef
            .collection("registrations")
            .document(eventId)

        firestore.runTransaction { transaction ->

            val user = transaction.get(userRef)
            val event = transaction.get(eventRef)
            val registration = transaction.get(regRef)

            require(user.exists()) {
                "Student profile does not exist."
            }

            require(
                user.getString("accountType") == "student"
            ) {
                "Only students can register for events."
            }

            require(event.exists()) {
                "Event does not exist."
            }

            require(
                event.getBoolean("isPublished") == true
            ) {
                "Event is not published."
            }

            require(!registration.exists()) {
                "You are already registered for this event."
            }

            val communityId =
                event.getString("communityId")
                    ?: error("Invalid event community.")

            val studentName =
                user.getString("name")
                    ?: error("Student name is missing.")

            val studentEmail =
                user.getString("email")
                    ?: error("Student email is missing.")

            val regData = mapOf(
                "eventId" to eventId,
                "communityId" to communityId,
                "studentName" to studentName,
                "studentEmail" to studentEmail,
                "registeredAt" to
                    System.currentTimeMillis().toTimestamp()
            )

            transaction.set(regRef, regData)

            Unit
        }.await()

        Unit

    }.recoverCatching { throwable ->
        throw Exception(
            FirebaseErrorMapper.map(throwable),
            throwable
        )
    }

    override suspend fun cancelRegistration(
        eventId: String
    ): Result<Unit> = runCatching {

        require(eventId.isNotBlank()) {
            "Event ID cannot be empty."
        }

        firestore.collection("users")
            .document(currentUid())
            .collection("registrations")
            .document(eventId)
            .delete()
            .await()

        Unit

    }.recoverCatching { throwable ->
        throw Exception(
            FirebaseErrorMapper.map(throwable),
            throwable
        )
    }

    override suspend fun isRegistered(
        eventId: String
    ): Result<Boolean> = runCatching {

        require(eventId.isNotBlank()) {
            "Event ID cannot be empty."
        }

        firestore.collection("users")
            .document(currentUid())
            .collection("registrations")
            .document(eventId)
            .get()
            .await()
            .exists()

    }.recoverCatching { throwable ->
        throw Exception(
            FirebaseErrorMapper.map(throwable),
            throwable
        )
    }

    override suspend fun getCurrentUserRegistrations():
        Result<List<Registration>> = runCatching {

        val snapshot = firestore.collection("users")
            .document(currentUid())
            .collection("registrations")
            .get()
            .await()

        snapshot.documents.mapNotNull {
            it.toRegistration(it.id)
        }

    }.recoverCatching { throwable ->
        throw Exception(
            FirebaseErrorMapper.map(throwable),
            throwable
        )
    }

    override suspend fun getRegistrationCount(
        eventId: String
    ): Result<Long> = runCatching {

        require(eventId.isNotBlank()) {
            "Event ID cannot be empty."
        }

        val adminUid = verifyEventOwnership(eventId)

        val snapshot = firestore
            .collectionGroup("registrations")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("communityId", adminUid)
            .count()
            .get(AggregateSource.SERVER)
            .await()

        snapshot.count

    }.recoverCatching { throwable ->
        throw Exception(
            FirebaseErrorMapper.map(throwable),
            throwable
        )
    }

    override suspend fun getEventParticipants(
        eventId: String
    ): Result<List<ParticipantInfo>> = runCatching {

        require(eventId.isNotBlank()) {
            "Event ID cannot be empty."
        }

        val adminUid = verifyEventOwnership(eventId)

        val snapshot = firestore
            .collectionGroup("registrations")
            .whereEqualTo("eventId", eventId)
            .whereEqualTo("communityId", adminUid)
            .get(Source.SERVER)
            .await()

        snapshot.documents.map { doc ->

            val studentUid =
                doc.reference.parent.parent?.id
                    ?: error("Invalid registration path.")

            val name = doc.getString("studentName")
                ?: error("Student name is missing.")

            val email = doc.getString("studentEmail")
                ?: error("Student email is missing.")

            val registeredAt =
                doc.getTimestamp("registeredAt")
                    ?.toDate()?.time
                    ?: error("Registration timestamp is missing.")

            ParticipantInfo(
                userId = studentUid,
                name = name,
                email = email,
                registeredAt = registeredAt
            )

        }.sortedByDescending { it.registeredAt }

    }.recoverCatching { throwable ->
        throw Exception(
            FirebaseErrorMapper.map(throwable),
            throwable
        )
    }
}
