package com.greenstage.ui.events

import com.greenstage.data.model.Event

sealed interface EventUiState {
    data object Loading : EventUiState
    data class Success(val event: Event) : EventUiState
    data class Error(val message: String) : EventUiState
}
