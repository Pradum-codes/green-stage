package com.greenstage.data.source.mock

import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Community
import com.greenstage.data.model.Event
import com.greenstage.data.model.User
import com.greenstage.data.repository.AuthState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the in-memory mock data layer.
 *
 * All repositories in each fixture share one MockDataSource instance,
 * matching the intended application wiring.
 */

private class MockRepositoryFixture {
    val dataSource = MockDataSource()
    val auth = MockAuthRepository(dataSource)
    val users = MockUserRepository(dataSource)
    val communities = MockCommunityRepository(dataSource)
    val events = MockEventRepository(dataSource)
    val registrations = MockRegistrationRepository(dataSource)
    val bookmarks = MockBookmarkRepository(dataSource)

    suspend fun createStudent(
        name: String = "Test Student",
        email: String = "student@example.com"
    ): User = auth.registerStudent(name, email, "password123").getOrThrow()

    suspend fun createAdmin(
        name: String = "Test Admin",
        email: String = "admin@example.com",
        communityName: String = "Computer Society"
    ) = auth.registerCommunityAdmin(
        name = name,
        email = email,
        password = "password123",
        communityName = communityName,
        communityDescription = "A community for computing enthusiasts",
        communityCategory = "Technology"
    ).getOrThrow()

    fun event(
        id: String = "event-1",
        communityId: String,
        createdBy: String,
        published: Boolean = true,
        startAt: Long = 2_000_000L,
        endAt: Long = 3_000_000L
    ) = Event(
        id = id,
        title = "Android Workshop",
        description = "A practical Android development workshop",
        category = "Technology",
        venue = "Block A",
        startAt = startAt,
        endAt = endAt,
        imageUrl = null,
        communityId = communityId,
        createdBy = createdBy,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
        isPublished = published
    )

    suspend fun createPublishedEvent(id: String = "event-1"): Event {
        val admin = createAdmin()
        val event = event(
            id = id,
            communityId = admin.community.id,
            createdBy = admin.user.id,
            published = true
        )
        events.createEvent(event).getOrThrow()
        return event
    }
}

class MockAuthRepositoryTest {
    @Test
    fun registerStudent_createsStudentAndAuthenticates() = runBlocking {
        val fixture = MockRepositoryFixture()

        val user = fixture.createStudent(
            name = "  Alex  ",
            email = "  ALEX@Example.com "
        )

        assertEquals("Alex", user.name)
        assertEquals("alex@example.com", user.email)
        assertEquals(AccountType.STUDENT, user.accountType)
        assertEquals(user.id, fixture.dataSource.currentUserId)
        assertEquals(AuthState.Authenticated(user), fixture.auth.observeAuthState().first())
    }

    @Test
    fun registerStudent_rejectsDuplicateEmailIgnoringCaseAndWhitespace() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.createStudent()

        val result = fixture.auth.registerStudent(
            "Another Student",
            " STUDENT@example.com ",
            "password123"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun registerStudent_rejectsInvalidCredentials() = runBlocking {
        val fixture = MockRepositoryFixture()

        assertTrue(fixture.auth.registerStudent("", "a@example.com", "password123").isFailure)
        assertTrue(fixture.auth.registerStudent("Alex", "invalid-email", "password123").isFailure)
        assertTrue(fixture.auth.registerStudent("Alex", "a@example.com", "123").isFailure)
    }

    @Test
    fun registerAdmin_createsUserAndOwnedCommunityTogether() = runBlocking {
        val fixture = MockRepositoryFixture()

        val result = fixture.createAdmin()

        assertEquals(AccountType.ADMIN, result.user.accountType)
        assertEquals(result.user.id, result.community.createdBy)
        assertEquals(result.user.id, fixture.dataSource.currentUserId)
        assertEquals(result.user, fixture.dataSource.users[result.user.id])
        assertEquals(result.community, fixture.dataSource.communities[result.community.id])
    }

    @Test
    fun login_normalizesEmailAndRejectsWrongPassword() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.createStudent()

        fixture.auth.logout().getOrThrow()

        val user = fixture.auth.login(" STUDENT@EXAMPLE.COM ", "password123").getOrThrow()
        assertEquals("student@example.com", user.email)

        fixture.auth.logout().getOrThrow()
        assertTrue(fixture.auth.login("student@example.com", "wrong-password").isFailure)
    }

