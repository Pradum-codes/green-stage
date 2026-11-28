package com.greenstage.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenstage.app.ui.theme.OnPowderBlue
import com.greenstage.app.ui.theme.OnSoftCoral
import com.greenstage.app.ui.theme.OnSoftLavender
import com.greenstage.app.ui.theme.OnSoftYellow
import com.greenstage.app.ui.theme.Outline
import com.greenstage.app.ui.theme.PowderBlue
import com.greenstage.app.ui.theme.SoftCoral
import com.greenstage.app.ui.theme.SoftLavender
import com.greenstage.app.ui.theme.SoftYellow

@Composable
fun CategoryChip(
    category: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (category.lowercase()) {
        "technology", "tech" -> PowderBlue to OnPowderBlue
        "music" -> SoftCoral to OnSoftCoral
        "workshop" -> SoftLavender to OnSoftLavender
        "sustainability" -> SoftYellow to OnSoftYellow
        else -> SoftYellow to OnSoftYellow
    }

    Surface(
        modifier = modifier.border(2.dp, Outline, CircleShape),
        shape = CircleShape,
        color = bgColor
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
