package com.greenstage.ui.admin.myevents

import com.greenstage.data.model.Event

sealed interface MyEventsUiState {
    data object Loading : MyEventsUiState
    data class Error(val message: String) : MyEventsUiState
    data class Success(
        val events: List<Event>,
        val communityName: String
    ) : MyEventsUiState
}
