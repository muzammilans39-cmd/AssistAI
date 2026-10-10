package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReminderEntity::class,
        AlarmEntity::class,
        AppointmentEntity::class,
        CallRecordEntity::class,
        MedicationEntity::class,
        EmergencyContactEntity::class,
        VisualScanEntity::class,
        VoiceCommandLogEntity::class,
        MedicationHistoryLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AssistDatabase : RoomDatabase() {
    abstract fun assistDao(): AssistDao

    companion object {
        @Volatile
        private var INSTANCE: AssistDatabase? = null

        fun getInstance(context: Context): AssistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AssistDatabase::class.java,
                    "assist_ai_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
