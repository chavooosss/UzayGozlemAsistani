package com.uzaygozlem.asistan.astro

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Çıplak gözle görülebilen gökcisimlerinin (gezegenler, parlak yıldızlar, Ay)
 * o anki konumunu hesaplar. Gezegenler için Paul Schlyter'in yörünge elemanı
 * yöntemi kullanılır (~1-2 açı dakikası hassasiyet — çıplak göz için fazlasıyla
 * yeterli). Güneş ile çapraz doğrulandı (bkz. SunCalc karşılaştırması).
 */
object SkyBodies {

    enum class BodyType { PLANET, STAR, MOON }

    data class SkyObject(
        val name: String,
        val type: BodyType,
        val azimuthDeg: Double,
        val elevationDeg: Double,
        val magnitude: Double,
    )

    private const val RAD = Math.PI / 180.0

    // --- Parlak yıldız kataloğu: ad, RA (derece), Dec (derece), kadir ---
    private data class Star(val name: String, val raDeg: Double, val decDeg: Double, val mag: Double)

    // ~2.7 kadire kadar parlak yıldızlar (J2000). Güneyin çok aşağısındakiler
    // Türkiye'den doğmaz; visibleNow bunları zaten eler.
    private val STARS = listOf(
        Star("Sirius (Akyıldız)", 101.287, -16.716, -1.46),
        Star("Canopus (Süheyl)", 95.988, -52.696, -0.74),
        Star("Arktürus", 213.915, 19.182, -0.05),
        Star("Vega", 279.234, 38.784, 0.03),
        Star("Capella", 79.172, 45.998, 0.08),
        Star("Rigel", 78.634, -8.202, 0.13),
        Star("Procyon", 114.825, 5.225, 0.34),
        Star("Achernar", 24.429, -57.237, 0.46),
        Star("Betelgeuse", 88.793, 7.407, 0.45),
        Star("Hadar", 210.956, -60.373, 0.61),
        Star("Altair", 297.696, 8.868, 0.77),
        Star("Acrux", 186.650, -63.099, 0.76),
        Star("Aldebaran", 68.980, 16.509, 0.85),
        Star("Antares", 247.352, -26.432, 1.09),
        Star("Spica (Başak)", 201.298, -11.161, 1.04),
        Star("Pollux", 116.329, 28.026, 1.14),
        Star("Fomalhaut", 344.413, -29.622, 1.16),
        Star("Deneb", 310.358, 45.280, 1.25),
        Star("Mimosa", 191.930, -59.689, 1.25),
        Star("Regulus", 152.093, 11.967, 1.35),
        Star("Adhara", 104.656, -28.972, 1.50),
        Star("Shaula", 263.402, -37.104, 1.62),
        Star("Castor", 113.650, 31.888, 1.58),
        Star("Gacrux", 187.791, -57.113, 1.63),
        Star("Bellatrix", 81.283, 6.350, 1.64),
        Star("Elnath", 81.573, 28.608, 1.65),
        Star("Alnilam", 84.053, -1.202, 1.69),
        Star("Alnitak", 85.190, -1.943, 1.77),
        Star("Alioth", 193.507, 55.960, 1.76),
        Star("Mirfak", 51.081, 49.861, 1.79),
        Star("Dubhe", 165.932, 61.751, 1.79),
        Star("Wezen", 107.098, -26.393, 1.83),
        Star("Alkaid", 206.885, 49.313, 1.86),
        Star("Kaus Australis", 276.043, -34.385, 1.85),
        Star("Menkalinan", 89.882, 44.947, 1.90),
        Star("Alhena", 99.428, 16.399, 1.93),
        Star("Mirzam", 95.675, -17.956, 1.98),
        Star("Alphard", 141.897, -8.659, 1.98),
        Star("Kutup Yıldızı", 37.954, 89.264, 1.98),
        Star("Hamal", 31.793, 23.462, 2.00),
        Star("Algieba", 154.993, 19.842, 2.01),
        Star("Diphda", 10.897, -17.987, 2.04),
        Star("Mizar", 200.981, 54.925, 2.23),
        Star("Nunki", 283.816, -26.297, 2.05),
        Star("Menkent", 211.671, -36.370, 2.06),
        Star("Alpheratz", 2.097, 29.090, 2.06),
        Star("Saiph", 86.939, -9.670, 2.06),
        Star("Mirach", 17.433, 35.621, 2.05),
        Star("Kochab", 222.676, 74.156, 2.08),
        Star("Rasalhague", 263.734, 12.560, 2.08),
        Star("Algol", 47.042, 40.956, 2.12),
        Star("Denebola", 177.265, 14.572, 2.11),
        Star("Mintaka", 83.002, -0.299, 2.23),
        Star("Sadr", 305.557, 40.257, 2.23),
        Star("Schedar", 10.127, 56.537, 2.24),
        Star("Eltanin", 269.152, 51.489, 2.36),
        Star("Caph", 2.294, 59.150, 2.28),
        Star("Dschubba", 240.083, -22.622, 2.29),
        Star("Alphecca (Gemma)", 233.672, 26.715, 2.22),
        Star("Izar", 221.247, 27.074, 2.35),
        Star("Enif", 326.046, 9.875, 2.39),
        Star("Scheat", 345.944, 28.083, 2.42),
        Star("Alderamin", 319.645, 62.585, 2.45),
        Star("Markab", 346.190, 15.205, 2.49),
        Star("Menkar", 45.570, 4.090, 2.53),
        Star("Zubenelgenubi", 222.720, -16.042, 2.75),
        Star("Unukalhai", 236.067, 6.426, 2.63),
        Star("Cor Caroli", 194.007, 38.318, 2.89),
        Star("Albireo", 292.680, 27.960, 3.05),
    )

