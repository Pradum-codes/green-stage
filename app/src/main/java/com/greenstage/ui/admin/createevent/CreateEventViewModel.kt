package com.greenstage.ui.admin.createevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenstage.data.model.Event
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CreateEventViewModel(
    private val eventRepository: EventRepository,
    private val communityRepository: CommunityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    fun createEvent(
        title: String,
        description: String,
        category: String,
        venue: String,
        startAt: Long,
        endAt: Long,
        isPublished: Boolean
    ) {
        if (title.isBlank() || description.isBlank() || category.isBlank() || venue.isBlank()) {
            _uiState.value = CreateEventUiState(errorMessage = "All text fields are required.")
            return
        }

        if (endAt <= startAt) {
            _uiState.value = CreateEventUiState(errorMessage = "End time must be after start time.")
            return
        }

        _uiState.value = CreateEventUiState(isLoading = true)

        viewModelScope.launch {
            val communityResult = communityRepository.getOwnedCommunity()
            if (communityResult.isFailure) {
                _uiState.value = CreateEventUiState(
                    errorMessage = communityResult.exceptionOrNull()?.message
                        ?: "Failed to get owned community."
                )
                return@launch
            }

            val community = communityResult.getOrThrow()
            val now = System.currentTimeMillis()
            val event = Event(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                category = category.trim(),
                venue = venue.trim(),
                startAt = startAt,
                endAt = endAt,
                imageUrl = null,
                communityId = community.id,
                createdBy = community.createdBy,
                createdAt = now,
                updatedAt = now,
                isPublished = isPublished
            )

            val result = eventRepository.createEvent(event)
            if (result.isFailure) {
                _uiState.value = CreateEventUiState(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to create event."
                )
            } else {
                _uiState.value = CreateEventUiState(isSuccess = true)
            }
        }
    }

    fun resetState() {
        _uiState.value = CreateEventUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val eventRepository: EventRepository,
        private val communityRepository: CommunityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CreateEventViewModel::class.java)) {
                return CreateEventViewModel(eventRepository, communityRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
