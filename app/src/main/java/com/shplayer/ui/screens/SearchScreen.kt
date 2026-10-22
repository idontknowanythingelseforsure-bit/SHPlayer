package com.shplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shplayer.data.Song
import com.shplayer.ui.LibraryUiState
import com.shplayer.ui.PlayActions
import com.shplayer.ui.components.EmptyState
import com.shplayer.ui.components.FastScroller
import com.shplayer.ui.components.PersonPlaceholder
import com.shplayer.ui.components.SectionHeader
import com.shplayer.ui.components.SongRow
import com.shplayer.ui.songMenuActions

@Composable
fun SearchScreen(
    state: LibraryUiState,
    actions: PlayActions,
    currentSongId: Long,
    onOpenArtist: (String) -> Unit,
    onResultsChanged: (List<Song>) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val songs = remember(query, state.songs) {
        if (query.isBlank()) {
            emptyList()
        } else {
            state.songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
            }.take(150)
        }
    }
    val artists = remember(query, state.artists) {
        if (query.isBlank()) {
            emptyList()
        } else {
            state.artists.filter { it.name.contains(query, ignoreCase = true) }.take(20)
        }
    }

    LaunchedEffect(songs) {
        onResultsChanged(songs)
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search songs, artists, albums") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        when {
            query.isBlank() -> EmptyState("Search your library")
            songs.isEmpty() && artists.isEmpty() -> EmptyState("No results for \"$query\"")
            else -> {
                val rowCount = songs.size + artists.size +
                    (if (songs.isNotEmpty()) 1 else 0) +
                    (if (artists.isNotEmpty()) 1 else 0)

                Box(Modifier.weight(1f)) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        if (artists.isNotEmpty()) {
                            item(key = "artists-header") { SectionHeader("Artists") }
                            items(artists, key = { "artist-${it.name}" }) { artist ->
                                ArtistResultRow(
                                    name = artist.name,
                                    subtitle = "${artist.songCount} songs · ${artist.albumCount} albums",
                                    onClick = { onOpenArtist(artist.name) },
                                )
                            }
                        }

                        if (songs.isNotEmpty()) {
                            item(key = "songs-header") { SectionHeader("Songs") }
                            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                                SongRow(
                                    song = song,
                                    isCurrent = song.id == currentSongId,
                                    showArtwork = false,
                                    isFavorite = song.id in state.favorites,
                                    actions = actions.songMenuActions(song, songs, index),
                                )
                            }
                        }

                        item(key = "search-bottom") { Spacer(Modifier.size(12.dp)) }
                    }

                    FastScroller(
                        listState = listState,
                        itemCount = rowCount,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(24.dp)
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistResultRow(
    name: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape),
        ) {
            PersonPlaceholder(Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 17.sp,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
