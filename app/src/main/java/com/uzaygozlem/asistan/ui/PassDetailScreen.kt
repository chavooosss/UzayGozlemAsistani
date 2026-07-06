package com.uzaygozlem.asistan.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SatellitePass
import com.uzaygozlem.asistan.astro.Visibility
import java.time.Duration
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormat = DateTimeFormatter.ofPattern("d MMMM")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PassDetailScreen(
    pass: SatellitePass,
    onBack: () -> Unit,
    onLiveTrack: () -> Unit,
    onLogObservation: (String) -> Unit,
    onWatch: (String) -> Unit,
    inWatchlist: Boolean,
) {
    var showLogDialog by remember { mutableStateOf(false) }
    var logged by remember(pass) { mutableStateOf(false) }
    if (showLogDialog) {
        ObservationDialog(
            satelliteName = pass.satelliteName.substringBefore(" ("),
            onDismiss = { showLogDialog = false },
            onSave = { note ->
                showLogDialog = false
                logged = true
                onLogObservation(note)
            },
        )
    }
    BackHandler(onBack = onBack)
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
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 14.dp),
            )
            Column {
                Text(
                    text = pass.satelliteName.substringBefore(" ("),
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.TextPrimary,
                )
                Text(
                    text = "${pass.aos.format(dateFormat)} · " +
                        "${pass.aos.format(timeFormat)}–${pass.los.format(timeFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        AppCard {
            SectionLabel("Gökyüzü haritası")
            Spacer(Modifier.height(4.dp))
            Text(
                "Dış çember ufuk, merkez tepe noktası (zenit).",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            SkyPathMap(
                pass = pass,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LegendItem(Palette.Green, "Görünür bölüm")
                LegendItem(Palette.TextSecondary, "Görünmez bölüm")
                LegendItem(Palette.Gold, "Yükseliş noktası")
            }
        }
        Spacer(Modifier.height(10.dp))

        CompassCard(targetAzimuthDeg = pass.aosAzimuthDeg)
        Spacer(Modifier.height(10.dp))

        AppCard {
            SectionLabel("Geçiş bilgisi")
            Spacer(Modifier.height(10.dp))
            InfoRow(
                "Yükseliş",
                "${pass.aos.format(timeFormat)} · " +
                    PassCalculator.azimuthToTurkish(pass.aosAzimuthDeg),
            )
            InfoRow(
                "Zirve",
                "${pass.tca.format(timeFormat)} · ${pass.maxElevationDeg.toInt()}° yükseklik",
            )
            InfoRow(
                "Batış",
                "${pass.los.format(timeFormat)} · " +
                    PassCalculator.azimuthToTurkish(pass.losAzimuthDeg),
            )
            InfoRow(
                "Süre",
                "${Duration.between(pass.aos, pass.los).toMinutes()} dakika",
            )
            pass.cloudCoverPct?.let { InfoRow("Bulutluluk (zirve saati)", "%$it") }
            pass.magnitudeAtPeak?.let {
                InfoRow("Parlaklık (tahmini)", "%.1f kadir".format(it))
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pass.cloudCoverPct?.let { CloudChip(it) }
                VisibilityChipFor(pass)
            }
        }
        Spacer(Modifier.height(10.dp))

        val info = com.uzaygozlem.asistan.data.Encyclopedia.forSatellite(
            pass.satelliteName.substringBefore(" ("),
        )
        if (info.isNotEmpty()) {
            AppCard {
                SectionLabel("Hakkında")
                Spacer(Modifier.height(6.dp))
                Text(info, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
            }
            Spacer(Modifier.height(10.dp))
        }

        Button(
            onClick = onLiveTrack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("🛰 Canlı Takip")
        }
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { if (!logged) showLogDialog = true },
            enabled = !logged,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (logged) "✔ Günlüğe eklendi" else "✔ Gözlemledim")
        }
        Spacer(Modifier.height(8.dp))

        val context = LocalContext.current
        OutlinedButton(
            onClick = { addToCalendar(context, pass) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("📅 Takvime Ekle")
        }
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { onWatch(pass.satelliteName) },
            enabled = !inWatchlist,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (inWatchlist) "⭐ İzleme listesinde" else "⭐ İzleme listesine ekle")
        }
        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { shareText(context, formatPassForShare(pass)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("↗ Paylaş")
        }
    }
}

