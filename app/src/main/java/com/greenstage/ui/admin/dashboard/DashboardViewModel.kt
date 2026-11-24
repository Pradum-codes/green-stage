package com.greenstage.ui.admin.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val communityRepository: CommunityRepository,
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        _uiState.value = DashboardUiState.Loading
        viewModelScope.launch {
            val communityResult = communityRepository.getOwnedCommunity()
            if (communityResult.isFailure) {
                _uiState.value = DashboardUiState.Error(
                    communityResult.exceptionOrNull()?.message ?: "Failed to load community."
                )
                return@launch
            }

            val community = communityResult.getOrThrow()
            val eventsResult = eventRepository.getOwnedCommunityEvents()
            if (eventsResult.isFailure) {
                _uiState.value = DashboardUiState.Error(
                    eventsResult.exceptionOrNull()?.message ?: "Failed to load community events."
                )
                return@launch
            }

            val events = eventsResult.getOrThrow()
            _uiState.value = DashboardUiState.Success(
                community = community,
                events = events
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    class Factory(
        private val communityRepository: CommunityRepository,
        private val eventRepository: EventRepository,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(communityRepository, eventRepository, authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
