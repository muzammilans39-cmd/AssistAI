package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.AlarmEntity
import com.example.data.MedicationEntity
import com.example.data.ReminderEntity
import com.example.ui.theme.EmeraldSuccess

@Composable
fun RemindersAndAlarmsScreen(
    reminders: List<ReminderEntity>,
    alarms: List<AlarmEntity>,
    medications: List<MedicationEntity> = emptyList(),
    onSaveReminder: (Int, String, String, String, String, String, Boolean) -> Unit,
    onToggleReminder: (ReminderEntity) -> Unit,
    onDeleteReminder: (ReminderEntity) -> Unit,
    onToggleMedicationTaken: (MedicationEntity) -> Unit = {},
    onAddDailyMedicationSchedule: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onAddAlarm: (String, String, String) -> Unit,
    onToggleAlarm: (AlarmEntity) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    onStartVoiceCommand: () -> Unit,
    onSpeakText: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Reminders & Meds, 1 = Alarms

    // Daily Medication Schedule Form State
    var showMedScheduleForm by rememberSaveable { mutableStateOf(false) }
    var medName by rememberSaveable { mutableStateOf("") }
    var medDosage by rememberSaveable { mutableStateOf("1 Tablet (10mg)") }
    var medTime by rememberSaveable { mutableStateOf("8:00 AM") }
    var medFoodNote by rememberSaveable { mutableStateOf("Take with breakfast and water") }

    // Reminder Form State
    var showReminderForm by rememberSaveable { mutableStateOf(false) }
    var editingReminderId by rememberSaveable { mutableIntStateOf(0) }
    var remTitle by rememberSaveable { mutableStateOf("") }
    var remDate by rememberSaveable { mutableStateOf("Today") }
    var remTime by rememberSaveable { mutableStateOf("9:00 AM") }
    var remCategory by rememberSaveable { mutableStateOf("Medicine") }
    var remNotes by rememberSaveable { mutableStateOf("") }
    var remIsDaily by rememberSaveable { mutableStateOf(false) }

    // Alarm Form State
    var showAlarmForm by rememberSaveable { mutableStateOf(false) }
    var alarmTime by rememberSaveable { mutableStateOf("8:00 AM") }
    var alarmLabel by rememberSaveable { mutableStateOf("Morning Medicine") }
    var alarmRepeat by rememberSaveable { mutableStateOf("Daily") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reminders_alarms_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Large Segmented Tab Selector: Reminders vs Alarms
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { selectedTab = 0 },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .testTag("tab_reminders_button")
                ) {
                    Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reminders (${reminders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedTab = 1 },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .testTag("tab_alarms_button")
                ) {
                    Icon(imageVector = Icons.Default.Alarm, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alarms (${alarms.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Voice Creation Banner
        item {
            OutlinedButton(
                onClick = onStartVoiceCommand,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .testTag("voice_create_reminder_alarm_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (selectedTab == 0) "Create Reminder by Voice" else "Create Alarm by Voice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (selectedTab == 0) {
            // --- DAILY MEDICATION SCHEDULE & DOSE LOGGER MODULE ---
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminders_daily_med_schedule_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val takenCount = medications.count { it.takenToday }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Medication,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "💊 DAILY MEDICATION SCHEDULE",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "$takenCount of ${medications.size} scheduled doses taken today",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val summary = if (medications.isEmpty()) {
                                        "No daily medications scheduled yet. Tap Log Daily Medication Schedule to add one."
                                    } else {
                                        "Daily Medication Schedule: $takenCount of ${medications.size} taken today. " +
                                            medications.joinToString("; ") {
                                                "${it.name} ${it.dosage} at ${it.scheduleTime}, ${if (it.takenToday) "already taken" else "not taken yet"}"
                                            }
                                    }
                                    onSpeakText(summary)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read daily medication schedule aloud",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = { showMedScheduleForm = !showMedScheduleForm },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag("toggle_med_schedule_form_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (showMedScheduleForm) "Close Medication Schedule Form" else "Log Daily Medication Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showMedScheduleForm) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "ADD DAILY MEDICATION SCHEDULE",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    OutlinedTextField(
                                        value = medName,
                                        onValueChange = { medName = it },
                                        label = { Text("Medication Name (e.g., Lisinopril)") },
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("med_schedule_name_input")
                                    )

                                    OutlinedTextField(
                                        value = medDosage,
                                        onValueChange = { medDosage = it },
                                        label = { Text("Dosage (e.g., 1 Tablet 10mg)") },
                                        textStyle = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("med_schedule_dosage_input")
                                    )

                                    Text("Quick Time-of-Day Presets:", style = MaterialTheme.typography.labelLarge)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("8:00 AM", "1:00 PM", "6:00 PM", "9:00 PM").forEach { presetTime ->
                                            OutlinedButton(
                                                onClick = { medTime = presetTime },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (medTime == presetTime) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(presetTime, style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = medTime,
                                        onValueChange = { medTime = it },
                                        label = { Text("Daily Schedule Time") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("med_schedule_time_input")
                                    )

                                    OutlinedTextField(
                                        value = medFoodNote,
                                        onValueChange = { medFoodNote = it },
                                        label = { Text("Instructions (e.g., Take with food & water)") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("med_schedule_food_input")
                                    )

                                    Button(
                                        onClick = {
                                            if (medName.isNotBlank()) {
                                                onAddDailyMedicationSchedule(
                                                    medName,
                                                    medDosage,
                                                    medTime,
                                                    medFoodNote
                                                )
                                                medName = ""
                                                showMedScheduleForm = false
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 56.dp)
                                            .testTag("save_med_schedule_submit_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Vibration, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Save Daily Medication Schedule",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(medications, key = { "rem_med_${it.id}" }) { med ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (med.takenToday) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 2.5.dp,
                        color = if (med.takenToday) EmeraldSuccess else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_med_schedule_card_${med.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "💊 Daily • ${med.scheduleTime}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onSpeakText(
                                        "${med.name}, ${med.dosage}. Scheduled daily at ${med.scheduleTime}. ${med.withFoodInstruction}. Status: ${if (med.takenToday) "Marked as taken today" else "Not taken yet today"}."
                                    )
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read ${med.name} schedule aloud"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${med.name} (${med.dosage})",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (med.takenToday) TextDecoration.LineThrough else TextDecoration.None
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${med.withFoodInstruction} • ${med.pillsRemaining} doses left",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onToggleMedicationTaken(med) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (med.takenToday) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp)
                                .testTag("mark_med_schedule_taken_${med.id}")
                        ) {
                            Icon(
                                imageVector = if (med.takenToday) Icons.Default.CheckCircle else Icons.Default.Vibration,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (med.takenToday) "✓ Taken Today (Haptic Confirmed)" else "Mark as Taken",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- GENERAL & MEDICINE REMINDERS SECTION ---
            item {
                Text(
                    text = "ALL REMINDERS (${reminders.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Button(
                    onClick = {
                        editingReminderId = 0
                        remTitle = ""
                        remDate = "Today"
                        remTime = "10:00 AM"
                        remCategory = "Medicine"
                        remNotes = ""
                        remIsDaily = false
                        showReminderForm = !showReminderForm
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .testTag("add_reminder_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showReminderForm) "Close Reminder Form" else "Add New Reminder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showReminderForm) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (editingReminderId == 0) "NEW REMINDER" else "EDIT REMINDER",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = remTitle,
                                onValueChange = { remTitle = it },
                                label = { Text("Reminder Title (e.g., Medicine reminder)") },
                                textStyle = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reminder_title_input")
                            )

                            // Quick Date Selector Buttons
                            Text("Select Day / Date:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Today", "Tomorrow", "Daily", "Friday").forEach { d ->
                                    OutlinedButton(
                                        onClick = {
                                            remDate = d
                                            remIsDaily = (d == "Daily")
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (remDate == d) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(d, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = remDate,
                                    onValueChange = { remDate = it },
                                    label = { Text("Date / Day") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = remTime,
                                    onValueChange = { remTime = it },
                                    label = { Text("Time (e.g., 9:00 AM)") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reminder_time_input")
                                )
                            }

                            OutlinedTextField(
                                value = remNotes,
                                onValueChange = { remNotes = it },
                                label = { Text("Notes / Instructions (optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (remTitle.isNotBlank()) {
                                        onSaveReminder(
                                            editingReminderId,
                                            remTitle,
                                            remDate,
                                            remTime,
                                            remCategory,
                                            remNotes,
                                            remIsDaily
                                        )
                                        showReminderForm = false
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 58.dp)
                                    .testTag("save_reminder_submit_button")
                            ) {
                                Text("Save Reminder & Set Notification Alert", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            items(reminders, key = { "rem_${it.id}" }) { reminder ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reminder.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_card_${reminder.id}")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${reminder.dateText} • ${reminder.timeText}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        onSpeakText("${reminder.title}. Scheduled for ${reminder.dateText} at ${reminder.timeText}. ${reminder.notes}")
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Read reminder aloud"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingReminderId = reminder.id
                                        remTitle = reminder.title
                                        remDate = reminder.dateText
                                        remTime = reminder.timeText
                                        remCategory = reminder.category
                                        remNotes = reminder.notes
                                        remIsDaily = reminder.isDaily
                                        showReminderForm = true
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit reminder"
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteReminder(reminder) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete reminder",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )

                        if (reminder.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = reminder.notes,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val isMedReminder = reminder.category.equals("Medicine", ignoreCase = true) ||
                            reminder.title.contains("medicine", ignoreCase = true) ||
                            reminder.title.contains("pill", ignoreCase = true) ||
                            reminder.title.contains("take ", ignoreCase = true)

                        Button(
                            onClick = { onToggleReminder(reminder) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reminder.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag("toggle_reminder_done_${reminder.id}")
                        ) {
                            Icon(
                                imageVector = when {
                                    reminder.isCompleted -> Icons.Default.CheckCircle
                                    isMedReminder -> Icons.Default.Vibration
                                    else -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    reminder.isCompleted && isMedReminder -> "✓ Taken Today (Tap to Reopen)"
                                    reminder.isCompleted -> "Completed (Tap to Reopen)"
                                    isMedReminder -> "Mark as Taken"
                                    else -> "Mark Reminder as Complete"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            // --- ALARMS TAB ---
            item {
                Button(
                    onClick = { showAlarmForm = !showAlarmForm },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp)
                        .testTag("add_alarm_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showAlarmForm) "Close Alarm Form" else "Create New Alarm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (showAlarmForm) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CREATE LARGE-BUTTON ALARM",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Text("Quick Time Presets:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("7:00 AM", "8:00 AM", "9:00 AM", "6:00 PM").forEach { t ->
                                    OutlinedButton(
                                        onClick = { alarmTime = t },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(t, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = alarmTime,
                                onValueChange = { alarmTime = it },
                                label = { Text("Alarm Time (e.g., 8:30 AM)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("alarm_time_input")
                            )

                            OutlinedTextField(
                                value = alarmLabel,
                                onValueChange = { alarmLabel = it },
                                label = { Text("Alarm Label (e.g., Morning Medicine)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("alarm_label_input")
                            )

                            Text("Repeat Days:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Daily", "Mon–Fri", "Weekends", "Once").forEach { rep ->
                                    OutlinedButton(
                                        onClick = { alarmRepeat = rep },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (alarmRepeat == rep) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(rep, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onAddAlarm(alarmTime, alarmLabel, alarmRepeat)
                                    showAlarmForm = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 58.dp)
                                    .testTag("save_alarm_submit_button")
                            ) {
                                Text("Save Alarm", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            items(alarms, key = { "alarm_${it.id}" }) { alarm ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alarm.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(2.5.dp, if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_card_${alarm.id}")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alarm.timeText,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = alarm.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Repeats: ${alarm.repeatDays}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { onToggleAlarm(alarm) },
                                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            onSpeakText("Alarm for ${alarm.timeText}. Label: ${alarm.label}. Repeats ${alarm.repeatDays}. Currently ${if (alarm.isEnabled) "On" else "Off"}.")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Read alarm aloud"
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteAlarm(alarm) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete alarm",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
