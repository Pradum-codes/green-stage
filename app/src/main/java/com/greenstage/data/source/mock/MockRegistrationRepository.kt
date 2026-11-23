package com.greenstage.data.source.mock

import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Registration
import com.greenstage.data.repository.RegistrationRepository

internal class MockRegistrationRepository(
    private val dataSource: MockDataSource
) : RegistrationRepository {

    override suspend fun registerForEvent(
        eventId: String
    ): Result<Unit> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            val event = dataSource.events[eventId]
                ?: error("Event not found.")

            check(event.isPublished) {
                "You cannot register for an unpublished event."
            }

            val userRegistrations = dataSource.registrations
                .getOrPut(userId) { mutableMapOf() }

            check(!userRegistrations.containsKey(eventId)) {
                "You are already registered for this event."
            }

            userRegistrations[eventId] = Registration(
                eventId = eventId,
                registeredAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun cancelRegistration(
        eventId: String
    ): Result<Unit> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            val userRegistrations = dataSource.registrations[userId]
                ?: error("No registrations found.")

            check(userRegistrations.containsKey(eventId)) {
                "You are not registered for this event."
            }

            userRegistrations.remove(eventId)
        }
    }

    override suspend fun isRegistered(
        eventId: String
    ): Result<Boolean> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            dataSource.registrations[userId]
                ?.containsKey(eventId) ?: false
        }
    }

    override suspend fun getCurrentUserRegistrations():
            Result<List<Registration>> = runCatching {
        synchronized(dataSource.lock) {
            val userId = requireStudent()

            dataSource.registrations[userId]
                ?.values
                ?.toList()
                ?: emptyList()
        }
    }

    override suspend fun getRegistrationCount(
        eventId: String
    ): Result<Long> = runCatching {
        synchronized(dataSource.lock) {
            val userId = dataSource.currentUserId
                ?: error("No authenticated user.")

            val user = dataSource.users[userId]
                ?: error("User profile not found.")

            val event = dataSource.events[eventId]
                ?: error("Event not found.")

            check(
                user.accountType == AccountType.ADMIN &&
                        event.createdBy == userId
            ) {
                "Only the event owner can view its registration count."
            }

            dataSource.registrations.values.count {
                it.containsKey(eventId)
            }.toLong()
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
