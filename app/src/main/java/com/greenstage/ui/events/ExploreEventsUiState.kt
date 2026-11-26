package com.greenstage.ui.events

import com.greenstage.data.model.Event

sealed interface ExploreEventsUiState {
    object Loading : ExploreEventsUiState
    data class Success(
        val events: List<Event>,
        val searchQuery: String = "",
        val selectedCategory: String? = null
    ) : ExploreEventsUiState
    data class Error(val message: String) : ExploreEventsUiState
}
