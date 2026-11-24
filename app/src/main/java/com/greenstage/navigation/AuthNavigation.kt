package com.greenstage.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.greenstage.data.repository.AuthRepository
import com.greenstage.ui.auth.AdminRegisterScreen
import com.greenstage.ui.auth.AuthViewModel
import com.greenstage.ui.auth.ForgotPasswordScreen
import com.greenstage.ui.auth.LoginScreen
import com.greenstage.ui.auth.RegisterScreen
import com.greenstage.ui.auth.StudentRegisterScreen

fun NavGraphBuilder.authNavigation(
    authRepository: AuthRepository,
    navController: NavHostController
) {
    navigation(
        startDestination = Routes.LOGIN,
        route = Routes.AUTH_GRAPH
    ) {
        composable(Routes.LOGIN) {
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(authRepository)
            )
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToStudentRegister = { navController.navigate(Routes.STUDENT_REGISTER) },
                onNavigateToAdminRegister = { navController.navigate(Routes.ADMIN_REGISTER) },
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.STUDENT_REGISTER) {
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(authRepository)
            )
            StudentRegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.ADMIN_REGISTER) {
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(authRepository)
            )
            AdminRegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModel.Factory(authRepository)
            )
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) }
            )
        }
    }
}
