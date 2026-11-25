package com.greenstage.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.ui.events.EventDetailsScreen
import com.greenstage.ui.events.EventViewModel
import com.greenstage.ui.home.HomeScreen
import com.greenstage.ui.home.HomeViewModel

fun NavGraphBuilder.studentNavigation(
    authRepository: AuthRepository,
    eventRepository: EventRepository,
    registrationRepository: RegistrationRepository,
    bookmarkRepository: BookmarkRepository,
    navController: NavHostController
) {
    navigation(
        startDestination = Routes.STUDENT_HOME,
        route = Routes.STUDENT_GRAPH
    ) {
        composable(Routes.STUDENT_HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(eventRepository, authRepository)
            )
            HomeScreen(
                viewModel = viewModel,
                onEventClick = { eventId ->
                    navController.navigate(String.format(Routes.STUDENT_EVENT_DETAILS_ROUTE, eventId))
                }
            )
        }

        composable(
            route = Routes.STUDENT_EVENT_DETAILS,
            arguments = listOf(
                navArgument(Routes.ARG_EVENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString(Routes.ARG_EVENT_ID) ?: ""
            val viewModel: EventViewModel = viewModel(
                factory = EventViewModel.Factory(eventRepository, registrationRepository, bookmarkRepository)
            )
            EventDetailsScreen(
                eventId = eventId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
