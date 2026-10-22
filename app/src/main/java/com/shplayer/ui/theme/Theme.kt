package com.shplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.shplayer.data.Prefs

/**
 * PlayerPro-inspired palette: near-black surfaces with a warm amber accent.
 *
 * OPTIMIZATION HOOK: skins/themes can be added by extending [ShPalette] and
 * selecting it here, mirroring PlayerPro's separate skin packages.
 */
object ShPalette {
    val Accent = Color(0xFFFF7A18)
    val AccentDim = Color(0xFFCE5F12)
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1A1A1A)
    val Row = Color(0xFF1E1E1E)
    val SurfaceHigh = Color(0xFF232323)
    val SurfaceHigher = Color(0xFF2A2A2A)
    val TextPrimary = Color(0xFFEDEDED)
    val TextSecondary = Color(0xFF9A9A9A)
    val Divider = Color(0xFF2B2B2B)
    val Pill = Color(0xFF34373C)
    val DiscLight = Color(0xFFE6E6E6)
    val DiscMid = Color(0xFFB4B4B4)
    val DiscDark = Color(0xFF8A8A8A)
}

private val DarkColors = darkColorScheme(
    primary = ShPalette.Accent,
    onPrimary = Color(0xFF201204),
    primaryContainer = Color(0xFF3B2410),
    onPrimaryContainer = Color(0xFFFFD9BC),
    secondary = Color(0xFFB9BEC7),
    onSecondary = Color(0xFF14171C),
    background = ShPalette.Background,
    onBackground = ShPalette.TextPrimary,
    surface = ShPalette.Surface,
    onSurface = ShPalette.TextPrimary,
    surfaceVariant = ShPalette.Row,
    onSurfaceVariant = ShPalette.TextSecondary,
    outline = ShPalette.Divider,
    outlineVariant = ShPalette.SurfaceHigher,
    error = Color(0xFFCF6679),
)

private val LightColors = lightColorScheme(
    primary = ShPalette.AccentDim,
    onPrimary = Color.White,
    background = Color(0xFFF7F7F9),
    onBackground = Color(0xFF15171C),
    surface = Color.White,
    onSurface = Color(0xFF15171C),
    surfaceVariant = Color(0xFFECEDF1),
    onSurfaceVariant = Color(0xFF4A4F58),
    outline = Color(0xFFC9CCD3),
)

@Composable
fun ShPlayerTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        Prefs.THEME_LIGHT -> false
        Prefs.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            val window = (context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(colorScheme = colors, content = content)
}
