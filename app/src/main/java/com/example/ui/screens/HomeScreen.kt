package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.AlarmEntity
import com.example.data.AppointmentEntity
import com.example.data.CallRecordEntity
import com.example.data.EmergencyContactEntity
import com.example.data.ReminderEntity
import com.example.ui.components.DiscreetBatteryStatusBanner
import com.example.ui.components.LargeDashboardOptionButton
import com.example.ui.components.LargeMicrophoneHeroButton
import com.example.ui.components.NextUpcomingHeroBanner
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ContrastMode
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalContrastMode
import com.example.util.rememberDeviceBatteryState
import com.example.viewmodel.AssistScreen

@Composable
fun HomeScreen(
    reminders: List<ReminderEntity>,
    alarms: List<AlarmEntity>,
    appointments: List<AppointmentEntity>,
    callRecords: List<CallRecordEntity>,
    emergencyContacts: List<EmergencyContactEntity>,
    nextUpcomingSummary: String,
    isListening: Boolean,
    livePartialTranscript: String,
    onMicClick: () -> Unit,
    onNavigate: (AssistScreen) -> Unit,
    onSpeakText: (String) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onTriggerImmediateSosCall: (String, String) -> Unit,
    commandLogsCount: Int = 0
) {
    val context = LocalContext.current
    val activeRemindersCount = reminders.count { !it.isCompleted }
    val activeAlarmsCount = alarms.count { it.isEnabled }
    val upcomingEventsCount = appointments.size
    val callRecordsCount = callRecords.size

    val primaryEmergencyContact = remember(emergencyContacts) {
        emergencyContacts.firstOrNull { it.isPrimaryCaregiver }
            ?: emergencyContacts.firstOrNull()
            ?: EmergencyContactEntity(
                id = 1,
                name = "Sarah Miller (Daughter)",
                relationship = "Primary Caregiver & Daughter",
                phoneNumber = "555-0911",
                isPrimaryCaregiver = true,
                notes = "Lives 10 minutes away. Has spare house key."
            )
    }

    var pendingEmergencyPhone by remember { mutableStateOf<String?>(null) }

    fun placeDirectOrDialCall(rawPhone: String) {
        val cleanPhone = rawPhone.filter { it.isDigit() || it == '+' || it == '-' }.ifBlank { "555-0911" }
        val callUri = Uri.parse("tel:$cleanPhone")
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            try {
                val directCallIntent = Intent(Intent.ACTION_CALL, callUri)
                context.startActivity(directCallIntent)
                return
            } catch (_: Exception) {
                // Fall back to ACTION_DIAL below if device/emulator blocks direct call
            }
        }
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL, callUri)
            context.startActivity(dialIntent)
        } catch (_: Exception) {
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val phoneToCall = pendingEmergencyPhone ?: primaryEmergencyContact.phoneNumber
        pendingEmergencyPhone = null
        placeDirectOrDialCall(phoneToCall)
    }

    fun triggerHomeSosEmergencyCall() {
        val targetName = primaryEmergencyContact.name
        val targetPhone = primaryEmergencyContact.phoneNumber
        onTriggerImmediateSosCall(targetName, targetPhone)

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCallPermission) {
            placeDirectOrDialCall(targetPhone)
        } else {
            pendingEmergencyPhone = targetPhone
            try {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            } catch (_: Exception) {
                placeDirectOrDialCall(targetPhone)
            }
        }
    }

    val batteryState by rememberDeviceBatteryState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Discreet Large-Text Battery Status Indicator
        item {
            DiscreetBatteryStatusBanner(
                batteryState = batteryState,
                onSpeakBatteryStatus = {
                    onSpeakText(batteryState.spokenSummary)
                }
            )
        }

        // 0. High-Contrast SOS Emergency Immediate Call Banner & Button
        item {
            val contrastMode = LocalContrastMode.current
            val sosHighlight = when (contrastMode) {
                ContrastMode.BLACK_AND_WHITE -> Color.White
                else -> Color(0xFFFFEA00)
            }
            val sosContainer = when (contrastMode) {
                ContrastMode.ULTRA_LOW_VISION, ContrastMode.BLACK_AND_WHITE -> Color.Black
                else -> Color(0xFF990000)
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = sosContainer
                ),
                border = BorderStroke(3.5.dp, sosHighlight),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_sos_emergency_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = sosHighlight,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "SOS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "EMERGENCY 1-TAP CALL",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = sosHighlight,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${primaryEmergencyContact.name} • ${primaryEmergencyContact.phoneNumber}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (contrastMode == ContrastMode.ULTRA_LOW_VISION) sosHighlight else Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onSpeakText(
                                    "SOS Emergency Button. Tap the large red and yellow button to immediately call your primary emergency contact, ${primaryEmergencyContact.name}, at ${primaryEmergencyContact.phoneNumber}."
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_sos_speak_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read SOS emergency contact details aloud",
                                tint = sosHighlight,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { triggerHomeSosEmergencyCall() },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sosHighlight,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 84.dp)
                            .semantics {
                                contentDescription =
                                    "SOS Emergency Call Button. Immediately calls ${primaryEmergencyContact.name} at ${primaryEmergencyContact.phoneNumber}"
                            }
                            .testTag("home_sos_emergency_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "SOS — CALL ${primaryEmergencyContact.name.substringBefore(" ").uppercase()} NOW",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Dialing ${primaryEmergencyContact.phoneNumber} Immediately",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF1F1F1F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val secondarySosTint = if (contrastMode == ContrastMode.ULTRA_LOW_VISION) sosHighlight else Color.White
                    OutlinedButton(
                        onClick = { onNavigate(AssistScreen.EMERGENCY_SOS) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, secondarySosTint),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = secondarySosTint
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("home_sos_manage_contacts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = secondarySosTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View Medical ID & Manage Emergency Contacts",
                            style = MaterialTheme.typography.labelLarge,
                            color = secondarySosTint,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1. Large Voice Assistant Microphone Hero Card
        item {
            LargeMicrophoneHeroButton(
                isListening = isListening,
                livePartialText = livePartialTranscript,
                commandLogsCount = commandLogsCount,
                onMicClick = onMicClick,
                onOpenVoiceAssistant = { onNavigate(AssistScreen.AI_ASSISTANT) },
                onOpenCommandLog = { onNavigate(AssistScreen.COMMAND_LOG) }
            )
        }

        // 2. Next Upcoming Reminder / Event Banner
        item {
            NextUpcomingHeroBanner(
                nextSummaryText = nextUpcomingSummary,
                onSpeakNext = { onSpeakText(nextUpcomingSummary) },
                onOpenPlanner = { onNavigate(AssistScreen.DAILY_PLANNER) }
            )
        }

        // 3. Dashboard Section Header
        item {
            Text(
                text = "MAIN MENU (5 LARGE OPTIONS)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Option 1: 🎙️ AI Assistant
        item {
            LargeDashboardOptionButton(
                emojiIcon = "🎙️",
                vectorIcon = Icons.Default.RecordVoiceOver,
                title = "AI Assistant",
                subtitle = "Speak commands, ask questions & simplify complex text",
                badgeText = "Voice & Gemini AI Ready",
                accentColor = MaterialTheme.colorScheme.primary,
                testTagName = "dashboard_ai_assistant_button",
                onClick = { onNavigate(AssistScreen.AI_ASSISTANT) },
                onSpeakDescription = {
                    onSpeakText("AI Assistant. Tap to control the app with your voice, ask questions, or convert complex text into easy language.")
                }
            )
        }

        // Option 2: 📞 Call Analysis
        item {
            LargeDashboardOptionButton(
                emojiIcon = "📞",
                vectorIcon = Icons.Default.PhoneInTalk,
                title = "Call Analysis",
                subtitle = "Record or upload calls, read AI summary & save appointments",
                badgeText = "$callRecordsCount Analyzed Calls",
                accentColor = Color(0xFF0F766E),
                testTagName = "dashboard_call_analysis_button",
                onClick = { onNavigate(AssistScreen.CALL_ANALYSIS) },
                onSpeakDescription = {
                    onSpeakText("Call Analysis. Record or upload a phone call. AI extracts names, dates, times, phone numbers, and appointments automatically. $callRecordsCount analyzed calls saved.")
                }
            )
        }

        // Option 3: ⏰ Reminders & Alarms
        item {
            LargeDashboardOptionButton(
                emojiIcon = "⏰",
                vectorIcon = Icons.Default.Alarm,
                title = "Reminders & Alarms",
                subtitle = "Medicine reminders, daily tasks & large-button alarms",
                badgeText = "$activeRemindersCount Reminders • $activeAlarmsCount Alarms",
                accentColor = Color(0xFFB45309),
                testTagName = "dashboard_reminders_alarms_button",
                onClick = { onNavigate(AssistScreen.REMINDERS_ALARMS) },
                onSpeakDescription = {
                    onSpeakText("Reminders and Alarms. You have $activeRemindersCount active reminders and $activeAlarmsCount active alarms.")
                }
            )
        }

        // Option 4: 📅 Appointments
        item {
            LargeDashboardOptionButton(
                emojiIcon = "📅",
                vectorIcon = Icons.Default.CalendarMonth,
                title = "Appointments",
                subtitle = "Doctor visits, community meetings & train/travel plans",
                badgeText = "$upcomingEventsCount Upcoming Events",
                accentColor = Color(0xFF6D28D9),
                testTagName = "dashboard_appointments_button",
                onClick = { onNavigate(AssistScreen.APPOINTMENTS) },
                onSpeakDescription = {
                    onSpeakText("Appointments, Meetings, and Travel Plans. You have $upcomingEventsCount upcoming events scheduled.")
                }
            )
        }

        // Option 5: 📋 Daily Planner
        item {
            LargeDashboardOptionButton(
                emojiIcon = "📋",
                vectorIcon = Icons.AutoMirrored.Filled.EventNote,
                title = "Daily Planner",
                subtitle = "View all of today's reminders, alarms, meetings & tasks",
                badgeText = "Complete Daily Overview",
                accentColor = EmeraldSuccess,
                testTagName = "dashboard_daily_planner_button",
                onClick = { onNavigate(AssistScreen.DAILY_PLANNER) },
                onSpeakDescription = {
                    onSpeakText("Daily Planner. View your complete daily schedule including today's reminders, alarms, appointments, meetings, travel plans, and important tasks.")
                }
            )
        }

        // Advanced Assistive Features Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ADVANCED ASSISTIVE & SAFETY TOOLS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // Option 6: 👁️ Visual AI Camera & Label Reader
        item {
            LargeDashboardOptionButton(
                emojiIcon = "👁️",
                vectorIcon = Icons.Default.Visibility,
                title = "Visual AI Camera Reader",
                subtitle = "Read medicine bottles, bills, currency & room scenes aloud",
                badgeText = "Multimodal Vision & OCR",
                accentColor = Color(0xFF0284C7),
                testTagName = "dashboard_visual_scanner_button",
                onClick = { onNavigate(AssistScreen.VISUAL_SCANNER) },
                onSpeakDescription = {
                    onSpeakText("Visual AI Camera Reader. Take a photo or scan medicine bottles, utility bills, dollar bills, or room scenes to hear them read aloud in large print.")
                }
            )
        }

        // Option 7: 💊 Medication Manager & Caregiver History Log
        item {
            LargeDashboardOptionButton(
                emojiIcon = "💊",
                vectorIcon = Icons.Default.Medication,
                title = "Medication Manager",
                subtitle = "Track daily pill intake with visual reminders, 'Taken' toggle & caregiver history log",
                badgeText = "Visual Reminders & Caregiver Log",
                accentColor = Color(0xFF059669),
                testTagName = "dashboard_medications_button",
                onClick = { onNavigate(AssistScreen.MEDICATIONS) },
                onSpeakDescription = {
                    onSpeakText("Medication Manager. Track daily pill intake with visual reminders, flip the simple Taken toggle, and review the Caregiver History Log.")
                }
            )
        }

        // Option 8: 🆘 Emergency SOS & Caregiver Quick-Dial
        item {
            LargeDashboardOptionButton(
                emojiIcon = "🆘",
                vectorIcon = Icons.Default.Emergency,
                title = "Emergency SOS & Caregiver",
                subtitle = "Audible SOS voice beacon, Medical ID & 1-tap caregiver call",
                badgeText = "24/7 Safety & Medical ID",
                accentColor = Color(0xFFB91C1C),
                testTagName = "dashboard_emergency_sos_button",
                onClick = { onNavigate(AssistScreen.EMERGENCY_SOS) },
                onSpeakDescription = {
                    onSpeakText("Emergency SOS and Caregiver Hub. Activate an audible Medical ID beacon or call your daughter Sarah and doctor with one tap.")
                }
            )
        }

        // Option 9: 📜 Command Log (Last 10 Voice Commands)
        item {
            LargeDashboardOptionButton(
                emojiIcon = "📜",
                vectorIcon = Icons.Default.History,
                title = "Command Log",
                subtitle = "Review the last 10 voice commands executed by AssistAI & what it did",
                badgeText = "$commandLogsCount / 10 Recent Voice Commands",
                accentColor = Color(0xFF4338CA),
                testTagName = "dashboard_command_log_button",
                onClick = { onNavigate(AssistScreen.COMMAND_LOG) },
                onSpeakDescription = {
                    onSpeakText("Command Log. Review the last 10 voice commands executed by the AI and hear what the app did for you. $commandLogsCount commands recorded.")
                }
            )
        }

        // 4. Quick Check-Off Card for Priority Reminders
        if (reminders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "QUICK REMINDERS CHECKLIST",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            items(reminders.take(3).size) { index ->
                val reminder = reminders[index]
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_quick_reminder_${reminder.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${reminder.dateText} • ${reminder.timeText}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = reminder.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (reminder.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = reminder.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    onSpeakText("${reminder.title}. Scheduled for ${reminder.dateText} at ${reminder.timeText}. ${reminder.notes}")
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read reminder aloud",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { onToggleReminder(reminder) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (reminder.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("home_complete_reminder_${reminder.id}")
                            ) {
                                Icon(
                                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (reminder.isCompleted) "Done" else "Mark Done",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
