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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import id.ibnurizqia.launcher.LauncherSettings.DrawerBackground
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.exp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_FAVORITES = 6

// OLED palette: true black, three text emphasis levels (all pass WCAG AA on black)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFBDBDBD) // 11:1
private val TextMuted = Color(0xFF8C8C8C) // 6.2:1
private val SurfaceDim = Color(0xFF121212) // barely-lit fill for the search field
private val SurfaceRaised = Color(0xFF1F1F1F) // menu segments: lifted just enough off pure black

/** Long-press menu content for one app; call `close` to dismiss it. */
private typealias AppMenu = @Composable (app: App, close: () -> Unit) -> Unit

/** [detail] tells apart apps that share a name (e.g. Xiaomi's and Google's Calendar); null otherwise. */
class App(val key: String, val label: String, val info: LauncherActivityInfo, val detail: String? = null)

class CalendarEvent(val id: Long, val title: String, val begin: Long)

/** A "Show: ..." setup shortcut: tap to grant, long-press to stop offering it. */
class Hint(val label: String, val onTap: () -> Unit, val onDismiss: () -> Unit)

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
    private var showBattery by mutableStateOf(false)
    private var drawerOpen by mutableStateOf(false)
    private var settingsOpen by mutableStateOf(false)
    private val settings by lazy { LauncherSettings(prefs) }
    private var navGestures by mutableStateOf(false) // our accessibility service is on
    private var weather by mutableStateOf<Weather?>(null)
    private var weatherFetchedAt = 0L
    private var nextAlarm by mutableStateOf<Long?>(null)
    private var nextEvent by mutableStateOf<CalendarEvent?>(null)
    private var allDayToday by mutableStateOf(emptyList<CalendarEvent>()) // holidays, birthdays...
    private var screenTime by mutableStateOf<Long?>(null)
    private var recent by mutableStateOf(emptyList<String>()) // packages, most recently used first
    private val media by lazy { MediaWatcher(this) }
    private var hints by mutableStateOf(emptyList<Hint>())
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
                    val drawer by animateFloatAsState(if (drawerOpen) 1f else 0f, tween(if (drawerOpen) 250 else 180), label = "drawer")
                    // blur needs Android 12; below that "Home" would be unreadable text on text, so it's black
                    val behind = settings.drawerBackground.let {
                        if (it == DrawerBackground.Home && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) DrawerBackground.Black else it
                    }
                    // Home (wallpaper included) stays put under the drawer, sinking back as it opens: blurred and
                    // dimmed; or only its content fades, leaving the wallpaper crisp; or it all fades to black,
                    // which is also the battery-friendliest.
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val radius = drawer * (settings.blurLevel * 5).dp.toPx()
                                renderEffect = if (behind == DrawerBackground.Home && radius >= 1f) BlurEffect(radius, radius, TileMode.Decal) else null
                            }
                            .drawWithContent {
                                drawContent()
                                val dim = when (behind) { DrawerBackground.Home -> 0.6f; DrawerBackground.Wallpaper -> 0f; DrawerBackground.Black -> 1f }
                                drawRect(Color.Black, alpha = drawer * dim)
                            }
                    ) {
                        Crossfade(settings.wallpaper, animationSpec = tween(300), label = "wallpaper") { Box(Modifier.fillMaxSize().wallpaper(it)) }
                        Box(Modifier.graphicsLayer { alpha = if (behind == DrawerBackground.Wallpaper) 1f - drawer else 1f }) { HomeScreen(menu) }
                    }
                    AnimatedVisibility(
                        visible = drawerOpen,
                        // rises from below, where the swipe came from; leaves a bit faster than it came
                        enter = slideInVertically(tween(250, easing = EaseOutCubic)) { it / 8 } + fadeIn(tween(250)),
                        exit = slideOutVertically(tween(180, easing = EaseInCubic)) { it / 8 } + fadeOut(tween(180)),
                    ) {
                        // pointerInput: touches anywhere on the drawer, even its empty edges, stay off the home below
                        Box(Modifier.fillMaxSize().pointerInput(Unit) {}) {
                            AppList(
                                apps, hidden,
                                recent = if (settings.recent.on) recentApps(10) else emptyList(),
                                showIndex = settings.index.on,
                                autoKeyboard = settings.keyboard.on,
                                onOpen = ::open,
                                onClose = { drawerOpen = false },
                                onSettings = { drawerOpen = false; settingsOpen = true },
                                menu = menu,
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = settingsOpen,
                        // a page further in: slides in from the side, back slides it out the same way
                        enter = slideInHorizontally(tween(250, easing = EaseOutCubic)) { it / 10 } + fadeIn(tween(250)),
                        exit = slideOutHorizontally(tween(180, easing = EaseInCubic)) { it / 10 } + fadeOut(tween(180)),
                    ) {
                        SettingsPage(
                            settings,
                            navGestures = navGestures,
                            onGestures = { explainGestures = true },
                            onDefaultHome = { launch(Intent(Settings.ACTION_HOME_SETTINGS)) },
                            onPermissions = { launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) },
                            onSource = { launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ibnurizqiaramadan/polos-launcher-android"))) },
                            onBack = ::closeSettings,
                        )
                    }
                    renaming?.let { app ->
                        RenameDialog(app, onRename = { rename(app, it); renaming = null }, onDismiss = { renaming = null })
                    }
                    if (showBattery) {
                        BatteryDialog(
                            onUsage = {
                                showBattery = false
                                launch(Intent(Intent.ACTION_POWER_USAGE_SUMMARY))
                            },
                            onDismiss = { showBattery = false },
                        )
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
        navGestures = gesturesEnabled()
        nextAlarm = nextClockAlarm()
        if (granted(CALENDAR)) {
            lifecycleScope.launch {
                nextEvent = withContext(Dispatchers.IO) { queryNextEvent() }
                allDayToday = withContext(Dispatchers.IO) { queryAllDayToday() }
            }
        }
        if (hasUsageAccess()) {
            lifecycleScope.launch {
                screenTime = withContext(Dispatchers.Default) { screenTimeToday() }
                recent = withContext(Dispatchers.Default) { recentPackages() }
            }
        }
    }

    // Android exposes only the single soonest "alarm clock" from any app, and reminder/tracker apps use that
    // API for exact wake-ups (e.g. a midnight job), which isn't an alarm to the user. Only trust ones a clock set.
    // ponytail: a clock alarm queued behind such a job stays hidden until the job passes; there's no API to list more
    private fun nextClockAlarm(): Long? {
        val next = getSystemService(AlarmManager::class.java).nextAlarmClock ?: return null
        val creator = next.showIntent?.creatorPackage ?: return next.triggerTime // no creator to check: trust it
        val clocks = packageManager.queryIntentActivities(Intent(AlarmClock.ACTION_SHOW_ALARMS), 0).map { it.activityInfo.packageName }
        if (creator !in clocks) return null
        return formattedNextAlarm(next.triggerTime) ?: next.triggerTime
    }

    // HyperOS's Clock registers its "alarm arriving" stage (15 min early) as an alarm clock too, so nextAlarmClock
    // runs early there. The system's formatted next-alarm string ("Jum 08.00") still holds the real ring time on
    // those phones, and elsewhere the framework derives it from the same alarm clock, so it never disagrees.
    // It's written with the locale's "EHm"/"Ehma" pattern, so it's parsed back with exactly that.
    @Suppress("DEPRECATION")
    private fun formattedNextAlarm(fallback: Long): Long? {
        val text = Settings.System.getString(contentResolver, Settings.System.NEXT_ALARM_FORMATTED)?.takeIf { it.isNotBlank() } ?: return null
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(this)) "EHm" else "Ehma")
        val parsed = runCatching { SimpleDateFormat(pattern, locale).parse(text) }.getOrNull() ?: return null
        val wanted = Calendar.getInstance().apply { time = parsed }
        // the string has only a weekday and a time: the first such moment at or after the system's own trigger
        val at = Calendar.getInstance().apply {
            timeInMillis = fallback
            set(Calendar.HOUR_OF_DAY, wanted.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, wanted.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < fallback - 60_000) add(Calendar.DAY_OF_MONTH, 1)
            while (get(Calendar.DAY_OF_WEEK) != wanted.get(Calendar.DAY_OF_WEEK)) add(Calendar.DAY_OF_MONTH, 1)
        }
        // only trust it as a correction of the same alarm, not as a different one days later
        return at.timeInMillis.takeIf { it - fallback in 0..24 * 3600_000L }
    }

    // Once Android stops showing the dialog (denied twice, or "don't ask again"), the switch lives in App info.
    private fun askPermission(permission: String) {
        val blocked = prefs.getBoolean("asked:$permission", false) && !shouldShowRequestPermissionRationale(permission)
        prefs.edit { putBoolean("asked:$permission", true) }
        if (blocked) {
            launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } else {
            requestPermission.launch(permission)
        }
    }

    private fun granted(permission: String) = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    // a hint shows until tapped once; after that the user can still grant access from App info
    // A hint stays while its access is missing, so a denied permission can always be fixed from home;
    // long-pressing it hides it for good (the access can still be granted from App info).
    private fun refreshHints() {
        hints = buildList {
            fun hint(text: String, key: String, ready: Boolean, request: () -> Unit) {
                if (!ready && !prefs.getBoolean("dismissed:$key", false)) {
                    add(Hint(text, onTap = request, onDismiss = { prefs.edit { putBoolean("dismissed:$key", true) }; refreshHints() }))
                }
            }
            if (settings.weather.on) hint("weather", LOCATION, granted(LOCATION)) { askPermission(LOCATION) }
            if (settings.events.on) hint("events", CALENDAR, granted(CALENDAR)) { askPermission(CALENDAR) }
            if (settings.screenTime.on) hint("screen time", "usage", hasUsageAccess()) {
                // straight to this app's toggle where supported, else the full list
                runCatching { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", packageName, null))) }
                    .onFailure { launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            }
            // only offered on 3-button phones; with system gestures it would just double up
            if (usesButtonNavigation()) hint("gestures", "gestures", gesturesEnabled()) { explainGestures = true }
            if (settings.music.on) hint("music", "media", hasNotificationAccess()) {
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

    // Reads what Google Calendar (and any other synced calendar) keeps in the system provider;
    // only calendars shown in the calendar app and events not declined count.
    private val eventFilter = "${Instances.VISIBLE} = 1 AND " +
        "${Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"

    // next timed event within a week that hasn't ended
    private fun queryNextEvent(): CalendarEvent? {
        val now = System.currentTimeMillis()
        val range = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(range, now)
        ContentUris.appendId(range, now + 7 * 24 * 3600_000L)
        return contentResolver.query(
            range.build(),
            arrayOf(Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN),
            "${Instances.ALL_DAY} = 0 AND ${Instances.END} > ? AND $eventFilter",
            arrayOf("$now"),
            "${Instances.BEGIN} ASC",
        )?.use { c -> if (c.moveToFirst()) CalendarEvent(c.getLong(0), c.getString(1).orEmpty(), c.getLong(2)) else null }
    }

    // All-day events are stored at UTC midnight, so match them by local calendar day instead of by time
    private fun queryAllDayToday(): List<CalendarEvent> {
        val today = LocalDate.now().toEpochDay() + 2_440_588 // Julian day number, as the provider counts days
        val byDay = Instances.CONTENT_BY_DAY_URI.buildUpon().appendPath("$today").appendPath("$today").build()
        return contentResolver.query(
            byDay,
            arrayOf(Instances.EVENT_ID, Instances.TITLE, Instances.BEGIN),
            "${Instances.ALL_DAY} = 1 AND $eventFilter",
            null,
            "${Instances.TITLE} ASC",
        )?.use { c ->
            buildList { while (c.moveToNext()) add(CalendarEvent(c.getLong(0), c.getString(1).orEmpty(), c.getLong(2))) }
        }.orEmpty()
    }

    // ponytail: 30 min in-memory throttle; persist the last result if cold starts without network matter
    private fun refreshWeather() {
        if (!settings.weather.on || !granted(LOCATION) || System.currentTimeMillis() - weatherFetchedAt < 30 * 60_000) return
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
        closeSettings()
    }

    // hints and weather depend on what's switched on, so catch up with any change made in settings
    private fun closeSettings() {
        if (!settingsOpen) return
        settingsOpen = false
        refreshHints()
        refreshWeather()
    }

    @Composable
    private fun HomeScreen(menu: AppMenu) {
        val haptics = LocalHapticFeedback.current
        Column(
            Modifier
                .fillMaxSize()
                // on an empty spot: double-tap locks, long-press opens settings; taps on the clock, favorites etc.
                // are theirs, not this
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { if (settings.doubleTapLock.on && !GestureService.lockScreen()) explainGestures = true },
                        onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            settingsOpen = true
                        },
                    )
                }
                .pointerInput(Unit) {
                    val threshold = 80.dp.toPx()
                    var drag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = {
                            when {
                                drag < -threshold -> drawerOpen = true
                                drag > threshold && settings.swipeDown.on -> expandNotifications()
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
                    weather?.takeIf { settings.weather.on }?.let { w ->
                        // the place makes it clear which area this is for; tapping opens that area's forecast
                        InfoText(listOfNotNull(w.summary, w.place).joinToString(", ")) {
                            launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, listOfNotNull("weather", w.place).joinToString(" ")))
                        }
                        if (settings.battery.on) InfoText("·")
                    }
                    if (settings.battery.on) InfoText("${batteryLevel()}%") { showBattery = true }
                }
                Spacer(Modifier.height(12.dp))
                // up here, under the weather: the header's free space absorbs it, so favorites never move
                media.nowPlaying?.takeIf { settings.music.on }?.let { track ->
                    Spacer(Modifier.height(8.dp))
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
                }
                // all setup hints on one line instead of a stack of "Tap to show ..." rows
                if (settings.hints.on && hints.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.Center) {
                        SubText("Show:")
                        hints.forEach { SubText(it.label, color = TextSecondary, onLongClick = it.onDismiss, onClick = it.onTap) }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // left: things to read (today) in quiet grey; right: favorites to tap, within thumb reach
            // bottom-aligned: however tall the left column gets, favorites stay exactly where they are
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) { TodayColumn() }
                Favorites(favorites.mapNotNull { key -> apps.find { it.key == key } }, ::open, ::swapFavorites, menu)
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                if (settings.shortcuts.on) SubText("Phone", Modifier.align(Alignment.CenterStart)) { launch(Intent(Intent.ACTION_DIAL)) }
                screenTime?.takeIf { settings.screenTime.on }?.let { SubText("Screen time ${formatDuration(it)}", Modifier.align(Alignment.Center)) }
                if (settings.shortcuts.on) {
                    SubText("Camera", Modifier.align(Alignment.CenterEnd)) { launch(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }
                }
            }
        }
    }

    // SubText pads 12dp; the extra 12dp lines these up with the 24dp app rows
    @Composable
    private fun TodayColumn() {
        val w = weather.takeIf { settings.weather.on }
        val event = nextEvent.takeIf { settings.events.on }
        val allDay = if (settings.events.on) allDayToday else emptyList()
        val alarm = nextAlarm.takeIf { settings.alarm.on }
        val device = if (settings.device.on) rememberDeviceLines() else null // off: no polling at all
        val hasToday = w != null || event != null || alarm != null || allDay.isNotEmpty()
        if (!hasToday && device == null) return
        Column(Modifier.padding(start = 12.dp, bottom = 16.dp)) {
            if (hasToday) SubText("Today")
            w?.let {
                val forecast = { launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, listOfNotNull("weather", it.place).joinToString(" "))) }
                SubText("${it.high}° / ${it.low}°", color = TextSecondary, onClick = forecast)
                SubText("Rain ${it.rainChance}%", color = TextSecondary, onClick = forecast)
            }
            allDay.take(2).forEach { event ->
                SubText("All day  ${event.title}", color = TextSecondary) { openEvent(event) }
            }
            event?.let { event ->
                val time = if (event.begin <= System.currentTimeMillis()) "Now" else timeText(event.begin)
                SubText("$time  ${event.title}", color = TextSecondary) { openEvent(event) }
            }
            alarm?.let { SubText("Alarm ${timeText(it)}", color = TextSecondary) { launch(Intent(AlarmClock.ACTION_SHOW_ALARMS)) } }
            // grouped apart from the day's info; only the battery line has details behind it
            device?.let {
                if (hasToday) Spacer(Modifier.height(12.dp))
                SubText("Device")
                // on big font sizes these wrap rather than lose the temperature behind an ellipsis
                SubText(it.ram, color = TextSecondary, tabular = true, maxLines = 2)
                it.cpu?.let { cpu -> SubText(cpu, color = TextSecondary, tabular = true, maxLines = 2) }
                it.battery?.let { battery -> SubText(battery, color = TextSecondary, tabular = true, maxLines = 2) { showBattery = true } }
            }
        }
    }

    private data class DeviceLines(val ram: String, val cpu: String?, val battery: String?)

    // Refreshed every 5 s only while home is actually on screen: repeatOnLifecycle stops the loop when the
    // screen goes off or another app (or the drawer, which disposes this) takes over. The state holds the
    // shown text (a data class), so an unchanged reading causes no recomposition or redraw at all.
    @Composable
    private fun rememberDeviceLines(): DeviceLines? {
        var lines by remember { mutableStateOf<DeviceLines?>(null) }
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    lines = withContext(Dispatchers.IO) {
                        systemStats().let { DeviceLines(it.ramText(), it.cpuText(), batteryStats()?.lineText()) }
                    }
                    delay(5_000)
                }
            }
        }
        return lines
    }

    // last few apps used that aren't already a favorite (or hidden)
    private fun recentApps(count: Int) = recent.asSequence()
        .mapNotNull { pkg -> apps.firstOrNull { it.info.componentName.packageName == pkg } }
        .filter { it.key !in favorites && it.key !in hidden }
        .take(count)
        .toList()

    private fun openEvent(event: CalendarEvent) = launch(
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
    )

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
        val isHidden = app.key in hidden
        MenuHeader(app)
        MenuGroup(
            close,
            listOfNotNull(
                MenuAction(Icons.Filled.Home, if (favorite) "Remove from home" else "Add to home") { toggleFavorite(app) }
                    .takeIf { favorite || favorites.size < MAX_FAVORITES },
                MenuAction(Icons.Filled.Edit, "Rename") { renaming = app },
                MenuAction(if (isHidden) VisibilityIcon else VisibilityOffIcon, if (isHidden) "Unhide" else "Hide") { toggleHidden(app) },
            ),
        )
        MenuGroup(close, listOf(MenuAction(Icons.Filled.Info, "App info") { openInfo(app) }))
    }

    private fun toggleFavorite(app: App) {
        favorites = if (app.key in favorites) favorites - app.key else favorites + app.key
        prefs.edit { putString("favorites", favorites.joinToString("\n")) }
    }

    private fun swapFavorites(a: String, b: String) {
        favorites = favorites.map { when (it) { a -> b; b -> a; else -> it } }
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
    tabular: Boolean = false, // fixed-width digits for numbers that keep changing, so the line doesn't wobble
    maxLines: Int = 1,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) = Text(
    text = text,
    color = color,
    style = if (tabular) LocalTextStyle.current.copy(fontFeatureSettings = "tnum") else LocalTextStyle.current,
    fontSize = 14.sp,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
        .then(
            when {
                onClick != null && onLongClick != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                onClick != null -> Modifier.clickable(onClick = onClick)
                else -> Modifier
            }
        )
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
private fun AppList(
    apps: List<App>,
    hidden: Set<String>,
    recent: List<App>,
    showIndex: Boolean,
    autoKeyboard: Boolean,
    onOpen: (App) -> Unit,
    onClose: () -> Unit,
    onSettings: () -> Unit,
    menu: AppMenu,
) {
    var query by remember { mutableStateOf("") }
    val search = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoKeyboard) search.requestFocus() } // off by default: the keyboard waits for a tap
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
                // visible way in for those who don't know long-pressing home opens it
                if (!searching && !showHidden) item {
                    Text(
                        text = "Launcher settings",
                        color = TextMuted,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSettings)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    )
                }
            }
            // hidden while searching: results are short, and above the keyboard it'd be too cramped to hit
            if (showIndex && shown.isNotEmpty() && !searching && !WindowInsets.isImeVisible) {
                AlphabetScroller(
                    firstIndex,
                    shown.lastIndex,
                    currentLetter,
                    listState,
                    Modifier.align(Alignment.TopEnd).fillMaxHeight().padding(end = 16.dp), // clear of the edge gesture strip
                )
            }
        }
        // recent apps right above the search field, within thumb reach; swipe sideways for more, while
        // "Recent:" stays put. Out of the way while searching.
        if (!searching && recent.isNotEmpty()) {
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SubText("Recent:")
                val row = rememberLazyListState()
                LazyRow(state = row, modifier = Modifier.weight(1f).fadingEdges(row), contentPadding = PaddingValues(end = 12.dp)) {
                    items(recent, key = { it.key }) { app -> SubText(app.label, color = TextSecondary) { onOpen(app) } }
                }
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
                .focusRequester(search)
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
// Fades a scrolling row out at whichever end has more to scroll to, so it reads as swipeable rather than cut off.
// Offscreen so the fade masks only the row, letting whatever is behind it show through.
private fun Modifier.fadingEdges(state: LazyListState) = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val fade = 32.dp.toPx()
        if (state.canScrollBackward) {
            drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), endX = fade), blendMode = BlendMode.DstIn)
        }
        if (state.canScrollForward) {
            drawRect(Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade), blendMode = BlendMode.DstIn)
        }
    }

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

