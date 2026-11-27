package com.greenstage.data.repository

import com.greenstage.data.model.Registration

interface RegistrationRepository {

    suspend fun registerForEvent(
        eventId: String
    ): Result<Unit>

    suspend fun cancelRegistration(
        eventId: String
    ): Result<Unit>

    suspend fun isRegistered(
        eventId: String
    ): Result<Boolean>

    suspend fun getCurrentUserRegistrations():
            Result<List<Registration>>

    suspend fun getRegistrationCount(
        eventId: String
    ): Result<Long>

    suspend fun getEventParticipants(
        eventId: String
    ): Result<List<com.greenstage.ui.events.ParticipantInfo>>
}
