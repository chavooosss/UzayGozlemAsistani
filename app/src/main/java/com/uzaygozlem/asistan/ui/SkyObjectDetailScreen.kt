package com.uzaygozlem.asistan.ui

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.drawText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SkyBodies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

private fun typeName(type: SkyBodies.BodyType): String = when (type) {
    SkyBodies.BodyType.PLANET -> "Gezegen"
    SkyBodies.BodyType.STAR -> "Yıldız"
    SkyBodies.BodyType.MOON -> "Ay"
}

@Composable
fun SkyObjectDetailScreen(
    objectName: String,
    observerLat: Double,
    observerLon: Double,
    onBack: () -> Unit,
    onWatch: (String) -> Unit,
    inWatchlist: Boolean,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Cismin anlık konumunu birkaç saniyede bir tazele (yavaş hareket eder)
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(3000)
        }
    }
    val current = remember(nowMs) {
        SkyBodies.positionOf(objectName, nowMs, observerLat, observerLon)
    }

    // Görünürlük penceresi (24s) — ağır olduğundan arka planda bir kez
    val visibility by produceState<SkyBodies.SkyVisibility?>(null, objectName) {
        value = withContext(Dispatchers.Default) {
            SkyBodies.visibility(objectName, System.currentTimeMillis(), observerLat, observerLon)
        }
    }

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
                    text = objectName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Palette.TextPrimary,
                )
                current?.let {
                    Text(
                        text = typeName(it.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = Palette.TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Anlık konum
        AppCard {
            SectionLabel("Şu an")
            Spacer(Modifier.height(4.dp))
            if (current == null) {
                Text("Hesaplanıyor…", color = Palette.TextSecondary)
            } else if (current.elevationDeg > 0) {
                Text(
                    "${PassCalculator.azimuthToTurkish(current.azimuthDeg.toInt())} · " +
                        "${current.elevationDeg.toInt()}° yükseklikte",
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.TextPrimary,
                )
                if (current.type != SkyBodies.BodyType.MOON) {
                    Spacer(Modifier.height(8.dp))
                    MagnitudeChip(current.magnitude)
                }
            } else {
                Text(
                    "Şu an ufkun altında",
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // Ne zaman görünür
        AppCard {
            SectionLabel("Ne zaman görünür")
            Spacer(Modifier.height(8.dp))
            val v = visibility
            if (v == null) {
                Text("Hesaplanıyor…", color = Palette.TextSecondary)
            } else {
                when (v.state) {
                    SkyBodies.RiseState.CIRCUMPOLAR ->
                        InfoLine("Durum", "Hiç batmıyor (gün boyu ufuk üstünde)")
                    SkyBodies.RiseState.NEVER_RISES ->
                        InfoLine("Durum", "Önümüzdeki 24 saatte doğmuyor")
                    else -> {
                        v.riseMs?.let { InfoLine("Doğuş", formatTime(it)) }
                        v.transitMs?.let {
                            InfoLine(
                                "Tepe",
                                "${formatTime(it)} · en yüksek ${v.transitElevationDeg.toInt()}°",
                            )
                        }
                        v.setMs?.let { InfoLine("Batış", formatTime(it)) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (v.darkFromMs != null && v.darkUntilMs != null) {
                    StatusChip(
                        "✦ En iyi görüş: ${formatTime(v.darkFromMs)}–${formatTime(v.darkUntilMs)} " +
                            "(karanlık gökyüzü)",
                        Palette.Green,
                    )
                } else {
                    StatusChip(
                        "Önümüzdeki 24 saatte karanlıkta ufuk üstünde olmuyor",
                        Palette.TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Hakkında (offline ansiklopedi)
        current?.let {
            val info = com.uzaygozlem.asistan.data.Encyclopedia.forSkyObject(
                objectName, it.type == SkyBodies.BodyType.STAR,
            )
            if (info.isNotEmpty()) {
                AppCard {
                    SectionLabel("Hakkında")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        info,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.TextPrimary,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        // Canlı pusula / radar
        if (current != null) {
            AltAzGuideCard(
                targetAzimuthDeg = current.azimuthDeg,
                targetElevationDeg = current.elevationDeg,
            )
            Spacer(Modifier.height(10.dp))
        }

        // Gökyüzü haritası (tek cisim)
        current?.let {
            AppCard {
                SectionLabel("Gökyüzündeki yeri")
                Spacer(Modifier.height(10.dp))
                SingleBodyDome(
                    obj = it,
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        androidx.compose.material3.OutlinedButton(
            onClick = { onWatch(objectName) },
            enabled = !inWatchlist,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (inWatchlist) "⭐ İzleme listesinde" else "⭐ İzleme listesine ekle")
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Palette.TextPrimary)
    }
}

private fun formatTime(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(timeFormat)

@Composable
private fun SingleBodyDome(obj: SkyBodies.SkyObject, modifier: Modifier = Modifier) {
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val dirStyle = androidx.compose.ui.text.TextStyle(
        color = Palette.TextSecondary,
        fontSize = 13.sp,
    )
    androidx.compose.foundation.Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f * 0.84f
        val center = Offset(cx, cy)
        val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 10f))

        drawCircle(Palette.Outline, radius, center, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
        drawCircle(Palette.Outline, radius * 2f / 3f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f, pathEffect = dash))
        drawCircle(Palette.Outline, radius / 3f, center, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f, pathEffect = dash))

        listOf("K" to 0.0, "D" to 90.0, "G" to 180.0, "B" to 270.0).forEach { (label, az) ->
            val angle = az * Math.PI / 180.0
            val lx = cx + (radius + 20f) * kotlin.math.sin(angle).toFloat()
            val ly = cy - (radius + 20f) * kotlin.math.cos(angle).toFloat()
            val m = textMeasurer.measure(label, dirStyle)
            drawText(m, topLeft = Offset(lx - m.size.width / 2f, ly - m.size.height / 2f))
        }

        if (obj.elevationDeg > 0) {
            val rr = radius * ((90.0 - obj.elevationDeg.coerceIn(0.0, 90.0)) / 90.0).toFloat()
            val a = obj.azimuthDeg * Math.PI / 180.0
            val pos = Offset(cx + rr * kotlin.math.sin(a).toFloat(), cy - rr * kotlin.math.cos(a).toFloat())
            val color = when (obj.type) {
                SkyBodies.BodyType.PLANET -> Palette.Gold
                SkyBodies.BodyType.STAR -> androidx.compose.ui.graphics.Color(0xFFBFD4FF)
                SkyBodies.BodyType.MOON -> androidx.compose.ui.graphics.Color(0xFFE8E3F0)
            }
            drawCircle(color.copy(alpha = 0.25f), 16f, pos)
            drawCircle(color, 8f, pos)
        }
    }
}
