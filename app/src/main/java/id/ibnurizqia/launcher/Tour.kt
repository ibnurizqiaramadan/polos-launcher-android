package id.ibnurizqia.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

/** One highlight: a spot on the home screen (by [spot] key, null for the whole screen) and what to say about it. */
class TourStep(val spot: String?, val title: String, val body: String)

val TourSteps = listOf(
    TourStep("header", "Time and date", "Tap the time for your alarms, the date for your calendar. Weather and battery sit underneath once you turn them on."),
    TourStep("favorites", "Favorites", "Up to six apps within thumb reach. Hold one to reorder it, or to take it off home."),
    TourStep("today", "Today", "Weather, events, your next alarm and device stats. Each line opens the app behind it."),
    TourStep("bottom", "Shortcuts", "Phone and Camera in the corners, today's screen time in the middle."),
    TourStep(null, "Gestures", "Swipe up for all your apps, swipe down for notifications. Hold an empty spot for Settings, double-tap to lock."),
)

/** Records this element's bounds under [key] for the tour's spotlight. */
fun Modifier.spot(key: String, spots: MutableMap<String, Rect>) = onGloballyPositioned {
    spots[key] = Rect(it.positionInRoot(), Size(it.size.width.toFloat(), it.size.height.toFloat()))
}

/**
 * The first-run highlights: the home screen dims except for a soft-cornered window around the step's spot,
 * with a card saying what it does. Touches never reach home underneath; Skip and the Back button leave at
 * any point. Steps whose spot isn't on screen (e.g. Today before any access is granted) are left out.
 */
@Composable
fun TourOverlay(spots: Map<String, Rect>, onDone: () -> Unit) {
    val steps = TourSteps.filter { it.spot == null || (spots[it.spot]?.height ?: 0f) > 1f }
    var index by remember { mutableStateOf(0) }
    BackHandler(onBack = onDone)
    val step = steps[index.coerceAtMost(steps.lastIndex)]
    val density = LocalDensity.current
    val pad = with(density) { 12.dp.toPx() }
    // the window glides from one spot to the next; the full-screen step shrinks it away to nothing
    val target = step.spot?.let { spots[it] }?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }
    var screen by remember { mutableStateOf(Size.Zero) }
    val window by animateValueAsState(
        target ?: Rect(Offset(screen.width / 2, screen.height / 2), Size.Zero),
        Rect.VectorConverter, tween(300, easing = EaseOutCubic), label = "spotlight",
    )
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screen = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) {} // topmost: home below sees none of the touches
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = 0.72f))
            if (window.width > 0f) {
                drawRoundRect(Color.Black, window.topLeft, window.size, CornerRadius(20.dp.toPx()), blendMode = BlendMode.Clear)
            }
        }
        // the card sits under the window when there's room, else above it; centred for the full-screen step
        val insets = WindowInsets.safeDrawing
        val top = insets.getTop(density); val bottom = insets.getBottom(density)
        val gap = with(density) { 24.dp.toPx() }
        var cardHeight by remember { mutableStateOf(0) }
        Column(
            Modifier
                .offset {
                    val y = when {
                        target == null -> (screen.height - cardHeight) / 2
                        target.bottom < screen.height * 0.6f -> target.bottom + gap
                        else -> target.top - gap - cardHeight
                    }
                    IntOffset(0, y.coerceIn(top + gap, (screen.height - bottom - gap - cardHeight).coerceAtLeast(top + gap)).roundToInt())
                }
                .onSizeChanged { cardHeight = it.height }
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceRaised)
                .padding(20.dp),
        ) {
                if (step.spot == null) {
                    // one small motion with meaning: the swipe this step talks about
                    val bounce by rememberInfiniteTransition(label = "swipe").animateFloat(
                        0f, -10f, infiniteRepeatable(tween(600, easing = EaseOutCubic), RepeatMode.Reverse), label = "bounce",
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = TextPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).size(36.dp).offset { IntOffset(0, bounce.roundToInt()) },
                    )
                }
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(tween(200, easing = EaseOutCubic)) togetherWith fadeOut(tween(120, easing = EaseInCubic)) },
                    label = "step",
                ) { s ->
                    Column {
                        Text(s.title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(8.dp))
                        Text(s.body, fontSize = 15.sp, color = TextSecondary, lineHeight = 22.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1} of ${steps.size}", fontSize = 14.sp, color = TextMuted)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDone) { Text("Skip", color = TextSecondary) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { if (index < steps.lastIndex) index++ else onDone() },
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Color.Black),
                    ) { Text(if (index < steps.lastIndex) "Next" else "Done") }
            }
        }
    }
}

/**
 * Every optional access with whether it's granted. A missing one opens the place to grant it; a granted one
 * opens App info, where it can be taken away again.
 */
@Composable
fun AccessPage(access: List<Access>, onAppInfo: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val granted = access.count { it.granted }
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
            Text("Permissions", fontSize = 22.sp)
        }
        Text(
            if (granted == access.size) "Everything is granted. All of these are optional; each one only adds the feature next to it."
            else "$granted of ${access.size} granted. All are optional; each one only adds the feature next to it. Tap one to grant it.",
            fontSize = 14.sp, color = TextMuted, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SettingGroup(
            "Access",
            access.map {
                SettingRow(
                    it.title, it.purpose,
                    value = if (it.granted) "Granted" else "Not granted",
                    emphasised = !it.granted,
                    onClick = if (it.granted) onAppInfo else it.grant,
                )
            },
        )
        SettingGroup("Everything else", listOf(SettingRow("App info", "Internet is the only other permission, used for the weather request alone", onClick = onAppInfo)))
    }
}

/** Every gesture and tap in one place, plus a way to see the highlights again. */
@Composable
fun GuidePage(navGestures: Boolean, onReplay: () -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
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
            Text("How to use", fontSize = 22.sp)
        }
        SettingGroup(
            "Home screen",
            listOf(
                SettingRow("Swipe up", "All your apps"),
                SettingRow("Swipe down", "Notifications"),
                SettingRow("Tap the time, date, weather or battery", "Alarms, calendar, forecast, battery details"),
                SettingRow("Tap a Today line", "Opens the event, alarm or forecast behind it"),
                SettingRow("Hold a favorite, then drag", "Reorders it; hold without moving for its menu"),
                SettingRow("Hold an empty spot", "Settings"),
                SettingRow("Double-tap an empty spot", "Locks the screen (needs navigation gestures)"),
            ),
        )
        SettingGroup(
            "App drawer",
            listOf(
                SettingRow("Tap search", "The best match sits right above the field; Enter opens it"),
                SettingRow("Drag the letters on the right", "Jumps through the list"),
                SettingRow("Hold an app", "Add to home, rename, hide, app info"),
                SettingRow("Pull down at the top", "Closes the drawer"),
                SettingRow("Hidden apps", "Listed at the very bottom"),
            ),
        )
        SettingGroup(
            "Navigation gestures",
            listOf(
                SettingRow("Swipe in from the left or right edge", "Back"),
                SettingRow("Swipe up from the bottom centre", "Home"),
                SettingRow("Swipe up and hold", "Recents"),
                if (navGestures) SettingRow("On", "Polos gestures is enabled in Accessibility")
                else SettingRow("Off", "Turn on under Settings > Gestures > Navigation gestures"),
            ),
        )
        SettingGroup("Highlights", listOf(SettingRow("Show the highlights again", "The short tour from the first start", onClick = onReplay)))
    }
}
