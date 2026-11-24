package com.greenstage.ui

import com.greenstage.data.model.AccountType
import com.greenstage.data.source.mock.MockAuthRepository
import com.greenstage.data.source.mock.MockCommunityRepository
import com.greenstage.data.source.mock.MockDataSource
import com.greenstage.data.source.mock.MockEventRepository
import com.greenstage.ui.admin.createevent.CreateEventViewModel
import com.greenstage.ui.admin.dashboard.DashboardUiState
import com.greenstage.ui.admin.dashboard.DashboardViewModel
import com.greenstage.ui.auth.AuthViewModel
import com.greenstage.ui.events.EventUiState
import com.greenstage.ui.events.EventViewModel
import com.greenstage.ui.home.HomeUiState
import com.greenstage.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

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
    fun authViewModel_isDemoAvailable() {
        val viewModel = AuthViewModel(authRepository)
        assertTrue(viewModel.uiState.value.isDemoAvailable)
    }

    @Test
    fun authViewModel_loginAsDemoStudent() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.loginAsDemo(AccountType.STUDENT)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("demo-student", dataSource.currentUserId)
    }

    @Test
    fun authViewModel_loginAsDemoAdmin() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.loginAsDemo(AccountType.ADMIN)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals("demo-admin", dataSource.currentUserId)
    }

    @Test
    fun authViewModel_loginSuccess() = runTest {
        authRepository.registerStudent("John", "john@example.com", "password123")
        authRepository.logout()

        val viewModel = AuthViewModel(authRepository)
        viewModel.login("john@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(false, state.isLoading)
        val johnUser = dataSource.users.values.first { it.email == "john@example.com" }
        assertEquals(dataSource.currentUserId, johnUser.id)
    }

    @Test
    fun authViewModel_loginFailure() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.login("unknown@example.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun authViewModel_registerStudentSuccess() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.registerStudent("Student One", "student1@example.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        val createdStudent = dataSource.users.values.first { it.email == "student1@example.com" }
        assertEquals(AccountType.STUDENT, createdStudent.accountType)
    }

    @Test
    fun authViewModel_registerAdminSuccess() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.registerCommunityAdmin(
            name = "Admin One",
            email = "admin1@example.com",
            password = "password123",
            communityName = "Tech Club",
            communityDescription = "Tech community",
            communityCategory = "Technology"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        val createdAdmin = dataSource.users.values.first { it.email == "admin1@example.com" }
        assertEquals(AccountType.ADMIN, createdAdmin.accountType)
        assertTrue(dataSource.communities.values.any { it.name == "Tech Club" })
    }

    @Test
    fun homeViewModel_loadPublishedEvents() = runTest {
        val adminReg = authRepository.registerCommunityAdmin(
            name = "Admin", email = "admin@example.com", password = "password123",
            communityName = "Tech Club", communityDescription = "Desc", communityCategory = "Tech"
        ).getOrThrow()

        val event = com.greenstage.data.model.Event(
            id = "event-1",
            title = "Kotlin Conf",
            description = "Kotlin event",
            category = "Tech",
            venue = "Auditorium",
            startAt = 1000L,
            endAt = 2000L,
            imageUrl = null,
            communityId = adminReg.community.id,
            createdBy = adminReg.user.id,
            createdAt = 500L,
            updatedAt = 500L,
            isPublished = true
        )
        eventRepository.createEvent(event)

        val viewModel = HomeViewModel(eventRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Success)
        val events = (state as HomeUiState.Success).events
        assertTrue(events.any { it.title == "Kotlin Conf" })
    }

    @Test
    fun dashboardViewModel_loadDashboardData() = runTest {
        authRepository.registerCommunityAdmin(
            name = "Admin", email = "admin@example.com", password = "password123",
            communityName = "Dev Club", communityDescription = "Developers", communityCategory = "Tech"
        )

        val viewModel = DashboardViewModel(communityRepository, eventRepository, authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val successState = state as DashboardUiState.Success
        assertEquals("Dev Club", successState.community.name)
        assertTrue(successState.events.isEmpty())
    }

    @Test
    fun createEventViewModel_createsEventSuccessfully() = runTest {
        authRepository.registerCommunityAdmin(
            name = "Admin", email = "admin@example.com", password = "password123",
            communityName = "Dev Club", communityDescription = "Developers", communityCategory = "Tech"
        )

        val viewModel = CreateEventViewModel(eventRepository, communityRepository)
        viewModel.createEvent(
            title = "New Android Workshop",
            description = "Learn Compose",
            category = "Tech",
            venue = "Room 101",
            startAt = 1000L,
            endAt = 2000L,
            isPublished = true
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertTrue(dataSource.events.values.any { it.title == "New Android Workshop" })
    }

    @Test
    fun eventViewModel_loadsEventDetails() = runTest {
        val adminReg = authRepository.registerCommunityAdmin(
            name = "Admin", email = "admin@example.com", password = "password123",
            communityName = "Tech Club", communityDescription = "Desc", communityCategory = "Tech"
        ).getOrThrow()

        val event = com.greenstage.data.model.Event(
            id = "event-details-1",
            title = "Compose In Depth",
            description = "Deep dive",
            category = "Tech",
            venue = "Hall A",
            startAt = 1000L,
            endAt = 2000L,
            imageUrl = null,
            communityId = adminReg.community.id,
            createdBy = adminReg.user.id,
            createdAt = 500L,
            updatedAt = 500L,
            isPublished = true
        )
        eventRepository.createEvent(event)

        val viewModel = EventViewModel(eventRepository)
        viewModel.loadEvent("event-details-1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EventUiState.Success)
        assertEquals("Compose In Depth", (state as EventUiState.Success).event.title)
    }
}
