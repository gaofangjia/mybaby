package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_items")
data class HabitItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val childId: Int = 1,
    val name: String,
    val orderIndex: Int = 0,
    val isDeleted: Boolean = false
)
