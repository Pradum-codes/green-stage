package com.greenstage.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenstage.app.R
import com.greenstage.app.ui.theme.OnPrimary
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.Primary
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.ui.components.EmptyState
import com.greenstage.ui.components.ErrorState
import com.greenstage.ui.components.EventCard
import com.greenstage.ui.components.LoadingIndicator
import com.greenstage.ui.home.ScreenHeading

@Composable
fun ExploreEventsScreen(
    viewModel: ExploreEventsViewModel,
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is ExploreEventsUiState.Loading -> LoadingIndicator(
                modifier = Modifier.padding(paddingValues)
            )
            is ExploreEventsUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.loadEvents() },
                modifier = Modifier.padding(paddingValues)
            )
            is ExploreEventsUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ScreenHeading(
                    title = "Explore Events",
                    subtitle = "Search by keyword or filter by category."
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard, RoundedCornerShape(6.dp))
                        .border(2.dp, Outline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.magnifying_glass_solid_full),
                        contentDescription = "Search",
                        modifier = Modifier.size(18.dp),
                        tint = OnSurfaceCard
                    )
                    androidx.compose.foundation.text.BasicTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurfaceCard),
                        decorationBox = { inner ->
                            if (state.searchQuery.isEmpty()) Text("Search events by keyword...", color = com.greenstage.app.ui.theme.OnSurfaceVariant)
                            inner()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                val categories = listOf("All", "Sustainability", "Technology", "Music", "Workshop")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = (state.selectedCategory == null && category == "All") ||
                            state.selectedCategory.equals(category, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(50)) else Modifier)
                                .background(if (isSelected) Primary else SurfaceCard, RoundedCornerShape(50))
                                .border(2.dp, Outline, RoundedCornerShape(50))
                                .clickable {
                                viewModel.onCategorySelected(if (category == "All") null else category)
                            }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
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
                    if (state.events.isEmpty()) {
                        EmptyState(
                            message = "No matching events. Try another keyword or clear your category filter.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
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
