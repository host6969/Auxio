/*
 * Copyright (c) 2024 Auxio Project
 * PlaybackServiceFragment.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.playback.service

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import org.oxycblt.auxio.ForegroundListener
import org.oxycblt.auxio.ForegroundServiceNotification
import org.oxycblt.auxio.IntegerTable
import org.oxycblt.auxio.playback.PlaybackSettings
import org.oxycblt.auxio.playback.state.DeferredPlayback
import org.oxycblt.auxio.playback.state.PlaybackStateManager
import org.oxycblt.auxio.playback.system.MediaSessionHolder
import org.oxycblt.auxio.util.logD
import org.oxycblt.auxio.widgets.WidgetComponent

class PlaybackServiceFragment
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val playbackManager: PlaybackStateManager,
    private val playbackSettings: PlaybackSettings,
    private val sessionHolderFactory: MediaSessionHolder.Factory,
    private val widgetComponent: WidgetComponent,
    exoHolderFactory: ExoPlaybackStateHolder.Factory
) : MediaSessionCompat.Callback(), PlaybackStateManager.Listener {
    private val waitJob = Job()
    private val exoHolder = exoHolderFactory.create()
    private var foregroundListener: ForegroundListener? = null

    private lateinit var sessionHolder: MediaSessionHolder
    private lateinit var systemReceiver: SystemPlaybackReceiver

    // --- MEDIASESSION CALLBACKS ---

    @SuppressLint("WrongConstant")
    fun attach(listener: ForegroundListener): MediaSessionCompat.Token {
        foregroundListener = listener
        playbackManager.addListener(this)
        exoHolder.attach()
        sessionHolder = sessionHolderFactory.create(context)
        systemReceiver = SystemPlaybackReceiver(playbackManager, playbackSettings, widgetComponent)
        ContextCompat.registerReceiver(
            context, systemReceiver, systemReceiver.intentFilter, ContextCompat.RECEIVER_EXPORTED)
        widgetComponent.attach()
        return sessionHolder.token
    }

    fun handleTaskRemoved() {
        if (!playbackManager.progression.isPlaying) {
            playbackManager.endSession()
        }
    }

    fun start(startedBy: Int) {
        // At minimum we want to ensure an active playback state.
        // TODO: Possibly also force to go foreground?
        logD("Handling non-native start.")
        val action =
            when (startedBy) {
                IntegerTable.START_ID_ACTIVITY -> null
                IntegerTable.START_ID_TASKER ->
                    DeferredPlayback.RestoreState(
                        play = true, fallback = DeferredPlayback.ShuffleAll)
                // External services using Auxio better know what they are doing.
                else -> DeferredPlayback.RestoreState(play = false)
            }
        if (action != null) {
            logD("Initing service fragment using action $action")
            playbackManager.playDeferred(action)
        }
    }

    val notification: ForegroundServiceNotification?
        get() = if (exoHolder.sessionOngoing) sessionHolder.notification else null

    fun release() {
        waitJob.cancel()
        widgetComponent.release()
        context.unregisterReceiver(systemReceiver)
        sessionHolder.release()
        exoHolder.release()
        playbackManager.removeListener(this)
        foregroundListener = null
    }

    override fun onSessionEnded() {
        foregroundListener?.updateForeground(ForegroundListener.Change.MEDIA_SESSION)
    }
}
