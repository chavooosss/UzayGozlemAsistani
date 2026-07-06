package com.uzaygozlem.asistan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.uzaygozlem.asistan.R

/**
 * Gece gözlemi için tasarlanmış "derin uzay" paleti: koyu mor-lacivert
 * zemin, kırmızı ana vurgu (gece görüşünü bozmaz), altın ikincil vurgu.
 */
object Palette {
    val BackgroundTop = Color(0xFF160F26)
    val BackgroundBottom = Color(0xFF0A0612)
    val Surface = Color(0xFF181128)
    val SurfaceHigh = Color(0xFF221937)
    val Outline = Color(0xFF2F2548)
    val Primary = Color(0xFFE5484D)
    val Gold = Color(0xFFE8B84B)
    val Green = Color(0xFF7BC67E)
    val TextPrimary = Color(0xFFEFEAF7)
    val TextSecondary = Color(0xFFA79FBC)
}

private val NightColors = darkColorScheme(
    primary = Palette.Primary,
    onPrimary = Color(0xFF1A0A0B),
    secondary = Palette.Gold,
    onSecondary = Color(0xFF201708),
    background = Palette.BackgroundBottom,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextSecondary,
    error = Color(0xFFFF6B6B),
    outline = Palette.Outline,
)

@OptIn(ExperimentalTextApi::class)
private val SpaceGrotesk = FontFamily(
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

// Başlıklar Space Grotesk, gövde metni sistem fontu (okunabilirlik).
private val AppTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold,
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
        ),
    )
}

@Composable
fun UzayGozlemTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColors,
        typography = AppTypography,
        content = content,
    )
}
