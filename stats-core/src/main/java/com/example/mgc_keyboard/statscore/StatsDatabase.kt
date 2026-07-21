package com.example.mgc_keyboard.statscore

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

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

        private fun build(context: Context): StatsDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = DbPassphrase.getOrCreate(context.applicationContext)
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(
                context.applicationContext,
                StatsDatabase::class.java,
                "mental_melook_stats.db"
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration(true)
                .build()
        }
    }
}
