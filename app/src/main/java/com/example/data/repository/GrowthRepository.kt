package com.example.data.repository

import com.example.data.local.ProfileDao
import com.example.data.local.HabitItemDao
import com.example.data.local.DailyRecordDao
import com.example.data.local.RuleDao
import com.example.data.model.ChildProfile
import com.example.data.model.HabitItem
import com.example.data.model.DailyRecord
import com.example.data.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GrowthRepository(
    private val profileDao: ProfileDao,
    private val habitItemDao: HabitItemDao,
    private val dailyRecordDao: DailyRecordDao,
    private val ruleDao: RuleDao
) {
    val childProfile: Flow<ChildProfile?> = profileDao.getProfileFlow()
    val activeHabits: Flow<List<HabitItem>> = habitItemDao.getActiveHabitsFlow()
    val allCompletedRecords: Flow<List<DailyRecord>> = dailyRecordDao.getAllCompletedRecordsFlow()
    val rules: Flow<Rule?> = ruleDao.getRuleFlow()
    val totalFlowersCount: Flow<Int> = dailyRecordDao.getCompletedRecordsCountFlow()

    fun getRecordsInDateRange(startDate: String, endDate: String): Flow<List<DailyRecord>> {
        return dailyRecordDao.getRecordsInDateRangeFlow(startDate, endDate)
    }

    suspend fun getRecordsInDateRangeDirect(startDate: String, endDate: String): List<DailyRecord> {
        return dailyRecordDao.getRecordsInDateRangeDirect(startDate, endDate)
    }

    suspend fun updateProfile(name: String, goal: String) {
        val current = profileDao.getProfileDirect() ?: ChildProfile()
        profileDao.insertOrUpdateProfile(current.copy(name = name, goal = goal))
    }

    suspend fun addHabit(name: String) {
        val active = habitItemDao.getActiveHabitsDirect()
        val nextOrder = if (active.isEmpty()) 0 else (active.maxOf { it.orderIndex } + 1)
        habitItemDao.insertHabit(HabitItem(name = name, orderIndex = nextOrder))
    }

    suspend fun updateHabit(habit: HabitItem) {
        habitItemDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habitId: Int) {
        habitItemDao.deleteHabitSoft(habitId)
    }

    suspend fun toggleRecord(habitId: Int, date: String, completed: Boolean) {
        if (completed) {
            dailyRecordDao.insertOrUpdateRecord(DailyRecord(habitId = habitId, date = date, isCompleted = true))
        } else {
            dailyRecordDao.deleteRecord(habitId = habitId, date = date)
        }
    }

    suspend fun updateRules(rewardRule: String, punishRule: String) {
        val current = ruleDao.getRuleDirect() ?: Rule()
        ruleDao.insertOrUpdateRule(current.copy(rewardRule = rewardRule, punishRule = punishRule))
    }

    suspend fun ensureDefaultsExist() {
        val profile = profileDao.getProfileDirect()
        if (profile == null) {
            profileDao.insertOrUpdateProfile(ChildProfile())
        }

        val active = habitItemDao.getActiveHabitsDirect()
        if (active.isEmpty()) {
            val defaults = listOf(
                "按时起床",
                "认真完成作业",
                "课外阅读 30分钟",
                "积极体育运动",
                "晚上九点早睡"
            )
            defaults.forEachIndexed { idx, name ->
                habitItemDao.insertHabit(HabitItem(name = name, orderIndex = idx))
            }
        }

        val rule = ruleDao.getRuleDirect()
        if (rule == null) {
            ruleDao.insertOrUpdateRule(Rule())
        }
    }
}
