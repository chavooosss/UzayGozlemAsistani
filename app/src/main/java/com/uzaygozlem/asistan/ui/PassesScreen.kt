package com.uzaygozlem.asistan.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uzaygozlem.asistan.UiState
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.data.SATELLITE_CATALOG
import com.uzaygozlem.asistan.astro.SatellitePass
import com.uzaygozlem.asistan.astro.Visibility
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassesScreen(
    state: UiState,
    onRetry: () -> Unit,
    onManualLocation: (Double, Double) -> Unit,
    onPassClick: (SatellitePass) -> Unit,
    onSatelliteSelection: (Set<Int>) -> Unit,
    onAddSatelliteById: (Int) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        SatellitePickerDialog(
            catalog = SATELLITE_CATALOG + state.customSatellites,
            selected = state.selectedSatelliteIds,
            adding = state.addingSatellite,
            addError = state.addSatelliteError,
            onDismiss = { showPicker = false },
            onSave = { ids ->
                showPicker = false
                onSatelliteSelection(ids)
            },
            onAddById = onAddSatelliteById,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.locationText?.let { "📍 $it · 24 saat" } ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.TextSecondary,
            )
            Box(Modifier.clickable { showPicker = true }) {
                StatusChip(
                    "🛰 Uydu seç (${state.selectedSatelliteIds.size})",
                    Palette.TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        when {
            state.loading && state.passes.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = Palette.Primary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Yörüngeler hesaplanıyor…",
                        color = Palette.TextSecondary,
                    )
                }
            }
            state.error != null && !state.loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRetry) { Text("Tekrar Dene") }
                    if (state.askManualLocation) {
                        Spacer(Modifier.height(24.dp))
                        ManualLocationForm(onManualLocation)
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.loading,
                    onRefresh = onRetry,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.warning?.let { warning ->
                            item {
                                Text(
                                    text = warning,
                                    color = Palette.Gold,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        if (state.passes.isEmpty()) {
                            item {
                                Text(
                                    "Önümüzdeki 24 saatte geçiş bulunamadı.",
                                    color = Palette.TextSecondary,
                                )
                            }
                        } else {
                            items(state.passes) { pass ->
                                PassCard(pass, onClick = { onPassClick(pass) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SatellitePickerDialog(
    catalog: List<com.uzaygozlem.asistan.data.TrackedSatellite>,
    selected: Set<Int>,
    adding: Boolean,
    addError: String?,
    onDismiss: () -> Unit,
    onSave: (Set<Int>) -> Unit,
    onAddById: (Int) -> Unit,
) {
    var current by remember { mutableStateOf(selected) }
    var idText by remember { mutableStateOf("") }
    // Yeni eklenen uydu otomatik seçilir; listeye yansısın
    LaunchedEffect(catalog.size) {
        current = current + catalog.map { it.noradId }.filter { it in selected }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = {
            Text("Takip edilecek uydular", color = Palette.TextPrimary)
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                catalog.forEach { satellite ->
                    val checked = satellite.noradId in current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                current = if (checked) current - satellite.noradId
                                else current + satellite.noradId
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                current = if (isChecked) current + satellite.noradId
                                else current - satellite.noradId
                            },
                        )
                        Column {
                            Text(
                                satellite.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Palette.TextPrimary,
                            )
                            Text(
                                "NORAD ${satellite.noradId} · ✦ ~%.1f kadir"
                                    .format(satellite.standardMagnitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.TextSecondary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                SectionLabel("NORAD ID ile ekle")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Uydunun numarasını gir (ör. ISS = 25544). Numarayı " +
                        "n2yo.com veya celestrak.org'dan bulabilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSecondary,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = idText,
                        onValueChange = { s -> idText = s.filter { it.isDigit() } },
                        label = { Text("NORAD ID") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            idText.toIntOrNull()?.let { onAddById(it) }
                            idText = ""
                        },
                        enabled = !adding && idText.toIntOrNull() != null,
                    ) { Text(if (adding) "…" else "Ekle") }
                }
                addError?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(current) },
                enabled = current.isNotEmpty(),
            ) { Text("Uygula") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}

@Composable
private fun ManualLocationForm(onManualLocation: (Double, Double) -> Unit) {
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    val lat = latText.replace(',', '.').toDoubleOrNull()
    val lon = lonText.replace(',', '.').toDoubleOrNull()
    val valid = lat != null && lat in -90.0..90.0 && lon != null && lon in -180.0..180.0

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Manuel konum (ondalık derece):",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Enlem") },
                modifier = Modifier.width(140.dp),
                singleLine = true,
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = lonText,
                onValueChange = { lonText = it },
                label = { Text("Boylam") },
                modifier = Modifier.width(140.dp),
                singleLine = true,
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { if (valid) onManualLocation(lat!!, lon!!) },
            enabled = valid,
        ) { Text("Bu Konumu Kullan") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassCard(pass: SatellitePass, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pass.satelliteName.substringBefore(" ("),
                style = MaterialTheme.typography.titleMedium,
                color = Palette.TextPrimary,
            )
            StatusChip(dayLabel(pass), Palette.TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            TimelinePoint(
                label = "Yükseliş",
                time = pass.aos.format(timeFormat),
                detail = PassCalculator.azimuthToTurkish(pass.aosAzimuthDeg),
                modifier = Modifier.weight(1f),
                align = Alignment.Start,
            )
            TimelinePoint(
                label = "Zirve",
                time = pass.tca.format(timeFormat),
                detail = "${pass.maxElevationDeg.toInt()}° yükseklik",
                modifier = Modifier.weight(1f),
                align = Alignment.CenterHorizontally,
            )
            TimelinePoint(
                label = "Batış",
                time = pass.los.format(timeFormat),
                detail = PassCalculator.azimuthToTurkish(pass.losAzimuthDeg),
                modifier = Modifier.weight(1f),
                align = Alignment.End,
            )
        }
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            VisibilityBadge(pass)
            pass.cloudCoverPct?.let { CloudChip(it) }
            if (pass.visibility == Visibility.VISIBLE) {
                pass.magnitudeAtPeak?.let { MagnitudeChip(it) }
            }
        }
    }
}

@Composable
private fun TimelinePoint(
    label: String,
    time: String,
    detail: String,
    modifier: Modifier = Modifier,
    align: Alignment.Horizontal,
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        SectionLabel(label)
        Spacer(Modifier.height(2.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.titleMedium,
            color = Palette.TextPrimary,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = Palette.TextSecondary,
        )
    }
}

@Composable
private fun VisibilityBadge(pass: SatellitePass) {
    when (pass.visibility) {
        Visibility.VISIBLE -> {
            val window = if (pass.visibleFrom != null && pass.visibleUntil != null) {
                " · ${pass.visibleFrom.format(timeFormat)}–${pass.visibleUntil.format(timeFormat)}"
            } else ""
            StatusChip("● GÖRÜNÜR$window", Palette.Green)
        }
        Visibility.ECLIPSED ->
            StatusChip("Görünmez · Dünya'nın gölgesinde", Palette.TextSecondary)
        Visibility.SKY_TOO_BRIGHT ->
            StatusChip("Görünmez · Gökyüzü aydınlık", Palette.TextSecondary)
    }
}

private fun dayLabel(pass: SatellitePass): String {
    val today = LocalDate.now()
    return when (pass.aos.toLocalDate()) {
        today -> "Bugün"
        today.plusDays(1) -> "Yarın"
        else -> pass.aos.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}
