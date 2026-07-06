package com.uzaygozlem.asistan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.uzaygozlem.asistan.astro.SatellitePass
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uzaygozlem.asistan.location.LocationProvider
import com.uzaygozlem.asistan.ui.HomeSummary
import com.uzaygozlem.asistan.ui.IntroDialog
import com.uzaygozlem.asistan.ui.JournalScreen
import com.uzaygozlem.asistan.ui.LiveTrackScreen
import com.uzaygozlem.asistan.ui.MapScreen
import com.uzaygozlem.asistan.ui.MeteorDetailScreen
import com.uzaygozlem.asistan.ui.MeteorsScreen
import com.uzaygozlem.asistan.ui.MoonScreen
import com.uzaygozlem.asistan.ui.NightModeButton
import com.uzaygozlem.asistan.ui.Palette
import com.uzaygozlem.asistan.ui.PlanScreen
import com.uzaygozlem.asistan.ui.PassDetailScreen
import com.uzaygozlem.asistan.ui.PassesScreen
import com.uzaygozlem.asistan.ui.PillTabs
import com.uzaygozlem.asistan.data.WatchItem
import com.uzaygozlem.asistan.ui.SkyObjectDetailScreen
import com.uzaygozlem.asistan.ui.SkyScreen
import com.uzaygozlem.asistan.ui.UzayGozlemTheme
import com.uzaygozlem.asistan.ui.WatchlistScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UzayGozlemTheme {
                App()
            }
        }
    }
}

private val TAB_TITLES =
    listOf("Plan", "Geçişler", "Gökyüzü", "Meteorlar", "Ay", "Harita", "İzleme", "Günlük")

