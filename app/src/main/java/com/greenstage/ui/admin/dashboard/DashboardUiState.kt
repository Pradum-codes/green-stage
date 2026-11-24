package com.greenstage.ui.admin.dashboard

import com.greenstage.data.model.Community
import com.greenstage.data.model.Event

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val community: Community,
        val events: List<Event>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}
