package com.shplayer.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shplayer.data.Album
import com.shplayer.data.Artist
import com.shplayer.data.Folder
import com.shplayer.data.Genre
import com.shplayer.data.Playlist
import com.shplayer.data.Song
import com.shplayer.ui.components.Artwork
import com.shplayer.ui.components.CdDisc
import com.shplayer.ui.components.EmptyState
import com.shplayer.ui.components.FastScrollerGrid
import com.shplayer.ui.components.PersonPlaceholder
import com.shplayer.ui.components.formatLongDuration

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    onOpen: (Album) -> Unit,
) {
    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize()) {
        if (albums.isEmpty()) {
            EmptyState("No albums found")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = albums.size, key = { albums[it].id }) { index ->
                    val album = albums[index]
                    AlbumTile(album = album, onClick = { onOpen(album) })
                }
            }
            FastScrollerGrid(
                gridState = gridState,
                itemCount = albums.size,
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
private fun AlbumTile(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(2.dp)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Artwork(albumId = album.id, modifier = Modifier.fillMaxSize())
            TileCaption(title = album.title)
        }
    }
}

@Composable
private fun BoxScope.TileCaption(title: String) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color(0xCC0F0F0F))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color(0xFFEDEDED),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun ArtistsScreen(
    artists: List<Artist>,
    songs: List<Song>,
    onOpen: (Artist) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val artistArt = remember(songs) {
        songs.filter { it.albumId > 0 }
            .groupBy { it.artist }
            .mapValues { entry -> entry.value.first().albumId }
    }

    Box(Modifier.fillMaxSize()) {
        if (artists.isEmpty()) {
            EmptyState("No artists found")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = artists.size, key = { artists[it].name }) { index ->
                    val artist = artists[index]
                    Column(
                        modifier = Modifier
                            .padding(2.dp)
                            .clickable { onOpen(artist) },
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                            val art = artistArt[artist.name] ?: 0L
                            if (art > 0L) {
                                Artwork(albumId = art, modifier = Modifier.fillMaxSize())
                            } else {
                                PersonPlaceholder(Modifier.fillMaxSize())
                            }
                            TileCaption(title = artist.name)
                        }
                    }
                }
            }
            FastScrollerGrid(
                gridState = gridState,
                itemCount = artists.size,
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
fun GenresScreen(
    genres: List<Genre>,
    onOpen: (Genre) -> Unit,
) {
    val gridState = rememberLazyGridState()

    Box(Modifier.fillMaxSize()) {
        if (genres.isEmpty()) {
            EmptyState("No genres found")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(count = genres.size, key = { genres[it].name }) { index ->
                    val genre = genres[index]
                    Column(
                        modifier = Modifier
                            .padding(2.dp)
                            .clickable { onOpen(genre) },
                    ) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                            CdDisc(Modifier.fillMaxSize())
                            TileCaption(title = genre.name)
                        }
                    }
                }
            }
            FastScrollerGrid(
                gridState = gridState,
                itemCount = genres.size,
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
fun PlaylistsScreen(
    playlists: List<Playlist>,
    onCreate: () -> Unit,
    onOpen: (Playlist) -> Unit,
    onRename: (Playlist) -> Unit = {},
    onDelete: (Playlist) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "create-playlist") {
                CreatePlaylistRow(onClick = onCreate)
            }

            if (playlists.isEmpty()) {
                item(key = "empty-playlists") {
                    Text(
                        text = "No playlists yet — create one to start collecting songs.",
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    )
                }
            } else {
                items(count = playlists.size, key = { playlists[it].id }) { index ->
                    val playlist = playlists[index]
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onOpen(playlist) },
                        onRename = { onRename(playlist) },
                        onDelete = { onDelete(playlist) },
                    )
                }
            }

            item(key = "playlist-bottom-space") { Spacer(Modifier.height(88.dp)) }
        }

        FloatingActionButton(
            onClick = onCreate,
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Create playlist",
            )
        }
    }
}

@Composable
private fun CreatePlaylistRow(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddBox,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Create New Playlist",
                    fontSize = 18.sp,
                    color = colors.onBackground,
                )
                Text(
                    text = "Tap to build a custom playlist",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalDivider(color = colors.outline, thickness = 1.dp)
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val editable = playlist.id < 0L

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.QueueMusic,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    fontSize = 18.sp,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${playlist.songCount} songs",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            if (editable) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Playlist options",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit playlist name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove playlist") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = colors.error,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalDivider(color = colors.outline, thickness = 1.dp)
    }
}

@Composable
fun FoldersScreen(
    folders: List<Folder>,
    songs: List<Song>,
    folderRoots: Set<String>,
    onAddFolderRoot: (String) -> Unit,
    onClearFolderRoots: () -> Unit,
    onOpen: (Folder) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var showAll by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            treeUriToPath(uri)?.let(onAddFolderRoot)
        }
    }

    val visible = remember(folders, folderRoots, showAll) {
        if (folderRoots.isEmpty() || showAll) {
            folders
        } else {
            folders.filter { folder ->
                folderRoots.any { root ->
                    folder.path == root || folder.path.startsWith("$root/")
                }
            }
        }
    }

    val durations = remember(songs) {
        songs.groupBy { it.folder }.mapValues { entry -> entry.value.sumOf { it.durationMs } }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "add-folder") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { picker.launch(null) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CreateNewFolder,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Add folder from file manager",
                        fontSize = 18.sp,
                        color = colors.onBackground,
                    )
                    Text(
                        text = "Pick a folder to show only its music",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            HorizontalDivider(color = colors.outline, thickness = 1.dp)
        }

        if (folderRoots.isNotEmpty()) {
            item(key = "folder-filter") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (showAll || folderRoots.isEmpty()) {
                            "Showing all folders"
                        } else {
                            "Showing selected folders"
                        },
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showAll = !showAll }) {
                        Text(
                            text = if (showAll) "Selected only" else "Show all",
                            color = colors.primary,
                            fontSize = 13.sp,
                        )
                    }
                    TextButton(onClick = onClearFolderRoots) {
                        Text("Clear", color = colors.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = colors.outline, thickness = 1.dp)
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty-folders") {
                Text(
                    text = if (folders.isEmpty()) {
                        "No folders found with audio."
                    } else {
                        "No audio folders inside the selected locations. Pick another folder or tap Show all."
                    },
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }
        } else {
            items(count = visible.size, key = { visible[it].path }) { index ->
                val folder = visible[index]
                FolderRow(
                    folder = folder,
                    durationMs = durations[folder.path] ?: 0L,
                    onClick = { onOpen(folder) },
                )
            }
        }
    }
}

/**
 * Converts a SAF tree uri (`primary:Music`) to the filesystem path MediaStore
 * uses in its DATA column (`/storage/emulated/0/Music`).
 */
private fun treeUriToPath(uri: Uri): String? = runCatching {
    val documentId = DocumentsContract.getTreeDocumentId(uri)
    val parts = documentId.split(':', limit = 2)
    val volume = parts[0]
    val relative = parts.getOrElse(1) { "" }.trim('/')
    val base = if (volume.equals("primary", ignoreCase = true)) {
        Environment.getExternalStorageDirectory().absolutePath
    } else {
        "/storage/$volume"
    }
    if (relative.isBlank()) base else "$base/$relative"
}.getOrNull()

@Composable
private fun FolderRow(folder: Folder, durationMs: Long, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontSize = 18.sp,
                    color = colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${folder.songCount} songs · ${formatLongDuration(durationMs)}",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        HorizontalDivider(color = colors.outline, thickness = 1.dp)
    }
}
