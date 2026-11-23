package com.greenstage.data.source.mock

import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Event
import com.greenstage.data.repository.EventRepository

internal class MockEventRepository(
    private val dataSource: MockDataSource
) : EventRepository {

    override suspend fun createEvent(
        event: Event
    ): Result<String> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireAdmin()

            require(event.title.isNotBlank()) {
                "Event title cannot be empty."
            }

            require(event.description.isNotBlank()) {
                "Event description cannot be empty."
            }

            require(event.category.isNotBlank()) {
                "Event category cannot be empty."
            }

            require(event.venue.isNotBlank()) {
                "Event venue cannot be empty."
            }

            require(event.endAt > event.startAt) {
                "Event end time must be after its start time."
            }

            val community = dataSource.communities.values.firstOrNull {
                it.createdBy == userId
            } ?: error("Owned community not found.")

            require(event.communityId == community.id) {
                "Event must belong to your community."
            }

            require(event.createdBy == userId) {
                "Event creator does not match the authenticated user."
            }

            require(!dataSource.events.containsKey(event.id)) {
                "An event with this ID already exists."
            }

            dataSource.events[event.id] = event

            event.id
        }
    }

    override suspend fun getPublishedEvents(): Result<List<Event>> =
        runCatching {
            synchronized(dataSource.lock) {
                dataSource.currentUserId
                    ?: error("No authenticated user.")

                dataSource.events.values
                    .filter { it.isPublished }
                    .sortedBy { it.startAt }
            }
        }

    override suspend fun getEvent(
        eventId: String
    ): Result<Event> = runCatching {
        synchronized(dataSource.lock) {
            val userId = dataSource.currentUserId
                ?: error("No authenticated user.")

            val user = dataSource.users[userId]
                ?: error("User profile not found.")

            val event = dataSource.events[eventId]
                ?: error("Event not found.")

            val isOwner = event.createdBy == userId

            check(event.isPublished || (
                    user.accountType == AccountType.ADMIN && isOwner
                    )) {
                "You cannot access this unpublished event."
            }

            event
        }
    }

    override suspend fun getOwnedCommunityEvents(): Result<List<Event>> =
        runCatching {
            synchronized(dataSource.lock) {
                val userId = requireAdmin()

                val community = dataSource.communities.values.firstOrNull {
                    it.createdBy == userId
                } ?: error("Owned community not found.")

                dataSource.events.values
                    .filter { it.communityId == community.id }
                    .sortedBy { it.startAt }
            }
        }

    override suspend fun updateEvent(
        event: Event
    ): Result<Unit> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireAdmin()

            val existingEvent = dataSource.events[event.id]
                ?: error("Event not found.")

            check(existingEvent.createdBy == userId) {
                "You do not own this event."
            }

            require(event.title.isNotBlank()) {
                "Event title cannot be empty."
            }

            require(event.description.isNotBlank()) {
                "Event description cannot be empty."
            }

            require(event.category.isNotBlank()) {
                "Event category cannot be empty."
            }

            require(event.venue.isNotBlank()) {
                "Event venue cannot be empty."
            }

            require(event.endAt > event.startAt) {
                "Event end time must be after its start time."
            }

            check(event.createdBy == existingEvent.createdBy) {
                "Event creator cannot be changed."
            }

            check(event.communityId == existingEvent.communityId) {
                "Event community cannot be changed."
            }

            val updatedEvent = event.copy(
                updatedAt = System.currentTimeMillis()
            )

            dataSource.events[event.id] = updatedEvent
        }
    }

    override suspend fun deleteEvent(
        eventId: String
    ): Result<Unit> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireAdmin()

            val event = dataSource.events[eventId]
                ?: error("Event not found.")

            check(event.createdBy == userId) {
                "You do not own this event."
            }

            dataSource.events.remove(eventId)

            // Remove registrations associated with this event.
            dataSource.registrations.values.forEach {
                it.remove(eventId)
            }

            // Remove bookmarks associated with this event.
            dataSource.bookmarks.values.forEach {
                it.remove(eventId)
            }
        }
    }

    override suspend fun setPublished(
        eventId: String,
        isPublished: Boolean
    ): Result<Unit> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireAdmin()

            val event = dataSource.events[eventId]
                ?: error("Event not found.")

            check(event.createdBy == userId) {
                "You do not own this event."
            }

            dataSource.events[eventId] = event.copy(
                isPublished = isPublished,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun requireAdmin(): String {
        val userId = dataSource.currentUserId
            ?: error("No authenticated user.")

        val user = dataSource.users[userId]
            ?: error("User profile not found.")

        check(user.accountType == AccountType.ADMIN) {
            "This operation requires an administrator account."
        }

        return userId
    }
}
