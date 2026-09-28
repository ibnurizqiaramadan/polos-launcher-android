package com.minimalist.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Color.TRANSPARENT
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

class App(val label: String, val info: LauncherActivityInfo)

class MainActivity : ComponentActivity() {
    private val launcherApps by lazy { getSystemService(LauncherApps::class.java) }
    private var apps by mutableStateOf(emptyList<App>())
    private var drawerOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), SystemBarStyle.dark(TRANSPARENT))
        setContent {
            BackHandler { drawerOpen = false } // home screen: back never leaves the launcher
            if (drawerOpen) {
                AppList(apps, onOpen = ::open, onInfo = ::openInfo)
            } else {
                HomeScreen(onSwipeUp = { drawerOpen = true }, onSwipeDown = ::expandNotifications)
            }
        }
    }

    // ponytail: reload on every resume catches installs/uninstalls; switch to LauncherApps.Callback if it gets slow
    override fun onResume() {
        super.onResume()
        apps = launcherApps.getActivityList(null, Process.myUserHandle())
            .map { App(it.label.toString(), it) }
            .sortedBy { it.label.lowercase() }
    }

    // Home button pressed while the launcher is already in front
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerOpen = false
    }

    private fun open(app: App) {
        launcherApps.startMainActivity(app.info.componentName, app.info.user, null, null)
        drawerOpen = false
    }

    private fun openInfo(app: App) = startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", app.info.componentName.packageName, null),
        )
    )

    // ponytail: hidden StatusBarManager API via reflection, works on stock Android 8-15;
    // AccessibilityService GLOBAL_ACTION_NOTIFICATIONS if some OEM blocks it
    private fun expandNotifications() {
        runCatching {
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(getSystemService("statusbar"))
        }
    }
}

@Composable
private fun HomeScreen(onSwipeUp: () -> Unit, onSwipeDown: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val threshold = 80.dp.toPx()
                var drag = 0f
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = {
                        when {
                            drag < -threshold -> onSwipeUp()
                            drag > threshold -> onSwipeDown()
                        }
                    },
                    onVerticalDrag = { _, dy -> drag += dy },
                )
            }
            .safeDrawingPadding()
            .padding(24.dp)
    ) {
        // Platform TextClock follows the 24h setting, time zone changes and wake-from-sleep on its own
        TextClock(formatPattern = null, sizeSp = 64f) // null = system h:mm / HH:mm
        TextClock(formatPattern = "EEEE, d MMMM", sizeSp = 18f)
        Text("${batteryLevel()}%", color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun TextClock(formatPattern: String?, sizeSp: Float) = AndroidView(
    factory = {
        android.widget.TextClock(it).apply {
            formatPattern?.let { f -> format12Hour = f; format24Hour = f }
            textSize = sizeSp
            setTextColor(android.graphics.Color.WHITE)
        }
    }
)

@Composable
private fun batteryLevel(): Int {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            }
        }
        // sticky broadcast: current level arrives right away
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return level
}

@Composable
private fun AppList(apps: List<App>, onOpen: (App) -> Unit, onInfo: (App) -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = apps.filter { it.label.contains(query, ignoreCase = true) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() } // pops the keyboard as soon as the drawer opens

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)) // keeps white labels readable on light wallpapers
            .safeDrawingPadding()
    ) {
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 22.sp),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (query.isNotBlank()) shown.firstOrNull()?.let(onOpen) }),
            decorationBox = { field ->
                if (query.isEmpty()) Text("Search", color = Color.Gray, fontSize = 22.sp)
                field()
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            items(shown, key = { it.info.componentName.flattenToString() }) { app ->
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onOpen(app) }, onLongClick = { onInfo(app) })
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
    }
}
