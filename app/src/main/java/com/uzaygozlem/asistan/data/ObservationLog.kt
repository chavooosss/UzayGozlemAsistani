package com.uzaygozlem.asistan.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Tek bir gözlem kaydı: "bu geçişi gördüm" + isteğe bağlı not. */
data class Observation(
    val id: Long,
    val timestampMs: Long,       // gözlemin yapıldığı an
    val satelliteName: String,
    val maxElevationDeg: Int,
    val magnitude: Double?,      // varsa tahmini parlaklık
    val note: String,
)

/** Gözlem günlüğünü basit bir JSON dosyasında tutar. */
class ObservationRepository(filesDir: File) {

    private val file = File(filesDir, "observations.json")

    fun load(): List<Observation> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Observation(
                            id = o.getLong("id"),
                            timestampMs = o.getLong("time"),
                            satelliteName = o.getString("name"),
                            maxElevationDeg = o.getInt("maxEl"),
                            magnitude = if (o.has("mag")) o.getDouble("mag") else null,
                            note = o.optString("note", ""),
                        ),
                    )
                }
            }.sortedByDescending { it.timestampMs }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(observation: Observation): List<Observation> {
        val updated = load() + observation
        save(updated)
        return updated.sortedByDescending { it.timestampMs }
    }

    fun remove(id: Long): List<Observation> {
        val updated = load().filterNot { it.id == id }
        save(updated)
        return updated
    }

    private fun save(observations: List<Observation>) {
        try {
            val array = JSONArray()
            observations.forEach { obs ->
                array.put(
                    JSONObject()
                        .put("id", obs.id)
                        .put("time", obs.timestampMs)
                        .put("name", obs.satelliteName)
                        .put("maxEl", obs.maxElevationDeg)
                        .apply { obs.magnitude?.let { put("mag", it) } }
                        .put("note", obs.note),
                )
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            // günlük yazılamadıysa sessizce geç — uygulama akışını bozma
        }
    }
}
