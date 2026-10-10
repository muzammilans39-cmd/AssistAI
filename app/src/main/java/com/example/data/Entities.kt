package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dateText: String,
    val timeText: String,
    val category: String = "Personal",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isDaily: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timeText: String,
    val label: String,
    val repeatDays: String,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val eventType: String, // "Appointment", "Meeting", "Travel"
    val dateText: String,
    val timeText: String,
    val location: String,
    val notes: String = "",
    val sourceCallId: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val recordedAtText: String,
    val durationText: String,
    val transcript: String,
    val summary: String,
    val simpleExplanation: String,
    val extractedNames: String,
    val extractedDates: String,
    val extractedTimes: String,
    val extractedLocations: String,
    val extractedPhones: String,
    val extractedTasks: String,
    val extractedInstructions: String,
    val detectedCategory: String,
    val suggestedActionTitle: String,
    val suggestedActionDate: String,
    val suggestedActionTime: String,
    val suggestedActionLocation: String,
    val suggestedActionNotes: String,
    val scamRiskLevel: String = "SAFE", // "SAFE", "CAUTION", "HIGH_SCAM_RISK"
    val scamWarningExplanation: String = "Legitimate call. No financial or identity scam indicators detected.",
    val isSavedToPlanner: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val scheduleTime: String,
    val withFoodInstruction: String,
    val purposeSimple: String,
    val pillsRemaining: Int,
    val takenToday: Boolean = false,
    val warningNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val isPrimaryCaregiver: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "visual_scans")
data class VisualScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val scanType: String, // "Medicine Label", "Bill / Letter", "Currency", "Scene Description"
    val spokenDescription: String,
    val extractedLargeText: String,
    val safetyOrActionNote: String,
    val suggestedReminderTitle: String,
    val suggestedReminderTime: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_command_logs")
data class VoiceCommandLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val spokenCommand: String,
    val aiActionTaken: String,
    val spokenConfirmation: String,
    val intentCategory: String = "Voice Command",
    val executedAtText: String = "Just now",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "medication_history_logs")
data class MedicationHistoryLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationName: String,
    val dosage: String,
    val scheduleTime: String,
    val status: String = "TAKEN", // "TAKEN" or "UNDONE"
    val loggedAtText: String,
    val loggedByMethod: String = "Taken Toggle",
    val caregiverNote: String = "",
    val pillsRemainingAfter: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
