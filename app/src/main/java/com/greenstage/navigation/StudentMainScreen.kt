package com.greenstage.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.greenstage.app.R
import com.greenstage.app.ui.theme.OnPrimary
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.Primary
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.data.repository.AuthRepository
import com.greenstage.data.repository.BookmarkRepository
import com.greenstage.data.repository.EventRepository
import com.greenstage.data.repository.RegistrationRepository
import com.greenstage.data.repository.UserRepository
import com.greenstage.ui.components.StudentHeader
import com.greenstage.ui.events.EventDetailsDialog
import com.greenstage.ui.events.EventViewModel
import com.greenstage.ui.events.ExploreEventsScreen
import com.greenstage.ui.events.ExploreEventsViewModel
import com.greenstage.ui.home.HomeScreen
import com.greenstage.ui.home.HomeViewModel
import com.greenstage.ui.profile.ProfileScreen
import com.greenstage.ui.profile.ProfileViewModel
import com.greenstage.ui.saved.SavedEventsScreen
import com.greenstage.ui.saved.SavedEventsViewModel

@Composable
fun StudentMainScreen(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    eventRepository: EventRepository,
    registrationRepository: RegistrationRepository,
    bookmarkRepository: BookmarkRepository,
    rootNavController: NavHostController
) {
    val studentNavController = rememberNavController()
    val navBackStackEntry by studentNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = Surface,
        topBar = {
            StudentHeader(title = "Green Stage")
        },
        bottomBar = {
            StudentBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    if (currentRoute != route) {
                        studentNavController.navigate(route) {
                            popUpTo(Routes.STUDENT_HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = studentNavController,
            startDestination = Routes.STUDENT_HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.STUDENT_HOME) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(eventRepository, authRepository)
                )
                HomeScreen(
                    viewModel = viewModel,
                    onEventClick = { eventId ->
                        studentNavController.navigate(
                            String.format(Routes.STUDENT_EVENT_DETAILS_ROUTE, eventId)
                        )
                    }
                )
            }

            composable(Routes.STUDENT_EXPLORE) {
                val viewModel: ExploreEventsViewModel = viewModel(
                    factory = ExploreEventsViewModel.Factory(eventRepository)
                )
                ExploreEventsScreen(
                    viewModel = viewModel,
                    onEventClick = { eventId ->
                        studentNavController.navigate(
                            String.format(Routes.STUDENT_EVENT_DETAILS_ROUTE, eventId)
                        )
                    }
                )
            }

            composable(Routes.STUDENT_SAVED) {
                val viewModel: SavedEventsViewModel = viewModel(
                    factory = SavedEventsViewModel.Factory(
                        bookmarkRepository,
                        registrationRepository,
                        eventRepository
                    )
                )
                SavedEventsScreen(
                    viewModel = viewModel,
                    onEventClick = { eventId ->
                        studentNavController.navigate(
                            String.format(Routes.STUDENT_EVENT_DETAILS_ROUTE, eventId)
                        )
                    }
                )
            }

            composable(Routes.STUDENT_PROFILE) {
                val viewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModel.Factory(userRepository, authRepository)
                )
                ProfileScreen(viewModel = viewModel)
            }

            dialog(
                route = Routes.STUDENT_EVENT_DETAILS,
                arguments = listOf(
                    navArgument(Routes.ARG_EVENT_ID) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString(Routes.ARG_EVENT_ID) ?: ""
                val viewModel: EventViewModel = viewModel(
                    factory = EventViewModel.Factory(
                        eventRepository,
                        registrationRepository,
                        bookmarkRepository
                    )
                )
                EventDetailsDialog(
                    eventId = eventId,
                    viewModel = viewModel,
                    onDismissRequest = { studentNavController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun StudentBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        Triple(Routes.STUDENT_HOME, "Home", R.drawable.home),
        Triple(Routes.STUDENT_EXPLORE, "Explore", R.drawable.explore),
        Triple(Routes.STUDENT_SAVED, "Saved", R.drawable.bookmark),
        Triple(Routes.STUDENT_PROFILE, "Profile", R.drawable.person)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .border(width = 2.dp, color = Outline)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            val shape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(1.dp)
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(x = 2.dp, y = 2.dp)
                            .background(Outline, shape)
                    )
                }
                Column(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = if (selected) Primary else Color.Transparent,
                            shape = shape
                        )
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = if (selected) Outline else Color.Transparent,
                            shape = shape
                        )
                        .clickable { onNavigate(route) }
                        .padding(vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = if (selected) OnPrimary else OnSurfaceCard
                    )
                    Text(
                        text = label,
                        color = if (selected) OnPrimary else OnSurfaceCard,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
