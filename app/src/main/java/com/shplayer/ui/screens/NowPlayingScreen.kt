package com.shplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.shplayer.R
import com.shplayer.data.Song
import com.shplayer.playback.PlayerUiState
import com.shplayer.ui.components.Artwork
import com.shplayer.ui.components.BigPlayButton
import com.shplayer.ui.components.EmptyState
import com.shplayer.ui.components.formatDuration

@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    currentSong: Song?,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToFolder: (Song) -> Unit,
    onInfo: (Song) -> Unit,
    onShare: (Song) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.onBackground,
                )
            }
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Now playing",
                fontSize = 20.sp,
                color = colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    painter = painterResource(R.drawable.ic_eq_sliders),
                    contentDescription = "Sound effects",
                    tint = colors.primary,
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
                    val song = currentSong
                    DropdownMenuItem(
                        text = { Text("Go to album") },
                        enabled = song != null,
                        onClick = {
                            menuOpen = false
                            song?.let(onGoToAlbum)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Go to folder") },
                        enabled = song != null,
                        onClick = {
                            menuOpen = false
                            song?.let(onGoToFolder)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Song info") },
                        enabled = song != null,
                        onClick = {
                            menuOpen = false
                            song?.let(onInfo)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        enabled = song != null,
                        onClick = {
                            menuOpen = false
                            song?.let(onShare)
                        },
                    )
                }
            }
        }

        if (!state.hasTrack) {
            EmptyState("Nothing playing")
            return@Column
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .aspectRatio(1f)
                    .clip(CircleShape),
            ) {
                Artwork(albumId = state.albumId, modifier = Modifier.fillMaxSize())
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant)
                .height(62.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    painter = painterResource(R.drawable.ic_eq_sliders),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (state.queueSize > 0) {
                Text(
                    text = "${state.queueIndex + 1}/${state.queueSize}",
                    fontSize = 11.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    fontSize = 17.sp,
                    color = colors.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(state.artist.ifBlank { "Unknown artist" }, state.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" - "),
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant,
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = colors.onBackground,
                    modifier = Modifier.size(40.dp),
                )
            }
            BigPlayButton(isPlaying = state.isPlaying, onClick = onTogglePlay)
            IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = colors.onBackground,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = onCycleRepeat) {
                Icon(
                    imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) {
                        colors.onSurfaceVariant
                    } else {
                        colors.primary
                    },
                )
            }
        }

        val duration = state.durationMs.coerceAtLeast(1L)
        var dragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(state.songId) {
            dragging = false
            dragValue = 0f
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Slider(
                value = if (dragging) {
                    dragValue.coerceIn(0f, duration.toFloat())
                } else {
                    state.positionMs.coerceIn(0L, duration).toFloat()
                },
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    onSeek(dragValue.toLong())
                    dragging = false
                },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.outline,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(if (dragging) dragValue.toLong() else state.positionMs),
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = formatDuration(duration),
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        if (state.formatLine.isNotBlank()) {
            Text(
                text = state.formatLine,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
