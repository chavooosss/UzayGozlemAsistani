package com.uzaygozlem.asistan.astro

import kotlin.math.cos
import kotlin.math.sin

enum class MoonPhase(val turkishName: String, val emoji: String) {
    YENI_AY("Yeni Ay", "🌑"),
    BUYUYEN_HILAL("Büyüyen Hilal", "🌒"),
    ILK_DORDUN("İlk Dördün", "🌓"),
    BUYUYEN_SISKIN("Büyüyen Şişkin Ay", "🌔"),
    DOLUNAY("Dolunay", "🌕"),
    KUCULEN_SISKIN("Küçülen Şişkin Ay", "🌖"),
    SON_DORDUN("Son Dördün", "🌗"),
    KUCULEN_HILAL("Küçülen Hilal", "🌘"),
}

/**
 * Basit Ay evresi hesabı: Güneş ve Ay'ın ekliptik boylam farkı (elongasyon).
 * Ana pertürbasyon terimi dahil; aydınlanma yüzdesi için %1-2 doğruluk,
 * gözlem planlaması için yeterli.
 */
object MoonCalc {

    const val SYNODIC_DAYS = 29.530588853
    private const val ELONGATION_RATE = 360.0 / SYNODIC_DAYS // derece/gün

    /** Ay−Güneş ekliptik boylam farkı (elongasyon, 0–360°). 0=Yeni Ay, 180=Dolunay. */
    fun elongationDeg(epochMillis: Long): Double {
        val d = epochMillis / 86400000.0 + 2440587.5 - 2451545.0

        // Güneşin görünür ekliptik boylamı
        val g = Math.toRadians((357.529 + 0.98560028 * d).mod(360.0))
        val sunLon = (280.459 + 0.98564736 * d + 1.915 * sin(g) + 0.020 * sin(2 * g)).mod(360.0)

        // Ayın ekliptik boylamı (en büyük düzeltme terimi olan evection dahil)
        val moonAnomaly = Math.toRadians((134.963 + 13.064993 * d).mod(360.0))
        val moonLon = (218.316 + 13.176396 * d + 6.289 * sin(moonAnomaly)).mod(360.0)

        return (moonLon - sunLon).mod(360.0)
    }

    /** Ay diskinin aydınlanma yüzdesi (0–100). */
    fun illuminationPercent(epochMillis: Long): Double =
        (1 - cos(Math.toRadians(elongationDeg(epochMillis)))) / 2 * 100

    /** 8 evreden hangisinde olduğumuz (her evre 45°'lik dilim). */
    fun phase(epochMillis: Long): MoonPhase {
        val index = ((elongationDeg(epochMillis) + 22.5) / 45.0).toInt() % 8
        return MoonPhase.entries[index]
    }

    /** Hedef elongasyona (0 = Yeni Ay, 180 = Dolunay) kalan gün sayısı. */
    fun daysUntilElongation(epochMillis: Long, targetDeg: Double): Double {
        val diff = (targetDeg - elongationDeg(epochMillis)).mod(360.0)
        return diff / ELONGATION_RATE
    }
}
