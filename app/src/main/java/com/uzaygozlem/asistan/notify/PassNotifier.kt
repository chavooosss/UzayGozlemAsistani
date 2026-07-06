package com.uzaygozlem.asistan.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.uzaygozlem.asistan.R
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SatellitePass
import com.uzaygozlem.asistan.astro.Visibility
import com.uzaygozlem.asistan.data.UpcomingShower
import java.time.format.DateTimeFormatter

/**
 * Görünür geçişlerden LEAD_MINUTES dakika önce bildirim atacak alarmları kurar.
 * Her refresh'te önce eski alarmlar iptal edilir, güncel liste yeniden planlanır.
 * (Alarmlar yeniden başlatmada silinir; uygulama tekrar açılınca yeniden kurulur.)
 */
object PassNotifier {

    const val CHANNEL_ID = "gecis_bildirim"
    private const val TAG = "PassNotifier"
    private const val LEAD_MINUTES = 10L
    private const val MAX_ALARMS = 20
    private const val METEOR_BASE_CODE = 100
    private const val MAX_METEOR_ALARMS = 8
    private const val KEY_SAT_ALARMS = "sched_sat_alarms"
    private const val KEY_METEOR_ALARMS = "sched_meteor_alarms"
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Uydu geçiş bildirimleri",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Görünür uydu geçişlerinden $LEAD_MINUTES dakika önce uyarır"
        }
        manager.createNotificationChannel(channel)
    }

    fun scheduleForPasses(context: Context, passes: List<SatellitePass>) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        // Önceki planlamayı temizle
        for (code in 0 until MAX_ALARMS) {
            alarmManager.cancel(alarmIntent(context, code, null, null))
        }

        val now = System.currentTimeMillis()
        val upcoming = passes
            .filter { it.visibility == Visibility.VISIBLE }
            .mapNotNull { pass ->
                val visibleStart = pass.visibleFrom ?: pass.aos
                val triggerAt = visibleStart.toInstant().toEpochMilli() -
                    LEAD_MINUTES * 60_000L
                if (triggerAt > now) Triple(pass, visibleStart, triggerAt) else null
            }
            .sortedBy { it.third }
            .take(MAX_ALARMS)

        val entries = upcoming.mapIndexed { index, (pass, visibleStart, triggerAt) ->
            val shortName = pass.satelliteName.substringBefore(" (")
            val visibleEnd = pass.visibleUntil ?: pass.los
            val title = "$shortName birazdan gökyüzünde 🛰"
            val text = buildString {
                append("${visibleStart.format(timeFormat)} · ")
                append(PassCalculator.azimuthToTurkish(pass.aosAzimuthDeg))
                append(" ufkundan belirecek\n")
                append("En yüksek noktası ${pass.maxElevationDeg.toInt()}° — ")
                append("${visibleEnd.format(timeFormat)}'e kadar izlenebilir\n")
                append("Işıkları kıs, gökyüzüne bak ✨")
            }
            AlarmEntry(index, triggerAt, title, text, 8 * 60_000L)
        }
        entries.forEach { setAlarm(context, alarmManager, it) }
        persist(context, KEY_SAT_ALARMS, entries)
    }

    /**
     * Yaklaşan meteor yağmurlarının zirve akşamı (21:00) için hatırlatma kurar.
     * Uydu alarmlarıyla çakışmasın diye ayrı requestCode aralığı kullanılır.
     */
    fun scheduleForShowers(context: Context, showers: List<UpcomingShower>) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        for (code in METEOR_BASE_CODE until METEOR_BASE_CODE + MAX_METEOR_ALARMS) {
            alarmManager.cancel(alarmIntent(context, code, null, null))
        }

        val now = System.currentTimeMillis()
        val zone = java.time.ZoneId.systemDefault()
        val entries = showers.take(MAX_METEOR_ALARMS).mapIndexedNotNull { index, upcoming ->
            val triggerAt = upcoming.peakDate.atTime(21, 0).atZone(zone)
                .toInstant().toEpochMilli()
            if (triggerAt <= now) return@mapIndexedNotNull null

            val moonNote = when {
                upcoming.moonIlluminationPct < 30 -> "gözlem için harika bir gece"
                upcoming.moonIlluminationPct < 70 -> "orta düzey ay ışığı var"
                else -> "parlak Ay sönük meteorları bastırabilir"
            }
            val title = "${upcoming.shower.name} bu gece zirvede 🌠"
            val text = buildString {
                append("Saatte ~${upcoming.shower.zhr} meteor (ideal koşulda) · ")
                append("radyant: ${upcoming.shower.radiant}\n")
                append("Ay aydınlanması %${upcoming.moonIlluminationPct} — $moonNote\n")
                append("En iyi izleme gece yarısından sonra, şehir ışıklarından uzakta ✨")
            }
            AlarmEntry(METEOR_BASE_CODE + index, triggerAt, title, text, 30 * 60_000L)
        }
        entries.forEach { setAlarm(context, alarmManager, it) }
        persist(context, KEY_METEOR_ALARMS, entries)
    }

    // --- Alarm kurma + kalıcılık (boot sonrası yeniden kurmak için) ---

    private data class AlarmEntry(
        val code: Int, val triggerAt: Long, val title: String,
        val text: String, val windowMs: Long,
    )

    private fun setAlarm(context: Context, alarmManager: AlarmManager, e: AlarmEntry) {
        val pending = alarmIntent(context, e.code, e.title, e.text)
        val canExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, e.triggerAt, pending)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, e.triggerAt, e.windowMs, pending)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("uzay_gozlem", Context.MODE_PRIVATE)

    private fun persist(context: Context, key: String, entries: List<AlarmEntry>) {
        val arr = org.json.JSONArray()
        entries.forEach {
            arr.put(
                org.json.JSONObject()
                    .put("code", it.code).put("t", it.triggerAt)
                    .put("title", it.title).put("text", it.text).put("w", it.windowMs),
            )
        }
        prefs(context).edit().putString(key, arr.toString()).apply()
    }

    /**
     * Telefon yeniden başladığında alarmlar silinir; kayıtlı planı okuyup
     * gelecekteki bildirimleri yeniden kurar (konum/ağ gerektirmez).
     */
    fun rescheduleFromStorage(context: Context) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = System.currentTimeMillis()
        listOf(KEY_SAT_ALARMS, KEY_METEOR_ALARMS).forEach { key ->
            val json = prefs(context).getString(key, null) ?: return@forEach
            try {
                val arr = org.json.JSONArray(json)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val t = o.getLong("t")
                    if (t <= now) continue
                    setAlarm(
                        context, alarmManager,
                        AlarmEntry(o.getInt("code"), t, o.getString("title"),
                            o.getString("text"), o.getLong("w")),
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Alarm geri yükleme hatası: ${e.message}")
            }
        }
    }

    private fun alarmIntent(
        context: Context,
        requestCode: Int,
        title: String?,
        text: String?,
    ): PendingIntent {
        val intent = Intent(context, PassAlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
            putExtra("id", requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Telefon açılışında bildirim alarmlarını yeniden kurar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            PassNotifier.rescheduleFromStorage(context)
        }
    }
}

class PassAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PassNotifier", "Alarm tetiklendi: ${intent.getStringExtra("title")}")
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("PassNotifier", "Bildirim izni yok, gösterilemedi")
            return
        }
        PassNotifier.ensureChannel(context)
        val openApp = PendingIntent.getActivity(
            context,
            0,
            context.packageManager.getLaunchIntentForPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = intent.getStringExtra("text") ?: ""
        val notification = NotificationCompat.Builder(context, PassNotifier.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sat_notify)
            .setContentTitle(intent.getStringExtra("title") ?: "Uydu geçişi yaklaşıyor")
            .setContentText(text.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFFE5484D.toInt())
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context)
            .notify(intent.getIntExtra("id", 0), notification)
    }
}
