package com.example.data

import kotlinx.coroutines.flow.Flow

class AssistRepository(private val dao: AssistDao) {
    val reminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
    val alarms: Flow<List<AlarmEntity>> = dao.getAllAlarms()
    val appointments: Flow<List<AppointmentEntity>> = dao.getAllAppointments()
    val callRecords: Flow<List<CallRecordEntity>> = dao.getAllCallRecords()
    val medications: Flow<List<MedicationEntity>> = dao.getAllMedications()
    val emergencyContacts: Flow<List<EmergencyContactEntity>> = dao.getAllEmergencyContacts()
    val visualScans: Flow<List<VisualScanEntity>> = dao.getAllVisualScans()
    val voiceCommandLogs: Flow<List<VoiceCommandLogEntity>> = dao.getLastTenVoiceCommandLogs()
    val medicationHistoryLogs: Flow<List<MedicationHistoryLogEntity>> = dao.getAllMedicationHistoryLogs()

    suspend fun insertReminder(reminder: ReminderEntity): Long = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: ReminderEntity) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: ReminderEntity) = dao.deleteReminder(reminder)

    suspend fun insertAlarm(alarm: AlarmEntity): Long = dao.insertAlarm(alarm)
    suspend fun updateAlarm(alarm: AlarmEntity) = dao.updateAlarm(alarm)
    suspend fun deleteAlarm(alarm: AlarmEntity) = dao.deleteAlarm(alarm)

    suspend fun insertAppointment(appointment: AppointmentEntity): Long = dao.insertAppointment(appointment)
    suspend fun updateAppointment(appointment: AppointmentEntity) = dao.updateAppointment(appointment)
    suspend fun deleteAppointment(appointment: AppointmentEntity) = dao.deleteAppointment(appointment)

    suspend fun insertCallRecord(callRecord: CallRecordEntity): Long = dao.insertCallRecord(callRecord)
    suspend fun updateCallRecord(callRecord: CallRecordEntity) = dao.updateCallRecord(callRecord)
    suspend fun deleteCallRecord(callRecord: CallRecordEntity) = dao.deleteCallRecord(callRecord)

    suspend fun insertMedication(medication: MedicationEntity): Long = dao.insertMedication(medication)
    suspend fun updateMedication(medication: MedicationEntity) = dao.updateMedication(medication)
    suspend fun deleteMedication(medication: MedicationEntity) = dao.deleteMedication(medication)

    suspend fun insertEmergencyContact(contact: EmergencyContactEntity): Long = dao.insertEmergencyContact(contact)
    suspend fun deleteEmergencyContact(contact: EmergencyContactEntity) = dao.deleteEmergencyContact(contact)

    suspend fun insertVisualScan(scan: VisualScanEntity): Long = dao.insertVisualScan(scan)
    suspend fun deleteVisualScan(scan: VisualScanEntity) = dao.deleteVisualScan(scan)

    suspend fun getLastTenVoiceCommandLogsOnce(): List<VoiceCommandLogEntity> =
        dao.getLastTenVoiceCommandLogsOnce()

    suspend fun insertVoiceCommandLog(log: VoiceCommandLogEntity): Long {
        val id = dao.insertVoiceCommandLog(log)
        dao.pruneOldVoiceCommandLogs()
        return id
    }

    suspend fun deleteVoiceCommandLog(log: VoiceCommandLogEntity) = dao.deleteVoiceCommandLog(log)
    suspend fun clearAllVoiceCommandLogs() = dao.clearAllVoiceCommandLogs()

    suspend fun getAllMedicationHistoryLogsOnce(): List<MedicationHistoryLogEntity> =
        dao.getAllMedicationHistoryLogsOnce()

    suspend fun insertMedicationHistoryLog(log: MedicationHistoryLogEntity): Long =
        dao.insertMedicationHistoryLog(log)

    suspend fun deleteMedicationHistoryLog(log: MedicationHistoryLogEntity) =
        dao.deleteMedicationHistoryLog(log)

    suspend fun clearAllMedicationHistoryLogs() =
        dao.clearAllMedicationHistoryLogs()

    suspend fun ensureDemoDataSeeded() {
        val hasReminders = dao.getReminderCount() > 0
        val hasAppointments = dao.getAppointmentCount() > 0
        val hasAlarms = dao.getAlarmCount() > 0
        val hasCalls = dao.getCallRecordCount() > 0
        val hasMedications = dao.getMedicationCount() > 0
        val hasContacts = dao.getEmergencyContactCount() > 0
        val hasScans = dao.getVisualScanCount() > 0
        val hasCommandLogs = dao.getVoiceCommandLogCount() > 0
        val hasMedHistoryLogs = dao.getMedicationHistoryLogCount() > 0

        if (!hasReminders) {
            dao.insertReminder(
                ReminderEntity(
                    title = "Call daughter",
                    dateText = "Today",
                    timeText = "7:00 PM",
                    category = "Call",
                    notes = "Check in with Sarah and confirm Friday's train station pickup.",
                    isCompleted = false,
                    isDaily = false
                )
            )
            dao.insertReminder(
                ReminderEntity(
                    title = "Medicine reminder",
                    dateText = "Daily",
                    timeText = "9:00 AM",
                    category = "Medicine",
                    notes = "Take 1 blood pressure tablet (Amlodipine 5mg) with a full glass of water after breakfast.",
                    isCompleted = false,
                    isDaily = true
                )
            )
            dao.insertReminder(
                ReminderEntity(
                    title = "Evening eye drops",
                    dateText = "Today",
                    timeText = "8:30 PM",
                    category = "Medicine",
                    notes = "Put 1 lubricating drop in each eye before bed.",
                    isCompleted = false,
                    isDaily = true
                )
            )
        }

        if (!hasAppointments) {
            dao.insertAppointment(
                AppointmentEntity(
                    title = "Doctor appointment",
                    eventType = "Appointment",
                    dateText = "Tomorrow",
                    timeText = "10:00 AM",
                    location = "City Medical Center, Suite 204",
                    notes = "Routine checkup with Dr. Anita Patel. Bring current medication list and insurance card."
                )
            )
            dao.insertAppointment(
                AppointmentEntity(
                    title = "Train journey",
                    eventType = "Travel",
                    dateText = "Friday",
                    timeText = "8:30 AM",
                    location = "Central Station, Platform 3",
                    notes = "Express Train #402 to Oakridge. Coach B, Lower Berth Seat 12. Assisted boarding requested."
                )
            )
            dao.insertAppointment(
                AppointmentEntity(
                    title = "Senior Community Book Club",
                    eventType = "Meeting",
                    dateText = "Today",
                    timeText = "4:00 PM",
                    location = "Maplewood Community Center, Room 1B",
                    notes = "Weekly audio-book discussion group with Mr. Henderson."
                )
            )
        }

        if (!hasAlarms) {
            dao.insertAlarm(
                AlarmEntity(
                    timeText = "9:00 AM",
                    label = "Medicine reminder",
                    repeatDays = "Daily",
                    isEnabled = true
                )
            )
            dao.insertAlarm(
                AlarmEntity(
                    timeText = "7:30 AM",
                    label = "Morning Wake Up & Stretch",
                    repeatDays = "Mon, Tue, Wed, Thu, Fri",
                    isEnabled = true
                )
            )
            dao.insertAlarm(
                AlarmEntity(
                    timeText = "6:30 PM",
                    label = "Prepare Dinner & Call Daughter",
                    repeatDays = "Daily",
                    isEnabled = true
                )
            )
        }

        if (!hasCalls) {
            dao.insertCallRecord(
                CallRecordEntity(
                    title = "Dr. Anita Patel's Clinic – Appointment Call",
                    recordedAtText = "Today, 11:15 AM",
                    durationText = "1m 42s",
                    transcript = "Hello Mr. Arthur, this is Nurse Clara calling from Dr. Anita Patel's clinic at City Medical Center, Suite 204. I'm calling to confirm your Doctor appointment scheduled for Tomorrow at 10:00 AM. Please remember to fast for 8 hours before your blood test—drink only plain water after midnight. Also, please bring your blood pressure notebook and your current pill bottles. If you need to reschedule or need wheelchair assistance at the main entrance, please call our front desk at 555-0194. See you tomorrow at 10:00 AM!",
                    summary = "Nurse Clara from Dr. Anita Patel's clinic confirmed your doctor appointment for tomorrow at 10:00 AM at City Medical Center, Suite 204.",
                    simpleExplanation = "In simple words: You have a doctor visit tomorrow morning at 10:00 AM. Do not eat food after midnight tonight, but you CAN drink plain water. Put your blood pressure notebook and medicine bottles in your bag tonight so they are ready.",
                    extractedNames = "Nurse Clara • Dr. Anita Patel • Mr. Arthur",
                    extractedDates = "Tomorrow",
                    extractedTimes = "10:00 AM • After midnight (8-hour fast)",
                    extractedLocations = "City Medical Center, Suite 204",
                    extractedPhones = "555-0194",
                    extractedTasks = "Bring blood pressure notebook • Bring current pill bottles",
                    extractedInstructions = "Fast for 8 hours before blood test (plain water only after midnight) • Call 555-0194 for wheelchair assistance at entrance",
                    detectedCategory = "Appointment",
                    suggestedActionTitle = "Doctor appointment (Dr. Anita Patel)",
                    suggestedActionDate = "Tomorrow",
                    suggestedActionTime = "10:00 AM",
                    suggestedActionLocation = "City Medical Center, Suite 204",
                    suggestedActionNotes = "Fast 8 hrs (water only). Bring BP notebook & pill bottles. Clinic phone: 555-0194.",
                    scamRiskLevel = "SAFE",
                    scamWarningExplanation = "SAFE CALL: Verified medical clinic appointment reminder from Dr. Anita Patel's office.",
                    isSavedToPlanner = true
                )
            )
            dao.insertCallRecord(
                CallRecordEntity(
                    title = "National Rail Assisted Travel Desk",
                    recordedAtText = "Yesterday, 3:20 PM",
                    durationText = "1m 18s",
                    transcript = "Good afternoon! This is David from the National Rail Passenger Assistance Desk at 555-0822. Your Train journey on Friday at 8:30 AM from Central Station, Platform 3 is confirmed. You are seated in Coach B, Seat 12. Our station staff member Maria will meet you at the main taxi drop-off zone at 8:00 AM with a mobility buggy. Please keep your booking reference NR-8841 handy.",
                    summary = "David from National Rail confirmed your train journey on Friday at 8:30 AM from Central Station, Platform 3 (Coach B, Seat 12) with mobility assistance at 8:00 AM.",
                    simpleExplanation = "In simple words: Your train leaves Friday morning at 8:30 AM from Platform 3. Arrive by 8:00 AM at the taxi drop-off area—a helper named Maria will meet you with a ride straight to your train seat.",
                    extractedNames = "David (Rail Desk) • Maria (Station Staff)",
                    extractedDates = "Friday",
                    extractedTimes = "8:30 AM (Departure) • 8:00 AM (Meet Assistance)",
                    extractedLocations = "Central Station, Platform 3 (Taxi Drop-off Zone)",
                    extractedPhones = "555-0822",
                    extractedTasks = "Arrive at taxi drop-off by 8:00 AM • Keep booking code NR-8841 ready",
                    extractedInstructions = "Meet staff member Maria at 8:00 AM for mobility buggy to Coach B, Seat 12",
                    detectedCategory = "Travel",
                    suggestedActionTitle = "Train journey (Assisted Boarding)",
                    suggestedActionDate = "Friday",
                    suggestedActionTime = "8:30 AM",
                    suggestedActionLocation = "Central Station, Platform 3",
                    suggestedActionNotes = "Meet Maria at 8:00 AM at taxi drop-off. Coach B, Seat 12. Ref: NR-8841.",
                    scamRiskLevel = "SAFE",
                    scamWarningExplanation = "SAFE CALL: Standard rail passenger assistance confirmation with booking reference.",
                    isSavedToPlanner = true
                )
            )
        }

        if (!hasMedications) {
            dao.insertMedication(
                MedicationEntity(
                    name = "Amlodipine (Blood Pressure)",
                    dosage = "5 mg • 1 White Round Tablet",
                    scheduleTime = "9:00 AM (Morning)",
                    withFoodInstruction = "Take after breakfast with a full glass of water",
                    purposeSimple = "Keeps your blood pressure steady and protects your heart.",
                    pillsRemaining = 18,
                    takenToday = false,
                    warningNotes = "Avoid grapefruit juice. Stand up slowly from chairs to prevent dizziness."
                )
            )
            dao.insertMedication(
                MedicationEntity(
                    name = "Metformin (Blood Sugar)",
                    dosage = "500 mg • 1 Oval Tablet",
                    scheduleTime = "1:00 PM & 7:00 PM",
                    withFoodInstruction = "Always take WITH lunch and dinner",
                    purposeSimple = "Helps keep your blood sugar at a healthy level after meals.",
                    pillsRemaining = 24,
                    takenToday = false,
                    warningNotes = "Take with food to avoid an upset stomach."
                )
            )
            dao.insertMedication(
                MedicationEntity(
                    name = "Lubricating Eye Drops",
                    dosage = "1 Drop in Each Eye",
                    scheduleTime = "8:30 PM (Bedtime)",
                    withFoodInstruction = "No food needed • Wash hands before use",
                    purposeSimple = "Soothes dry eyes and keeps your vision comfortable.",
                    pillsRemaining = 12,
                    takenToday = false,
                    warningNotes = "Wait 5 minutes before using any other eye drops."
                )
            )
        }

        if (!hasContacts) {
            dao.insertEmergencyContact(
                EmergencyContactEntity(
                    name = "Sarah Miller (Daughter)",
                    relationship = "Primary Caregiver & Daughter",
                    phoneNumber = "555-0911",
                    isPrimaryCaregiver = true,
                    notes = "Lives 10 minutes away. Has spare house key and medical power of attorney."
                )
            )
            dao.insertEmergencyContact(
                EmergencyContactEntity(
                    name = "Dr. Anita Patel's Clinic",
                    relationship = "Primary Care Physician",
                    phoneNumber = "555-0194",
                    isPrimaryCaregiver = false,
                    notes = "City Medical Center, Suite 204. 24-hour nurse triage line."
                )
            )
            dao.insertEmergencyContact(
                EmergencyContactEntity(
                    name = "Senior Mobility & Transit Dispatch",
                    relationship = "Wheelchair & Assisted Ride Service",
                    phoneNumber = "555-0822",
                    isPrimaryCaregiver = false,
                    notes = "Door-to-door senior shuttle service."
                )
            )
        }

        if (!hasScans) {
            dao.insertVisualScan(
                VisualScanEntity(
                    title = "Amlodipine 5mg Prescription Bottle",
                    scanType = "Medicine Label",
                    spokenDescription = "This is a white prescription pill bottle from Greenleaf Pharmacy for Arthur Miller. It contains Amlodipine 5 milligram tablets for blood pressure.",
                    extractedLargeText = "AMLODIPINE BESYLATE 5 MG TABLET • Take 1 tablet by mouth every morning after breakfast • Refills left: 2 • Rx #449102",
                    safetyOrActionNote = "IMPORTANT: Take once daily in the morning. Do not take with grapefruit juice. Expires 12/2027.",
                    suggestedReminderTitle = "Take Amlodipine 5mg Tablet",
                    suggestedReminderTime = "9:00 AM"
                )
            )
        }

        if (!hasCommandLogs) {
            val baseTime = System.currentTimeMillis() - 3600_000L
            val initialLogs = defaultTenVoiceCommandLogs(baseTime)
            initialLogs.forEach { dao.insertVoiceCommandLog(it) }
        }

        if (!hasMedHistoryLogs) {
            val baseTime = System.currentTimeMillis() - 86_400_000L
            val initialHistoryLogs = listOf(
                MedicationHistoryLogEntity(
                    medicationId = 1,
                    medicationName = "Amlodipine (Blood Pressure)",
                    dosage = "5 mg • 1 White Round Tablet",
                    scheduleTime = "9:00 AM (Morning)",
                    status = "TAKEN",
                    loggedAtText = "Yesterday, 9:03 AM",
                    loggedByMethod = "Taken Toggle",
                    caregiverNote = "Caregiver Log: Verified 1 White Round Tablet taken after breakfast with water. 18 pills remaining.",
                    pillsRemainingAfter = 18,
                    createdAt = baseTime + 1000L
                ),
                MedicationHistoryLogEntity(
                    medicationId = 2,
                    medicationName = "Metformin (Blood Sugar)",
                    dosage = "500 mg • 1 Oval Tablet",
                    scheduleTime = "1:00 PM & 7:00 PM",
                    status = "TAKEN",
                    loggedAtText = "Yesterday, 1:10 PM",
                    loggedByMethod = "Taken Toggle",
                    caregiverNote = "Caregiver Log: Verified lunch dose taken with meal. 24 pills remaining.",
                    pillsRemainingAfter = 24,
                    createdAt = baseTime + 2000L
                ),
                MedicationHistoryLogEntity(
                    medicationId = 3,
                    medicationName = "Lubricating Eye Drops",
                    dosage = "1 Drop in Each Eye",
                    scheduleTime = "8:30 PM (Bedtime)",
                    status = "TAKEN",
                    loggedAtText = "Yesterday, 8:28 PM",
                    loggedByMethod = "Voice Command",
                    caregiverNote = "Caregiver Log: Bedtime eye drops logged via voice confirmation. 12 doses remaining.",
                    pillsRemainingAfter = 12,
                    createdAt = baseTime + 3000L
                )
            )
            initialHistoryLogs.forEach { dao.insertMedicationHistoryLog(it) }
        }
    }

    companion object {
        fun defaultTenVoiceCommandLogs(baseTime: Long = System.currentTimeMillis() - 3600_000L): List<VoiceCommandLogEntity> {
            return listOf(
                VoiceCommandLogEntity(
                    id = 1,
                    spokenCommand = "Set a reminder to call daughter Sarah at 7:00 PM today",
                    aiActionTaken = "✓ Saved Reminder: Call daughter (Today • 7:00 PM)",
                    spokenConfirmation = "I have saved your reminder to call your daughter Sarah today at 7:00 PM.",
                    intentCategory = "Reminder",
                    executedAtText = "Today, 8:10 AM",
                    createdAt = baseTime + 1000L
                ),
                VoiceCommandLogEntity(
                    id = 2,
                    spokenCommand = "What appointments do I have tomorrow?",
                    aiActionTaken = "📅 Read Upcoming Events (Doctor appointment at 10:00 AM)",
                    spokenConfirmation = "Tomorrow at 10:00 AM you have a Doctor appointment with Dr. Anita Patel at City Medical Center, Suite 204.",
                    intentCategory = "Appointment",
                    executedAtText = "Today, 8:25 AM",
                    createdAt = baseTime + 2000L
                ),
                VoiceCommandLogEntity(
                    id = 3,
                    spokenCommand = "Set a daily alarm for 9:00 AM for my morning blood pressure medicine",
                    aiActionTaken = "✓ Alarm Set: 9:00 AM (Medicine reminder)",
                    spokenConfirmation = "Your daily 9:00 AM alarm for Medicine reminder is now enabled.",
                    intentCategory = "Alarm",
                    executedAtText = "Today, 8:40 AM",
                    createdAt = baseTime + 3000L
                ),
                VoiceCommandLogEntity(
                    id = 4,
                    spokenCommand = "I took my morning blood pressure medicine",
                    aiActionTaken = "💊 Marked Medication as Taken: Amlodipine (Blood Pressure)",
                    spokenConfirmation = "Marked Amlodipine (5 mg • 1 White Round Tablet) as Taken today and logged to Caregiver History.",
                    intentCategory = "Medication",
                    executedAtText = "Today, 9:02 AM",
                    createdAt = baseTime + 4000L
                ),
                VoiceCommandLogEntity(
                    id = 5,
                    spokenCommand = "Summarize my last phone call from Dr. Patel's clinic",
                    aiActionTaken = "📞 Last Recording Summary Read Aloud",
                    spokenConfirmation = "Nurse Clara confirmed your appointment tomorrow at 10:00 AM. Fast for 8 hours after midnight and bring your pill bottles.",
                    intentCategory = "Call Summary",
                    executedAtText = "Today, 9:20 AM",
                    createdAt = baseTime + 5000L
                ),
                VoiceCommandLogEntity(
                    id = 6,
                    spokenCommand = "What is my battery level?",
                    aiActionTaken = "🔋 Checked Device Battery Status",
                    spokenConfirmation = "Read your current battery percentage and power status aloud.",
                    intentCategory = "Accessibility",
                    executedAtText = "Today, 9:35 AM",
                    createdAt = baseTime + 6000L
                ),
                VoiceCommandLogEntity(
                    id = 7,
                    spokenCommand = "Read my reminders for today",
                    aiActionTaken = "🔊 Read 3 Active Reminders Aloud",
                    spokenConfirmation = "You have 3 active reminders: Call daughter at 7:00 PM; Medicine reminder at 9:00 AM; Evening eye drops at 8:30 PM.",
                    intentCategory = "Reminder",
                    executedAtText = "Today, 9:50 AM",
                    createdAt = baseTime + 7000L
                ),
                VoiceCommandLogEntity(
                    id = 8,
                    spokenCommand = "Create a meeting for Senior Book Club at 4:00 PM today",
                    aiActionTaken = "✓ Saved Meeting: Senior Community Book Club (Today • 4:00 PM)",
                    spokenConfirmation = "Added Senior Community Book Club meeting today at 4:00 PM at Maplewood Community Center.",
                    intentCategory = "Appointment",
                    executedAtText = "Today, 10:05 AM",
                    createdAt = baseTime + 8000L
                ),
                VoiceCommandLogEntity(
                    id = 9,
                    spokenCommand = "Turn on High Contrast Mode",
                    aiActionTaken = "🎨 Switched High Contrast Mode",
                    spokenConfirmation = "High Contrast Mode enabled: Black and Yellow Low-Vision Palette.",
                    intentCategory = "Accessibility",
                    executedAtText = "Today, 10:15 AM",
                    createdAt = baseTime + 9000L
                ),
                VoiceCommandLogEntity(
                    id = 10,
                    spokenCommand = "Open Daily Planner",
                    aiActionTaken = "🧭 Navigated to Daily Planner",
                    spokenConfirmation = "Opening your Daily Planner.",
                    intentCategory = "Navigation",
                    executedAtText = "Today, 10:30 AM",
                    createdAt = baseTime + 10000L
                )
            )
        }
    }
}
