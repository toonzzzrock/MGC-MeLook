package com.example.mgc_keyboard.statscore

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BaselineDao {
    @Query("SELECT * FROM behavioral_baseline WHERE id = 0")
    fun observeLatest(): Flow<BehavioralBaseline?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(baseline: BehavioralBaseline)

    @Insert
    suspend fun insertWithdrawalSignal(signal: WithdrawalSignal)

    @Query("SELECT * FROM withdrawal_signal ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentWithdrawalSignals(limit: Int): Flow<List<WithdrawalSignal>>
}

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(entry: AuditLogEntry)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AuditLogEntry>>
}
