package com.greenstage.ui.saved

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

class SavedEventsViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val registrationRepository: RegistrationRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedEventsUiState>(SavedEventsUiState.Loading)
    val uiState: StateFlow<SavedEventsUiState> = _uiState.asStateFlow()

    init {
        loadSavedEvents()
    }

    fun loadSavedEvents() {
        _uiState.value = SavedEventsUiState.Loading
        viewModelScope.launch {
            val bookmarksResult = bookmarkRepository.getCurrentUserBookmarks()
            val registrationsResult = registrationRepository.getCurrentUserRegistrations()
            val publishedEventsResult = eventRepository.getPublishedEvents()

            if (bookmarksResult.isFailure || registrationsResult.isFailure || publishedEventsResult.isFailure) {
                val errorMsg = bookmarksResult.exceptionOrNull()?.message
                    ?: registrationsResult.exceptionOrNull()?.message
                    ?: publishedEventsResult.exceptionOrNull()?.message
                    ?: "Failed to load saved events."
                _uiState.value = SavedEventsUiState.Error(errorMsg)
                return@launch
            }

            val bookmarks = bookmarksResult.getOrThrow()
            val registrations = registrationsResult.getOrThrow()
            val publishedEvents = publishedEventsResult.getOrThrow()

            val bookmarkedEventIds = bookmarks.map { it.eventId }.toSet()
            val registeredEventIds = registrations.map { it.eventId }.toSet()

            val bookmarkedEvents = publishedEvents.filter { bookmarkedEventIds.contains(it.id) }
            val registeredEvents = publishedEvents.filter { registeredEventIds.contains(it.id) }

            _uiState.value = SavedEventsUiState.Success(
                bookmarkedEvents = bookmarkedEvents,
                registeredEvents = registeredEvents
            )
        }
    }

    fun removeBookmark(eventId: String) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(eventId)
            loadSavedEvents()
        }
    }

    fun cancelRegistration(eventId: String) {
        viewModelScope.launch {
            registrationRepository.cancelRegistration(eventId)
            loadSavedEvents()
        }
    }

    class Factory(
        private val bookmarkRepository: BookmarkRepository,
        private val registrationRepository: RegistrationRepository,
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SavedEventsViewModel::class.java)) {
                return SavedEventsViewModel(bookmarkRepository, registrationRepository, eventRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
