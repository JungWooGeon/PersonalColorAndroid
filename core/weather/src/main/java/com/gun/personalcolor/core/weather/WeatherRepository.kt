package com.gun.personalcolor.core.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.roundToLong

/**
 * Today's weather from Open-Meteo (no API key) at the device's coarse location.
 * Cached for an hour; when offline, a result up to 6 hours old is reused ("최근 정보").
 */
@Singleton
class WeatherRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val cache = context.getSharedPreferences("weather", Context.MODE_PRIVATE)

    /** Null when there's no location permission, no location, or no network and no recent cache. Never throws. */
    suspend fun today(): TodayWeather? = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext null
        val now = System.currentTimeMillis()
        val cached = readCache()
        if (cached != null && now - cached.fetchedAtMillis in 0 until FRESH_MS) return@withContext cached
        val fresh = try { fetch(now) } catch (e: Exception) { Log.w(TAG, "fetch failed", e); null }
        fresh?.also { writeCache(it) } ?: cached?.takeIf { now - it.fetchedAtMillis in 0 until STALE_MS }
    }

    private suspend fun fetch(now: Long): TodayWeather? {
        val loc = location() ?: return null
        // Privacy: only ~1km precision ever leaves the device.
        val lat = round2(loc.latitude)
        val lon = round2(loc.longitude)
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${fmt(lat)}&longitude=${fmt(lon)}" +
            "&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,relative_humidity_2m" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,weather_code" +
            "&timezone=auto&forecast_days=1"
        val body = httpGet(url) ?: return null
        return parseForecast(body, placeLabel(lat, lon), now)
    }

    @SuppressLint("MissingPermission") // checked in today()
    private suspend fun location(): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        val current = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).awaitOrNull()
        }
        if (current != null) return current
        cts.cancel()
        return withTimeoutOrNull(2_000) { client.lastLocation.awaitOrNull() }
    }

    private suspend fun placeLabel(lat: Double, lon: Double): String? = try {
        if (!Geocoder.isPresent()) null
        else withTimeoutOrNull(5_000) {
            val geocoder = Geocoder(context, Locale.KOREA)
            val address: Address? = if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) { if (cont.isActive) cont.resume(addresses.firstOrNull()) }
                        override fun onError(errorMessage: String?) { if (cont.isActive) cont.resume(null) }
                    })
                }
            } else {
                @Suppress("DEPRECATION") geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
            }
            address?.let { shortPlace(it.adminArea, it.locality, it.subLocality, it.thoroughfare) }
        }
    } catch (e: Exception) { Log.w(TAG, "geocode failed", e); null }

    private fun httpGet(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            if (conn.responseCode != HttpURLConnection.HTTP_OK) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    private fun readCache(): TodayWeather? =
        cache.getString(KEY, null)?.let { runCatching { json.decodeFromString<TodayWeather>(it) }.getOrNull() }

    private fun writeCache(w: TodayWeather) = cache.edit().putString(KEY, json.encodeToString(w)).apply()

    companion object {
        private const val TAG = "Weather"
        private const val KEY = "today"
        private const val FRESH_MS = 60 * 60_000L
        private const val STALE_MS = 6 * 60 * 60_000L
        private const val LOCATION_TIMEOUT_MS = 8_000L
        private val json = Json { ignoreUnknownKeys = true }

        fun hasPermission(context: Context) =
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        /** True when [w] is older than the fresh window — show it as "최근 정보". */
        fun isStale(w: TodayWeather, now: Long = System.currentTimeMillis()) = now - w.fetchedAtMillis >= FRESH_MS

        private fun round2(v: Double) = (v * 100).roundToLong() / 100.0
        private fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)
    }
}

private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { if (cont.isActive) cont.resume(if (it.isSuccessful) it.result else null) }
}
