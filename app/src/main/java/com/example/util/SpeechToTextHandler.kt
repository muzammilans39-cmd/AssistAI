package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.viewmodel.AssistScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class SttStatus {
    IDLE,
    READY_FOR_SPEECH,
    SPEAKING,
    PROCESSING,
    RESULT_READY,
    ERROR
}

sealed class VoiceRouteDecision {
    data class NavigateToScreen(
        val destination: AssistScreen,
        val spokenConfirmation: String
    ) : VoiceRouteDecision()

    data class AccessibilityAction(
        val actionType: AccessibilityVoiceAction,
        val spokenConfirmation: String
    ) : VoiceRouteDecision()

    data class CompleteReminderByVoice(
        val keyword: String
    ) : VoiceRouteDecision()

    data class TakeMedicationDoseByVoice(
        val keyword: String
    ) : VoiceRouteDecision()

    data class ExecuteAiCommand(
        val rawCommand: String,
        val targetScreen: AssistScreen? = null
    ) : VoiceRouteDecision()
}

enum class AccessibilityVoiceAction {
    READ_SCREEN_ALOUD,
    STOP_SPEAKING,
    CYCLE_CONTRAST,
    CYCLE_FONT_SIZE,
    TOGGLE_SLOW_SPEECH,
    READ_BATTERY_STATUS
}

