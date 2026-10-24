package com.shplayer.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shplayer.data.ArtworkLoader
import com.shplayer.data.Song
import com.shplayer.playback.PlayerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

fun formatLongDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

/**
 * The default "no artwork" disc, drawn the way PlayerPro renders its CD skin.
 * Used for every song/album without embedded art.
 */
@Composable
fun CdDisc(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = Color(0xFF272727), radius = radius, center = center)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF0F0F0),
                    Color(0xFFC4C4C4),
                    Color(0xFF929292),
                    Color(0xFFDCDCDC),
                    Color(0xFF9E9E9E),
                ),
                start = Offset(center.x - radius, center.y - radius),
                end = Offset(center.x + radius, center.y + radius),
            ),
            radius = radius * 0.98f,
            center = center,
        )

        // Grooves
        for (i in 1..7) {
            drawCircle(
                color = Color(0x14000000),
                radius = radius * (0.42f + i * 0.075f),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Hub
        drawCircle(color = Color(0xFF7E7E7E), radius = radius * 0.40f, center = center)
        drawCircle(color = Color(0xFFA6A6A6), radius = radius * 0.36f, center = center)
        drawCircle(color = Color(0xFF565656), radius = radius * 0.20f, center = center)
        drawCircle(color = Color(0xFF141414), radius = radius * 0.075f, center = center)
    }
}

@Composable
fun Artwork(
    albumId: Long,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
) {
    val context = LocalContext.current
    var bitmap by remember(albumId) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(albumId) {
        bitmap = withContext(Dispatchers.IO) {
            ArtworkLoader.load(context, albumId, 512)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CdDiskPlaceholder()
        }
    }
}

@Composable
private fun CdDiskPlaceholder() {
    CdDisc(Modifier.fillMaxSize())
}

@Composable
fun PersonPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(Color(0xFF3A3A3A), Color(0xFF2C2C2C)),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color(0xFF6E6E6E),
                radius = w * 0.16f,
                center = Offset(w / 2f, h * 0.36f),
            )
            drawOval(
                color = Color(0xFF6E6E6E),
                topLeft = Offset(w * 0.22f, h * 0.56f),
                size = Size(w * 0.56f, h * 0.5f),
            )
        }
    }
}

@Composable
fun CountPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 13.sp,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

data class SongMenuActions(
    val onPlay: () -> Unit,
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onInfo: () -> Unit,
    val onShare: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    /** Set only while the row is shown inside an editable playlist. */
    val onRemoveFromPlaylist: (() -> Unit)? = null,
)

@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    showArtwork: Boolean,
    isFavorite: Boolean,
    actions: SongMenuActions,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { actions.onPlay() }
                .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showArtwork) {
                Artwork(albumId = song.albumId, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrent) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = song.title,
                        fontSize = 18.sp,
                        color = if (isCurrent) colors.primary else colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = formatDuration(song.durationMs),
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = { menuOpen = false; actions.onPlay() },
                    )
                    DropdownMenuItem(
                        text = { Text("Play next") },
                        onClick = { menuOpen = false; actions.onPlayNext() },
                    )
                    DropdownMenuItem(
                        text = { Text("Enqueue") },
                        onClick = { menuOpen = false; actions.onAddToQueue() },
                    )
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
                        onClick = { menuOpen = false; actions.onToggleFavorite() },
                    )
                    DropdownMenuItem(
                        text = { Text("Add to playlist") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = null,
                            )
                        },
                        onClick = { menuOpen = false; actions.onAddToPlaylist() },
                    )
                    actions.onRemoveFromPlaylist?.let { removeFromPlaylist ->
                        DropdownMenuItem(
                            text = { Text("Remove from playlist") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = colors.error,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                removeFromPlaylist()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Song info") },
                        onClick = { menuOpen = false; actions.onInfo() },
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { menuOpen = false; actions.onShare() },
                    )
                }
            }
        }
        HorizontalDivider(
            color = colors.outline,
            thickness = 1.dp,
        )
    }
}

/**
 * PlayerPro-style draggable scroll thumb (the "slider button" on the right edge).
 * Drag or tap anywhere along the track to jump through the list.
 */
