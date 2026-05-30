package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {
    @Query("SELECT d.* FROM daily_records d INNER JOIN habit_items h ON d.habitId = h.id WHERE d.isCompleted = 1 AND h.childId = :childId AND h.isDeleted = 0")
    fun getAllCompletedRecordsFlow(childId: Int): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records")
    suspend fun getAllRecordsGloballyDirect(): List<DailyRecord>

    @Query("SELECT d.* FROM daily_records d INNER JOIN habit_items h ON d.habitId = h.id WHERE h.childId = :childId AND h.isDeleted = 0 AND d.date = :date")
    fun getRecordsByDateFlow(childId: Int, date: String): Flow<List<DailyRecord>>

    @Query("SELECT d.* FROM daily_records d INNER JOIN habit_items h ON d.habitId = h.id WHERE h.childId = :childId AND h.isDeleted = 0 AND d.date BETWEEN :startDate AND :endDate")
    fun getRecordsInDateRangeFlow(childId: Int, startDate: String, endDate: String): Flow<List<DailyRecord>>

    @Query("SELECT d.* FROM daily_records d INNER JOIN habit_items h ON d.habitId = h.id WHERE h.childId = :childId AND h.isDeleted = 0 AND d.date BETWEEN :startDate AND :endDate")
    suspend fun getRecordsInDateRangeDirect(childId: Int, startDate: String, endDate: String): List<DailyRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE habitId = :habitId AND date = :date")
    suspend fun deleteRecord(habitId: Int, date: String)

    @Query("SELECT COUNT(d.habitId) FROM daily_records d INNER JOIN habit_items h ON d.habitId = h.id WHERE d.isCompleted = 1 AND h.childId = :childId AND h.isDeleted = 0")
    fun getCompletedRecordsCountFlow(childId: Int): Flow<Int>
}
