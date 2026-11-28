package com.greenstage.ui

import com.greenstage.data.model.Event
import com.greenstage.data.source.mock.MockAuthRepository
import com.greenstage.data.source.mock.MockCommunityRepository
import com.greenstage.data.source.mock.MockDataSource
import com.greenstage.data.source.mock.MockEventRepository
import com.greenstage.ui.admin.createevent.CreateEventViewModel
import com.greenstage.ui.admin.dashboard.DashboardUiState
import com.greenstage.ui.admin.dashboard.DashboardViewModel
import com.greenstage.ui.admin.myevents.MyEventsUiState
import com.greenstage.ui.admin.myevents.MyEventsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminFeatureTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataSource: MockDataSource
    private lateinit var authRepository: MockAuthRepository
    private lateinit var communityRepository: MockCommunityRepository
    private lateinit var eventRepository: MockEventRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataSource = MockDataSource()
        authRepository = MockAuthRepository(dataSource)
        communityRepository = MockCommunityRepository(dataSource)
        eventRepository = MockEventRepository(dataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun myEventsViewModel_loadsAndManagesAdminEvents() = runTest {
        val adminReg = authRepository.registerCommunityAdmin(
            name = "Admin User",
            email = "admin@greenstage.edu",
            password = "password123",
            communityName = "Robotics Club",
            communityDescription = "Build robots",
            communityCategory = "Technology"
        ).getOrThrow()

        val event1 = Event(
            id = "event-admin-1",
            title = "Robot Workshop",
            description = "Hands on",
            category = "Technology",
            venue = "Lab A",
            startAt = 1000L,
            endAt = 2000L,
            imageUrl = null,
            communityId = adminReg.community.id,
            createdBy = adminReg.user.id,
            createdAt = 500L,
            updatedAt = 500L,
            isPublished = true
        )
        val event2 = Event(
            id = "event-admin-2",
            title = "Robot Race",
            description = "Competition",
            category = "Technology",
            venue = "Field",
            startAt = 3000L,
            endAt = 4000L,
            imageUrl = null,
            communityId = adminReg.community.id,
            createdBy = adminReg.user.id,
            createdAt = 500L,
            updatedAt = 500L,
            isPublished = false
        )
        eventRepository.createEvent(event1).getOrThrow()
        eventRepository.createEvent(event2).getOrThrow()

        val viewModel = MyEventsViewModel(communityRepository, eventRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state is MyEventsUiState.Success)
        val successState = state as MyEventsUiState.Success
        assertEquals("Robotics Club", successState.communityName)
        assertEquals(2, successState.events.size)

        // Toggle publish status of event1 (published -> draft)
        viewModel.togglePublishStatus("event-admin-1", true)
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        val updatedEvents = (state as MyEventsUiState.Success).events
        val updatedEvent1 = updatedEvents.first { it.id == "event-admin-1" }
        assertFalse(updatedEvent1.isPublished)

        // Delete event2
        viewModel.deleteEvent("event-admin-2")
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        val remainingEvents = (state as MyEventsUiState.Success).events
        assertEquals(1, remainingEvents.size)
        assertEquals("event-admin-1", remainingEvents.first().id)
    }

    @Test
    fun dashboardViewModel_loadsCommunityAndMetrics() = runTest {
        authRepository.registerCommunityAdmin(
            name = "Admin User",
            email = "admin@greenstage.edu",
            password = "password123",
            communityName = "Green Society",
            communityDescription = "Eco initiative",
            communityCategory = "Sustainability"
        ).getOrThrow()

        val viewModel = DashboardViewModel(communityRepository, eventRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val successState = state as DashboardUiState.Success
        assertEquals("Green Society", successState.community.name)
        assertEquals("Sustainability", successState.community.category)
    }

    @Test
    fun createEventViewModel_createsEventSuccessfully() = runTest {
        authRepository.registerCommunityAdmin(
            name = "Admin User",
            email = "admin@greenstage.edu",
            password = "password123",
            communityName = "Music Club",
            communityDescription = "Jam sessions",
            communityCategory = "Music"
        ).getOrThrow()

        val viewModel = CreateEventViewModel(eventRepository, communityRepository)
        viewModel.createEvent(
            title = "Jazz Night",
            description = "Live jazz performance",
            category = "Music",
            venue = "Auditorium",
            startAt = 5000L,
            endAt = 6000L,
            isPublished = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertTrue(dataSource.events.values.any { it.title == "Jazz Night" })
    }
}
