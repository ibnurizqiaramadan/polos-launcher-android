package id.ibnurizqia.launcher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Navigation gestures, always on while the service is enabled (meant for 3-button nav, which HyperOS forces
 * with third-party launchers): swipe in from a side edge = Back, swipe up from the bottom centre = Home,
 * swipe up and hold = Recents. Also backs the home screen's double-tap-to-lock.
 * On gesture-nav phones the system claims its own edge swipes first (our strip just gets a cancel), so
 * nothing fires twice.
 * It only lays invisible touch strips over the screen edges and fires global actions; it never reads the screen.
 */
class GestureService : AccessibilityService() {
    private val zones = mutableListOf<View>()

    companion object {
        // same-process handle so the launcher can ask for a lock; null while the service is off
        private var running: GestureService? = null

        /** Locks like the power button (biometrics keep working, unlike DevicePolicyManager.lockNow). False if unavailable. */
        fun lockScreen(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && running?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true

        /**
         * Switches the service off, as if toggled in Accessibility settings; turning it back on has to happen
         * there. For banking apps that refuse to run while any accessibility service is on. False if it's off already.
         */
        fun stop(): Boolean = running?.let { ServiceLog.add(it, "switched off from Polos settings"); it.disableSelf(); true } ?: false
    }

    override fun onServiceConnected() {
        running = this
        addZones()
        ServiceLog.add(this, "gesture service connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        ServiceLog.add(this, "gesture service unbound by the system")
        return super.onUnbind(intent)
    }

    // strip sizes depend on screen size, so rebuild them on rotation
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        removeZones()
        addZones()
    }

    override fun onDestroy() {
        ServiceLog.add(this, "gesture service destroyed")
        running = null
        removeZones()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {} // subscribes to no events
    override fun onInterrupt() {}

    private fun addZones() {
        val windows = getSystemService(WindowManager::class.java)
        val metrics = resources.displayMetrics
        fun dp(value: Int) = (value * metrics.density).roundToInt()
        fun add(zone: View, width: Int, height: Int, gravity: Int) {
            val params = WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply { this.gravity = gravity }
            windows.addView(zone, params)
            zones += zone
        }
        // ponytail: strips swallow taps in the outer 16dp and bottom 12dp; per-app exclusions if that ever bites
        val edgeHeight = (metrics.heightPixels * 0.7f).roundToInt()
        add(EdgeZone(this, fromLeft = true), dp(16), edgeHeight, Gravity.START or Gravity.CENTER_VERTICAL)
        add(EdgeZone(this, fromLeft = false), dp(16), edgeHeight, Gravity.END or Gravity.CENTER_VERTICAL)
        add(BottomZone(this), (metrics.widthPixels * 0.4f).roundToInt(), dp(12), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
    }

    private fun removeZones() {
        val windows = getSystemService(WindowManager::class.java)
        zones.forEach { runCatching { windows.removeView(it) } }
        zones.clear()
    }
}

fun Context.gesturesEnabled(): Boolean {
    val ours = ComponentName(this, GestureService::class.java)
    return Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?.split(':')
        ?.any { ComponentName.unflattenFromString(it) == ours } == true
}

/** [Broken]: switched on, but Android stopped it (its process died, e.g. after an update) and won't restart it until it's toggled. */
enum class GestureState { Off, On, Broken }

// the enabled-services list only holds services that are actually bound, so a crashed one is missing from it
fun Context.gestureState(): GestureState {
    if (!gesturesEnabled()) return GestureState.Off
    val ours = ComponentName(this, GestureService::class.java)
    val bound = getSystemService(AccessibilityManager::class.java)
        .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { ComponentName.unflattenFromString(it.id) == ours }
    return if (bound) GestureState.On else GestureState.Broken
}

/** Accessibility settings, with the hint stock Android uses to scroll to and highlight our service; harmless elsewhere. */
fun Context.accessibilitySettingsIntent(): Intent {
    val ours = ComponentName(this, GestureService::class.java).flattenToString()
    return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        .putExtra(":settings:fragment_args_key", ours)
        .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", ours) })
}

/**
 * Whether the phone is on button navigation. HyperOS/MIUI keep their own switch (force_fsg_nav_bar: 1 = full
 * screen gestures) and can leave AOSP's "navigation_mode" stale after toggling, showing 3 buttons while it
 * still says gestures, so their switch wins when it exists. Elsewhere "navigation_mode" (not public API):
 * 0 = 3-button, 1 = 2-button, 2 = gestures; missing means buttons.
 */
fun Context.usesButtonNavigation(): Boolean {
    Settings.Global.getString(contentResolver, MIUI_GESTURES)?.let { return it == "0" }
    return Settings.Secure.getInt(contentResolver, NAVIGATION_MODE, 0) == 0
}

private const val MIUI_GESTURES = "force_fsg_nav_bar"
private const val NAVIGATION_MODE = "navigation_mode"

private const val THRESHOLD_DP = 40
private const val HOLD_MS = 300L

/** Swipe inward past the threshold, then release: Back. A tick marks the point of no return. */
@SuppressLint("ClickableViewAccessibility", "ViewConstructor")
private class EdgeZone(private val service: AccessibilityService, private val fromLeft: Boolean) : View(service) {
    private val threshold = THRESHOLD_DP * resources.displayMetrics.density
    private var startX = 0f
    private var startY = 0f
    private var armed = false

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                armed = false
            }
            MotionEvent.ACTION_MOVE -> {
                val inward = (event.rawX - startX) * if (fromLeft) 1 else -1
                val nowArmed = inward > threshold && inward > abs(event.rawY - startY)
                if (nowArmed && !armed) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                armed = nowArmed
            }
            MotionEvent.ACTION_UP -> if (armed) service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }
        return true
    }
}

/** Swipe up and release: Home. Swipe up and hold still: Recents. A plain tap: Home, since it sits on the Home button. */
@SuppressLint("ClickableViewAccessibility", "ViewConstructor")
private class BottomZone(private val service: AccessibilityService) : View(service) {
    private val density = resources.displayMetrics.density
    private var startX = 0f
    private var startY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var armed = false
    private var handled = false
    private val openRecents = Runnable {
        handled = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX; startY = event.rawY
                lastX = event.rawX; lastY = event.rawY
                armed = false
                handled = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!armed && startY - event.rawY > THRESHOLD_DP * density) {
                    armed = true
                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    postDelayed(openRecents, HOLD_MS)
                } else if (armed && !handled && hypot(event.rawX - lastX, event.rawY - lastY) > 8 * density) {
                    // still travelling: the hold only counts once the finger rests
                    removeCallbacks(openRecents)
                    postDelayed(openRecents, HOLD_MS)
                }
                if (hypot(event.rawX - lastX, event.rawY - lastY) > 8 * density) {
                    lastX = event.rawX; lastY = event.rawY
                }
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(openRecents)
                val tap = hypot(event.rawX - startX, event.rawY - startY) < 8 * density
                if (!handled && (armed || tap)) service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
            MotionEvent.ACTION_CANCEL -> removeCallbacks(openRecents)
        }
        return true
    }
}
