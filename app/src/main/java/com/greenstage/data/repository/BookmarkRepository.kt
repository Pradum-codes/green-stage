package com.greenstage.data.repository

import com.greenstage.data.model.Bookmark

interface BookmarkRepository {

    suspend fun addBookmark(
        eventId: String
    ): Result<Unit>

    suspend fun removeBookmark(
        eventId: String
    ): Result<Unit>

    suspend fun isBookmarked(
        eventId: String
    ): Result<Boolean>

    suspend fun getCurrentUserBookmarks():
            Result<List<Bookmark>>
}
