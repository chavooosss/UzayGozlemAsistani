package com.uzaygozlem.asistan.ui

import android.content.Context
import android.content.Intent
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SatellitePass
import com.uzaygozlem.asistan.astro.Visibility
import java.time.format.DateTimeFormatter

private val shareTimeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val shareDateFmt = DateTimeFormatter.ofPattern("d MMMM")

/** Bir metni sistemin paylaş menüsüyle gönderir. */
fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Paylaş"))
}

/** Bir uydu geçişini okunur bir metin olarak biçimlendirir. */
fun formatPassForShare(pass: SatellitePass): String {
    val name = pass.satelliteName.substringBefore(" (")
    val sb = StringBuilder()
    sb.append("🛰 $name geçişi · ${pass.aos.format(shareDateFmt)}\n")
    sb.append("Yükseliş ${pass.aos.format(shareTimeFmt)} " +
        "(${PassCalculator.azimuthToTurkish(pass.aosAzimuthDeg)}) → ")
    sb.append("Zirve ${pass.tca.format(shareTimeFmt)} (${pass.maxElevationDeg.toInt()}°) → ")
    sb.append("Batış ${pass.los.format(shareTimeFmt)} " +
        "(${PassCalculator.azimuthToTurkish(pass.losAzimuthDeg)})\n")
    when (pass.visibility) {
        Visibility.VISIBLE -> sb.append(
            "● Çıplak gözle GÖRÜNÜR: ${pass.visibleFrom?.format(shareTimeFmt)}–" +
                "${pass.visibleUntil?.format(shareTimeFmt)}",
        )
        Visibility.ECLIPSED -> sb.append("○ Görünmez (Dünya'nın gölgesinde)")
        Visibility.SKY_TOO_BRIGHT -> sb.append("○ Görünmez (gökyüzü aydınlık)")
    }
    pass.magnitudeAtPeak?.let { sb.append(" · ✦ %.1f kadir".format(it)) }
    sb.append("\n— Uzay Gözlem Asistanı")
    return sb.toString()
}