@Composable
fun FastScrollBar(
    itemCount: Int,
    firstVisibleIndex: Int,
    onScrollToIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (itemCount < 6) return

    var trackHeight by remember { mutableStateOf(1) }
    val density = LocalDensity.current
    val thumbHeightPx = with(density) { 46.dp.toPx() }

    val fraction = if (itemCount <= 1) 0f else (firstVisibleIndex.toFloat() / (itemCount - 1)).coerceIn(0f, 1f)

    fun scrollTo(y: Float) {
        val travel = (trackHeight - thumbHeightPx).coerceAtLeast(1f)
        val target = ((y - thumbHeightPx / 2f) / travel).coerceIn(0f, 1f)
        val index = (target * (itemCount - 1)).roundToInt()
        onScrollToIndex(index)
    }

    Box(modifier.onSizeChanged { trackHeight = it.height }) {
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width / 2f
            val thumbHeight = 46.dp.toPx().coerceAtMost(size.height)
            val travel = (size.height - thumbHeight).coerceAtLeast(1f)
            val thumbTop = fraction * travel

            drawRoundRect(
                color = Color(0x33FFFFFF),
                topLeft = Offset(x - 1.dp.toPx(), 0f),
                size = Size(2.dp.toPx(), size.height),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
            drawRoundRect(
                color = ShAccent,
                topLeft = Offset(x - 2.5.dp.toPx(), thumbTop),
                size = Size(5.dp.toPx(), thumbHeight),
                cornerRadius = CornerRadius(2.5.dp.toPx()),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(itemCount) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> scrollTo(offset.y) },
                    ) { change, _ ->
                        scrollTo(change.position.y)
                    }
                }
                .pointerInput(itemCount) {
                    detectTapGestures { offset -> scrollTo(offset.y) }
                },
        )
    }
}

@Composable
fun FastScroller(
    listState: LazyListState,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    FastScrollBar(
        itemCount = itemCount,
        firstVisibleIndex = listState.firstVisibleItemIndex,
        onScrollToIndex = { index -> scope.launch { listState.scrollToItem(index) } },
        modifier = modifier,
    )
}

@Composable
fun FastScrollerGrid(
    gridState: LazyGridState,
    itemCount: Int,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    FastScrollBar(
        itemCount = itemCount,
        firstVisibleIndex = gridState.firstVisibleItemIndex,
        onScrollToIndex = { index -> scope.launch { gridState.scrollToItem(index) } },
        modifier = modifier,
    )
}

private val ShAccent = Color(0xFFFF7A18)

@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    if (!state.hasTrack) return
    val colors = MaterialTheme.colorScheme

    Surface(color = colors.surfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onOpen)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(albumId = state.albumId, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    fontSize = 17.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.artist.ifBlank { "Unknown artist" },
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggle, modifier = Modifier.size(52.dp)) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
fun BigPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFD9D9D9), Color(0xFFB0B0B0)),
                ),
            )
            .border(3.dp, accent, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(46.dp),
        )
    }
}

/** Header block used by playlist / album / folder detail screens. */
@Composable
fun DetailHeader(
    title: String,
    subtitle: String,
    meta: String,
    countText: String,
    artAlbumId: Long,
    showDisc: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onQueueAll: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(84.dp)) {
                if (showDisc || artAlbumId <= 0L) {
                    CdDisc(Modifier.fillMaxSize())
                } else {
                    Artwork(albumId = artAlbumId, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (countText.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                CountPill(countText)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPlayAll, modifier = Modifier.weight(1f)) {
                Text("Play all", color = colors.onSurfaceVariant, fontSize = 15.sp)
            }
            Button(
                onClick = onShuffle,
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                shape = RoundedCornerShape(2.dp),
            ) {
                Text("Shuffle", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            TextButton(onClick = onQueueAll, modifier = Modifier.weight(1f)) {
                Text("Queue all", color = colors.onSurfaceVariant, fontSize = 15.sp)
            }
        }

        HorizontalDivider(color = colors.outline, thickness = 1.dp)
    }
}

@Composable
fun IconLabelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(18.dp))
        Text(
            text = label,
            fontSize = 18.sp,
            color = colors.onBackground,
            modifier = Modifier.weight(1f),
        )
        if (count.isNotBlank()) {
            Text(
                text = count,
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 11.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun FavoriteMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text("Favorites") },
        leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
fun QueueMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text("Queue") },
        leadingIcon = { Icon(Icons.Rounded.QueuePlayNext, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
fun ShareMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text("Share") },
        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
fun InfoMenuItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text("Info") },
        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        onClick = onClick,
    )
}
