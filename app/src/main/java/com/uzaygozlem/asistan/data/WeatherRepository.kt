package com.uzaygozlem.asistan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Open-Meteo'dan (ücretsiz, anahtarsız) saatlik bulutluluk tahmini çeker.
 * Sonuç: epoch-saniye → bulutluluk yüzdesi. Hata durumunda boş map döner;
 * bulutluluk "olsa iyi olur" bilgisi olduğu için uygulamayı asla bloklamaz.
 */
class WeatherRepository(private val cacheDir: File) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getCloudCover(latitude: Double, longitude: Double): Map<Long, Int> =
        withContext(Dispatchers.IO) {
            val cached = readCache(latitude, longitude)
            if (cached != null) return@withContext parse(cached)
            try {
                val url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=%.3f&longitude=%.3f" +
                        "&hourly=cloud_cover&forecast_days=3" +
                        "&timeformat=unixtime&timezone=UTC",
                    latitude, longitude,
                )
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val body = response.body?.string().orEmpty()
                    writeCache(latitude, longitude, body)
                    parse(body)
                }
            } catch (e: Exception) {
                emptyMap()
            }
        }

    private fun parse(json: String): Map<Long, Int> = try {
        val hourly = JSONObject(json).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val clouds = hourly.getJSONArray("cloud_cover")
        buildMap {
            for (i in 0 until times.length()) {
                if (!clouds.isNull(i)) put(times.getLong(i), clouds.getInt(i))
            }
        }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun cacheFile() = File(cacheDir, "weather_cache.txt")

    private fun readCache(latitude: Double, longitude: Double): String? {
        val file = cacheFile()
        if (!file.exists()) return null
        return try {
            val lines = file.readText().split('\n', limit = 3)
            val savedAt = lines[0].toLong()
            val (lat, lon) = lines[1].split(',').map { it.toDouble() }
            val fresh = System.currentTimeMillis() - savedAt < TimeUnit.HOURS.toMillis(1)
            val samePlace = abs(lat - latitude) < 0.25 && abs(lon - longitude) < 0.25
            if (fresh && samePlace) lines[2] else null
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(latitude: Double, longitude: Double, body: String) {
        try {
            cacheFile().writeText(
                "${System.currentTimeMillis()}\n$latitude,$longitude\n$body",
            )
        } catch (e: Exception) {
            // önbellek yazılamazsa sorun değil
        }
    }
}
