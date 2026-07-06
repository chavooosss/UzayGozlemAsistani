package com.uzaygozlem.asistan.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.SatPosEclipse
import com.github.amsacode.predict4java.TLE
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SatellitePass
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Date
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.delay

private data class LiveSatState(
    val azimuthDeg: Double,
    val elevationDeg: Double,
    val eclipsed: Boolean,
)

/**
 * Canlı takip: uydunun O ANKİ konumu her saniye SGP4 ile hesaplanır,
 * telefonun doğrultulduğu yönle karşılaştırılıp kullanıcı uyduya
 * yönlendirilir. Telefonu gökyüzüne tutup dolaştır; uydu radara girince
 * nokta ortaya gelir ve kilitlenir.
 */
@Composable
fun LiveTrackScreen(pass: SatellitePass, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    // Gözlem sırasında ekran kapanmasın
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val predictor = remember(pass) {
        try {
            PassPredictor(
                TLE(pass.tleLines.toTypedArray()),
                GroundStationPosition(pass.observerLat, pass.observerLon, pass.observerAltM),
            )
        } catch (e: Exception) {
            null
        }
    }

    var satState by remember { mutableStateOf<LiveSatState?>(null) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }

    LaunchedEffect(predictor) {
        while (true) {
            now = ZonedDateTime.now()
            predictor?.let {
                try {
                    val satPos = it.getSatPos(Date())
                    satState = LiveSatState(
                        azimuthDeg = Math.toDegrees(satPos.azimuth),
                        elevationDeg = Math.toDegrees(satPos.elevation),
                        eclipsed = SatPosEclipse.isEclipsed(satPos),
                    )
                } catch (e: Exception) {
                    // hesap hatasında eski durumu koru
                }
            }
            delay(1000)
        }
    }

    val pointing = rememberDevicePointing()

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
                    text = "● CANLI TAKİP",
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.Primary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        val state = satState
        if (predictor == null) {
            Text(
                "Yörünge verisi yüklenemedi. Listeyi yenileyip tekrar deneyin.",
                color = MaterialTheme.colorScheme.error,
            )
            return@Column
        }
        if (state == null) {
            Text("Konum hesaplanıyor…", color = Palette.TextSecondary)
            return@Column
        }

        StatusCard(pass, state, now)
        Spacer(Modifier.height(10.dp))

        AppCard {
            SectionLabel("Uydu radarı")
            Spacer(Modifier.height(4.dp))
            Text(
                "Telefonun arkasını gökyüzüne doğrult. Altın nokta uydu; " +
                    "noktayı ortadaki hedefe getir.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            if (pointing == null) {
                Text(
                    "Yön sensörü bekleniyor…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                )
            } else {
                SatelliteRadar(
                    satAzimuthDeg = state.azimuthDeg,
                    satElevationDeg = state.elevationDeg,
                    pointing = pointing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
                Spacer(Modifier.height(10.dp))
                GuidanceText(state, pointing)
            }
        }
    }
}

@Composable
private fun StatusCard(pass: SatellitePass, state: LiveSatState, now: ZonedDateTime) {
    AppCard {
        when {
            now.isBefore(pass.aos) -> {
                val remaining = Duration.between(now, pass.aos)
                SectionLabel("Ufkun altında")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Yükselişe ${remaining.toMinutes()} dk ${remaining.seconds % 60} sn",
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.TextPrimary,
                )
                Text(
                    "${PassCalculator.azimuthToTurkish(pass.aosAzimuthDeg)} ufkunu izle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                )
            }
            now.isAfter(pass.los) -> {
                SectionLabel("Geçiş sona erdi")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bir sonraki geçiş için listeye dön",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.TextSecondary,
                )
            }
            else -> {
                SectionLabel("Gökyüzünde!")
                Spacer(Modifier.height(4.dp))
                Text(
                    "${PassCalculator.azimuthToTurkish(state.azimuthDeg.toInt())} · " +
                        "${state.elevationDeg.toInt()}° yükseklikte",
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.TextPrimary,
                )
                if (state.eclipsed) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Şu an Dünya'nın gölgesinde — çıplak gözle seçilemeyebilir",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.Gold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidanceText(state: LiveSatState, pointing: DevicePointing) {
    var azDiff = (state.azimuthDeg - pointing.azimuthDeg).toFloat()
    if (azDiff > 180f) azDiff -= 360f
    if (azDiff < -180f) azDiff += 360f
    val elDiff = (state.elevationDeg - pointing.elevationDeg).toFloat()
    val locked = hypot(azDiff, elDiff) < 8f

    if (locked) {
        StatusChip("🎯 Uydu tam burada!", Palette.Green)
    } else {
        val horizontal = if (abs(azDiff) >= 4f) {
            if (azDiff > 0) "sağa dön ${abs(azDiff).toInt()}°" else "sola dön ${abs(azDiff).toInt()}°"
        } else null
        val vertical = if (abs(elDiff) >= 4f) {
            if (elDiff > 0) "yukarı kaldır ${abs(elDiff).toInt()}°" else "aşağı indir ${abs(elDiff).toInt()}°"
        } else null
        val text = listOfNotNull(horizontal, vertical).joinToString(" · ")
            .replaceFirstChar { it.uppercase() }
        StatusChip(text.ifEmpty { "Az kaldı…" }, Palette.Gold)
    }
}

/**
 * Radar görünümü: merkez = telefonun baktığı nokta, ±45°'lik görüş alanı.
 * Uydu altın nokta olarak çizilir; alan dışındaysa kenarda yön oku belirir.
 */
@Composable
private fun SatelliteRadar(
    satAzimuthDeg: Double,
    satElevationDeg: Double,
    pointing: DevicePointing,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f * 0.9f
        val center = Offset(cx, cy)
        val degToPx = radius / 45f // ±45° görüş alanı

        // Radar halkaları + artı işareti
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))
        drawCircle(Palette.Outline, radius, center, style = Stroke(2.5f))
        drawCircle(Palette.Outline, radius / 2f, center, style = Stroke(1.5f, pathEffect = dash))
        drawLine(Palette.Outline, Offset(cx - 18f, cy), Offset(cx + 18f, cy), 2f)
        drawLine(Palette.Outline, Offset(cx, cy - 18f), Offset(cx, cy + 18f), 2f)

        var azDiff = (satAzimuthDeg - pointing.azimuthDeg).toFloat()
        if (azDiff > 180f) azDiff -= 360f
        if (azDiff < -180f) azDiff += 360f
        val elDiff = (satElevationDeg - pointing.elevationDeg).toFloat()

        val rawX = azDiff * degToPx
        val rawY = -elDiff * degToPx
        val distance = hypot(rawX, rawY)
        val locked = hypot(azDiff, elDiff) < 8f
        val dotColor = if (locked) Palette.Green else Palette.Gold

        if (distance <= radius - 14f) {
            // Görüş alanında: uyduyu çiz
            val pos = Offset(cx + rawX, cy + rawY)
            drawCircle(dotColor.copy(alpha = 0.25f), 26f, pos)
            drawCircle(dotColor, 12f, pos)
        } else {
            // Alan dışında: kenarda yön göstergesi
            val scale = (radius - 14f) / distance
            val edge = Offset(cx + rawX * scale, cy + rawY * scale)
            drawCircle(dotColor.copy(alpha = 0.5f), 10f, edge)
            drawLine(
                dotColor,
                Offset(cx + rawX * scale * 0.88f, cy + rawY * scale * 0.88f),
                edge,
                strokeWidth = 6f,
                cap = StrokeCap.Round,
            )
        }

        if (locked) {
            drawCircle(Palette.Green, radius, center, style = Stroke(5f))
        }
    }
}
