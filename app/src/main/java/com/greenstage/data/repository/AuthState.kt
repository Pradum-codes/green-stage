package com.greenstage.data.repository

import com.greenstage.data.model.User

sealed interface AuthState {

    data object Loading : AuthState

    data object SignedOut : AuthState

    data class Authenticated(
        val user: User
    ) : AuthState

    data class ProfileError(
        val message: String
    ) : AuthState

    data class RegistrationRecoveryError(
        val message: String
    ) : AuthState
}
