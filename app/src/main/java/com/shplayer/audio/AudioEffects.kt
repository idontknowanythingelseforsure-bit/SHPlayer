package com.shplayer.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Snapshot of everything the Equalizer screen renders. */
data class EffectsUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val backendName: String = "None",
    val bandCount: Int = 0,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val bandLevelsMb: List<Int> = emptyList(),
    val bandLabels: List<String> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPreset: Int = -1,
    val bassBoost: Int = 0,
    val bassSupported: Boolean = false,
    val virtualizer: Int = 0,
    val virtualizerSupported: Boolean = false,
    val reverbPreset: Int = 0,
    val reverbSupported: Boolean = false,
    val reverbPresets: List<String> = emptyList(),
)

/**
 * Swappable DSP seam.
 *
 * PlayerPro ships two interchangeable effect implementations behind one
 * interface (`u7.j`): the platform AudioFX chain (`u7.d`) and its own soundpack
 * engine (`u7.m`). SH Player keeps that shape so a native engine can be added
 * later without touching the UI.
 */
interface EffectsBackend {
    val name: String

    /** @return true when the backend could attach to [sessionId]. */
    fun attach(sessionId: Int): Boolean

    fun release()

    fun snapshot(): EffectsUiState

    fun setEnabled(enabled: Boolean)

    fun setPreset(index: Int)

    fun setBandLevel(band: Int, levelMb: Int)

    fun setBassBoost(strength: Int)

    fun setVirtualizer(strength: Int)

    fun setReverb(preset: Int)
}

/** Used before playback starts or when the device has no AudioFX support. */
class NullEffectsBackend : EffectsBackend {
    override val name: String = "Unavailable"

    override fun attach(sessionId: Int): Boolean = false

    override fun release() = Unit

    override fun snapshot(): EffectsUiState = EffectsUiState()

    override fun setEnabled(enabled: Boolean) = Unit

    override fun setPreset(index: Int) = Unit

    override fun setBandLevel(band: Int, levelMb: Int) = Unit

    override fun setBassBoost(strength: Int) = Unit

    override fun setVirtualizer(strength: Int) = Unit

    override fun setReverb(preset: Int) = Unit
}

/** Platform AudioFX implementation: Equalizer + BassBoost + Virtualizer. */
class SystemAudioEffectsBackend : EffectsBackend {

    override val name: String = "System AudioFX"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null

    override fun attach(sessionId: Int): Boolean {
        release()

        val eq = runCatching { Equalizer(0, sessionId) }.getOrNull() ?: return false
        equalizer = eq
        bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()
        reverb = runCatching { PresetReverb(0, sessionId) }.getOrNull()

        runCatching { eq.enabled = false }
        runCatching { bassBoost?.enabled = false }
        runCatching { virtualizer?.enabled = false }
        runCatching { reverb?.enabled = false }
        return true
    }

    override fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { reverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
    }

    override fun snapshot(): EffectsUiState {
        val eq = equalizer ?: return EffectsUiState()

        val range = runCatching { eq.bandLevelRange }.getOrNull() ?: shortArrayOf(-1500, 1500)
        val bandCount = runCatching { eq.numberOfBands.toInt() }.getOrDefault(0)
        val levels = (0 until bandCount).map { band ->
            runCatching { eq.getBandLevel(band.toShort()).toInt() }.getOrDefault(0)
        }
        val labels = (0 until bandCount).map { band ->
            formatFrequency(runCatching { eq.getCenterFreq(band.toShort()) }.getOrDefault(0))
        }
        val presets = runCatching {
            (0 until eq.numberOfPresets.toInt()).map { index ->
                eq.getPresetName(index.toShort())
            }
        }.getOrDefault(emptyList())

        return EffectsUiState(
            available = true,
            enabled = runCatching { eq.enabled }.getOrDefault(false),
            backendName = name,
            bandCount = bandCount,
            minLevelMb = range.getOrElse(0) { -1500 }.toInt(),
            maxLevelMb = range.getOrElse(1) { 1500 }.toInt(),
            bandLevelsMb = levels,
            bandLabels = labels,
            presets = presets,
            currentPreset = runCatching { eq.currentPreset.toInt() }.getOrDefault(-1),
            bassBoost = runCatching { bassBoost?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0),
            bassSupported = bassBoost?.strengthSupported == true,
            virtualizer = runCatching { virtualizer?.roundedStrength?.toInt() ?: 0 }.getOrDefault(0),
            virtualizerSupported = virtualizer?.strengthSupported == true,
            reverbSupported = reverb != null,
            reverbPreset = runCatching { reverb?.preset?.toInt() ?: 0 }.getOrDefault(0),
            reverbPresets = if (reverb != null) REVERB_PRESETS else emptyList(),
        )
    }

