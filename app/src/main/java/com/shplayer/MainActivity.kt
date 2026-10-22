package com.shplayer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.shplayer.data.Permissions
import com.shplayer.data.Prefs
import com.shplayer.playback.PlayerController
import com.shplayer.ui.AppRoot
import com.shplayer.ui.screens.PermissionScreen
import com.shplayer.ui.theme.ShPlayerTheme

class MainActivity : ComponentActivity() {

    private val pendingAudioUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only treat the intent as a fresh "Open with" on the first creation so
        // a configuration/process restore does not replay the file.
        if (savedInstanceState == null) {
            pendingAudioUri.value = audioUriFromIntent(intent)
        }
        PlayerController.ensureConnected(this)
        setContent { ShPlayerRoot(pendingAudioUri) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAudioUri.value = audioUriFromIntent(intent)
    }

    private fun audioUriFromIntent(intent: Intent?): Uri? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_VIEW && intent.action != Intent.ACTION_SEND) return null
        intent.data?.let { return it }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}

@Composable
private fun ShPlayerRoot(pendingAudioUri: MutableState<Uri?>) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var themeMode by remember { mutableIntStateOf(Prefs.themeMode) }
    var granted by remember { mutableStateOf(Permissions.hasAudioPermission(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Notification permission is optional; playback still works. */ }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        if (allowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Permissions.hasNotificationPermission(context)
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Refresh after returning from the system settings screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = Permissions.hasAudioPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ShPlayerTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            if (granted) {
                AppRoot(
                    externalAudioUri = pendingAudioUri.value,
                    onExternalAudioConsumed = { pendingAudioUri.value = null },
                )
            } else {
                PermissionScreen(
                    onGrant = { audioLauncher.launch(Permissions.audioPermission) },
                    onOpenSettings = { openAppSettings(context) },
                )
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )
    context.startActivity(intent)
}
