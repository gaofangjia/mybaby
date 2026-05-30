package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ChildProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM child_profile ORDER BY id ASC")
    fun getAllProfilesFlow(): Flow<List<ChildProfile>>

    @Query("SELECT * FROM child_profile ORDER BY id ASC")
    suspend fun getAllProfilesDirect(): List<ChildProfile>

    @Query("SELECT * FROM child_profile WHERE id = :id LIMIT 1")
    fun getProfileFlow(id: Int): Flow<ChildProfile?>

    @Query("SELECT * FROM child_profile WHERE id = :id LIMIT 1")
    suspend fun getProfileDirect(id: Int): ChildProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ChildProfile): Long

    @Query("DELETE FROM child_profile WHERE id = :id")
    suspend fun deleteProfile(id: Int)
}