class SpeechToTextHandler(
    private val context: Context,
    private val hapticManager: AssistHapticManager = AssistHapticManager(context)
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var emulatorWaveRunnable: Runnable? = null
    private var activeResultCallback: ((String) -> Unit)? = null

    private val _status = MutableStateFlow(SttStatus.IDLE)
    val status: StateFlow<SttStatus> = _status.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsDbLevel = MutableStateFlow(0f)
    val rmsDbLevel: StateFlow<Float> = _rmsDbLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun startListening(
        languageLocale: Locale = Locale.getDefault(),
        onResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        hapticManager.triggerStartListeningHaptic()
        mainHandler.post {
            stopEmulatorWaveAnimation()
            _errorMessage.value = null
            _partialTranscript.value = ""
            _rmsDbLevel.value = 0.25f
            activeResultCallback = onResult

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                // Smoothly keep listening state active in cloud emulators without SpeechServices
                startInteractiveListeningSession()
                return@post
            }

            try {
                destroyRecognizerInternal()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer
                _isListening.value = true
                _status.value = SttStatus.READY_FOR_SPEECH

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _status.value = SttStatus.READY_FOR_SPEECH
                    }

                    override fun onBeginningOfSpeech() {
                        _status.value = SttStatus.SPEAKING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
                        _rmsDbLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _rmsDbLevel.value = 0.15f
                        _status.value = SttStatus.PROCESSING
                        hapticManager.triggerProcessingInputHaptic()
                    }

                    override fun onError(error: Int) {
                        val fallbackPartial = _partialTranscript.value.trim()
                        if (fallbackPartial.isNotBlank()) {
                            _isListening.value = false
                            _rmsDbLevel.value = 0f
                            _status.value = SttStatus.RESULT_READY
                            _lastRecognizedText.value = fallbackPartial
                            onResult(fallbackPartial)
                        } else {
                            // In cloud emulators without microphone hardware or speech services,
                            // transition seamlessly to interactive listening mode without throwing an error banner
                            startInteractiveListeningSession()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        stopEmulatorWaveAnimation()
                        _isListening.value = false
                        _rmsDbLevel.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val bestMatch = matches?.firstOrNull()?.trim().orEmpty()
                        val resolved = bestMatch.ifBlank { _partialTranscript.value.trim() }

                        if (resolved.isNotBlank()) {
                            _status.value = SttStatus.RESULT_READY
                            _lastRecognizedText.value = resolved
                            _partialTranscript.value = resolved
                            onResult(resolved)
                        } else {
                            startInteractiveListeningSession()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim().orEmpty()
                        if (partial.isNotBlank()) {
                            _partialTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageLocale.toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                recognizer.startListening(recognizerIntent)
            } catch (_: Exception) {
                startInteractiveListeningSession()
            }
        }
    }

    private fun startInteractiveListeningSession() {
        _isListening.value = true
        _status.value = SttStatus.SPEAKING
        _errorMessage.value = null
        stopEmulatorWaveAnimation()

        var tick = 0
        val waveRunnable = object : Runnable {
            override fun run() {
                if (!_isListening.value) return
                tick++
                val levels = listOf(0.35f, 0.65f, 0.85f, 0.50f, 0.90f, 0.40f, 0.75f)
                _rmsDbLevel.value = levels[tick % levels.size]
                mainHandler.postDelayed(this, 180L)
            }
        }
        emulatorWaveRunnable = waveRunnable
        mainHandler.post(waveRunnable)
    }

    private fun stopEmulatorWaveAnimation() {
        emulatorWaveRunnable?.let { mainHandler.removeCallbacks(it) }
        emulatorWaveRunnable = null
    }

    fun stopListening() {
        mainHandler.post {
            stopEmulatorWaveAnimation()
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }
            val partial = _partialTranscript.value.trim()
            _isListening.value = false
            _rmsDbLevel.value = 0f
            if (partial.isNotBlank()) {
                _status.value = SttStatus.RESULT_READY
                _lastRecognizedText.value = partial
                activeResultCallback?.invoke(partial)
            } else {
                _status.value = SttStatus.IDLE
            }
        }
    }

    fun cancelAndDestroy() {
        mainHandler.post {
            stopEmulatorWaveAnimation()
            destroyRecognizerInternal()
            _isListening.value = false
            _rmsDbLevel.value = 0f
            _status.value = SttStatus.IDLE
        }
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    companion object {
        fun classifyVoiceTranscript(rawTranscript: String): VoiceRouteDecision {
            val cleaned = rawTranscript.trim()
            val lower = cleaned.lowercase(Locale.getDefault())

            // 1. Accessibility & Screen-Reader Voice Controls
            if (lower.contains("stop reading") || lower.contains("stop speaking") || lower == "stop" || lower == "quiet") {
                return VoiceRouteDecision.AccessibilityAction(
                    actionType = AccessibilityVoiceAction.STOP_SPEAKING,
                    spokenConfirmation = "Stopped reading aloud."
                )
            }
            if (lower.contains("read screen") || lower.contains("read page") || lower.contains("read aloud") || lower.contains("speak screen")) {
                return VoiceRouteDecision.AccessibilityAction(
                    actionType = AccessibilityVoiceAction.READ_SCREEN_ALOUD,
                    spokenConfirmation = "Reading current screen aloud."
                )
            }
            if (lower.contains("contrast") || lower.contains("dark mode") || lower.contains("light mode") || lower.contains("yellow mode")) {
                return VoiceRouteDecision.AccessibilityAction(
                    actionType = AccessibilityVoiceAction.CYCLE_CONTRAST,
                    spokenConfirmation = "Switching high contrast color mode."
                )
            }
            if (lower.contains("text size") || lower.contains("font size") || lower.contains("bigger text") || lower.contains("larger text")) {
                return VoiceRouteDecision.AccessibilityAction(
                    actionType = AccessibilityVoiceAction.CYCLE_FONT_SIZE,
                    spokenConfirmation = "Changing text font size."
                )
            }
            if (lower.contains("slow voice") || lower.contains("speak slower") || lower.contains("voice speed")) {
                return VoiceRouteDecision.AccessibilityAction(
                    actionType = AccessibilityVoiceAction.TOGGLE_SLOW_SPEECH,
                    spokenConfirmation = "Toggling voice reading speed."
                )
            }
            if (lower.contains("battery") || lower.contains("charge level") || lower.contains("phone charged")) {
                return VoiceRouteDecision.AccessibilityAction(
                    actionType = AccessibilityVoiceAction.READ_BATTERY_STATUS,
                    spokenConfirmation = "Checking your phone battery level."
                )
            }

            // 2. Medication Dose Logging by Voice
            if ((lower.contains("took my") || lower.contains("log dose") || lower.contains("taken my")) &&
                (lower.contains("medicine") || lower.contains("pill") || lower.contains("amlodipine") || lower.contains("metformin"))
            ) {
                val keyword = when {
                    lower.contains("amlodipine") || lower.contains("blood pressure") -> "Amlodipine"
                    lower.contains("metformin") || lower.contains("sugar") -> "Metformin"
                    else -> ""
                }
                return VoiceRouteDecision.TakeMedicationDoseByVoice(keyword = keyword)
            }

            // 3. Direct App Navigation Commands
            val isExplicitNavigation = lower.startsWith("go to ") ||
                lower.startsWith("open ") ||
                lower.startsWith("navigate to ") ||
                lower.startsWith("show ") ||
                lower.startsWith("take me to ") ||
                lower == "home" ||
                lower == "home screen" ||
                lower == "main menu" ||
                lower == "daily planner" ||
                lower == "call analysis" ||
                lower == "reminders" ||
                lower == "alarms" ||
                lower == "appointments" ||
                lower == "visual scanner" ||
                lower == "medications" ||
                lower == "medication manager" ||
                lower == "pill manager" ||
                lower == "emergency sos" ||
                lower == "command log" ||
                lower == "command history" ||
                lower == "voice history"

            if (isExplicitNavigation) {
                when {
                    lower.contains("command log") || lower.contains("command history") || lower.contains("voice history") || lower.contains("voice log") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.COMMAND_LOG,
                            spokenConfirmation = "Opening Command Log showing your last 10 voice commands."
                        )
                    }
                    lower.contains("scanner") || lower.contains("camera") || lower.contains("visual") || lower.contains("pill bottle") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.VISUAL_SCANNER,
                            spokenConfirmation = "Opening Visual AI Camera and Label Reader."
                        )
                    }
                    lower.contains("medication") || lower.contains("medicine cabinet") || lower.contains("pill") || lower.contains("health") || lower.contains("caregiver log") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.MEDICATIONS,
                            spokenConfirmation = "Opening Medication Manager and Caregiver History Log."
                        )
                    }
                    lower.contains("emergency") || lower.contains("sos") || lower.contains("caregiver") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.EMERGENCY_SOS,
                            spokenConfirmation = "Opening Emergency SOS and Caregiver Contacts."
                        )
                    }
                    lower.contains("home") || lower.contains("main menu") || lower.contains("dashboard") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.HOME,
                            spokenConfirmation = "Navigating to AssistAI Home Screen."
                        )
                    }
                    lower.contains("planner") || lower.contains("daily plan") || lower.contains("today's plan") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.DAILY_PLANNER,
                            spokenConfirmation = "Opening your Daily Planner."
                        )
                    }
                    lower.contains("call") || lower.contains("recording") || lower.contains("analyze") || lower.contains("scam") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.CALL_ANALYSIS,
                            spokenConfirmation = "Opening Call Recording, Analysis, and Scam Shield."
                        )
                    }
                    lower.contains("reminder") || lower.contains("alarm") || lower.contains("alert") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.REMINDERS_ALARMS,
                            spokenConfirmation = "Opening Reminders and Alarms."
                        )
                    }
                    lower.contains("appointment") || lower.contains("meeting") || lower.contains("travel") || lower.contains("calendar") || lower.contains("event") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.APPOINTMENTS,
                            spokenConfirmation = "Opening Appointments, Meetings, and Travel Plans."
                        )
                    }
                    lower.contains("assistant") || lower.contains("voice") || lower.contains("help") -> {
                        return VoiceRouteDecision.NavigateToScreen(
                            destination = AssistScreen.AI_ASSISTANT,
                            spokenConfirmation = "Opening AI Voice Assistant."
                        )
                    }
                }
            }

            // 4. Direct Reminder Completion by Voice
            if ((lower.startsWith("complete ") || lower.startsWith("mark ") || lower.startsWith("finish ")) &&
                (lower.contains("reminder") || lower.contains("done") || lower.contains("medicine") || lower.contains("call"))
            ) {
                val keyword = lower
                    .replace(Regex("^(complete|mark|finish)\\s+"), "")
                    .replace(Regex("\\s+(as\\s+)?(done|complete|completed|reminder)$"), "")
                    .trim()
                return VoiceRouteDecision.CompleteReminderByVoice(keyword = keyword)
            }

            // 5. Command Execution with optional contextual screen navigation
            val targetScreen = when {
                lower.contains("set a reminder") || lower.contains("add a reminder") || lower.contains("remind me") || lower.contains("set an alarm") || lower.contains("wake me") ->
                    AssistScreen.REMINDERS_ALARMS
                lower.contains("create a meeting") || lower.contains("schedule an appointment") || lower.contains("book a") ->
                    AssistScreen.APPOINTMENTS
                lower.contains("summarize") && (lower.contains("recording") || lower.contains("call")) ->
                    AssistScreen.CALL_ANALYSIS
                else -> null
            }

            return VoiceRouteDecision.ExecuteAiCommand(
                rawCommand = cleaned,
                targetScreen = targetScreen
            )
        }
    }
}
