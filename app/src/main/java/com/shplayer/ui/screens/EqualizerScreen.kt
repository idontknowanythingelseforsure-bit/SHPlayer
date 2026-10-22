package com.shplayer.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shplayer.audio.EffectsManager
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import java.util.Locale

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val fx by EffectsManager.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

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
            Text(
                text = "EQUALIZER",
                fontSize = 15.sp,
                letterSpacing = 2.sp,
                color = colors.primary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            EnabledSegmented(
                enabled = fx.enabled,
                available = fx.available,
                onChange = { EffectsManager.setEnabled(it) },
            )
            IconButton(onClick = {
                for (band in 0 until fx.bandCount) {
                    EffectsManager.setBandLevel(band, 0)
                }
            }) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reset equalizer",
                    tint = colors.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (!fx.available) {
                Text(
                    text = "Sound effects are not available until playback starts on this device.",
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            DropdownSelector(
                label = "Presets",
                value = fx.presets.getOrNull(fx.currentPreset) ?: "Custom",
                options = fx.presets,
                enabled = fx.available && fx.enabled,
                onSelect = { EffectsManager.setPreset(it) },
            )

            if (fx.bandCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .height(186.dp)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text("+15", fontSize = 10.sp, color = colors.onSurfaceVariant)
                        Text("0", fontSize = 10.sp, color = colors.onSurfaceVariant)
                        Text("-15", fontSize = 10.sp, color = colors.onSurfaceVariant)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (band in 0 until fx.bandCount) {
                            BandSlider(
                                label = fx.bandLabels.getOrElse(band) { "" },
                                level = fx.bandLevelsMb.getOrElse(band) { 0 },
                                min = fx.minLevelMb,
                                max = fx.maxLevelMb,
                                enabled = fx.enabled,
                                onChange = { EffectsManager.setBandLevel(band, it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }

            DropdownSelector(
                label = "Reverb",
                value = fx.reverbPresets.getOrNull(fx.reverbPreset) ?: "None",
                options = fx.reverbPresets,
                enabled = fx.enabled && fx.reverbSupported,
                onSelect = { EffectsManager.setReverb(it) },
            )

            if (fx.bassSupported || fx.virtualizerSupported) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    if (fx.bassSupported) {
                        RotaryKnob(
                            label = "Bass",
                            value = fx.bassBoost,
                            enabled = fx.enabled,
                            onChange = { EffectsManager.setBassBoost(it) },
                        )
                    }
                    if (fx.virtualizerSupported) {
                        RotaryKnob(
                            label = "Virtualizer",
                            value = fx.virtualizer,
                            enabled = fx.enabled,
                            onChange = { EffectsManager.setVirtualizer(it) },
                        )
                    }
                }
            }

            VolumeSection()

            Text(
                text = fx.backendName,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun EnabledSegmented(
    enabled: Boolean,
    available: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(3.dp)

    Row(
        modifier = Modifier
            .border(1.dp, colors.outline, shape)
            .padding(1.dp),
    ) {
        listOf(true to "ON", false to "OFF").forEach { (value, label) ->
            val selected = enabled == value
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                modifier = Modifier
                    .background(if (selected) colors.primary else Color.Transparent, shape)
                    .clickable(enabled = available) { onChange(value) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DropdownSelector(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                fontSize = 16.sp,
                color = if (enabled) colors.onBackground else colors.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                )
            }
        }
    }
    HorizontalDivider(color = colors.outline, thickness = 1.dp)
}

@Composable
private fun BandSlider(
    label: String,
    level: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val range = (max - min).coerceAtLeast(1)
    val fraction = ((level - min).toFloat() / range).coerceIn(0f, 1f)

    fun fromY(y: Float, height: Int): Int {
        if (height <= 0) return level
        val f = 1f - (y / height).coerceIn(0f, 1f)
        return (min + f * range).roundToInt()
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .width(34.dp)
                .pointerInput(enabled, min, max) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        onChange(fromY(change.position.y, size.height))
                    }
                }
                .pointerInput(enabled, min, max) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        onChange(fromY(offset.y, size.height))
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val x = size.width / 2f
                val trackWidth = 4.dp.toPx()

                for (i in 0..4) {
                    val y = size.height * i / 4f
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(x - 11.dp.toPx(), y),
                        end = Offset(x + 11.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                drawLine(
                    color = Color(0xFF3A3A3A),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round,
                )

                val thumbY = size.height * (1f - fraction)

                drawLine(
                    color = colors.primary.copy(alpha = if (enabled) 0.55f else 0.2f),
                    start = Offset(x, thumbY),
                    end = Offset(x, size.height),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round,
                )

                drawCircle(
                    color = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                    radius = 8.5.dp.toPx(),
                    center = Offset(x, thumbY),
                )
                drawCircle(
                    color = if (enabled) colors.primary else colors.onSurfaceVariant,
                    radius = 4.5.dp.toPx(),
                    center = Offset(x, thumbY),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = String.format(Locale.US, "%+.1f", level / 100f),
            fontSize = 9.sp,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun RotaryKnob(
    label: String,
    value: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var dragStart by remember { mutableIntStateOf(value) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { dragStart = value },
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = (-dragAmount.y / 3f).roundToInt()
                        onChange((dragStart + delta).coerceIn(0, 1000))
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                drawCircle(color = Color(0xFF1E1E1E), radius = radius, center = center)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFF4F4F4), Color(0xFFC2C2C2), Color(0xFF7E7E7E)),
                        center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                        radius = radius * 1.3f,
                    ),
                    radius = radius * 0.88f,
                    center = center,
                )
                drawCircle(
                    color = Color(0xFF4A4A4A),
                    radius = radius * 0.88f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )

                val arcInset = radius * 0.12f
                val arcSize = Size(radius * 1.76f, radius * 1.76f)

                drawArc(
                    color = Color(0xFF3C3C3C),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(arcInset, arcInset),
                    size = arcSize,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = if (enabled) colors.primary else colors.onSurfaceVariant,
                    startAngle = 135f,
                    sweepAngle = 270f * (value / 1000f),
                    useCenter = false,
                    topLeft = Offset(arcInset, arcInset),
                    size = arcSize,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
                )

                val angle = Math.toRadians((135.0 + 270.0 * (value / 1000f)))
                drawLine(
                    color = colors.primary,
                    start = center,
                    end = Offset(
                        center.x + cos(angle).toFloat() * radius * 0.55f,
                        center.y + sin(angle).toFloat() * radius * 0.55f,
                    ),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(color = Color(0xFF333333), radius = radius * 0.15f, center = center)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun VolumeSection() {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var volume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = "VOLUME",
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            color = colors.onSurfaceVariant,
        )
        Slider(
            value = volume.toFloat(),
            onValueChange = { value ->
                val newVolume = value.roundToInt().coerceIn(0, maxVolume)
                volume = newVolume
                runCatching {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                }
            },
            valueRange = 0f..maxVolume.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.outline,
            ),
        )
    }
}
