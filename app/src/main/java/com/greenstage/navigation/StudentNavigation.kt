package com.greenstage.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.data.repository.UserRepository

fun NavGraphBuilder.studentNavigation(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    eventRepository: EventRepository,
    registrationRepository: RegistrationRepository,
    bookmarkRepository: BookmarkRepository,
    navController: NavHostController
) {
    composable(Routes.STUDENT_GRAPH) {
        StudentMainScreen(
            authRepository = authRepository,
            userRepository = userRepository,
            eventRepository = eventRepository,
            registrationRepository = registrationRepository,
            bookmarkRepository = bookmarkRepository,
            rootNavController = navController
        )
    }
}
