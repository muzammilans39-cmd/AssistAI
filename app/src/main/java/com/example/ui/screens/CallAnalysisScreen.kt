package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CallRecordEntity
import com.example.ui.components.AnimatedVoiceWaveform
import com.example.ui.theme.EmeraldSuccess

@Composable
fun CallAnalysisScreen(
    callRecords: List<CallRecordEntity>,
    selectedCallId: Int?,
    isRecordingCall: Boolean,
    recordingSeconds: Int,
    draftTranscript: String,
    isAnalyzingCall: Boolean,
    onUpdateDraftTranscript: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopAndAnalyzeRecording: () -> Unit,
    onUploadAudioUri: (Uri) -> Unit,
    onAnalyzeSampleTranscript: (String) -> Unit,
    onSelectCallRecord: (Int) -> Unit,
    onSaveAsAppointment: (CallRecordEntity) -> Unit,
    onSaveAsReminder: (CallRecordEntity) -> Unit,
    onDeleteCallRecord: (CallRecordEntity) -> Unit,
    onSpeakText: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadAudioUri(uri)
        }
    }

    var showTranscriptEditor by rememberSaveable { mutableStateOf(false) }
    val selectedCall = callRecords.firstOrNull { it.id == selectedCallId } ?: callRecords.firstOrNull()

    val sampleScenarios = listOf(
        "Pharmacy Refill Call" to "Hello Mrs. Eleanor, this is Pharmacist Kevin from Greenleaf Pharmacy at 450 Main Street. Your prescription refill for Lisinopril is ready for pickup Tomorrow at 2:30 PM. Please bring your ID card and take 1 tablet every morning with food. Call us at 555-0319 if you need free home delivery.",
        "Eye Clinic Checkup Call" to "Good morning Mr. Arthur! This is Nurse Brenda from Oakwood Eye Center, Suite 108. We are confirming your cataract follow-up appointment on Monday at 11:15 AM with Dr. Robert Chen. Please bring your dark sunglasses and arrange a ride home because dilating drops will blur your vision for 3 hours. Clinic number: 555-0642.",
        "Unverified Telemarketing Call" to "Hello! This is an unscheduled promotional sales call offering a same-day home window inspection if you agree to an immediate upfront reservation deposit today."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("call_analysis_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Mandatory Legal Consent Disclaimer Banner
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("call_consent_disclaimer_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Legal Notice",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Legal Consent Disclaimer: Recording phone calls or conversations may require consent from all participants. Always inform callers and follow your local laws.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Record Call or Upload Audio Controls
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRecordingCall) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 3.dp,
                    color = if (isRecordingCall) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "RECORD CALL OR UPLOAD AUDIO FOR AI ANALYSIS",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (isRecordingCall) {
                        AnimatedVoiceWaveform(
                            isActive = true,
                            amplitudeLevel = 0.78f,
                            statusTitle = "RECORDING CALL AUDIO",
                            subtitleText = "Speak clearly or tap Stop & Analyze when finished",
                            elapsedSeconds = recordingSeconds,
                            onTapWaveform = onStopAndAnalyzeRecording
                        )
                    }

                    // Large Record / Stop Button
                    Button(
                        onClick = {
                            if (isRecordingCall) onStopAndAnalyzeRecording() else onStartRecording()
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecordingCall) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .testTag("record_call_button")
                    ) {
                        Icon(
                            imageVector = if (isRecordingCall) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isRecordingCall) "Stop & Analyze Recording with AI" else "Record Call / Conversation",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Upload Audio File Button
                    OutlinedButton(
                        onClick = {
                            try {
                                audioPickerLauncher.launch("audio/*")
                            } catch (_: Exception) {
                                onAnalyzeSampleTranscript(sampleScenarios.first().second)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .testTag("upload_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Upload Audio Recording (.mp3, .wav, .m4a)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Test Sample Call Recordings
                    Text(
                        text = "Or test instant AI Call Analysis with a sample call:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    sampleScenarios.forEachIndexed { idx, (label, sampleText) ->
                        OutlinedButton(
                            onClick = { onAnalyzeSampleTranscript(sampleText) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("analyze_sample_call_$idx")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analyze Sample: $label",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showTranscriptEditor = !showTranscriptEditor },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (showTranscriptEditor) "Hide Custom Call Text Input" else "Paste or Type Custom Call Transcript",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    if (showTranscriptEditor) {
                        OutlinedTextField(
                            value = draftTranscript,
                            onValueChange = onUpdateDraftTranscript,
                            label = { Text("Paste or speak call transcript here") },
                            minLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_call_transcript_input")
                        )
                        Button(
                            onClick = {
                                if (draftTranscript.isNotBlank()) {
                                    onAnalyzeSampleTranscript(draftTranscript)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .testTag("analyze_custom_transcript_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Transcript with Gemini AI", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    if (isAnalyzingCall) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Gemini AI is transcribing and extracting names, dates, times, and appointments…",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 3. Selector for Saved Analyzed Calls
        if (callRecords.size > 1) {
            item {
                Text(
                    text = "RECENT ANALYZED CALLS (${callRecords.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    callRecords.forEach { record ->
                        val isSelected = selectedCall?.id == record.id
                        Button(
                            onClick = { onSelectCallRecord(record.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("select_call_record_${record.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${record.title} (${record.recordedAtText})",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Text("VIEWING", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Selected Call Analysis Result Details
        if (selectedCall != null) {
            // AI Scam & Fraud Shield Card
            item {
                val isHighRisk = selectedCall.scamRiskLevel == "HIGH_SCAM_RISK"
                val isCaution = selectedCall.scamRiskLevel == "CAUTION"
                val shieldBgColor = when {
                    isHighRisk -> MaterialTheme.colorScheme.errorContainer
                    isCaution -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
                val shieldBorderColor = when {
                    isHighRisk -> MaterialTheme.colorScheme.error
                    isCaution -> MaterialTheme.colorScheme.secondary
                    else -> EmeraldSuccess
                }
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = shieldBgColor),
                    border = BorderStroke(3.dp, shieldBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scam_shield_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    isHighRisk -> "🚨 SCAM SHIELD: HIGH FRAUD RISK!"
                                    isCaution -> "⚠️ SCAM SHIELD: USE CAUTION"
                                    else -> "🛡️ SCAM SHIELD: VERIFIED SAFE CALL"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isHighRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { onSpeakText(selectedCall.scamWarningExplanation) }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read scam shield assessment aloud"
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = selectedCall.scamWarningExplanation,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isHighRisk) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // AI Summary & Plain Language Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("call_summary_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI CONVERSATION SUMMARY",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedCall.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = {
                                    onSpeakText("Summary for ${selectedCall.title}: ${selectedCall.summary}. ${selectedCall.simpleExplanation}")
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read AI summary aloud",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = selectedCall.summary,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "💡 EASY LANGUAGE EXPLANATION:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedCall.simpleExplanation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Automatic Detection & Save to Reminder / Calendar Event Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("call_detected_event_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DETECTED ${selectedCall.detectedCategory.uppercase()}:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = "${selectedCall.suggestedActionTitle} • ${selectedCall.suggestedActionDate} at ${selectedCall.suggestedActionTime}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Location: ${selectedCall.suggestedActionLocation}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onSaveAsAppointment(selectedCall) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 60.dp)
                                    .testTag("save_call_as_appointment_button")
                            ) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Event", style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = { onSaveAsReminder(selectedCall) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 60.dp)
                                    .testTag("save_call_as_reminder_button")
                            ) {
                                Icon(imageVector = Icons.Default.AlarmAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Reminder", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // Extracted Important Information (Names, Dates, Times, Locations, Phone Numbers, Tasks, Instructions)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extracted_entities_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "EXTRACTED IMPORTANT INFORMATION",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        ExtractedInfoRow("👤 Names", selectedCall.extractedNames)
                        ExtractedInfoRow("📅 Dates", selectedCall.extractedDates)
                        ExtractedInfoRow("🕒 Times", selectedCall.extractedTimes)
                        ExtractedInfoRow("📍 Locations", selectedCall.extractedLocations)
                        ExtractedInfoRow("☎️ Phone Numbers", selectedCall.extractedPhones)
                        ExtractedInfoRow("✅ Tasks & Promises", selectedCall.extractedTasks)
                        ExtractedInfoRow("⚠️ Important Instructions", selectedCall.extractedInstructions)
                    }
                }
            }

            // Full Speech-to-Text Transcript Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("call_transcript_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SPEECH-TO-TEXT TRANSCRIPT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(
                                    onClick = { onSpeakText("Full Call Transcript: ${selectedCall.transcript}") }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Read full transcript aloud"
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCallRecord(selectedCall) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete call record",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "“${selectedCall.transcript}”",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractedInfoRow(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
