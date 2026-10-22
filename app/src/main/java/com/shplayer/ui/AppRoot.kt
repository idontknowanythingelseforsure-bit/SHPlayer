package com.shplayer.ui

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shplayer.data.Song
import com.shplayer.playback.PlayerController
import com.shplayer.ui.components.AddSongsToPlaylistDialog
import com.shplayer.ui.components.AddToPlaylistDialog
import com.shplayer.ui.components.CreatePlaylistDialog
import com.shplayer.ui.components.IconLabelRow
import com.shplayer.ui.components.MiniPlayer
import com.shplayer.ui.components.SectionHeader
import com.shplayer.ui.components.SongInfoDialog
import com.shplayer.ui.screens.AlbumDetailScreen
import com.shplayer.ui.screens.AlbumsScreen
import com.shplayer.ui.screens.AllSongsScreen
import com.shplayer.ui.screens.ArtistDetailScreen
import com.shplayer.ui.screens.ArtistsScreen
import com.shplayer.ui.screens.EqualizerScreen
import com.shplayer.ui.screens.FolderDetailScreen
import com.shplayer.ui.screens.FolderPickerScreen
import com.shplayer.ui.screens.FoldersScreen
import com.shplayer.ui.screens.GenreDetailScreen
import com.shplayer.ui.screens.GenresScreen
import com.shplayer.ui.screens.HomeScreen
import com.shplayer.ui.screens.NowPlayingScreen
import com.shplayer.ui.screens.PlaylistDetailScreen
import com.shplayer.ui.screens.PlaylistsScreen
import com.shplayer.ui.screens.SearchScreen
import com.shplayer.ui.screens.SettingsScreen
import com.shplayer.ui.screens.SmartScreen
import com.shplayer.ui.screens.SongPickerScreen
import com.shplayer.ui.screens.smartSongs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppMenuAction { SoundEffects, Settings, SleepTimer, About }

