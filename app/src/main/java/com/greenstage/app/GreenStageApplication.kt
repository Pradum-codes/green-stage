package com.greenstage.app

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.data.repository.UserRepository
import com.greenstage.data.source.firebase.FirebaseAuthRepository
import com.greenstage.data.source.firebase.FirebaseBookmarkRepository
import com.greenstage.data.source.firebase.FirebaseCommunityRepository
import com.greenstage.data.source.firebase.FirebaseEventRepository
import com.greenstage.data.source.firebase.FirebaseRegistrationRepository
import com.greenstage.data.source.firebase.FirebaseUserRepository
import com.greenstage.data.source.mock.MockAuthRepository
import com.greenstage.data.source.mock.MockBookmarkRepository
import com.greenstage.data.source.mock.MockCommunityRepository
import com.greenstage.data.source.mock.MockDataSource
import com.greenstage.data.source.mock.MockEventRepository
import com.greenstage.data.source.mock.MockRegistrationRepository
import com.greenstage.data.source.mock.MockUserRepository

class GreenStageApplication : Application() {

    companion object {
        // Toggle flag for environment selection. Default is false (Firebase mode).
        const val USE_MOCK_REPOSITORIES = false
    }

    private val mockDataSource by lazy { MockDataSource() }

    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firebaseFirestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val authRepository: AuthRepository by lazy {
        if (USE_MOCK_REPOSITORIES) {
            MockAuthRepository(mockDataSource)
        } else {
            FirebaseAuthRepository(firebaseAuth, firebaseFirestore)
        }
    }

    val userRepository: UserRepository by lazy {
        if (USE_MOCK_REPOSITORIES) {
            MockUserRepository(mockDataSource)
        } else {
            FirebaseUserRepository(firebaseAuth, firebaseFirestore)
        }
    }

    val communityRepository: CommunityRepository by lazy {
        if (USE_MOCK_REPOSITORIES) {
            MockCommunityRepository(mockDataSource)
        } else {
            FirebaseCommunityRepository(firebaseAuth, firebaseFirestore)
        }
    }

    val eventRepository: EventRepository by lazy {
        if (USE_MOCK_REPOSITORIES) {
            MockEventRepository(mockDataSource)
        } else {
            FirebaseEventRepository(firebaseAuth, firebaseFirestore)
        }
    }

    val registrationRepository: RegistrationRepository by lazy {
        if (USE_MOCK_REPOSITORIES) {
            MockRegistrationRepository(mockDataSource)
        } else {
            FirebaseRegistrationRepository(firebaseAuth, firebaseFirestore)
        }
    }

    val bookmarkRepository: BookmarkRepository by lazy {
        if (USE_MOCK_REPOSITORIES) {
            MockBookmarkRepository(mockDataSource)
        } else {
            FirebaseBookmarkRepository(firebaseAuth, firebaseFirestore)
        }
    }

    override fun onCreate() {
        super.onCreate()

        android.util.Log.d(
            "GreenStageInit",
            "GreenStageApplication onCreate() executed"
        )

//        if (BuildConfig.DEBUG) {
//            firebaseAuth.useEmulator("127.0.0.1", 9099)
//            firebaseFirestore.useEmulator("127.0.0.1", 8080)
//        }
    }
}
