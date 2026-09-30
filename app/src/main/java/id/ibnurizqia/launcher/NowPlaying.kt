package id.ibnurizqia.launcher

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.NotificationManagerCompat

/** Never reads notifications: Android only hands out media sessions to apps with an enabled listener. */
class MediaListener : NotificationListenerService()

fun Context.hasNotificationAccess() = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)

class NowPlaying(val title: String, val artist: String, val playing: Boolean)

/** Follows the current media session between [start] and [stop]. Needs notification access for [MediaListener]. */
class MediaWatcher(context: Context) {
    var nowPlaying by mutableStateOf<NowPlaying?>(null)
        private set
    var controller: MediaController? = null
        private set

    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, MediaListener::class.java)
    private val onSessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { follow(it) }
    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
    }

    fun start() {
        runCatching { // SecurityException if notification access was revoked
            sessions.addOnActiveSessionsChangedListener(onSessionsChanged, listener)
            follow(sessions.getActiveSessions(listener))
        }
    }

    // keeps the last nowPlaying so the home screen doesn't flicker on return
    fun stop() {
        sessions.removeOnActiveSessionsChangedListener(onSessionsChanged)
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun follow(active: List<MediaController>?) {
        controller?.unregisterCallback(callback)
        // whatever is playing, else the top-priority session (e.g. a paused player)
        controller = active?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: active?.firstOrNull()
        controller?.registerCallback(callback)
        update()
    }

    private fun update() {
        val state = controller?.playbackState?.state
        val metadata = controller?.metadata
        nowPlaying = if (metadata != null && (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED)) {
            NowPlaying(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
                playing = state == PlaybackState.STATE_PLAYING,
            )
        } else {
            null
        }
    }
}

// Material "filled" media icons missing from material-icons-core (it only has PlayArrow); paths are Google's
// originals, copied rather than pulling in the multi-megabyte material-icons-extended for three glyphs.
val PauseIcon: ImageVector by lazy {
    materialIcon("Filled.Pause") {
        materialPath {
            moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); verticalLineToRelative(14f); close()
            moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineToRelative(-4f); close()
        }
    }
}

val SkipNextIcon: ImageVector by lazy {
    materialIcon("Filled.SkipNext") {
        materialPath {
            moveTo(6f, 18f); lineToRelative(8.5f, -6f); lineTo(6f, 6f); verticalLineToRelative(12f); close()
            moveTo(16f, 6f); verticalLineToRelative(12f); horizontalLineToRelative(2f); verticalLineTo(6f); horizontalLineToRelative(-2f); close()
        }
    }
}

val SkipPreviousIcon: ImageVector by lazy {
    materialIcon("Filled.SkipPrevious") {
        materialPath {
            moveTo(6f, 6f); horizontalLineToRelative(2f); verticalLineToRelative(12f); horizontalLineTo(6f); close()
            moveTo(9.5f, 12f); lineToRelative(8.5f, 6f); verticalLineTo(6f); close()
        }
    }
}
