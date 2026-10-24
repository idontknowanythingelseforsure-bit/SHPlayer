package com.shplayer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shplayer.data.Song
import com.shplayer.ui.LibraryUiState
import com.shplayer.ui.LibraryViewModel
import com.shplayer.ui.PlayActions
import com.shplayer.ui.SmartKind
import com.shplayer.ui.SortMode
import com.shplayer.ui.sortedFor
import com.shplayer.ui.components.DetailHeader
import com.shplayer.ui.components.EmptyState
import com.shplayer.ui.components.FastScroller
import com.shplayer.ui.components.SongRow
import com.shplayer.ui.songMenuActions

/** Shared by [SmartScreen] and the top bar's "add current list" action. */
fun smartSongs(kind: SmartKind, state: LibraryUiState): List<Song> = when (kind) {
    SmartKind.RecentlyPlayed -> {
        val songsById = state.songs.associateBy { it.id }
        state.recentlyPlayed.mapNotNull { songsById[it] }
    }
    SmartKind.RecentlyAdded -> state.songs.sortedByDescending { it.dateAdded }
    SmartKind.TopRated -> state.songs.filter { it.id in state.favorites }
    SmartKind.MostPlayed -> state.songs
        .filter { (state.playCounts[it.id] ?: 0) > 0 }
        .sortedByDescending { state.playCounts[it.id] ?: 0 }
}

@Composable
fun SongListPage(
    songs: List<Song>,
    actions: PlayActions,
    currentSongId: Long,
    favorites: Set<Long>,
    header: (@Composable () -> Unit)? = null,
    showArtwork: Boolean = false,
    emptyText: String = "No songs here",
    /** Available only while viewing a user-created playlist. */
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
) {
    val listState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        if (songs.isEmpty()) {
            EmptyState(emptyText)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (header != null) {
                    item(key = "header") { header() }
                }
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        showArtwork = showArtwork,
                        isFavorite = song.id in favorites,
                        actions = actions.songMenuActions(
                            song = song,
                            songs = songs,
                            index = index,
                            onRemoveFromPlaylist = onRemoveFromPlaylist?.let { remove ->
                                { remove(song) }
                            },
                        ),
                    )
                }
                item(key = "bottom-space") { Spacer(Modifier.height(12.dp)) }
            }

            FastScroller(
                listState = listState,
                itemCount = songs.size + if (header != null) 1 else 0,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(24.dp)
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun AllSongsScreen(
    state: LibraryUiState,
    actions: PlayActions,
    currentSongId: Long,
    sortMode: SortMode = SortMode.Default,
) {
    SongListPage(
        songs = state.songs.sortedFor(sortMode),
        actions = actions,
        currentSongId = currentSongId,
        favorites = state.favorites,
    )
}

@Composable
fun SmartScreen(
    kind: SmartKind,
    state: LibraryUiState,
    actions: PlayActions,
    currentSongId: Long,
    sortMode: SortMode = SortMode.Default,
) {
    val songs = smartSongs(kind, state).sortedFor(sortMode)

    SongListPage(
        songs = songs,
        actions = actions,
        currentSongId = currentSongId,
        favorites = state.favorites,
        header = {
            DetailHeader(
                title = kind.title,
                subtitle = kind.subtitle,
                meta = "${songs.map { it.artist }.distinct().size} artists",
                countText = songs.size.toString(),
                artAlbumId = songs.firstOrNull()?.albumId ?: 0L,
                showDisc = true,
                onPlayAll = { actions.play(songs, 0) },
                onShuffle = { actions.shuffleAll(songs) },
                onQueueAll = { actions.queueAll(songs) },
            )
        },
        emptyText = when (kind) {
            SmartKind.RecentlyPlayed -> "Nothing played yet"
            SmartKind.TopRated -> "No favorites yet"
            SmartKind.MostPlayed -> "Nothing played yet"
            SmartKind.RecentlyAdded -> "No songs yet"
        },
    )
}

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    state: LibraryUiState,
    actions: PlayActions,
    currentSongId: Long,
    sortMode: SortMode = SortMode.Default,
) {
    val album = state.albums.firstOrNull { it.id == albumId }
    val songs = state.songs
        .filter { it.albumId == albumId }
        .sortedBy { it.trackNumber }
        .sortedFor(sortMode)

    SongListPage(
        songs = songs,
        actions = actions,
        currentSongId = currentSongId,
        favorites = state.favorites,
        header = {
            DetailHeader(
                title = album?.title ?: songs.firstOrNull()?.album.orEmpty().ifBlank { "Album" },
                subtitle = album?.artist ?: songs.firstOrNull()?.artist.orEmpty(),
                meta = album?.year?.takeIf { it > 0 }?.toString().orEmpty(),
                countText = songs.size.toString(),
                artAlbumId = albumId,
                showDisc = false,
                onPlayAll = { actions.play(songs, 0) },
                onShuffle = { actions.shuffleAll(songs) },
                onQueueAll = { actions.queueAll(songs) },
            )
        },
    )
}