    override fun setEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        runCatching { virtualizer?.enabled = enabled }
        runCatching { reverb?.enabled = enabled }
    }

    override fun setPreset(index: Int) {
        runCatching { equalizer?.usePreset(index.toShort()) }
    }

    override fun setBandLevel(band: Int, levelMb: Int) {
        runCatching { equalizer?.setBandLevel(band.toShort(), levelMb.toShort()) }
    }

    override fun setBassBoost(strength: Int) {
        runCatching { bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort()) }
    }

    override fun setVirtualizer(strength: Int) {
        runCatching { virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort()) }
    }

    override fun setReverb(preset: Int) {
        runCatching { reverb?.preset = preset.coerceIn(0, REVERB_PRESETS.lastIndex).toShort() }
    }

    private fun formatFrequency(milliHertz: Int): String {
        val hz = milliHertz / 1000
        return if (hz >= 1000) "${hz / 1000}k" else "$hz"
    }

    private companion object {
        val REVERB_PRESETS = listOf(
            "None",
            "Small room",
            "Medium room",
            "Large room",
            "Medium hall",
            "Large hall",
            "Plate",
        )
    }
}

/**
 * Reserved native engine slot.
 *
 * OPTIMIZATION HOOK: PlayerPro's DSP Pack is a separate APK
 * (`com.tbig.playerpro.soundpack`) exposing `FFMpeg`/`l7.*` internally. A future
 * SH Player DSP module can implement this class with JNI bindings and be returned
 * from [EffectsBackendFactory] — no UI changes required.
 */
class NativeDspBackend(private val libraryPath: String) : EffectsBackend {

    override val name: String = "Native DSP"

    override fun attach(sessionId: Int): Boolean = false

    override fun release() = Unit

    override fun snapshot(): EffectsUiState = EffectsUiState()

    override fun setEnabled(enabled: Boolean) = Unit

    override fun setPreset(index: Int) = Unit

    override fun setBandLevel(band: Int, levelMb: Int) = Unit

    override fun setBassBoost(strength: Int) = Unit

    override fun setVirtualizer(strength: Int) = Unit

    override fun setReverb(preset: Int) = Unit

    @Suppress("unused")
    fun describe(): String = "NativeDspBackend($libraryPath)"
}

object EffectsBackendFactory {
    /**
     * OPTIMIZATION HOOK: prefer a native DSP backend when one is bundled, then
     * fall back to platform AudioFX:
     *
     * ```
     * if (NativeDspBackend.isAvailable(context)) return NativeDspBackend(path)
     * ```
     */
    fun create(sessionId: Int): EffectsBackend {
        val system = SystemAudioEffectsBackend()
        return if (system.attach(sessionId)) system else NullEffectsBackend()
    }
}

/**
 * Single owner of the active effects backend.
 *
 * Lives in the app process, so [PlaybackService] and the UI share one instance.
 */
object EffectsManager {

    private val _state = MutableStateFlow(EffectsUiState())
    val state: StateFlow<EffectsUiState> = _state.asStateFlow()

    private var backend: EffectsBackend = NullEffectsBackend()
    private var lastSessionId: Int = Int.MIN_VALUE

    fun init(context: Context) {
        // Reserved for future native DSP discovery; keeps the application context
        // available without leaking an Activity.
        @Suppress("UNUSED_VARIABLE")
        val appContext = context.applicationContext
    }

    @Synchronized
    @androidx.annotation.OptIn(UnstableApi::class)
    fun onAudioSessionChanged(sessionId: Int) {
        if (sessionId == lastSessionId) return
        lastSessionId = sessionId

        runCatching { backend.release() }
        backend = if (sessionId == C.AUDIO_SESSION_ID_UNSET) {
            NullEffectsBackend()
        } else {
            EffectsBackendFactory.create(sessionId)
        }
        publish()
    }

    fun setEnabled(enabled: Boolean) {
        backend.setEnabled(enabled)
        publish()
    }

    fun setPreset(index: Int) {
        backend.setPreset(index)
        publish()
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        backend.setBandLevel(band, levelMb)
        publish()
    }

    fun setBassBoost(strength: Int) {
        backend.setBassBoost(strength)
        publish()
    }

    fun setVirtualizer(strength: Int) {
        backend.setVirtualizer(strength)
        publish()
    }

    fun setReverb(preset: Int) {
        backend.setReverb(preset)
        publish()
    }

    private fun publish() {
        _state.value = runCatching { backend.snapshot() }.getOrElse { EffectsUiState() }
    }
}
