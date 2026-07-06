package com.uzaygozlem.asistan.data

data class TrackedSatellite(
    val noradId: Int,
    val displayName: String,
    // 1000 km mesafedeki standart parlaklık (kadir) — yaklaşık değerler
    val standardMagnitude: Double,
)

/**
 * Seçilebilir uydu kataloğu: çıplak gözle görülebilecek popüler hedefler.
 * İlk üçü varsayılan olarak açıktır; diğerleri daha sönüktür (karanlık
 * gökyüzü ister), Geçişler sekmesindeki "Uydu seç" ile açılır.
 */
val SATELLITE_CATALOG = listOf(
    TrackedSatellite(25544, "ISS (Uluslararası Uzay İstasyonu)", -1.8),
    TrackedSatellite(48274, "Tiangong (Çin Uzay İstasyonu)", -0.3),
    TrackedSatellite(20580, "Hubble Uzay Teleskobu", 2.2),
    TrackedSatellite(25994, "Terra (NASA yer gözlem)", 3.0),
    TrackedSatellite(27424, "Aqua (NASA yer gözlem)", 3.2),
    TrackedSatellite(27386, "Envisat (ESA, dev pasif uydu)", 3.7),
    TrackedSatellite(39084, "Landsat 8 (yer gözlem)", 4.3),
    TrackedSatellite(43013, "NOAA-20 (meteoroloji)", 5.0),
)

val DEFAULT_SATELLITE_IDS = setOf(25544, 48274, 20580)
