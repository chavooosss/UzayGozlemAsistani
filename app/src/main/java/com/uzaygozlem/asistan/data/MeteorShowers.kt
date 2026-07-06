package com.uzaygozlem.asistan.data

import com.uzaygozlem.asistan.astro.MoonCalc
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Yıllık büyük meteor yağmurları. Zirve tarihleri yıldan yıla ±1 gün
 * oynayabilir; planlama için yaklaşık değerler yeterli.
 * ZHR: ideal koşullarda saatlik zenit meteor sayısı.
 * radiantRaDeg/DecDeg: meteorların çıkıyor göründüğü nokta (radyant, J2000).
 */
data class MeteorShower(
    val name: String,
    val radiant: String,
    val peak: MonthDay,
    val zhr: Int,
    val radiantRaDeg: Double,
    val radiantDecDeg: Double,
    val parent: String,       // kaynak kuyruklu yıldız/asteroit
    val activePeriod: String, // etkin olduğu tarih aralığı
)

val METEOR_SHOWERS = listOf(
    MeteorShower("Quadrantidler", "Çoban (Boötes)", MonthDay.of(1, 3), 120,
        230.0, 49.0, "Asteroit 2003 EH1", "28 Aralık – 12 Ocak"),
    MeteorShower("Lyridler", "Çalgı (Lyra)", MonthDay.of(4, 22), 18,
        271.0, 34.0, "Kuyruklu yıldız Thatcher", "16 – 25 Nisan"),
    MeteorShower("Eta Aquaridler", "Kova (Aquarius)", MonthDay.of(5, 6), 50,
        338.0, -1.0, "Halley kuyruklu yıldızı", "19 Nisan – 28 Mayıs"),
    MeteorShower("Perseidler", "Kahraman (Perseus)", MonthDay.of(8, 12), 100,
        48.0, 58.0, "Kuyruklu yıldız Swift-Tuttle", "17 Temmuz – 24 Ağustos"),
    MeteorShower("Orionidler", "Avcı (Orion)", MonthDay.of(10, 21), 20,
        95.0, 16.0, "Halley kuyruklu yıldızı", "2 Ekim – 7 Kasım"),
    MeteorShower("Leonidler", "Aslan (Leo)", MonthDay.of(11, 17), 15,
        152.0, 22.0, "Kuyruklu yıldız Tempel-Tuttle", "6 – 30 Kasım"),
    MeteorShower("Geminidler", "İkizler (Gemini)", MonthDay.of(12, 14), 150,
        112.0, 33.0, "Asteroit 3200 Phaethon", "4 – 17 Aralık"),
    MeteorShower("Ursidler", "Küçük Ayı (Ursa Minor)", MonthDay.of(12, 22), 10,
        217.0, 76.0, "Kuyruklu yıldız 8P/Tuttle", "17 – 26 Aralık"),
)

data class UpcomingShower(
    val shower: MeteorShower,
    val peakDate: LocalDate,
    val daysLeft: Long,
    val moonIlluminationPct: Int, // zirve gecesindeki Ay aydınlanması
)

/** Bugüne göre en yakın [count] yağmuru, zirve gecesi Ay aydınlanmasıyla döner. */
fun upcomingShowers(today: LocalDate = LocalDate.now(), count: Int = 5): List<UpcomingShower> {
    return METEOR_SHOWERS.map { shower ->
        var peakDate = shower.peak.atYear(today.year)
        if (peakDate.isBefore(today)) peakDate = shower.peak.atYear(today.year + 1)
        val peakNightMillis = peakDate.atTime(23, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        UpcomingShower(
            shower = shower,
            peakDate = peakDate,
            daysLeft = ChronoUnit.DAYS.between(today, peakDate),
            moonIlluminationPct = MoonCalc.illuminationPercent(peakNightMillis).toInt(),
        )
    }.sortedBy { it.daysLeft }.take(count)
}
