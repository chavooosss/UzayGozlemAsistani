package com.uzaygozlem.asistan

import android.app.Application
import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uzaygozlem.asistan.astro.MoonCalc
import com.uzaygozlem.asistan.astro.MoonPhase
import com.uzaygozlem.asistan.astro.PassCalculator
import com.uzaygozlem.asistan.astro.SatellitePass
import com.uzaygozlem.asistan.astro.Visibility
import com.uzaygozlem.asistan.data.DEFAULT_SATELLITE_IDS
import com.uzaygozlem.asistan.data.Observation
import com.uzaygozlem.asistan.data.ObservationRepository
import com.uzaygozlem.asistan.data.SATELLITE_CATALOG
import com.uzaygozlem.asistan.data.TleRepository
import com.uzaygozlem.asistan.data.TrackedSatellite
import com.uzaygozlem.asistan.data.UpcomingShower
import com.uzaygozlem.asistan.data.WatchItem
import com.uzaygozlem.asistan.data.WatchlistRepository
import com.uzaygozlem.asistan.data.WeatherRepository
import com.uzaygozlem.asistan.data.upcomingShowers
import com.uzaygozlem.asistan.location.LocationProvider
import com.uzaygozlem.asistan.notify.PassNotifier
import com.uzaygozlem.asistan.widget.NextPassWidget
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MoonInfo(
    val phase: MoonPhase,
    val illuminationPct: Double,
    val daysToNewMoon: Double,
    val daysToFullMoon: Double,
)

data class GeoPoint(val latitude: Double, val longitude: Double, val altitudeM: Double = 0.0)

/** Harita sekmesi için: bir uydunun adı + güncel TLE'si. */
data class SatelliteTle(val noradId: Int, val displayName: String, val lines: List<String>)

