package com.greenstage.data.repository

import com.greenstage.data.model.Event

interface EventRepository {

    suspend fun createEvent(
        event: Event
    ): Result<String>

    suspend fun getPublishedEvents(): Result<List<Event>>

    suspend fun getEvent(
        eventId: String
    ): Result<Event>

    suspend fun getOwnedCommunityEvents(): Result<List<Event>>

    suspend fun updateEvent(
        event: Event
    ): Result<Unit>

    suspend fun deleteEvent(
        eventId: String
    ): Result<Unit>

    suspend fun setPublished(
        eventId: String,
        isPublished: Boolean
    ): Result<Unit>
}
