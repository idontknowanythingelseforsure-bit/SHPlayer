package com.shplayer.ui.screens

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
import androidx.compose.ui.unit.dp
import com.shplayer.data.Song
import com.shplayer.ui.LibraryUiState
import com.shplayer.ui.LibraryViewModel
import com.shplayer.ui.PlayActions
import com.shplayer.ui.SmartKind
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
                        actions = actions.songMenuActions(song, songs, index),
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
) {
    SongListPage(
        songs = state.songs,
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
) {
    val songs = smartSongs(kind, state)

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
) {
    val album = state.albums.firstOrNull { it.id == albumId }
    val songs = state.songs
        .filter { it.albumId == albumId }
        .sortedBy { it.trackNumber }

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
) {
    val songs = state.songs.filter { it.artist == artist }
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
) {
    var songs by remember(genre) { mutableStateOf<List<Song>?>(null) }

    LaunchedEffect(genre) {
        viewModel.loadGenreSongs(genre) { songs = it }
    }

    val loaded = songs
    if (loaded == null) {
        LoadingBox()
    } else {
        SongListPage(
            songs = loaded,
            actions = actions,
            currentSongId = currentSongId,
            favorites = emptySet(),
            header = {
                DetailHeader(
                    title = genre,
                    subtitle = "Genre",
                    meta = "${loaded.map { it.artist }.distinct().size} artists",
                    countText = loaded.size.toString(),
                    artAlbumId = loaded.firstOrNull()?.albumId ?: 0L,
                    showDisc = true,
                    onPlayAll = { actions.play(loaded, 0) },
                    onShuffle = { actions.shuffleAll(loaded) },
                    onQueueAll = { actions.queueAll(loaded) },
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
) {
    var songs by remember(playlistId) { mutableStateOf<List<Song>?>(null) }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistSongs(playlistId) { songs = it }
    }

    val loaded = songs
    if (loaded == null) {
        LoadingBox()
    } else {
        Box(Modifier.fillMaxSize()) {
            SongListPage(
                songs = loaded,
                actions = actions,
                currentSongId = currentSongId,
                favorites = emptySet(),
                header = {
                    DetailHeader(
                        title = name,
                        subtitle = "Playlist",
                        meta = "${loaded.map { it.artist }.distinct().size} artists",
                        countText = loaded.size.toString(),
                        artAlbumId = loaded.firstOrNull()?.albumId ?: 0L,
                        showDisc = true,
                        onPlayAll = { actions.play(loaded, 0) },
                        onShuffle = { actions.shuffleAll(loaded) },
                        onQueueAll = { actions.queueAll(loaded) },
                    )
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
) {
    val songs = state.songs
        .filter { it.folder == path }
        .sortedBy { it.title.lowercase() }

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
