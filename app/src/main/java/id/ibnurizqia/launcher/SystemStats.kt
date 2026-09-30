package id.ibnurizqia.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import java.io.File
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlin.math.abs

/**
 * A snapshot for the home screen. [cpuMhz] is each online core's current clock (empty where the kernel
 * hides it); [thermal] is PowerManager's status, 0 = normal.
 */
class SystemStats(val ramUsed: Long, val ramTotal: Long, val cpuMhz: List<Int>, val thermal: Int)

// same files CPU-info apps read; system-wide CPU *usage* (/proc/stat) is off limits to apps since Android 8
private val cpuFreqFiles by lazy {
    File("/sys/devices/system/cpu").listFiles { f -> f.name.matches(Regex("cpu\\d+")) }.orEmpty()
        .map { File(it, "cpufreq/scaling_cur_freq") }
}

/** Cheap: one binder call for memory and thermal plus a few tiny sysfs reads. Call off the main thread. */
fun Context.systemStats(): SystemStats {
    val memory = ActivityManager.MemoryInfo().also { getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
    val cpuMhz = cpuFreqFiles
        .mapNotNull { runCatching { it.readText().trim().toInt() / 1000 }.getOrNull() } // offline cores just fail
        .filter { it >= 100 } // placeholder values (emulators report "1") would read as 0.0 GHz
    val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) getSystemService(PowerManager::class.java).currentThermalStatus else 0
    return SystemStats(memory.totalMem - memory.availMem, memory.totalMem, cpuMhz, thermal)
}

/** "RAM 8.0 / 11.1 GB" */
fun SystemStats.ramText(): String {
    fun gb(bytes: Long) = String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024 * 1024))
    return "RAM ${gb(ramUsed)} / ${gb(ramTotal)} GB"
}

/** "CPU 1.4–2.2 GHz" (slowest to fastest core), plus "· Warm/Hot/Very hot" only when the phone heats up; null without CPU data. */
fun SystemStats.cpuText(): String? {
    if (cpuMhz.isEmpty()) return null
    fun ghz(mhz: Int) = String.format(Locale.US, "%.1f", mhz / 1000.0)
    val speed = if (cpuMhz.min() == cpuMhz.max()) ghz(cpuMhz.max()) else "${ghz(cpuMhz.min())}–${ghz(cpuMhz.max())}"
    val heat = when (thermal) {
        PowerManager.THERMAL_STATUS_NONE -> null
        PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
        PowerManager.THERMAL_STATUS_MODERATE -> "Hot"
        else -> "Very hot" // severe and up: the system is throttling
    }
    return listOfNotNull("CPU $speed GHz", heat).joinToString("  ·  ")
}

/** Everything Android shares about the battery; nullable fields are the ones some phones don't expose. */
class BatteryStats(
    val level: Int,
    val status: Int,
    val plugged: Int,
    val health: Int,
    val tempC: Float,
    val voltageMv: Int,
    val technology: String?,
    val cycles: Int?,
    val designMah: Int?,
    val currentMa: Int?, // magnitude; direction comes from [charging]
) {
    // needs a power source too: some firmwares (and emulators) keep reporting CHARGING right after unplugging
    val charging get() = status == BatteryManager.BATTERY_STATUS_CHARGING && plugged != 0
}

/** Latest battery reading: the sticky broadcast (no receiver kept) plus the live current. Call off the main thread. */
fun Context.batteryStats(): BatteryStats? {
    val intent = ContextCompat.registerReceiver(this, null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        ?: return null
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val raw = getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    return BatteryStats(
        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale,
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
        plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
        tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
        voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
        technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
        cycles = (if (Build.VERSION.SDK_INT >= 34) intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) else -1)
            .takeIf { it >= 0 } ?: sysfsInt("cycle_count"),
        designMah = sysfsInt("charge_full_design")?.div(1000)?.takeIf { it in 1_000..20_000 }, // µAh; not public API
        // µA by spec, but some OEMs report mA and signs vary, so keep the magnitude and trust the charging status
        currentMa = raw.takeIf { it != Int.MIN_VALUE && it != 0 }?.let { abs(it) }?.let { if (it > 20_000) it / 1000 else it },
    )
}

private fun sysfsInt(name: String) =
    runCatching { File("/sys/class/power_supply/battery/$name").readText().trim().toInt() }.getOrNull()

/** Home line: "Using 420 mA · 37.3°C" / "Charging 1520 mA · 37.3°C". */
fun BatteryStats.lineText(): String {
    val flow = currentMa?.let { if (charging) "Charging $it mA" else "Using $it mA" } ?: statusText()
    return "$flow  ·  ${String.format(Locale.US, "%.1f", tempC)}°C"
}

fun BatteryStats.statusText() = when (status) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
    BatteryManager.BATTERY_STATUS_FULL -> "Full"
    else -> "Unknown"
}

/** Label/value rows for the detail dialog, skipping what this phone doesn't report. */
fun BatteryStats.detailRows(): List<Pair<String, String>> = listOfNotNull(
    "Status" to statusText(),
    "Power source" to when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "Charger"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        0 -> "Battery"
        else -> "Dock"
    },
    "Level" to "$level%",
    "Health" to when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    },
    cycles?.let { "Cycle count" to "$it" },
    technology?.let { "Technology" to it },
    designMah?.let { "Design capacity" to "$it mAh" },
    "Temperature" to String.format(Locale.US, "%.1f °C", tempC),
    "Voltage" to String.format(Locale.US, "%.2f V", voltageMv / 1000f),
    currentMa?.let { "Current" to (if (charging) "+" else "−") + "$it mA" },
)
