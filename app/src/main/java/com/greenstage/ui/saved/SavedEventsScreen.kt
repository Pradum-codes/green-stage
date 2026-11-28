package com.greenstage.ui.saved

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenstage.app.ui.theme.OnPrimary
import com.greenstage.app.ui.theme.OnSoftCoral
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.OnSurfaceVariant
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.OutlineVariant
import com.greenstage.app.ui.theme.Primary
import com.greenstage.app.ui.theme.SoftCoral
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.data.model.Event
import com.greenstage.ui.components.CategoryChip
import com.greenstage.ui.components.EmptyState
import com.greenstage.ui.components.ErrorState
import com.greenstage.ui.components.LoadingIndicator
import com.greenstage.ui.components.OffsetShadowCard
import com.greenstage.ui.home.ScreenHeading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedEventsScreen(
    viewModel: SavedEventsViewModel,
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.loadSavedEvents(isRefresh = true)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ScreenHeading(
                        title = "Saved Events",
                        subtitle = "Manage your bookmarked and registered events."
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Outline, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Bookmarked", "Registered").forEachIndexed { index, label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        if (selectedTab == index) Primary else SurfaceCard,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = if (selectedTab == index) 2.dp else 0.dp,
                                        color = if (selectedTab == index) Outline else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedTab = index },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (selectedTab == index) OnPrimary else OnSurfaceCard,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                when (val state = uiState) {
                    is SavedEventsUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator()
                            }
                        }
                    }
                    is SavedEventsUiState.Error -> {
                        item {
                            ErrorState(
                                message = state.message,
                                onRetry = { viewModel.loadSavedEvents() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            )
                        }
                    }
                    is SavedEventsUiState.Success -> {
                        val events = if (selectedTab == 0) state.bookmarkedEvents else state.registeredEvents
                        if (events.isEmpty()) {
                            item {
                                EmptyState(
                                    message = if (selectedTab == 0) {
                                        "No bookmarked events. Bookmark events from Home or Explore to save them here."
                                    } else {
                                        "No registered events. Register for an event from its details page to see it here."
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                )
                            }
                        } else {
                            items(events, key = { it.id }) { event ->
                                SavedEventCard(
                                    event = event,
                                    isBookmarked = selectedTab == 0,
                                    onViewDetails = { onEventClick(event.id) },
                                    onAction = {
                                        if (selectedTab == 0) {
                                            viewModel.removeBookmark(event.id)
                                        } else {
                                            viewModel.cancelRegistration(event.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedEventCard(
    event: Event,
    isBookmarked: Boolean,
    onViewDetails: () -> Unit,
    onAction: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    OffsetShadowCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Outline, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryChip(category = event.category)
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceCard
            )
            val date = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", LocalConfiguration.current.locales[0])
                .format(java.util.Date(event.startAt))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(date, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                Text(event.venue, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
            }
            HorizontalDivider(thickness = 2.dp, color = OutlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.material3.TextButton(onClick = onViewDetails) {
                    Text(
                        "View Details",
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(2.dp, Outline),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftCoral,
                        contentColor = OnSoftCoral
                    )
                ) {
                    Text(
                        if (isBookmarked) "Remove Bookmark" else "Cancel Registration",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
