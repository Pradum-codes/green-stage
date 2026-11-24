package com.greenstage.ui.admin.createevent

data class CreateEventUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)
