package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SkyBodies
import com.uzaygozlem.asistan.astro.SunCalc
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private fun bodyColor(type: SkyBodies.BodyType): Color = when (type) {
    SkyBodies.BodyType.PLANET -> Palette.Gold
    SkyBodies.BodyType.STAR -> Color(0xFFBFD4FF)
    SkyBodies.BodyType.MOON -> Color(0xFFE8E3F0)
}

private fun typeLabel(type: SkyBodies.BodyType): String = when (type) {
    SkyBodies.BodyType.PLANET -> "Gezegen"
    SkyBodies.BodyType.STAR -> "Yıldız"
    SkyBodies.BodyType.MOON -> "Uydu (Ay)"
}

@Composable
fun SkyScreen(
    observerLat: Double?,
    observerLon: Double?,
    onObjectClick: (String) -> Unit,
) {
    if (observerLat == null || observerLon == null) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            SectionLabel("Gökyüzü")
            Spacer(Modifier.height(10.dp))
            Text(
                "Konum belirlenince o an gökyüzünde görünen gezegenler ve " +
                    "yıldızlar burada listelenir.",
                color = Palette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    // Dakikada bir tazele (gökcisimleri yavaş hareket eder)
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(60_000)
        }
    }

    val allVisible = remember(nowMs, observerLat, observerLon) {
        SkyBodies.visibleNow(nowMs, observerLat, observerLon)
            .filter { it.magnitude <= 6.0 }
    }
    val daylight = remember(nowMs, observerLat, observerLon) {
        SunCalc.sunElevationDeg(nowMs, observerLat, observerLon) > -6.0
    }

    var directionFilter by remember { mutableStateOf<String?>(null) }
    val visible = if (directionFilter == null) allVisible
        else allVisible.filter { quadrant(it.azimuthDeg) == directionFilter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        SectionLabel("Şu an gökyüzünde · ${visible.size} cisim")
        Spacer(Modifier.height(6.dp))
        if (daylight) {
            Text(
                "☀ Gökyüzü şu an aydınlık — konumlar doğru ama cisimler " +
                    "hava kararınca görünür olur.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Gold,
            )
            Spacer(Modifier.height(6.dp))
        }
        DirectionFilterRow(
            selected = directionFilter,
            onSelect = { directionFilter = it },
        )
        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AppCard {
                    SectionLabel("Gökyüzü haritası")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Dış çember ufuk, merkez tepe noktası. Altın: gezegen, " +
                            "mavi: yıldız, beyaz: Ay.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    SkyDome(
                        objects = visible,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                }
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        "Şu an ufkun üstünde parlak cisim yok.",
                        color = Palette.TextSecondary,
                    )
                }
            } else {
                items(visible) { obj ->
                    SkyObjectRow(obj, onClick = { onObjectClick(obj.name) })
                }
            }
        }
    }
}

/** Azimutu 4 ana yöne indirger (her yön ±45°). */
private fun quadrant(az: Double): String {
    val a = ((az % 360) + 360) % 360
    return when {
        a >= 315 || a < 45 -> "Kuzey"
        a < 135 -> "Doğu"
        a < 225 -> "Güney"
        else -> "Batı"
    }
}

@Composable
private fun DirectionFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    val options = listOf(null to "Tümü", "Kuzey" to "Kuzey", "Doğu" to "Doğu",
        "Güney" to "Güney", "Batı" to "Batı")
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val active = selected == value
            Box(
                Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(
                        if (active) Palette.Primary.copy(alpha = 0.18f) else Palette.Surface,
                    )
                    .border(
                        1.dp,
                        if (active) Palette.Primary else Palette.Outline,
                        androidx.compose.foundation.shape.RoundedCornerShape(50),
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Palette.Primary else Palette.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SkyObjectRow(obj: SkyBodies.SkyObject, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = obj.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = bodyColor(obj.type),
                )
                Text(
                    text = "${typeLabel(obj.type)} · " +
                        "${PassCalculator.azimuthToTurkish(obj.azimuthDeg.toInt())} · " +
                        "${obj.elevationDeg.toInt()}° yükseklik",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
            if (obj.type != SkyBodies.BodyType.MOON) {
                MagnitudeChip(obj.magnitude)
            }
        }
    }
}

/**
 * Polar gökyüzü kubbesi: azimut açı, yükseklik merkeze uzaklık
 * (ufuk = dış çember, zenit = merkez). Parlaklığa göre nokta boyutu değişir.
 */
@Composable
private fun SkyDome(objects: List<SkyBodies.SkyObject>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val dirStyle = TextStyle(color = Palette.TextSecondary, fontSize = 13.sp)
    val labelStyle = TextStyle(color = Palette.TextPrimary, fontSize = 11.sp)

    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f * 0.86f
        val center = Offset(cx, cy)
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))

        drawCircle(Palette.Outline, radius, center, style = Stroke(2.5f))
        drawCircle(Palette.Outline, radius * 2f / 3f, center, style = Stroke(1.5f, pathEffect = dash))
        drawCircle(Palette.Outline, radius / 3f, center, style = Stroke(1.5f, pathEffect = dash))

        listOf("K" to 0.0, "D" to 90.0, "G" to 180.0, "B" to 270.0).forEach { (label, az) ->
            val angle = az * Math.PI / 180.0
            val lx = cx + (radius + 20f) * sin(angle).toFloat()
            val ly = cy - (radius + 20f) * cos(angle).toFloat()
            val m = textMeasurer.measure(label, dirStyle)
            drawText(m, topLeft = Offset(lx - m.size.width / 2f, ly - m.size.height / 2f))
        }

        fun toOffset(az: Double, el: Double): Offset {
            val rr = radius * ((90.0 - el.coerceIn(0.0, 90.0)) / 90.0).toFloat()
            val a = az * Math.PI / 180.0
            return Offset(cx + rr * sin(a).toFloat(), cy - rr * cos(a).toFloat())
        }

        objects.forEach { obj ->
            val pos = toOffset(obj.azimuthDeg, obj.elevationDeg)
            val color = bodyColor(obj.type)
            // parlaklık → yarıçap (parlak = büyük)
            val dotR = when {
                obj.magnitude < 0 -> 8f
                obj.magnitude < 1.5 -> 6f
                obj.magnitude < 3 -> 4.5f
                else -> 3f
            }
            drawCircle(color.copy(alpha = 0.25f), dotR + 5f, pos)
            drawCircle(color, dotR, pos)
            // Sadece parlak cisimleri etiketle (kalabalık olmasın)
            if (obj.magnitude < 1.6 || obj.type != SkyBodies.BodyType.STAR) {
                val shortName = obj.name.substringBefore(" (")
                val m = textMeasurer.measure(shortName, labelStyle)
                drawText(
                    m,
                    topLeft = Offset(
                        (pos.x + dotR + 4f).coerceAtMost(size.width - m.size.width),
                        (pos.y - m.size.height / 2f).coerceIn(0f, size.height - m.size.height),
                    ),
                )
            }
        }
    }
}
