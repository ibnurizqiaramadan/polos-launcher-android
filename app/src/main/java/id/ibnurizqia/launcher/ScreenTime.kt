package id.ibnurizqia.launcher

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/** Granted in Settings > Usage access, not through a runtime dialog. */
fun Context.hasUsageAccess(): Boolean {
    val appOps = getSystemService(AppOpsManager::class.java)
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * Foreground time of other apps since local midnight, in millis.
 * ponytail: sums resume→pause pairs per activity like Digital Wellbeing, but won't match it to the minute
 * (split screen double-counts, an app already open at midnight is missed).
 */
@SuppressLint("InlinedApi") // ACTIVITY_RESUMED/PAUSED keep the old MOVE_TO_FOREGROUND/BACKGROUND values
fun Context.screenTimeToday(): Long {
    val midnight = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val events = getSystemService(UsageStatsManager::class.java).queryEvents(midnight, System.currentTimeMillis())
    val event = UsageEvents.Event()
    val resumedAt = HashMap<String, Long>()
    var total = 0L
    while (events.getNextEvent(event)) {
        if (event.packageName == packageName) continue // time on the home screen isn't screen time
        val activity = "${event.packageName}/${event.className}"
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> resumedAt[activity] = event.timeStamp
            UsageEvents.Event.ACTIVITY_PAUSED -> resumedAt.remove(activity)?.let { total += event.timeStamp - it }
        }
    }
    return total
}

/** Packages other than this launcher, most recently used first (last 3 days). Needs usage access. */
fun Context.recentPackages(): List<String> {
    val now = System.currentTimeMillis()
    return getSystemService(UsageStatsManager::class.java)
        .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 3 * 24 * 3600_000L, now)
        .filter { it.packageName != packageName && it.totalTimeInForeground > 0 }
        .groupBy { it.packageName } // daily buckets repeat a package
        .map { (pkg, stats) -> pkg to stats.maxOf { it.lastTimeUsed } }
        .sortedByDescending { it.second }
        .map { it.first }
}

/** "2h 14m", or "14m" under an hour. */
fun formatDuration(millis: Long): String {
    val minutes = millis / 60_000
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "${minutes}m"
}
