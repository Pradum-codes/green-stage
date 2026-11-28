package com.greenstage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.data.repository.UserRepository

fun NavGraphBuilder.adminNavigation(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    communityRepository: CommunityRepository,
    eventRepository: EventRepository,
    registrationRepository: RegistrationRepository,
    bookmarkRepository: BookmarkRepository,
    navController: NavHostController
) {
    composable(Routes.ADMIN_GRAPH) {
        AdminMainScreen(
            authRepository = authRepository,
            userRepository = userRepository,
            communityRepository = communityRepository,
            eventRepository = eventRepository,
            registrationRepository = registrationRepository,
            bookmarkRepository = bookmarkRepository,
            rootNavController = navController
        )
    }
}