    // --- Gezegen yörünge elemanları (Schlyter). Her çift: (t=0 değeri, gün başına) ---
    private class PlanetElements(
        val name: String,
        val N: DoubleArray, val i: DoubleArray, val w: DoubleArray,
        val a: Double, val e: DoubleArray, val M: DoubleArray,
        val mag0: Double, val magK: Double,
    )

    private val PLANETS = listOf(
        PlanetElements(
            "Merkür",
            doubleArrayOf(48.3313, 3.24587e-5), doubleArrayOf(7.0047, 5.00e-8),
            doubleArrayOf(29.1241, 1.01444e-5), 0.387098, doubleArrayOf(0.205635, 5.59e-10),
            doubleArrayOf(168.6562, 4.0923344368), -0.36, 0.027,
        ),
        PlanetElements(
            "Venüs",
            doubleArrayOf(76.6799, 2.46590e-5), doubleArrayOf(3.3946, 2.75e-8),
            doubleArrayOf(54.8910, 1.38374e-5), 0.723330, doubleArrayOf(0.006773, -1.302e-9),
            doubleArrayOf(48.0052, 1.6021302244), -4.34, 0.013,
        ),
        PlanetElements(
            "Mars",
            doubleArrayOf(49.5574, 2.11081e-5), doubleArrayOf(1.8497, -1.78e-8),
            doubleArrayOf(286.5016, 2.92961e-5), 1.523688, doubleArrayOf(0.093405, 2.516e-9),
            doubleArrayOf(18.6021, 0.5240207766), -1.51, 0.016,
        ),
        PlanetElements(
            "Jüpiter",
            doubleArrayOf(100.4542, 2.76854e-5), doubleArrayOf(1.3030, -1.557e-7),
            doubleArrayOf(273.8777, 1.64505e-5), 5.20256, doubleArrayOf(0.048498, 4.469e-9),
            doubleArrayOf(19.8950, 0.0830853001), -9.25, 0.014,
        ),
        PlanetElements(
            "Satürn",
            doubleArrayOf(113.6634, 2.38980e-5), doubleArrayOf(2.4886, -1.081e-7),
            doubleArrayOf(339.3939, 2.97661e-5), 9.55475, doubleArrayOf(0.055546, -9.499e-9),
            doubleArrayOf(316.9670, 0.0334442282), -9.0, 0.044,
        ),
    )

    /** Verilen an ve konum için ufkun üstündeki tüm cisimler, yükseklik sırasıyla. */
    fun visibleNow(epochMillis: Long, latDeg: Double, lonDeg: Double): List<SkyObject> =
        allNow(epochMillis, latDeg, lonDeg).filter { it.elevationDeg > 0 }

    /** Tek bir cismin belirli andaki durumu (ada göre). */
    fun positionOf(name: String, epochMillis: Long, latDeg: Double, lonDeg: Double): SkyObject? =
        allNow(epochMillis, latDeg, lonDeg).firstOrNull { it.name == name }

