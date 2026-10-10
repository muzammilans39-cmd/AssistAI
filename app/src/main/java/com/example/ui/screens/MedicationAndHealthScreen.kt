package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MedicationEntity
import com.example.data.MedicationHistoryLogEntity
import com.example.ui.theme.ContrastMode
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalContrastMode

@Composable
fun MedicationAndHealthScreen(
    medications: List<MedicationEntity>,
    medicationHistoryLogs: List<MedicationHistoryLogEntity> = emptyList(),
    primaryCaregiverName: String = "Sarah Miller (Primary Caregiver & Daughter)",
    medicationAiAnswer: String?,
    isAiProcessing: Boolean,
    onToggleTakenToday: (MedicationEntity) -> Unit,
    onAddMedication: (String, String, String, String, String, Int) -> Unit,
    onDeleteMedication: (MedicationEntity) -> Unit,
    onDeleteHistoryLog: (MedicationHistoryLogEntity) -> Unit = {},
    onClearHistoryLogs: () -> Unit = {},
    onReadCaregiverReportAloud: () -> Unit = {},
    onSendCaregiverReport: () -> Unit = {},
    onAskSafetyQuestion: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val contrastMode = LocalContrastMode.current
    var showAddForm by rememberSaveable { mutableStateOf(false) }
    var medName by rememberSaveable { mutableStateOf("") }
    var medDosage by rememberSaveable { mutableStateOf("1 Tablet") }
    var medTime by rememberSaveable { mutableStateOf("9:00 AM") }
    var medFood by rememberSaveable { mutableStateOf("Take with breakfast and water") }
    var medPurpose by rememberSaveable { mutableStateOf("Supports daily health") }
    var customQuestion by rememberSaveable { mutableStateOf("") }
    var selectedHistoryFilter by rememberSaveable { mutableStateOf("All") }

    val takenCount = medications.count { it.takenToday }
    val pendingMedications = medications.filter { !it.takenToday }
    val completionFraction = if (medications.isEmpty()) 0f else (takenCount.toFloat() / medications.size.toFloat()).coerceIn(0f, 1f)

    val filteredHistoryLogs = when (selectedHistoryFilter) {
        "Taken" -> medicationHistoryLogs.filter { it.status.equals("TAKEN", ignoreCase = true) }
        "Undone" -> medicationHistoryLogs.filter { !it.status.equals("TAKEN", ignoreCase = true) }
        else -> medicationHistoryLogs
    }

    val sampleSafetyQuestions = listOf(
        "Can I drink grapefruit juice while taking Amlodipine?",
        "Should I take Metformin with food or on an empty stomach?",
        "What should I do if I accidentally miss a morning pill?"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("medication_health_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Medication Manager Hero Summary & Daily Intake Progress Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medication_manager_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💊 MEDICATION MANAGER",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$takenCount of ${medications.size} Daily Doses Taken",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.testTag("medication_daily_intake_headline")
                            )
                            Text(
                                text = "Use the simple 'Taken' toggle on any pill card to log intake directly to the Caregiver History Log.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = {
                                onSpeakText(
                                    "Medication Manager. You have taken $takenCount out of ${medications.size} daily medications today. ${medicationHistoryLogs.size} entries recorded in the Caregiver History Log."
                                )
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .testTag("medication_manager_speak_summary_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read medication progress aloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Visual Daily Intake Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily Pill Intake Progress",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${(completionFraction * 100).toInt()}% Complete",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = completionFraction.coerceAtLeast(0.06f))
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(
                                        if (contrastMode.isForcedHighContrast) {
                                            MaterialTheme.colorScheme.primary
                                        } else if (takenCount == medications.size && medications.isNotEmpty()) {
                                            EmeraldSuccess
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                        }
                    }

                    // Quick Status Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "✓ $takenCount Taken",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Logged Today",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "⏰ ${pendingMedications.size} Due",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Visual Reminders",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "📋 ${medicationHistoryLogs.size} Logs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("summary_caregiver_log_count")
                                )
                                Text(
                                    text = "Caregiver Log",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Visual Dose Reminders Banner (Morning, Afternoon, Bedtime Visual Cues)
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visual_dose_reminders_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🔔 VISUAL PILL REMINDERS TODAY",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = if (pendingMedications.isEmpty()) {
                                        "All visual dose reminders for today are complete!"
                                    } else {
                                        "${pendingMedications.size} scheduled dose(s) waiting for 'Taken' confirmation"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                val visualSummary = if (pendingMedications.isEmpty()) {
                                    "All daily pill reminders have been marked as taken today."
                                } else {
                                    "Visual pill reminders still due today: " + pendingMedications.joinToString("; ") {
                                        "${it.name}, ${it.dosage}, scheduled for ${it.scheduleTime} (${it.withFoodInstruction})"
                                    }
                                }
                                onSpeakText(visualSummary)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read visual dose reminders aloud",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    // Visual Time-of-Day Reminder Pills
                    medications.forEach { med ->
                        val timeSlotEmoji = when {
                            med.scheduleTime.contains("Morning", ignoreCase = true) || med.scheduleTime.contains("AM", ignoreCase = true) -> "☀️ Morning"
                            med.scheduleTime.contains("Bedtime", ignoreCase = true) || med.scheduleTime.contains("8:30", ignoreCase = true) -> "🌙 Bedtime"
                            else -> "🌤️ Afternoon / Meal"
                        }
                        val pillShapeVisual = when {
                            med.dosage.contains("Drop", ignoreCase = true) -> "💧 Eye Dropper"
                            med.dosage.contains("Oval", ignoreCase = true) -> "💊 Oval Tablet"
                            else -> "⚪ Round Tablet"
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = 2.dp,
                                color = if (med.takenToday) {
                                    if (contrastMode.isForcedHighContrast) MaterialTheme.colorScheme.primary else EmeraldSuccess
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("visual_reminder_item_${med.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$timeSlotEmoji • ${med.scheduleTime}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            text = pillShapeVisual,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${med.name} (${med.dosage})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "🍽️ ${med.withFoodInstruction}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    color = if (med.takenToday) {
                                        if (contrastMode.isForcedHighContrast) MaterialTheme.colorScheme.primary else EmeraldSuccess
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (med.takenToday) "✓ TAKEN" else "⏰ DUE",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (med.takenToday) {
                                            if (contrastMode.isForcedHighContrast) MaterialTheme.colorScheme.onPrimary else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Add New Medication Button & Form
        item {
            Button(
                onClick = { showAddForm = !showAddForm },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag("add_medication_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showAddForm) "Close Medication Form" else "Add New Medicine to Pillbox",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showAddForm) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ADD MEDICINE TO MEDICATION MANAGER",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = medName,
                            onValueChange = { medName = it },
                            label = { Text("Medicine Name (e.g., Vitamin D3)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_medication_name_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = medDosage,
                                onValueChange = { medDosage = it },
                                label = { Text("Dosage") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = medTime,
                                onValueChange = { medTime = it },
                                label = { Text("Time") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = medFood,
                            onValueChange = { medFood = it },
                            label = { Text("Food Instructions") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = medPurpose,
                            onValueChange = { medPurpose = it },
                            label = { Text("Simple Purpose (what it helps with)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (medName.isNotBlank()) {
                                    onAddMedication(medName, medDosage, medTime, medFood, medPurpose, 30)
                                    medName = ""
                                    showAddForm = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag("save_new_medication_button")
                        ) {
                            Text("Save Medicine", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // 4. Daily Pill Intake Cards with Simple 'Taken' Toggle
        item {
            Text(
                text = "DAILY PILL SCHEDULE & 'TAKEN' TOGGLES",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        items(medications, key = { "med_${it.id}" }) { med ->
            val takenAccentColor = if (contrastMode.isForcedHighContrast) {
                MaterialTheme.colorScheme.primary
            } else {
                EmeraldSuccess
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (med.takenToday) 3.dp else 2.5.dp,
                    color = if (med.takenToday) takenAccentColor else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medication_card_${med.id}")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${med.scheduleTime} • ${med.pillsRemaining} Left",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row {
                            IconButton(
                                onClick = {
                                    onSpeakText("${med.name}. Dosage: ${med.dosage}. Schedule: ${med.scheduleTime}. ${med.withFoodInstruction}. What it does: ${med.purposeSimple}. ${med.warningNotes}")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read medicine instructions aloud"
                                )
                            }
                            IconButton(onClick = { onDeleteMedication(med) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete medicine",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = med.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = med.dosage,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = "💡 What it is for: ${med.purposeSimple}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "🍽️ Visual Reminder: ${med.withFoodInstruction}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (med.warningNotes.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Safety Note: ${med.warningNotes}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Simple 'Taken' Toggle Control Box (Logs to Caregiver History)
                    Surface(
                        color = if (med.takenToday) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (med.takenToday) takenAccentColor else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medication_taken_toggle_row_${med.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (med.takenToday) {
                                        "Taken Toggle: ✓ TAKEN TODAY"
                                    } else {
                                        "Taken Toggle: NOT TAKEN YET"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (med.takenToday) {
                                        "Logged to Caregiver History Log for $primaryCaregiverName"
                                    } else {
                                        "Flip 'Taken' toggle to log dose for caregiver"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = med.takenToday,
                                onCheckedChange = { onToggleTakenToday(med) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = takenAccentColor
                                ),
                                modifier = Modifier
                                    .semantics {
                                        contentDescription = "Taken toggle for ${med.name}"
                                    }
                                    .testTag("medication_taken_switch_${med.id}")
                            )
                        }
                    }

                    // Full-Width Accessible 'Taken' Action Button
                    Button(
                        onClick = { onToggleTakenToday(med) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (med.takenToday) takenAccentColor else MaterialTheme.colorScheme.primary,
                            contentColor = if (contrastMode == ContrastMode.ULTRA_LOW_VISION || contrastMode == ContrastMode.BLACK_AND_WHITE) {
                                Color.Black
                            } else {
                                Color.White
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 58.dp)
                            .testTag("toggle_med_taken_${med.id}")
                    ) {
                        Icon(
                            imageVector = if (med.takenToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (med.takenToday) {
                                "✓ Taken Today (Logged to Caregiver History)"
                            } else {
                                "Mark as Taken (Log to Caregiver History)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 5. Caregiver Medication History Log Section
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("caregiver_history_log_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "👩‍⚕️ CAREGIVER HISTORY LOG",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Caregiver: $primaryCaregiverName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "${medicationHistoryLogs.size} Entries",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("caregiver_history_count_badge")
                            )
                        }
                    }

                    Text(
                        text = "Every time you flip the 'Taken' toggle or confirm a pill by voice, a timestamped verification entry is recorded here for your caregiver and doctor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Caregiver Action Buttons (Read Log Aloud & Share Report)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onReadCaregiverReportAloud,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("read_caregiver_log_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Read Log",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = onSendCaregiverReport,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("share_caregiver_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Caregiver Report",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Filter Chips + Clear History Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Taken", "Undone").forEach { filter ->
                            val selected = selectedHistoryFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { selectedHistoryFilter = filter },
                                label = {
                                    Text(
                                        text = when (filter) {
                                            "Taken" -> "✓ Taken Doses"
                                            "Undone" -> "↺ Reset / Undone"
                                            else -> "All History (${medicationHistoryLogs.size})"
                                        },
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("caregiver_filter_${filter.lowercase()}")
                            )
                        }

                        if (medicationHistoryLogs.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onClearHistoryLogs,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("clear_caregiver_log_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Log")
                            }
                        }
                    }

                    if (filteredHistoryLogs.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("caregiver_history_empty_state")
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "No Caregiver History Entries Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Toggle 'Taken' on any medication above to log a verified intake record for your caregiver.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        filteredHistoryLogs.forEach { log ->
                            val isTakenStatus = log.status.equals("TAKEN", ignoreCase = true)
                            val badgeColor = if (contrastMode.isForcedHighContrast) {
                                MaterialTheme.colorScheme.primary
                            } else if (isTakenStatus) {
                                EmeraldSuccess
                            } else {
                                MaterialTheme.colorScheme.secondary
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(2.dp, badgeColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("caregiver_history_item_${log.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = badgeColor,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (isTakenStatus) "✓ TAKEN" else "↺ RESET",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (contrastMode == ContrastMode.ULTRA_LOW_VISION || contrastMode == ContrastMode.BLACK_AND_WHITE) {
                                                        Color.Black
                                                    } else {
                                                        Color.White
                                                    },
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Text(
                                                text = log.loggedAtText,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    onSpeakText(
                                                        "Caregiver History Entry: ${log.medicationName}, ${log.dosage}, marked ${log.status} at ${log.loggedAtText} via ${log.loggedByMethod}. ${log.caregiverNote}"
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("speak_caregiver_log_${log.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Read caregiver log entry aloud",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteHistoryLog(log) },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_caregiver_log_${log.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete caregiver log entry",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "${log.medicationName} • ${log.dosage}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "Scheduled: ${log.scheduleTime} • Logged via: ${log.loggedByMethod} • ${log.pillsRemainingAfter} pills left",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (log.caregiverNote.isNotBlank()) {
                                        Text(
                                            text = log.caregiverNote,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. AI Medication Interaction & Safety Q&A Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI MEDICATION & INTERACTION HELPER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Text(
                        text = "Tap a common question or ask about your medicines in plain language:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    sampleSafetyQuestions.forEachIndexed { idx, q ->
                        OutlinedButton(
                            onClick = { onAskSafetyQuestion(q) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("med_safety_question_$idx")
                        ) {
                            Text(
                                text = "“$q”",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customQuestion,
                        onValueChange = { customQuestion = it },
                        label = { Text("Ask another question about your pills") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (customQuestion.isNotBlank()) {
                                onAskSafetyQuestion(customQuestion)
                                customQuestion = ""
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                    ) {
                        Text("Ask Gemini Medication Helper", style = MaterialTheme.typography.titleMedium)
                    }

                    if (isAiProcessing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Checking medication guidance…", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (!medicationAiAnswer.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PLAIN-LANGUAGE GUIDANCE:",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { onSpeakText(medicationAiAnswer) }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Read answer aloud"
                                        )
                                    }
                                }
                                Text(
                                    text = medicationAiAnswer,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
