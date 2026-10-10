package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AccessibilitySettingsDialog
import com.example.ui.components.AccessibleBottomBar
import com.example.ui.components.AccessibleTopBar
import com.example.ui.components.GlobalVoiceControlOverlay
import com.example.ui.screens.AppointmentsScreen
import com.example.ui.screens.CallAnalysisScreen
import com.example.ui.screens.CommandLogScreen
import com.example.ui.screens.DailyPlannerScreen
import com.example.ui.screens.EmergencySosScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MedicationAndHealthScreen
import com.example.ui.screens.RemindersAndAlarmsScreen
import com.example.ui.screens.VisualAiScannerScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.AssistAITheme
import com.example.viewmodel.AssistScreen
import com.example.viewmodel.AssistViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val assistViewModel: AssistViewModel = viewModel()
            val contrastMode by assistViewModel.contrastMode.collectAsStateWithLifecycle()
            val fontScale by assistViewModel.fontScale.collectAsStateWithLifecycle()

            AssistAITheme(
                contrastMode = contrastMode,
                fontScale = fontScale
            ) {
                AssistAIMainApp(viewModel = assistViewModel)
            }
        }
    }
}

@Composable
fun AssistAIMainApp(viewModel: AssistViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val appointments by viewModel.appointments.collectAsStateWithLifecycle()
    val callRecords by viewModel.callRecords.collectAsStateWithLifecycle()
    val medications by viewModel.medications.collectAsStateWithLifecycle()
    val emergencyContacts by viewModel.emergencyContacts.collectAsStateWithLifecycle()
    val visualScans by viewModel.visualScans.collectAsStateWithLifecycle()
    val voiceCommandLogs by viewModel.voiceCommandLogs.collectAsStateWithLifecycle()
    val medicationHistoryLogs by viewModel.medicationHistoryLogs.collectAsStateWithLifecycle()

    val assistantMessages by viewModel.assistantMessages.collectAsStateWithLifecycle()
    val isAiProcessing by viewModel.isAiProcessing.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val simplifiedExplanation by viewModel.simplifiedExplanationResult.collectAsStateWithLifecycle()
    val medicationAiAnswer by viewModel.medicationAiAnswer.collectAsStateWithLifecycle()
    val isScanningVisual by viewModel.isScanningVisual.collectAsStateWithLifecycle()
    val slowSpeechEnabled by viewModel.slowSpeechEnabled.collectAsStateWithLifecycle()
    val showVoiceOverlay by viewModel.showVoiceOverlay.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val contrastMode by viewModel.contrastMode.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    val isSpeaking by viewModel.speechHelper.isSpeaking.collectAsStateWithLifecycle()
    val isListening by viewModel.speechHelper.isListening.collectAsStateWithLifecycle()
    val sttStatus by viewModel.speechHelper.sttStatus.collectAsStateWithLifecycle()
    val rmsDbLevel by viewModel.speechHelper.rmsDbLevel.collectAsStateWithLifecycle()
    val livePartialTranscript by viewModel.speechHelper.livePartialTranscript.collectAsStateWithLifecycle()
    val lastHapticPattern by viewModel.speechHelper.lastHapticPattern.collectAsStateWithLifecycle()

    val isRecordingCall by viewModel.isRecordingCall.collectAsStateWithLifecycle()
    val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    val draftCallTranscript by viewModel.draftCallTranscript.collectAsStateWithLifecycle()
    val isAnalyzingCall by viewModel.isAnalyzingCall.collectAsStateWithLifecycle()
    val selectedCallId by viewModel.selectedCallId.collectAsStateWithLifecycle()

    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingVoiceAction?.invoke()
        pendingVoiceAction = null
    }

    fun runWithAudioPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingVoiceAction = action
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val screenTitle = when (currentScreen) {
        AssistScreen.HOME -> "AssistAI"
        AssistScreen.AI_ASSISTANT -> "AI Voice Assistant"
        AssistScreen.CALL_ANALYSIS -> "Call & Scam Shield"
        AssistScreen.REMINDERS_ALARMS -> "Reminders & Alarms"
        AssistScreen.APPOINTMENTS -> "Appointments & Travel"
        AssistScreen.DAILY_PLANNER -> "Daily Planner"
        AssistScreen.VISUAL_SCANNER -> "Visual AI Reader"
        AssistScreen.MEDICATIONS -> "Medication Manager"
        AssistScreen.EMERGENCY_SOS -> "Emergency & SOS"
        AssistScreen.COMMAND_LOG -> "Command Log"
    }

    val nextUpcomingSummary = remember(reminders, appointments) {
        viewModel.buildNextUpcomingSummary()
    }

    if (showVoiceOverlay) {
        GlobalVoiceControlOverlay(
            isListening = isListening,
            sttStatus = sttStatus,
            rmsDbLevel = rmsDbLevel,
            partialTranscript = livePartialTranscript,
            lastHapticPattern = lastHapticPattern,
            onToggleMic = {
                if (isListening) {
                    viewModel.stopVoiceCommandListening()
                } else {
                    runWithAudioPermission {
                        viewModel.startVoiceCommandListening(navigateToAssistant = false)
                    }
                }
            },
            onExecuteVoicePhrase = { phrase ->
                viewModel.executeVoiceCommand(phrase)
            },
            onDismiss = {
                viewModel.setShowVoiceOverlay(false)
            }
        )
    }

    if (showSettingsDialog) {
        AccessibilitySettingsDialog(
            contrastMode = contrastMode,
            fontScale = fontScale,
            slowSpeechEnabled = slowSpeechEnabled,
            onToggleHighContrast = { enabled ->
                viewModel.toggleHighContrastMode(enabled)
            },
            onSelectContrastMode = { mode ->
                viewModel.setContrastMode(mode)
            },
            onCycleFontSize = { viewModel.cycleFontScale() },
            onToggleSlowSpeech = { viewModel.toggleSlowSpeech() },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AccessibleTopBar(
                screenTitle = screenTitle,
                showBackButton = currentScreen != AssistScreen.HOME,
                isSpeaking = isSpeaking,
                isListening = isListening,
                slowSpeechEnabled = slowSpeechEnabled,
                onBackClick = { viewModel.navigateBackToHome() },
                onReadScreenAloud = { viewModel.readCurrentScreenAloud() },
                onOpenVoiceControl = {
                    viewModel.setShowVoiceOverlay(true)
                    runWithAudioPermission {
                        viewModel.startVoiceCommandListening(navigateToAssistant = false)
                    }
                },
                onCycleContrast = { viewModel.cycleContrastMode() },
                onCycleFontSize = { viewModel.cycleFontScale() },
                onToggleSlowSpeech = { viewModel.toggleSlowSpeech() },
                onOpenSettings = { viewModel.setShowSettingsDialog(true) }
            )
        },
        bottomBar = {
            AccessibleBottomBar(
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
            ) {
                // Status Feedback Banner
                AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("status_feedback_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = statusBanner.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.showBanner(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss notification banner"
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        AssistScreen.HOME -> {
                            HomeScreen(
                                reminders = reminders,
                                alarms = alarms,
                                appointments = appointments,
                                callRecords = callRecords,
                                emergencyContacts = emergencyContacts,
                                nextUpcomingSummary = nextUpcomingSummary,
                                isListening = isListening,
                                livePartialTranscript = livePartialTranscript,
                                onMicClick = {
                                    viewModel.setShowVoiceOverlay(true)
                                    runWithAudioPermission {
                                        viewModel.startVoiceCommandListening(navigateToAssistant = false)
                                    }
                                },
                                onNavigate = { viewModel.navigateTo(it) },
                                onSpeakText = { viewModel.speakText(it) },
                                onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                                onTriggerImmediateSosCall = { name, phone ->
                                    viewModel.triggerImmediateSosCall(name, phone)
                                },
                                commandLogsCount = voiceCommandLogs.size
                            )
                        }

                        AssistScreen.AI_ASSISTANT -> {
                            VoiceAssistantScreen(
                                messages = assistantMessages,
                                isListening = isListening,
                                livePartialTranscript = livePartialTranscript,
                                isAiProcessing = isAiProcessing,
                                simplifiedExplanationResult = simplifiedExplanation,
                                lastHapticPattern = lastHapticPattern,
                                onMicClick = {
                                    if (isListening) {
                                        viewModel.stopVoiceCommandListening()
                                    } else {
                                        runWithAudioPermission {
                                            viewModel.startVoiceCommandListening(navigateToAssistant = false)
                                        }
                                    }
                                },
                                onExecuteCommand = { viewModel.executeVoiceCommand(it) },
                                onSimplifyText = { viewModel.simplifyTextWithAi(it) },
                                onSpeakText = { viewModel.speakText(it) },
                                onTestHapticPattern = { viewModel.testVoiceHapticPattern(it) },
                                onOpenCommandLog = { viewModel.navigateTo(AssistScreen.COMMAND_LOG) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.CALL_ANALYSIS -> {
                            CallAnalysisScreen(
                                callRecords = callRecords,
                                selectedCallId = selectedCallId,
                                isRecordingCall = isRecordingCall,
                                recordingSeconds = recordingSeconds,
                                draftTranscript = draftCallTranscript,
                                isAnalyzingCall = isAnalyzingCall,
                                onUpdateDraftTranscript = { viewModel.updateDraftCallTranscript(it) },
                                onStartRecording = {
                                    runWithAudioPermission {
                                        viewModel.startCallRecordingSession()
                                    }
                                },
                                onStopAndAnalyzeRecording = { viewModel.stopAndAnalyzeCallRecording() },
                                onUploadAudioUri = { viewModel.analyzeUploadedAudioUri(it) },
                                onAnalyzeSampleTranscript = { viewModel.analyzeSampleOrCustomTranscript(it) },
                                onSelectCallRecord = { viewModel.selectCallRecord(it) },
                                onSaveAsAppointment = { viewModel.saveCallSuggestionAsAppointment(it) },
                                onSaveAsReminder = { viewModel.saveCallSuggestionAsReminder(it) },
                                onDeleteCallRecord = { viewModel.deleteCallRecord(it) },
                                onSpeakText = { viewModel.speakText(it) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.REMINDERS_ALARMS -> {
                            RemindersAndAlarmsScreen(
                                reminders = reminders,
                                alarms = alarms,
                                medications = medications,
                                onSaveReminder = { id, title, date, time, cat, notes, daily ->
                                    viewModel.addOrUpdateReminder(id, title, date, time, cat, notes, daily)
                                },
                                onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                                onDeleteReminder = { viewModel.deleteReminder(it) },
                                onToggleMedicationTaken = { viewModel.toggleMedicationTakenToday(it) },
                                onAddDailyMedicationSchedule = { name, dosage, time, food ->
                                    viewModel.addDailyMedicationScheduleFromReminders(name, dosage, time, food)
                                },
                                onAddAlarm = { time, label, repeat ->
                                    viewModel.addAlarm(time, label, repeat)
                                },
                                onToggleAlarm = { viewModel.toggleAlarmEnabled(it) },
                                onDeleteAlarm = { viewModel.deleteAlarm(it) },
                                onStartVoiceCommand = {
                                    viewModel.setShowVoiceOverlay(true)
                                    runWithAudioPermission {
                                        viewModel.startVoiceCommandListening(navigateToAssistant = false)
                                    }
                                },
                                onSpeakText = { viewModel.speakText(it) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.APPOINTMENTS -> {
                            AppointmentsScreen(
                                appointments = appointments,
                                callRecords = callRecords,
                                onSaveAppointment = { id, title, type, date, time, loc, notes ->
                                    viewModel.addOrUpdateAppointment(id, title, type, date, time, loc, notes)
                                },
                                onDeleteAppointment = { viewModel.deleteAppointment(it) },
                                onSaveCallSuggestionAsAppointment = { viewModel.saveCallSuggestionAsAppointment(it) },
                                onOpenCallAnalysis = { viewModel.navigateTo(AssistScreen.CALL_ANALYSIS) },
                                onSpeakText = { viewModel.speakText(it) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.DAILY_PLANNER -> {
                            DailyPlannerScreen(
                                reminders = reminders,
                                alarms = alarms,
                                appointments = appointments,
                                callRecords = callRecords,
                                onToggleReminder = { viewModel.toggleReminderCompleted(it) },
                                onToggleAlarm = { viewModel.toggleAlarmEnabled(it) },
                                onReadEntirePlannerAloud = { viewModel.readCurrentScreenAloud() },
                                onSpeakText = { viewModel.speakText(it) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.VISUAL_SCANNER -> {
                            VisualAiScannerScreen(
                                visualScans = visualScans,
                                isScanning = isScanningVisual,
                                onAnalyzeCameraBitmap = { bitmap, mode ->
                                    viewModel.analyzeCameraBitmap(bitmap, mode)
                                },
                                onAnalyzePickedUri = { uri, mode ->
                                    viewModel.analyzePickedImageUri(uri, mode)
                                },
                                onAnalyzePresetScenario = { mode, prompt ->
                                    viewModel.analyzePresetVisualScenario(mode, prompt)
                                },
                                onSaveScanReminder = { scan ->
                                    viewModel.saveVisualScanAsReminder(scan)
                                },
                                onDeleteScan = { scan ->
                                    viewModel.deleteVisualScan(scan)
                                },
                                onSpeakText = { viewModel.speakText(it) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.MEDICATIONS -> {
                            val primaryCaregiver = emergencyContacts.firstOrNull { it.isPrimaryCaregiver }?.name
                                ?: "Sarah Miller (Primary Caregiver & Daughter)"
                            MedicationAndHealthScreen(
                                medications = medications,
                                medicationHistoryLogs = medicationHistoryLogs,
                                primaryCaregiverName = primaryCaregiver,
                                medicationAiAnswer = medicationAiAnswer,
                                isAiProcessing = isAiProcessing,
                                onToggleTakenToday = { viewModel.toggleMedicationTakenToday(it) },
                                onAddMedication = { name, dose, time, food, purpose, count ->
                                    viewModel.addMedication(name, dose, time, food, purpose, count)
                                },
                                onDeleteMedication = { viewModel.deleteMedication(it) },
                                onDeleteHistoryLog = { viewModel.deleteMedicationHistoryLog(it) },
                                onClearHistoryLogs = { viewModel.clearMedicationHistoryLogs() },
                                onReadCaregiverReportAloud = { viewModel.readCaregiverHistoryReportAloud() },
                                onSendCaregiverReport = { viewModel.sendCaregiverReportUpdate() },
                                onAskSafetyQuestion = { viewModel.askMedicationSafetyQuestion(it) },
                                onSpeakText = { viewModel.speakText(it) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.EMERGENCY_SOS -> {
                            EmergencySosScreen(
                                contacts = emergencyContacts,
                                onTriggerAudibleSos = { viewModel.triggerAudibleSosAlert() },
                                onAddContact = { name, rel, phone, notes ->
                                    viewModel.addEmergencyContact(name, rel, phone, notes)
                                },
                                onDeleteContact = { viewModel.deleteEmergencyContact(it) },
                                onSpeakText = { viewModel.speakText(it) },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }

                        AssistScreen.COMMAND_LOG -> {
                            CommandLogScreen(
                                commandLogs = voiceCommandLogs,
                                onSpeakText = { viewModel.speakText(it) },
                                onReRunCommand = { cmd -> viewModel.executeVoiceCommand(cmd) },
                                onDeleteCommandLog = { log -> viewModel.deleteVoiceCommandLog(log) },
                                onClearAllLogs = { viewModel.clearVoiceCommandLogs() },
                                onStartNewVoiceCommand = {
                                    viewModel.setShowVoiceOverlay(true)
                                    runWithAudioPermission {
                                        viewModel.startVoiceCommandListening(navigateToAssistant = false)
                                    }
                                },
                                onBack = { viewModel.navigateBackToHome() }
                            )
                        }
                    }
                }
            }
        }
    }
}
