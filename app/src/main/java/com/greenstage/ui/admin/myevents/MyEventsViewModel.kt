package com.greenstage.ui.admin.myevents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyEventsViewModel(
    private val communityRepository: CommunityRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyEventsUiState>(MyEventsUiState.Loading)
    val uiState: StateFlow<MyEventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        _uiState.value = MyEventsUiState.Loading
        viewModelScope.launch {
            val communityResult = communityRepository.getOwnedCommunity()
            if (communityResult.isFailure) {
                _uiState.value = MyEventsUiState.Error(
                    communityResult.exceptionOrNull()?.message ?: "Failed to load community."
                )
                return@launch
            }

            val community = communityResult.getOrThrow()
            val eventsResult = eventRepository.getOwnedCommunityEvents()
            if (eventsResult.isFailure) {
                _uiState.value = MyEventsUiState.Error(
                    eventsResult.exceptionOrNull()?.message ?: "Failed to load events."
                )
                return@launch
            }

            val events = eventsResult.getOrThrow()
            _uiState.value = MyEventsUiState.Success(
                events = events,
                communityName = community.name
            )
        }
    }

    fun togglePublishStatus(eventId: String, isCurrentlyPublished: Boolean) {
        viewModelScope.launch {
            val result = eventRepository.setPublished(eventId, !isCurrentlyPublished)
            if (result.isSuccess) {
                loadEvents()
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            val result = eventRepository.deleteEvent(eventId)
            if (result.isSuccess) {
                loadEvents()
            }
        }
    }

    class Factory(
        private val communityRepository: CommunityRepository,
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MyEventsViewModel::class.java)) {
                return MyEventsViewModel(communityRepository, eventRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
