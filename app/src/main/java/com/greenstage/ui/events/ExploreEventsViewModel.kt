package com.greenstage.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenstage.data.model.Event
import com.greenstage.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExploreEventsViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private var allEvents: List<Event> = emptyList()

    private val _uiState = MutableStateFlow<ExploreEventsUiState>(ExploreEventsUiState.Loading)
    val uiState: StateFlow<ExploreEventsUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    fun loadEvents() {
        _uiState.value = ExploreEventsUiState.Loading
        viewModelScope.launch {
            val result = eventRepository.getPublishedEvents()
            result.fold(
                onSuccess = { events ->
                    allEvents = events
                    _uiState.value = ExploreEventsUiState.Success(
                        events = events,
                        searchQuery = "",
                        selectedCategory = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = ExploreEventsUiState.Error(
                        error.message ?: "Failed to load events."
                    )
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value as? ExploreEventsUiState.Success ?: return
        updateFilteredState(searchQuery = query, selectedCategory = currentState.selectedCategory)
    }

    fun onCategorySelected(category: String?) {
        val currentState = _uiState.value as? ExploreEventsUiState.Success ?: return
        updateFilteredState(searchQuery = currentState.searchQuery, selectedCategory = category)
    }

    private fun updateFilteredState(searchQuery: String, selectedCategory: String?) {
        val filtered = allEvents.filter { event ->
            val matchesCategory = selectedCategory.isNullOrBlank() || selectedCategory == "All" || event.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    event.title.contains(searchQuery, ignoreCase = true) ||
                    event.description.contains(searchQuery, ignoreCase = true) ||
                    event.category.contains(searchQuery, ignoreCase = true) ||
                    event.venue.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
        _uiState.value = ExploreEventsUiState.Success(
            events = filtered,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory
        )
    }

    class Factory(
        private val eventRepository: EventRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExploreEventsViewModel::class.java)) {
                return ExploreEventsViewModel(eventRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
