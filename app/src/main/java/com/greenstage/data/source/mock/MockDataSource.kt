package com.greenstage.data.source.mock

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
}
