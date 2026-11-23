
package com.greenstage.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            Text("Login Screen")
        }

        composable(Routes.REGISTER) {
            Text("Register Screen")
        }

        composable(Routes.FORGOT_PASSWORD) {
            Text("Forgot Password Screen")
        }

        composable(Routes.HOME) {
            Text("Home Screen")
        }

        composable(Routes.EXPLORE_EVENTS) {
            Text("Explore Events Screen")
        }

        composable(Routes.EVENT_DETAILS) {
            Text("Event Details Screen")
        }

        composable(Routes.SAVED_EVENTS) {
            Text("Saved Events Screen")
        }

        composable(Routes.PROFILE) {
            Text("Profile Screen")
        }

        composable(Routes.ADMIN_DASHBOARD) {
            Text("Admin Dashboard")
        }

        composable(Routes.CREATE_EVENT) {
            Text("Create Event Screen")
        }

        composable(Routes.MY_EVENTS) {
            Text("My Events Screen")
        }
    }
}
