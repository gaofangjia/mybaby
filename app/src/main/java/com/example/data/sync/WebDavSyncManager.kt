package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.model.ChildProfile
import com.example.data.model.DailyRecord
import com.example.data.model.HabitItem
import com.example.data.model.Rule
import com.example.data.repository.GrowthRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChildProfileBackup(val id: Int, val name: String, val goal: String)

@JsonClass(generateAdapter = true)
data class HabitItemBackup(val id: Int, val childId: Int, val name: String, val orderIndex: Int, val isDeleted: Boolean)

@JsonClass(generateAdapter = true)
data class DailyRecordBackup(val habitId: Int, val date: String, val isCompleted: Boolean)

@JsonClass(generateAdapter = true)
data class RuleBackup(val id: Int, val rewardRule: String, val punishRule: String)

@JsonClass(generateAdapter = true)
data class GrowthBackupData(
    val profiles: List<ChildProfileBackup>,
    val habits: List<HabitItemBackup>,
    val records: List<DailyRecordBackup>,
    val rules: List<RuleBackup>
)

class WebDavSyncManager(
    private val context: Context,
    private val repository: GrowthRepository
) {
    private val sharedPrefs = context.getSharedPreferences("webdav_sync_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(GrowthBackupData::class.java)

    fun getSyncSettings(): SyncSettings {
        return SyncSettings(
            url = sharedPrefs.getString("url", "") ?: "",
            username = sharedPrefs.getString("username", "") ?: "",
            password = sharedPrefs.getString("password", "") ?: "",
            fileName = sharedPrefs.getString("filename", "child_growth_backup.json") ?: "child_growth_backup.json",
            lastSyncTime = sharedPrefs.getString("last_sync_time", "从未同步") ?: "从未同步"
        )
    }

    fun saveSyncSettings(settings: SyncSettings) {
        sharedPrefs.edit()
            .putString("url", settings.url.trim())
            .putString("username", settings.username.trim())
            .putString("password", settings.password.trim())
            .putString("filename", settings.fileName.trim())
            .putString("last_sync_time", settings.lastSyncTime)
            .apply()
    }

    private fun buildRequestUrl(settings: SyncSettings): String {
        var base = settings.url.trim()
        if (!base.endsWith("/")) {
            base += "/"
        }
        return base + settings.fileName.trim()
    }

    // Backup current Room contents to WebDAV
    suspend fun backupToCloud(): SyncResult = withContext(Dispatchers.IO) {
        val settings = getSyncSettings()
        if (settings.url.isBlank() || settings.username.isBlank() || settings.password.isBlank()) {
            return@withContext SyncResult.Error("请先配置完整的 WebDAV 服务器信息")
        }

        try {
            val profiles = repository.getAllProfilesDirect().map { ChildProfileBackup(it.id, it.name, it.goal) }
            val habits = repository.getAllHabitsGloballyDirect().map { HabitItemBackup(it.id, it.childId, it.name, it.orderIndex, it.isDeleted) }
            val records = repository.getAllRecordsGloballyDirect().map { DailyRecordBackup(it.habitId, it.date, it.isCompleted) }
            val rules = repository.getAllRulesDirect().map { RuleBackup(it.id, it.rewardRule, it.punishRule) }

            val backup = GrowthBackupData(profiles, habits, records, rules)
            val jsonString = backupAdapter.toJson(backup)

            val requestUrl = buildRequestUrl(settings)
            val authHeader = Credentials.basic(settings.username, settings.password)

            val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(requestUrl)
                .header("Authorization", authHeader)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    val updatedSettings = settings.copy(lastSyncTime = "$timeStr 备份成功")
                    saveSyncSettings(updatedSettings)
                    SyncResult.Success("数据已成功备份到云端")
                } else {
                    SyncResult.Error("备份失败，服务器响应: ${response.code} (${response.message})")
                }
            }
        } catch (e: Exception) {
            Log.e("WebDavSyncManager", "Backup failed", e)
            SyncResult.Error("备份请求异常: ${e.localizedMessage}")
        }
    }

    // Destructive Restore from Cloud
    suspend fun restoreFromCloud(): SyncResult = withContext(Dispatchers.IO) {
        val settings = getSyncSettings()
        if (settings.url.isBlank() || settings.username.isBlank() || settings.password.isBlank()) {
            return@withContext SyncResult.Error("请先配置完整的 WebDAV 服务器信息")
        }

        try {
            val requestUrl = buildRequestUrl(settings)
            val authHeader = Credentials.basic(settings.username, settings.password)

            val request = Request.Builder()
                .url(requestUrl)
                .header("Authorization", authHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext SyncResult.Error("云端备份不存在，请先执行「备份到云端」")
                }
                if (!response.isSuccessful) {
                    return@withContext SyncResult.Error("获取备份失败，服务器响应: ${response.code}")
                }

                val bodyStr = response.body?.string() ?: return@withContext SyncResult.Error("云端返回数据为空")
                val backup = backupAdapter.fromJson(bodyStr) ?: return@withContext SyncResult.Error("文件解析失败")

                // Override database safely
                repository.clearAllData()

                backup.profiles.forEach {
                    repository.insertProfileDirect(ChildProfile(id = it.id, name = it.name, goal = it.goal))
                }
                backup.habits.forEach {
                    repository.insertHabitDirect(HabitItem(id = it.id, childId = it.childId, name = it.name, orderIndex = it.orderIndex, isDeleted = it.isDeleted))
                }
                backup.records.forEach {
                    repository.insertRecordDirect(DailyRecord(habitId = it.habitId, date = it.date, isCompleted = it.isCompleted))
                }
                backup.rules.forEach {
                    repository.insertRuleDirect(Rule(id = it.id, rewardRule = it.rewardRule, punishRule = it.punishRule))
                }

                val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val updatedSettings = settings.copy(lastSyncTime = "$timeStr 恢复成功")
                saveSyncSettings(updatedSettings)
                SyncResult.Success("已成功从云端恢复数据")
            }
        } catch (e: Exception) {
            Log.e("WebDavSyncManager", "Restore failed", e)
            SyncResult.Error("恢复请求异常: ${e.localizedMessage}")
        }
    }

    // Intelligent Bi-directional Merge Sync
    suspend fun smartSyncMerge(): SyncResult = withContext(Dispatchers.IO) {
        val settings = getSyncSettings()
        if (settings.url.isBlank() || settings.username.isBlank() || settings.password.isBlank()) {
            return@withContext SyncResult.Error("请先配置完整的 WebDAV 服务器信息")
        }

        try {
            val requestUrl = buildRequestUrl(settings)
            val authHeader = Credentials.basic(settings.username, settings.password)

            val request = Request.Builder()
                .url(requestUrl)
                .header("Authorization", authHeader)
                .get()
                .build()

            var cloudBackup: GrowthBackupData? = null

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        cloudBackup = backupAdapter.fromJson(bodyStr)
                    }
                } else if (response.code != 404) {
                    return@withContext SyncResult.Error("检查云端备份失败: ${response.code}")
                }
            }

            val localProfiles = repository.getAllProfilesDirect()
            val localHabits = repository.getAllHabitsGloballyDirect()
            val localRecords = repository.getAllRecordsGloballyDirect()
            val localRules = repository.getAllRulesDirect()

            if (cloudBackup == null) {
                // Cloud backup doesn't exist yet, simply upload current local state
                val profilesBackup = localProfiles.map { ChildProfileBackup(it.id, it.name, it.goal) }
                val habitsBackup = localHabits.map { HabitItemBackup(it.id, it.childId, it.name, it.orderIndex, it.isDeleted) }
                val recordsBackup = localRecords.map { DailyRecordBackup(it.habitId, it.date, it.isCompleted) }
                val rulesBackup = localRules.map { RuleBackup(it.id, it.rewardRule, it.punishRule) }

                val backup = GrowthBackupData(profilesBackup, habitsBackup, recordsBackup, rulesBackup)
                val jsonString = backupAdapter.toJson(backup)

                val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val uploadReq = Request.Builder()
                    .url(requestUrl)
                    .header("Authorization", authHeader)
                    .put(requestBody)
                    .build()

                client.newCall(uploadReq).execute().use { response ->
                    if (response.isSuccessful) {
                        val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        val updatedSettings = settings.copy(lastSyncTime = "$timeStr 智能同步成功(已上传本地)")
                        saveSyncSettings(updatedSettings)
                        return@withContext SyncResult.Success("智能同步已完成(云端为空，已备份本地)")
                    } else {
                        return@withContext SyncResult.Error("上传本地数据失败 code: ${response.code}")
                    }
                }
            }

            val cloud = cloudBackup!!

            // Merge Profiles
            val mergedProfiles = mutableMapOf<Int, ChildProfile>()
            localProfiles.forEach { mergedProfiles[it.id] = it }
            cloud.profiles.forEach { cProd ->
                val local = mergedProfiles[cProd.id]
                if (local == null) {
                    mergedProfiles[cProd.id] = ChildProfile(id = cProd.id, name = cProd.name, goal = cProd.goal)
                }
            }

            // Merge Habits
            val mergedHabits = mutableMapOf<Int, HabitItem>()
            localHabits.forEach { mergedHabits[it.id] = it }
            cloud.habits.forEach { cHabit ->
                val local = mergedHabits[cHabit.id]
                if (local == null) {
                    mergedHabits[cHabit.id] = HabitItem(
                        id = cHabit.id,
                        childId = cHabit.childId,
                        name = cHabit.name,
                        orderIndex = cHabit.orderIndex,
                        isDeleted = cHabit.isDeleted
                    )
                } else {
                    mergedHabits[cHabit.id] = local.copy(isDeleted = local.isDeleted || cHabit.isDeleted)
                }
            }

            // Merge Daily Records
            val mergedRecords = mutableMapOf<String, DailyRecord>()
            localRecords.forEach { mergedRecords["${it.habitId}_${it.date}"] = it }
            cloud.records.forEach { cRec ->
                val key = "${cRec.habitId}_${cRec.date}"
                val local = mergedRecords[key]
                if (local == null) {
                    mergedRecords[key] = DailyRecord(habitId = cRec.habitId, date = cRec.date, isCompleted = cRec.isCompleted)
                } else {
                    mergedRecords[key] = local.copy(isCompleted = local.isCompleted || cRec.isCompleted)
                }
            }

            // Merge Rules
            val mergedRules = mutableMapOf<Int, Rule>()
            localRules.forEach { mergedRules[it.id] = it }
            cloud.rules.forEach { cRule ->
                val local = mergedRules[cRule.id]
                if (local == null) {
                    mergedRules[cRule.id] = Rule(id = cRule.id, rewardRule = cRule.rewardRule, punishRule = cRule.punishRule)
                }
            }

            // Repopulate locally
            repository.clearAllData()
            mergedProfiles.values.forEach { repository.insertProfileDirect(it) }
            mergedHabits.values.forEach { repository.insertHabitDirect(it) }
            mergedRecords.values.forEach { repository.insertRecordDirect(it) }
            mergedRules.values.forEach { repository.insertRuleDirect(it) }

            // Write consolidated backup to cloud
            val uploadProfiles = mergedProfiles.values.map { ChildProfileBackup(it.id, it.name, it.goal) }
            val uploadHabits = mergedHabits.values.map { HabitItemBackup(it.id, it.childId, it.name, it.orderIndex, it.isDeleted) }
            val uploadRecords = mergedRecords.values.map { DailyRecordBackup(it.habitId, it.date, it.isCompleted) }
            val uploadRules = mergedRules.values.map { RuleBackup(it.id, it.rewardRule, it.punishRule) }

            val uploadBackup = GrowthBackupData(uploadProfiles, uploadHabits, uploadRecords, uploadRules)
            val uploadJson = backupAdapter.toJson(uploadBackup)

            val requestBody = uploadJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val putReq = Request.Builder()
                .url(requestUrl)
                .header("Authorization", authHeader)
                .put(requestBody)
                .build()

            client.newCall(putReq).execute().use { res ->
                if (res.isSuccessful) {
                    val timeStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    val updatedSettings = settings.copy(lastSyncTime = "$timeStr 智能合并成功")
                    saveSyncSettings(updatedSettings)
                    SyncResult.Success("智能双向合并同步成功")
                } else {
                    SyncResult.Error("智能同步上传失败: ${res.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("WebDavSyncManager", "Smart Sync failed", e)
            SyncResult.Error("智能同步异常: ${e.localizedMessage}")
        }
    }
}

data class SyncSettings(
    val url: String,
    val username: String,
    val password: String,
    val fileName: String,
    val lastSyncTime: String
)

sealed interface SyncResult {
    data class Success(val message: String) : SyncResult
    data class Error(val message: String) : SyncResult
}
