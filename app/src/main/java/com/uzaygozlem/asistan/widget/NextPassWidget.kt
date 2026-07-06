package com.uzaygozlem.asistan.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.uzaygozlem.asistan.MainActivity

/**
 * Ana ekran widget'ı: bu geceki ilk görünür geçişin özeti.
 * Veri, uygulama her yenilendiğinde SharedPreferences'a yazılır ve widget
 * güncellenir; yani en taze bilgi için arada uygulamayı açmak yeterli.
 */
class NextPassWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("uzay_gozlem", Context.MODE_PRIVATE)
        val line1 = prefs.getString("widget_line1", "🛰 Uzay Gözlem") ?: ""
        val line2 = prefs.getString("widget_line2", "Veri için uygulamayı aç") ?: ""

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF181128)))
                    .cornerRadius(20.dp)
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Text(
                    text = line1,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFEFEAF7)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    text = line2,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFA79FBC)),
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}

class NextPassWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextPassWidget()
}
