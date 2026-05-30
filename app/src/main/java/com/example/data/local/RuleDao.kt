package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM growth_rules WHERE id = :childId LIMIT 1")
    fun getRuleFlow(childId: Int): Flow<Rule?>

    @Query("SELECT * FROM growth_rules WHERE id = :childId LIMIT 1")
    suspend fun getRuleDirect(childId: Int): Rule?

    @Query("SELECT * FROM growth_rules")
    suspend fun getAllRulesDirect(): List<Rule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: Rule)
}