/**
 * Home favorites. Hold one and drag it up or down to reorder; the others slide out of its way. A hold
 * without moving still opens the app's menu, which closes once the drag starts.
 */
@Composable
private fun Favorites(apps: List<App>, onOpen: (App) -> Unit, onSwap: (String, String) -> Unit, menu: AppMenu) {
    val state = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var dragged by remember { mutableStateOf<String?>(null) }
    var startTop by remember { mutableIntStateOf(0) } // the dragged row's offset when the drag began
    var travel by remember { mutableFloatStateOf(0f) } // finger movement since then
    var settling by remember { mutableStateOf<Job?>(null) }
    var held by remember { mutableStateOf(false) } // finger still down on the dragged row
    val lift by animateFloatAsState(if (held) 1.05f else 1f, label = "lift") // settles back while it glides in
    // where the dragged row is drawn: under the finger, but kept inside the list, which clips anything outside it
    fun shownTop(size: Int) = (startTop + travel).coerceIn(0f, (state.layoutInfo.viewportSize.height - size).coerceAtLeast(0).toFloat())
    LazyColumn(
        state = state,
        userScrollEnabled = false,
        horizontalAlignment = Alignment.End,
        modifier = Modifier.pointerInput(Unit) {
            val dragSlop = viewConfiguration.touchSlop * 2 // a finger drifting while lifting off the menu isn't a drag
            awaitEachGesture {
                // Initial pass, nothing consumed until a drag starts: taps and holds stay the row's own
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val row = state.layoutInfo.visibleItemsInfo
                    .find { down.position.y.toInt() in it.offset until it.offset + it.size } ?: return@awaitEachGesture
                val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
                    }
                }
                if (released != null) return@awaitEachGesture // a tap or a scroll, not a hold
                var expected = -1 // row index the last swap should land on, so a stale layout can't swap it back
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val dy = change.position.y - down.position.y
                    if (dragged == null && abs(dy) > dragSlop) {
                        settling?.cancel()
                        dragged = row.key as String
                        startTop = row.offset
                        held = true
                    }
                    if (dragged == null) continue
                    change.consume() // keeps home's own swipe (drawer / notifications) out of it
                    travel = dy // unclamped here, so pulling past either end still reaches the first and last slot
                    val rows = state.layoutInfo.visibleItemsInfo
                    val current = rows.find { it.key == dragged } ?: continue
                    if (expected != -1 && current.index != expected) continue
                    // swap once the dragged row's middle passes a neighbour's middle
                    val middle = startTop + travel + current.size / 2
                    val next = rows.find { it.index == current.index + 1 }
                    val prev = rows.find { it.index == current.index - 1 }
                    val target = when {
                        next != null && middle > next.offset + next.size / 2 -> next
                        prev != null && middle < prev.offset + prev.size / 2 -> prev
                        else -> null
                    } ?: continue
                    expected = target.index
                    onSwap(dragged!!, target.key as String)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                val key = dragged ?: return@awaitEachGesture
                held = false
                // glide into the slot it was dropped on, then hand it back to the list
                val dropped = state.layoutInfo.visibleItemsInfo.find { it.key == key }
                val slot = dropped?.offset ?: startTop
                dropped?.let { travel = shownTop(it.size) - startTop } // start from where it's drawn, not the finger
                settling = scope.launch {
                    animate(travel, (slot - startTop).toFloat(), animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { v, _ -> travel = v }
                    dragged = null
                    travel = 0f
                }
            }
        },
    ) {
        items(apps, key = { it.key }) { app ->
            val isDragged = app.key == dragged
            AppItem(
                app, 28.sp, onOpen, menu,
                fontWeight = FontWeight.Light,
                align = Alignment.End,
                dragging = isDragged,
                modifier = if (isDragged) {
                    Modifier.zIndex(1f).graphicsLayer {
                        // follows the finger wherever the list has put its slot by now
                        val info = state.layoutInfo.visibleItemsInfo.find { it.key == app.key }
                        translationY = if (info != null) shownTop(info.size) - info.offset else 0f
                        scaleX = lift
                        scaleY = lift
                        transformOrigin = TransformOrigin(1f, 0.5f) // grows from the right edge it's aligned to
                    }
                } else {
                    Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
                },
            )
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
    dragging: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(dragging) { if (dragging) menuOpen = false } // the hold became a drag: the menu isn't wanted
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
            Text(text = app.label, fontSize = fontSize, fontWeight = fontWeight, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
 * Android 16 (Material 3 Expressive) style long-press menu: separate rounded segments on black instead of
 * one flat sheet, springing out from the finger. Opens up and to the left of the finger so the thumb never
 * covers it, moving right / below only when there's no room. Its parent must be a zero-size anchor at the touch point.
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
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
    Popup(popupPositionProvider = placement, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            Modifier
                .graphicsLayer {
                    transformOrigin = placement.origin // the finger, wherever the menu ended up
                    scaleX = 0.8f + 0.2f * appear.value // the spring overshoots a touch, then settles
                    scaleY = scaleX
                    alpha = appear.value.coerceIn(0f, 1f)
                }
                .width(IntrinsicSize.Max)
                .widthIn(min = 232.dp, max = 288.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) { content() }
    }
}

class MenuAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

// which app this is about: the name, and the package that tells twins apart
@Composable
private fun MenuHeader(app: App) = Column(
    Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(SegmentOuter))
        .background(SurfaceRaised)
        .padding(horizontal = 20.dp, vertical = 14.dp)
) {
    Text(app.label, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(app.info.componentName.packageName, color = TextMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

// connected segments: 2dp apart, big outer corners, small inner ones (the Android 16 grouped-list look)
@Composable
private fun MenuGroup(close: () -> Unit, actions: List<MenuAction>) = Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    actions.forEachIndexed { i, action ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(segmentShape(first = i == 0, last = i == actions.lastIndex))
                .background(SurfaceRaised)
                .clickable { close(); action.onClick() }
                .padding(horizontal = 20.dp, vertical = 14.dp), // ~50dp tall rows
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(action.icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(action.label, fontSize = 15.sp)
        }
    }
}

private val SegmentOuter = 20.dp
private val SegmentInner = 4.dp

private fun segmentShape(first: Boolean, last: Boolean) = RoundedCornerShape(
    topStart = if (first) SegmentOuter else SegmentInner,
    topEnd = if (first) SegmentOuter else SegmentInner,
    bottomStart = if (last) SegmentOuter else SegmentInner,
    bottomEnd = if (last) SegmentOuter else SegmentInner,
)

// Material "visibility" / "visibility_off" (filled), which material-icons-core doesn't ship
private val VisibilityIcon by lazy {
    svgIcon(
        "Filled.Visibility",
        "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5z" +
            "M12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z",
    )
}
private val VisibilityOffIcon by lazy {
    svgIcon(
        "Filled.VisibilityOff",
        "M12,7c2.76,0 5,2.24 5,5 0,0.65 -0.13,1.26 -0.36,1.83l2.92,2.92c1.51,-1.26 2.7,-2.89 3.43,-4.75 " +
            "-1.73,-4.39 -6,-7.5 -11,-7.5 -1.4,0 -2.74,0.25 -3.98,0.7l2.16,2.16C10.74,7.13 11.35,7 12,7z" +
            "M2,4.27l2.28,2.28 0.46,0.46C3.08,8.3 1.78,10.02 1,12c1.73,4.39 6,7.5 11,7.5 1.55,0 3.03,-0.3 4.38,-0.84" +
            "l0.42,0.42L19.73,22 21,20.73 3.27,3 2,4.27z" +
            "M7.53,9.8l1.55,1.55c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.66 1.34,3 3,3 0.22,0 0.44,-0.03 0.65,-0.08" +
            "l1.55,1.55c-0.67,0.33 -1.41,0.53 -2.2,0.53 -2.76,0 -5,-2.24 -5,-5 0,-0.79 0.2,-1.53 0.53,-2.2z" +
            "M11.84,9.02l3.15,3.15 0.02,-0.16c0,-1.66 -1.34,-3 -3,-3l-0.17,0.01z",
    )
}

private fun svgIcon(name: String, pathData: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
    .build()

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
    /** The finger relative to the placed menu, so it can grow out of that point. */
    var origin by mutableStateOf(TransformOrigin.Center)
        private set

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
        val placedX = x ?: left.coerceIn(margin, maxX)
        origin = TransformOrigin(
            ((fingerX - placedX).toFloat() / width).coerceIn(0f, 1f),
            ((fingerY - y).toFloat() / height).coerceIn(0f, 1f),
        )
        return IntOffset(placedX, y)
    }
}

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

/** Ampere-style battery details; the current keeps updating while the dialog is open. */
@Composable
private fun BatteryDialog(onUsage: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    LaunchedEffect(Unit) {
        while (true) {
            rows = withContext(Dispatchers.IO) { context.batteryStats()?.detailRows().orEmpty() }
            delay(2_000)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Battery") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(label, color = TextMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(value, fontSize = 14.sp, style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { TextButton(onClick = onUsage) { Text("Battery usage") } },
    )
}

/** Accessibility needs a clear disclosure before sending the user to turn it on. */
@Composable
private fun GesturesDialog(onContinue: () -> Unit, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Gestures") },
    text = {
        Text(
            "Swipe in from the left or right edge to go Back, swipe up from the bottom centre to go Home, " +
                "swipe up and hold for Recents. Double-tap an empty spot on the home screen to lock the phone, " +
                "like the power button.\n\n" +
                "This uses Android's Accessibility service only to trigger these actions. " +
                "It doesn't read or collect anything on your screen.\n\n" +
                "Next, turn on \"Polos gestures\" in Accessibility settings. If Android says it's a " +
                "restricted setting, first open App info > \u22ee > Allow restricted settings."
        )
    },
    confirmButton = { TextButton(onClick = onContinue) { Text("Open settings") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
)

/**
 * Everything adjustable, in Android 16's grouped rounded rows. The page sits on the chosen wallpaper, so it
 * previews it too. Changes apply at once; there's nothing to save.
 */
@Composable
private fun SettingsPage(
    settings: LauncherSettings,
    navGestures: Boolean,
    onGestures: () -> Unit,
    onDefaultHome: () -> Unit,
    onPermissions: () -> Unit,
    onSource: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() }
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .wallpaper(settings.wallpaper)
            .safeDrawingPadding() // outside the scroll, so rows stop at the status bar instead of sliding under it
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(4.dp))
            Text("Settings", fontSize = 22.sp)
        }
        SectionTitle("Wallpaper")
        Column(
            Modifier
                .clip(RoundedCornerShape(SegmentOuter))
                .background(SurfaceRaised)
                .padding(16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Drawn by the launcher and kept mostly black, so AMOLED pixels stay off.", fontSize = 14.sp, color = TextMuted)
            Wallpaper.entries.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { WallpaperTile(it, selected = it == settings.wallpaper, Modifier.weight(1f)) { settings.pick(it) } }
                }
            }
        }
        SettingGroup(
            "Home screen",
            listOf(
                settings.weather.row("Weather", "Conditions under the date, plus today's high, low and rain"),
                settings.battery.row("Battery percentage"),
                settings.music.row("Now playing", "Song and controls while music plays"),
                settings.events.row("Calendar events", "All-day events and the next one coming up"),
                settings.alarm.row("Next alarm"),
                settings.device.row("Device stats", "RAM, CPU speed and battery current"),
                settings.screenTime.row("Screen time"),
                settings.shortcuts.row("Phone and Camera", "Shortcuts in the bottom corners"),
                settings.hints.row("Setup hints", "The \"Show:\" line for features that still need access"),
            ),
        )
        SettingGroup(
            "App drawer",
            listOf(
                SettingRow(
                    "Background",
                    when (settings.drawerBackground) {
                        DrawerBackground.Home -> if (canBlur) "The home screen, blurred and dimmed" else "Blur needs Android 12; shows black instead"
                        DrawerBackground.Wallpaper -> "Only the wallpaper; the home screen's text hides"
                        DrawerBackground.Black -> "Plain black, the easiest on an AMOLED battery"
                    },
                ) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        DrawerBackground.entries.forEachIndexed { i, option ->
                            SegmentedButton(
                                selected = option == settings.drawerBackground,
                                onClick = { settings.pick(option) },
                                shape = SegmentedButtonDefaults.itemShape(i, DrawerBackground.entries.size),
                                colors = SegmentedColors,
                            ) { Text(option.name, fontSize = 14.sp) }
                        }
                    }
                },
                // only while the home screen is behind the drawer: a strength for anything else would just be noise
                if (canBlur && settings.drawerBackground == DrawerBackground.Home) {
                    SettingRow("Blur level", value = "${settings.blurLevel}") {
                        Slider(
                            value = settings.blurLevel.toFloat(),
                            onValueChange = { settings.adjustBlur(it.roundToInt()) },
                            valueRange = 1f..10f,
                            steps = 8, // the 8 stops between 1 and 10
                            colors = SliderColors,
                            modifier = Modifier.semantics { contentDescription = "Blur level" },
                        )
                    }
                } else {
                    null
                },
                settings.recent.row("Recent apps", "Up to 10, above the search field; swipe sideways for more"),
                settings.index.row("Alphabet index", "Letters along the right edge to jump through the list"),
                settings.keyboard.row("Open keyboard right away", "Otherwise it waits until you tap search"),
            ).filterNotNull(),
        )
        SettingGroup(
            "Gestures",
            listOf(
                SettingRow("Navigation gestures", "Swipe in from the edges for Back, Home and Recents", value = if (navGestures) "On" else "Off", onClick = onGestures),
                settings.doubleTapLock.row("Double-tap to lock", if (navGestures) "Double-tap an empty spot on home" else "Needs navigation gestures on"),
                settings.swipeDown.row("Swipe down for notifications"),
            ),
        )
        SettingGroup(
            "Launcher",
            listOf(
                SettingRow("Default home app", "Choose which launcher the Home button opens", onClick = onDefaultHome),
                SettingRow("Permissions", "Location, calendar and other access", onClick = onPermissions),
            ),
        )
        // tap opens the source: the launcher asks for Accessibility and notification access, so it should be checkable
        Text(
            "Polos ${version ?: ""}  ·  by xyrus10",
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSource)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * One settings row: a switch when [checked] is set, a link when there's an [onClick], optionally showing its
 * current [value], with [below] for a control under the title (e.g. a slider).
 */
