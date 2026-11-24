package com.greenstage.ui.events

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenstage.ui.components.CategoryChip
import com.greenstage.ui.components.ErrorState
import com.greenstage.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    eventId: String,
    viewModel: EventViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    Button(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        when (val state = uiState) {
            is EventUiState.Loading -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }
            is EventUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadEvent(eventId) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is EventUiState.Success -> {
                val event = state.event
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CategoryChip(category = event.category)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Venue",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = event.venue,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm", Locale.getDefault())
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Start: ${dateFormat.format(Date(event.startAt))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "End: ${dateFormat.format(Date(event.endAt))}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