    @Test
    fun passwordReset_requiresExistingEmail() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.createStudent()

        assertTrue(fixture.auth.sendPasswordResetEmail("student@example.com").isSuccess)
        assertTrue(fixture.auth.sendPasswordResetEmail("missing@example.com").isFailure)
    }

    @Test
    fun logoutClearsSessionButPreservesStoredUser() = runBlocking {
        val fixture = MockRepositoryFixture()
        val user = fixture.createStudent()

        fixture.auth.logout().getOrThrow()

        assertNull(fixture.dataSource.currentUserId)
        assertEquals(AuthState.SignedOut, fixture.auth.observeAuthState().first())
        assertEquals(user, fixture.dataSource.users[user.id])
    }
}

class MockDemoAuthTest {
    @Test
    fun demoDataInitializedCorrectly() {
        val dataSource = MockDataSource()

        assertNotNull(dataSource.users["demo-student"])
        assertEquals("student@greenstage.demo", dataSource.users["demo-student"]?.email)

        assertNotNull(dataSource.users["demo-admin"])
        assertEquals("admin@greenstage.demo", dataSource.users["demo-admin"]?.email)

        assertNotNull(dataSource.communities["demo-admin"])
        assertEquals("demo-admin", dataSource.communities["demo-admin"]?.createdBy)

        assertTrue(dataSource.events.size >= 3)
        assertTrue(dataSource.events.values.all { it.communityId == "demo-admin" })
    }

    @Test
    fun loginAsDemo_studentSucceedsAndUpdatesAuthState() = runBlocking {
        val fixture = MockRepositoryFixture()

        val result = fixture.auth.loginAsDemo(AccountType.STUDENT)
        assertTrue(result.isSuccess)

        val user = result.getOrThrow()
        assertEquals("demo-student", user.id)
        assertEquals(AccountType.STUDENT, user.accountType)
        assertEquals(user.id, fixture.dataSource.currentUserId)
        assertEquals(AuthState.Authenticated(user), fixture.auth.observeAuthState().first())
    }

    @Test
    fun loginAsDemo_adminSucceedsAndUpdatesAuthState() = runBlocking {
        val fixture = MockRepositoryFixture()

        val result = fixture.auth.loginAsDemo(AccountType.ADMIN)
        assertTrue(result.isSuccess)

        val user = result.getOrThrow()
        assertEquals("demo-admin", user.id)
        assertEquals(AccountType.ADMIN, user.accountType)
        assertEquals(user.id, fixture.dataSource.currentUserId)
        assertEquals(AuthState.Authenticated(user), fixture.auth.observeAuthState().first())
    }

    @Test
    fun demoStudentCanRetrievePublishedEvents() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.auth.loginAsDemo(AccountType.STUDENT).getOrThrow()

        val publishedEvents = fixture.events.getPublishedEvents().getOrThrow()
        assertTrue(publishedEvents.isNotEmpty())
        assertTrue(publishedEvents.any { it.id == "demo-event-1" })
    }

    @Test
    fun demoAdminCanRetrieveOwnedCommunityEventsAndValidateCommunity() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.auth.loginAsDemo(AccountType.ADMIN).getOrThrow()

        val ownedCommunity = fixture.communities.getOwnedCommunity().getOrThrow()
        assertEquals("demo-admin", ownedCommunity.id)
        assertEquals("demo-admin", ownedCommunity.createdBy)

        val ownedEvents = fixture.events.getOwnedCommunityEvents().getOrThrow()
        assertTrue(ownedEvents.isNotEmpty())
        assertTrue(ownedEvents.all { it.communityId == ownedCommunity.id })
    }

    @Test
    fun logoutClearsDemoSession() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.auth.loginAsDemo(AccountType.STUDENT).getOrThrow()

        fixture.auth.logout().getOrThrow()

        assertNull(fixture.dataSource.currentUserId)
        assertEquals(AuthState.SignedOut, fixture.auth.observeAuthState().first())
    }

    @Test
    fun demoAuthDoesNotBreakRegularRegistrationAndLogin() = runBlocking {
        val fixture = MockRepositoryFixture()

        // 1. Regular student registration
        val student = fixture.auth.registerStudent("New Student", "newstudent@example.com", "password123").getOrThrow()
        assertEquals("newstudent@example.com", student.email)

        fixture.auth.logout().getOrThrow()

        // 2. Demo admin login
        fixture.auth.loginAsDemo(AccountType.ADMIN).getOrThrow()
        assertEquals("demo-admin", fixture.dataSource.currentUserId)

        fixture.auth.logout().getOrThrow()

        // 3. Regular student login
        val relogged = fixture.auth.login("newstudent@example.com", "password123").getOrThrow()
        assertEquals(student.id, relogged.id)
    }
}