    /** Herhangi bir RA/Dec (derece) için azimut+yükseklik — meteor radyantı gibi. */
    fun altAz(
        raDeg: Double, decDeg: Double, epochMillis: Long, latDeg: Double, lonDeg: Double,
    ): Pair<Double, Double> {
        val d = epochMillis / 86400000.0 + 2440587.5 - 2451545.0
        return raDecToAltAz(raDeg, decDeg, d, latDeg, lonDeg)
    }

    enum class RiseState { UP_NOW, RISES_LATER, CIRCUMPOLAR, NEVER_RISES }

    data class SkyVisibility(
        val state: RiseState,
        val riseMs: Long?,
        val transitMs: Long?,
        val transitElevationDeg: Double,
        val setMs: Long?,
        val darkFromMs: Long?,   // karanlık + ufuk üstü penceresinin başı
        val darkUntilMs: Long?,
    )

    /**
     * Bir cismin önümüzdeki 24 saatteki görünürlük penceresi: doğuş, tepe,
     * batış saatleri ve gökyüzünün karardığı (güneş < -6°) görüş aralığı.
     * 5 dakikalık örnekleme ile hesaplanır.
     */
    fun visibility(
        name: String,
        fromMs: Long,
        latDeg: Double,
        lonDeg: Double,
    ): SkyVisibility {
        val stepMs = 5 * 60_000L
        data class Sample(val t: Long, val el: Double, val dark: Boolean)
        val samples = (0..(24 * 60 / 5)).map { k ->
            val t = fromMs + k * stepMs
            val el = positionOf(name, t, latDeg, lonDeg)?.elevationDeg ?: -90.0
            val dark = SunCalc.sunElevationDeg(t, latDeg, lonDeg) < SunCalc.KARANLIK_ESIGI_DERECE
            Sample(t, el, dark)
        }

        val currentUp = samples.first().el > 0
        var riseMs: Long? = null
        var setMs: Long? = null
        for (i in 1 until samples.size) {
            val prev = samples[i - 1].el; val cur = samples[i].el
            if (prev <= 0 && cur > 0 && riseMs == null) riseMs = samples[i].t
            if (prev > 0 && cur <= 0 && setMs == null && (currentUp || riseMs != null)) {
                setMs = samples[i].t
            }
        }
        val transit = samples.maxByOrNull { it.el }!!

        // İlk kesintisiz "karanlık + ufuk üstü" aralığı
        var darkFrom: Long? = null
        var darkUntil: Long? = null
        for (s in samples) {
            if (s.el > 0 && s.dark) {
                if (darkFrom == null) darkFrom = s.t
                darkUntil = s.t
            } else if (darkFrom != null) {
                break
            }
        }

        val state = when {
            samples.all { it.el > 0 } -> RiseState.CIRCUMPOLAR
            samples.all { it.el <= 0 } -> RiseState.NEVER_RISES
            currentUp -> RiseState.UP_NOW
            else -> RiseState.RISES_LATER
        }
        return SkyVisibility(state, riseMs, transit.t, transit.el, setMs, darkFrom, darkUntil)
    }

    fun allNow(epochMillis: Long, latDeg: Double, lonDeg: Double): List<SkyObject> {
        val d = epochMillis / 86400000.0 + 2440587.5 - 2451545.0
        val result = mutableListOf<SkyObject>()

        // Ay
        moonRaDec(d)?.let { (ra, dec) ->
            val (az, el) = raDecToAltAz(ra, dec, d, latDeg, lonDeg)
            result += SkyObject("Ay", BodyType.MOON, az, el, -12.0)
        }

        // Gezegenler
        for (p in PLANETS) {
            val (ra, dec, mag) = planetRaDecMag(p, d)
            val (az, el) = raDecToAltAz(ra, dec, d, latDeg, lonDeg)
            result += SkyObject(p.name, BodyType.PLANET, az, el, mag)
        }

        // Yıldızlar
        for (s in STARS) {
            val (az, el) = raDecToAltAz(s.raDeg, s.decDeg, d, latDeg, lonDeg)
            result += SkyObject(s.name, BodyType.STAR, az, el, s.mag)
        }

        return result.sortedByDescending { it.elevationDeg }
    }

    private fun lin(e: DoubleArray, d: Double) = e[0] + e[1] * d
    private fun rev(x: Double) = ((x % 360.0) + 360.0) % 360.0