@Composable
private fun ObservationDialog(
    satelliteName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = { Text("$satelliteName gözlemi", color = Palette.TextPrimary) },
        text = {
            Column {
                Text(
                    "Günlüğe kaydedilecek. İstersen kısa bir not ekle:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Not (isteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}

/** Geçişi takvim uygulamasına etkinlik olarak gönderir (izin gerektirmez). */
private fun addToCalendar(context: Context, pass: SatellitePass) {
    val name = pass.satelliteName.substringBefore(" (")
    val visibleNote = if (pass.visibility == Visibility.VISIBLE) {
        "Görünür: ${pass.visibleFrom?.format(timeFormat)}–${pass.visibleUntil?.format(timeFormat)}"
    } else {
        "Bu geçiş çıplak gözle görünmeyebilir"
    }
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(
            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
            pass.aos.toInstant().toEpochMilli(),
        )
        .putExtra(
            CalendarContract.EXTRA_EVENT_END_TIME,
            pass.los.toInstant().toEpochMilli(),
        )
        .putExtra(CalendarContract.Events.TITLE, "🛰 $name geçişi")
        .putExtra(
            CalendarContract.Events.DESCRIPTION,
            "${PassCalculator.azimuthToTurkish(pass.aosAzimuthDeg)} yönünden yükselecek, " +
                "en yüksek ${pass.maxElevationDeg.toInt()}°. $visibleNote. " +
                "(Uzay Gözlem Asistanı)",
        )
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Takvim uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun VisibilityChipFor(pass: SatellitePass) {
    when (pass.visibility) {
        Visibility.VISIBLE -> StatusChip(
            "● GÖRÜNÜR · ${pass.visibleFrom?.format(timeFormat)}–" +
                "${pass.visibleUntil?.format(timeFormat)}",
            Palette.Green,
        )
        Visibility.ECLIPSED ->
            StatusChip("Görünmez · Dünya'nın gölgesinde", Palette.TextSecondary)
        Visibility.SKY_TOO_BRIGHT ->
            StatusChip("Görünmez · Gökyüzü aydınlık", Palette.TextSecondary)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextPrimary,
        )
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(10.dp).height(10.dp)) {
            drawCircle(color = color)
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Palette.TextSecondary,
        )
    }
}

/**
 * Polar gökyüzü haritası: azimut açı, yükseklik ise merkeze uzaklık olarak
 * çizilir (ufuk = dış çember, zenit = merkez). Uydu izi görünür bölümde
 * yeşil, görünmez bölümde soluk çizilir.
 */
@Composable
private fun SkyPathMap(pass: SatellitePass, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Palette.TextSecondary,
        fontSize = 13.sp,
    )
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f * 0.86f
        val center = Offset(cx, cy)

        // Ufuk + 30° ve 60° yükseklik çemberleri
        drawCircle(Palette.Outline, radius, center, style = Stroke(2.5f))
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))
        drawCircle(
            Palette.Outline, radius * 2f / 3f, center,
            style = Stroke(1.5f, pathEffect = dash),
        )
        drawCircle(
            Palette.Outline, radius / 3f, center,
            style = Stroke(1.5f, pathEffect = dash),
        )
        drawCircle(Palette.TextSecondary, 3f, center)

        // Yön etiketleri (azimut 0 = Kuzey yukarıda)
        val labels = listOf("K" to 0.0, "D" to 90.0, "G" to 180.0, "B" to 270.0)
        labels.forEach { (label, azimuth) ->
            val angle = Math.toRadians(azimuth)
            val lx = cx + (radius + 20f) * sin(angle).toFloat()
            val ly = cy - (radius + 20f) * cos(angle).toFloat()
            val measured = textMeasurer.measure(label, labelStyle)
            drawText(
                measured,
                topLeft = Offset(
                    lx - measured.size.width / 2f,
                    ly - measured.size.height / 2f,
                ),
            )
        }

        // Uydu izi: ufuk üstündeki noktalar, parça parça renklendirilir
        fun toOffset(azimuthDeg: Double, elevationDeg: Double): Offset {
            val r = radius * ((90.0 - elevationDeg.coerceIn(0.0, 90.0)) / 90.0).toFloat()
            val angle = Math.toRadians(azimuthDeg)
            return Offset(
                cx + r * sin(angle).toFloat(),
                cy - r * cos(angle).toFloat(),
            )
        }

        val aboveHorizon = pass.track.filter { it.elevationDeg > 0 }
        aboveHorizon.zipWithNext().forEach { (a, b) ->
            drawLine(
                color = if (a.visible && b.visible) Palette.Green
                else Palette.TextSecondary.copy(alpha = 0.45f),
                start = toOffset(a.azimuthDeg, a.elevationDeg),
                end = toOffset(b.azimuthDeg, b.elevationDeg),
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        }

        // Yükseliş noktası işareti
        aboveHorizon.firstOrNull()?.let {
            drawCircle(Palette.Gold, 9f, toOffset(it.azimuthDeg, it.elevationDeg))
        }
    }
}
