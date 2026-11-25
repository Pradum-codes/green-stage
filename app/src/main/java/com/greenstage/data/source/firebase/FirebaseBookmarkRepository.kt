package com.greenstage.data.source.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.Bookmark
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.source.firebase.FirestoreMapper.toBookmark
import com.greenstage.data.source.firebase.FirestoreMapper.toTimestamp
import kotlinx.coroutines.tasks.await

class FirebaseBookmarkRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : BookmarkRepository {

    private fun currentUid(): String {
        return firebaseAuth.currentUser?.uid
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: error("No authenticated user.")
    }

    override suspend fun addBookmark(
        eventId: String
    ): Result<Unit> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }
        val uid = currentUid()

        val eventDoc = firestore.collection("events").document(eventId).get().await()
        check(eventDoc.exists()) { "Event not found." }

        val isPublished = eventDoc.getBoolean("isPublished") ?: false
        check(isPublished) { "You cannot bookmark an unpublished event." }

        val bookmarkRef = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .document(eventId)

        val bookmarkData = mapOf(
            "savedAt" to System.currentTimeMillis().toTimestamp()
        )

        bookmarkRef.set(bookmarkData).await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun removeBookmark(
        eventId: String
    ): Result<Unit> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }
        val uid = currentUid()

        val bookmarkRef = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .document(eventId)

        bookmarkRef.delete().await()
        Unit
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun isBookmarked(
        eventId: String
    ): Result<Boolean> = runCatching {
        require(eventId.isNotBlank()) { "Event ID cannot be empty." }
        val uid = currentUid()

        val doc = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .document(eventId)
            .get()
            .await()

        doc.exists()
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }

    override suspend fun getCurrentUserBookmarks():
            Result<List<Bookmark>> = runCatching {
        val uid = currentUid()

        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("bookmarks")
            .get()
            .await()

        snapshot.documents.mapNotNull { it.toBookmark(it.id) }
    }.recoverCatching { throwable ->
        throw Exception(FirebaseErrorMapper.map(throwable))
    }
}