    /** Bir gezegenin geosentrik RA/Dec (derece) ve yaklaşık parlaklığı. */
    private fun planetRaDecMag(p: PlanetElements, d: Double): Triple<Double, Double, Double> {
        val n = rev(lin(p.N, d)); val incl = lin(p.i, d); val w = rev(lin(p.w, d))
        val a = p.a; val e = lin(p.e, d); val m = rev(lin(p.M, d))

        var ecc = m + e / RAD * sin(m * RAD) * (1 + e * cos(m * RAD))
        repeat(3) {
            ecc -= (ecc - e / RAD * sin(ecc * RAD) - m) / (1 - e * cos(ecc * RAD))
        }
        val xv = a * (cos(ecc * RAD) - e)
        val yv = a * sqrt(1 - e * e) * sin(ecc * RAD)
        val v = rev(atan2(yv, xv) / RAD)
        val r = sqrt(xv * xv + yv * yv)

        val xh = r * (cos(n * RAD) * cos((v + w) * RAD) -
            sin(n * RAD) * sin((v + w) * RAD) * cos(incl * RAD))
        val yh = r * (sin(n * RAD) * cos((v + w) * RAD) +
            cos(n * RAD) * sin((v + w) * RAD) * cos(incl * RAD))
        val zh = r * sin((v + w) * RAD) * sin(incl * RAD)

        val (xs, ys, rs) = sunRect(d)
        val xg = xh + xs; val yg = yh + ys; val zg = zh
        val bigR = sqrt(xg * xg + yg * yg + zg * zg)

        val oblecl = 23.4393 - 3.563e-7 * d
        val xe = xg
        val ye = yg * cos(oblecl * RAD) - zg * sin(oblecl * RAD)
        val ze = yg * sin(oblecl * RAD) + zg * cos(oblecl * RAD)
        val ra = rev(atan2(ye, xe) / RAD)
        val dec = atan2(ze, sqrt(xe * xe + ye * ye)) / RAD

        // Faz açısı ve parlaklık
        val cosFv = ((r * r + bigR * bigR - rs * rs) / (2 * r * bigR)).coerceIn(-1.0, 1.0)
        val fv = acos(cosFv) / RAD
        val mag = p.mag0 + 5 * log10(r * bigR) + p.magK * fv
        return Triple(ra, dec, mag)
    }

    /**
     * Güneşin o an tam tepede olduğu yeryüzü noktası (subsolar): (enlem, boylam).
     * Dünya haritasında gündüz/gece gölgesi için kullanılır.
     */
    fun sunGroundPoint(epochMillis: Long): Pair<Double, Double> {
        val d = epochMillis / 86400000.0 + 2440587.5 - 2451545.0
        val (xs, ys, _) = sunRect(d)
        val oblecl = 23.4393 - 3.563e-7 * d
        // Güneşin ekvatoral koordinatları
        val xe = xs
        val ye = ys * cos(oblecl * RAD)
        val ze = ys * sin(oblecl * RAD)
        val ra = rev(atan2(ye, xe) / RAD)
        val dec = atan2(ze, sqrt(xe * xe + ye * ye)) / RAD
        val gmst = (18.697374558 + 24.06570982441908 * d).mod(24.0) * 15.0
        var lon = ra - gmst
        lon = ((lon + 180.0).mod(360.0)) - 180.0
        return dec to lon
    }

    /** Güneşin belirli bir yer noktasındaki yükseklik açısı (gölge çizimi için). */
    fun sunElevationAt(subLatDeg: Double, subLonDeg: Double, latDeg: Double, lonDeg: Double): Double {
        val sinElev = sin(latDeg * RAD) * sin(subLatDeg * RAD) +
            cos(latDeg * RAD) * cos(subLatDeg * RAD) * cos((lonDeg - subLonDeg) * RAD)
        return asin(sinElev.coerceIn(-1.0, 1.0)) / RAD
    }

    /** Güneşin dikdörtgen (ekliptik) koordinatları: (x, y, r). */
    private fun sunRect(d: Double): Triple<Double, Double, Double> {
        val w = 282.9404 + 4.70935e-5 * d
        val e = 0.016709 - 1.151e-9 * d
        val m = rev(356.0470 + 0.9856002585 * d)
        val ecc = m + e / RAD * sin(m * RAD) * (1 + e * cos(m * RAD))
        val xv = cos(ecc * RAD) - e
        val yv = sqrt(1 - e * e) * sin(ecc * RAD)
        val v = atan2(yv, xv) / RAD
        val r = sqrt(xv * xv + yv * yv)
        val lon = rev(v + w)
        return Triple(r * cos(lon * RAD), r * sin(lon * RAD), r)
    }

