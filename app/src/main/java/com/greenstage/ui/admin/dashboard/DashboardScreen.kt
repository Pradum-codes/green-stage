package com.greenstage.ui.admin.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenstage.app.ui.theme.OnPrimary
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.OutlineVariant
import com.greenstage.app.ui.theme.PowderBlue
import com.greenstage.app.ui.theme.Primary
import com.greenstage.app.ui.theme.SoftYellow
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.ui.components.CategoryChip
import com.greenstage.ui.components.EmptyState
import com.greenstage.ui.components.ErrorState
import com.greenstage.ui.components.EventCard
import com.greenstage.ui.components.LoadingIndicator
import com.greenstage.ui.components.OffsetShadowCard
import com.greenstage.ui.home.ScreenHeading

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCreateEventClick: () -> Unit,
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        containerColor = Surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is DashboardUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadDashboard() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is DashboardUiState.Success -> {
                val publishedCount = state.events.count { it.isPublished }
                val totalCount = state.events.size

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ScreenHeading(
                        title = "Dashboard",
                        subtitle = "Community metrics and recent event activity."
                    )

                    // Community Info Neubrutalism Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.community.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OnSurfaceCard,
                                    fontWeight = FontWeight.Bold
                                )
                                CategoryChip(category = state.community.category)
                            }

                            Text(
                                text = state.community.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = com.greenstage.app.ui.theme.OnSurfaceVariant
                            )

                            HorizontalDivider(
                                thickness = 2.dp,
                                color = OutlineVariant
                            )

                            // Metrics Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricBadge(
                                    label = "Total Events",
                                    value = totalCount.toString(),
                                    backgroundColor = PowderBlue,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBadge(
                                    label = "Published",
                                    value = publishedCount.toString(),
                                    backgroundColor = SoftYellow,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Section Header + Create Event Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Events",
                            style = MaterialTheme.typography.titleMedium,
                            color = OnSurfaceCard,
                            fontWeight = FontWeight.Bold
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
                            Text("+ Create Event", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (state.events.isEmpty()) {
                        EmptyState(
                            message = "No events created yet. Tap '+ Create Event' to publish your first community event.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.events, key = { it.id }) { event ->
                                EventCard(
                                    event = event,
                                    onClick = { onEventClick(event.id) },
                                    compact = true
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
private fun MetricBadge(
    label: String,
    value: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.border(2.dp, Outline, RoundedCornerShape(6.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceCard
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceCard
            )
        }
    }
}