private class SettingRow(
    val title: String,
    val summary: String? = null,
    val checked: Boolean? = null,
    val value: String? = null,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val below: (@Composable () -> Unit)? = null,
)

private fun LauncherSettings.Option.row(title: String, summary: String? = null, enabled: Boolean = true) =
    SettingRow(title, summary, checked = on, enabled = enabled, onClick = ::toggle)

@Composable
private fun SectionTitle(text: String) = Text(
    text,
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    color = TextSecondary,
    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
)

// monochrome like the rest of the launcher: white when on, outlined grey when off
private val SwitchColors
    @Composable get() = SwitchDefaults.colors(
        checkedThumbColor = Color.Black,
        checkedTrackColor = TextPrimary,
        checkedBorderColor = TextPrimary,
        uncheckedThumbColor = TextMuted,
        uncheckedTrackColor = Color.Black,
        uncheckedBorderColor = TextMuted,
    )

private val SegmentedColors
    @Composable get() = SegmentedButtonDefaults.colors(
        activeContainerColor = TextPrimary,
        activeContentColor = Color.Black,
        activeBorderColor = TextPrimary,
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = TextSecondary,
        inactiveBorderColor = TextMuted,
    )

private val SliderColors
    @Composable get() = SliderDefaults.colors(
        thumbColor = TextPrimary,
        activeTrackColor = TextPrimary,
        activeTickColor = Color.Black,
        inactiveTrackColor = Color.Black,
        inactiveTickColor = TextMuted,
    )

