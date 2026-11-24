package com.greenstage.app

import android.app.Application

import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.data.repository.UserRepository

import com.greenstage.data.source.mock.MockDataSource
import com.greenstage.data.source.mock.MockAuthRepository
import com.greenstage.data.source.mock.MockBookmarkRepository
import com.greenstage.data.source.mock.MockCommunityRepository
import com.greenstage.data.source.mock.MockEventRepository
import com.greenstage.data.source.mock.MockRegistrationRepository
import com.greenstage.data.source.mock.MockUserRepository

class GreenStageApplication : Application() {

    // One shared data source for all mock repositories.
    private val mockDataSource = MockDataSource()

    // Repository instances exposed through interfaces.
    val authRepository: AuthRepository =
        MockAuthRepository(mockDataSource)

    val userRepository: UserRepository =
        MockUserRepository(mockDataSource)

    val communityRepository: CommunityRepository =
        MockCommunityRepository(mockDataSource)

    val eventRepository: EventRepository =
        MockEventRepository(mockDataSource)

    val registrationRepository: RegistrationRepository =
        MockRegistrationRepository(mockDataSource)

    val bookmarkRepository: BookmarkRepository =
        MockBookmarkRepository(mockDataSource)
}
