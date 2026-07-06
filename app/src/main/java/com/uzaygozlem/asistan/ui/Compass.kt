package com.uzaygozlem.asistan.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uzaygozlem.asistan.astro.PassCalculator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Cihazın manyetik kuzeye göre yönünü (derece) canlı verir.
 * Rotation vector sensörü + yumuşatma (jitter'ı önlemek için).
 * Sensör yoksa null döner.
 */
@Composable
fun rememberDeviceHeading(): Float? {
    val context = LocalContext.current
    var heading by remember { mutableStateOf<Float?>(null) }
    var sensorMissing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val raw = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0)
                    .toFloat()
                val previous = heading
                heading = if (previous == null) {
                    raw
                } else {
                    // en kısa açı farkı üzerinden yumuşatma
                    var diff = raw - previous
                    if (diff > 180f) diff -= 360f
                    if (diff < -180f) diff += 360f
                    (previous + 0.25f * diff + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorMissing = true
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return if (sensorMissing) null else heading
}

/** Cihazın (arka kamera yönünün) baktığı nokta: azimut + yükseklik (derece). */
data class DevicePointing(val azimuthDeg: Float, val elevationDeg: Float)

/**
 * Telefonun arka yüzünün gökyüzünde nereye doğrultulduğunu canlı verir.
 * Telefonu gökyüzüne tutup uydu ararken kullanılır (canlı takip modu).
 */
@Composable
fun rememberDevicePointing(): DevicePointing? {
    val context = LocalContext.current
    var pointing by remember { mutableStateOf<DevicePointing?>(null) }

    DisposableEffect(Unit) {
        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)

            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                // Arka kameranın dünya koordinatlarındaki yönü: R * (0,0,-1)
                // (X=doğu, Y=kuzey, Z=yukarı)
                val wx = -rotationMatrix[2]
                val wy = -rotationMatrix[5]
                val wz = -rotationMatrix[8]
                val rawAz = ((Math.toDegrees(
                    kotlin.math.atan2(wx.toDouble(), wy.toDouble()),
                ) + 360.0) % 360.0).toFloat()
                val rawEl = Math.toDegrees(
                    kotlin.math.asin(wz.toDouble().coerceIn(-1.0, 1.0)),
                ).toFloat()

                val previous = pointing
                pointing = if (previous == null) {
                    DevicePointing(rawAz, rawEl)
                } else {
                    var azDiff = rawAz - previous.azimuthDeg
                    if (azDiff > 180f) azDiff -= 360f
                    if (azDiff < -180f) azDiff += 360f
                    DevicePointing(
                        azimuthDeg = (previous.azimuthDeg + 0.3f * azDiff + 360f) % 360f,
                        elevationDeg = previous.elevationDeg +
                            0.3f * (rawEl - previous.elevationDeg),
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return pointing
}

/**
 * Canlı pusula kartı: kadran cihazla birlikte döner, altın ok hedef yönü
 * (uydunun yükseliş azimutu) gösterir. Telefonun üstü hedefe dönünce
 * ok yeşile döner.
 */
@Composable
fun CompassCard(targetAzimuthDeg: Int, modifier: Modifier = Modifier) {
    val heading = rememberDeviceHeading()

    AppCard(modifier = modifier) {
        SectionLabel("Pusula · Yükseliş yönü")
        Spacer(Modifier.height(4.dp))

        if (heading == null) {
            Text(
                "Yön sensörü bekleniyor… (cihazda pusula sensörü yoksa bu " +
                    "özellik çalışmaz)",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
            return@AppCard
        }

        var diff = targetAzimuthDeg - heading
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        val aligned = abs(diff) < 12f

        CompassDial(
            headingDeg = heading,
            targetAzimuthDeg = targetAzimuthDeg.toFloat(),
            aligned = aligned,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
        )
        Spacer(Modifier.height(10.dp))
        if (aligned) {
            StatusChip(
                "✨ Doğru yöne bakıyorsun · " +
                    "${PassCalculator.azimuthToTurkish(targetAzimuthDeg)} ($targetAzimuthDeg°)",
                Palette.Green,
            )
        } else {
            StatusChip(
                "Telefonun üstünü ${if (diff > 0) "sağa →" else "← sola"} çevir " +
                    "(${abs(diff).toInt()}°)",
                Palette.Gold,
            )
        }
    }
}

/**
 * Bir gökcismini (verilen azimut+yükseklik) telefonu doğrultarak bulmaya
 * yarayan canlı radar kartı. Uydu canlı takibiyle aynı mantık, gezegen/
 * yıldız/Ay için. Cismin konumu yavaş değiştiğinden çağıran taraf az/el'i
 * belli aralıklarla güncelleyebilir.
 */
@Composable
fun AltAzGuideCard(
    targetAzimuthDeg: Double,
    targetElevationDeg: Double,
    modifier: Modifier = Modifier,
) {
    val pointing = rememberDevicePointing()

    AppCard(modifier = modifier) {
        SectionLabel("Telefonu doğrult")
        Spacer(Modifier.height(4.dp))
        Text(
            "Telefonun arkasını gökyüzüne tut; altın noktayı ortadaki hedefe getir.",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
        Spacer(Modifier.height(10.dp))
        if (targetElevationDeg <= 0) {
            Text(
                "Bu cisim şu an ufkun altında — doğuşunu bekle.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.Gold,
            )
            return@AppCard
        }
        if (pointing == null) {
            Text(
                "Yön sensörü bekleniyor…",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
            )
            return@AppCard
        }
        AltAzRadar(
            targetAzimuthDeg = targetAzimuthDeg,
            targetElevationDeg = targetElevationDeg,
            pointing = pointing,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
        )
        Spacer(Modifier.height(10.dp))

        var azDiff = (targetAzimuthDeg - pointing.azimuthDeg).toFloat()
        if (azDiff > 180f) azDiff -= 360f
        if (azDiff < -180f) azDiff += 360f
        val elDiff = (targetElevationDeg - pointing.elevationDeg).toFloat()
        val locked = hypot(azDiff, elDiff) < 8f
        if (locked) {
            StatusChip("🎯 Tam burada!", Palette.Green)
        } else {
            val h = if (abs(azDiff) >= 4f) {
                if (azDiff > 0) "sağa dön ${abs(azDiff).toInt()}°" else "sola dön ${abs(azDiff).toInt()}°"
            } else null
            val v = if (abs(elDiff) >= 4f) {
                if (elDiff > 0) "yukarı ${abs(elDiff).toInt()}°" else "aşağı ${abs(elDiff).toInt()}°"
            } else null
            StatusChip(
                listOfNotNull(h, v).joinToString(" · ")
                    .replaceFirstChar { it.uppercase() }.ifEmpty { "Az kaldı…" },
                Palette.Gold,
            )
        }
    }
}

@Composable
private fun AltAzRadar(
    targetAzimuthDeg: Double,
    targetElevationDeg: Double,
    pointing: DevicePointing,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f * 0.9f
        val center = Offset(cx, cy)
        val degToPx = radius / 45f

        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 10f))
        drawCircle(Palette.Outline, radius, center, style = Stroke(2.5f))
        drawCircle(Palette.Outline, radius / 2f, center, style = Stroke(1.5f, pathEffect = dash))
        drawLine(Palette.Outline, Offset(cx - 18f, cy), Offset(cx + 18f, cy), 2f)
        drawLine(Palette.Outline, Offset(cx, cy - 18f), Offset(cx, cy + 18f), 2f)

        var azDiff = (targetAzimuthDeg - pointing.azimuthDeg).toFloat()
        if (azDiff > 180f) azDiff -= 360f
        if (azDiff < -180f) azDiff += 360f
        val elDiff = (targetElevationDeg - pointing.elevationDeg).toFloat()

        val rawX = azDiff * degToPx
        val rawY = -elDiff * degToPx
        val distance = hypot(rawX, rawY)
        val locked = hypot(azDiff, elDiff) < 8f
        val dotColor = if (locked) Palette.Green else Palette.Gold

        if (distance <= radius - 14f) {
            val pos = Offset(cx + rawX, cy + rawY)
            drawCircle(dotColor.copy(alpha = 0.25f), 26f, pos)
            drawCircle(dotColor, 12f, pos)
        } else {
            val scale = (radius - 14f) / distance
            val edge = Offset(cx + rawX * scale, cy + rawY * scale)
            drawCircle(dotColor.copy(alpha = 0.5f), 10f, edge)
            drawLine(
                dotColor,
                Offset(cx + rawX * scale * 0.88f, cy + rawY * scale * 0.88f),
                edge, strokeWidth = 6f, cap = StrokeCap.Round,
            )
        }
        if (locked) drawCircle(Palette.Green, radius, center, style = Stroke(5f))
    }
}

@Composable
private fun CompassDial(
    headingDeg: Float,
    targetAzimuthDeg: Float,
    aligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val cardinalStyle = TextStyle(
        color = Palette.TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    )
    val northStyle = cardinalStyle.copy(color = Palette.Primary)

    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f * 0.80f
        val center = Offset(cx, cy)

        // Ekrandaki açı: gerçek azimut - cihaz yönü (cihazın üstü = yukarı)
        fun screenAngleRad(azimuthDeg: Float): Double =
            Math.toRadians((azimuthDeg - headingDeg).toDouble())

        fun onCircle(azimuthDeg: Float, r: Float): Offset {
            val a = screenAngleRad(azimuthDeg)
            return Offset(cx + r * sin(a).toFloat(), cy - r * cos(a).toFloat())
        }

        drawCircle(Palette.Outline, radius, center, style = Stroke(2.5f))

        // 30°'lik tikler
        for (deg in 0 until 360 step 30) {
            val outer = onCircle(deg.toFloat(), radius)
            val inner = onCircle(deg.toFloat(), radius - if (deg % 90 == 0) 16f else 9f)
            drawLine(Palette.Outline, inner, outer, strokeWidth = 3f)
        }

        // Yön harfleri (K kırmızı)
        listOf("K" to 0f, "D" to 90f, "G" to 180f, "B" to 270f).forEach { (label, az) ->
            val pos = onCircle(az, radius - 34f)
            val measured = textMeasurer.measure(
                label,
                if (az == 0f) northStyle else cardinalStyle,
            )
            drawText(
                measured,
                topLeft = Offset(
                    pos.x - measured.size.width / 2f,
                    pos.y - measured.size.height / 2f,
                ),
            )
        }

        // Cihazın baktığı yön: tepede sabit küçük işaret
        val topMarker = Path().apply {
            moveTo(cx, cy - radius - 14f)
            lineTo(cx - 9f, cy - radius + 4f)
            lineTo(cx + 9f, cy - radius + 4f)
            close()
        }
        drawPath(topMarker, Palette.TextPrimary)

        // Hedef ok: merkezden yükseliş yönüne
        val arrowColor = if (aligned) Palette.Green else Palette.Gold
        val tip = onCircle(targetAzimuthDeg, radius - 22f)
        drawLine(
            arrowColor,
            center,
            onCircle(targetAzimuthDeg, radius - 44f),
            strokeWidth = 8f,
            cap = StrokeCap.Round,
        )
        // ok başı
        val a = screenAngleRad(targetAzimuthDeg)
        val left = Offset(
            tip.x - 22f * sin(a + 2.6).toFloat(),
            tip.y + 22f * cos(a + 2.6).toFloat(),
        )
        val right = Offset(
            tip.x - 22f * sin(a - 2.6).toFloat(),
            tip.y + 22f * cos(a - 2.6).toFloat(),
        )
        val head = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        }
        drawPath(head, arrowColor)

        drawCircle(Palette.TextPrimary, 5f, center)
    }
}
