package com.shplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import com.shplayer.MainActivity
import com.shplayer.R
import com.shplayer.SeekOverlayActivity
import com.shplayer.playback.PlayerController
import com.shplayer.playback.PlayerUiState
import java.util.concurrent.atomic.AtomicBoolean

/**
 * PlayerPro-style home-screen widget: disc, track counter, title/artist,
 * prev/play-pause/next and a progress line. Tapping the progress line opens a
 * compact seek sheet (RemoteViews cannot host a draggable slider itself).
 * Controls are broadcast to this receiver, which waits for the MediaController
 * connection so they also work from a cold process.
 */
class PlayerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val state = PlayerController.state.value
        appWidgetIds.forEach { id -> update(context, appWidgetManager, id, state) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null) {
            super.onReceive(context, intent)
            return
        }

        if (action != ACTION_PREV && action != ACTION_TOGGLE && action != ACTION_NEXT) {
            super.onReceive(context, intent)
            return
        }

        val result = goAsync()
        val finished = AtomicBoolean(false)
        val finish = {
            if (finished.compareAndSet(false, true)) result.finish()
        }
        // Safety net: never hold the broadcast longer than the ANR window.
        Handler(Looper.getMainLooper()).postDelayed(Runnable { finish() }, 5_000L)

        PlayerController.whenConnected(context) { controller ->
            when (action) {
                ACTION_PREV -> if (controller.currentPosition > 3_000L) {
                    controller.seekTo(0L)
                } else if (controller.hasPreviousMediaItem()) {
                    controller.seekToPreviousMediaItem()
                }
                ACTION_NEXT -> if (controller.hasNextMediaItem()) {
                    controller.seekToNextMediaItem()
                }
                ACTION_TOGGLE -> if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
            }
            finish()
        }
    }

    companion object {
        const val ACTION_PREV = "com.shplayer.widget.PREV"
        const val ACTION_TOGGLE = "com.shplayer.widget.TOGGLE"
        const val ACTION_NEXT = "com.shplayer.widget.NEXT"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, PlayerWidgetProvider::class.java),
            )
            if (ids.isEmpty()) return
            val state = PlayerController.state.value
            ids.forEach { id -> update(context, manager, id, state) }
        }

        private fun update(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            state: PlayerUiState,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_player_v2)
            views.setTextViewText(
                R.id.widget_title,
                state.title.ifBlank { context.getString(R.string.app_name) },
            )
            views.setTextViewText(
                R.id.widget_artist,
                if (state.hasTrack) state.artist.ifBlank { "Unknown artist" } else "Nothing playing",
            )
            if (state.queueSize > 0) {
                views.setViewVisibility(R.id.widget_badge, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_badge,
                    "${state.queueIndex + 1}/${state.queueSize}",
                )
            } else {
                views.setViewVisibility(R.id.widget_badge, View.GONE)
            }
            views.setImageViewResource(
                R.id.widget_play,
                if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            views.setOnClickPendingIntent(
                R.id.widget_prev,
                broadcast(context, ACTION_PREV, 101),
            )
            views.setOnClickPendingIntent(
                R.id.widget_play,
                broadcast(context, ACTION_TOGGLE, 102),
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                broadcast(context, ACTION_NEXT, 103),
            )
            views.setOnClickPendingIntent(
                R.id.widget_seek_area,
                openSeek(context),
            )

            // ClipDrawable levels run 0..10000. RemoteViews has no direct
            // "set level" action, but setInt invokes ImageView.setImageLevel.
            val progressLevel = if (state.durationMs > 0L) {
                ((state.positionMs.toDouble() / state.durationMs) * 10_000.0)
                    .toInt()
                    .coerceIn(0, 10_000)
            } else {
                0
            }
            views.setInt(R.id.widget_progress, "setImageLevel", progressLevel)

            views.setOnClickPendingIntent(R.id.widget_root, openApp(context))
            manager.updateAppWidget(widgetId, views)
        }

        private fun broadcast(
            context: Context,
            action: String,
            requestCode: Int,
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, PlayerWidgetProvider::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun openSeek(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            106,
            Intent(context, SeekOverlayActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            100,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
