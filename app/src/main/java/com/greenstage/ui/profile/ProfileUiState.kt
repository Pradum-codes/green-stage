package com.greenstage.ui.profile

import com.greenstage.data.model.User

sealed interface ProfileUiState {
    object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}
