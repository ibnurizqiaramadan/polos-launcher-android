package com.minimalist.launcher

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