class MockUserRepositoryTest {
    @Test
    fun getCurrentUser_failsWhenSignedOut() = runBlocking {
        val fixture = MockRepositoryFixture()
        assertTrue(fixture.users.getCurrentUser().isFailure)
    }

    @Test
    fun updateProfile_normalizesInterestsAndUpdatesAuthState() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.createStudent()

        fixture.users.updateProfile(
            name = "  Alex Morgan  ",
            interests = listOf(" Android ", "Kotlin", "", "Android", "  ")
        ).getOrThrow()

        val updated = fixture.users.getCurrentUser().getOrThrow()
        assertEquals("Alex Morgan", updated.name)
        assertEquals(listOf("Android", "Kotlin"), updated.interests)
        assertEquals(AuthState.Authenticated(updated), fixture.auth.observeAuthState().first())
    }

    @Test
    fun updateProfile_rejectsBlankName() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.createStudent()

        assertTrue(
            fixture.users.updateProfile("   ", listOf("Android")).isFailure
        )
    }
}

class MockCommunityRepositoryTest {
    @Test
    fun adminCanReadAndUpdateOwnedCommunity() = runBlocking {
        val fixture = MockRepositoryFixture()
        val admin = fixture.createAdmin()

        val owned = fixture.communities.getOwnedCommunity().getOrThrow()
        assertEquals(admin.community.id, owned.id)

        fixture.communities.updateCommunity(
            communityId = owned.id,
            name = "  LPU Developers ",
            description = " Developer community ",
            category = " Software "
        ).getOrThrow()

        val updated = fixture.communities.getOwnedCommunity().getOrThrow()
        assertEquals("LPU Developers", updated.name)
        assertEquals("Developer community", updated.description)
        assertEquals("Software", updated.category)
    }

    @Test
    fun studentCannotReadOrUpdateCommunity() = runBlocking {
        val fixture = MockRepositoryFixture()
        val admin = fixture.createAdmin()
        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        assertTrue(fixture.communities.getOwnedCommunity().isFailure)
        assertTrue(
            fixture.communities.updateCommunity(
                admin.community.id, "Changed", "Description", "Technology"
            ).isFailure
        )
    }

    @Test
    fun adminCannotUpdateAnotherAdminsCommunity() = runBlocking {
        val fixture = MockRepositoryFixture()
        val firstAdmin = fixture.createAdmin(email = "first@example.com")
        fixture.auth.logout().getOrThrow()
        fixture.createAdmin(email = "second@example.com")

        val result = fixture.communities.updateCommunity(
            firstAdmin.community.id,
            "Hijacked community",
            "Changed description",
            "Technology"
        )

        assertTrue(result.isFailure)
    }
}

class MockEventRepositoryTest {
    @Test
    fun adminCanCreateListUpdateAndPublishOwnedEvent() = runBlocking {
        val fixture = MockRepositoryFixture()
        val admin = fixture.createAdmin()
        val initialPublishedCount = fixture.events.getPublishedEvents().getOrThrow().size

        val draft = fixture.event(
            communityId = admin.community.id,
            createdBy = admin.user.id,
            published = false
        )

        assertEquals(draft.id, fixture.events.createEvent(draft).getOrThrow())
        assertEquals(initialPublishedCount, fixture.events.getPublishedEvents().getOrThrow().size)
        assertEquals(draft, fixture.events.getEvent(draft.id).getOrThrow())

        val edited = draft.copy(title = "Updated Workshop")
        fixture.events.updateEvent(edited).getOrThrow()
        fixture.events.setPublished(draft.id, true).getOrThrow()

        assertEquals("Updated Workshop", fixture.events.getEvent(draft.id).getOrThrow().title)
        assertEquals(initialPublishedCount + 1, fixture.events.getPublishedEvents().getOrThrow().size)
        assertEquals(1, fixture.events.getOwnedCommunityEvents().getOrThrow().size)
    }

