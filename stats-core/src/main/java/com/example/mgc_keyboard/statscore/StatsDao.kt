package com.example.mgc_keyboard.statscore

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query("SELECT * FROM hourly_stats WHERE hourBucket BETWEEN :from AND :to ORDER BY hourBucket")
    fun observeRange(from: Long, to: Long): Flow<List<HourlyStat>>

    @Query("SELECT * FROM hourly_stats ORDER BY hourBucket DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HourlyStat>>

    @Query("SELECT * FROM hourly_stats ORDER BY hourBucket ASC")
    suspend fun getAll(): List<HourlyStat>

    @Query("SELECT * FROM hourly_stats WHERE hourBucket = :bucket")
    suspend fun getByBucket(bucket: Long): HourlyStat?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(stat: HourlyStat)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stats: List<HourlyStat>)

    @Query(
        """
        UPDATE hourly_stats SET
            screenTimeMillis = :screenTimeMillis,
            distinctAppCount = :distinctAppCount
        WHERE hourBucket = :bucket
        """
    )
    suspend fun setUsage(bucket: Long, screenTimeMillis: Long, distinctAppCount: Int)

    @Query(
        """
        UPDATE hourly_stats SET
            totalKeyPresses = totalKeyPresses + :dKeys,
            backspacePresses = backspacePresses + :dBackspace,
            sentimentSum = sentimentSum + :dSentimentSum,
            wordsScored = wordsScored + :dWords,
            appSwitchCount = appSwitchCount + :dSwitches
        WHERE hourBucket = :bucket
        """
    )
    suspend fun mergeDelta(
        bucket: Long,
        dKeys: Int,
        dBackspace: Int,
        dSentimentSum: Float,
        dWords: Int,
        dSwitches: Int
    )
}
