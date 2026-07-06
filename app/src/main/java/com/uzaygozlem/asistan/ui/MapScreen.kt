package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.Satellite
import com.github.amsacode.predict4java.SatelliteFactory
import com.github.amsacode.predict4java.TLE
import com.uzaygozlem.asistan.R
import com.uzaygozlem.asistan.SatelliteTle
import com.uzaygozlem.asistan.astro.SkyBodies
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class MapSatellite(val name: String, val latDeg: Double, val lonDeg: Double)

private val REFERENCE_GS = GroundStationPosition(0.0, 0.0, 0.0)

/**
 * Canlı dünya haritası: seçili uyduların anlık yer izdüşümü (5 sn'de bir
 * güncellenir) + ilk uydunun ±45 dakikalık yer izi. Equirectangular
 * projeksiyon: x = boylam, y = enlem.
 */
@Composable
fun MapScreen(
    tles: List<SatelliteTle>,
    observerLat: Double? = null,
    observerLon: Double? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        SectionLabel("Canlı uydu haritası")
        Spacer(Modifier.height(10.dp))

        if (tles.isEmpty()) {
            Text(
                "Yörünge verisi henüz yok. Geçişler sekmesi yüklendikten sonra " +
                    "harita aktifleşir.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
            )
            return@Column
        }

        val satellites = remember(tles) {
            tles.mapNotNull { tle ->
                try {
                    tle.displayName.substringBefore(" (") to
                        SatelliteFactory.createSatellite(TLE(tle.lines.toTypedArray()))
                } catch (e: Exception) {
                    null
                }
            }
        }

        var positions by remember { mutableStateOf<List<MapSatellite>>(emptyList()) }
        var groundTrack by remember { mutableStateOf<List<Offset>>(emptyList()) }

        LaunchedEffect(satellites) {
            while (true) {
                withContext(Dispatchers.Default) {
                    val now = System.currentTimeMillis()
                    positions = satellites.mapNotNull { (name, sat) ->
                        satPositionAt(sat, now)?.let { (lat, lon) ->
                            MapSatellite(name, lat, lon)
                        }
                    }
                    // İlk uydunun ±45 dk yer izi (2 dk adım)
                    groundTrack = satellites.firstOrNull()?.let { (_, sat) ->
                        (-45..45 step 2).mapNotNull { minute ->
                            satPositionAt(sat, now + minute * 60_000L)
                                ?.let { (lat, lon) ->
                                    Offset(lon.toFloat(), lat.toFloat())
                                }
                        }
                    } ?: emptyList()
                }
                delay(5000)
            }
        }

        val textMeasurer = rememberTextMeasurer()
        val labelStyle = TextStyle(color = Color(0xFFEFEAF7), fontSize = 11.sp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1280f / 644f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0A0814)),
        ) {
            Image(
                painter = painterResource(R.drawable.world_map),
                contentDescription = "Dünya haritası",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds,
                alpha = 0.45f,
            )
            Canvas(Modifier.fillMaxSize()) {
                fun toPx(latDeg: Double, lonDeg: Double): Offset = Offset(
                    ((lonDeg + 180.0) / 360.0 * size.width).toFloat(),
                    ((90.0 - latDeg) / 180.0 * size.height).toFloat(),
                )

                // Gündüz/gece gölgesi: gece tarafını karart, alacakaranlığı ara ton
                val (subLat, subLon) = SkyBodies.sunGroundPoint(System.currentTimeMillis())
                val cols = 48; val rows = 24
                val cw = size.width / cols; val ch = size.height / rows
                for (cxi in 0 until cols) {
                    for (cyi in 0 until rows) {
                        val lon = -180.0 + (cxi + 0.5) / cols * 360.0
                        val lat = 90.0 - (cyi + 0.5) / rows * 180.0
                        val sunEl = SkyBodies.sunElevationAt(subLat, subLon, lat, lon)
                        val alpha = when {
                            sunEl < -12 -> 0.55f
                            sunEl < 0 -> 0.35f
                            sunEl < 6 -> 0.15f
                            else -> 0f
                        }
                        if (alpha > 0f) {
                            drawRect(
                                Color(0xFF060410).copy(alpha = alpha),
                                topLeft = Offset(cxi * cw, cyi * ch),
                                size = androidx.compose.ui.geometry.Size(cw + 1f, ch + 1f),
                            )
                        }
                    }
                }
                // Güneşin tepede olduğu nokta
                val sunPos = toPx(subLat, subLon)
                drawCircle(Color(0xFFFFD873).copy(alpha = 0.3f), 18f, sunPos)
                drawCircle(Color(0xFFFFD873), 8f, sunPos)

                // Yer izi: tarih değiştirme çizgisinde kırılır
                groundTrack.zipWithNext().forEach { (a, b) ->
                    if (kotlin.math.abs(a.x - b.x) < 180f) {
                        drawLine(
                            Palette.Primary.copy(alpha = 0.7f),
                            toPx(a.y.toDouble(), a.x.toDouble()),
                            toPx(b.y.toDouble(), b.x.toDouble()),
                            strokeWidth = 3f,
                        )
                    }
                }

                positions.forEachIndexed { index, sat ->
                    val pos = toPx(sat.latDeg, sat.lonDeg)
                    val color = if (index == 0) Palette.Primary else Palette.Gold
                    drawCircle(color.copy(alpha = 0.3f), 16f, pos)
                    drawCircle(color, 7f, pos)
                    val measured = textMeasurer.measure(sat.name, labelStyle)
                    drawText(
                        measured,
                        topLeft = Offset(
                            (pos.x + 12f).coerceAtMost(size.width - measured.size.width),
                            (pos.y - measured.size.height / 2f)
                                .coerceIn(0f, size.height - measured.size.height),
                        ),
                    )
                }

                // Gözlemci konumu
                if (observerLat != null && observerLon != null) {
                    val me = toPx(observerLat, observerLon)
                    drawCircle(Color(0xFF7BC67E).copy(alpha = 0.35f), 14f, me)
                    drawCircle(Color(0xFF7BC67E), 6f, me)
                    drawCircle(Color.White, 2.5f, me)
                    val meLabel = textMeasurer.measure("Siz", labelStyle)
                    drawText(
                        meLabel,
                        topLeft = Offset(
                            (me.x + 10f).coerceAtMost(size.width - meLabel.size.width),
                            (me.y + 6f).coerceAtMost(size.height - meLabel.size.height),
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Karanlık bölge: gecenin olduğu yer · sarı nokta: Güneşin tam tepede " +
                "olduğu yer · yeşil: sizin konumunuz. Kırmızı iz: " +
                "${positions.firstOrNull()?.name ?: "ilk uydu"} yer izi (±45 dk). " +
                "5 saniyede bir güncellenir.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
    }
}

/** Uydunun verilen andaki yer izdüşümü (enlem, boylam derece). */
private fun satPositionAt(satellite: Satellite, timeMs: Long): Pair<Double, Double>? {
    return try {
        val pos = satellite.getPosition(REFERENCE_GS, Date(timeMs))
        val lat = Math.toDegrees(pos.latitude)
        var lon = Math.toDegrees(pos.longitude)
        if (lon > 180.0) lon -= 360.0
        lat to lon
    } catch (e: Exception) {
        null
    }
}