    @Test
    fun createEvent_rejectsInvalidTimesAndWrongCommunity() = runBlocking {
        val fixture = MockRepositoryFixture()
        val admin = fixture.createAdmin()

        val invalidTimes = fixture.event(
            communityId = admin.community.id,
            createdBy = admin.user.id,
            startAt = 3_000_000L,
            endAt = 2_000_000L
        )
        assertTrue(fixture.events.createEvent(invalidTimes).isFailure)

        val wrongCommunity = fixture.event(
            id = "wrong-community",
            communityId = "not-owned",
            createdBy = admin.user.id
        )
        assertTrue(fixture.events.createEvent(wrongCommunity).isFailure)
    }

    @Test
    fun studentCannotCreateOrManageEvents() = runBlocking {
        val fixture = MockRepositoryFixture()
        fixture.createAdmin()
        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        val event = fixture.event(
            communityId = "community",
            createdBy = "student"
        )

        assertTrue(fixture.events.createEvent(event).isFailure)
        assertTrue(fixture.events.setPublished("missing", true).isFailure)
        assertTrue(fixture.events.deleteEvent("missing").isFailure)
    }

    @Test
    fun studentCannotReadUnpublishedEvent() = runBlocking {
        val fixture = MockRepositoryFixture()
        val admin = fixture.createAdmin()
        val draft = fixture.event(
            communityId = admin.community.id,
            createdBy = admin.user.id,
            published = false
        )
        fixture.events.createEvent(draft).getOrThrow()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        assertTrue(fixture.events.getEvent(draft.id).isFailure)
    }

    @Test
    fun deleteEventAlsoRemovesRegistrationsAndBookmarks() = runBlocking {
        val fixture = MockRepositoryFixture()
        val admin = fixture.createAdmin()
        val event = fixture.event(
            communityId = admin.community.id,
            createdBy = admin.user.id,
            published = true
        )
        fixture.events.createEvent(event).getOrThrow()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()
        fixture.registrations.registerForEvent(event.id).getOrThrow()
        fixture.bookmarks.addBookmark(event.id).getOrThrow()

        fixture.auth.logout().getOrThrow()
        fixture.auth.login("admin@example.com", "password123").getOrThrow()
        fixture.events.deleteEvent(event.id).getOrThrow()

        assertTrue(fixture.dataSource.events[event.id] == null)
        assertTrue(fixture.dataSource.registrations.values.none { it.containsKey(event.id) })
        assertTrue(fixture.dataSource.bookmarks.values.none { it.containsKey(event.id) })
    }
}

class MockRegistrationRepositoryTest {
    @Test
    fun studentCanRegisterCancelAndCheckRegistration() = runBlocking {
        val fixture = MockRepositoryFixture()
        val event = fixture.createPublishedEvent()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        fixture.registrations.registerForEvent(event.id).getOrThrow()
        assertTrue(fixture.registrations.isRegistered(event.id).getOrThrow())
        assertEquals(1, fixture.registrations.getCurrentUserRegistrations().getOrThrow().size)

        fixture.registrations.cancelRegistration(event.id).getOrThrow()
        assertFalse(fixture.registrations.isRegistered(event.id).getOrThrow())
    }

    @Test
    fun registrationRejectsDuplicateAndUnpublishedEvent() = runBlocking {
        val fixture = MockRepositoryFixture()
        val published = fixture.createPublishedEvent("published")
        val admin = fixture.auth.login("admin@example.com", "password123").getOrThrow()
        val draft = fixture.event(
            id = "draft",
            communityId = fixture.communities.getOwnedCommunity().getOrThrow().id,
            createdBy = admin.id,
            published = false
        )
        fixture.events.createEvent(draft).getOrThrow()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        fixture.registrations.registerForEvent(published.id).getOrThrow()
        assertTrue(fixture.registrations.registerForEvent(published.id).isFailure)
        assertTrue(fixture.registrations.registerForEvent(draft.id).isFailure)
    }

    @Test
    fun registrationCountIsVisibleOnlyToOwningAdmin() = runBlocking {
        val fixture = MockRepositoryFixture()
        val event = fixture.createPublishedEvent()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()
        fixture.registrations.registerForEvent(event.id).getOrThrow()

        fixture.auth.logout().getOrThrow()
        fixture.auth.login("admin@example.com", "password123").getOrThrow()
        assertEquals(1L, fixture.registrations.getRegistrationCount(event.id).getOrThrow())

        fixture.auth.logout().getOrThrow()
        fixture.createStudent(email = "other@example.com")
        assertTrue(fixture.registrations.getRegistrationCount(event.id).isFailure)
    }

