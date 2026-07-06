package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uzaygozlem.asistan.data.UpcomingShower
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

@Composable
fun MeteorsScreen(showers: List<UpcomingShower>, onShowerClick: (UpcomingShower) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        SectionLabel("Yaklaşan meteor yağmurları")
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(showers) { upcoming ->
                ShowerCard(upcoming, onClick = { onShowerClick(upcoming) })
            }
        }
    }
}

@Composable
private fun ShowerCard(upcoming: UpcomingShower, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = upcoming.shower.name,
                style = MaterialTheme.typography.titleMedium,
                color = Palette.TextPrimary,
            )
            StatusChip(
                text = when (upcoming.daysLeft) {
                    0L -> "Bu gece!"
                    1L -> "Yarın"
                    else -> "${upcoming.daysLeft} gün"
                },
                color = if (upcoming.daysLeft <= 3) Palette.Green else Palette.TextSecondary,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Zirve: ${upcoming.peakDate.format(dateFormat)}",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Radyant: ${upcoming.shower.radiant} · saatte ~${upcoming.shower.zhr} meteor (ideal koşul)",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        MoonImpactChip(upcoming.moonIlluminationPct)
    }
}

@Composable
private fun MoonImpactChip(illuminationPct: Int) {
    val (note, color) = when {
        illuminationPct < 30 ->
            "Zirve gecesi Ay %$illuminationPct · gözlem için harika" to Palette.Green
        illuminationPct < 70 ->
            "Zirve gecesi Ay %$illuminationPct · orta düzey ay ışığı" to Palette.Gold
        else ->
            "Zirve gecesi Ay %$illuminationPct · parlak Ay meteorları bastırır" to
                MaterialTheme.colorScheme.error
    }
    StatusChip(note, color)
}