@Composable
private fun App(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPass by remember { mutableStateOf<SatellitePass?>(null) }
    var liveTrackPass by remember { mutableStateOf<SatellitePass?>(null) }
    var skyObjectName by remember { mutableStateOf<String?>(null) }
    var meteorShower by remember {
        mutableStateOf<com.uzaygozlem.asistan.data.UpcomingShower?>(null)
    }
    val prefs = remember { context.getSharedPreferences("uzay_gozlem", Context.MODE_PRIVATE) }
    var nightMode by remember { mutableStateOf(prefs.getBoolean("night_mode", false)) }
    var showIntro by remember { mutableStateOf(!prefs.getBoolean("seen_intro", false)) }

    if (showIntro) {
        IntroDialog(onDismiss = {
            showIntro = false
            prefs.edit().putBoolean("seen_intro", true).apply()
        })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            LocationProvider.hasPermission(context)
        viewModel.onPermissionResult(locationGranted)
    }

    LaunchedEffect(Unit) {
        val needed = mutableListOf<String>()
        if (!LocationProvider.hasPermission(context)) {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
            needed += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            needed += Manifest.permission.POST_NOTIFICATIONS
        }
        if (needed.isEmpty()) {
            viewModel.refresh()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Gece modu: tüm arayüzü kırmızıyla çarparak (multiply) göze
            // mavi/beyaz ışık gitmesini engeller, karanlık adaptasyonu korur.
            // Arka plan degrade layer'ın İÇİNDE olmalı ki o da kararabilsin.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                if (nightMode) {
                    drawRect(
                        color = Color(0xFFFF2A1A),
                        blendMode = BlendMode.Multiply,
                    )
                }
            }
            .background(
                Brush.verticalGradient(
                    listOf(Palette.BackgroundTop, Palette.BackgroundBottom),
                ),
            ),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
        ) { padding ->
            val livePass = liveTrackPass
            val detailPass = selectedPass
            val skyName = skyObjectName
            val meteor = meteorShower
            if (meteor != null) {
                Box(Modifier.padding(padding)) {
                    MeteorDetailScreen(
                        upcoming = meteor,
                        observerLat = state.observerLat,
                        observerLon = state.observerLon,
                        onBack = { meteorShower = null },
                    )
                }
            } else if (skyName != null && state.observerLat != null && state.observerLon != null) {
                Box(Modifier.padding(padding)) {
                    SkyObjectDetailScreen(
                        objectName = skyName,
                        observerLat = state.observerLat!!,
                        observerLon = state.observerLon!!,
                        onBack = { skyObjectName = null },
                        onWatch = { viewModel.addToWatchlist(WatchItem.Kind.SKY, it) },
                        inWatchlist = state.watchlist.any {
                            it.kind == WatchItem.Kind.SKY && it.name == skyName
                        },
                    )
                }
            } else if (livePass != null) {
                Box(Modifier.padding(padding)) {
                    LiveTrackScreen(
                        pass = livePass,
                        onBack = { liveTrackPass = null },
                    )
                }
            } else if (detailPass != null) {
                Box(Modifier.padding(padding)) {
                    PassDetailScreen(
                        pass = detailPass,
                        onBack = { selectedPass = null },
                        onLiveTrack = { liveTrackPass = detailPass },
                        onLogObservation = { note ->
                            viewModel.addObservation(detailPass, note)
                        },
                        onWatch = { viewModel.addToWatchlist(WatchItem.Kind.SATELLITE, it) },
                        inWatchlist = state.watchlist.any {
                            it.kind == WatchItem.Kind.SATELLITE &&
                                it.name == detailPass.satelliteName.substringBefore(" (")
                        },
                    )
                }
            } else {
                Column(Modifier.padding(padding)) {
                    HomeSummary(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PillTabs(
                            titles = TAB_TITLES,
                            selected = selectedTab,
                            onSelect = { selectedTab = it },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        NightModeButton(
                            active = nightMode,
                            onToggle = {
                                nightMode = !nightMode
                                prefs.edit().putBoolean("night_mode", nightMode).apply()
                            },
                        )
                    }
                    Box(Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            0 -> PlanScreen(
                                passes = state.passes,
                                moon = state.moon,
                                showers = state.showers,
                                observerLat = state.observerLat,
                                observerLon = state.observerLon,
                            )
                            1 -> PassesScreen(
                                state = state,
                                onRetry = { viewModel.refresh() },
                                onManualLocation = { lat, lon ->
                                    viewModel.setManualLocation(lat, lon)
                                },
                                onPassClick = { selectedPass = it },
                                onSatelliteSelection = {
                                    viewModel.setSelectedSatellites(it)
                                },
                                onAddSatelliteById = { viewModel.addSatelliteById(it) },
                            )
                            2 -> SkyScreen(
                                observerLat = state.observerLat,
                                observerLon = state.observerLon,
                                onObjectClick = { skyObjectName = it },
                            )
                            3 -> MeteorsScreen(
                                showers = state.showers,
                                onShowerClick = { meteorShower = it },
                            )
                            4 -> state.moon?.let {
                                MoonScreen(
                                    moon = it,
                                    onOpenDetail = if (state.observerLat != null &&
                                        state.observerLon != null
                                    ) {
                                        { skyObjectName = "Ay" }
                                    } else null,
                                )
                            }
                            5 -> MapScreen(
                                tles = state.tles,
                                observerLat = state.observerLat,
                                observerLon = state.observerLon,
                            )
                            6 -> WatchlistScreen(
                                watchlist = state.watchlist,
                                onOpen = { item ->
                                    when (item.kind) {
                                        WatchItem.Kind.SKY -> skyObjectName = item.name
                                        WatchItem.Kind.SATELLITE -> {
                                            val p = state.passes.firstOrNull {
                                                it.satelliteName.substringBefore(" (") == item.name
                                            }
                                            if (p != null) selectedPass = p
                                        }
                                    }
                                },
                                onDelete = { viewModel.removeFromWatchlist(it) },
                            )
                            7 -> JournalScreen(
                                observations = state.observations,
                                onDelete = { viewModel.removeObservation(it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
