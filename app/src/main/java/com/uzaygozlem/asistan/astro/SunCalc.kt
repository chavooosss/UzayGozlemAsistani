package com.uzaygozlem.asistan.astro

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Basit güneş konumu hesabı (düşük hassasiyetli, ~0.2° doğruluk).
 * Gözlem planlaması için fazlasıyla yeterli; amaç güneşin ufkun
 * ne kadar altında olduğunu bilmek (alacakaranlık kontrolü).
 */
object SunCalc {

    /** Sivil alacakaranlık eşiği: güneş bunun altındaysa uydular görülebilir. */
    const val KARANLIK_ESIGI_DERECE = -6.0

    /** Verilen an ve konum için güneşin yükseklik açısı (derece). */
    fun sunElevationDeg(epochMillis: Long, latDeg: Double, lonDeg: Double): Double {
        // J2000'den bu yana geçen gün sayısı
        val d = epochMillis / 86400000.0 + 2440587.5 - 2451545.0

        // Güneşin ekliptik boylamı (ortalama anomali + merkez denklemi)
        val g = Math.toRadians(norm360(357.529 + 0.98560028 * d))
        val q = norm360(280.459 + 0.98564736 * d)
        val eclipticLon = Math.toRadians(norm360(q + 1.915 * sin(g) + 0.020 * sin(2 * g)))

        // Ekvatoral koordinatlara dönüşüm
        val obliquity = Math.toRadians(23.439 - 0.00000036 * d)
        val rightAscension = atan2(cos(obliquity) * sin(eclipticLon), cos(eclipticLon))
        val declination = asin(sin(obliquity) * sin(eclipticLon))

        // Yerel yıldız zamanı → saat açısı
        val gmstHours = (18.697374558 + 24.06570982441908 * d).mod(24.0)
        val lstHours = (gmstHours + lonDeg / 15.0).mod(24.0)
        val hourAngle = Math.toRadians(lstHours * 15.0) - rightAscension

        val lat = Math.toRadians(latDeg)
        val elevation = asin(
            sin(lat) * sin(declination) + cos(lat) * cos(declination) * cos(hourAngle),
        )
        return Math.toDegrees(elevation)
    }

    private fun norm360(x: Double): Double = x.mod(360.0)
}
