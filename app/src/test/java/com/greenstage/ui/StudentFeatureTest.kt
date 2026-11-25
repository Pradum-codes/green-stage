package com.greenstage.ui

import com.greenstage.data.model.Event
import com.greenstage.data.source.mock.MockAuthRepository
import com.greenstage.data.source.mock.MockBookmarkRepository
import com.greenstage.data.source.mock.MockDataSource
import com.greenstage.data.source.mock.MockEventRepository
import com.greenstage.data.source.mock.MockRegistrationRepository
import com.greenstage.ui.events.EventUiState
import com.greenstage.ui.events.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudentFeatureTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataSource: MockDataSource
    private lateinit var authRepository: MockAuthRepository
    private lateinit var eventRepository: MockEventRepository
    private lateinit var registrationRepository: MockRegistrationRepository
    private lateinit var bookmarkRepository: MockBookmarkRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataSource = MockDataSource()
        authRepository = MockAuthRepository(dataSource)
        eventRepository = MockEventRepository(dataSource)
        registrationRepository = MockRegistrationRepository(dataSource)
        bookmarkRepository = MockBookmarkRepository(dataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun eventViewModel_studentRegisterAndBookmarkEvent() = runTest {
        val admin = authRepository.registerCommunityAdmin(
            name = "Admin", email = "admin@example.com", password = "password123",
            communityName = "Tech Society", communityDescription = "Desc", communityCategory = "Tech"
        ).getOrThrow()

        val event = Event(
            id = "student-test-event",
            title = "Kotlin Coroutines Masterclass",
            description = "Learn coroutines and flow",
            category = "Tech",
            venue = "Lab 1",
            startAt = 2000L,
            endAt = 3000L,
            imageUrl = null,
            communityId = admin.community.id,
            createdBy = admin.user.id,
            createdAt = 1000L,
            updatedAt = 1000L,
            isPublished = true
        )
        eventRepository.createEvent(event).getOrThrow()

        authRepository.logout().getOrThrow()
        authRepository.registerStudent("Student User", "student@example.com", "password123").getOrThrow()

        val viewModel = EventViewModel(eventRepository, registrationRepository, bookmarkRepository)
        viewModel.loadEvent("student-test-event")
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state is EventUiState.Success)
        val successState = state as EventUiState.Success
        assertEquals("Kotlin Coroutines Masterclass", successState.event.title)
        assertTrue(!successState.isRegistered)
        assertTrue(!successState.isBookmarked)

        viewModel.registerForEvent("student-test-event")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue((state as EventUiState.Success).isRegistered)
        assertEquals(1L, state.registrationCount)

        viewModel.addBookmark("student-test-event")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue((state as EventUiState.Success).isBookmarked)

        viewModel.cancelRegistration("student-test-event")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(!(state as EventUiState.Success).isRegistered)

        viewModel.removeBookmark("student-test-event")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertTrue(!(state as EventUiState.Success).isBookmarked)
    }
}
