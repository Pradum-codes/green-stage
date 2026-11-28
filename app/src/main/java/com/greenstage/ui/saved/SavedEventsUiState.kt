package com.greenstage.ui.saved

import com.greenstage.data.model.Event

sealed interface SavedEventsUiState {
    object Loading : SavedEventsUiState
    data class Success(
        val bookmarkedEvents: List<Event>,
        val registeredEvents: List<Event>
    ) : SavedEventsUiState
    data class Error(val message: String) : SavedEventsUiState
}
