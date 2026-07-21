package com.example.mgc_keyboard.statscore

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [HourlyStat::class, BehavioralBaseline::class, WithdrawalSignal::class, AuditLogEntry::class],
    version = 2,
    exportSchema = false
)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsDao
    abstract fun baselineDao(): BaselineDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile private var instance: StatsDatabase? = null

        fun getInstance(context: Context): StatsDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): StatsDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                StatsDatabase::class.java,
                "mental_melook_stats.db"
            )
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
