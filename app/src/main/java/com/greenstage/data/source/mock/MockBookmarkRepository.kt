package com.greenstage.data.source.mock

import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Bookmark
import com.greenstage.data.repository.BookmarkRepository

internal class MockBookmarkRepository(
    private val dataSource: MockDataSource
) : BookmarkRepository {

    override suspend fun addBookmark(
        eventId: String
    ): Result<Unit> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            val event = dataSource.events[eventId]
                ?: error("Event not found.")

            check(event.isPublished) {
                "You cannot bookmark an unpublished event."
            }

            val userBookmarks = dataSource.bookmarks
                .getOrPut(userId) { mutableMapOf() }

            userBookmarks.putIfAbsent(
                eventId,
                Bookmark(
                    eventId = eventId,
                    savedAt = System.currentTimeMillis()
                )
            )

            Unit
        }
    }

    override suspend fun removeBookmark(
        eventId: String
    ): Result<Unit> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            dataSource.bookmarks[userId]?.remove(eventId)
        }
    }

    override suspend fun isBookmarked(
        eventId: String
    ): Result<Boolean> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            dataSource.bookmarks[userId]
                ?.containsKey(eventId) ?: false
        }
    }

    override suspend fun getCurrentUserBookmarks():
            Result<List<Bookmark>> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            dataSource.bookmarks[userId]
                ?.values
                ?.toList()
                ?: emptyList()
        }
    }

    private fun requireStudent(): String {
        val userId = dataSource.currentUserId
            ?: error("No authenticated user.")

        val user = dataSource.users[userId]
            ?: error("User profile not found.")

        check(user.accountType == AccountType.STUDENT) {
            "This operation requires a student account."
        }

        return userId
    }
}
