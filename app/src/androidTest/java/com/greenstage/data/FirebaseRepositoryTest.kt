package com.greenstage.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.model.Event
import com.greenstage.data.source.firebase.FirebaseAuthRepository
import com.greenstage.data.source.firebase.FirebaseBookmarkRepository
import com.greenstage.data.source.firebase.FirebaseEventRepository
import com.greenstage.data.source.firebase.FirebaseRegistrationRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FirebaseRepositoryTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var authRepo: FirebaseAuthRepository
    private lateinit var registrationRepo: FirebaseRegistrationRepository
    private lateinit var bookmarkRepo: FirebaseBookmarkRepository
    private lateinit var eventRepo: FirebaseEventRepository

    @Before
    fun setUp() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        try {
            auth.useEmulator("127.0.0.1", 9099)
            firestore.useEmulator("127.0.0.1", 8080)
        } catch (_: Exception) {}

        authRepo = FirebaseAuthRepository(auth, firestore)
        registrationRepo = FirebaseRegistrationRepository(auth, firestore)
        bookmarkRepo = FirebaseBookmarkRepository(auth, firestore)
        eventRepo = FirebaseEventRepository(auth, firestore)
    }

    @After
    fun tearDown() {
        runBlocking {
            auth.signOut()
        }
    }

    @Test
    fun testFirebaseStudentAndAdminIntegrationFlow() = runBlocking {
        auth.signOut()

        val suffix = System.currentTimeMillis()
        val adminEmail = "admin_$suffix@example.com"
        val studentEmail = "student_$suffix@example.com"

        // 1. Register Admin and create a published event
        val adminResult = authRepo.registerCommunityAdmin(
            name = "Test Admin",
            email = adminEmail,
            password = "password123",
            communityName = "Test Community",
            communityDescription = "Test Description",
            communityCategory = "Technology"
        )
        assertTrue("Admin registration failed: ${adminResult.exceptionOrNull()?.message}", adminResult.isSuccess)
        val admin = adminResult.getOrThrow()

        val eventId = "event_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val event = Event(
            id = eventId,
            title = "Firebase Test Workshop",
            description = "Testing Firebase repositories",
            category = "Technology",
            venue = "Online",
            startAt = now + 100000L,
            endAt = now + 200000L,
            imageUrl = null,
            communityId = admin.community.id,
            createdBy = admin.user.id,
            createdAt = now,
            updatedAt = now,
            isPublished = true
        )

        val createEventResult = eventRepo.createEvent(event)
        assertTrue("Event creation failed: ${createEventResult.exceptionOrNull()?.message}", createEventResult.isSuccess)

        // 2. Sign out admin and register student
        auth.signOut()

        val studentResult = authRepo.registerStudent(
            name = "Test Student",
            email = studentEmail,
            password = "password123"
        )
        assertTrue("Student registration failed: ${studentResult.exceptionOrNull()?.message}", studentResult.isSuccess)

        // 3. Test Registration Operations
        val isRegBefore = registrationRepo.isRegistered(eventId).getOrThrow()
        assertFalse(isRegBefore)

        val registerResult = registrationRepo.registerForEvent(eventId)
        assertTrue("Event registration failed: ${registerResult.exceptionOrNull()?.message}", registerResult.isSuccess)

        val isRegAfter = registrationRepo.isRegistered(eventId).getOrThrow()
        assertTrue(isRegAfter)

        val studentRegistrations = registrationRepo.getCurrentUserRegistrations().getOrThrow()
        assertTrue(studentRegistrations.any { it.eventId == eventId })

        // 4. Test Bookmark Operations
        val isBookmarkedBefore = bookmarkRepo.isBookmarked(eventId).getOrThrow()
        assertFalse(isBookmarkedBefore)

        val addBookmarkResult = bookmarkRepo.addBookmark(eventId)
        assertTrue("Adding bookmark failed: ${addBookmarkResult.exceptionOrNull()?.message}", addBookmarkResult.isSuccess)

        val isBookmarkedAfter = bookmarkRepo.isBookmarked(eventId).getOrThrow()
        assertTrue(isBookmarkedAfter)

        val studentBookmarks = bookmarkRepo.getCurrentUserBookmarks().getOrThrow()
        assertTrue(studentBookmarks.any { it.eventId == eventId })

        // 5. Test Cancel Registration and Remove Bookmark
        val cancelResult = registrationRepo.cancelRegistration(eventId)
        assertTrue(cancelResult.isSuccess)
        assertFalse(registrationRepo.isRegistered(eventId).getOrThrow())

        val removeBookmarkResult = bookmarkRepo.removeBookmark(eventId)
        assertTrue(removeBookmarkResult.isSuccess)
        assertFalse(bookmarkRepo.isBookmarked(eventId).getOrThrow())
    }
}
