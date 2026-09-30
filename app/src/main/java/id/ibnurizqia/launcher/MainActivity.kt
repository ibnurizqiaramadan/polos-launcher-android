package id.ibnurizqia.launcher

import android.Manifest
import android.app.AlarmManager
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Color.TRANSPARENT
import android.graphics.Typeface
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_FAVORITES = 6

// OLED palette: true black, three text emphasis levels (all pass WCAG AA on black)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFBDBDBD) // 11:1
private val TextMuted = Color(0xFF8C8C8C) // 6.2:1
private val SurfaceDim = Color(0xFF121212) // barely-lit fill for the search field

/** Long-press menu content for one app; call `close` to dismiss it. */
private typealias AppMenu = @Composable (app: App, close: () -> Unit) -> Unit

/** [detail] tells apart apps that share a name (e.g. Xiaomi's and Google's Calendar); null otherwise. */
class App(val key: String, val label: String, val info: LauncherActivityInfo, val detail: String? = null)

class CalendarEvent(val id: Long, val title: String, val begin: Long)

private const val LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
private const val CALENDAR = Manifest.permission.READ_CALENDAR

class MainActivity : ComponentActivity() {
    private val launcherApps by lazy { getSystemService(LauncherApps::class.java) }
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }
    private var apps by mutableStateOf(emptyList<App>())
    private var favorites by mutableStateOf(emptyList<String>()) // app keys, in display order
    private var hidden by mutableStateOf(emptySet<String>())
    private var renaming by mutableStateOf<App?>(null)
    private var explainGestures by mutableStateOf(false)
    private var drawerOpen by mutableStateOf(false)
    private var weather by mutableStateOf<Weather?>(null)
    private var weatherFetchedAt = 0L
    private var nextAlarm by mutableStateOf<Long?>(null)
    private var nextEvent by mutableStateOf<CalendarEvent?>(null)
    private var screenTime by mutableStateOf<Long?>(null)
    private val media by lazy { MediaWatcher(this) }
    private var hints by mutableStateOf(emptyList<Pair<String, () -> Unit>>()) // setup hints: text to on-tap
    // the dialog pausing us means onResume re-reads every permission, so the result itself is unused
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(TRANSPARENT), SystemBarStyle.dark(TRANSPARENT))
        favorites = prefs.getString("favorites", "")!!.lines().filter { it.isNotEmpty() }
        hidden = prefs.getStringSet("hidden", emptySet())!!.toSet()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = Color.Black, contentColor = TextPrimary) {
                    val menu: AppMenu = { app, close -> Menu(app, close) }
                    BackHandler { drawerOpen = false } // home screen: back never leaves the launcher
                    AnimatedContent(
                        targetState = drawerOpen,
                        transitionSpec = {
                            if (targetState) {
                                // drawer rises from below (where the swipe came from) while home fades back
                                (slideInVertically(tween(250, easing = EaseOutCubic)) { it / 8 } + fadeIn(tween(250))) togetherWith
                                    fadeOut(tween(150))
                            } else {
                                // leaves a bit faster than it came, staying on top while it slides away
                                (fadeIn(tween(200)) togetherWith
                                    (slideOutVertically(tween(180, easing = EaseInCubic)) { it / 8 } + fadeOut(tween(180))))
                                    .apply { targetContentZIndex = -1f }
                            }
                        },
                        label = "drawer",
                    ) { open ->
                        if (open) AppList(apps, hidden, onOpen = ::open, onClose = { drawerOpen = false }, menu = menu) else HomeScreen(menu)
                    }
                    renaming?.let { app ->
                        RenameDialog(app, onRename = { rename(app, it); renaming = null }, onDismiss = { renaming = null })
                    }
                    if (explainGestures) {
                        GesturesDialog(
                            onContinue = {
                                explainGestures = false
                                launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onDismiss = { explainGestures = false },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasNotificationAccess()) media.start()
    }

    override fun onStop() {
        super.onStop()
        media.stop()
    }

    // ponytail: reload on every resume catches installs/uninstalls; switch to LauncherApps.Callback if it gets slow
    override fun onResume() {
        super.onResume()
        loadApps()
        refreshHints()
        refreshWeather()
        nextAlarm = nextClockAlarm()
        if (granted(CALENDAR)) lifecycleScope.launch { nextEvent = withContext(Dispatchers.IO) { queryNextEvent() } }
        if (hasUsageAccess()) lifecycleScope.launch { screenTime = withContext(Dispatchers.Default) { screenTimeToday() } }
    }

    // Android exposes only the single soonest "alarm clock" from any app, and reminder/tracker apps use that
    // API for exact wake-ups (e.g. a midnight job), which isn't an alarm to the user. Only trust ones a clock set.
    // ponytail: a clock alarm queued behind such a job stays hidden until the job passes; there's no API to list more
    private fun nextClockAlarm(): Long? {
        val next = getSystemService(AlarmManager::class.java).nextAlarmClock ?: return null
        val creator = next.showIntent?.creatorPackage ?: return next.triggerTime // no creator to check: trust it
        val clocks = packageManager.queryIntentActivities(Intent(AlarmClock.ACTION_SHOW_ALARMS), 0).map { it.activityInfo.packageName }
        return next.triggerTime.takeIf { creator in clocks }
    }

    private fun granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    // a hint shows until tapped once; after that the user can still grant access from App info
    private fun refreshHints() {
        hints = buildList {
            fun hint(text: String, key: String, ready: Boolean, request: () -> Unit) {
                if (!ready && !prefs.getBoolean("asked:$key", false)) {
                    add(text to { prefs.edit { putBoolean("asked:$key", true) }; request() })
                }
            }
            hint("weather", LOCATION, granted(LOCATION)) { requestPermission.launch(LOCATION) }
            hint("events", CALENDAR, granted(CALENDAR)) { requestPermission.launch(CALENDAR) }
            hint("screen time", "usage", hasUsageAccess()) {
                // straight to this app's toggle where supported, else the full list
                runCatching { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", packageName, null))) }
                    .onFailure { launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            }
            // only offered on 3-button phones; with system gestures it would just double up
            if (usesButtonNavigation()) hint("gestures", "gestures", gesturesEnabled()) { explainGestures = true }
            hint("music", "media", hasNotificationAccess()) {
                launch(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            ComponentName(this@MainActivity, MediaListener::class.java).flattenToString(),
                        )
                    } else {
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    }
                )
            }
        }
    }

    // next timed event within 24h that hasn't ended and wasn't declined
    private fun queryNextEvent(): CalendarEvent? {
        val now = System.currentTimeMillis()
        val range = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(range, now)
        ContentUris.appendId(range, now + 24 * 3600_000)
        return contentResolver.query(
            range.build(),
            arrayOf(Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN),
            "${Instances.ALL_DAY} = 0 AND ${Instances.END} > ? AND ${Instances.VISIBLE} = 1 AND " +
                "${Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}",
            arrayOf("$now"),
            "${Instances.BEGIN} ASC",
        )?.use { c -> if (c.moveToFirst()) CalendarEvent(c.getLong(0), c.getString(1).orEmpty(), c.getLong(2)) else null }
    }

    // ponytail: 30 min in-memory throttle; persist the last result if cold starts without network matter
    private fun refreshWeather() {
        if (!granted(LOCATION) || System.currentTimeMillis() - weatherFetchedAt < 30 * 60_000) return
        weatherFetchedAt = System.currentTimeMillis()
        lifecycleScope.launch {
            val result = runCatching { currentWeather() }.getOrNull()
            if (result != null) weather = result else weatherFetchedAt = 0 // failed: retry on next resume
        }
    }

    private fun loadApps() {
        val infos = launcherApps.getActivityList(null, Process.myUserHandle())
        val labels = infos.map { prefs.getString("label:${it.componentName.flattenToString()}", null) ?: it.label.toString() }
        val sameName = labels.groupingBy { it.lowercase() }.eachCount()
        val sameNameAndPackage = infos.indices.groupingBy { labels[it].lowercase() to infos[it].componentName.packageName }.eachCount()
        apps = infos.mapIndexed { i, info ->
            val component = info.componentName
            // twins get a subtitle: the package, or the activity too when one app has two entries with that name
            val detail = when {
                sameName.getValue(labels[i].lowercase()) < 2 -> null
                sameNameAndPackage.getValue(labels[i].lowercase() to component.packageName) > 1 -> component.flattenToShortString()
                else -> component.packageName
            }
            App(component.flattenToString(), labels[i], info, detail)
        }.sortedWith(compareBy({ it.label.lowercase() }, { it.detail }))
    }

    // Re-applied on every focus gain: dialogs, menus and other apps bring the bar back.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        WindowCompat.getInsetsController(window, window.decorView).run {
            // 3-button phones: no button bar on the launcher itself; a swipe from the bottom shows it briefly.
            // Gesture-nav phones keep theirs, since hidden bars make edge gestures take two swipes.
            if (usesButtonNavigation()) {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.navigationBars())
            } else {
                show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    // Home button pressed while the launcher is already in front
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerOpen = false
    }

    @Composable
    private fun HomeScreen(menu: AppMenu) {
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
                                drag < -threshold -> drawerOpen = true
                                drag > threshold -> expandNotifications()
                            }
                        },
                        onVerticalDrag = { _, dy -> drag += dy },
                    )
                }
                .safeDrawingPadding()
                .padding(top = 40.dp, bottom = 16.dp)
        ) {
            // header steps down: time > date > conditions > agenda, each smaller and dimmer
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Platform TextClock handles minute ticks, time zone changes and wake-from-sleep on its own
                TextClock("HH:mm", 72f, light = true, modifier = Modifier.clickable { launch(Intent(AlarmClock.ACTION_SHOW_ALARMS)) })
                TextClock("EEEE, d MMMM", 18f, modifier = Modifier.clickable { launch(calendarAt(System.currentTimeMillis())) })
                Spacer(Modifier.height(4.dp))
                Row {
                    weather?.let { w ->
                        // the place makes it clear which area this is for; tapping opens that area's forecast
                        InfoText(listOfNotNull(w.summary, w.place).joinToString(", ")) {
                            launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, listOfNotNull("weather", w.place).joinToString(" ")))
                        }
                        InfoText("·")
                    }
                    InfoText("${batteryLevel()}%") { launch(Intent(Intent.ACTION_POWER_USAGE_SUMMARY)) }
                }
                Spacer(Modifier.height(12.dp))
                nextEvent?.let { event ->
                    val time = if (event.begin <= System.currentTimeMillis()) "Now" else timeText(event.begin)
                    SubText("$time  ${event.title}") {
                        launch(
                            Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
                                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
                        )
                    }
                }
                nextAlarm?.let { SubText("Alarm ${timeText(it)}") { launch(Intent(AlarmClock.ACTION_SHOW_ALARMS)) } }
                // all setup hints on one line instead of a stack of "Tap to show ..." rows
                if (hints.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.Center) {
                        SubText("Show:")
                        hints.forEach { (label, onTap) -> SubText(label, color = TextSecondary, onClick = onTap) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // favorites sit low on the right: within thumb reach
            favorites.mapNotNull { key -> apps.find { it.key == key } }
                .forEach { AppItem(it, 28.sp, ::open, menu, Modifier.align(Alignment.End), FontWeight.Light, Alignment.End) }
            Spacer(Modifier.height(24.dp))
            media.nowPlaying?.let { track ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = listOf(track.title, track.artist).filter { it.isNotEmpty() }.joinToString("  ·  "),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable { media.controller?.packageName?.let(packageManager::getLaunchIntentForPackage)?.let(::launch) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    // play/pause is the main action: larger and brighter; IconButton gives 48dp targets + ripple
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        MediaButton(SkipPreviousIcon, "Previous", 28.dp, TextSecondary) { media.controller?.transportControls?.skipToPrevious() }
                        MediaButton(if (track.playing) PauseIcon else Icons.Filled.PlayArrow, if (track.playing) "Pause" else "Play", 36.dp, TextPrimary) {
                            media.controller?.transportControls?.run { if (track.playing) pause() else play() }
                        }
                        MediaButton(SkipNextIcon, "Next", 28.dp, TextSecondary) { media.controller?.transportControls?.skipToNext() }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                SubText("Phone", Modifier.align(Alignment.CenterStart)) { launch(Intent(Intent.ACTION_DIAL)) }
                screenTime?.let { SubText("Screen time ${formatDuration(it)}", Modifier.align(Alignment.Center)) }
                SubText("Camera", Modifier.align(Alignment.CenterEnd)) {
                    launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                }
            }
        }
    }

    private fun calendarAt(millis: Long) =
        Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").appendPath("$millis").build())

    private fun launch(intent: Intent) = safely { startActivity(intent) }

    // A home screen must never crash because some app refuses to open: missing handler, or one guarded by a
    // permission (Xiaomi's clock wants SET_ALARM for SHOW_ALARMS), or an app disabled since the list loaded.
    private fun safely(start: () -> Unit) {
        try {
            start()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No app can open this", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "That app didn't allow opening it from here", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    private fun Menu(app: App, close: () -> Unit) {
        val favorite = app.key in favorites
        if (favorite || favorites.size < MAX_FAVORITES) {
            MenuItem(if (favorite) "Remove from home" else "Add to home", close) { toggleFavorite(app) }
        }
        MenuItem("Rename", close) { renaming = app }
        MenuItem(if (app.key in hidden) "Unhide" else "Hide", close) { toggleHidden(app) }
        MenuItem("App info", close) { openInfo(app) }
    }

    private fun toggleFavorite(app: App) {
        favorites = if (app.key in favorites) favorites - app.key else favorites + app.key
        prefs.edit { putString("favorites", favorites.joinToString("\n")) }
    }

    private fun toggleHidden(app: App) {
        hidden = if (app.key in hidden) hidden - app.key else hidden + app.key
        prefs.edit { putStringSet("hidden", hidden) }
    }

    // blank name restores the app's own label
    private fun rename(app: App, name: String) {
        prefs.edit { if (name.isBlank()) remove("label:${app.key}") else putString("label:${app.key}", name.trim()) }
        loadApps()
    }

    private fun open(app: App) {
        safely { launcherApps.startMainActivity(app.info.componentName, app.info.user, null, null) }
        drawerOpen = false
    }

    private fun openInfo(app: App) = launch(
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
private fun TextClock(formatPattern: String, sizeSp: Float, light: Boolean = false, modifier: Modifier = Modifier) = AndroidView(
    modifier = modifier,
    factory = {
        android.widget.TextClock(it).apply {
            format12Hour = formatPattern
            format24Hour = formatPattern
            textSize = sizeSp
            setTextColor(android.graphics.Color.WHITE)
            typeface = Typeface.create(if (light) "sans-serif-light" else "sans-serif", Typeface.NORMAL)
            fontFeatureSettings = "tnum" // fixed-width digits: the clock doesn't wobble every minute
            includeFontPadding = false
        }
    }
)

@Composable
private fun InfoText(text: String, onClick: (() -> Unit)? = null) = Text(
    text = text,
    color = TextSecondary,
    fontSize = 16.sp,
    modifier = (if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 6.dp, vertical = 4.dp),
)

// "14:00" today, "Tue 09:00" otherwise
private fun timeText(millis: Long) = DateFormat.format(if (DateUtils.isToday(millis)) "HH:mm" else "EEE HH:mm", millis)

@Composable
private fun MediaButton(icon: ImageVector, label: String, size: Dp, tint: Color, onClick: () -> Unit) =
    IconButton(onClick = onClick) { Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size)) }

@Composable
private fun SubText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextMuted,
    onClick: (() -> Unit)? = null,
) = Text(
    text = text,
    color = color,
    fontSize = 14.sp,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 12.dp, vertical = 8.dp), // tall enough to hit; Compose extends it to 48dp
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppList(apps: List<App>, hidden: Set<String>, onOpen: (App) -> Unit, onClose: () -> Unit, menu: AppMenu) {
    var query by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    BackHandler(showHidden) { showHidden = false }
    val shown = apps.filter { (it.key in hidden) == showHidden && it.label.contains(query, ignoreCase = true) }
        .sortedBy { matchRank(it.label, query) } // stable: alphabetical within each rank
    val hiddenCount = apps.count { it.key in hidden }
    val listState = rememberLazyListState()
    // first list index of each letter present, e.g. {'C'=0, 'D'=5, ...}
    val firstIndex = HashMap<Char, Int>().apply { shown.forEachIndexed { i, app -> putIfAbsent(section(app.label), i) } }
    val pullToClose = rememberPullToClose(onClose)
    // search results stack up from the search field, best match right above it, within thumb reach
    val searching = query.isNotEmpty()
    LaunchedEffect(query) { listState.scrollToItem(0) }
    // letter of the topmost visible app; only changes when scrolling crosses into another section
    val currentLetter by remember(shown) {
        derivedStateOf { shown.getOrNull(listState.firstVisibleItemIndex)?.let { IndexLetters.indexOf(section(it.label)) } ?: -1 }
    }

    Column(
        Modifier
            .fillMaxSize()
            .nestedScroll(pullToClose)
            // the drawer follows the finger down and fades a little while being pulled
            .graphicsLayer {
                translationY = pullToClose.pull
                alpha = 1f - (pullToClose.pull / size.height).coerceIn(0f, 0.5f)
            }
            .safeDrawingPadding()
    ) {
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                reverseLayout = searching,
                modifier = Modifier.fillMaxSize().padding(end = 48.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(shown, key = { it.key }) { AppItem(it, 22.sp, onOpen, menu, Modifier.fillMaxWidth()) }
                if (showHidden || hiddenCount > 0) item {
                    Text(
                        text = if (showHidden) "Back to apps" else "Hidden apps ($hiddenCount)",
                        color = TextMuted,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHidden = !showHidden }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            }
            // hidden while searching: results are short, and above the keyboard it'd be too cramped to hit
            if (shown.isNotEmpty() && !searching && !WindowInsets.isImeVisible) {
                AlphabetScroller(
                    firstIndex,
                    shown.lastIndex,
                    currentLetter,
                    listState,
                    Modifier.align(Alignment.TopEnd).fillMaxHeight().padding(end = 16.dp), // clear of the edge gesture strip
                )
            }
        }
        // bottom, right above the keyboard: thumb reach
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 18.sp),
            cursorBrush = SolidColor(TextPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (query.isNotBlank()) shown.firstOrNull()?.let(onOpen) }),
            decorationBox = { field ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Box {
                        if (query.isEmpty()) Text("Search apps", color = TextMuted, fontSize = 18.sp)
                        field()
                    }
                }
            },
            // pill so it reads as an input, not a label
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(SurfaceDim, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

/**
 * Pull-to-close for the drawer: once the list is at its top, a further downward drag pulls the whole drawer.
 * Released past 96dp (or flung down) it closes; otherwise it springs back.
 */
private class PullToClose(
    private val closeDistance: Float,
    private val closeVelocity: Float,
    private val onClose: () -> Unit,
) : NestedScrollConnection {
    var pull by mutableFloatStateOf(0f)
        private set

    // while pulled, an upward drag first takes the drawer back up before the list scrolls
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (pull <= 0f || available.y >= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        val used = maxOf(available.y, -pull)
        pull += used
        return Offset(0f, used)
    }

    // the list couldn't scroll further up, so the rest of a downward drag pulls the drawer
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (available.y <= 0f || source != NestedScrollSource.UserInput) return Offset.Zero
        pull += available.y
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pull <= 0f) return Velocity.Zero
        if (pull > closeDistance || available.y > closeVelocity) onClose()
        else animate(pull, 0f) { value, _ -> pull = value }
        return available // the fling belongs to the pull, not the list
    }
}

