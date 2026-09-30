package id.ibnurizqia.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** [summary] like "28° Cloudy"; [place] is the area it's for (e.g. "Wanareja"), when the geocoder knows it. */
class Weather(val summary: String, val place: String?)

/** Weather where the phone is now, or null without a location. Caller must hold ACCESS_COARSE_LOCATION. */
suspend fun Context.currentWeather(): Weather? {
    val last = lastKnownLocation()
    // a cached fix can be hours old and from somewhere else, so only take a recent one; a stale one is the last resort
    val location = last?.takeIf { it.ageMillis() < 15 * 60_000 } ?: freshLocation() ?: last ?: return null
    return Weather(fetchWeather(location.latitude, location.longitude), placeName(location))
}

private fun Location.ageMillis() = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

// shown next to the weather so it's obvious which area it's for
@Suppress("DEPRECATION") // the listener variant is API 33+; on a background thread the blocking one is fine
private suspend fun Context.placeName(location: Location): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    runCatching { Geocoder(this@placeName).getFromLocation(location.latitude, location.longitude, 1) }
        .getOrNull()?.firstOrNull()
        ?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
}

@SuppressLint("MissingPermission") // checked by caller
private fun Context.lastKnownLocation(): Location? {
    val lm = getSystemService(LocationManager::class.java)
    return lm.getProviders(true)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
}

// nothing cached yet (fresh boot, no other app used location): ask for one fresh fix
@SuppressLint("MissingPermission")
private suspend fun Context.freshLocation(): Location? = suspendCancellableCoroutine { cont ->
    val signal = CancellationSignal()
    cont.invokeOnCancellation { signal.cancel() }
    runCatching {
        LocationManagerCompat.getCurrentLocation(
            getSystemService(LocationManager::class.java),
            // fused (GPS + Wi-Fi + cell) is guaranteed from Android 12; network-only before that
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) LocationManager.FUSED_PROVIDER else LocationManager.NETWORK_PROVIDER,
            signal,
            ContextCompat.getMainExecutor(this),
        ) { cont.resume(it) }
    }.onFailure { cont.resume(null) } // e.g. provider missing on this device
}

// Open-Meteo: free, no API key
private suspend fun fetchWeather(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
    val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code")
    val conn = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
    }
    val current = conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }.getJSONObject("current")
    "${current.getDouble("temperature_2m").roundToInt()}° ${describe(current.getInt("weather_code"))}".trim()
}

// WMO weather interpretation codes, see open-meteo.com/en/docs
private fun describe(code: Int) = when (code) {
    0 -> "Clear"
    1, 2 -> "Partly cloudy"
    3 -> "Cloudy"
    45, 48 -> "Fog"
    in 51..57 -> "Drizzle"
    in 61..67, in 80..82 -> "Rain"
    in 71..77, 85, 86 -> "Snow"
    in 95..99 -> "Thunderstorm"
    else -> ""
}