    /** Ay'ın geosentrik RA/Dec (derece); ana pertürbasyon terimleri dahil. */
    private fun moonRaDec(d: Double): Pair<Double, Double>? {
        val n = rev(125.1228 - 0.0529538083 * d)
        val i = 5.1454
        val w = rev(318.0634 + 0.1643573223 * d)
        val a = 60.2666
        val e = 0.054900
        val m = rev(115.3654 + 13.0649929509 * d)

        var ecc = m + e / RAD * sin(m * RAD) * (1 + e * cos(m * RAD))
        repeat(3) {
            ecc -= (ecc - e / RAD * sin(ecc * RAD) - m) / (1 - e * cos(ecc * RAD))
        }
        val xv = a * (cos(ecc * RAD) - e)
        val yv = a * sqrt(1 - e * e) * sin(ecc * RAD)
        val v = rev(atan2(yv, xv) / RAD)
        val r = sqrt(xv * xv + yv * yv)

        val xh = r * (cos(n * RAD) * cos((v + w) * RAD) -
            sin(n * RAD) * sin((v + w) * RAD) * cos(i * RAD))
        val yh = r * (sin(n * RAD) * cos((v + w) * RAD) +
            cos(n * RAD) * sin((v + w) * RAD) * cos(i * RAD))
        val zh = r * sin((v + w) * RAD) * sin(i * RAD)

        var lon = rev(atan2(yh, xh) / RAD)
        var lat = atan2(zh, sqrt(xh * xh + yh * yh)) / RAD

        // Pertürbasyonlar için Güneş ve Ay ortalama açıları
        val ms = rev(356.0470 + 0.9856002585 * d)     // Güneş ort. anomali
        val ws = 282.9404 + 4.70935e-5 * d
        val ls = rev(ms + ws)                          // Güneş ort. boylam
        val lm = rev(n + w + m)                        // Ay ort. boylam
        val dEl = rev(lm - ls)                         // ortalama elongasyon
        val f = rev(lm - n)                            // enlem argümanı

        // Boylamdaki başlıca pertürbasyonlar (derece)
        lon += -1.274 * sin((m - 2 * dEl) * RAD)       // evection
        lon += 0.658 * sin(2 * dEl * RAD)              // variation
        lon += -0.186 * sin(ms * RAD)                  // yıllık denklem
        lon += -0.059 * sin((2 * m - 2 * dEl) * RAD)
        lon += -0.057 * sin((m - 2 * dEl + ms) * RAD)
        lon += 0.053 * sin((m + 2 * dEl) * RAD)
        // Enlemdeki başlıca pertürbasyonlar
        lat += -0.173 * sin((f - 2 * dEl) * RAD)
        lat += -0.055 * sin((m - f - 2 * dEl) * RAD)
        lat += -0.046 * sin((m + f - 2 * dEl) * RAD)

        // Ekliptik → ekvatoral
        val oblecl = 23.4393 - 3.563e-7 * d
        val xe0 = cos(lat * RAD) * cos(lon * RAD)
        val ye0 = cos(lat * RAD) * sin(lon * RAD)
        val ze0 = sin(lat * RAD)
        val xe = xe0
        val ye = ye0 * cos(oblecl * RAD) - ze0 * sin(oblecl * RAD)
        val ze = ye0 * sin(oblecl * RAD) + ze0 * cos(oblecl * RAD)
        val ra = rev(atan2(ye, xe) / RAD)
        val dec = atan2(ze, sqrt(xe * xe + ye * ye)) / RAD
        return ra to dec
    }

    /** RA/Dec (derece) → gözlemci için azimut (kuzeyden) ve yükseklik (derece). */
    private fun raDecToAltAz(
        raDeg: Double, decDeg: Double, d: Double, latDeg: Double, lonDeg: Double,
    ): Pair<Double, Double> {
        val gmst = (18.697374558 + 24.06570982441908 * d).mod(24.0)
        val lstDeg = (gmst * 15 + lonDeg).mod(360.0)
        val ha = (lstDeg - raDeg) * RAD
        val dec = decDeg * RAD
        val lat = latDeg * RAD

        val sinAlt = sin(dec) * sin(lat) + cos(dec) * cos(lat) * cos(ha)
        val alt = asin(sinAlt.coerceIn(-1.0, 1.0))
        val cosAz = ((sin(dec) - sin(alt) * sin(lat)) / (cos(alt) * cos(lat)))
            .coerceIn(-1.0, 1.0)
        var az = acos(cosAz) / RAD
        if (sin(ha) > 0) az = 360 - az
        return az to alt / RAD
    }
}
