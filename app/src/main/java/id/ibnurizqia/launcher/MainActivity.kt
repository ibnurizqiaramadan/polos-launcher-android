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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
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

class App(val key: String, val label: String, val info: LauncherActivityInfo)

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
    private var weather by mutableStateOf<String?>(null)
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
                    if (drawerOpen) {
                        AppList(apps, hidden, onOpen = ::open, menu = menu)
                    } else {
                        HomeScreen(menu)
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
        nextAlarm = getSystemService(AlarmManager::class.java).nextAlarmClock?.triggerTime
        if (granted(CALENDAR)) lifecycleScope.launch { nextEvent = withContext(Dispatchers.IO) { queryNextEvent() } }
        if (hasUsageAccess()) lifecycleScope.launch { screenTime = withContext(Dispatchers.Default) { screenTimeToday() } }
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
        apps = launcherApps.getActivityList(null, Process.myUserHandle())
            .map {
                val key = it.componentName.flattenToString()
                App(key, prefs.getString("label:$key", null) ?: it.label.toString(), it)
            }
            .sortedBy { it.label.lowercase() }
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
                    weather?.let {
                        InfoText(it) { launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, "weather")) }
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
                .forEach { AppItem(it, 28.sp, ::open, menu, Modifier.align(Alignment.End), FontWeight.Light) }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubText("Prev") { media.controller?.transportControls?.skipToPrevious() }
                        SubText(if (track.playing) "Pause" else "Play", color = TextSecondary) {
                            media.controller?.transportControls?.run { if (track.playing) pause() else play() }
                        }
                        SubText("Next") { media.controller?.transportControls?.skipToNext() }
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

    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No app can open this", Toast.LENGTH_SHORT).show()
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
private fun AppList(apps: List<App>, hidden: Set<String>, onOpen: (App) -> Unit, menu: AppMenu) {
    var query by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    BackHandler(showHidden) { showHidden = false }
    val shown = apps.filter { (it.key in hidden) == showHidden && it.label.contains(query, ignoreCase = true) }
    val hiddenCount = apps.count { it.key in hidden }
    val listState = rememberLazyListState()
    // first list index of each letter present, e.g. {'C'=0, 'D'=5, ...}
    val firstIndex = HashMap<Char, Int>().apply { shown.forEachIndexed { i, app -> putIfAbsent(section(app.label), i) } }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
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
            // hidden while typing: squeezed above the keyboard it'd be too cramped to hit, and results are short anyway
            if (shown.isNotEmpty() && !WindowInsets.isImeVisible) {
                AlphabetScroller(
                    firstIndex,
                    shown.lastIndex,
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

/** Always the full index so every letter keeps its spot: muscle memory beats a compact list. */
private val IndexLetters = listOf('#') + ('A'..'Z')

// digits, symbols and non-Latin scripts all land in '#'
private fun section(label: String) = label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'

private val WaveShift = 40.dp

/**
 * Letter index spread over the full list height: drag to jump, letters near the finger bulge left like a wave.
 * Letters with no apps are dimmed but still land on the next letter that has some.
 */
@Composable
private fun AlphabetScroller(firstIndex: Map<Char, Int>, lastIndex: Int, listState: LazyListState, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    val waveShiftPx = with(LocalDensity.current) { WaveShift.toPx() }
    var letterPx by remember { mutableFloatStateOf(1f) } // height / letters, from the laid-out column
    var touching by remember { mutableStateOf(false) }
    var touchY by remember { mutableFloatStateOf(0f) } // finger y inside the index; kept after lift so the wave fades out in place
    // letters follow the finger with no lag; only the wave's strength eases in and out
    val strength by animateFloatAsState(if (touching) 1f else 0f, tween(if (touching) 120 else 200), label = "wave")
    // recompose only when the letter under the finger changes, not on every move
    val active by remember {
        derivedStateOf { if (touching) (touchY / letterPx).toInt().coerceIn(IndexLetters.indices) else null }
    }

    LaunchedEffect(active) {
        val index = active ?: return@LaunchedEffect
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
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        // read at placement: moving the finger re-places letters without recomposing them
                        .offset {
                            val distance = i + 0.5f - touchY / letterPx // in letters
                            IntOffset(-(waveShiftPx * strength * exp(-distance * distance / 18f)).roundToInt(), 0)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter.toString(),
                        color = when {
                            i == active -> TextPrimary
                            letter in firstIndex -> TextMuted
                            else -> TextMuted.copy(alpha = 0.4f) // no apps under this letter
                        },
                        fontSize = 13.sp,
                    )
                }
            }
        }
        active?.let { index ->
            Box(
                Modifier
                    .offset { IntOffset((-96).dp.roundToPx(), (8.dp.toPx() + touchY - 28.dp.toPx()).roundToInt()) }
                    .size(56.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(IndexLetters[index].toString(), color = Color.Black, fontSize = 28.sp)
            }
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
) {
    var menuOpen by remember { mutableStateOf(false) }
    // propagateMinConstraints: a full-width row makes the whole row tappable; a wrapped one anchors the menu to the label
    Box(modifier, propagateMinConstraints = true) {
        Text(
            text = app.label,
            fontSize = fontSize,
            fontWeight = fontWeight,
            modifier = Modifier
                .combinedClickable(onClick = { onOpen(app) }, onLongClick = { menuOpen = true })
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
        if (menuOpen) { // composed only while open: keeps every list item light while scrolling
            DropdownMenu(expanded = true, onDismissRequest = { menuOpen = false }) {
                menu(app) { menuOpen = false }
            }
        }
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
