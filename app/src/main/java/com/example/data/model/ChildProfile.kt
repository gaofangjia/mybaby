package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "child_profile")
data class ChildProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "小明",
    val goal: String = "掌握生活好习惯，健康快乐成长"
)
