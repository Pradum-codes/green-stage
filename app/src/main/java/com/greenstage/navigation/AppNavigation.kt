package com.greenstage.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.greenstage.data.model.AccountType
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.AuthState
import com.greenstage.data.repository.CommunityRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.UserRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    communityRepository: CommunityRepository,
    eventRepository: EventRepository,
    registrationRepository: RegistrationRepository,
    bookmarkRepository: BookmarkRepository
) {
    val navController = rememberNavController()
    val authState by authRepository.observeAuthState().collectAsState(initial = AuthState.Loading)
    val coroutineScope = rememberCoroutineScope()
    var adminCommunityError by remember { mutableStateOf<String?>(null) }
    var validatedUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Loading -> {
                validatedUserId = null
                adminCommunityError = null
                navController.navigate(Routes.LOADING) {
                    popUpTo(navController.graph.id) { inclusive = false }
                }
            }
            is AuthState.SignedOut -> {
                validatedUserId = null
                adminCommunityError = null
                navController.navigate(Routes.AUTH_GRAPH) {
                    popUpTo(navController.graph.id) { inclusive = false }
                }
            }
            is AuthState.Authenticated -> {
                if (state.user.accountType == AccountType.STUDENT) {
                    validatedUserId = null
                    adminCommunityError = null
                    navController.navigate(Routes.STUDENT_GRAPH) {
                        popUpTo(navController.graph.id) { inclusive = false }
                    }
                } else if (state.user.accountType == AccountType.ADMIN) {
                    if (validatedUserId != state.user.id) {
                        // 1. Immediately navigate to the loading destination before calling communityRepository.getOwnedCommunity()
                        navController.navigate(Routes.LOADING) {
                            popUpTo(navController.graph.id) { inclusive = false }
                        }
                        // 2. Call validation
                        val result = communityRepository.getOwnedCommunity()

                        // 3. Ensure coroutine is still active (not cancelled due to authState change during suspension)
                        currentCoroutineContext().ensureActive()

                        // 4. Navigate to admin graph ONLY after successful validation
                        if (result.isSuccess) {
                            validatedUserId = state.user.id
                            adminCommunityError = null
                            navController.navigate(Routes.ADMIN_GRAPH) {
                                popUpTo(navController.graph.id) { inclusive = false }
                            }
                        } else {
                            validatedUserId = null
                            adminCommunityError = result.exceptionOrNull()?.message ?: "Failed to load owned community"
                            navController.navigate(Routes.ADMIN_COMMUNITY_ERROR) {
                                popUpTo(navController.graph.id) { inclusive = false }
                            }
                        }
                    }
                }
            }
            is AuthState.ProfileError -> {
                validatedUserId = null
                adminCommunityError = null
                navController.navigate(Routes.PROFILE_ERROR) {
                    popUpTo(navController.graph.id) { inclusive = false }
                }
            }
            is AuthState.RegistrationRecoveryError -> {
                validatedUserId = null
                adminCommunityError = null
                navController.navigate(Routes.REGISTRATION_RECOVERY_ERROR) {
                    popUpTo(navController.graph.id) { inclusive = false }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOADING
    ) {
        composable(Routes.LOADING) {
            com.greenstage.ui.components.SplashScreen()
        }

        authNavigation(
            authRepository = authRepository,
            navController = navController
        )

        studentNavigation(
            authRepository = authRepository,
            userRepository = userRepository,
            eventRepository = eventRepository,
            registrationRepository = registrationRepository,
            bookmarkRepository = bookmarkRepository,
            navController = navController
        )

        adminNavigation(
            authRepository = authRepository,
            userRepository = userRepository,
            communityRepository = communityRepository,
            eventRepository = eventRepository,
            registrationRepository = registrationRepository,
            bookmarkRepository = bookmarkRepository,
            navController = navController
        )

        composable(Routes.PROFILE_ERROR) {
            val message = (authState as? AuthState.ProfileError)?.message ?: "Profile Error"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Profile Error: $message")
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Sign Out")
                }
            }
        }

        composable(Routes.REGISTRATION_RECOVERY_ERROR) {
            val message = (authState as? AuthState.RegistrationRecoveryError)?.message ?: "Registration Recovery Error"
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Registration Recovery Error: $message")
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Sign Out")
                }
            }
        }

        composable(Routes.ADMIN_COMMUNITY_ERROR) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Admin Community Error: ${adminCommunityError ?: "Unknown error"}")
                Button(
                    onClick = {
                        coroutineScope.launch {
                            authRepository.logout()
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}
