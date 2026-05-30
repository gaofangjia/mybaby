package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "growth_rules")
data class Rule(
    @PrimaryKey val id: Int = 1,
    val rewardRule: String = "1. 累计获得 10 朵小红花，可以吃一次美味冰淇淋。\n2. 累计获得 30 朵小红花，可以获得一件漂亮的新衣服/新文具。\n3. 累计获得 50 朵小红花，可以兑换一次周末去游乐园的机会！",
    val punishRule: String = "1. 连续 3 天有未完成的早起/早睡习惯，扣除 2 朵小红花。\n2. 玩手机或看电视等娱乐活动超时，扣除 1 朵小红花。"
)
