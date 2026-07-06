package com.uzaygozlem.asistan.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * İzleme listesi öğesi: sonradan tekrar bakmak üzere kaydedilen bir uydu
 * ya da gökcismi. "Gördüm" (gözlem günlüğü) değil, "incelemek istiyorum".
 */
data class WatchItem(
    val id: Long,
    val kind: Kind,
    val name: String,       // gökcismi adı veya uydu kısa adı
) {
    enum class Kind { SKY, SATELLITE }
}

class WatchlistRepository(filesDir: File) {

    private val file = File(filesDir, "watchlist.json")

    fun load(): List<WatchItem> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        WatchItem(
                            id = o.getLong("id"),
                            kind = WatchItem.Kind.valueOf(o.getString("kind")),
                            name = o.getString("name"),
                        ),
                    )
                }
            }.sortedByDescending { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(item: WatchItem): List<WatchItem> {
        // Aynı ad + tür varsa tekrar ekleme
        val existing = load()
        if (existing.any { it.kind == item.kind && it.name == item.name }) return existing
        val updated = existing + item
        save(updated)
        return updated.sortedByDescending { it.id }
    }

    fun remove(id: Long): List<WatchItem> {
        val updated = load().filterNot { it.id == id }
        save(updated)
        return updated
    }

    private fun save(items: List<WatchItem>) {
        try {
            val arr = JSONArray()
            items.forEach {
                arr.put(
                    JSONObject().put("id", it.id).put("kind", it.kind.name).put("name", it.name),
                )
            }
            file.writeText(arr.toString())
        } catch (e: Exception) {
            // sessizce geç
        }
    }
}
