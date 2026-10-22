package com.shplayer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shplayer.audio.EffectsManager
import com.shplayer.data.Prefs
import com.shplayer.ui.components.SectionHeader

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val fx by EffectsManager.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    var gapless by remember { mutableStateOf(Prefs.gaplessEnabled) }
    var crossfade by remember { mutableFloatStateOf(Prefs.crossfadeSeconds.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader("Appearance")
        SettingRow(
            title = "Skin",
            subtitle = "Black & orange (PlayerPro-inspired)",
        )
        Divider()

        SectionHeader("Playback")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Gapless playback",
                    fontSize = 17.sp,
                    color = colors.onBackground,
                )
                Text(
                    text = "Coming soon",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            Switch(
                checked = gapless,
                onCheckedChange = {
                    gapless = it
                    Prefs.gaplessEnabled = it
                },
                enabled = false,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.primary,
                ),
            )
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Crossfade",
                    fontSize = 17.sp,
                    color = colors.onBackground,
                )
                Text(
                    text = "${crossfade.toInt()} s",
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            Slider(
                value = crossfade,
                onValueChange = {
                    crossfade = it
                    Prefs.crossfadeSeconds = it.toInt()
                },
                valueRange = 0f..12f,
                enabled = false,
                colors = SliderDefaults.colors(
                    thumbColor = colors.primary,
                    activeTrackColor = colors.primary,
                ),
            )
            Text(
                text = "Coming soon",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
            )
        }
        Divider()

        SectionHeader("Sound effects")
        SettingRow(
            title = "DSP backend",
            subtitle = fx.backendName,
        )
        SettingRow(
            title = "Status",
            subtitle = if (fx.available) "Ready" else "Waiting for playback",
        )
        Divider()

        SectionHeader("About")
        SettingRow(
            title = "SH Player",
            subtitle = "Version $versionName",
        )
        SettingRow(
            title = "Interface",
            subtitle = "PlayerPro-inspired browser, lists and disc artwork",
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                color = colors.onBackground,
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        thickness = 1.dp,
    )
}
