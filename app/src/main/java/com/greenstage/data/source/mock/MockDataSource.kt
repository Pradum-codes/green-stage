package com.greenstage.data.source.mock

import com.greenstage.data.model.AccountType
import com.greenstage.data.model.Bookmark
import com.greenstage.data.model.Community
import com.greenstage.data.model.Event
import com.greenstage.data.model.Registration
import com.greenstage.data.model.User
import com.greenstage.data.repository.AuthState
import kotlinx.coroutines.flow.MutableStateFlow

internal class MockDataSource {

    val lock = Any()

    val users = mutableMapOf<String, User>()

    val passwordsByEmail = mutableMapOf<String, String>()

    val communities = mutableMapOf<String, Community>()

    val events = mutableMapOf<String, Event>()

    val registrations =
        mutableMapOf<String, MutableMap<String, Registration>>()

    val bookmarks =
        mutableMapOf<String, MutableMap<String, Bookmark>>()

    var currentUserId: String? = null

    val authState = MutableStateFlow<AuthState>(
        AuthState.SignedOut
    )

    init {
        initDemoData()
    }

    private fun initDemoData() {
        val now = System.currentTimeMillis()

        // 1. Demo Student Account
        val demoStudent = User(
            id = "demo-student",
            name = "Demo Student",
            email = "student@greenstage.demo",
            accountType = AccountType.STUDENT,
            interests = listOf("Technology", "Workshop"),
            createdAt = now,
            updatedAt = now
        )
        users[demoStudent.id] = demoStudent
        passwordsByEmail[demoStudent.email] = "demo123"

        // 2. Demo Admin Account
        val demoAdmin = User(
            id = "demo-admin",
            name = "Demo Admin",
            email = "admin@greenstage.demo",
            accountType = AccountType.ADMIN,
            interests = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        users[demoAdmin.id] = demoAdmin
        passwordsByEmail[demoAdmin.email] = "demo123"

        // 3. Demo Admin Community
        val demoCommunity = Community(
            id = demoAdmin.id,
            name = "Green Stage Tech Community",
            description = "The official developer & tech community showcasing workshops, hackathons, and tech talks.",
            category = "Technology",
            createdBy = demoAdmin.id,
            createdAt = now,
            updatedAt = now
        )
        communities[demoCommunity.id] = demoCommunity

        // 4. Sample Demo Events
        val demoEvents = listOf(
            Event(
                id = "demo-event-1",
                title = "Android Jetpack Compose Workshop",
                description = "Hands-on deep dive into modern declarative UI on Android with Jetpack Compose.",
                category = "Technology",
                venue = "Main Auditorium, Block A",
                startAt = now + 86_400_000L,
                endAt = now + 93_600_000L,
                imageUrl = null,
                communityId = demoCommunity.id,
                createdBy = demoAdmin.id,
                createdAt = now,
                updatedAt = now,
                isPublished = true
            ),
            Event(
                id = "demo-event-2",
                title = "Green Tech Hackathon 2026",
                description = "Build sustainable technology solutions to reduce carbon footprint and promote green energy.",
                category = "Hackathon",
                venue = "Innovation Lab 3",
                startAt = now + 172_800_000L,
                endAt = now + 259_200_000L,
                imageUrl = null,
                communityId = demoCommunity.id,
                createdBy = demoAdmin.id,
                createdAt = now,
                updatedAt = now,
                isPublished = true
            ),
            Event(
                id = "demo-event-3",
                title = "AI & Machine Learning Symposium",
                description = "Explore current trends in on-device AI, LLMs, and intelligent mobile apps.",
                category = "AI / Data",
                venue = "Virtual Conference Hall",
                startAt = now + 345_600_000L,
                endAt = now + 352_800_000L,
                imageUrl = null,
                communityId = demoCommunity.id,
                createdBy = demoAdmin.id,
                createdAt = now,
                updatedAt = now,
                isPublished = true
            )
        )

        demoEvents.forEach { event ->
            events[event.id] = event
        }
    }
}