@Composable
fun AppRoot(
    externalAudioUri: Uri? = null,
    onExternalAudioConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val libraryViewModel: LibraryViewModel = viewModel()
    val library by libraryViewModel.state.collectAsStateWithLifecycle()
    val playerState by PlayerController.state.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    val current = backStack.last()

    var infoSong by remember { mutableStateOf<Song?>(null) }
    var playlistSong by remember { mutableStateOf<Song?>(null) }
    var bulkPlaylistSongs by remember { mutableStateOf<List<Song>?>(null) }
    var addSongsDialog by remember { mutableStateOf(false) }
    var createPlaylistDialog by remember { mutableStateOf(false) }
    var sleepDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }

    val navigate: (Route) -> Unit = { route ->
        addSongsDialog = false
        // Re-scan when opening "Recently added" so songs that appeared on the
        // device since the last refresh show up without restarting the app.
        if (route is Route.Smart && route.kind == SmartKind.RecentlyAdded) {
            libraryViewModel.refresh()
        }
        backStack.add(route)
    }
    val goBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    val drawerNavigate: (Route) -> Unit = { route ->
        backStack.clear()
        backStack.add(Route.Home)
        if (route != Route.Home) backStack.add(route)
        if (route is Route.Smart && route.kind == SmartKind.RecentlyAdded) {
            libraryViewModel.refresh()
        }
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = backStack.size > 1) { goBack() }

    LaunchedEffect(Unit) {
        PlayerController.ensureConnected(context)
    }

    LaunchedEffect(playerState.songId) {
        if (playerState.songId > 0L) {
            libraryViewModel.recordPlay(playerState.songId)
        }
    }

    // Audio files opened from a file manager ("Open with" -> SH Player).
    LaunchedEffect(externalAudioUri) {
        val uri = externalAudioUri ?: return@LaunchedEffect
        val song = withContext(Dispatchers.IO) { songFromUri(context, uri) }
        if (song != null) {
            PlayerController.play(context, listOf(song), 0)
            if (backStack.last() != Route.NowPlaying) backStack.add(Route.NowPlaying)
        } else {
            Toast.makeText(context, "Unable to open this audio file", Toast.LENGTH_SHORT).show()
        }
        onExternalAudioConsumed()
    }

    val recentSongs = remember(library.songs, library.recentlyPlayed) {
        val songsById = library.songs.associateBy { it.id }
        library.recentlyPlayed.mapNotNull { songsById[it] }.take(10)
    }

    // The songs the current screen lists, so the top-bar "Add to playlist"
    // action can add the whole list (album, folder, smart list, ...).
    var searchSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var routeSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    LaunchedEffect(
        current,
        library.songs,
        library.recentlyPlayed,
        library.favorites,
        library.playCounts,
        library.localPlaylists,
        searchSongs,
    ) {
        routeSongs = when (val route = current) {
            Route.Songs -> library.songs
            is Route.Smart -> smartSongs(route.kind, library)
            is Route.AlbumDetail -> library.songs
                .filter { it.albumId == route.albumId }
                .sortedBy { it.trackNumber }
            is Route.ArtistDetail -> library.songs.filter { it.artist == route.artist }
            is Route.GenreDetail -> libraryViewModel.resolveGenreSongs(route.genre)
            is Route.PlaylistDetail -> libraryViewModel.resolvePlaylistSongs(route.playlistId)
            is Route.FolderDetail -> library.songs.filter { it.folder == route.path }
            Route.Search -> searchSongs
            else -> emptyList()
        }
    }

    val playActions = remember(context) {
        PlayActions(
            play = { songs, index -> if (songs.isNotEmpty()) PlayerController.play(context, songs, index) },
            playNext = { PlayerController.playNext(context, it) },
            enqueue = { PlayerController.addToQueue(context, it) },
            queueAll = { PlayerController.enqueue(context, it) },
            shuffleAll = { songs -> if (songs.isNotEmpty()) PlayerController.playQueue(context, songs, shuffle = true) },
            toggleFavorite = { song ->
                val added = libraryViewModel.toggleFavorite(song.id)
                Toast.makeText(
                    context,
                    if (added) "Added to favorites" else "Removed from favorites",
                    Toast.LENGTH_SHORT,
                ).show()
            },
            info = { song -> infoSong = song },
            share = { song -> shareSong(context, song) },
            addToPlaylist = { song -> playlistSong = song },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(304.dp),
            ) {
                DrawerContent(
                    library = library,
                    onNavigate = drawerNavigate,
                )
            }
        },
        gesturesEnabled = backStack.size == 1,
    ) {
        val showTopBar = current != Route.NowPlaying && current != Route.Equalizer
        val showMiniPlayer = current != Route.NowPlaying &&
            current != Route.Equalizer &&
            playerState.hasTrack

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (showTopBar) {
                    AppTopBar(
                        title = topBarTitle(current, library),
                        icon = topBarIcon(current),
                        canGoBack = backStack.size > 1,
                        onBack = goBack,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onSearch = { if (current != Route.Search) navigate(Route.Search) },
                        showAddToPlaylist = routeSongs.isNotEmpty(),
                        onAddToPlaylist = {
                            bulkPlaylistSongs = routeSongs
                        },
                        onMenuAction = { action ->
                            when (action) {
                                AppMenuAction.SoundEffects -> navigate(Route.Equalizer)
                                AppMenuAction.Settings -> navigate(Route.Settings)
                                AppMenuAction.SleepTimer -> sleepDialog = true
                                AppMenuAction.About -> aboutDialog = true
                            }
                        },
                    )
                }
            },
            bottomBar = {
                Column(Modifier.navigationBarsPadding()) {
                    if (showMiniPlayer) {
                        MiniPlayer(
                            state = playerState,
                            onToggle = { PlayerController.togglePlayPause() },
                            onOpen = { navigate(Route.NowPlaying) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(if (showTopBar) Modifier else Modifier.statusBarsPadding()),
            ) {
                when (val route = current) {
                    Route.Home -> HomeScreen(
                        state = library,
                        recentSongs = recentSongs,
                        actions = playActions,
                        currentSongId = playerState.songId,
                        onNavigate = navigate,
                    )

                    Route.Songs -> AllSongsScreen(
                        state = library,
                        actions = playActions,
                        currentSongId = playerState.songId,
                    )

                    is Route.Smart -> SmartScreen(
                        kind = route.kind,
                        state = library,
                        actions = playActions,
                        currentSongId = playerState.songId,
                    )

                    Route.Albums -> AlbumsScreen(
                        albums = library.albums,
                        onOpen = { navigate(Route.AlbumDetail(it.id)) },
                    )

                    is Route.AlbumDetail -> AlbumDetailScreen(
                        albumId = route.albumId,
                        state = library,
                        actions = playActions,
                        currentSongId = playerState.songId,
                    )

                    Route.Artists -> ArtistsScreen(
                        artists = library.artists,
                        songs = library.songs,
                        onOpen = { navigate(Route.ArtistDetail(it.name)) },
                    )

                    is Route.ArtistDetail -> ArtistDetailScreen(
                        artist = route.artist,
                        state = library,
                        actions = playActions,
                        currentSongId = playerState.songId,
                    )

                    Route.Genres -> GenresScreen(
                        genres = library.genres,
                        onOpen = { navigate(Route.GenreDetail(it.name)) },
                    )

                    is Route.GenreDetail -> GenreDetailScreen(
                        genre = route.genre,
                        viewModel = libraryViewModel,
                        actions = playActions,
                        currentSongId = playerState.songId,
                    )

                    Route.Playlists -> PlaylistsScreen(
                        playlists = library.allPlaylists,
                        onCreate = { createPlaylistDialog = true },
                        onOpen = { navigate(Route.PlaylistDetail(it.id, it.name)) },
                    )

                    is Route.PlaylistDetail -> PlaylistDetailScreen(
                        playlistId = route.playlistId,
                        name = route.name,
                        viewModel = libraryViewModel,
                        actions = playActions,
                        currentSongId = playerState.songId,
                        canAddSongs = route.playlistId < 0L,
                        onAddSongs = { addSongsDialog = true },
                    )

                    is Route.SongPicker -> SongPickerScreen(
                        songs = library.songs,
                        playlistName = route.playlistName,
                        onAdd = { picked ->
                            val added = libraryViewModel.addSongsToPlaylist(
                                route.playlistId,
                                picked.map { it.id },
                            )
                            Toast.makeText(
                                context,
                                when {
                                    added > 0 -> "Added $added songs to ${route.playlistName}"
                                    picked.isEmpty() -> "No songs selected"
                                    else -> "Those songs are already in ${route.playlistName}"
                                },
                                Toast.LENGTH_SHORT,
                            ).show()
                            goBack()
                        },
                    )

                    is Route.FolderPicker -> FolderPickerScreen(
                        folders = library.folders,
                        songs = library.songs,
                        playlistName = route.playlistName,
                        onPick = { folder ->
                            val ids = library.songs
                                .filter { it.folder == folder.path }
                                .map { it.id }
                            val added = libraryViewModel.addSongsToPlaylist(
                                route.playlistId,
                                ids,
                            )
                            Toast.makeText(
                                context,
                                when {
                                    added > 0 -> "Added $added songs to ${route.playlistName}"
                                    ids.isEmpty() -> "This folder has no songs"
                                    else -> "Those songs are already in ${route.playlistName}"
                                },
                                Toast.LENGTH_SHORT,
                            ).show()
                            goBack()
                        },
                    )

                    Route.Folders -> FoldersScreen(
                        folders = library.folders,
                        songs = library.songs,
                        folderRoots = library.folderRoots,
                        onAddFolderRoot = { libraryViewModel.addFolderRoot(it) },
                        onClearFolderRoots = { libraryViewModel.clearFolderRoots() },
                        onOpen = { navigate(Route.FolderDetail(it.path, it.name)) },
                    )

                    is Route.FolderDetail -> FolderDetailScreen(
                        path = route.path,
                        name = route.name,
                        state = library,
                        actions = playActions,
                        currentSongId = playerState.songId,
                    )

                    Route.Search -> SearchScreen(
                        state = library,
                        actions = playActions,
                        currentSongId = playerState.songId,
                        onOpenArtist = { navigate(Route.ArtistDetail(it)) },
                        onResultsChanged = { searchSongs = it },
                    )

                    Route.NowPlaying -> NowPlayingScreen(
                        state = playerState,
                        currentSong = library.songs.firstOrNull { it.id == playerState.songId },
                        onBack = goBack,
                        onTogglePlay = { PlayerController.togglePlayPause() },
                        onNext = { PlayerController.next() },
                        onPrevious = { PlayerController.previous() },
                        onSeek = { PlayerController.seekTo(it) },
                        onToggleShuffle = { PlayerController.toggleShuffle() },
                        onCycleRepeat = { PlayerController.cycleRepeatMode() },
                        onOpenEqualizer = { navigate(Route.Equalizer) },
                        onGoToAlbum = { song ->
                            if (song.albumId > 0L) navigate(Route.AlbumDetail(song.albumId))
                        },
                        onGoToFolder = { song ->
                            if (song.folder.isNotBlank()) {
                                navigate(Route.FolderDetail(song.folder, song.folder.substringAfterLast('/')))
                            }
                        },
                        onInfo = { infoSong = it },
                        onShare = { shareSong(context, it) },
                    )

                    Route.Equalizer -> EqualizerScreen(onBack = goBack)

                    Route.Settings -> SettingsScreen()
                }
            }
        }
    }

    infoSong?.let { song ->
        SongInfoDialog(song = song, onDismiss = { infoSong = null })
    }

    playlistSong?.let { song ->
        AddToPlaylistDialog(
            itemDescription = song.title,
            playlists = library.localPlaylists,
            onPick = { playlistId ->
                val added = libraryViewModel.addSongToPlaylist(playlistId, song.id)
                Toast.makeText(
                    context,
                    if (added) "Added to playlist" else "Playlist not found",
                    Toast.LENGTH_SHORT,
                ).show()
                playlistSong = null
            },
            onCreate = { name ->
                val playlistId = libraryViewModel.createPlaylist(name)
                libraryViewModel.addSongToPlaylist(playlistId, song.id)
                Toast.makeText(context, "Playlist created", Toast.LENGTH_SHORT).show()
                playlistSong = null
            },
            onDismiss = { playlistSong = null },
        )
    }

    bulkPlaylistSongs?.let { songs ->
        AddToPlaylistDialog(
            itemDescription = "${songs.size} songs",
            playlists = library.localPlaylists,
            onPick = { playlistId ->
                val added = libraryViewModel.addSongsToPlaylist(playlistId, songs.map { it.id })
                Toast.makeText(
                    context,
                    if (added > 0) "Added $added songs" else "Already in playlist",
                    Toast.LENGTH_SHORT,
                ).show()
                bulkPlaylistSongs = null
            },
            onCreate = { name ->
                val playlistId = libraryViewModel.createPlaylist(name)
                val added = libraryViewModel.addSongsToPlaylist(playlistId, songs.map { it.id })
                Toast.makeText(
                    context,
                    "Playlist created · $added songs added",
                    Toast.LENGTH_SHORT,
                ).show()
                bulkPlaylistSongs = null
            },
            onDismiss = { bulkPlaylistSongs = null },
        )
    }

    if (addSongsDialog) {
        val playlist = current as? Route.PlaylistDetail
        if (playlist != null) {
            AddSongsToPlaylistDialog(
                playlistName = playlist.name,
                onAddSongs = {
                    navigate(Route.SongPicker(playlist.playlistId, playlist.name))
                },
                onAddFolder = {
                    navigate(Route.FolderPicker(playlist.playlistId, playlist.name))
                },
                onDismiss = { addSongsDialog = false },
            )
        }
    }

    if (createPlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { createPlaylistDialog = false },
            onCreate = { name ->
                libraryViewModel.createPlaylist(name)
                Toast.makeText(context, "Playlist created", Toast.LENGTH_SHORT).show()
                createPlaylistDialog = false
            },
        )
    }

    if (sleepDialog) {
        SleepTimerDialog(
            onDismiss = { sleepDialog = false },
            onSelect = { minutes ->
                PlayerController.startSleepTimer(minutes)
                sleepDialog = false
                Toast.makeText(
                    context,
                    if (minutes == 0) "Sleep timer off" else "Sleep timer set",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
    }

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            title = { Text("SH Player") },
            text = {
                Text(
                    "A PlayerPro-inspired music player: black & orange interface, " +
                        "recently-played home, flat song lists, disc artwork, " +
                        "playlist fillers and a draggable fast-scroll slider.",
                )
            },
            confirmButton = {
                TextButton(onClick = { aboutDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

@Composable
private fun DrawerContent(
    library: LibraryUiState,
    onNavigate: (Route) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Route.Home) }
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "SH Player",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
            )
        }

        SectionHeader("Favorites")
        IconLabelRow(
            icon = Icons.Rounded.PlayCircle,
            label = SmartKind.RecentlyPlayed.title,
            count = library.recentlyPlayed.size.toString(),
            onClick = { onNavigate(Route.Smart(SmartKind.RecentlyPlayed)) },
        )
        IconLabelRow(
            icon = Icons.Rounded.History,
            label = SmartKind.RecentlyAdded.title,
            count = library.songs.size.toString(),
            onClick = { onNavigate(Route.Smart(SmartKind.RecentlyAdded)) },
        )
        IconLabelRow(
            icon = Icons.Rounded.Star,
            label = SmartKind.TopRated.title,
            count = library.favorites.size.toString(),
            onClick = { onNavigate(Route.Smart(SmartKind.TopRated)) },
        )
        IconLabelRow(
            icon = Icons.Rounded.TrendingUp,
            label = SmartKind.MostPlayed.title,
            count = library.playCounts.size.toString(),
            onClick = { onNavigate(Route.Smart(SmartKind.MostPlayed)) },
        )

        SectionHeader("Browsers")
        IconLabelRow(
            icon = Icons.Rounded.LibraryMusic,
            label = "Songs",
            count = library.songs.size.toString(),
            onClick = { onNavigate(Route.Songs) },
        )
        IconLabelRow(
            icon = Icons.Rounded.Album,
            label = "Albums",
            count = library.albums.size.toString(),
            onClick = { onNavigate(Route.Albums) },
        )
        IconLabelRow(
            icon = Icons.Rounded.Person,
            label = "Artists",
            count = library.artists.size.toString(),
            onClick = { onNavigate(Route.Artists) },
        )
        IconLabelRow(
            icon = Icons.Rounded.Category,
            label = "Genres",
            count = library.genres.size.toString(),
            onClick = { onNavigate(Route.Genres) },
        )
        IconLabelRow(
            icon = Icons.Rounded.QueueMusic,
            label = "Playlists",
            count = library.allPlaylists.size.toString(),
            onClick = { onNavigate(Route.Playlists) },
        )
        IconLabelRow(
            icon = Icons.Rounded.Folder,
            label = "Folders",
            count = library.folders.size.toString(),
            onClick = { onNavigate(Route.Folders) },
        )

        HorizontalDivider(color = colors.outline, thickness = 1.dp)
        IconLabelRow(
            icon = Icons.Rounded.Settings,
            label = "Settings",
            count = "",
            onClick = { onNavigate(Route.Settings) },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AppTopBar(
    title: String,
    icon: ImageVector,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
    onSearch: () -> Unit,
    showAddToPlaylist: Boolean,
    onAddToPlaylist: () -> Unit,
    onMenuAction: (AppMenuAction) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = if (canGoBack) onBack else onOpenDrawer) {
            Icon(
                imageVector = if (canGoBack) Icons.Rounded.ArrowBack else Icons.Rounded.Menu,
                contentDescription = "Navigation",
                tint = colors.onBackground,
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = colors.onBackground,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = colors.onBackground,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (showAddToPlaylist) {
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = colors.primary,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onAddToPlaylist()
                        },
                    )
                    HorizontalDivider(color = colors.outline, thickness = 0.5.dp)
                }
                DropdownMenuItem(
                    text = { Text("Sound effects") },
                    onClick = {
                        menuOpen = false
                        onMenuAction(AppMenuAction.SoundEffects)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        menuOpen = false
                        onMenuAction(AppMenuAction.Settings)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Sleep timer") },
                    onClick = {
                        menuOpen = false
                        onMenuAction(AppMenuAction.SleepTimer)
                    },
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        menuOpen = false
                        onMenuAction(AppMenuAction.About)
                    },
                )
            }
        }
    }
    HorizontalDivider(color = colors.outline, thickness = 1.dp)
}

@Composable
private fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                listOf(
                    0 to "Off",
                    15 to "15 minutes",
                    30 to "30 minutes",
                    45 to "45 minutes",
                    60 to "1 hour",
                    90 to "1 hour 30",
                ).forEach { (minutes, label) ->
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        color = colors.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.primary)
            }
        },
    )
}

