package com.uzaygozlem.asistan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * CelesTrak'tan TLE verisi çeker ve dosya tabanlı önbellekte tutar.
 * Önbellek TAZELIK_SAATI saatten yeniyse ağa hiç çıkılmaz; ağ hatasında
 * bayat da olsa eldeki önbellek kullanılır.
 */
class TleRepository(private val cacheDir: File) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed class TleResult {
        data class Success(val lines: List<String>, val stale: Boolean) : TleResult()
        data class Failure(val message: String) : TleResult()
    }

    suspend fun getTle(noradId: Int): TleResult = withContext(Dispatchers.IO) {
        val cached = readCache(noradId)
        if (cached != null && cached.ageMillis < TimeUnit.HOURS.toMillis(TAZELIK_SAATI)) {
            return@withContext TleResult.Success(cached.lines, stale = false)
        }
        try {
            val lines = fetchFromNetwork(noradId)
            writeCache(noradId, lines)
            TleResult.Success(lines, stale = false)
        } catch (e: Exception) {
            if (cached != null) {
                TleResult.Success(cached.lines, stale = true)
            } else {
                TleResult.Failure("TLE verisi alınamadı (internet bağlantısını kontrol edin)")
            }
        }
    }

    private fun fetchFromNetwork(noradId: Int): List<String> {
        val url = "https://celestrak.org/NORAD/elements/gp.php?CATNR=$noradId&FORMAT=tle"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.string().orEmpty().trim()
            val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size < 3 || !lines[1].startsWith("1 ") || !lines[2].startsWith("2 ")) {
                error("Beklenmeyen TLE formatı: $body")
            }
            return lines.take(3)
        }
    }

    private data class CachedTle(val lines: List<String>, val ageMillis: Long)

    private fun cacheFile(noradId: Int) = File(cacheDir, "tle_$noradId.txt")

    private fun readCache(noradId: Int): CachedTle? {
        val file = cacheFile(noradId)
        if (!file.exists()) return null
        return try {
            val content = file.readText().lines().filter { it.isNotBlank() }
            val savedAt = content.first().toLong()
            CachedTle(content.drop(1), System.currentTimeMillis() - savedAt)
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(noradId: Int, lines: List<String>) {
        cacheFile(noradId).writeText(
            "${System.currentTimeMillis()}\n" + lines.joinToString("\n")
        )
    }

    companion object {
        const val TAZELIK_SAATI = 6L
    }
}
