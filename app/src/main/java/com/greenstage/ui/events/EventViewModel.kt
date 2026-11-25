package com.greenstage.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventViewModel(
    private val eventRepository: EventRepository,
    private val registrationRepository: RegistrationRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EventUiState>(EventUiState.Loading)
    val uiState: StateFlow<EventUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        _uiState.value = EventUiState.Loading
        viewModelScope.launch {
            val eventResult = eventRepository.getEvent(eventId)
            if (eventResult.isFailure) {
                _uiState.value = EventUiState.Error(
                    eventResult.exceptionOrNull()?.message ?: "Failed to load event details."
                )
                return@launch
            }
            val event = eventResult.getOrThrow()

            val regResult = registrationRepository.isRegistered(eventId)
            val isRegistered = regResult.getOrDefault(false)

            val bookmarkResult = bookmarkRepository.isBookmarked(eventId)
            val isBookmarked = bookmarkResult.getOrDefault(false)

            val countResult = registrationRepository.getRegistrationCount(eventId)
            val registrationCount = countResult.getOrDefault(0L)

            _uiState.value = EventUiState.Success(
                event = event,
                isRegistered = isRegistered,
                isBookmarked = isBookmarked,
                registrationCount = registrationCount
            )
        }
    }

    fun registerForEvent(eventId: String) {
        val currentState = _uiState.value as? EventUiState.Success ?: return
        _uiState.value = currentState.copy(isActionLoading = true, actionError = null)
        viewModelScope.launch {
            val result = registrationRepository.registerForEvent(eventId)
            if (result.isSuccess) {
                val countResult = registrationRepository.getRegistrationCount(eventId)
                _uiState.value = currentState.copy(
                    isRegistered = true,
                    registrationCount = countResult.getOrDefault(currentState.registrationCount + 1),
                    isActionLoading = false
                )
            } else {
                _uiState.value = currentState.copy(
                    isActionLoading = false,
                    actionError = result.exceptionOrNull()?.message ?: "Failed to register."
                )
            }
        }
    }

    fun cancelRegistration(eventId: String) {
        val currentState = _uiState.value as? EventUiState.Success ?: return
        _uiState.value = currentState.copy(isActionLoading = true, actionError = null)
        viewModelScope.launch {
            val result = registrationRepository.cancelRegistration(eventId)
            if (result.isSuccess) {
                val countResult = registrationRepository.getRegistrationCount(eventId)
                _uiState.value = currentState.copy(
                    isRegistered = false,
                    registrationCount = countResult.getOrDefault(maxOf(0L, currentState.registrationCount - 1)),
                    isActionLoading = false
                )
            } else {
                _uiState.value = currentState.copy(
                    isActionLoading = false,
                    actionError = result.exceptionOrNull()?.message ?: "Failed to cancel registration."
                )
            }
        }
    }

    fun addBookmark(eventId: String) {
        val currentState = _uiState.value as? EventUiState.Success ?: return
        _uiState.value = currentState.copy(isActionLoading = true, actionError = null)
        viewModelScope.launch {
            val result = bookmarkRepository.addBookmark(eventId)
            if (result.isSuccess) {
                _uiState.value = currentState.copy(
                    isBookmarked = true,
                    isActionLoading = false
                )
            } else {
                _uiState.value = currentState.copy(
                    isActionLoading = false,
                    actionError = result.exceptionOrNull()?.message ?: "Failed to bookmark."
                )
            }
        }
    }

    fun removeBookmark(eventId: String) {
        val currentState = _uiState.value as? EventUiState.Success ?: return
        _uiState.value = currentState.copy(isActionLoading = true, actionError = null)
        viewModelScope.launch {
            val result = bookmarkRepository.removeBookmark(eventId)
            if (result.isSuccess) {
                _uiState.value = currentState.copy(
                    isBookmarked = false,
                    isActionLoading = false
                )
            } else {
                _uiState.value = currentState.copy(
                    isActionLoading = false,
                    actionError = result.exceptionOrNull()?.message ?: "Failed to remove bookmark."
                )
            }
        }
    }

    class Factory(
        private val eventRepository: EventRepository,
        private val registrationRepository: RegistrationRepository,
        private val bookmarkRepository: BookmarkRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
                return EventViewModel(eventRepository, registrationRepository, bookmarkRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