// the whole row is the touch target (64dp+); the switch only shows the state, so it takes no clicks itself
@Composable
private fun SettingGroup(title: String, rows: List<SettingRow>) {
    SectionTitle(title)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { i, row ->
            val click = row.onClick
            val action = when {
                click == null -> Modifier
                row.checked != null -> Modifier.toggleable(row.checked, enabled = row.enabled, role = Role.Switch) { click() }
                else -> Modifier.clickable(enabled = row.enabled, onClick = click)
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(segmentShape(first = i == 0, last = i == rows.lastIndex))
                    .background(SurfaceRaised)
                    .then(action)
                    .alpha(if (row.enabled) 1f else 0.38f)
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(row.title, fontSize = 16.sp)
                        row.summary?.let { Text(it, fontSize = 14.sp, color = TextMuted) }
                    }
                    row.checked?.let {
                        Spacer(Modifier.width(16.dp))
                        Switch(checked = it, onCheckedChange = null, enabled = row.enabled, colors = SwitchColors)
                    }
                    row.value?.let {
                        Spacer(Modifier.width(16.dp))
                        Text(it, fontSize = 14.sp, color = TextSecondary)
                    }
                }
                row.below?.invoke()
            }
        }
    }
}

// selection shows three ways: bright border, check badge and a bright label, not just a colour change
@Composable
private fun WallpaperTile(wallpaper: Wallpaper, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .clip(shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = wallpaper.name } // TalkBack: the name, not just "selected"
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3 / 4f)
                .clip(shape)
                .background(Color.Black)
                .wallpaper(wallpaper, scale = 0.5f)
                .then(if (selected) Modifier.border(2.dp, TextPrimary, shape) else Modifier)
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null, // the radio role already announces "selected"
                    tint = Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(20.dp)
                        .background(TextPrimary, CircleShape)
                        .padding(3.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // shrinks a little on big font sizes rather than cutting "Diamonds" to "Diamon"
        BasicText(
            wallpaper.name,
            style = LocalTextStyle.current.copy(
                color = if (selected) TextPrimary else TextMuted,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            ),
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp, stepSize = 1.sp),
            maxLines = 1,
        )
    }
}
