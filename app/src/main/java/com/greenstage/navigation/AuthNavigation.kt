package com.greenstage.navigation

import androidx.compose.material3.Text
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation

fun NavGraphBuilder.authNavigation() {
    navigation(
        startDestination = Routes.LOGIN,
        route = Routes.AUTH_GRAPH
    ) {
        composable(Routes.LOGIN) {
            Text("Login Screen")
        }

        composable(Routes.REGISTER) {
            Text("Register Choice Screen")
        }

        composable(Routes.STUDENT_REGISTER) {
            Text("Student Register Screen")
        }

        composable(Routes.ADMIN_REGISTER) {
            Text("Admin Register Screen")
        }

        composable(Routes.FORGOT_PASSWORD) {
            Text("Forgot Password Screen")
        }
    }
}
