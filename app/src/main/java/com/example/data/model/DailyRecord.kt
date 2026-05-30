package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "daily_records",
    primaryKeys = ["habitId", "date"]
)
data class DailyRecord(
    val habitId: Int,
    val date: String, // "yyyy-MM-dd"
    val isCompleted: Boolean = false
)
