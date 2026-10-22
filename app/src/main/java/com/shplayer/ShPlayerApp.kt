package com.shplayer

import android.app.Application
import com.shplayer.audio.EffectsManager
import com.shplayer.data.Prefs
import com.shplayer.playback.PlayerController
import com.shplayer.widget.PlayerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ShPlayerApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        EffectsManager.init(this)
        observePlayerForWidget()
    }

    /**
     * Keeps every placed widget in sync with playback. The filter only allows
     * one update per elapsed second, which keeps the widget's progress line
     * moving without rebuilding RemoteViews twice per second.
     */
    private fun observePlayerForWidget() {
        appScope.launch {
            PlayerController.state
                .distinctUntilChanged { old, new ->
                    old.title == new.title &&
                        old.artist == new.artist &&
                        old.isPlaying == new.isPlaying &&
                        old.queueIndex == new.queueIndex &&
                        old.queueSize == new.queueSize &&
                        old.albumId == new.albumId &&
                        old.durationMs == new.durationMs &&
                        old.positionMs / 1000L == new.positionMs / 1000L
                }
                .collect {
                    runCatching { PlayerWidgetProvider.updateAll(this@ShPlayerApp) }
                }
        }
    }
}
