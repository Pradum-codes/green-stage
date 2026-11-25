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
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.ui.admin.createevent.CreateEventScreen
import com.greenstage.ui.admin.createevent.CreateEventViewModel
import com.greenstage.ui.admin.dashboard.DashboardScreen
import com.greenstage.ui.admin.dashboard.DashboardViewModel
import com.greenstage.ui.events.EventDetailsScreen
import com.greenstage.ui.events.EventViewModel

fun NavGraphBuilder.adminNavigation(
    authRepository: AuthRepository,
    communityRepository: CommunityRepository,
    eventRepository: EventRepository,
    registrationRepository: RegistrationRepository,
    bookmarkRepository: BookmarkRepository,
    navController: NavHostController
) {
    navigation(
        startDestination = Routes.ADMIN_DASHBOARD,
        route = Routes.ADMIN_GRAPH
    ) {
        composable(Routes.ADMIN_DASHBOARD) {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(communityRepository, eventRepository, authRepository)
            )
            DashboardScreen(
                viewModel = viewModel,
                onCreateEventClick = { navController.navigate(Routes.ADMIN_CREATE_EVENT) },
                onEventClick = { eventId ->
                    navController.navigate(String.format(Routes.ADMIN_EVENT_DETAILS_ROUTE, eventId))
                }
            )
        }

        composable(Routes.ADMIN_CREATE_EVENT) {
            val viewModel: CreateEventViewModel = viewModel(
                factory = CreateEventViewModel.Factory(eventRepository, communityRepository)
            )
            CreateEventScreen(
                viewModel = viewModel,
                onEventCreated = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ADMIN_EVENT_DETAILS,
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
