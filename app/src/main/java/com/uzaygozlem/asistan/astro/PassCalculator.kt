package com.uzaygozlem.asistan.astro

import com.github.amsacode.predict4java.GroundStationPosition
import com.github.amsacode.predict4java.PassPredictor
import com.github.amsacode.predict4java.SatPosEclipse
import com.github.amsacode.predict4java.TLE
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

/**
 * Bir geçişin çıplak gözle görünürlük durumu.
 * Görünür olması için uydunun güneş ışığı alması (gölgede olmaması)
 * VE gözlemcide gökyüzünün karanlık olması (güneş < -6°) gerekir.
 */
enum class Visibility {
    VISIBLE,        // en az bir anda her iki koşul da sağlanıyor
    ECLIPSED,       // gökyüzü karanlık ama uydu Dünya'nın gölgesinde
    SKY_TOO_BRIGHT, // geçiş boyunca gökyüzü hiç yeterince kararmıyor
}

/** Geçiş izindeki tek örnek: gökyüzü haritası çizimi için. */
data class TrackPoint(
    val timeMs: Long,
    val azimuthDeg: Double,
    val elevationDeg: Double,
    val visible: Boolean,
)

data class SatellitePass(
    val satelliteName: String,
    val aos: ZonedDateTime,      // yükseliş (ufkun üstüne çıkış)
    val tca: ZonedDateTime,      // zirve zamanı
    val los: ZonedDateTime,      // batış (ufkun altına iniş)
    val maxElevationDeg: Double, // zirvede yükseklik açısı
    val aosAzimuthDeg: Int,
    val losAzimuthDeg: Int,
    val visibility: Visibility,
    val visibleFrom: ZonedDateTime?, // görünürlüğün başladığı an (varsa)
    val visibleUntil: ZonedDateTime?,
    val track: List<TrackPoint>,     // geçiş boyunca 30 sn'lik örnekler
    val cloudCoverPct: Int? = null,  // zirve saatindeki bulutluluk (varsa)
    // Canlı takip için: aynı yörüngeyi yeniden kurabilmek adına TLE + gözlemci
    val tleLines: List<String> = emptyList(),
    val observerLat: Double = 0.0,
    val observerLon: Double = 0.0,
    val observerAltM: Double = 0.0,
    // Zirvedeki yaklaşık görünür parlaklık (kadir; küçük = parlak)
    val magnitudeAtPeak: Double? = null,
)

object PassCalculator {

    private const val SAMPLE_STEP_MS = 30_000L

    /**
     * Verilen TLE ve gözlemci konumu için önümüzdeki [hoursAhead] saatteki
     * geçişleri hesaplar (SGP4, predict4java). Her geçiş 30 saniyede bir
     * örneklenir; hem görünürlük kararı hem gökyüzü haritası izi bu
     * örneklerden çıkar.
     */
    fun computePasses(
        tleLines: List<String>,
        displayName: String,
        latitude: Double,
        longitude: Double,
        altitudeMeters: Double,
        standardMagnitude: Double? = null,
        hoursAhead: Int = 24,
    ): List<SatellitePass> {
        val tle = TLE(tleLines.toTypedArray())
        val groundStation = GroundStationPosition(latitude, longitude, altitudeMeters)
        val predictor = PassPredictor(tle, groundStation)
        val zone = ZoneId.systemDefault()

        return predictor.getPasses(Date(), hoursAhead, false).map { pass ->
            val result = buildTrack(
                predictor, pass.startTime.time, pass.endTime.time, latitude, longitude,
            )
            val visiblePoints = result.points.filter { it.visible }
            val visibility = when {
                visiblePoints.isNotEmpty() -> Visibility.VISIBLE
                result.anyDarkSky -> Visibility.ECLIPSED
                else -> Visibility.SKY_TOO_BRIGHT
            }
            SatellitePass(
                satelliteName = displayName,
                aos = pass.startTime.toZoned(zone),
                tca = pass.tca.toZoned(zone),
                los = pass.endTime.toZoned(zone),
                maxElevationDeg = pass.maxEl,
                aosAzimuthDeg = pass.aosAzimuth,
                losAzimuthDeg = pass.losAzimuth,
                visibility = visibility,
                visibleFrom = visiblePoints.firstOrNull()?.timeMs?.toZoned(zone),
                visibleUntil = visiblePoints.lastOrNull()?.timeMs?.toZoned(zone),
                track = result.points,
                tleLines = tleLines,
                observerLat = latitude,
                observerLon = longitude,
                observerAltM = altitudeMeters,
                magnitudeAtPeak = standardMagnitude?.let { stdMag ->
                    estimateMagnitude(predictor, pass.tca, stdMag)
                },
            )
        }
    }

    /**
     * Zirvedeki yaklaşık parlaklık: standart kadir (1000 km referans) +
     * mesafe düzeltmesi. Faz açısı ihmal edilir (~±0.5 kadir belirsizlik),
     * "ne kadar parlak olacak" fikri vermek için yeterli.
     */
    private fun estimateMagnitude(
        predictor: PassPredictor,
        tca: Date,
        standardMagnitude: Double,
    ): Double? = try {
        val rangeKm = predictor.getSatPos(tca).range
        standardMagnitude + 5.0 * kotlin.math.log10(rangeKm / 1000.0)
    } catch (e: Exception) {
        null
    }

    private class TrackResult(val points: List<TrackPoint>, val anyDarkSky: Boolean)

    private fun buildTrack(
        predictor: PassPredictor,
        startMs: Long,
        endMs: Long,
        latitude: Double,
        longitude: Double,
    ): TrackResult {
        val points = mutableListOf<TrackPoint>()
        var anyDarkSky = false

        var t = startMs
        while (t <= endMs) {
            val satPos = predictor.getSatPos(Date(t))
            val elevationDeg = Math.toDegrees(satPos.elevation)
            val azimuthDeg = Math.toDegrees(satPos.azimuth)
            val skyDark = SunCalc.sunElevationDeg(t, latitude, longitude) <
                SunCalc.KARANLIK_ESIGI_DERECE
            if (skyDark) anyDarkSky = true
            val visible = skyDark && elevationDeg > 0 && !SatPosEclipse.isEclipsed(satPos)
            points.add(TrackPoint(t, azimuthDeg, elevationDeg, visible))
            t += SAMPLE_STEP_MS
        }
        return TrackResult(points, anyDarkSky)
    }

    private fun Date.toZoned(zone: ZoneId): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), zone)

    private fun Long.toZoned(zone: ZoneId): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(this), zone)

    /** Azimut derecesini Türkçe pusula yönüne çevirir (8 yön). */
    fun azimuthToTurkish(deg: Int): String {
        val directions = listOf(
            "Kuzey", "Kuzeydoğu", "Doğu", "Güneydoğu",
            "Güney", "Güneybatı", "Batı", "Kuzeybatı",
        )
        val index = (((deg % 360) + 360) % 360 + 22.5).toInt() / 45 % 8
        return directions[index]
    }
}
