package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE isCompleted = 1")
    fun getAllCompletedRecordsFlow(): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records WHERE date = :date")
    fun getRecordsByDateFlow(date: String): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records WHERE date BETWEEN :startDate AND :endDate")
    fun getRecordsInDateRangeFlow(startDate: String, endDate: String): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getRecordsInDateRangeDirect(startDate: String, endDate: String): List<DailyRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE habitId = :habitId AND date = :date")
    suspend fun deleteRecord(habitId: Int, date: String)

    @Query("SELECT COUNT(*) FROM daily_records WHERE isCompleted = 1")
    fun getCompletedRecordsCountFlow(): Flow<Int>
}