data class UiState(
    val loading: Boolean = false,
    val error: String? = null,
    val warning: String? = null,
    val locationText: String? = null,
    val passes: List<SatellitePass> = emptyList(),
    val permissionDenied: Boolean = false,
    val askManualLocation: Boolean = false,
    val moon: MoonInfo? = null,
    val showers: List<UpcomingShower> = emptyList(),
    val selectedSatelliteIds: Set<Int> = DEFAULT_SATELLITE_IDS,
    val tles: List<SatelliteTle> = emptyList(),
    val observations: List<Observation> = emptyList(),
    val observerLat: Double? = null,
    val observerLon: Double? = null,
    val customSatellites: List<TrackedSatellite> = emptyList(),
    val addingSatellite: Boolean = false,
    val addSatelliteError: String? = null,
    val watchlist: List<WatchItem> = emptyList(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TleRepository(application.filesDir)
    private val weatherRepository = WeatherRepository(application.filesDir)
    private val observationRepository = ObservationRepository(application.filesDir)
    private val watchlistRepository = WatchlistRepository(application.filesDir)
    private val prefs = application.getSharedPreferences("uzay_gozlem", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        _uiState.value = _uiState.value.copy(
            selectedSatelliteIds = selectedIds(),
            observations = observationRepository.load(),
            customSatellites = loadCustomSatellites(),
            watchlist = watchlistRepository.load(),
        )
        computeSkyAlmanac()
    }

    private fun effectiveCatalog(): List<TrackedSatellite> =
        SATELLITE_CATALOG + _uiState.value.customSatellites

    private fun loadCustomSatellites(): List<TrackedSatellite> {
        val json = prefs.getString("custom_sats", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(TrackedSatellite(o.getInt("id"), o.getString("name"), o.optDouble("mag", 4.5)))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCustomSatellites(list: List<TrackedSatellite>) {
        val arr = org.json.JSONArray()
        list.forEach {
            arr.put(
                org.json.JSONObject()
                    .put("id", it.noradId).put("name", it.displayName)
                    .put("mag", it.standardMagnitude),
            )
        }
        prefs.edit().putString("custom_sats", arr.toString()).apply()
    }

    fun clearAddSatelliteError() {
        _uiState.value = _uiState.value.copy(addSatelliteError = null)
    }

    /** NORAD ID ile katalog dışı uydu ekler; TLE'den adını çeker. */
    fun addSatelliteById(noradId: Int) {
        if (effectiveCatalog().any { it.noradId == noradId }) {
            // Zaten katalogda — sadece seç
            setSelectedSatellites(_uiState.value.selectedSatelliteIds + noradId)
            return
        }
        _uiState.value = _uiState.value.copy(addingSatellite = true, addSatelliteError = null)
        viewModelScope.launch {
            when (val result = repository.getTle(noradId)) {
                is TleRepository.TleResult.Success -> {
                    val name = result.lines.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                        ?: "NORAD $noradId"
                    val sat = TrackedSatellite(noradId, "$name (özel)", 4.5)
                    val custom = _uiState.value.customSatellites
                        .filterNot { it.noradId == noradId } + sat
                    saveCustomSatellites(custom)
                    val newSelected = _uiState.value.selectedSatelliteIds + noradId
                    prefs.edit()
                        .putStringSet("selected_sats", newSelected.map { it.toString() }.toSet())
                        .apply()
                    _uiState.value = _uiState.value.copy(
                        customSatellites = custom,
                        selectedSatelliteIds = newSelected,
                        addingSatellite = false,
                    )
                    refresh()
                }
                is TleRepository.TleResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        addingSatellite = false,
                        addSatelliteError = "Bu ID ($noradId) için uydu verisi bulunamadı. " +
                            "NORAD numarasını ve internet bağlantısını kontrol edin.",
                    )
                }
            }
        }
    }

    private fun selectedIds(): Set<Int> =
        prefs.getStringSet("selected_sats", null)
            ?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?.ifEmpty { DEFAULT_SATELLITE_IDS }
            ?: DEFAULT_SATELLITE_IDS

    fun setSelectedSatellites(ids: Set<Int>) {
        if (ids.isEmpty()) return
        prefs.edit()
            .putStringSet("selected_sats", ids.map { it.toString() }.toSet())
            .apply()
        _uiState.value = _uiState.value.copy(selectedSatelliteIds = ids)
        refresh()
    }

    fun addObservation(pass: SatellitePass, note: String) {
        val observation = Observation(
            id = System.currentTimeMillis(),
            timestampMs = System.currentTimeMillis(),
            satelliteName = pass.satelliteName.substringBefore(" ("),
            maxElevationDeg = pass.maxElevationDeg.toInt(),
            magnitude = pass.magnitudeAtPeak,
            note = note.trim(),
        )
        _uiState.value = _uiState.value.copy(
            observations = observationRepository.add(observation),
        )
    }

    fun removeObservation(id: Long) {
        _uiState.value = _uiState.value.copy(
            observations = observationRepository.remove(id),
        )
    }

    fun addToWatchlist(kind: WatchItem.Kind, name: String) {
        val item = WatchItem(System.currentTimeMillis(), kind, name.substringBefore(" ("))
        _uiState.value = _uiState.value.copy(watchlist = watchlistRepository.add(item))
    }

    fun removeFromWatchlist(id: Long) {
        _uiState.value = _uiState.value.copy(watchlist = watchlistRepository.remove(id))
    }

    private fun computeSkyAlmanac() {
        val now = System.currentTimeMillis()
        val showers = upcomingShowers()
        _uiState.value = _uiState.value.copy(
            moon = MoonInfo(
                phase = MoonCalc.phase(now),
                illuminationPct = MoonCalc.illuminationPercent(now),
                daysToNewMoon = MoonCalc.daysUntilElongation(now, 0.0),
                daysToFullMoon = MoonCalc.daysUntilElongation(now, 180.0),
            ),
            showers = showers,
        )
        PassNotifier.scheduleForShowers(getApplication(), showers)
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            refresh()
        } else {
            _uiState.value = _uiState.value.copy(
                permissionDenied = true,
                loading = false,
                askManualLocation = savedManualLocation() == null,
                error = if (savedManualLocation() == null)
                    "Konum izni verilmedi. Geçişler için aşağıdan manuel konum girebilirsiniz."
                else null,
            )
            if (savedManualLocation() != null) refresh()
        }
    }

    fun setManualLocation(latitude: Double, longitude: Double) {
        prefs.edit()
            .putFloat("manual_lat", latitude.toFloat())
            .putFloat("manual_lon", longitude.toFloat())
            .apply()
        refresh()
    }

    private fun savedManualLocation(): GeoPoint? {
        if (!prefs.contains("manual_lat")) return null
        return GeoPoint(
            prefs.getFloat("manual_lat", 0f).toDouble(),
            prefs.getFloat("manual_lon", 0f).toDouble(),
        )
    }

    private suspend fun resolveLocation(): GeoPoint? {
        val fromGps = LocationProvider.getLocation(getApplication())
        if (fromGps != null) {
            return GeoPoint(fromGps.latitude, fromGps.longitude, fromGps.altitude)
        }
        return savedManualLocation()
    }

    private data class SatFetchResult(
        val satellite: TrackedSatellite,
        val passes: List<SatellitePass>?,
        val stale: Boolean,
        val tleLines: List<String>?,
    )

    fun refresh() {
        computeSkyAlmanac()
        _uiState.value = _uiState.value.copy(loading = true, error = null, warning = null)
        viewModelScope.launch {
            val location = resolveLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    askManualLocation = true,
                    error = "Konum alınamadı. Konum servisini açın ya da manuel konum girin.",
                )
                return@launch
            }
            val locationText = String.format(
                Locale.getDefault(), "%.3f°, %.3f°", location.latitude, location.longitude,
            )

            val activeSatellites = effectiveCatalog().filter {
                it.noradId in _uiState.value.selectedSatelliteIds
            }

            // Uydular ve hava durumu paralel işlenir
            val (results, cloudCover) = coroutineScope {
                val weatherDeferred = async(Dispatchers.IO) {
                    weatherRepository.getCloudCover(location.latitude, location.longitude)
                }
                val satelliteResults = activeSatellites.map { satellite ->
                    async(Dispatchers.Default) {
                        when (val result = repository.getTle(satellite.noradId)) {
                            is TleRepository.TleResult.Success -> try {
                                SatFetchResult(
                                    satellite,
                                    PassCalculator.computePasses(
                                        tleLines = result.lines,
                                        displayName = satellite.displayName,
                                        latitude = location.latitude,
                                        longitude = location.longitude,
                                        altitudeMeters = location.altitudeM,
                                        standardMagnitude = satellite.standardMagnitude,
                                    ),
                                    result.stale,
                                    result.lines,
                                )
                            } catch (e: Exception) {
                                SatFetchResult(satellite, null, false, result.lines)
                            }
                            is TleRepository.TleResult.Failure ->
                                SatFetchResult(satellite, null, false, null)
                        }
                    }
                }.awaitAll()
                satelliteResults to weatherDeferred.await()
            }

            val allPasses = results.flatMap { it.passes ?: emptyList() }
            val anyStale = results.any { it.stale }
            val failures = results.filter { it.passes == null }
                .map { it.satellite.displayName }
            val tles = results.mapNotNull { r ->
                r.tleLines?.let {
                    SatelliteTle(r.satellite.noradId, r.satellite.displayName, it)
                }
            }

            val warning = buildList {
                if (anyStale) add("TLE verisi güncellenemedi, önbellekteki eski veri kullanılıyor.")
                if (failures.isNotEmpty()) add("Veri alınamayan uydular: ${failures.joinToString()}")
            }.joinToString(" ").ifEmpty { null }

            // Her geçişe zirve saatine en yakın bulutluluk tahminini iliştir
            fun cloudAt(time: ZonedDateTime): Int? {
                if (cloudCover.isEmpty()) return null
                val target = time.toEpochSecond()
                val nearest = cloudCover.keys.minByOrNull { abs(it - target) } ?: return null
                return if (abs(nearest - target) <= 5400) cloudCover[nearest] else null
            }

            val sortedPasses = allPasses.sortedBy { it.aos }
                .map { it.copy(cloudCoverPct = cloudAt(it.tca)) }

            _uiState.value = _uiState.value.copy(
                loading = false,
                error = if (allPasses.isEmpty() && failures.size == activeSatellites.size)
                    "Uydu verisi alınamadı. İnternet bağlantınızı kontrol edin." else null,
                warning = warning,
                locationText = locationText,
                passes = sortedPasses,
                askManualLocation = false,
                tles = tles,
                observerLat = location.latitude,
                observerLon = location.longitude,
            )
            PassNotifier.scheduleForPasses(getApplication(), sortedPasses)
            updateWidget(sortedPasses)
        }
    }

    private suspend fun updateWidget(passes: List<SatellitePass>) {
        val firstVisible = passes.firstOrNull { it.visibility == Visibility.VISIBLE }
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
        val (line1, line2) = if (firstVisible != null) {
            val name = firstVisible.satelliteName.substringBefore(" (")
            val start = (firstVisible.visibleFrom ?: firstVisible.aos).format(timeFormat)
            val cloud = firstVisible.cloudCoverPct?.let { " · ☁ %$it" } ?: ""
            "🛰 $name · $start" to
                "En yüksek ${firstVisible.maxElevationDeg.toInt()}°$cloud · " +
                PassCalculator.azimuthToTurkish(firstVisible.aosAzimuthDeg)
        } else {
            "🛰 Uzay Gözlem" to "24 saatte görünür geçiş yok"
        }
        prefs.edit()
            .putString("widget_line1", line1)
            .putString("widget_line2", line2)
            .apply()
        try {
            NextPassWidget().updateAll(getApplication())
        } catch (e: Exception) {
            // widget eklenmemişse sorun değil
        }
    }
}
