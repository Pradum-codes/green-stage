package com.greenstage.ui.events

import com.greenstage.data.model.Event

data class ParticipantInfo(
    val userId: String,
    val name: String,
    val email: String,
    val registeredAt: Long
)

sealed interface EventUiState {
    data object Loading : EventUiState
    data class Success(
        val event: Event,
        val isRegistered: Boolean = false,
        val isBookmarked: Boolean = false,
        val registrationCount: Long = 0L,
        val participants: List<ParticipantInfo> = emptyList(),
        val isActionLoading: Boolean = false,
        val actionError: String? = null
    ) : EventUiState
    data class Error(val message: String) : EventUiState
}
