package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistDao {
    // --- Reminders ---
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT COUNT(*) FROM reminders")
    suspend fun getReminderCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    // --- Alarms ---
    @Query("SELECT * FROM alarms ORDER BY createdAt ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getAlarmCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity)

    // --- Appointments / Meetings / Travel ---
    @Query("SELECT * FROM appointments ORDER BY createdAt ASC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT COUNT(*) FROM appointments")
    suspend fun getAppointmentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Delete
    suspend fun deleteAppointment(appointment: AppointmentEntity)

    // --- Call Records ---
    @Query("SELECT * FROM call_records ORDER BY createdAt DESC")
    fun getAllCallRecords(): Flow<List<CallRecordEntity>>

    @Query("SELECT COUNT(*) FROM call_records")
    suspend fun getCallRecordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(callRecord: CallRecordEntity): Long

    @Update
    suspend fun updateCallRecord(callRecord: CallRecordEntity)

    @Delete
    suspend fun deleteCallRecord(callRecord: CallRecordEntity)

    // --- Medications ---
    @Query("SELECT * FROM medications ORDER BY takenToday ASC, createdAt ASC")
    fun getAllMedications(): Flow<List<MedicationEntity>>

    @Query("SELECT COUNT(*) FROM medications")
    suspend fun getMedicationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    // --- Emergency Contacts ---
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimaryCaregiver DESC, createdAt ASC")
    fun getAllEmergencyContacts(): Flow<List<EmergencyContactEntity>>

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getEmergencyContactCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContact(contact: EmergencyContactEntity): Long

    @Delete
    suspend fun deleteEmergencyContact(contact: EmergencyContactEntity)

    // --- Visual AI Scans ---
    @Query("SELECT * FROM visual_scans ORDER BY createdAt DESC")
    fun getAllVisualScans(): Flow<List<VisualScanEntity>>

    @Query("SELECT COUNT(*) FROM visual_scans")
    suspend fun getVisualScanCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisualScan(scan: VisualScanEntity): Long

    @Delete
    suspend fun deleteVisualScan(scan: VisualScanEntity)

    // --- Voice Command Logs (Last 10 Executed Commands) ---
    @Query("SELECT * FROM voice_command_logs ORDER BY createdAt DESC, id DESC LIMIT 10")
    fun getLastTenVoiceCommandLogs(): Flow<List<VoiceCommandLogEntity>>

    @Query("SELECT * FROM voice_command_logs ORDER BY createdAt DESC, id DESC LIMIT 10")
    suspend fun getLastTenVoiceCommandLogsOnce(): List<VoiceCommandLogEntity>

    @Query("SELECT COUNT(*) FROM voice_command_logs")
    suspend fun getVoiceCommandLogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceCommandLog(log: VoiceCommandLogEntity): Long

    @Delete
    suspend fun deleteVoiceCommandLog(log: VoiceCommandLogEntity)

    @Query("DELETE FROM voice_command_logs WHERE id NOT IN (SELECT id FROM voice_command_logs ORDER BY createdAt DESC, id DESC LIMIT 10)")
    suspend fun pruneOldVoiceCommandLogs()

    @Query("DELETE FROM voice_command_logs")
    suspend fun clearAllVoiceCommandLogs()

    // --- Caregiver Medication Intake History Logs ---
    @Query("SELECT * FROM medication_history_logs ORDER BY createdAt DESC, id DESC")
    fun getAllMedicationHistoryLogs(): Flow<List<MedicationHistoryLogEntity>>

    @Query("SELECT * FROM medication_history_logs ORDER BY createdAt DESC, id DESC")
    suspend fun getAllMedicationHistoryLogsOnce(): List<MedicationHistoryLogEntity>

    @Query("SELECT COUNT(*) FROM medication_history_logs")
    suspend fun getMedicationHistoryLogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationHistoryLog(log: MedicationHistoryLogEntity): Long

    @Delete
    suspend fun deleteMedicationHistoryLog(log: MedicationHistoryLogEntity)

    @Query("DELETE FROM medication_history_logs")
    suspend fun clearAllMedicationHistoryLogs()
}
