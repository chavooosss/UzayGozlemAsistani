package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzaygozlem.asistan.MoonInfo
import kotlin.math.roundToInt

@Composable
fun MoonScreen(moon: MoonInfo, onOpenDetail: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(text = moon.phase.emoji, fontSize = 92.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = moon.phase.turkishName,
            style = MaterialTheme.typography.headlineSmall,
            color = Palette.TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        StatusChip("Aydınlanma %${moon.illuminationPct.roundToInt()}", Palette.Gold)
        Spacer(Modifier.height(28.dp))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Takvim")
                Text(
                    "🌑 Sonraki Yeni Ay · ~${moon.daysToNewMoon.roundToInt()} gün sonra",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Palette.TextPrimary,
                )
                Text(
                    "🌕 Sonraki Dolunay · ~${moon.daysToFullMoon.roundToInt()} gün sonra",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Palette.TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Derin gökyüzü gözlemi (galaksi, bulutsu, Samanyolu) için " +
                        "Yeni Ay civarındaki geceler en iyisidir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
        }
        if (onOpenDetail != null) {
            Spacer(Modifier.height(14.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onOpenDetail,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("🌙 Ay'ın konumu ve saatleri")
            }
        }
    }
}
