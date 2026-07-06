package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uzaygozlem.asistan.UiState
import com.uzaygozlem.asistan.astro.Visibility
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val HeroShape = RoundedCornerShape(24.dp)

/**
 * Ana ekranın üstündeki "bu gece" özeti: çıplak gözle görünür ilk geçiş,
 * yoksa neden yok bilgisi + Ay durumu.
 */
@Composable
fun HomeSummary(state: UiState, modifier: Modifier = Modifier) {
    val firstVisible = state.passes.firstOrNull { it.visibility == Visibility.VISIBLE }

    val headline = when {
        state.loading && state.passes.isEmpty() -> "Gökyüzü hesaplanıyor…"
        firstVisible != null ->
            "${firstVisible.satelliteName.substringBefore(" (")} · " +
                "${(firstVisible.visibleFrom ?: firstVisible.aos).format(timeFormat)} · " +
                "en yüksek ${firstVisible.maxElevationDeg.roundToInt()}°"
        state.passes.isNotEmpty() ->
            "Çıplak gözle görünür geçiş yok (24 saat)"
        else -> "Uzay Gözlem Asistanı"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF33163F), Color(0xFF1B0F2E)),
                ),
            )
            .border(1.dp, Palette.Outline, HeroShape)
            .padding(18.dp),
    ) {
        SectionLabel(if (firstVisible != null) "Bu gece · İlk görünür geçiş" else "Bu gece")
        Spacer(Modifier.height(6.dp))
        Text(
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            color = Palette.TextPrimary,
        )
        state.moon?.let { moon ->
            Spacer(Modifier.height(6.dp))
            val cloudPart = firstVisible?.cloudCoverPct?.let { " · ☁ %$it bulut" } ?: ""
            Text(
                text = "${moon.phase.emoji} ${moon.phase.turkishName} · " +
                    "aydınlanma %${moon.illuminationPct.roundToInt()}$cloudPart",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
        }
    }
}