@Composable
fun ArtistDetailScreen(
    artist: String,
    state: LibraryUiState,
    actions: PlayActions,
    currentSongId: Long,
    sortMode: SortMode = SortMode.Default,
) {
    val songs = state.songs.filter { it.artist == artist }.sortedFor(sortMode)
    val albumCount = songs.map { it.albumId }.distinct().size
    val artAlbumId = songs.firstOrNull { it.albumId > 0 }?.albumId ?: 0L

    SongListPage(
        songs = songs,
        actions = actions,
        currentSongId = currentSongId,
        favorites = state.favorites,
        header = {
            DetailHeader(
                title = artist,
                subtitle = "Artist",
                meta = "$albumCount albums",
                countText = songs.size.toString(),
                artAlbumId = artAlbumId,
                showDisc = false,
                onPlayAll = { actions.play(songs, 0) },
                onShuffle = { actions.shuffleAll(songs) },
                onQueueAll = { actions.queueAll(songs) },
            )
        },
    )
}

@Composable
fun GenreDetailScreen(
    genre: String,
    viewModel: LibraryViewModel,
    actions: PlayActions,
    currentSongId: Long,
    sortMode: SortMode = SortMode.Default,
) {
    var songs by remember(genre) { mutableStateOf<List<Song>?>(null) }

    LaunchedEffect(genre) {
        viewModel.loadGenreSongs(genre) { songs = it }
    }

    val loaded = songs
    if (loaded == null) {
        LoadingBox()
    } else {
        val ordered = loaded.sortedFor(sortMode)
        SongListPage(
            songs = ordered,
            actions = actions,
            currentSongId = currentSongId,
            favorites = emptySet(),
            header = {
                DetailHeader(
                    title = genre,
                    subtitle = "Genre",
                    meta = "${ordered.map { it.artist }.distinct().size} artists",
                    countText = ordered.size.toString(),
                    artAlbumId = ordered.firstOrNull()?.albumId ?: 0L,
                    showDisc = true,
                    onPlayAll = { actions.play(ordered, 0) },
                    onShuffle = { actions.shuffleAll(ordered) },
                    onQueueAll = { actions.queueAll(ordered) },
                )
            },
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    name: String,
    viewModel: LibraryViewModel,
    actions: PlayActions,
    currentSongId: Long,
    canAddSongs: Boolean = false,
    onAddSongs: () -> Unit = {},
    sortMode: SortMode = SortMode.Default,
) {
    val context = LocalContext.current
    var songs by remember(playlistId) { mutableStateOf<List<Song>?>(null) }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistSongs(playlistId) { songs = it }
    }

    val loaded = songs
    if (loaded == null) {
        LoadingBox()
    } else {
        val ordered = loaded.sortedFor(sortMode)
        Box(Modifier.fillMaxSize()) {
            SongListPage(
                songs = ordered,
                actions = actions,
                currentSongId = currentSongId,
                favorites = emptySet(),
                header = {
                    DetailHeader(
                        title = name,
                        subtitle = "Playlist",
                        meta = "${ordered.map { it.artist }.distinct().size} artists",
                        countText = ordered.size.toString(),
                        artAlbumId = ordered.firstOrNull()?.albumId ?: 0L,
                        showDisc = true,
                        onPlayAll = { actions.play(ordered, 0) },
                        onShuffle = { actions.shuffleAll(ordered) },
                        onQueueAll = { actions.queueAll(ordered) },
                    )
                },
                onRemoveFromPlaylist = if (canAddSongs) {
                    { song ->
                        val removed = viewModel.removeSongsFromPlaylist(
                            playlistId = playlistId,
                            songIds = listOf(song.id),
                        )
                        if (removed > 0) {
                            songs = songs?.filterNot { it.id == song.id }
                            Toast.makeText(
                                context,
                                "Removed from playlist",
                                Toast.LENGTH_SHORT,
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Song was not in this playlist",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                } else {
                    null
                },
            )

            if (canAddSongs) {
                FloatingActionButton(
                    onClick = onAddSongs,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add songs or folder to this playlist",
                    )
                }
            }
        }
    }
}

@Composable
fun FolderDetailScreen(
    path: String,
    name: String,
    state: LibraryUiState,
    actions: PlayActions,
    currentSongId: Long,
    sortMode: SortMode = SortMode.Default,
) {
    val songs = state.songs
        .filter { it.folder == path }
        .sortedBy { it.title.lowercase() }
        .sortedFor(sortMode)

    SongListPage(
        songs = songs,
        actions = actions,
        currentSongId = currentSongId,
        favorites = state.favorites,
        showArtwork = true,
        header = {
            DetailHeader(
                title = name,
                subtitle = "Folder",
                meta = path,
                countText = songs.size.toString(),
                artAlbumId = 0L,
                showDisc = true,
                onPlayAll = { actions.play(songs, 0) },
                onShuffle = { actions.shuffleAll(songs) },
                onQueueAll = { actions.queueAll(songs) },
            )
        },
    )
}
