package com.minimalist.launcher

import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Color.TRANSPARENT
import android.net.Uri
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Box(
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
    )
}

@Composable
private fun AppList(apps: List<App>, onOpen: (App) -> Unit, onInfo: (App) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)), // keeps white labels readable on light wallpapers
        contentPadding = WindowInsets.systemBars.asPaddingValues(),
    ) {
        items(apps, key = { it.info.componentName.flattenToString() }) { app ->
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
