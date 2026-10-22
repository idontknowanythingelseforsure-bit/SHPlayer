package com.shplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shplayer.data.Prefs
import com.shplayer.playback.PlayerController
import com.shplayer.ui.components.formatDuration
import com.shplayer.ui.theme.ShPlayerTheme
import kotlinx.coroutines.delay

/**
 * Compact seek sheet opened by tapping the widget's progress line.
 *
 * AppWidget `RemoteViews` cannot receive drag gestures, so this transparent
 * activity is the only way to offer a real draggable handle over the launcher.
 * The sheet closes two seconds after the handle is released (three seconds if
 * it is never touched).
 */
class SeekOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        PlayerController.ensureConnected(this)

        setContent {
            ShPlayerTheme(themeMode = Prefs.themeMode) {
                SeekSheet(onDismiss = { finish() })
            }
        }
    }
}

@Composable
private fun SeekSheet(onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val state by PlayerController.state.collectAsStateWithLifecycle()

    val duration = state.durationMs.coerceAtLeast(0L)
    val maxValue = duration.toFloat().coerceAtLeast(1f)

    var dragging by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(state.positionMs.toFloat()) }
    var dismissAt by remember { mutableLongStateOf(System.currentTimeMillis() + 3_000L) }

    // Follow playback until the user grabs the handle; afterwards the handle
    // stays where it was released and the sheet closes shortly.
    LaunchedEffect(state.positionMs, touched, maxValue) {
        if (!touched) {
            sliderValue = state.positionMs.toFloat().coerceIn(0f, maxValue)
        }
    }

    LaunchedEffect(dragging, dismissAt) {
        if (dragging) return@LaunchedEffect
        val remaining = dismissAt - System.currentTimeMillis()
        if (remaining > 0L) delay(remaining)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.outline)
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.title.ifBlank { "Nothing playing" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist.ifBlank { "SH Player" },
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Slider(
                value = sliderValue.coerceIn(0f, maxValue),
                onValueChange = {
                    touched = true
                    dragging = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    dragging = false
                    PlayerController.seekTo(sliderValue.toLong())
                    dismissAt = System.currentTimeMillis() + 2_000L
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.hasTrack && duration > 0L,
                valueRange = 0f..maxValue,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                    inactiveTrackColor = colors.outline,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(sliderValue.toLong()),
                    fontSize = 12.sp,
                    color = colors.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatDuration(duration),
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Drag to seek · closes 2 s after release",
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
