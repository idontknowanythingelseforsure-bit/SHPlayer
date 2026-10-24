package com.shplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shplayer.data.Folder
import com.shplayer.data.Song
import com.shplayer.ui.LibraryViewModel
import com.shplayer.ui.components.EmptyState
import com.shplayer.ui.components.formatDuration
import com.shplayer.ui.components.formatLongDuration

/**
 * Multi-select song list used to fill a local playlist. Selection is kept by
 * id so it survives filtering; "Select all" only selects the visible matches.
 */
@Composable
fun SongPickerScreen(
    songs: List<Song>,
    playlistName: String,
    onAdd: (List<Song>) -> Unit,
    confirmText: (Int) -> String = { count ->
        if (count == 0) {
            "Select songs to add to $playlistName"
        } else {
            "Add $count songs to $playlistName"
        }
    },
) {
    val colors = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val filtered = remember(songs, query) {
        if (query.isBlank()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Search songs") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { selected = selected + filtered.map { it.id } },
                enabled = filtered.isNotEmpty(),
            ) {
                Text("Select all", color = colors.primary, fontSize = 13.sp)
            }
            TextButton(
                onClick = { selected = emptySet() },
                enabled = selected.isNotEmpty(),
            ) {
                Text("Clear", color = colors.onSurfaceVariant, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${selected.size} selected",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
        }

        HorizontalDivider(color = colors.outline, thickness = 1.dp)

        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "No songs match \"$query\"",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(filtered, key = { it.id }) { song ->
                    val checked = song.id in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (checked) {
                                    selected - song.id
                                } else {
                                    selected + song.id
                                }
                            }
                            .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 16.sp,
                                color = colors.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artist,
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = formatDuration(song.durationMs),
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = colors.outline, thickness = 0.5.dp)
                }
            }
        }

        Button(
            onClick = { onAdd(songs.filter { it.id in selected }) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
        ) {
            Text(
                text = confirmText(selected.size),
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Multi-select list of the songs already in a local playlist. The caller
 * receives the selected songs and performs the removal.
 */
@Composable
fun RemoveSongsScreen(
    playlistId: Long,
    playlistName: String,
    viewModel: LibraryViewModel,
    onRemoved: (Int) -> Unit,
) {
    var songs by remember(playlistId) { mutableStateOf<List<Song>?>(null) }

    LaunchedEffect(playlistId) {
        songs = viewModel.resolvePlaylistSongs(playlistId)
    }

    val loaded = songs
    when {
        loaded == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        loaded.isEmpty() -> EmptyState("This playlist has no songs")
        else -> SongPickerScreen(
            songs = loaded,
            playlistName = playlistName,
            onAdd = { picked ->
                onRemoved(
                    viewModel.removeSongsFromPlaylist(
                        playlistId = playlistId,
                        songIds = picked.map { it.id },
                    ),
                )
            },
            confirmText = { count ->
                if (count == 0) {
                    "Select songs to remove"
                } else {
                    "Remove $count songs from \"$playlistName\""
                }
            },
        )
    }
}

/** Shows every folder; tapping one adds all of its songs to the playlist. */
@Composable
fun FolderPickerScreen(
    folders: List<Folder>,
    songs: List<Song>,
    playlistName: String,
    onPick: (Folder) -> Unit,
) {
    if (folders.isEmpty()) {
        EmptyState("No folders found")
        return
    }

    val colors = MaterialTheme.colorScheme
    val durationsByFolder = remember(songs) {
        songs.groupBy { it.folder }.mapValues { entry -> entry.value.sumOf { it.durationMs } }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = "Tap a folder to add all of its songs to \"$playlistName\".",
            fontSize = 13.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        HorizontalDivider(color = colors.outline, thickness = 1.dp)

        LazyColumn(Modifier.fillMaxSize()) {
            items(folders, key = { it.path }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(folder) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            fontSize = 16.sp,
                            color = colors.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${folder.songCount} songs · " +
                                formatLongDuration(durationsByFolder[folder.path] ?: 0L),
                            fontSize = 12.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add folder",
                        tint = colors.primary,
                    )
                }
                HorizontalDivider(color = colors.outline, thickness = 0.5.dp)
            }
        }
    }
}
