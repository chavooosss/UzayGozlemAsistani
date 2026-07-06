package com.uzaygozlem.asistan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SkyBodies
import com.uzaygozlem.asistan.astro.SunCalc
import com.uzaygozlem.asistan.data.UpcomingShower
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")

@Composable
fun MeteorDetailScreen(
    upcoming: UpcomingShower,
    observerLat: Double?,
    observerLon: Double?,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val shower = upcoming.shower

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(3000)
        }
    }

    val radiant = if (observerLat != null && observerLon != null) {
        SkyBodies.altAz(shower.radiantRaDeg, shower.radiantDecDeg, nowMs, observerLat, observerLon)
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "←",
                style = MaterialTheme.typography.headlineMedium,
                color = Palette.TextPrimary,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 14.dp),
            )
            Column {
                Text(
                    text = shower.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.TextPrimary,
                )
                Text(
                    text = "Meteor yağmuru",
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        // Zirve ve durum
        AppCard {
            SectionLabel("Zirve")
            Spacer(Modifier.height(6.dp))
            InfoLineM("Zirve gecesi", shower.peak.let { upcoming.peakDate.format(dateFormat) })
            InfoLineM(
                "Kalan",
                when (upcoming.daysLeft) {
                    0L -> "Bu gece!"
                    1L -> "Yarın"
                    else -> "${upcoming.daysLeft} gün"
                },
            )
            InfoLineM("Yoğunluk", "saatte ~${shower.zhr} meteor (ideal koşulda)")
            InfoLineM("Etkin dönem", shower.activePeriod)
            InfoLineM("Kaynağı", shower.parent)
            Spacer(Modifier.height(10.dp))
            MoonImpactChipPublic(upcoming.moonIlluminationPct)
        }
        Spacer(Modifier.height(10.dp))

        // Radyant konumu
        AppCard {
            SectionLabel("Radyant · nereye bakmalı")
            Spacer(Modifier.height(4.dp))
            Text(
                "Meteorlar ${shower.radiant} yönünden çıkıyormuş gibi görünür. " +
                    "Ama tüm gökyüzüne yayılırlar — radyanttan ~40° uzağa bakmak en iyisidir.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            if (radiant == null) {
                Text("Konum bekleniyor…", color = Palette.TextSecondary)
            } else if (radiant.second > 0) {
                Text(
                    "Şu an: ${PassCalculator.azimuthToTurkish(radiant.first.toInt())} · " +
                        "${radiant.second.toInt()}° yükseklikte",
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.TextPrimary,
                )
            } else {
                Text(
                    "Radyant şu an ufkun altında — yükselmesini bekle",
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.Gold,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // En iyi izleme
        AppCard {
            SectionLabel("En iyi izleme")
            Spacer(Modifier.height(6.dp))
            Text(
                "Meteor yağmurları için sabaha karşı (gece yarısından sonra) gökyüzü " +
                    "en verimlidir; radyant yükseldikçe daha çok meteor görünür. " +
                    "Şehir ışıklarından uzak, karanlık bir yer seç, gözlerinin " +
                    "karanlığa alışması için 20 dakika bekle ve gökyüzünün genelini tara.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            val moonNote = when {
                upcoming.moonIlluminationPct < 30 ->
                    "Zirve gecesi Ay %${upcoming.moonIlluminationPct} — karanlık gökyüzü, " +
                        "harika koşullar."
                upcoming.moonIlluminationPct < 70 ->
                    "Zirve gecesi Ay %${upcoming.moonIlluminationPct} — orta düzey ay ışığı, " +
                        "Ay batınca daha iyi olur."
                else ->
                    "Zirve gecesi Ay %${upcoming.moonIlluminationPct} — parlak Ay sönük " +
                        "meteorları yutar; yine de en parlaklar görülebilir."
            }
            StatusChip(moonNote, moonColor(upcoming.moonIlluminationPct))
        }
        Spacer(Modifier.height(10.dp))

        // Hakkında
        val info = com.uzaygozlem.asistan.data.Encyclopedia.forShower(shower.name)
        if (info.isNotEmpty()) {
            AppCard {
                SectionLabel("Hakkında")
                Spacer(Modifier.height(6.dp))
                Text(info, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
            }
            Spacer(Modifier.height(10.dp))
        }

        // Radyanta pusula
        if (radiant != null && radiant.second > 0) {
            AltAzGuideCard(
                targetAzimuthDeg = radiant.first,
                targetElevationDeg = radiant.second,
            )
        }
    }
}

@Composable
private fun InfoLineM(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
    }
}

private fun moonColor(pct: Int) = when {
    pct < 30 -> Palette.Green
    pct < 70 -> Palette.Gold
    else -> Palette.Primary
}

@Composable
private fun MoonImpactChipPublic(pct: Int) {
    StatusChip("Zirve gecesi Ay: %$pct aydınlanma", moonColor(pct))
}
