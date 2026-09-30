package id.ibnurizqia.launcher

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import java.io.File
import java.util.Locale

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
