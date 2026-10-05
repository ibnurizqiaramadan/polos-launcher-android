package id.ibnurizqia.launcher

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/** What the user can change about the launcher. Each change is saved at once, and Compose sees it right away. */
class LauncherSettings(private val prefs: SharedPreferences) {
    inner class Option(private val key: String, default: Boolean) {
        var on by mutableStateOf(prefs.getBoolean(key, default))
            private set

        fun toggle() {
            on = !on
            prefs.edit { putBoolean(key, on) }
        }
    }

    var wallpaper by mutableStateOf(Wallpaper.entries.find { it.name == prefs.getString("wallpaper", null) } ?: Wallpaper.Black)
        private set

    fun pick(wallpaper: Wallpaper) {
        this.wallpaper = wallpaper
        prefs.edit { putString("wallpaper", wallpaper.name) }
    }

    // home screen: everything on it except the clock, date and favorites can be turned off
    val weather = Option("show:weather", true)
    val battery = Option("show:battery", true)
    val music = Option("show:music", true)
    val dots = Option("show:dots", true)
    val events = Option("show:events", true)
    val alarm = Option("show:alarm", true)
    val device = Option("show:device", true)
    val screenTime = Option("show:screen_time", true)
    val shortcuts = Option("show:shortcuts", true)
    val hints = Option("show:hints", true)

    // app drawer
    /** What shows behind the app drawer: the blurred home screen, just the wallpaper, or plain black. */
    enum class DrawerBackground { Home, Wallpaper, Black }

    // the earlier "blur" toggle maps onto this: off meant black
    var drawerBackground by mutableStateOf(
        DrawerBackground.entries.find { it.name == prefs.getString("drawer:background", null) }
            ?: if (prefs.getBoolean("drawer:blur", true)) DrawerBackground.Home else DrawerBackground.Black
    )
        private set

    fun pick(background: DrawerBackground) {
        drawerBackground = background
        prefs.edit { putString("drawer:background", background.name) }
    }

    inner class Level(private val key: String, default: Int) {
        var value by mutableIntStateOf(prefs.getInt(key, default))
            private set

        fun set(level: Int) {
            if (level == value) return // a slider drag reports the same step many times
            value = level
            prefs.edit { putInt(key, level) }
        }
    }

    // blur radius in 5dp steps, one per background so switching doesn't carry a value that suits the other
    val homeBlur = Level("drawer:blur_level", 5) // 1..10: home without blur would be text over text
    val wallpaperBlur = Level("drawer:wallpaper_blur", 0) // 0..10, 0 keeps the pattern crisp

    val recent = Option("drawer:recent", true)
    val index = Option("drawer:index", true)
    val keyboard = Option("drawer:keyboard", false)

    // gestures on the home screen
    val doubleTapLock = Option("gesture:double_tap_lock", true)
    val swipeDown = Option("gesture:swipe_down", true)
}
