package com.shplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.shplayer.data.ArtworkLoader
import com.shplayer.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class PlayerUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val songId: Long = -1L,
    val albumId: Long = -1L,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queueIndex: Int = 0,
    val queueSize: Int = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val formatLine: String = "",
) {
    val hasTrack: Boolean get() = title.isNotBlank()
}

/**
 * Process-wide bridge between the Compose UI and [PlaybackService].
 *
 * OPTIMIZATION HOOK: this is a singleton for simplicity. It can be replaced by a
 * DI-scoped controller (Hilt/Koin) without touching the screens, which only read
 * [state] and call the public commands.
 */
object PlayerController {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connecting = false
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var queue: List<Song> = emptyList()
    private var pendingPlay: Pair<List<Song>, Int>? = null
    private val pendingOnConnected = mutableListOf<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
        }
    }

    fun ensureConnected(context: Context) {
        if (controller != null || connecting) return
        connecting = true

        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()

        future.addListener({
            connecting = false
            val result = runCatching { future.get() }
            result.onSuccess { mediaController ->
                controller = mediaController
                mediaController.addListener(listener)
                sync(mediaController)
                startTicker()
                pendingPlay?.let { (songs, index) ->
                    pendingPlay = null
                    play(app, songs, index)
                }
                val queued = synchronized(pendingOnConnected) {
                    pendingOnConnected.toList().also { pendingOnConnected.clear() }
                }
                queued.forEach { block -> runCatching { block(mediaController) } }
            }.onFailure {
                _state.update { it.copy(connected = false) }
            }
        }, ContextCompat.getMainExecutor(app))
    }

    /**
     * Runs [block] as soon as the MediaController is connected. Used by the
     * widget so its buttons work even when the process was started cold by the
     * broadcast.
     */
    fun whenConnected(context: Context, block: (MediaController) -> Unit) {
        val existing = controller
        if (existing != null) {
            block(existing)
            return
        }
        synchronized(pendingOnConnected) { pendingOnConnected.add(block) }
        ensureConnected(context)
    }

    fun play(context: Context, songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        ensureConnected(context)

        val mediaController = controller
        if (mediaController == null) {
            pendingPlay = songs to startIndex
            return
        }

        queue = songs
        mediaController.shuffleModeEnabled = false
        mediaController.setMediaItems(
            songs.map { it.toMediaItem() },
            startIndex.coerceIn(0, songs.lastIndex),
            0L,
        )
        mediaController.prepare()
        mediaController.play()
    }

    /** Inserts after the current track without disturbing playback position. */
    fun playNext(context: Context, song: Song) {
        val mediaController = controller
        if (mediaController == null || queue.isEmpty()) {
            play(context, listOf(song), 0)
            return
        }

        val index = mediaController.currentMediaItemIndex
        val position = mediaController.currentPosition
        val wasPlaying = mediaController.isPlaying

        val updated = queue.toMutableList().apply {
            add((index + 1).coerceIn(0, size), song)
        }
        queue = updated
        mediaController.setMediaItems(updated.map { it.toMediaItem() }, index, position)
        if (wasPlaying) mediaController.play()
    }

    /** Appends to the end of the current queue. */
    fun addToQueue(context: Context, song: Song) {
        val mediaController = controller
        if (mediaController == null || queue.isEmpty()) {
            play(context, listOf(song), 0)
            return
        }

        val index = mediaController.currentMediaItemIndex
        val position = mediaController.currentPosition
        val wasPlaying = mediaController.isPlaying

        val updated = queue + song
        queue = updated
        mediaController.setMediaItems(updated.map { it.toMediaItem() }, index, position)
        if (wasPlaying) mediaController.play()
    }

    /** Appends a whole list in one queue rebuild. */
    fun enqueue(context: Context, songs: List<Song>) {
        if (songs.isEmpty()) return
        val mediaController = controller
        if (mediaController == null || queue.isEmpty()) {
            play(context, songs, 0)
            return
        }

        val index = mediaController.currentMediaItemIndex
        val position = mediaController.currentPosition
        val wasPlaying = mediaController.isPlaying

        val updated = queue + songs
        queue = updated
        mediaController.setMediaItems(updated.map { it.toMediaItem() }, index, position)
        if (wasPlaying) mediaController.play()
    }

    fun playQueue(context: Context, songs: List<Song>, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val list = if (shuffle) songs.shuffled() else songs
        play(context, list, 0)
        controller?.shuffleModeEnabled = shuffle
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            if (mediaController.playbackState == Player.STATE_IDLE) {
                mediaController.prepare()
            }
            mediaController.play()
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val mediaController = controller ?: return
        if (mediaController.currentPosition > 3_000L) {
            mediaController.seekTo(0L)
        } else {
            mediaController.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun toggleShuffle() {
        val mediaController = controller ?: return
        mediaController.shuffleModeEnabled = !mediaController.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val mediaController = controller ?: return
        mediaController.repeatMode = when (mediaController.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    /** Pauses playback after [minutes]; cancels any previous timer. */
    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        sleepJob = null
        if (minutes <= 0) return
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
    }

    private fun startTicker() {
        if (ticker != null) return
        ticker = scope.launch {
            while (isActive) {
                val mediaController = controller
                if (mediaController != null && mediaController.isPlaying) {
                    val duration = mediaController.duration
                    _state.update {
                        it.copy(
                            positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                            durationMs = if (duration > 0) duration else it.durationMs,
                        )
                    }
                }
                delay(500L)
            }
        }
    }

    private fun sync(player: Player) {
        val index = player.currentMediaItemIndex
        val song = queue.getOrNull(index)
        val duration = player.duration
        val formatLine = formatLineOf(player)
        _state.update {
            it.copy(
                connected = true,
                isPlaying = player.isPlaying,
                songId = song?.id ?: -1L,
                albumId = song?.albumId ?: -1L,
                title = song?.title ?: player.mediaMetadata.title?.toString().orEmpty(),
                artist = song?.artist ?: player.mediaMetadata.artist?.toString().orEmpty(),
                album = song?.album.orEmpty(),
                durationMs = if (duration > 0) duration else (song?.durationMs ?: 0L),
                positionMs = player.currentPosition.coerceAtLeast(0L),
                queueIndex = index.coerceAtLeast(0),
                queueSize = player.mediaItemCount,
                shuffle = player.shuffleModeEnabled,
                repeatMode = player.repeatMode,
                hasNext = player.hasNextMediaItem(),
                hasPrevious = player.hasPreviousMediaItem(),
                formatLine = formatLine,
            )
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun formatLineOf(player: Player): String {
        val format = player.currentTracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.length > 0 }
            ?.getTrackFormat(0) ?: return ""

        val codec = codecName(format.sampleMimeType)
        val sampleRate = if (format.sampleRate > 0) {
            String.format(Locale.US, "%.1fkHz", format.sampleRate / 1000f)
        } else {
            ""
        }
        val bitrate = if (format.bitrate > 0) "${format.bitrate / 1000}kbps" else ""
        val channels = when (format.channelCount) {
            1 -> "mono"
            2 -> "stereo"
            else -> ""
        }
        return listOf(codec, sampleRate, bitrate, channels)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }

    private fun codecName(mime: String?): String = when {
        mime.isNullOrEmpty() -> ""
        mime.contains("mpeg", ignoreCase = true) -> "mpeg"
        mime.contains("aac", ignoreCase = true) -> "aac"
        mime.contains("flac", ignoreCase = true) -> "flac"
        mime.contains("opus", ignoreCase = true) -> "opus"
        mime.contains("vorbis", ignoreCase = true) -> "vorbis"
        mime.contains("pcm", ignoreCase = true) -> "pcm"
        else -> mime.substringAfterLast('/')
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(if (albumId > 0L) ArtworkLoader.albumArtUri(albumId) else null)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build(),
        )
        .build()
}
