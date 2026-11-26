package com.greenstage.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.greenstage.app.R

private val RobotoMono = FontFamily(
    Font(R.font.roboto_mono_thin, FontWeight.Thin),
    Font(R.font.roboto_mono_extralight, FontWeight.ExtraLight),
    Font(R.font.roboto_mono_light, FontWeight.Light),
    Font(R.font.roboto_mono_regular, FontWeight.Normal),
    Font(R.font.roboto_mono_medium, FontWeight.Medium),
    Font(R.font.roboto_mono_semibold, FontWeight.SemiBold),
    Font(R.font.roboto_mono_bold, FontWeight.Bold)
)

private val Manrope = FontFamily(
    Font(R.font.manrope_extralight, FontWeight.ExtraLight),
    Font(R.font.manrope_light, FontWeight.Light),
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold)
)

private val DefaultTypography = Typography()

val Typography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = RobotoMono),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = RobotoMono),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = RobotoMono),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = RobotoMono),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = RobotoMono),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = RobotoMono),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = RobotoMono),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = RobotoMono),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = RobotoMono),
    bodyLarge = DefaultTypography.bodyLarge.copy(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = RobotoMono),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = Manrope),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = RobotoMono),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = Manrope),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = Manrope)
)