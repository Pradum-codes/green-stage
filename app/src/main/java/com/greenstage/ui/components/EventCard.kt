package com.greenstage.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greenstage.app.R
import com.greenstage.app.ui.theme.OnSurfaceCard
import com.greenstage.app.ui.theme.OnSurfaceVariant
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.SurfaceCard
import com.greenstage.data.model.Event
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun EventCard(
    event: Event,
    onClick: (() -> Unit)? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(8.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, cardShape)
            .border(2.dp, Outline, cardShape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard,
            contentColor = OnSurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventImage(
                    event = event,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(2.dp, Outline, RoundedCornerShape(6.dp))
                )
                EventDescription(event = event, compact = true, modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                EventImage(
                    event = event,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .border(
                            width = 2.dp,
                            color = Outline,
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                )
                EventDescription(
                    event = event,
                    compact = false,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EventImage(event: Event, modifier: Modifier = Modifier) {
    if (event.imageUrl.isNullOrBlank()) {
        Image(
            painter = painterResource(R.drawable.green_stage_logo),
            contentDescription = event.title,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .background(SurfaceCard)
                .padding(12.dp)
        )
    } else {
        AsyncImage(
            model = event.imageUrl,
            contentDescription = event.title,
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.green_stage_logo),
            error = painterResource(R.drawable.green_stage_logo),
            modifier = modifier
        )
    }
}

@Composable
private fun EventDescription(event: Event, compact: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
    ) {
        CategoryChip(category = event.category)
        Text(
            text = event.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = OnSurfaceCard,
            maxLines = if (compact) 2 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis
        )
        val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", LocalConfiguration.current.locales[0])
        val startDateStr = dateFormat.format(Date(event.startAt))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
            text = "◷",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            )
            Text(startDateStr, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⌖", style = MaterialTheme.typography.bodyMedium, color = OnSurfaceCard)
            Text(
            text = event.venue,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            maxLines = if (compact) 1 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis
            )
        }
    }
}