@Composable
private fun rememberPullToClose(onClose: () -> Unit): PullToClose {
    val density = LocalDensity.current
    val latestOnClose by rememberUpdatedState(onClose)
    return remember(density) {
        with(density) { PullToClose(96.dp.toPx(), 1000.dp.toPx(), onClose = { latestOnClose() }) }
    }
}

/** Always the full index so every letter keeps its spot: muscle memory beats a compact list. */
private val IndexLetters = listOf('#') + ('A'..'Z')

// best search hits first: name starts with the query, then a word in it does, then it's anywhere inside
private fun matchRank(label: String, query: String) = when {
    query.isEmpty() || label.startsWith(query, ignoreCase = true) -> 0
    label.split(' ').any { it.startsWith(query, ignoreCase = true) } -> 1
    else -> 2
}

// digits, symbols and non-Latin scripts all land in '#'
private fun section(label: String) = label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'

private val WaveShift = 40.dp

/**
 * Letter index spread over the full list height: drag to jump.
 * Letters near the finger bulge left, grow and brighten on a smooth falloff; letters with no apps
 * are dimmed but still land on the next letter that has some.
 */
@Composable
private fun AlphabetScroller(
    firstIndex: Map<Char, Int>,
    lastIndex: Int,
    current: Int, // index into IndexLetters of the section at the top of the list, -1 if none
    listState: LazyListState,
    modifier: Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val waveShiftPx = with(LocalDensity.current) { WaveShift.toPx() }
    var letterPx by remember { mutableFloatStateOf(1f) } // height / letters, from the laid-out column
    var touching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) } // finger y inside the index; kept after lift so the wave fades out in place
    // letters follow the finger with no lag; only the effect's strength eases in and out
    val strength by animateFloatAsState(
        targetValue = if (touching) 1f else 0f,
        animationSpec = tween(if (touching) 150 else 220, easing = FastOutSlowInEasing),
        label = "wave",
    )
    // changes only when the finger crosses into another letter
    val active by remember {
        derivedStateOf { if (touching) (touchY / letterPx).toInt().coerceIn(IndexLetters.indices) else null }
    }
    var bubbleLetter by remember { mutableIntStateOf(0) } // stays put while the bubble fades out
    // the "you are here" mark glides between letters as the list scrolls instead of jumping
    val currentPos by animateFloatAsState(
        targetValue = current.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "current letter",
    )

    LaunchedEffect(active) {
        val index = active ?: return@LaunchedEffect
        bubbleLetter = index
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val target = IndexLetters.drop(index).firstNotNullOfOrNull { firstIndex[it] } ?: lastIndex
        listState.scrollToItem(target)
    }

    Box(modifier.width(48.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
                .onSizeChanged { letterPx = it.height / IndexLetters.size.toFloat() }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        touchY = down.position.y
                        touching = true
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { it.consume() }
                            touchY = event.changes.first().position.y
                        } while (event.changes.any { it.pressed })
                        touching = false
                    }
                }
        ) {
            IndexLetters.forEachIndexed { i, letter ->
                val restAlpha = if (letter in firstIndex) 0.55f else 0.22f // dim = no apps under this letter
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        // all read in the draw phase: dragging never recomposes the letters
                        .graphicsLayer {
                            val distance = i + 0.5f - touchY / letterPx // in letters
                            val wide = strength * exp(-distance * distance / 18f) // the wave
                            val near = strength * exp(-distance * distance / 3f) // the few letters under the finger
                            // where the scrolled list is; steps aside while the finger drives the index
                            val here = if (current < 0) 0f else (1f - strength) * exp(-(i - currentPos).let { it * it } / 0.6f)
                            translationX = -waveShiftPx * wide
                            scaleX = 1f + 0.5f * near + 0.2f * here
                            scaleY = scaleX
                            alpha = restAlpha + (1f - restAlpha) * maxOf(near, here)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter.toString(), color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
        // always composed; strength fades and scales it in and out
        Box(
            Modifier
                .offset { IntOffset((-96).dp.roundToPx(), (8.dp.toPx() + touchY - 28.dp.toPx()).roundToInt()) }
                .graphicsLayer {
                    alpha = strength
                    scaleX = 0.6f + 0.4f * strength
                    scaleY = 0.6f + 0.4f * strength
                }
                .size(56.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(IndexLetters[bubbleLetter].toString(), color = Color.Black, fontSize = 28.sp)
        }
    }
}

@Composable
private fun AppItem(
    app: App,
    fontSize: TextUnit,
    onOpen: (App) -> Unit,
    menu: AppMenu,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    align: Alignment.Horizontal = Alignment.Start,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var press by remember { mutableStateOf(Offset.Zero) } // where the finger went down, within the item
    // propagateMinConstraints: a full-width row makes the whole row tappable
    Box(modifier, propagateMinConstraints = true) {
        Column(
            Modifier
                // watch-only (Initial pass, nothing consumed): tap, long-press and ripple behave as before
                .pointerInput(Unit) {
                    awaitEachGesture { press = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial).position }
                }
                .combinedClickable(onClick = { onOpen(app) }, onLongClick = { menuOpen = true })
                // a little less air when a subtitle adds a line, so twins don't stand out as oversized rows
                .padding(horizontal = 24.dp, vertical = if (app.detail != null) 8.dp else 12.dp),
            horizontalAlignment = align,
        ) {
            Text(text = app.label, fontSize = fontSize, fontWeight = fontWeight)
            app.detail?.let {
                Text(text = it, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (menuOpen) { // composed only while open: keeps every list item light while scrolling
            // zero-size anchor at the finger; FingerMenu places itself around that point
            Box(
                Modifier
                    .matchParentSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset { IntOffset(press.x.roundToInt(), press.y.roundToInt()) }
            ) {
                FingerMenu(onDismiss = { menuOpen = false }) { menu(app) { menuOpen = false } }
            }
        }
    }
}

/**
 * A Material-looking menu that opens up and to the left of the finger so the thumb never covers it,
 * moving right / below only when there's no room. Its parent must be a zero-size anchor at the touch point.
 */
@Composable
private fun FingerMenu(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    // keep clear of the status and navigation bars ourselves; otherwise the system nudges the popup and it can land on the finger
    val bars = WindowInsets.safeDrawing
    val top = bars.getTop(density)
    val bottom = bars.getBottom(density)
    val placement = remember(density, top, bottom) {
        with(density) { AwayFromFinger(gap = 16.dp.roundToPx(), margin = 8.dp.roundToPx(), top = top, bottom = bottom) }
    }
    val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
    Popup(popupPositionProvider = placement, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        AnimatedVisibility(shown, enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.9f)) {
            Surface(
                shape = MenuDefaults.shape,
                color = MenuDefaults.containerColor,
                tonalElevation = MenuDefaults.TonalElevation,
                shadowElevation = MenuDefaults.ShadowElevation,
            ) {
                Column(Modifier.padding(vertical = 8.dp).width(IntrinsicSize.Max)) { content() }
            }
        }
    }
}

/**
 * Beside the finger, preferring its left (a right thumb comes in from the bottom right), then its right;
 * once beside it the menu can't be covered, so it just sits as high as fits, ideally above the finger.
 * Only if neither side fits does it go above, else below, the finger.
 */
private class AwayFromFinger(
    private val gap: Int,
    private val margin: Int,
    private val top: Int, // status bar
    private val bottom: Int, // navigation bar
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val (fingerX, fingerY) = anchorBounds.left to anchorBounds.top
        val (width, height) = popupContentSize.width to popupContentSize.height
        val minY = top + margin
        val maxY = maxOf(minY, windowSize.height - bottom - margin - height)
        val maxX = maxOf(margin, windowSize.width - margin - width)
        val left = fingerX - gap - width
        val right = fingerX + gap
        val x = when {
            left >= margin -> left
            right <= maxX -> right
            else -> null // too wide for either side
        }
        val above = fingerY - gap - height
        val y = when {
            x != null -> above.coerceIn(minY, maxY)
            above >= minY -> above
            else -> (fingerY + gap).coerceIn(minY, maxY)
        }
        return IntOffset(x ?: left.coerceIn(margin, maxX), y)
    }
}

@Composable
private fun MenuItem(text: String, close: () -> Unit, action: () -> Unit) =
    DropdownMenuItem(text = { Text(text) }, onClick = { close(); action() })

@Composable
private fun RenameDialog(app: App, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    // whole label pre-selected so typing replaces it
    var name by remember { mutableStateOf(TextFieldValue(app.label, TextRange(0, app.label.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(app.info.label.toString()) },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onRename(name.text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Accessibility needs a clear disclosure before sending the user to turn it on. */
@Composable
private fun GesturesDialog(onContinue: () -> Unit, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Navigation gestures") },
    text = {
        Text(
            "Swipe in from the left or right edge to go Back, swipe up from the bottom centre to go Home, " +
                "swipe up and hold for Recents.\n\n" +
                "This uses Android's Accessibility service only to trigger those three actions. " +
                "It doesn't read or collect anything on your screen.\n\n" +
                "Next, turn on \"Minimalist Launcher gestures\" in Accessibility settings. If Android says it's a " +
                "restricted setting, first open App info > \u22ee > Allow restricted settings."
        )
    },
    confirmButton = { TextButton(onClick = onContinue) { Text("Open settings") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
)
