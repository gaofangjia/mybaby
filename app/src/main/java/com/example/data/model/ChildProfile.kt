package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_profile")
data class ChildProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val goal: String = "掌握生活好习惯，健康快乐成长"
)
