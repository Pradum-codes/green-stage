package com.greenstage.ui.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isDemoAvailable: Boolean = false
)
