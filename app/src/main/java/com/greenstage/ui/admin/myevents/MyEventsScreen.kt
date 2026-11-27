package com.greenstage.ui.admin.myevents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenstage.app.R
import com.greenstage.app.ui.theme.OnPrimary
import com.greenstage.app.ui.theme.OnSoftCoral
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.Primary
import com.greenstage.app.ui.theme.SoftCoral
import com.greenstage.app.ui.theme.SoftYellow
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.data.model.Event
import com.greenstage.ui.components.EmptyState
import com.greenstage.ui.components.ErrorState
import com.greenstage.ui.components.EventCard
import com.greenstage.ui.components.LoadingIndicator
import com.greenstage.ui.components.OffsetShadowCard
import com.greenstage.ui.home.ScreenHeading

enum class EventStatusFilter { ALL, PUBLISHED, DRAFT }

@Composable
fun MyEventsScreen(
    viewModel: MyEventsViewModel,
    onCreateEventClick: () -> Unit,
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(EventStatusFilter.ALL) }

    Scaffold(
        containerColor = Surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is MyEventsUiState.Loading -> LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            is MyEventsUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.loadEvents() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            is MyEventsUiState.Success -> {
                val filteredEvents = state.events.filter { event ->
                    when (selectedFilter) {
                        EventStatusFilter.ALL -> true
                        EventStatusFilter.PUBLISHED -> event.isPublished
                        EventStatusFilter.DRAFT -> !event.isPublished
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title + Create Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScreenHeading(
                            title = "My Events",
                            subtitle = "Manage events for ${state.communityName}",
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = onCreateEventClick,
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = OnPrimary
                            ),
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .border(2.dp, Outline, RoundedCornerShape(6.dp))
                        ) {
                            Text("+ Create", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Filters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventStatusFilter.entries.forEach { filter ->
                            val isSelected = selectedFilter == filter
                            val label = when (filter) {
                                EventStatusFilter.ALL -> "All (${state.events.size})"
                                EventStatusFilter.PUBLISHED -> "Published (${state.events.count { it.isPublished }})"
                                EventStatusFilter.DRAFT -> "Drafts (${state.events.count { !it.isPublished }})"
                            }
                            Box(
                                modifier = Modifier
                                    .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(50)) else Modifier)
                                    .background(if (isSelected) Primary else SurfaceCard, RoundedCornerShape(50))
                                    .border(2.dp, Outline, RoundedCornerShape(50))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) OnPrimary else OnSurfaceCard,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (filteredEvents.isEmpty()) {
                            EmptyState(
                                message = when (selectedFilter) {
                                    EventStatusFilter.ALL -> "No events created yet. Tap '+ Create' to add your first event."
                                    EventStatusFilter.PUBLISHED -> "No published events found."
                                    EventStatusFilter.DRAFT -> "No draft events found."
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredEvents, key = { it.id }) { event ->
                                    AdminEventCardItem(
                                        event = event,
                                        onEventClick = { onEventClick(event.id) },
                                        onTogglePublish = {
                                            viewModel.togglePublishStatus(event.id, event.isPublished)
                                        },
                                        onDelete = {
                                            viewModel.deleteEvent(event.id)
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
}

@Composable
private fun AdminEventCardItem(
    event: Event,
    onEventClick: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    OffsetShadowCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Outline, shape)
            .clickable(onClick = onEventClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EventCard(
                event = event,
                compact = true,
                onClick = onEventClick
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (event.isPublished) SoftYellow else SoftCoral,
                    modifier = Modifier.border(2.dp, Outline, RoundedCornerShape(50))
                ) {
                    Text(
                        text = if (event.isPublished) "PUBLISHED" else "DRAFT",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceCard,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onTogglePublish,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (event.isPublished) SoftCoral else Primary,
                            contentColor = if (event.isPublished) OnSoftCoral else OnPrimary
                        ),
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(6.dp))
                            .border(2.dp, Outline, RoundedCornerShape(6.dp))
                    ) {
                        Text(
                            text = if (event.isPublished) "Unpublish" else "Publish",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onDelete,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftCoral,
                            contentColor = OnSoftCoral
                        ),
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(6.dp))
                            .border(2.dp, Outline, RoundedCornerShape(6.dp))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.xmark_solid_full),
                            contentDescription = "Delete",
                            modifier = Modifier.size(14.dp),
                            tint = OnSoftCoral
                        )
                    }
                }
            }
        }
    }
}
