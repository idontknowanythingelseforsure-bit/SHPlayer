package com.shplayer.ui

import com.shplayer.data.Song
import com.shplayer.ui.components.SongMenuActions

/**
 * Playback commands shared by every list/grid screen.
 *
 * Keeping them in one object avoids threading a dozen lambdas through the
 * composable tree and gives the screens a single stable identity.
 */
data class PlayActions(
    val play: (List<Song>, Int) -> Unit,
    val playNext: (Song) -> Unit,
    val enqueue: (Song) -> Unit,
    val queueAll: (List<Song>) -> Unit,
    val shuffleAll: (List<Song>) -> Unit,
    val toggleFavorite: (Song) -> Unit,
    val info: (Song) -> Unit,
    val share: (Song) -> Unit,
    val addToPlaylist: (Song) -> Unit,
)

/**
 * Standard overflow menu for a row in [songs]. Keeps every screen's row menu
 * identical (including the new "Add to playlist" entry).
 */
fun PlayActions.songMenuActions(
    song: Song,
    songs: List<Song>,
    index: Int,
    onRemoveFromPlaylist: (() -> Unit)? = null,
): SongMenuActions = SongMenuActions(
    onPlay = { if (songs.isNotEmpty()) play(songs, index.coerceIn(0, songs.lastIndex)) },
    onPlayNext = { playNext(song) },
    onAddToQueue = { enqueue(song) },
    onToggleFavorite = { toggleFavorite(song) },
    onInfo = { info(song) },
    onShare = { share(song) },
    onAddToPlaylist = { addToPlaylist(song) },
    onRemoveFromPlaylist = onRemoveFromPlaylist,
)
