package id.ibnurizqia.launcher

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A small on-device log of what happened to the process and the gesture service, for the times HyperOS
 * shows the service as "not working": when it connected, when it was torn down, and why Android stopped
 * the app (from the system's own exit records). Nothing leaves the phone unless the user shares it.
 */
object ServiceLog {
    private const val KEEP = 300
    private val writer = Executors.newSingleThreadExecutor()
    private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun file(context: Context) = File(context.filesDir, "service.log")

    fun add(context: Context, event: String) {
        val app = context.applicationContext
        val line = "${stamp.format(Date())}  $event (pid ${Process.myPid()})\n"
        writer.execute {
            runCatching {
                val f = file(app)
                f.appendText(line)
                val lines = f.readLines()
                if (lines.size > KEEP + 100) f.writeText(lines.takeLast(KEEP).joinToString("\n", postfix = "\n"))
            }
        }
    }

    /** Newest first. */
    fun read(context: Context): List<String> = runCatching { file(context).readLines().asReversed() }.getOrDefault(emptyList())

    /** Android's own record of why this app's process ended, newest first (Android 11+). */
    fun exits(context: Context): List<String> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return listOf("Needs Android 11 or newer")
        val am = context.getSystemService(ActivityManager::class.java)
        return runCatching { am.getHistoricalProcessExitReasons(context.packageName, 0, 20) }.getOrDefault(emptyList()).map { info ->
            val reason = when (info.reason) {
                ApplicationExitInfo.REASON_EXIT_SELF -> "exited itself"
                ApplicationExitInfo.REASON_SIGNALED -> "killed by signal ${info.status}"
                ApplicationExitInfo.REASON_LOW_MEMORY -> "low memory"
                ApplicationExitInfo.REASON_CRASH -> "crash"
                ApplicationExitInfo.REASON_CRASH_NATIVE -> "native crash"
                ApplicationExitInfo.REASON_ANR -> "not responding"
                ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "failed to start"
                ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission changed"
                ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource use"
                ApplicationExitInfo.REASON_USER_REQUESTED -> "stopped by the user or the system (force stop)"
                ApplicationExitInfo.REASON_USER_STOPPED -> "user stopped"
                ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency died"
                ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "app updated"
                ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "package state changed"
                ApplicationExitInfo.REASON_OTHER -> "other"
                else -> "reason ${info.reason}"
            }
            val importance = when {
                info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "in the foreground"
                info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "visible"
                info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "running a service"
                else -> "in the background"
            }
            listOfNotNull(
                "${stamp.format(Date(info.timestamp))}  $reason, $importance",
                info.description?.takeIf { it.isNotBlank() }?.let { "    $it" },
            ).joinToString("\n")
        }
    }
}

/** Only here to note when the process comes up and when the system asks it to shed memory. */
class PolosApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLog.add(this, "process started")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) ServiceLog.add(this, "system asked for memory (level $level)")
    }
}

@Composable
fun LogPage(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var exits by remember { mutableStateOf(emptyList<String>()) }
    var events by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            exits = ServiceLog.exits(context)
            events = ServiceLog.read(context)
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(4.dp))
            Text("Service log", fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                val text = buildString {
                    appendLine("Polos service log, ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
                    appendLine(); appendLine("Why the app was stopped:"); exits.forEach { appendLine(it) }
                    appendLine(); appendLine("Gesture service:"); events.forEach { appendLine(it) }
                }
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                runCatching { context.startActivity(Intent.createChooser(send, "Share log")) }
            }) { Icon(Icons.Filled.Share, contentDescription = "Share log") }
        }
        Text(
            "Stays on this phone unless you share it. Useful when Accessibility shows the service as not working.",
            fontSize = 14.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LogSection("Why the app was stopped", exits, "No exits recorded yet")
        LogSection("Gesture service", events, "Nothing yet")
    }
}

@Composable
private fun LogSection(title: String, lines: List<String>, empty: String) {
    SectionTitle(title)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SegmentOuter))
            .background(SurfaceRaised)
            .padding(16.dp)
    ) {
        if (lines.isEmpty()) Text(empty, fontSize = 13.sp, color = TextMuted)
        lines.forEach { Text(it, fontSize = 12.sp, lineHeight = 18.sp, color = TextSecondary, fontFamily = FontFamily.Monospace) }
    }
}
