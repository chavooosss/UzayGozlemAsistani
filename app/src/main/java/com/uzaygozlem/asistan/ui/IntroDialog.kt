package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** İlk açılışta bir kez gösterilen kısa tanıtım. */
@Composable
fun IntroDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = { Text("Uzay Gözlem'e hoş geldin 🌌", color = Palette.TextPrimary) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                IntroLine("🌙 Plan", "Bu gece ne izleyebileceğinin özeti — açılış ekranı.")
                IntroLine("🛰 Geçişler", "Uydu geçişleri; kartlara dokun, canlı takip et.")
                IntroLine("✨ Gökyüzü", "Şu an görünen gezegen ve yıldızlar; yöne göre süz.")
                IntroLine("🌠 Meteorlar", "Yaklaşan meteor yağmurları ve radyant yönü.")
                IntroLine("🌕 Ay", "Evre, aydınlanma ve Ay'ın konumu.")
                IntroLine("🗺 Harita", "Uyduların canlı dünya konumu ve gündüz/gece.")
                IntroLine("⭐ İzleme", "İlgini çekeni kaydet, sonra tekrar aç.")
                IntroLine("✔ Günlük", "Gördüklerini kaydet, kendi gözlem arşivini oluştur.")
                Spacer(Modifier.height(8.dp))
                Text(
                    "İpucu: Bir cisme dokununca pusula seni ona doğrultur. " +
                        "Görünür geçişlerden 10 dk önce bildirim alırsın.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Başla") }
        },
    )
}

@Composable
private fun IntroLine(title: String, desc: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Palette.Primary)
        Text(desc, style = MaterialTheme.typography.bodySmall, color = Palette.TextPrimary)
    }
}
