package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uzaygozlem.asistan.data.WatchItem

@Composable
fun WatchlistScreen(
    watchlist: List<WatchItem>,
    onOpen: (WatchItem) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        SectionLabel("İzleme listesi · ${watchlist.size} kayıt")
        Spacer(Modifier.height(10.dp))

        if (watchlist.isEmpty()) {
            Text(
                "Henüz kayıt yok.\n\nBir uydu geçişinin ya da gökcisminin (gezegen/" +
                    "yıldız) detayında \"⭐ İzleme listesine ekle\" ile buraya " +
                    "kaydedebilirsin. Sonra tıklayınca güncel konumu, ne zaman " +
                    "görüneceği ve pusulasıyla tekrar açılır.",
                style = MaterialTheme.typography.bodyMedium,
                color = Palette.TextSecondary,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(watchlist) { item -> WatchRow(item, onOpen, onDelete) }
            }
        }
    }
}

@Composable
private fun WatchRow(
    item: WatchItem,
    onOpen: (WatchItem) -> Unit,
    onDelete: (Long) -> Unit,
) {
    AppCard(onClick = { onOpen(item) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Palette.TextPrimary,
                )
                Text(
                    text = when (item.kind) {
                        WatchItem.Kind.SKY -> "Gökcismi · detayı görmek için dokun"
                        WatchItem.Kind.SATELLITE -> "Uydu · sonraki geçiş için dokun"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
            }
            Text(
                text = "🗑",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .clickable { onDelete(item.id) }
                    .padding(6.dp),
            )
        }
    }
}
