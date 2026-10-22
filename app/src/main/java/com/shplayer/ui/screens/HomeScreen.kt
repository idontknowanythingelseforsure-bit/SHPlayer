package com.shplayer.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shplayer.data.Song
import com.shplayer.ui.LibraryUiState
import com.shplayer.ui.PlayActions
import com.shplayer.ui.Route
import com.shplayer.ui.SmartKind
import com.shplayer.ui.components.SectionHeader
import com.shplayer.ui.components.SongRow
import com.shplayer.ui.songMenuActions

/**
 * Home keeps the library simple: a "Recently added" shortcut opens the full
 * smart list (every auto-scanned song, newest first), followed by the recently
 * played list and a "See all" link. Every other browser and smart list lives in
 * the navigation drawer.
 */
@Composable
fun HomeScreen(
    state: LibraryUiState,
    recentSongs: List<Song>,
    actions: PlayActions,
    currentSongId: Long,
    onNavigate: (Route) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "SH Player",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = "Music player",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(9.dp))
                .border(
                    width = 1.dp,
                    color = colors.primary.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(9.dp),
                )
                .clickable { onNavigate(Route.Smart(SmartKind.RecentlyAdded)) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Recently added",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Show all recently added songs",
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        SectionHeader("Recently played")
        if (recentSongs.isEmpty()) {
            Text(
                text = "Songs you play will appear here. Browse the menu to open your library.",
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        } else {
            recentSongs.forEachIndexed { index, song ->
                SongRow(
                    song = song,
                    isCurrent = song.id == currentSongId,
                    showArtwork = true,
                    isFavorite = song.id in state.favorites,
                    actions = actions.songMenuActions(song, recentSongs, index),
                )
            }
            TextButton(
                onClick = { onNavigate(Route.Smart(SmartKind.RecentlyPlayed)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text("See all", color = colors.primary, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.size(16.dp))
    }
}
