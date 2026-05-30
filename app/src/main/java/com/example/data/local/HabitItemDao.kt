package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HabitItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitItemDao {
    @Query("SELECT * FROM habit_items WHERE isDeleted = 0 AND childId = :childId ORDER BY orderIndex ASC, id ASC")
    fun getActiveHabitsFlow(childId: Int): Flow<List<HabitItem>>

    @Query("SELECT * FROM habit_items WHERE childId = :childId ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllHabitsDirect(childId: Int): List<HabitItem>

    @Query("SELECT * FROM habit_items ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllHabitsGloballyDirect(): List<HabitItem>

    @Query("SELECT * FROM habit_items WHERE isDeleted = 0 AND childId = :childId ORDER BY orderIndex ASC, id ASC")
    suspend fun getActiveHabitsDirect(childId: Int): List<HabitItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitItem): Long

    @Update
    suspend fun updateHabit(habit: HabitItem)

    @Query("UPDATE habit_items SET isDeleted = 1 WHERE id = :id")
    suspend fun deleteHabitSoft(id: Int)

    @Query("DELETE FROM habit_items WHERE id = :id")
    suspend fun deleteHabitHard(id: Int)
}
