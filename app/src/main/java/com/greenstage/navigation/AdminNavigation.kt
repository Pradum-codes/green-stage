package com.greenstage.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.adminNavigation() {
    navigation(
        startDestination = Routes.ADMIN_DASHBOARD,
        route = Routes.ADMIN_GRAPH
    ) {
        composable(Routes.ADMIN_DASHBOARD) {
            Text("Admin Dashboard")
        }

        composable(Routes.ADMIN_CREATE_EVENT) {
            Text("Create Event Screen")
        }

        composable(Routes.ADMIN_MY_EVENTS) {
            Text("My Events Screen")
        }

        composable(Routes.ADMIN_PROFILE) {
            Text("Admin Profile Screen")
        }

        composable(
            route = Routes.ADMIN_EVENT_DETAILS,
            arguments = listOf(
                navArgument(Routes.ARG_EVENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments
                ?.getString(Routes.ARG_EVENT_ID)

            Text("Admin Event Details: $eventId")
        }

        composable(
            route = Routes.ADMIN_EDIT_EVENT,
            arguments = listOf(
                navArgument(Routes.ARG_EVENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments
                ?.getString(Routes.ARG_EVENT_ID)

            Text("Edit Admin Event: $eventId")
        }
    }
}
