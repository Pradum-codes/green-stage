package com.greenstage.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenstage.data.model.AccountType
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.source.mock.MockAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            isDemoAvailable = authRepository is MockAuthRepository
        )
    }

    fun loginAsDemo(accountType: AccountType) {
        val mockAuthRepo = authRepository as? MockAuthRepository
        if (mockAuthRepo == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Demo mode is only available in mock environment."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = mockAuthRepo.loginAsDemo(accountType)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Demo login failed."
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Email and password cannot be empty.")
            return
        }

        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            if (result.isFailure) {
                _uiState.value = AuthUiState(
                    errorMessage = result.exceptionOrNull()?.message ?: "Login failed."
                )
            } else {
                _uiState.value = AuthUiState(isLoading = false)
            }
        }
    }

    fun registerStudent(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "All fields are required.")
            return
        }

        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.registerStudent(name, email, password)
            if (result.isFailure) {
                _uiState.value = AuthUiState(
                    errorMessage = result.exceptionOrNull()?.message ?: "Student registration failed."
                )
            } else {
                _uiState.value = AuthUiState(isLoading = false)
            }
        }
    }

    fun registerCommunityAdmin(
        name: String,
        email: String,
        password: String,
        communityName: String,
        communityDescription: String,
        communityCategory: String
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank() ||
            communityName.isBlank() || communityDescription.isBlank() || communityCategory.isBlank()
        ) {
            _uiState.value = AuthUiState(errorMessage = "All fields are required.")
            return
        }

        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.registerCommunityAdmin(
                name = name,
                email = email,
                password = password,
                communityName = communityName,
                communityDescription = communityDescription,
                communityCategory = communityCategory
            )
            if (result.isFailure) {
                _uiState.value = AuthUiState(
                    errorMessage = result.exceptionOrNull()?.message ?: "Admin registration failed."
                )
            } else {
                _uiState.value = AuthUiState(isLoading = false)
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState(errorMessage = "Email cannot be empty.")
            return
        }

        _uiState.value = AuthUiState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isFailure) {
                _uiState.value = AuthUiState(
                    errorMessage = result.exceptionOrNull()?.message ?: "Password reset failed."
                )
            } else {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    successMessage = "Password reset instructions sent to your email."
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