private fun topBarTitle(route: Route, library: LibraryUiState): String = when (route) {
    Route.Home -> ""
    is Route.AlbumDetail -> library.albums.firstOrNull { it.id == route.albumId }?.title ?: "Album"
    else -> route.title()
}

private fun topBarIcon(route: Route): ImageVector = when (route) {
    Route.Home, Route.Songs, Route.Search -> Icons.Rounded.LibraryMusic
    Route.Albums, is Route.AlbumDetail -> Icons.Rounded.Album
    Route.Artists, is Route.ArtistDetail -> Icons.Rounded.Person
    Route.Genres, is Route.GenreDetail -> Icons.Rounded.Category
    Route.Playlists, is Route.PlaylistDetail -> Icons.Rounded.QueueMusic
    Route.Folders, is Route.FolderDetail -> Icons.Rounded.Folder
    is Route.SongPicker, is Route.FolderPicker -> Icons.AutoMirrored.Rounded.PlaylistAdd
    Route.Settings -> Icons.Rounded.Settings
    is Route.Smart -> Icons.Rounded.Star
    else -> Icons.Rounded.MusicNote
}

private fun shareSong(context: Context, song: Song) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(song.uri))
        putExtra(Intent.EXTRA_SUBJECT, song.title)
        putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Share song"))
    }
}

/**
 * Turns an audio document opened from a file manager into a playable [Song].
 * MediaStore metadata is used when the uri points at a library track; raw
 * documents fall back to MediaMetadataRetriever so any audio file can play.
 */
private fun songFromUri(context: Context, uri: Uri): Song? {
    val fallbackTitle = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() }
        ?: "Audio file"
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        Song(
            id = 0L,
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fallbackTitle,
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Unknown artist",
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
            albumId = 0L,
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L,
            uri = uri.toString(),
            trackNumber = 0,
            year = 0,
            dateAdded = 0L,
            genre = "",
            folder = "",
        )
    } catch (t: Throwable) {
        Song(
            id = 0L,
            title = fallbackTitle,
            artist = "Unknown artist",
            album = "",
            albumId = 0L,
            durationMs = 0L,
            uri = uri.toString(),
            trackNumber = 0,
            year = 0,
            dateAdded = 0L,
            genre = "",
            folder = "",
        )
    } finally {
        runCatching { retriever.release() }
    }
}
