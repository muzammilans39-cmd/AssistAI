package com.example.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Distinct tactile vibration patterns designed for blind and visually impaired users
 * to distinguish voice assistant lifecycle transitions without looking at the screen.
 */
enum class VoiceHapticPattern(
    val displayName: String,
    val tactileDescription: String,
    val timings: LongArray,
    val amplitudes: IntArray
) {
    /**
     * Rising double-pulse ("soft-FIRM") when the microphone opens and starts listening.
     */
    START_LISTENING(
        displayName = "Listening Started (Rising Double-Pulse)",
        tactileDescription = "Short light tap followed by a firm pulse — microphone is open",
        timings = longArrayOf(0L, 45L, 55L, 95L),
        amplitudes = intArrayOf(0, 140, 0, 255)
    ),

    /**
     * Rhythmic triple-tick ("tick-tick-tick") when voice capture ends and AI is processing input.
     */
    PROCESSING_INPUT(
        displayName = "Processing Input (Rhythmic Triple-Tick)",
        tactileDescription = "Three gentle rhythmic pulses — analyzing your voice command",
        timings = longArrayOf(0L, 35L, 65L, 35L, 65L, 40L),
        amplitudes = intArrayOf(0, 120, 0, 155, 0, 190)
    ),

    /**
     * Crisp two-beat confirmation pulse when a voice command or navigation succeeds.
     */
    COMMAND_CONFIRMED(
        displayName = "Command Confirmed (Success Cadence)",
        tactileDescription = "Two crisp confirmation beats — voice command executed",
        timings = longArrayOf(0L, 60L, 70L, 135L),
        amplitudes = intArrayOf(0, 200, 0, 255)
    ),

    /**
     * Light single tap for general accessible button presses or TTS start.
     */
    UI_TAP(
        displayName = "Tactile Button Tap",
        tactileDescription = "Single crisp tactile tap",
        timings = longArrayOf(0L, 40L),
        amplitudes = intArrayOf(0, 180)
    ),

    /**
     * High-attention triple alert pulse for SOS beacon or critical warnings.
     */
    EMERGENCY_ALERT(
        displayName = "SOS Emergency Pulse",
        tactileDescription = "Three strong high-amplitude alert pulses",
        timings = longArrayOf(0L, 120L, 80L, 120L, 80L, 160L),
        amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
    )
}

/**
 * Manages accessible haptic feedback patterns using Android's [VibratorManager] (API 31+)
 * with backward compatibility for [Vibrator] on earlier API levels.
 */
class AssistHapticManager(private val context: Context) {

    private val _hapticsEnabled = MutableStateFlow(true)
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _lastTriggeredPattern = MutableStateFlow<VoiceHapticPattern?>(null)
    val lastTriggeredPattern: StateFlow<VoiceHapticPattern?> = _lastTriggeredPattern.asStateFlow()

    private val _patternHistoryCount = MutableStateFlow(0)
    val patternHistoryCount: StateFlow<Int> = _patternHistoryCount.asStateFlow()

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
    }

    fun toggleHapticsEnabled(): Boolean {
        val next = !_hapticsEnabled.value
        _hapticsEnabled.value = next
        if (next) {
            triggerCommandConfirmedHaptic()
        }
        return next
    }

    /**
     * Triggers the [VoiceHapticPattern.START_LISTENING] rising double-pulse when the
     * voice assistant starts listening.
     */
    fun triggerStartListeningHaptic() {
        triggerPattern(VoiceHapticPattern.START_LISTENING)
    }

    /**
     * Triggers the [VoiceHapticPattern.PROCESSING_INPUT] rhythmic triple-tick when the
     * voice assistant processes spoken or typed input.
     */
    fun triggerProcessingInputHaptic() {
        triggerPattern(VoiceHapticPattern.PROCESSING_INPUT)
    }

    /**
     * Triggers the [VoiceHapticPattern.COMMAND_CONFIRMED] confirmation cadence when the
     * voice assistant confirms a successful command or navigation action.
     */
    fun triggerCommandConfirmedHaptic() {
        triggerPattern(VoiceHapticPattern.COMMAND_CONFIRMED)
    }

    /**
     * Triggers a light single tap for standard UI feedback.
     */
    fun triggerTapHaptic() {
        triggerPattern(VoiceHapticPattern.UI_TAP)
    }

    /**
     * Triggers a strong triple pulse for SOS emergency actions.
     */
    fun triggerEmergencyAlertHaptic() {
        triggerPattern(VoiceHapticPattern.EMERGENCY_ALERT)
    }

    /**
     * Executes the waveform vibration on the device's [VibratorManager] (Android 12+)
     * or legacy [Vibrator].
     */
    fun triggerPattern(pattern: VoiceHapticPattern) {
        _lastTriggeredPattern.value = pattern
        _patternHistoryCount.value = _patternHistoryCount.value + 1

        if (!_hapticsEnabled.value) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                if (vibratorManager != null) {
                    val defaultVibrator = vibratorManager.defaultVibrator
                    val effect = if (defaultVibrator.hasAmplitudeControl()) {
                        VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
                    } else {
                        VibrationEffect.createWaveform(pattern.timings, -1)
                    }
                    vibratorManager.vibrate(CombinedVibration.createParallel(effect))
                    return
                }
            }

            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (vibrator.hasAmplitudeControl()) {
                        VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1)
                    } else {
                        VibrationEffect.createWaveform(pattern.timings, -1)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern.timings, -1)
                }
            }
        } catch (_: Exception) {
            // Gracefully ignore on emulators or devices without vibrator hardware
        }
    }
}
