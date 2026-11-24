package com.greenstage.ui.home

import com.greenstage.data.model.Event

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Success(val events: List<Event>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
