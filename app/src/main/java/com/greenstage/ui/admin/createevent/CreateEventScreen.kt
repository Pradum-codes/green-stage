package com.greenstage.ui.admin.createevent

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.OnSurfaceVariant
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.Primary
import com.greenstage.app.ui.theme.Surface
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.ui.components.LoadingIndicator
import com.greenstage.ui.components.OffsetShadowCard
import com.greenstage.ui.home.ScreenHeading

@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel,
    onEventCreated: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Technology") }
    var venue by remember { mutableStateOf("") }
    var isPublished by remember { mutableStateOf(true) }

    val now = System.currentTimeMillis()
    var startAt by remember { mutableLongStateOf(now + 3_600_000L) }
    var endAt by remember { mutableLongStateOf(now + 7_200_000L) }

    val categories = listOf("Technology", "Sustainability", "Music", "Workshop", "Sports", "Social")

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetState()
            onEventCreated()
        }
    }

    Scaffold(
        containerColor = Surface,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreenHeading(
                    title = "Create Event",
                    subtitle = "Publish a new event for your community members."
                )
            }

            val formShape = RoundedCornerShape(8.dp)
            OffsetShadowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Outline, formShape),
                shape = formShape,
                colors = CardDefaults.cardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Event Title",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceCard,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                viewModel.clearError()
                            },
                            placeholder = { Text("e.g. Annual Tech Symposium") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Outline,
                                unfocusedBorderColor = Outline,
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    // Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceCard,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = category.equals(cat, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(50)) else Modifier)
                                        .background(if (isSelected) Primary else SurfaceCard, RoundedCornerShape(50))
                                        .border(2.dp, Outline, RoundedCornerShape(50))
                                        .clickable {
                                            category = cat
                                            viewModel.clearError()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) OnPrimary else OnSurfaceCard,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Venue Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Venue / Location",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceCard,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = venue,
                            onValueChange = {
                                venue = it
                                viewModel.clearError()
                            },
                            placeholder = { Text("e.g. Main Auditorium / Room 101") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Outline,
                                unfocusedBorderColor = Outline,
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    // Description Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.labelLarge,
                            color = OnSurfaceCard,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                viewModel.clearError()
                            },
                            placeholder = { Text("Describe the event agenda, topics, or speaker details...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Outline,
                                unfocusedBorderColor = Outline,
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    // Publish Immediately Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Surface, RoundedCornerShape(6.dp))
                            .border(2.dp, Outline, RoundedCornerShape(6.dp))
                            .clickable { isPublished = !isPublished }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isPublished,
                            onCheckedChange = { isPublished = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Primary,
                                checkmarkColor = OnPrimary
                            )
                        )
                        Text(
                            text = "Publish Event Immediately",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceCard
                        )
                    }

                    uiState.errorMessage?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (uiState.isLoading) {
                        LoadingIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        Button(
                            onClick = {
                                viewModel.createEvent(
                                    title = title,
                                    description = description,
                                    category = category,
                                    venue = venue,
                                    startAt = startAt,
                                    endAt = endAt,
                                    isPublished = isPublished
                                )
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = OnPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(6.dp))
                                .border(2.dp, Outline, RoundedCornerShape(6.dp))
                        ) {
                            Text(
                                text = "Create Event",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
