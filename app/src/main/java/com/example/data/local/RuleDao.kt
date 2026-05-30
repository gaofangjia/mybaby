package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM growth_rules WHERE id = 1 LIMIT 1")
    fun getRuleFlow(): Flow<Rule?>

    @Query("SELECT * FROM growth_rules WHERE id = 1 LIMIT 1")
    suspend fun getRuleDirect(): Rule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: Rule)
}
