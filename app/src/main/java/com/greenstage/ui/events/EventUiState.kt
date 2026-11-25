package com.greenstage.ui.events

import com.greenstage.data.model.Event

sealed interface EventUiState {
    data object Loading : EventUiState
    data class Success(
        val event: Event,
        val isRegistered: Boolean = false,
        val isBookmarked: Boolean = false,
        val registrationCount: Long = 0L,
        val isActionLoading: Boolean = false,
        val actionError: String? = null
    ) : EventUiState
    data class Error(val message: String) : EventUiState
}
