package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.Event
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.source.firebase.FirestoreMapper.toEvent
import com.greenstage.data.source.firebase.FirestoreMapper.toTimestamp
import kotlinx.coroutines.tasks.await

class FirebaseEventRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : EventRepository {

    private fun currentUid(): String {
        return firebaseAuth.currentUser?.uid
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: error("No authenticated user.")
    }

    override suspend fun createEvent(event: Event): Result<String> = runCatching {
        val uid = currentUid()
        require(event.title.isNotBlank()) { "Event title cannot be empty." }
        require(event.description.isNotBlank()) { "Event description cannot be empty." }
        require(event.category.isNotBlank()) { "Event category cannot be empty." }
        require(event.venue.isNotBlank()) { "Event venue cannot be empty." }
        require(event.endAt > event.startAt) { "Event end time must be after start time." }
        check(event.communityId == uid) { "Event must belong to your community." }
        check(event.createdBy == uid) { "Event creator does not match authenticated user." }

        val eventMap = mapOf(
            "title" to event.title.trim(),
            "description" to event.description.trim(),
            "category" to event.category.trim(),
            "venue" to event.venue.trim(),
            "startAt" to event.startAt.toTimestamp(),
            "endAt" to event.endAt.toTimestamp(),
            "imageUrl" to event.imageUrl,
            "communityId" to event.communityId,
            "createdBy" to event.createdBy,
            "createdAt" to event.createdAt.toTimestamp(),
            "updatedAt" to event.updatedAt.toTimestamp(),
            "isPublished" to event.isPublished
        )

        firestore.collection("events").document(event.id).set(eventMap).await()
        event.id
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun getPublishedEvents(): Result<List<Event>> = runCatching {
        val snapshot = firestore.collection("events")
            .whereEqualTo("isPublished", true)
            .get()
            .await()

        snapshot.documents.mapNotNull { it.toEvent(it.id) }
            .sortedBy { it.startAt }
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun getEvent(eventId: String): Result<Event> = runCatching {
        val uid = currentUid()
        val doc = firestore.collection("events").document(eventId).get().await()
        val event = doc.toEvent(eventId) ?: error("Event not found.")

        val isOwner = event.createdBy == uid
        check(event.isPublished || isOwner) { "You cannot access this unpublished event." }

        event
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun getOwnedCommunityEvents(): Result<List<Event>> = runCatching {
        val uid = currentUid()
        val snapshot = firestore.collection("events")
            .whereEqualTo("communityId", uid)
            .get()
            .await()

        snapshot.documents.mapNotNull { it.toEvent(it.id) }
            .sortedBy { it.startAt }
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
        val uid = currentUid()
        val docRef = firestore.collection("events").document(event.id)
        val doc = docRef.get().await()
        val existing = doc.toEvent(event.id) ?: error("Event not found.")

        check(existing.createdBy == uid) { "You do not own this event." }
        check(event.communityId == uid) { "Event community cannot be changed." }

        val updates = mapOf(
            "title" to event.title.trim(),
            "description" to event.description.trim(),
            "category" to event.category.trim(),
            "venue" to event.venue.trim(),
            "startAt" to event.startAt.toTimestamp(),
            "endAt" to event.endAt.toTimestamp(),
            "imageUrl" to event.imageUrl,
            "isPublished" to event.isPublished,
            "updatedAt" to System.currentTimeMillis().toTimestamp()
        )

        docRef.update(updates).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        val uid = currentUid()
        val docRef = firestore.collection("events").document(eventId)
        val doc = docRef.get().await()
        val existing = doc.toEvent(eventId) ?: error("Event not found.")

        check(existing.createdBy == uid) { "You do not own this event." }

        docRef.delete().await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun setPublished(eventId: String, isPublished: Boolean): Result<Unit> = runCatching {
        val uid = currentUid()
        val docRef = firestore.collection("events").document(eventId)
        val doc = docRef.get().await()
        val existing = doc.toEvent(eventId) ?: error("Event not found.")

        check(existing.createdBy == uid) { "You do not own this event." }

        docRef.update(mapOf("isPublished" to isPublished, "updatedAt" to System.currentTimeMillis().toTimestamp())).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }
}