    @Test
    fun registrationRequiresStudentAccount() = runBlocking {
        val fixture = MockRepositoryFixture()
        val event = fixture.createPublishedEvent()

        assertTrue(fixture.registrations.registerForEvent(event.id).isFailure)
        assertTrue(fixture.registrations.isRegistered(event.id).isFailure)
    }
}

class MockBookmarkRepositoryTest {
    @Test
    fun studentCanAddCheckListAndRemoveBookmark() = runBlocking {
        val fixture = MockRepositoryFixture()
        val event = fixture.createPublishedEvent()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        fixture.bookmarks.addBookmark(event.id).getOrThrow()
        assertTrue(fixture.bookmarks.isBookmarked(event.id).getOrThrow())
        assertEquals(1, fixture.bookmarks.getCurrentUserBookmarks().getOrThrow().size)

        fixture.bookmarks.removeBookmark(event.id).getOrThrow()
        assertFalse(fixture.bookmarks.isBookmarked(event.id).getOrThrow())
    }

    @Test
    fun addingDuplicateBookmarkDoesNotCreateDuplicate() = runBlocking {
        val fixture = MockRepositoryFixture()
        val event = fixture.createPublishedEvent()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        fixture.bookmarks.addBookmark(event.id).getOrThrow()
        val originalSavedAt = fixture.bookmarks.getCurrentUserBookmarks()
            .getOrThrow().single().savedAt

        fixture.bookmarks.addBookmark(event.id).getOrThrow()
        val bookmarks = fixture.bookmarks.getCurrentUserBookmarks().getOrThrow()

        assertEquals(1, bookmarks.size)
        assertEquals(originalSavedAt, bookmarks.single().savedAt)
    }

    @Test
    fun bookmarkRequiresPublishedEventAndStudentAccount() = runBlocking {
        val fixture = MockRepositoryFixture()
        val admin = fixture.createAdmin()
        val draft = fixture.event(
            communityId = admin.community.id,
            createdBy = admin.user.id,
            published = false
        )
        fixture.events.createEvent(draft).getOrThrow()

        assertTrue(fixture.bookmarks.addBookmark(draft.id).isFailure)

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()
        assertTrue(fixture.bookmarks.addBookmark(draft.id).isFailure)
        assertTrue(fixture.bookmarks.addBookmark("missing").isFailure)
    }

    @Test
    fun bookmarksAreIsolatedPerStudent() = runBlocking {
        val fixture = MockRepositoryFixture()
        val event = fixture.createPublishedEvent()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()
        fixture.bookmarks.addBookmark(event.id).getOrThrow()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent(email = "second@example.com")

        assertFalse(fixture.bookmarks.isBookmarked(event.id).getOrThrow())
        assertTrue(fixture.bookmarks.getCurrentUserBookmarks().getOrThrow().isEmpty())
    }
}

class MockRepositoriesIntegrationTest {
    @Test
    fun adminPublishesEventAndStudentCanDiscoverRegisterAndBookmarkIt() = runBlocking {
        val fixture = MockRepositoryFixture()

        val admin = fixture.createAdmin()
        val event = fixture.event(
            communityId = admin.community.id,
            createdBy = admin.user.id,
            published = true
        )
        fixture.events.createEvent(event).getOrThrow()

        fixture.auth.logout().getOrThrow()
        fixture.createStudent()

        val discovered = fixture.events.getPublishedEvents().getOrThrow()
        assertTrue(discovered.any { it.id == event.id })

        fixture.registrations.registerForEvent(event.id).getOrThrow()
        fixture.bookmarks.addBookmark(event.id).getOrThrow()

        assertTrue(fixture.registrations.isRegistered(event.id).getOrThrow())
        assertTrue(fixture.bookmarks.isBookmarked(event.id).getOrThrow())

        // Switch back to the owner account and verify the aggregate count.
        fixture.auth.logout().getOrThrow()
        fixture.auth.login("admin@example.com", "password123").getOrThrow()
        assertEquals(1L, fixture.registrations.getRegistrationCount(event.id).getOrThrow())
    }
}
