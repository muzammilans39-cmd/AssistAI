package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AssistDatabase
import com.example.data.AssistRepository
import com.example.data.MedicationEntity
import com.example.network.GeminiService
import com.example.ui.theme.ContrastMode
import com.example.util.AccessibilityVoiceAction
import com.example.util.AssistHapticManager
import com.example.util.BatteryStatusHelper
import com.example.util.SpeechToTextHandler
import com.example.util.VoiceHapticPattern
import com.example.util.VoiceRouteDecision
import com.example.viewmodel.AssistScreen
import com.example.viewmodel.AssistViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AssistAI", appName)
    }

    @Test
    fun `haptic manager triggers VibratorManager patterns for listening processing and confirmation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val hapticManager = AssistHapticManager(context)

        hapticManager.triggerStartListeningHaptic()
        assertEquals(VoiceHapticPattern.START_LISTENING, hapticManager.lastTriggeredPattern.value)
        assertEquals(1, hapticManager.patternHistoryCount.value)

        hapticManager.triggerProcessingInputHaptic()
        assertEquals(VoiceHapticPattern.PROCESSING_INPUT, hapticManager.lastTriggeredPattern.value)
        assertEquals(2, hapticManager.patternHistoryCount.value)

        hapticManager.triggerCommandConfirmedHaptic()
        assertEquals(VoiceHapticPattern.COMMAND_CONFIRMED, hapticManager.lastTriggeredPattern.value)
        assertEquals(3, hapticManager.patternHistoryCount.value)

        hapticManager.triggerEmergencyAlertHaptic()
        assertEquals(VoiceHapticPattern.EMERGENCY_ALERT, hapticManager.lastTriggeredPattern.value)
        assertEquals(4, hapticManager.patternHistoryCount.value)
    }

    @Test
    fun `classifyVoiceTranscript routes navigation commands accurately`() {
        val plannerRoute = SpeechToTextHandler.classifyVoiceTranscript("Open Daily Planner")
        assertTrue(plannerRoute is VoiceRouteDecision.NavigateToScreen)
        assertEquals(AssistScreen.DAILY_PLANNER, (plannerRoute as VoiceRouteDecision.NavigateToScreen).destination)

        val scannerRoute = SpeechToTextHandler.classifyVoiceTranscript("Open Visual Scanner")
        assertTrue(scannerRoute is VoiceRouteDecision.NavigateToScreen)
        assertEquals(AssistScreen.VISUAL_SCANNER, (scannerRoute as VoiceRouteDecision.NavigateToScreen).destination)

        val medRoute = SpeechToTextHandler.classifyVoiceTranscript("Open Medications")
        assertTrue(medRoute is VoiceRouteDecision.NavigateToScreen)
        assertEquals(AssistScreen.MEDICATIONS, (medRoute as VoiceRouteDecision.NavigateToScreen).destination)

        val sosRoute = SpeechToTextHandler.classifyVoiceTranscript("Open Emergency SOS")
        assertTrue(sosRoute is VoiceRouteDecision.NavigateToScreen)
        assertEquals(AssistScreen.EMERGENCY_SOS, (sosRoute as VoiceRouteDecision.NavigateToScreen).destination)

        val commandLogRoute = SpeechToTextHandler.classifyVoiceTranscript("Open Command Log")
        assertTrue(commandLogRoute is VoiceRouteDecision.NavigateToScreen)
        assertEquals(AssistScreen.COMMAND_LOG, (commandLogRoute as VoiceRouteDecision.NavigateToScreen).destination)

        val readAloudRoute = SpeechToTextHandler.classifyVoiceTranscript("Read Screen Aloud")
        assertTrue(readAloudRoute is VoiceRouteDecision.AccessibilityAction)
        assertEquals(
            AccessibilityVoiceAction.READ_SCREEN_ALOUD,
            (readAloudRoute as VoiceRouteDecision.AccessibilityAction).actionType
        )

        val batteryRoute = SpeechToTextHandler.classifyVoiceTranscript("What is my battery level?")
        assertTrue(batteryRoute is VoiceRouteDecision.AccessibilityAction)
        assertEquals(
            AccessibilityVoiceAction.READ_BATTERY_STATUS,
            (batteryRoute as VoiceRouteDecision.AccessibilityAction).actionType
        )
    }

    @Test
    fun `battery status helper builds senior friendly battery states accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val currentState = BatteryStatusHelper.readCurrentBatteryState(context)
        assertTrue(currentState.percentage in 0..100)
        assertFalse(currentState.spokenSummary.isBlank())

        val lowState = BatteryStatusHelper.buildBatteryState(
            percentage = 15,
            isCharging = false,
            powerSource = "On Battery"
        )
        assertTrue(lowState.isLowBattery)
        assertEquals("LOW BATTERY", lowState.statusBadgeText)

        val chargingState = BatteryStatusHelper.buildBatteryState(
            percentage = 65,
            isCharging = true,
            powerSource = "Charging (Wall Outlet)"
        )
        assertFalse(chargingState.isLowBattery)
        assertEquals("CHARGING NOW", chargingState.statusBadgeText)
    }

    @Test
    fun `classifyVoiceTranscript routes action commands accurately`() {
        val completeRoute = SpeechToTextHandler.classifyVoiceTranscript("Complete medicine reminder")
        assertTrue(completeRoute is VoiceRouteDecision.CompleteReminderByVoice)
        assertEquals("medicine", (completeRoute as VoiceRouteDecision.CompleteReminderByVoice).keyword)

        val medDoseRoute = SpeechToTextHandler.classifyVoiceTranscript("I took my morning medicine")
        assertTrue(medDoseRoute is VoiceRouteDecision.TakeMedicationDoseByVoice)

        val aiCommandRoute = SpeechToTextHandler.classifyVoiceTranscript("Set a reminder for tomorrow at 10 AM.")
        assertTrue(aiCommandRoute is VoiceRouteDecision.ExecuteAiCommand)
    }

    @Test
    fun `database seeding and gemini service fallbacks execute without error`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AssistDatabase.getInstance(context)
        val repo = AssistRepository(db.assistDao())
        repo.ensureDemoDataSeeded()

        val callResult = GeminiService.analyzeCallOrAudio(
            rawTranscript = "Hello, this is Dr. Patel's office reminding you of your appointment tomorrow at 10 AM."
        )
        assertNotNull(callResult)
        assertFalse(callResult.summary.isBlank())

        val voiceResult = GeminiService.processVoiceCommand(
            command = "Set a reminder for tomorrow at 10 AM",
            contextSummary = "No reminders yet."
        )
        assertNotNull(voiceResult)
        assertFalse(voiceResult.spokenReply.isBlank())
    }

    @Test
    fun `daily medication schedule logging and mark as taken triggers haptic feedback`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = AssistViewModel(app)

        viewModel.addDailyMedicationScheduleFromReminders(
            name = "Lisinopril",
            dosage = "10mg Tablet",
            scheduleTime = "8:00 AM",
            withFoodInstruction = "Take with breakfast"
        )
        assertEquals(
            VoiceHapticPattern.COMMAND_CONFIRMED,
            viewModel.speechHelper.lastHapticPattern.value
        )
    }

    @Test
    fun `high contrast mode toggle switches between standard and black-yellow or black-white palettes`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = AssistViewModel(app)

        assertFalse(viewModel.contrastMode.value.isForcedHighContrast)

        viewModel.toggleHighContrastMode(true)
        assertTrue(viewModel.contrastMode.value.isForcedHighContrast)
        assertEquals(ContrastMode.ULTRA_LOW_VISION, viewModel.contrastMode.value)

        viewModel.setContrastMode(ContrastMode.BLACK_AND_WHITE)
        assertTrue(viewModel.contrastMode.value.isForcedHighContrast)
        assertEquals(ContrastMode.BLACK_AND_WHITE, viewModel.contrastMode.value)

        viewModel.toggleHighContrastMode(false)
        assertFalse(viewModel.contrastMode.value.isForcedHighContrast)
        assertEquals(ContrastMode.HIGH_CONTRAST_LIGHT, viewModel.contrastMode.value)
    }

    @Test
    fun `command log records executed voice commands and retains at most last 10 commands`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = AssistViewModel(app)

        viewModel.clearVoiceCommandLogs()
        assertEquals(0, viewModel.voiceCommandLogs.value.size)

        for (i in 1..12) {
            viewModel.recordExecutedVoiceCommand(
                spokenCommand = "Voice command #$i",
                aiActionTaken = "✓ Executed action #$i",
                spokenConfirmation = "Completed command number $i",
                intentCategory = "Reminder"
            )
        }

        val logs = viewModel.voiceCommandLogs.value
        assertEquals(10, logs.size)
        assertEquals("Voice command #12", logs.first().spokenCommand)
        assertEquals("Voice command #3", logs.last().spokenCommand)

        viewModel.executeVoiceCommand("Open Command Log")
        assertEquals(AssistScreen.COMMAND_LOG, viewModel.currentScreen.value)
        assertEquals(10, viewModel.voiceCommandLogs.value.size)
        assertEquals("Open Command Log", viewModel.voiceCommandLogs.value.first().spokenCommand)
    }

    @Test
    fun `medication manager taken toggle logs intake events to caregiver history log`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = AssistViewModel(app)

        val medManagerRoute = SpeechToTextHandler.classifyVoiceTranscript("Open Medication Manager")
        assertTrue(medManagerRoute is VoiceRouteDecision.NavigateToScreen)
        assertEquals(AssistScreen.MEDICATIONS, (medManagerRoute as VoiceRouteDecision.NavigateToScreen).destination)

        viewModel.clearMedicationHistoryLogs()
        assertEquals(0, viewModel.medicationHistoryLogs.value.size)

        val sampleMedication = MedicationEntity(
            id = 101,
            name = "Amlodipine (Blood Pressure)",
            dosage = "5 mg • 1 White Round Tablet",
            scheduleTime = "9:00 AM (Morning)",
            withFoodInstruction = "Take after breakfast with water",
            purposeSimple = "Keeps blood pressure steady",
            pillsRemaining = 18,
            takenToday = false
        )

        // Toggle 'Taken' ON -> should log TAKEN entry to Caregiver History Log
        viewModel.toggleMedicationTakenToday(sampleMedication)
        assertEquals(1, viewModel.medicationHistoryLogs.value.size)
        val firstLog = viewModel.medicationHistoryLogs.value.first()
        assertEquals("Amlodipine (Blood Pressure)", firstLog.medicationName)
        assertEquals("TAKEN", firstLog.status)
        assertEquals("Taken Toggle", firstLog.loggedByMethod)
        assertEquals(17, firstLog.pillsRemainingAfter)
        assertTrue(firstLog.caregiverNote.contains("Caregiver Log"))

        // Toggle 'Taken' OFF -> should log UNDONE entry to Caregiver History Log
        val takenMed = sampleMedication.copy(takenToday = true, pillsRemaining = 17)
        viewModel.toggleMedicationTakenToday(takenMed)
        assertEquals(2, viewModel.medicationHistoryLogs.value.size)
        val secondLog = viewModel.medicationHistoryLogs.value.first()
        assertEquals("UNDONE", secondLog.status)
        assertEquals(18, secondLog.pillsRemainingAfter)
    }
}
