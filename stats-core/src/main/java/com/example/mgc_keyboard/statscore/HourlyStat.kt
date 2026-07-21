package com.example.mgc_keyboard.statscore

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hourly_stats")
data class HourlyStat(
    @PrimaryKey val hourBucket: Long,
    val totalKeyPresses: Int = 0,
    val backspacePresses: Int = 0,
    val sentimentSum: Float = 0f,
    val wordsScored: Int = 0,
    val appSwitchCount: Int = 0,
    val screenTimeMillis: Long = 0,
    val distinctAppCount: Int = 0
)

fun HourlyStat.averageSentiment(): Float? =
    if (wordsScored > 0) sentimentSum / wordsScored else null

fun HourlyStat.backspaceRate(): Float =
    if (totalKeyPresses > 0) backspacePresses.toFloat() / totalKeyPresses else 0f

fun HourlyStat.isInactive(): Boolean =
    totalKeyPresses == 0 && screenTimeMillis == 0L

fun currentHourBucket(nowMillis: Long = System.currentTimeMillis()): Long =
    nowMillis / 3_600_000L

fun HourlyStat.dayBucket(): Long = hourBucket / 24L
