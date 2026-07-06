package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.uzaygozlem.asistan.data.Observation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy · HH:mm")

@Composable
fun JournalScreen(
    observations: List<Observation>,
    onDelete: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        SectionLabel("Gözlem günlüğü · ${observations.size} kayıt")
        Spacer(Modifier.height(10.dp))

        if (observations.isEmpty()) {
            Text(
                "Henüz kayıt yok.\n\nBir uyduyu gerçekten gördüğünde, geçiş " +
                    "detayındaki \"✔ Gözlemledim\" düğmesiyle buraya ekleyebilirsin. " +
                    "Zamanla kendi gözlem geçmişin oluşur.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(observations) { obs -> ObservationCard(obs, onDelete) }
            }
        }
    }
}

@Composable
private fun ObservationCard(obs: Observation, onDelete: (Long) -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "✔ ${obs.satelliteName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.Green,
                )
                Text(
                    text = Instant.ofEpochMilli(obs.timestampMs)
                        .atZone(ZoneId.systemDefault())
                        .format(dateFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
            Text(
                text = "🗑",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .clickable { onDelete(obs.id) }
                    .padding(6.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "En yüksek ${obs.maxElevationDeg}°" +
                (obs.magnitude?.let { " · ✦ %.1f kadir".format(it) } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextPrimary,
        )
        if (obs.note.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "“${obs.note}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                ),
                color = Palette.TextSecondary,
            )
        }
    }
}
