package com.example.mgc_keyboard.statscore

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table: the most recently computed personal baseline (US3-1). */
@Entity(tableName = "behavioral_baseline")
data class BehavioralBaseline(
    @PrimaryKey val id: Int = 0,
    val computedAt: Long,
    val daysOfDataUsed: Int,
    val avgBackspaceRate: Float,
    val avgSentiment: Float,
    val avgAppSwitchesPerHour: Float,
    val avgDistinctAppsPerDay: Float,
    val avgScreenTimeMillisPerDay: Long,
    val avgLongestInactiveStretchHours: Float
)

/** US2-4: a detected drop in app-diversity or lengthened inactivity vs baseline. */
@Entity(tableName = "withdrawal_signal")
data class WithdrawalSignal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val reason: String,
    val magnitudePercent: Float
)

const val MIN_BASELINE_DAYS = 3
