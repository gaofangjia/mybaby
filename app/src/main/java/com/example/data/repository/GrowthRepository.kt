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

class GrowthRepository(
    private val profileDao: ProfileDao,
    private val habitItemDao: HabitItemDao,
    private val dailyRecordDao: DailyRecordDao,
    private val ruleDao: RuleDao
) {
    val allProfiles: Flow<List<ChildProfile>> = profileDao.getAllProfilesFlow()

    fun getProfileFlow(childId: Int): Flow<ChildProfile?> {
        return profileDao.getProfileFlow(childId)
    }

    fun getActiveHabitsFlow(childId: Int): Flow<List<HabitItem>> {
        return habitItemDao.getActiveHabitsFlow(childId)
    }

    fun getAllCompletedRecordsFlow(childId: Int): Flow<List<DailyRecord>> {
        return dailyRecordDao.getAllCompletedRecordsFlow(childId)
    }

    fun getRulesFlow(childId: Int): Flow<Rule?> {
        return ruleDao.getRuleFlow(childId)
    }

    fun getCompletedRecordsCountFlow(childId: Int): Flow<Int> {
        return dailyRecordDao.getCompletedRecordsCountFlow(childId)
    }

    fun getRecordsInDateRange(childId: Int, startDate: String, endDate: String): Flow<List<DailyRecord>> {
        return dailyRecordDao.getRecordsInDateRangeFlow(childId, startDate, endDate)
    }

    suspend fun getRecordsInDateRangeDirect(childId: Int, startDate: String, endDate: String): List<DailyRecord> {
        return dailyRecordDao.getRecordsInDateRangeDirect(childId, startDate, endDate)
    }

    // --- Profile Operations ---
    suspend fun createProfile(name: String, goal: String): Int {
        val newProfile = ChildProfile(name = name, goal = goal)
        val id = profileDao.insertOrUpdateProfile(newProfile).toInt()
        
        // Feed in default habits so the new child isn't blank
        val defaultHabits = listOf(
            "按时起床",
            "认真完成作业",
            "课外阅读 30分钟",
            "积极体育运动",
            "晚上九点早睡"
        )
        defaultHabits.forEachIndexed { idx, hName ->
            habitItemDao.insertHabit(HabitItem(childId = id, name = hName, orderIndex = idx))
        }

        // Insert rules
        ruleDao.insertOrUpdateRule(Rule(id = id))
        return id
    }

    suspend fun updateProfile(id: Int, name: String, goal: String) {
        val current = profileDao.getProfileDirect(id)
        if (current != null) {
            profileDao.insertOrUpdateProfile(current.copy(name = name, goal = goal))
        }
    }

    suspend fun deleteProfile(id: Int) {
        profileDao.deleteProfile(id)
    }

    // --- Habit Operations ---
    suspend fun addHabit(childId: Int, name: String) {
        val active = habitItemDao.getActiveHabitsDirect(childId)
        val nextOrder = if (active.isEmpty()) 0 else (active.maxOf { it.orderIndex } + 1)
        habitItemDao.insertHabit(HabitItem(childId = childId, name = name, orderIndex = nextOrder))
    }

    suspend fun updateHabit(habit: HabitItem) {
        habitItemDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habitId: Int) {
        habitItemDao.deleteHabitSoft(habitId)
    }

    // --- Record Operations ---
    suspend fun toggleRecord(habitId: Int, date: String, completed: Boolean) {
        if (completed) {
            dailyRecordDao.insertOrUpdateRecord(DailyRecord(habitId = habitId, date = date, isCompleted = true))
        } else {
            dailyRecordDao.deleteRecord(habitId = habitId, date = date)
        }
    }

    // --- Rules Operations ---
    suspend fun updateRules(childId: Int, rewardRule: String, punishRule: String) {
        val current = ruleDao.getRuleDirect(childId) ?: Rule(id = childId)
        ruleDao.insertOrUpdateRule(current.copy(rewardRule = rewardRule, punishRule = punishRule))
    }

    // --- Direct queries for WebDAV sync manager ---
    suspend fun getAllProfilesDirect(): List<ChildProfile> = profileDao.getAllProfilesDirect()
    suspend fun getAllHabitsGloballyDirect(): List<HabitItem> = habitItemDao.getAllHabitsGloballyDirect()
    suspend fun getAllRecordsGloballyDirect(): List<DailyRecord> = dailyRecordDao.getAllRecordsGloballyDirect()
    suspend fun getAllRulesDirect(): List<Rule> = ruleDao.getAllRulesDirect()

    // --- Reset and replace during import/sync ---
    suspend fun clearAllData() {
        val profiles = profileDao.getAllProfilesDirect()
        profiles.forEach { profileDao.deleteProfile(it.id) }

        val habits = habitItemDao.getAllHabitsGloballyDirect()
        habits.forEach { habitItemDao.deleteHabitHard(it.id) }

        // Rules and Records tables will clear natively due to deletions or we overwrite
    }

    suspend fun insertProfileDirect(profile: ChildProfile) = profileDao.insertOrUpdateProfile(profile)
    suspend fun insertHabitDirect(habit: HabitItem) = habitItemDao.insertHabit(habit)
    suspend fun insertRecordDirect(record: DailyRecord) = dailyRecordDao.insertOrUpdateRecord(record)
    suspend fun insertRuleDirect(rule: Rule) = ruleDao.insertOrUpdateRule(rule)

    suspend fun ensureDefaultsExist() {
        val profiles = profileDao.getAllProfilesDirect()
        if (profiles.isEmpty()) {
            val defaultId = profileDao.insertOrUpdateProfile(ChildProfile(id = 1, name = "小明", goal = "掌握生活好习惯，健康快乐成长")).toInt()
            
            val defaults = listOf(
                "按时起床",
                "认真完成作业",
                "课外阅读 30分钟",
                "积极体育运动",
                "晚上九点早睡"
            )
            defaults.forEachIndexed { idx, name ->
                habitItemDao.insertHabit(HabitItem(childId = defaultId, name = name, orderIndex = idx))
            }

            ruleDao.insertOrUpdateRule(Rule(id = defaultId))
        }
    }
}
