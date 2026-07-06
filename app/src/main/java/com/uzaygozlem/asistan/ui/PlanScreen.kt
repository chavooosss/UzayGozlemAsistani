package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.uzaygozlem.asistan.MoonInfo
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SatellitePass
import com.uzaygozlem.asistan.astro.SkyBodies
import com.uzaygozlem.asistan.astro.SunCalc
import com.uzaygozlem.asistan.astro.Visibility
import com.uzaygozlem.asistan.data.UpcomingShower
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val HeroShape = RoundedCornerShape(20.dp)

private data class Highlight(val icon: String, val text: String, val good: Boolean = true)

@Composable
fun PlanScreen(
    passes: List<SatellitePass>,
    moon: MoonInfo?,
    showers: List<UpcomingShower>,
    observerLat: Double?,
    observerLon: Double?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (observerLat == null || observerLon == null) {
            SectionLabel("Bu gece")
            Spacer(Modifier.height(10.dp))
            Text(
                "Konum belirlenince bu gece için kişisel gözlem planın burada oluşur.",
                color = Palette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        val now = System.currentTimeMillis()
        // Gökcisimlerini karanlık bir referans anda hesapla: şu an karanlıksa
        // şimdi, değilse bu gece 22:30.
        val darkNow = SunCalc.sunElevationDeg(now, observerLat, observerLon) <
            SunCalc.KARANLIK_ESIGI_DERECE
        val refMs = if (darkNow) now else {
            LocalDate.now().atTime(22, 30).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }

        val brightPlanets = SkyBodies.allNow(refMs, observerLat, observerLon)
            .filter {
                it.type == SkyBodies.BodyType.PLANET && it.elevationDeg > 5 && it.magnitude < 3.0
            }
            .sortedBy { it.magnitude }

        val visiblePasses = passes.filter { it.visibility == Visibility.VISIBLE }
        val bestPass = visiblePasses.minByOrNull { it.magnitudeAtPeak ?: 99.0 }
        val nextShower = showers.minByOrNull { it.daysLeft }

        // --- Öne çıkan öneri (hero) ---
        val hero = buildHero(bestPass, brightPlanets, moon)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HeroShape)
                .background(Brush.linearGradient(listOf(Color(0xFF33163F), Color(0xFF1B0F2E))))
                .border(1.dp, Palette.Outline, HeroShape)
                .padding(18.dp),
        ) {
            SectionLabel("Bu gece için önerim")
            Spacer(Modifier.height(8.dp))
            Text(
                hero,
                style = MaterialTheme.typography.titleMedium,
                color = Palette.TextPrimary,
            )
        }
        Spacer(Modifier.height(14.dp))

        // --- Öne çıkanlar listesi ---
        val highlights = buildHighlights(visiblePasses, brightPlanets, moon, nextShower)
        SectionLabel("Öne çıkanlar")
        Spacer(Modifier.height(8.dp))
        highlights.forEach { h ->
            HighlightRow(h)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(10.dp))
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.material3.OutlinedButton(
            onClick = {
                val body = buildString {
                    append("🌌 Bu gece — gözlem planı\n\n")
                    append(hero)
                    append("\n\n")
                    highlights.forEach { append("${it.icon} ${it.text}\n") }
                    append("— Uzay Gözlem Asistanı")
                }
                shareText(context, body)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("↗ Planı paylaş")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Bu plan konumun, saat ve hava durumuna göre otomatik oluşturulur; " +
                "aşağı çekip yenilersen güncellenir.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
    }
}

@Composable
private fun HighlightRow(h: Highlight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.Surface)
            .border(1.dp, Palette.Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(h.icon, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(
            h.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (h.good) Palette.TextPrimary else Palette.TextSecondary,
        )
    }
}

private fun buildHero(
    bestPass: SatellitePass?,
    brightPlanets: List<SkyBodies.SkyObject>,
    moon: MoonInfo?,
): String {
    if (bestPass != null) {
        val name = bestPass.satelliteName.substringBefore(" (")
        val start = (bestPass.visibleFrom ?: bestPass.aos).format(timeFmt)
        val dir = PassCalculator.azimuthToTurkish(bestPass.aosAzimuthDeg)
        val mag = bestPass.magnitudeAtPeak?.let { " (%.1f kadir, gökyüzünün en parlak nesnelerinden)".format(it) } ?: ""
        val cloud = bestPass.cloudCoverPct?.let {
            if (it <= 30) " Gökyüzü de açık." else if (it >= 70) " Ama bulut çok, şansa bakar." else ""
        } ?: ""
        return "$start'te $name görünür geçiş yapacak$mag. $dir ufkundan çıkacak — " +
            "en dikkat çekici olay bu.$cloud"
    }
    if (brightPlanets.isNotEmpty()) {
        val p = brightPlanets.first()
        val dir = PassCalculator.azimuthToTurkish(p.azimuthDeg.toInt())
        return "Bu gece uydu geçişi öne çıkmıyor ama ${p.name} $dir yönünde parlak " +
            "şekilde görünecek — çıplak gözle kolayca seçilir."
    }
    moon?.let {
        return if (it.illuminationPct < 25) {
            "Görünür uydu geçişi yok ama Ay ince (%${it.illuminationPct.roundToInt()}) — " +
                "karanlık gökyüzü, galaksi ve bulutsu gözlemi için ideal bir gece."
        } else {
            "Bu gece öne çıkan bir olay yok. ${it.phase.emoji} ${it.phase.turkishName} " +
                "gökyüzünde; parlak yıldızları ve takımyıldızları izleyebilirsin."
        }
    }
    return "Konumuna göre bu gecenin gözlem özeti hazırlanıyor."
}

private fun buildHighlights(
    visiblePasses: List<SatellitePass>,
    brightPlanets: List<SkyBodies.SkyObject>,
    moon: MoonInfo?,
    nextShower: UpcomingShower?,
): List<Highlight> = buildList {
    if (visiblePasses.isEmpty()) {
        add(Highlight("🛰", "Önümüzdeki 24 saatte çıplak gözle görünür uydu geçişi yok.", false))
    } else {
        visiblePasses.take(3).forEach { p ->
            val name = p.satelliteName.substringBefore(" (")
            val start = (p.visibleFrom ?: p.aos).format(timeFmt)
            val dir = PassCalculator.azimuthToTurkish(p.aosAzimuthDeg)
            add(
                Highlight(
                    "🛰",
                    "$start · $name, $dir ufkundan yükselecek " +
                        "(en yüksek ${p.maxElevationDeg.roundToInt()}°).",
                ),
            )
        }
    }

    brightPlanets.take(4).forEach { p ->
        val dir = PassCalculator.azimuthToTurkish(p.azimuthDeg.toInt())
        add(Highlight("🪐", "${p.name} · $dir, ${p.elevationDeg.roundToInt()}° yükseklikte, " +
            "%.1f kadir.".format(p.magnitude)))
    }

    moon?.let {
        val note = when {
            it.illuminationPct < 25 -> "ince — karanlık gökyüzü avantajı."
            it.illuminationPct < 65 -> "yarım dolayında — orta düzey ışık."
            else -> "parlak — sönük cisimleri bastırır, ama kraterleri izlemek için harika."
        }
        add(Highlight(it.phase.emoji, "${it.phase.turkishName}, %${it.illuminationPct.roundToInt()} " +
            "aydınlanma — $note"))
    }

    nextShower?.let {
        val whenTxt = when (it.daysLeft) {
            0L -> "bu gece zirvede!"
            1L -> "yarın zirvede."
            else -> "${it.daysLeft} gün sonra zirvede."
        }
        add(Highlight("🌠", "${it.shower.name} $whenTxt (saatte ~${it.shower.zhr} meteor, " +
            "zirve gecesi Ay %${it.moonIlluminationPct}).", it.daysLeft <= 3))
    }
}
