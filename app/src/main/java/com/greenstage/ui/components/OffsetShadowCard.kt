package com.greenstage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.greenstage.app.ui.theme.Outline

@Composable
fun OffsetShadowCard(
    modifier: Modifier = Modifier,
    shape: Shape,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 2.dp, y = 2.dp)
                .background(Outline, shape)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            content = content
        )
    }
}
