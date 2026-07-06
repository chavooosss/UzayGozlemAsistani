package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardShape = RoundedCornerShape(20.dp)
private val ChipShape = RoundedCornerShape(50)

/** Uygulamanın standart kartı: yumuşak köşe, ince kontur, düz yüzey. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Outline, CardShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        content = content,
    )
}

/** Durum çipi: yarı saydam renkli zemin üstünde aynı renk etiket. */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/** Hap biçimli sekme çubuğu; sekme sayısı sığmazsa yatay kaydırılır. */
@Composable
fun PillTabs(
    titles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .clip(ChipShape)
            .background(Palette.Surface)
            .border(1.dp, Palette.Outline, ChipShape)
            .padding(4.dp)
            .horizontalScroll(rememberScrollState()),
    ) {
        titles.forEachIndexed { index, title ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .clip(ChipShape)
                    .background(
                        if (isSelected) Palette.Primary.copy(alpha = 0.18f)
                        else Color.Transparent,
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Palette.Primary else Palette.TextSecondary,
                )
            }
        }
    }
}

/**
 * Gece modu düğmesi: aktifken göz karanlık adaptasyonunu koruyan
 * kırmızı ekran filtresini açar.
 */
@Composable
fun NightModeButton(active: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(ChipShape)
            .background(if (active) Color(0xFF3A0F12) else Palette.Surface)
            .border(
                1.dp,
                if (active) Color(0xFF7A2A2E) else Palette.Outline,
                ChipShape,
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "☾",
            fontSize = 20.sp,
            color = if (active) Color(0xFFFF5A5A) else Palette.TextSecondary,
        )
    }
}

/** Bulutluluk çipi: gözlem kalitesine göre renklenir. */
@Composable
fun CloudChip(pct: Int, modifier: Modifier = Modifier) {
    val color = when {
        pct <= 30 -> Palette.Green
        pct <= 70 -> Palette.Gold
        else -> Color(0xFFFF6B6B)
    }
    StatusChip("☁ %$pct bulut", color, modifier)
}

/**
 * Parlaklık çipi: kadir değeri (küçük = parlak). ISS tepede -3 civarına
 * çıkabilir; +3'ten sönükler şehirde zor seçilir.
 */
@Composable
fun MagnitudeChip(magnitude: Double, modifier: Modifier = Modifier) {
    val (label, color) = when {
        magnitude <= -1.5 -> "çok parlak" to Palette.Green
        magnitude <= 1.0 -> "parlak" to Palette.Green
        magnitude <= 3.0 -> "orta" to Palette.Gold
        else -> "sönük" to Palette.TextSecondary
    }
    StatusChip("✦ %.1f kadir · %s".format(magnitude, label), color, modifier)
}

/** Bölüm başlığı: küçük, aralıklı, ikincil renkte etiket. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Palette.TextSecondary,
        modifier = modifier,
    )
}
