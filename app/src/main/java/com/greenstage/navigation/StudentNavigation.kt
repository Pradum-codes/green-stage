package com.greenstage.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.studentNavigation() {
    navigation(
        startDestination = Routes.STUDENT_HOME,
        route = Routes.STUDENT_GRAPH
    ) {
        composable(Routes.STUDENT_HOME) {
            Text("Student Home Screen")
        }

        composable(Routes.STUDENT_EXPLORE) {
            Text("Explore Events Screen")
        }

        composable(Routes.STUDENT_SAVED) {
            Text("Saved Events Screen")
        }

        composable(Routes.STUDENT_PROFILE) {
            Text("Student Profile Screen")
        }

        composable(
            route = Routes.STUDENT_EVENT_DETAILS,
            arguments = listOf(
                navArgument(Routes.ARG_EVENT_ID) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments
                ?.getString(Routes.ARG_EVENT_ID)

            Text("Student Event Details: $eventId")
        }
    }
}
