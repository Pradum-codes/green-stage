package com.greenstage.ui.events

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greenstage.app.R
import com.greenstage.app.ui.theme.OnPrimary
import com.greenstage.app.ui.theme.OnSecondary
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.OnSurfaceVariant
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.OutlineVariant
import com.greenstage.app.ui.theme.Primary
import com.greenstage.app.ui.theme.Secondary
import com.greenstage.app.ui.theme.SoftYellow
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.ui.components.CategoryChip
import com.greenstage.ui.components.ErrorState
import com.greenstage.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsBottomSheet(
    eventId: String,
    viewModel: EventViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (val state = uiState) {
                    is EventUiState.Loading -> LoadingIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    is EventUiState.Error -> ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadEvent(eventId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    is EventUiState.Success -> {
                        val event = state.event
                        val shape = RoundedCornerShape(8.dp)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, shape)
                                .border(2.dp, Outline, shape),
                            shape = shape,
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            if (event.imageUrl.isNullOrBlank()) {
                                Image(
                                    painter = painterResource(R.drawable.green_stage_logo),
                                    contentDescription = event.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(208.dp)
                                        .background(SurfaceCard)
                                        .padding(32.dp)
                                )
                            } else {
                                AsyncImage(
                                    model = event.imageUrl,
                                    contentDescription = event.title,
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.green_stage_logo),
                                    error = painterResource(R.drawable.green_stage_logo),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(208.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryChip(category = event.category)
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, shape)
                                .border(2.dp, Outline, shape),
                            shape = shape,
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("⌖  Venue", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                                    Text(event.venue, style = MaterialTheme.typography.bodyLarge, color = OnSurfaceCard)
                                }
                                HorizontalDivider(thickness = 2.dp, color = OutlineVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val locale = LocalConfiguration.current.locales[0]
                                    val dateFormat = SimpleDateFormat("MMM d, yyyy", locale)
                                    val timeFormat = SimpleDateFormat("h:mm a", locale)
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Starts", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                                        Text(dateFormat.format(Date(event.startAt)), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceCard)
                                        Text(timeFormat.format(Date(event.startAt)), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Ends", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                                        Text(dateFormat.format(Date(event.endAt)), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceCard)
                                        Text(timeFormat.format(Date(event.endAt)), style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "About This Event",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        state.actionError?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Static Bottom Action Buttons
            val state = (uiState as? EventUiState.Success)
            if (state != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCard)
                        .border(width = 2.dp, color = Outline)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (state.isBookmarked) viewModel.removeBookmark(eventId)
                            else viewModel.addBookmark(eventId)
                        },
                        enabled = !state.isActionLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Outline),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isBookmarked) SoftYellow else SurfaceCard,
                            contentColor = OnSurfaceCard
                        )
                    ) {
                        Text(
                            if (state.isBookmarked) "▮  Bookmarked" else "▯  Bookmark",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (state.isRegistered) viewModel.cancelRegistration(eventId)
                            else viewModel.registerForEvent(eventId)
                        },
                        enabled = !state.isActionLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Outline),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isRegistered) Secondary else Primary,
                            contentColor = if (state.isRegistered) OnSecondary else OnPrimary
                        )
                    ) {
                        Text(
                            if (state.isRegistered) "✓  Registered" else "Register",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
