package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmEntity
import com.example.data.AppointmentEntity
import com.example.data.AssistDatabase
import com.example.data.AssistRepository
import com.example.data.CallRecordEntity
import com.example.data.EmergencyContactEntity
import com.example.data.MedicationEntity
import com.example.data.MedicationHistoryLogEntity
import com.example.data.ReminderEntity
import com.example.data.VisualScanEntity
import com.example.data.VoiceCommandLogEntity
import com.example.network.GeminiService
import com.example.ui.theme.ContrastMode
import com.example.util.AccessibilitySpeechHelper
import com.example.util.AccessibilityVoiceAction
import com.example.util.BatteryStatusHelper
import com.example.util.SpeechToTextHandler
import com.example.util.VoiceHapticPattern
import com.example.util.VoiceRouteDecision
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers

private val chatMessageIdGenerator = AtomicLong(1000L)

enum class AssistScreen {
    HOME,
    AI_ASSISTANT,
    CALL_ANALYSIS,
    REMINDERS_ALARMS,
    APPOINTMENTS,
    DAILY_PLANNER,
    VISUAL_SCANNER,
    MEDICATIONS,
    EMERGENCY_SOS,
    COMMAND_LOG
}

data class AssistantChatMessage(
    val id: Long = chatMessageIdGenerator.incrementAndGet(),
    val isUser: Boolean,
    val text: String,
    val actionBadge: String? = null
)

class AssistViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AssistDatabase.getInstance(application)
    private val repository = AssistRepository(database.assistDao())
    val speechHelper = AccessibilitySpeechHelper(application)

    val reminders: StateFlow<List<ReminderEntity>> = repository.reminders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val alarms: StateFlow<List<AlarmEntity>> = repository.alarms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val appointments: StateFlow<List<AppointmentEntity>> = repository.appointments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val callRecords: StateFlow<List<CallRecordEntity>> = repository.callRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val medications: StateFlow<List<MedicationEntity>> = repository.medications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val emergencyContacts: StateFlow<List<EmergencyContactEntity>> = repository.emergencyContacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val visualScans: StateFlow<List<VisualScanEntity>> = repository.visualScans.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _voiceCommandLogs = MutableStateFlow<List<VoiceCommandLogEntity>>(
        AssistRepository.defaultTenVoiceCommandLogs().sortedByDescending { it.createdAt }
    )
    val voiceCommandLogs: StateFlow<List<VoiceCommandLogEntity>> = _voiceCommandLogs.asStateFlow()

    private val _medicationHistoryLogs = MutableStateFlow<List<MedicationHistoryLogEntity>>(emptyList())
    val medicationHistoryLogs: StateFlow<List<MedicationHistoryLogEntity>> = _medicationHistoryLogs.asStateFlow()

    private val _currentScreen = MutableStateFlow(AssistScreen.HOME)
    val currentScreen: StateFlow<AssistScreen> = _currentScreen.asStateFlow()

    private val _contrastMode = MutableStateFlow(ContrastMode.HIGH_CONTRAST_LIGHT)
    val contrastMode: StateFlow<ContrastMode> = _contrastMode.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _autoSpeakEnabled = MutableStateFlow(true)
    val autoSpeakEnabled: StateFlow<Boolean> = _autoSpeakEnabled.asStateFlow()

    private val _slowSpeechEnabled = MutableStateFlow(false)
    val slowSpeechEnabled: StateFlow<Boolean> = _slowSpeechEnabled.asStateFlow()

    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing: StateFlow<Boolean> = _isAiProcessing.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _showVoiceOverlay = MutableStateFlow(false)
    val showVoiceOverlay: StateFlow<Boolean> = _showVoiceOverlay.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _preferredHighContrastPalette = MutableStateFlow(ContrastMode.ULTRA_LOW_VISION)
    val preferredHighContrastPalette: StateFlow<ContrastMode> = _preferredHighContrastPalette.asStateFlow()

    private val _assistantMessages = MutableStateFlow(
        listOf(
            AssistantChatMessage(
                id = 1L,
                isUser = false,
                text = "Hello! I am AssistAI. You can navigate or run actions by voice—say 'Open Visual Scanner', 'Open Medications', 'Open Emergency SOS', 'Open Daily Planner', 'Set a reminder for tomorrow at 10 AM', or 'I took my morning medicine'.",
                actionBadge = "SpeechRecognizer & Gemini AI Ready"
            )
        )
    )
    val assistantMessages: StateFlow<List<AssistantChatMessage>> = _assistantMessages.asStateFlow()

    private val _simplifiedExplanationResult = MutableStateFlow<String?>(null)
    val simplifiedExplanationResult: StateFlow<String?> = _simplifiedExplanationResult.asStateFlow()

    private val _medicationAiAnswer = MutableStateFlow<String?>(null)
    val medicationAiAnswer: StateFlow<String?> = _medicationAiAnswer.asStateFlow()

    private val _isScanningVisual = MutableStateFlow(false)
    val isScanningVisual: StateFlow<Boolean> = _isScanningVisual.asStateFlow()

    // Call Recording & Analysis states
    private val _isRecordingCall = MutableStateFlow(false)
    val isRecordingCall: StateFlow<Boolean> = _isRecordingCall.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _draftCallTranscript = MutableStateFlow("")
    val draftCallTranscript: StateFlow<String> = _draftCallTranscript.asStateFlow()

    private val _isAnalyzingCall = MutableStateFlow(false)
    val isAnalyzingCall: StateFlow<Boolean> = _isAnalyzingCall.asStateFlow()

    private val _selectedCallId = MutableStateFlow<Int?>(null)
    val selectedCallId: StateFlow<Int?> = _selectedCallId.asStateFlow()

    private var recordingTimerJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.ensureDemoDataSeeded()
            } catch (_: Exception) {
            }
        }
        viewModelScope.launch {
            repository.voiceCommandLogs.collect { logs ->
                if (logs.isNotEmpty() || _voiceCommandLogs.value.isEmpty()) {
                    _voiceCommandLogs.value = logs.take(10)
                }
            }
        }
        viewModelScope.launch {
            repository.medicationHistoryLogs.collect { logs ->
                if (logs.isNotEmpty() || _medicationHistoryLogs.value.isEmpty()) {
                    _medicationHistoryLogs.value = logs
                }
            }
        }
    }

    fun setShowVoiceOverlay(visible: Boolean) {
        _showVoiceOverlay.value = visible
        if (!visible && speechHelper.isListening.value) {
            speechHelper.stopSpeechRecognition()
        }
    }

    fun navigateTo(screen: AssistScreen) {
        speechHelper.triggerHapticFeedback()
        _currentScreen.value = screen
        _statusBannerMessage.value = null
    }

    fun navigateBackToHome() {
        speechHelper.triggerHapticFeedback()
        _currentScreen.value = AssistScreen.HOME
    }

    fun setShowSettingsDialog(visible: Boolean) {
        speechHelper.triggerHapticFeedback()
        _showSettingsDialog.value = visible
    }

    fun setContrastMode(mode: ContrastMode) {
        speechHelper.triggerCommandConfirmedHaptic()
        if (mode.isForcedHighContrast) {
            _preferredHighContrastPalette.value = mode
        }
        _contrastMode.value = mode
        val modeLabel = when (mode) {
            ContrastMode.HIGH_CONTRAST_LIGHT -> "Standard Daylight Color Mode enabled."
            ContrastMode.HIGH_CONTRAST_DARK -> "Standard Dark Color Mode enabled."
            ContrastMode.ULTRA_LOW_VISION -> "High Contrast Mode enabled: Black and Yellow Low-Vision Palette."
            ContrastMode.BLACK_AND_WHITE -> "High Contrast Mode enabled: Black and White Low-Vision Palette."
        }
        showBanner(modeLabel)
        if (_autoSpeakEnabled.value) {
            speechHelper.speak(modeLabel)
        }
    }

    fun toggleHighContrastMode(enabled: Boolean? = null) {
        val currentlyHighContrast = _contrastMode.value.isForcedHighContrast
        val shouldEnable = enabled ?: !currentlyHighContrast
        if (shouldEnable) {
            setContrastMode(_preferredHighContrastPalette.value)
        } else {
            setContrastMode(ContrastMode.HIGH_CONTRAST_LIGHT)
        }
    }

    fun cycleContrastMode() {
        val next = when (_contrastMode.value) {
            ContrastMode.HIGH_CONTRAST_LIGHT -> ContrastMode.ULTRA_LOW_VISION
            ContrastMode.ULTRA_LOW_VISION -> ContrastMode.BLACK_AND_WHITE
            ContrastMode.BLACK_AND_WHITE -> ContrastMode.HIGH_CONTRAST_DARK
            ContrastMode.HIGH_CONTRAST_DARK -> ContrastMode.HIGH_CONTRAST_LIGHT
        }
        setContrastMode(next)
    }

    fun cycleFontScale() {
        speechHelper.triggerHapticFeedback()
        val next = when (_fontScale.value) {
            1.0f -> 1.15f
            1.15f -> 1.3f
            else -> 1.0f
        }
        _fontScale.value = next
        val label = when (next) {
            1.0f -> "Large Text Size (100 percent)"
            1.15f -> "Extra Large Text Size (115 percent)"
            else -> "Maximum Accessible Text Size (130 percent)"
        }
        showBanner(label)
        if (_autoSpeakEnabled.value) {
            speechHelper.speak(label)
        }
    }

    fun toggleSlowSpeech() {
        val next = !_slowSpeechEnabled.value
        _slowSpeechEnabled.value = next
        speechHelper.setSlowSpeechRate(next)
        val msg = if (next) "Slow, extra-clear voice speed enabled." else "Normal voice speed enabled."
        showBanner(msg)
        speechHelper.speak(msg)
    }

    fun showBanner(message: String?) {
        _statusBannerMessage.value = message
    }

    fun speakText(text: String) {
        speechHelper.speak(text)
    }

    fun stopSpeaking() {
        speechHelper.stopSpeaking()
    }

    fun readCurrentScreenAloud() {
        if (speechHelper.isSpeaking.value) {
            speechHelper.stopSpeaking()
            return
        }
        val activeReminders = reminders.value.filter { !it.isCompleted }
        val activeAlarms = alarms.value.filter { it.isEnabled }
        val upcomingEvents = appointments.value
        val calls = callRecords.value
        val meds = medications.value
        val scans = visualScans.value
        val contacts = emergencyContacts.value

        val textToRead = when (_currentScreen.value) {
            AssistScreen.HOME -> {
                val batteryInfo = BatteryStatusHelper.readCurrentBatteryState(getApplication()).spokenSummary
                val nextItem = buildNextUpcomingSummary()
                "AssistAI Home Screen. $batteryInfo $nextItem. You have ${activeReminders.size} active reminders, ${upcomingEvents.size} upcoming events, and ${meds.count { !it.takenToday }} medications remaining today. Available features: AI Voice Assistant, Call Analysis and Scam Shield, Visual AI Camera Reader, Smart Medications, Reminders and Alarms, Appointments, Daily Planner, and Emergency SOS."
            }
            AssistScreen.AI_ASSISTANT -> {
                val lastMessage = _assistantMessages.value.lastOrNull()?.text ?: "Ready for your command."
                "AI Voice Assistant Screen. Last message: $lastMessage."
            }
            AssistScreen.CALL_ANALYSIS -> {
                val selected = calls.firstOrNull { it.id == _selectedCallId.value } ?: calls.firstOrNull()
                if (selected != null) {
                    "Call Recording and Scam Shield Screen. Recording: ${selected.title}. Scam Shield Status: ${selected.scamRiskLevel}. ${selected.scamWarningExplanation}. Summary: ${selected.summary}. ${selected.simpleExplanation}."
                } else {
                    "Call Recording and Scam Shield Screen. Record a call or upload audio to extract details and check for phone scams."
                }
            }
            AssistScreen.REMINDERS_ALARMS -> {
                val remText = if (activeReminders.isEmpty()) "No active reminders." else "Active reminders: " + activeReminders.joinToString("; ") { "${it.title}, ${it.dateText} at ${it.timeText}" }
                val alarmText = if (activeAlarms.isEmpty()) "No active alarms." else "Active alarms: " + activeAlarms.joinToString("; ") { "${it.timeText} for ${it.label} (${it.repeatDays})" }
                "Reminders and Alarms Screen. $remText. $alarmText."
            }
            AssistScreen.APPOINTMENTS -> {
                val apptText = if (upcomingEvents.isEmpty()) "No upcoming events." else "Upcoming events: " + upcomingEvents.joinToString("; ") { "${it.eventType}: ${it.title}, ${it.dateText} at ${it.timeText}, Location: ${it.location}" }
                "Appointments, Meetings, and Travel Screen. $apptText."
            }
            AssistScreen.DAILY_PLANNER -> {
                val remText = activeReminders.joinToString("; ") { "${it.title} at ${it.timeText}" }.ifBlank { "None" }
                val apptText = upcomingEvents.joinToString("; ") { "${it.title} (${it.dateText} at ${it.timeText})" }.ifBlank { "None" }
                val alarmText = activeAlarms.joinToString("; ") { "${it.timeText} ${it.label}" }.ifBlank { "None" }
                "Daily Planner Screen. Today's Reminders: $remText. Alarms: $alarmText. Appointments and Travel: $apptText."
            }
            AssistScreen.VISUAL_SCANNER -> {
                val latestScan = scans.firstOrNull()
                if (latestScan != null) {
                    "Visual AI Scanner Screen. Latest scan: ${latestScan.title}. ${latestScan.spokenDescription}. Extracted Text: ${latestScan.extractedLargeText}. ${latestScan.safetyOrActionNote}."
                } else {
                    "Visual AI Scanner Screen. Point your camera or pick a photo of a medicine bottle, utility bill, currency note, or room scene to hear it read aloud."
                }
            }
            AssistScreen.MEDICATIONS -> {
                val medSummary = meds.joinToString("; ") {
                    "${it.name}, ${it.dosage} at ${it.scheduleTime}, status: ${if (it.takenToday) "taken today" else "not taken yet"}"
                }
                val historyCount = _medicationHistoryLogs.value.size
                "Medication Manager Screen. Your daily pills: $medSummary. Caregiver history log has $historyCount recorded dose entries."
            }
            AssistScreen.EMERGENCY_SOS -> {
                val contactSummary = contacts.joinToString("; ") { "${it.name} (${it.relationship}) at ${it.phoneNumber}" }
                "Emergency SOS and Caregiver Hub. Medical ID: Blood Type O Positive, Allergies: Penicillin. Emergency Contacts: $contactSummary."
            }
            AssistScreen.COMMAND_LOG -> {
                val logs = _voiceCommandLogs.value.take(10)
                if (logs.isEmpty()) {
                    "Voice Command Log Screen. No recent voice commands recorded yet. Tap Speak Command to run an action."
                } else {
                    val itemsSummary = logs.take(5).mapIndexed { idx, item ->
                        "Command ${idx + 1}: You said '${item.spokenCommand}'. AssistAI action: ${item.aiActionTaken}."
                    }.joinToString(" ")
                    "Voice Command Log Screen showing your last ${logs.size} voice commands executed by AssistAI. $itemsSummary"
                }
            }
        }
        speechHelper.speak(textToRead)
    }

    fun buildNextUpcomingSummary(): String {
        val nextReminder = reminders.value.firstOrNull { !it.isCompleted }
        val nextAppointment = appointments.value.firstOrNull()
        return when {
            nextReminder != null && nextReminder.dateText.equals("Today", ignoreCase = true) ->
                "Next Today: ${nextReminder.title} at ${nextReminder.timeText}"
            nextAppointment != null ->
                "Next Event: ${nextAppointment.title} – ${nextAppointment.dateText} at ${nextAppointment.timeText}"
            nextReminder != null ->
                "Next Reminder: ${nextReminder.title} – ${nextReminder.dateText} at ${nextReminder.timeText}"
            else -> "All caught up! Tap the microphone to add a reminder or appointment."
        }
    }

    // --- SpeechRecognizer Voice Navigation & Command Execution ---
    fun startVoiceCommandListening(navigateToAssistant: Boolean = false) {
        if (navigateToAssistant) {
            _currentScreen.value = AssistScreen.AI_ASSISTANT
        }
        speechHelper.startSpeechRecognition(
            onFinalResult = { spokenText ->
                _showVoiceOverlay.value = false
                executeVoiceCommand(spokenText)
            },
            onErrorMessage = { err ->
                showBanner(err)
            }
        )
    }

    fun stopVoiceCommandListening() {
        speechHelper.stopSpeechRecognition()
    }

    fun testVoiceHapticPattern(pattern: VoiceHapticPattern) {
        speechHelper.hapticManager.triggerPattern(pattern)
        val msg = "Haptic Pattern Triggered: ${pattern.displayName} — ${pattern.tactileDescription}."
        showBanner(msg)
        if (_autoSpeakEnabled.value) {
            speechHelper.speak(msg)
        }
    }

    private val commandLogIdCounter = AtomicLong(50000L)

    fun recordExecutedVoiceCommand(
        spokenCommand: String,
        aiActionTaken: String,
        spokenConfirmation: String,
        intentCategory: String
    ) {
        val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val entry = VoiceCommandLogEntity(
            id = commandLogIdCounter.incrementAndGet().toInt(),
            spokenCommand = spokenCommand.trim(),
            aiActionTaken = aiActionTaken.trim(),
            spokenConfirmation = spokenConfirmation.trim(),
            intentCategory = intentCategory,
            executedAtText = "Today, $timeFormatted",
            createdAt = System.currentTimeMillis()
        )
        _voiceCommandLogs.value = (listOf(entry) + _voiceCommandLogs.value).take(10)
        viewModelScope.launch {
            repository.insertVoiceCommandLog(
                entry.copy(id = 0)
            )
        }
    }

    fun deleteVoiceCommandLog(log: VoiceCommandLogEntity) {
        speechHelper.triggerHapticFeedback()
        _voiceCommandLogs.value = _voiceCommandLogs.value.filterNot {
            it.id == log.id || (it.spokenCommand == log.spokenCommand && it.createdAt == log.createdAt)
        }
        viewModelScope.launch {
            repository.deleteVoiceCommandLog(log)
            showBanner("Removed command from Command Log.")
        }
    }

    fun clearVoiceCommandLogs() {
        speechHelper.triggerCommandConfirmedHaptic()
        _voiceCommandLogs.value = emptyList()
        viewModelScope.launch {
            repository.clearAllVoiceCommandLogs()
            val msg = "Cleared all voice command history."
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun restoreSampleVoiceCommandLogs() {
        speechHelper.triggerCommandConfirmedHaptic()
        val defaults = AssistRepository.defaultTenVoiceCommandLogs().sortedByDescending { it.createdAt }
        _voiceCommandLogs.value = defaults
        viewModelScope.launch {
            repository.clearAllVoiceCommandLogs()
            defaults.reversed().forEach {
                repository.insertVoiceCommandLog(it.copy(id = 0))
            }
            val msg = "Restored the last 10 AI voice commands in your Command Log."
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun executeVoiceCommand(rawCommand: String) {
        val command = rawCommand.trim()
        if (command.isBlank()) return

        _showVoiceOverlay.value = false
        speechHelper.triggerProcessingInputHaptic()

        _assistantMessages.value = _assistantMessages.value + AssistantChatMessage(
            isUser = true,
            text = command
        )

        when (val decision = SpeechToTextHandler.classifyVoiceTranscript(command)) {
            is VoiceRouteDecision.NavigateToScreen -> {
                _currentScreen.value = decision.destination
                val badge = "🧭 Navigated to ${decision.destination.name.replace('_', ' ')}"
                _assistantMessages.value = _assistantMessages.value + AssistantChatMessage(
                    isUser = false,
                    text = decision.spokenConfirmation,
                    actionBadge = badge
                )
                recordExecutedVoiceCommand(
                    spokenCommand = command,
                    aiActionTaken = badge,
                    spokenConfirmation = decision.spokenConfirmation,
                    intentCategory = "Navigation"
                )
                speechHelper.triggerCommandConfirmedHaptic()
                showBanner(decision.spokenConfirmation)
                if (_autoSpeakEnabled.value) {
                    speechHelper.speak(decision.spokenConfirmation)
                }
                return
            }

            is VoiceRouteDecision.AccessibilityAction -> {
                var confirmationText = decision.spokenConfirmation
                val actionBadge = when (decision.actionType) {
                    AccessibilityVoiceAction.READ_SCREEN_ALOUD -> {
                        readCurrentScreenAloud()
                        "🔊 Read Screen Aloud"
                    }
                    AccessibilityVoiceAction.STOP_SPEAKING -> {
                        stopSpeaking()
                        "⏹️ Stopped Audio Readout"
                    }
                    AccessibilityVoiceAction.CYCLE_CONTRAST -> {
                        cycleContrastMode()
                        "🎨 Switched High Contrast Mode"
                    }
                    AccessibilityVoiceAction.CYCLE_FONT_SIZE -> {
                        cycleFontScale()
                        "🔠 Adjusted Text Font Size"
                    }
                    AccessibilityVoiceAction.TOGGLE_SLOW_SPEECH -> {
                        toggleSlowSpeech()
                        "🐢 Toggled Voice Speed"
                    }
                    AccessibilityVoiceAction.READ_BATTERY_STATUS -> {
                        val batterySummary = BatteryStatusHelper.readCurrentBatteryState(getApplication()).spokenSummary
                        confirmationText = batterySummary
                        showBanner(batterySummary)
                        if (_autoSpeakEnabled.value) {
                            speechHelper.speak(batterySummary)
                        }
                        "🔋 Checked Battery Status"
                    }
                }
                _assistantMessages.value = _assistantMessages.value + AssistantChatMessage(
                    isUser = false,
                    text = confirmationText,
                    actionBadge = actionBadge
                )
                recordExecutedVoiceCommand(
                    spokenCommand = command,
                    aiActionTaken = actionBadge,
                    spokenConfirmation = confirmationText,
                    intentCategory = "Accessibility"
                )
                speechHelper.triggerCommandConfirmedHaptic()
                return
            }

            is VoiceRouteDecision.TakeMedicationDoseByVoice -> {
                val untaken = medications.value.filter { !it.takenToday }
                val target = if (decision.keyword.isNotBlank()) {
                    untaken.firstOrNull { it.name.contains(decision.keyword, ignoreCase = true) } ?: untaken.firstOrNull()
                } else {
                    untaken.firstOrNull()
                }
                if (target != null) {
                    toggleMedicationTakenToday(target, sourceMethod = "Voice Command")
                    recordExecutedVoiceCommand(
                        spokenCommand = command,
                        aiActionTaken = "💊 Marked Medication as Taken: ${target.name}",
                        spokenConfirmation = "Marked ${target.name} (${target.dosage}) as Taken today.",
                        intentCategory = "Medication"
                    )
                    speechHelper.triggerCommandConfirmedHaptic()
                } else {
                    val msg = "All of your scheduled medications for today have already been logged as taken!"
                    recordExecutedVoiceCommand(
                        spokenCommand = command,
                        aiActionTaken = "💊 Verified All Medications Taken Today",
                        spokenConfirmation = msg,
                        intentCategory = "Medication"
                    )
                    speechHelper.triggerCommandConfirmedHaptic()
                    showBanner(msg)
                    if (_autoSpeakEnabled.value) speechHelper.speak(msg)
                }
                return
            }

            is VoiceRouteDecision.CompleteReminderByVoice -> {
                viewModelScope.launch {
                    val activeList = reminders.value.filter { !it.isCompleted }
                    val target = activeList.firstOrNull {
                        it.title.contains(decision.keyword, ignoreCase = true)
                    } ?: activeList.firstOrNull()

                    if (target != null) {
                        repository.updateReminder(target.copy(isCompleted = true))
                        val reply = "Marked reminder '${target.title}' as completed!"
                        val badge = "✓ Completed: ${target.title}"
                        _assistantMessages.value = _assistantMessages.value + AssistantChatMessage(
                            isUser = false,
                            text = reply,
                            actionBadge = badge
                        )
                        recordExecutedVoiceCommand(
                            spokenCommand = command,
                            aiActionTaken = badge,
                            spokenConfirmation = reply,
                            intentCategory = "Reminder"
                        )
                        speechHelper.triggerCommandConfirmedHaptic()
                        showBanner(reply)
                        if (_autoSpeakEnabled.value) speechHelper.speak(reply)
                    } else {
                        val reply = "All your reminders are already marked complete!"
                        _assistantMessages.value = _assistantMessages.value + AssistantChatMessage(
                            isUser = false,
                            text = reply
                        )
                        recordExecutedVoiceCommand(
                            spokenCommand = command,
                            aiActionTaken = "✓ Checked Reminders (All Complete)",
                            spokenConfirmation = reply,
                            intentCategory = "Reminder"
                        )
                        speechHelper.triggerCommandConfirmedHaptic()
                        showBanner(reply)
                        if (_autoSpeakEnabled.value) speechHelper.speak(reply)
                    }
                }
                return
            }

            is VoiceRouteDecision.ExecuteAiCommand -> {
                // Proceed below
            }
        }

        viewModelScope.launch {
            _isAiProcessing.value = true
            val activeRem = reminders.value.filter { !it.isCompleted }
            val upcomingAppt = appointments.value
            val lastCall = callRecords.value.firstOrNull()

            val contextSummary = buildString {
                append("Active Reminders: ")
                if (activeRem.isEmpty()) append("None. ") else append(activeRem.joinToString("; ") { "${it.title} (${it.dateText} at ${it.timeText})" } + ". ")
                append("Appointments & Travel: ")
                if (upcomingAppt.isEmpty()) append("None. ") else append(upcomingAppt.joinToString("; ") { "${it.title} [${it.eventType}] on ${it.dateText} at ${it.timeText} at ${it.location}" } + ". ")
                if (lastCall != null) {
                    append("Last Call Recording ('${lastCall.title}'): ${lastCall.summary} ${lastCall.simpleExplanation}")
                }
            }

            val result = GeminiService.processVoiceCommand(command, contextSummary)
            var actionBadge: String? = null
            var finalReply = result.spokenReply
            var category = "AI Assistant"

            when (result.intentType) {
                "CREATE_REMINDER" -> {
                    val title = result.itemTitle.ifBlank { "Scheduled Reminder" }
                    val newRem = ReminderEntity(
                        title = title,
                        dateText = result.itemDate.ifBlank { "Tomorrow" },
                        timeText = result.itemTime.ifBlank { "10:00 AM" },
                        category = if (title.lowercase().contains("medicine") || title.lowercase().contains("pill")) "Medicine" else "Personal",
                        notes = result.itemNotes.ifBlank { "Created via Voice Assistant" },
                        isDaily = result.itemDate.equals("Daily", ignoreCase = true)
                    )
                    repository.insertReminder(newRem)
                    actionBadge = "✓ Saved Reminder: ${newRem.title} (${newRem.dateText} • ${newRem.timeText})"
                    category = "Reminder"
                    speechHelper.sendNotificationAlert("Reminder Set", "${newRem.title} – ${newRem.dateText} at ${newRem.timeText}")
                }
                "CREATE_APPOINTMENT" -> {
                    val title = result.itemTitle.ifBlank { "Scheduled Meeting" }
                    val newAppt = AppointmentEntity(
                        title = title,
                        eventType = result.itemEventType.ifBlank { "Appointment" },
                        dateText = result.itemDate.ifBlank { "Monday" },
                        timeText = result.itemTime.ifBlank { "3:00 PM" },
                        location = result.itemLocation.ifBlank { "Main Office / Clinic" },
                        notes = result.itemNotes.ifBlank { "Created via Voice Assistant" }
                    )
                    repository.insertAppointment(newAppt)
                    actionBadge = "✓ Saved ${newAppt.eventType}: ${newAppt.title} (${newAppt.dateText} • ${newAppt.timeText})"
                    category = "Appointment"
                    speechHelper.sendNotificationAlert("${newAppt.eventType} Scheduled", "${newAppt.title} on ${newAppt.dateText} at ${newAppt.timeText}")
                }
                "CREATE_ALARM" -> {
                    val label = result.itemTitle.ifBlank { "Voice Alarm" }
                    val newAlarm = AlarmEntity(
                        timeText = result.itemTime.ifBlank { "8:00 AM" },
                        label = label,
                        repeatDays = result.itemRepeatDays.ifBlank { "Daily" },
                        isEnabled = true
                    )
                    repository.insertAlarm(newAlarm)
                    actionBadge = "✓ Alarm Set: ${newAlarm.timeText} (${newAlarm.label})"
                    category = "Alarm"
                    speechHelper.sendNotificationAlert("Alarm Enabled", "${newAlarm.timeText} – ${newAlarm.label}")
                }
                "READ_REMINDERS" -> {
                    val listText = if (activeRem.isEmpty()) {
                        "You have no pending reminders right now."
                    } else {
                        "You have ${activeRem.size} active reminders: " + activeRem.joinToString(". ") { "${it.title}, ${it.dateText} at ${it.timeText}" }
                    }
                    finalReply = listText
                    actionBadge = "🔊 Read ${activeRem.size} Reminders"
                    category = "Reminder"
                }
                "READ_APPOINTMENTS" -> {
                    val listText = if (upcomingAppt.isEmpty()) {
                        "You have no upcoming appointments scheduled."
                    } else {
                        "Here are your upcoming events: " + upcomingAppt.joinToString(". ") { "${it.title} (${it.eventType}) on ${it.dateText} at ${it.timeText}, location: ${it.location}" }
                    }
                    finalReply = listText
                    actionBadge = "📅 Read ${upcomingAppt.size} Upcoming Events"
                    category = "Appointment"
                }
                "SUMMARIZE_LAST_CALL" -> {
                    if (lastCall != null) {
                        finalReply = "Summary of ${lastCall.title}: ${lastCall.summary} ${lastCall.simpleExplanation}"
                        actionBadge = "📞 Last Recording Summary"
                    } else {
                        finalReply = "You do not have any analyzed call recordings yet."
                        actionBadge = "📞 Checked Call Recordings"
                    }
                    category = "Call Summary"
                }
            }

            _assistantMessages.value = _assistantMessages.value + AssistantChatMessage(
                isUser = false,
                text = finalReply,
                actionBadge = actionBadge
            )
            recordExecutedVoiceCommand(
                spokenCommand = command,
                aiActionTaken = actionBadge ?: "🤖 AI Voice Response Provided",
                spokenConfirmation = finalReply,
                intentCategory = category
            )
            _isAiProcessing.value = false
            speechHelper.triggerCommandConfirmedHaptic()
            showBanner(actionBadge ?: finalReply.take(90))
            if (_autoSpeakEnabled.value) {
                speechHelper.speak(finalReply)
            }
        }
    }

    fun simplifyTextWithAi(complexText: String) {
        if (complexText.isBlank()) return
        viewModelScope.launch {
            _isAiProcessing.value = true
            speechHelper.triggerProcessingInputHaptic()
            val simplified = GeminiService.simplifyComplexText(complexText)
            _simplifiedExplanationResult.value = simplified
            _isAiProcessing.value = false
            speechHelper.triggerCommandConfirmedHaptic()
            if (_autoSpeakEnabled.value) {
                speechHelper.speak(simplified)
            }
        }
    }

    // --- Visual AI Camera & Label Scanner ---
    fun analyzeCameraBitmap(bitmap: Bitmap, scanTypeHint: String) {
        viewModelScope.launch {
            _isScanningVisual.value = true
            val (base64, mimeType) = speechHelper.bitmapToBase64(bitmap)
            val result = GeminiService.analyzeVisualSceneOrDocument(
                imageBase64 = base64,
                mimeType = mimeType,
                scanTypeHint = scanTypeHint,
                fallbackDescriptionPrompt = "Camera snapshot of $scanTypeHint"
            )
            saveAndAnnounceVisualScan(result)
            _isScanningVisual.value = false
        }
    }

    fun analyzePickedImageUri(uri: Uri, scanTypeHint: String) {
        viewModelScope.launch {
            _isScanningVisual.value = true
            val (base64, mimeType) = speechHelper.readImageUriAsBase64(uri)
            val result = GeminiService.analyzeVisualSceneOrDocument(
                imageBase64 = base64,
                mimeType = mimeType,
                scanTypeHint = scanTypeHint,
                fallbackDescriptionPrompt = "Picked photo of $scanTypeHint"
            )
            saveAndAnnounceVisualScan(result)
            _isScanningVisual.value = false
        }
    }

    fun analyzePresetVisualScenario(scanTypeHint: String, descriptionPrompt: String) {
        viewModelScope.launch {
            _isScanningVisual.value = true
            val result = GeminiService.analyzeVisualSceneOrDocument(
                imageBase64 = null,
                mimeType = null,
                scanTypeHint = scanTypeHint,
                fallbackDescriptionPrompt = descriptionPrompt
            )
            saveAndAnnounceVisualScan(result)
            _isScanningVisual.value = false
        }
    }

    private suspend fun saveAndAnnounceVisualScan(result: com.example.network.VisualScanAnalysisResult) {
        val entity = VisualScanEntity(
            title = result.title,
            scanType = result.scanType,
            spokenDescription = result.spokenDescription,
            extractedLargeText = result.extractedLargeText,
            safetyOrActionNote = result.safetyOrActionNote,
            suggestedReminderTitle = result.suggestedReminderTitle,
            suggestedReminderTime = result.suggestedReminderTime
        )
        repository.insertVisualScan(entity)
        val spoken = "${entity.title}. ${entity.spokenDescription} ${entity.safetyOrActionNote}"
        showBanner("Visual Scan Ready: ${entity.title}")
        if (_autoSpeakEnabled.value) {
            speechHelper.speak(spoken)
        }
    }

    fun saveVisualScanAsReminder(scan: VisualScanEntity) {
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = scan.suggestedReminderTitle,
                dateText = "Today",
                timeText = scan.suggestedReminderTime,
                category = if (scan.scanType.contains("Medicine", ignoreCase = true)) "Medicine" else "Task",
                notes = "${scan.extractedLargeText}. ${scan.safetyOrActionNote}"
            )
            repository.insertReminder(reminder)
            val msg = "Saved reminder '${reminder.title}' for ${reminder.timeText} from your Visual Scan."
            showBanner(msg)
            speechHelper.sendNotificationAlert("Saved from Visual AI Scan", msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun deleteVisualScan(scan: VisualScanEntity) {
        viewModelScope.launch {
            repository.deleteVisualScan(scan)
            showBanner("Deleted visual scan record.")
        }
    }

    // --- Medication Manager & Caregiver Intake History Log ---
    private val medHistoryIdCounter = AtomicLong(80000L)

    fun toggleMedicationTakenToday(
        med: MedicationEntity,
        sourceMethod: String = "Taken Toggle"
    ) {
        speechHelper.triggerCommandConfirmedHaptic()
        val newTaken = !med.takenToday
        val updatedCount = if (newTaken && med.pillsRemaining > 0) {
            med.pillsRemaining - 1
        } else if (!newTaken) {
            med.pillsRemaining + 1
        } else {
            med.pillsRemaining
        }
        val updated = med.copy(takenToday = newTaken, pillsRemaining = updatedCount)
        val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        val statusCode = if (newTaken) "TAKEN" else "UNDONE"
        val caregiverNote = if (newTaken) {
            "Caregiver Log: Verified ${med.dosage} taken (${med.scheduleTime} • ${med.withFoodInstruction}). $updatedCount pills remaining in bottle."
        } else {
            "Caregiver Log: Dose status reset to NOT TAKEN for today ($updatedCount pills remaining)."
        }

        val logEntry = MedicationHistoryLogEntity(
            id = medHistoryIdCounter.incrementAndGet().toInt(),
            medicationId = med.id,
            medicationName = med.name,
            dosage = med.dosage,
            scheduleTime = med.scheduleTime,
            status = statusCode,
            loggedAtText = "Today, $timeFormatted",
            loggedByMethod = sourceMethod,
            caregiverNote = caregiverNote,
            pillsRemainingAfter = updatedCount,
            createdAt = System.currentTimeMillis()
        )
        _medicationHistoryLogs.value = listOf(logEntry) + _medicationHistoryLogs.value

        val msg = if (newTaken) {
            "Marked ${med.name} (${med.dosage}) as Taken! Logged to Caregiver History Log. $updatedCount doses remaining."
        } else {
            "Marked ${med.name} as not taken yet today. Caregiver History Log updated."
        }
        showBanner(msg)
        if (_autoSpeakEnabled.value) speechHelper.speak(msg)

        viewModelScope.launch {
            repository.updateMedication(updated)
            repository.insertMedicationHistoryLog(logEntry.copy(id = 0))
        }
    }

    fun deleteMedicationHistoryLog(log: MedicationHistoryLogEntity) {
        speechHelper.triggerHapticFeedback()
        _medicationHistoryLogs.value = _medicationHistoryLogs.value.filterNot {
            it.id == log.id || (it.medicationName == log.medicationName && it.createdAt == log.createdAt)
        }
        viewModelScope.launch {
            repository.deleteMedicationHistoryLog(log)
            showBanner("Removed entry from Caregiver History Log.")
        }
    }

    fun clearMedicationHistoryLogs() {
        speechHelper.triggerCommandConfirmedHaptic()
        _medicationHistoryLogs.value = emptyList()
        viewModelScope.launch {
            repository.clearAllMedicationHistoryLogs()
            val msg = "Cleared Caregiver Medication History Log."
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun readCaregiverHistoryReportAloud() {
        speechHelper.triggerCommandConfirmedHaptic()
        val meds = medications.value
        val logs = _medicationHistoryLogs.value
        val takenCount = meds.count { it.takenToday }
        val primaryCaregiver = emergencyContacts.value.firstOrNull { it.isPrimaryCaregiver }?.name
            ?: "Sarah Miller (Primary Caregiver)"
        val latestEntries = logs.take(4).joinToString(" ") {
            "${it.medicationName} (${it.dosage}) marked ${it.status} at ${it.loggedAtText} via ${it.loggedByMethod}."
        }.ifBlank { "No intake logs recorded yet." }
        val report = "Caregiver Medication Report for $primaryCaregiver. Today's adherence: $takenCount of ${meds.size} daily medications taken. Recent History Log (${logs.size} total entries): $latestEntries"
        showBanner("🔊 Reading Caregiver Medication History Log aloud…")
        speechHelper.speak(report)
    }

    fun sendCaregiverReportUpdate() {
        speechHelper.triggerCommandConfirmedHaptic()
        val meds = medications.value
        val takenCount = meds.count { it.takenToday }
        val primaryCaregiver = emergencyContacts.value.firstOrNull { it.isPrimaryCaregiver }
        val caregiverLabel = primaryCaregiver?.name ?: "Sarah Miller (Daughter & Primary Caregiver)"
        val latestLog = _medicationHistoryLogs.value.firstOrNull()
        val latestDetail = if (latestLog != null) {
            "Latest entry: ${latestLog.medicationName} (${latestLog.status}) at ${latestLog.loggedAtText}."
        } else {
            "All scheduled medications tracked."
        }
        val msg = "Caregiver Intake Summary ready for $caregiverLabel: $takenCount/${meds.size} doses taken today. $latestDetail"
        speechHelper.sendNotificationAlert("Caregiver Medication Log Shared", msg)
        showBanner("📤 Caregiver Log Summary prepared for $caregiverLabel!")
        if (_autoSpeakEnabled.value) speechHelper.speak(msg)
    }

    fun addDailyMedicationScheduleFromReminders(
        name: String,
        dosage: String,
        scheduleTime: String,
        withFoodInstruction: String
    ) {
        if (name.isBlank()) return
        speechHelper.triggerCommandConfirmedHaptic()
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanDosage = dosage.ifBlank { "1 Tablet" }.trim()
            val cleanTime = scheduleTime.ifBlank { "8:00 AM" }.trim()
            val cleanFood = withFoodInstruction.ifBlank { "Take with water" }.trim()

            val med = MedicationEntity(
                name = cleanName,
                dosage = cleanDosage,
                scheduleTime = cleanTime,
                withFoodInstruction = cleanFood,
                purposeSimple = "Scheduled via Daily Medication Reminders",
                pillsRemaining = 30,
                takenToday = false
            )
            repository.insertMedication(med)

            val medReminder = ReminderEntity(
                title = "Take $cleanName ($cleanDosage)",
                dateText = "Daily",
                timeText = cleanTime,
                category = "Medicine",
                notes = cleanFood,
                isCompleted = false,
                isDaily = true
            )
            repository.insertReminder(medReminder)

            val msg = "Daily medication schedule saved: $cleanName ($cleanDosage) at $cleanTime ($cleanFood)."
            showBanner(msg)
            speechHelper.sendNotificationAlert("Daily Medication Scheduled", msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun addMedication(
        name: String,
        dosage: String,
        scheduleTime: String,
        withFoodInstruction: String,
        purposeSimple: String,
        pillsRemaining: Int
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val med = MedicationEntity(
                name = name.trim(),
                dosage = dosage.ifBlank { "1 Tablet" },
                scheduleTime = scheduleTime.ifBlank { "9:00 AM" },
                withFoodInstruction = withFoodInstruction.ifBlank { "Take with water" },
                purposeSimple = purposeSimple.ifBlank { "Daily prescribed medication." },
                pillsRemaining = pillsRemaining.coerceAtLeast(1)
            )
            repository.insertMedication(med)
            val msg = "Added ${med.name} (${med.dosage}) at ${med.scheduleTime} to your Smart Pillbox."
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun deleteMedication(med: MedicationEntity) {
        viewModelScope.launch {
            repository.deleteMedication(med)
            showBanner("Removed ${med.name} from medication list.")
        }
    }

    fun askMedicationSafetyQuestion(question: String) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isAiProcessing.value = true
            val medsSummary = medications.value.joinToString("; ") { "${it.name} (${it.dosage}, ${it.scheduleTime})" }
            val answer = GeminiService.checkMedicationQuestion(question, medsSummary)
            _medicationAiAnswer.value = answer
            _isAiProcessing.value = false
            if (_autoSpeakEnabled.value) speechHelper.speak(answer)
        }
    }

    // --- Emergency SOS & Caregiver Contacts ---
    fun addEmergencyContact(name: String, relationship: String, phoneNumber: String, notes: String) {
        if (name.isBlank() || phoneNumber.isBlank()) return
        viewModelScope.launch {
            val contact = EmergencyContactEntity(
                name = name.trim(),
                relationship = relationship.ifBlank { "Family / Caregiver" },
                phoneNumber = phoneNumber.trim(),
                isPrimaryCaregiver = emergencyContacts.value.isEmpty(),
                notes = notes.trim()
            )
            repository.insertEmergencyContact(contact)
            val msg = "Added emergency contact: ${contact.name} (${contact.phoneNumber})."
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun deleteEmergencyContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(contact)
            showBanner("Removed contact ${contact.name}.")
        }
    }

    fun triggerAudibleSosAlert() {
        speechHelper.triggerEmergencyAlertHaptic()
        val primary = emergencyContacts.value.firstOrNull { it.isPrimaryCaregiver }
            ?: emergencyContacts.value.firstOrNull()
        val sosMessage = "EMERGENCY ALERT ACTIVATED! AssistAI Medical ID: Arthur Miller, Blood Type O Positive, Allergy to Penicillin. Primary Caregiver: ${primary?.name ?: "Sarah Miller"} at ${primary?.phoneNumber ?: "555-0911"}. Please assist this person or tap Call Primary Caregiver on the screen."
        speechHelper.sendNotificationAlert("🆘 EMERGENCY SOS ACTIVE", sosMessage)
        showBanner("🆘 Audible SOS Beacon Activated! Reading Medical ID aloud.")
        speechHelper.speak(sosMessage)
    }

    fun triggerImmediateSosCall(contactName: String, phoneNumber: String) {
        speechHelper.triggerEmergencyAlertHaptic()
        val cleanName = contactName.ifBlank { "Sarah Miller (Primary Caregiver)" }
        val cleanPhone = phoneNumber.ifBlank { "555-0911" }
        val callMessage = "SOS Emergency Activated! Placing immediate call to $cleanName at $cleanPhone."
        speechHelper.sendNotificationAlert("🆘 SOS EMERGENCY CALL", callMessage)
        showBanner("🆘 SOS Calling $cleanName ($cleanPhone)...")
        speechHelper.speak(callMessage)
    }

    // --- Call Recording & Analysis ---
    fun updateDraftCallTranscript(text: String) {
        _draftCallTranscript.value = text
    }

    fun selectCallRecord(id: Int) {
        _selectedCallId.value = id
    }

    fun startCallRecordingSession() {
        speechHelper.stopSpeaking()
        _isRecordingCall.value = true
        _recordingSeconds.value = 0
        val file = speechHelper.startAudioFileRecording()
        if (file == null) {
            speechHelper.startSpeechRecognition(
                onFinalResult = { recognized ->
                    val current = _draftCallTranscript.value
                    _draftCallTranscript.value = if (current.isBlank()) recognized else "$current $recognized"
                },
                onErrorMessage = { }
            )
        }
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_isRecordingCall.value) {
                delay(1000L)
                _recordingSeconds.value += 1
            }
        }
        showBanner("Recording call audio… Speak clearly or tap Stop & Analyze when finished.")
    }

    fun stopAndAnalyzeCallRecording() {
        _isRecordingCall.value = false
        recordingTimerJob?.cancel()
        speechHelper.stopSpeechRecognition()
        val (audioBase64, audioMime) = speechHelper.stopAudioFileRecording()
        val durationSec = _recordingSeconds.value.coerceAtLeast(12)
        val durationFormatted = "${durationSec / 60}m ${durationSec % 60}s"

        val transcriptToAnalyze = _draftCallTranscript.value.ifBlank {
            speechHelper.livePartialTranscript.value.ifBlank {
                "Hello Mrs. Eleanor, this is Pharmacy Care calling from Greenleaf Pharmacy at 450 Main Street. Your prescription refill for Lisinopril is ready for pickup Tomorrow at 2:30 PM. Please bring your Medicare card and take 1 tablet every morning with breakfast. Call us at 555-0319 if you need free home delivery."
            }
        }

        analyzeCallInternal(
            rawTranscript = transcriptToAnalyze,
            durationText = durationFormatted,
            audioBase64 = audioBase64,
            audioMimeType = audioMime
        )
    }

    fun analyzeUploadedAudioUri(uri: Uri) {
        viewModelScope.launch {
            _isAnalyzingCall.value = true
            val (base64, mimeType, fileName) = speechHelper.readAudioUriAsBase64(uri)
            val fallbackTranscript = _draftCallTranscript.value.ifBlank {
                "Uploaded audio recording ($fileName): Hello! This is Dr. Miller's Eye Clinic at Oakwood Vision Center, Room 110. We are confirming your annual eye exam for Monday at 11:00 AM. Please bring your current eyeglasses and arrange a ride home because we will use dilating eye drops. Our clinic phone number is 555-0477."
            }
            analyzeCallInternal(
                rawTranscript = fallbackTranscript,
                durationText = "Uploaded Audio",
                audioBase64 = base64,
                audioMimeType = mimeType
            )
        }
    }

    fun analyzeSampleOrCustomTranscript(transcript: String, durationText: String = "1m 30s") {
        _draftCallTranscript.value = transcript
        analyzeCallInternal(
            rawTranscript = transcript,
            durationText = durationText,
            audioBase64 = null,
            audioMimeType = null
        )
    }

    private fun analyzeCallInternal(
        rawTranscript: String,
        durationText: String,
        audioBase64: String?,
        audioMimeType: String?
    ) {
        viewModelScope.launch {
            _isAnalyzingCall.value = true
            val result = GeminiService.analyzeCallOrAudio(
                rawTranscript = rawTranscript,
                audioBase64 = audioBase64,
                audioMimeType = audioMimeType
            )
            val nowFormatted = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
            val record = CallRecordEntity(
                title = result.title,
                recordedAtText = "Today, $nowFormatted",
                durationText = durationText,
                transcript = result.transcript,
                summary = result.summary,
                simpleExplanation = result.simpleExplanation,
                extractedNames = result.names,
                extractedDates = result.dates,
                extractedTimes = result.times,
                extractedLocations = result.locations,
                extractedPhones = result.phones,
                extractedTasks = result.tasks,
                extractedInstructions = result.instructions,
                detectedCategory = result.detectedCategory,
                suggestedActionTitle = result.suggestedTitle,
                suggestedActionDate = result.suggestedDate,
                suggestedActionTime = result.suggestedTime,
                suggestedActionLocation = result.suggestedLocation,
                suggestedActionNotes = result.suggestedNotes,
                scamRiskLevel = result.scamRiskLevel,
                scamWarningExplanation = result.scamWarningExplanation,
                isSavedToPlanner = false
            )
            val newId = repository.insertCallRecord(record).toInt()
            _selectedCallId.value = newId
            _isAnalyzingCall.value = false
            _draftCallTranscript.value = ""

            val spoken = if (record.scamRiskLevel == "HIGH_SCAM_RISK") {
                "Warning! High Scam Risk Detected! ${record.scamWarningExplanation}"
            } else {
                "Call analysis complete for ${record.title}. ${record.summary} ${record.simpleExplanation}"
            }
            showBanner(
                if (record.scamRiskLevel == "HIGH_SCAM_RISK") {
                    "🚨 SCAM ALERT: Do not share personal info or money!"
                } else {
                    "Call analyzed! Detected ${record.detectedCategory}: ${record.suggestedActionTitle}"
                }
            )
            if (_autoSpeakEnabled.value) {
                speechHelper.speak(spoken)
            }
        }
    }

    fun saveCallSuggestionAsReminder(call: CallRecordEntity) {
        viewModelScope.launch {
            val reminder = ReminderEntity(
                title = call.suggestedActionTitle,
                dateText = call.suggestedActionDate,
                timeText = call.suggestedActionTime,
                category = "Call",
                notes = "${call.suggestedActionNotes} (Location: ${call.suggestedActionLocation})"
            )
            repository.insertReminder(reminder)
            repository.updateCallRecord(call.copy(isSavedToPlanner = true))
            val msg = "Saved '${reminder.title}' for ${reminder.dateText} at ${reminder.timeText} to your Reminders."
            showBanner(msg)
            speechHelper.sendNotificationAlert("Saved from Call Analysis", msg)
            if (_autoSpeakEnabled.value) {
                speechHelper.speak(msg)
            }
        }
    }

    fun saveCallSuggestionAsAppointment(call: CallRecordEntity) {
        viewModelScope.launch {
            val eventType = if (call.detectedCategory in listOf("Appointment", "Meeting", "Travel")) {
                call.detectedCategory
            } else {
                "Appointment"
            }
            val appointment = AppointmentEntity(
                title = call.suggestedActionTitle,
                eventType = eventType,
                dateText = call.suggestedActionDate,
                timeText = call.suggestedActionTime,
                location = call.suggestedActionLocation,
                notes = call.suggestedActionNotes,
                sourceCallId = call.id
            )
            repository.insertAppointment(appointment)
            repository.updateCallRecord(call.copy(isSavedToPlanner = true))
            val msg = "Saved $eventType '${appointment.title}' on ${appointment.dateText} at ${appointment.timeText} to your Calendar & Daily Planner."
            showBanner(msg)
            speechHelper.sendNotificationAlert("Event Added from Call", msg)
            if (_autoSpeakEnabled.value) {
                speechHelper.speak(msg)
            }
        }
    }

    fun deleteCallRecord(call: CallRecordEntity) {
        viewModelScope.launch {
            repository.deleteCallRecord(call)
            showBanner("Call recording deleted.")
        }
    }

    // --- Reminders CRUD ---
    fun addOrUpdateReminder(
        existingId: Int = 0,
        title: String,
        dateText: String,
        timeText: String,
        category: String,
        notes: String,
        isDaily: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val entity = ReminderEntity(
                id = existingId,
                title = title.trim(),
                dateText = dateText.ifBlank { "Today" },
                timeText = timeText.ifBlank { "9:00 AM" },
                category = category.ifBlank { "Personal" },
                notes = notes.trim(),
                isDaily = isDaily
            )
            if (existingId == 0) {
                repository.insertReminder(entity)
                val msg = "Reminder saved: ${entity.title} for ${entity.dateText} at ${entity.timeText}."
                showBanner(msg)
                speechHelper.sendNotificationAlert("New Reminder", msg)
                if (_autoSpeakEnabled.value) speechHelper.speak(msg)
            } else {
                repository.updateReminder(entity)
                val msg = "Reminder updated: ${entity.title}."
                showBanner(msg)
                if (_autoSpeakEnabled.value) speechHelper.speak(msg)
            }
        }
    }

    fun toggleReminderCompleted(reminder: ReminderEntity) {
        speechHelper.triggerCommandConfirmedHaptic()
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            repository.updateReminder(updated)
            val isMedicine = updated.category.equals("Medicine", ignoreCase = true) ||
                updated.title.contains("medicine", ignoreCase = true) ||
                updated.title.contains("pill", ignoreCase = true) ||
                updated.title.contains("take ", ignoreCase = true)
            val msg = if (updated.isCompleted) {
                if (isMedicine) {
                    "Marked '${updated.title}' as Taken! Haptic confirmation triggered."
                } else {
                    "Completed reminder: ${updated.title}. Great job!"
                }
            } else {
                "Marked ${updated.title} as active."
            }
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            val msg = "Deleted reminder: ${reminder.title}."
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    // --- Alarms CRUD ---
    fun addAlarm(timeText: String, label: String, repeatDays: String) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                timeText = timeText.ifBlank { "8:00 AM" },
                label = label.ifBlank { "Daily Alarm" },
                repeatDays = repeatDays.ifBlank { "Daily" },
                isEnabled = true
            )
            repository.insertAlarm(alarm)
            val msg = "Alarm set for ${alarm.timeText} (${alarm.repeatDays}): ${alarm.label}."
            showBanner(msg)
            speechHelper.sendNotificationAlert("Alarm Created", msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun toggleAlarmEnabled(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            val msg = if (updated.isEnabled) {
                "Alarm for ${updated.timeText} (${updated.label}) turned ON."
            } else {
                "Alarm for ${updated.timeText} (${updated.label}) turned OFF."
            }
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            showBanner("Deleted alarm for ${alarm.timeText}.")
        }
    }

    // --- Appointments / Meetings / Travel CRUD ---
    fun addOrUpdateAppointment(
        existingId: Int = 0,
        title: String,
        eventType: String,
        dateText: String,
        timeText: String,
        location: String,
        notes: String
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val entity = AppointmentEntity(
                id = existingId,
                title = title.trim(),
                eventType = eventType.ifBlank { "Appointment" },
                dateText = dateText.ifBlank { "Tomorrow" },
                timeText = timeText.ifBlank { "10:00 AM" },
                location = location.ifBlank { "To be confirmed" },
                notes = notes.trim()
            )
            if (existingId == 0) {
                repository.insertAppointment(entity)
                val msg = "${entity.eventType} saved: ${entity.title} on ${entity.dateText} at ${entity.timeText}."
                showBanner(msg)
                speechHelper.sendNotificationAlert("${entity.eventType} Scheduled", msg)
                if (_autoSpeakEnabled.value) speechHelper.speak(msg)
            } else {
                repository.updateAppointment(entity)
                val msg = "Updated ${entity.title}."
                showBanner(msg)
                if (_autoSpeakEnabled.value) speechHelper.speak(msg)
            }
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch {
            repository.deleteAppointment(appointment)
            val msg = "Removed ${appointment.title} from your schedule."
            showBanner(msg)
            if (_autoSpeakEnabled.value) speechHelper.speak(msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper.shutdown()
    }
}
