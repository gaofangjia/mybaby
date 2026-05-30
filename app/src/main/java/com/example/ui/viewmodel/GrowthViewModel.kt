package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChildProfile
import com.example.data.model.DailyRecord
import com.example.data.model.HabitItem
import com.example.data.model.Rule
import com.example.data.repository.GrowthRepository
import com.example.data.sync.SyncResult
import com.example.data.sync.SyncSettings
import com.example.data.sync.WebDavSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class GrowthViewModel(
    private val repository: GrowthRepository,
    val syncManager: WebDavSyncManager
) : ViewModel() {

    // Current selected week's Monday (default is today's week Monday)
    private val _selectedWeekMonday = MutableStateFlow<LocalDate>(getMondayOfWeek(LocalDate.now()))
    val selectedWeekMonday: StateFlow<LocalDate> = _selectedWeekMonday.asStateFlow()

    // Current selected month for statistics (default is current month)
    private val _selectedStatsMonth = MutableStateFlow<YearMonth>(YearMonth.now())
    val selectedStatsMonth: StateFlow<YearMonth> = _selectedStatsMonth.asStateFlow()

    // Current selected child ID
    private val _selectedChildId = MutableStateFlow<Int>(1)
    val selectedChildId: StateFlow<Int> = _selectedChildId.asStateFlow()

    // All profiles
    val allProfiles: StateFlow<List<ChildProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Child profile flow - dynamic
    val childProfile: StateFlow<ChildProfile?> = _selectedChildId
        .flatMapLatest { id -> repository.getProfileFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active habit items - dynamic
    val activeHabits: StateFlow<List<HabitItem>> = _selectedChildId
        .flatMapLatest { id -> repository.getActiveHabitsFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All completed daily records flow - dynamic
    val allCompletedRecords: StateFlow<List<DailyRecord>> = _selectedChildId
        .flatMapLatest { id -> repository.getAllCompletedRecordsFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rules flow - dynamic
    val rules: StateFlow<Rule?> = _selectedChildId
        .flatMapLatest { id -> repository.getRulesFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Cumulative flowers - dynamic
    val totalFlowersCount: StateFlow<Int> = _selectedChildId
        .flatMapLatest { id -> repository.getCompletedRecordsCountFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // WebDAV Configurations
    private val _webDavSettings = MutableStateFlow<SyncSettings>(syncManager.getSyncSettings())
    val webDavSettings: StateFlow<SyncSettings> = _webDavSettings.asStateFlow()

    private val _syncStatusMsg = MutableStateFlow<String>("")
    val syncStatusMsg: StateFlow<String> = _syncStatusMsg.asStateFlow()

    private val _isSyncing = MutableStateFlow<Boolean>(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Ensure defaults exist upon database creation
        viewModelScope.launch {
            repository.ensureDefaultsExist()
            // Pull first available child profile ID to initialize
            val profiles = repository.getAllProfilesDirect()
            if (profiles.isNotEmpty()) {
                _selectedChildId.value = profiles.first().id
            }
        }
    }

    // --- Helper date calculations ---
    companion object {
        fun getMondayOfWeek(date: LocalDate): LocalDate {
            var temp = date
            while (temp.dayOfWeek != DayOfWeek.MONDAY) {
                temp = temp.minusDays(1)
            }
            return temp
        }
    }

    // Monday to Sunday dates of the active week
    val activeWeekDates: StateFlow<List<LocalDate>> = _selectedWeekMonday
        .mapStateFlow { monday ->
            (0..6).map { monday.plusDays(it.toLong()) }
        }

    // Toggle previous/next week
    fun selectPreviousWeek() {
        _selectedWeekMonday.value = _selectedWeekMonday.value.minusWeeks(1)
    }

    fun selectNextWeek() {
        _selectedWeekMonday.value = _selectedWeekMonday.value.plusWeeks(1)
    }

    fun selectCurrentWeek() {
        _selectedWeekMonday.value = getMondayOfWeek(LocalDate.now())
    }

    // Toggle previous/next stats month
    fun selectPreviousStatsMonth() {
        _selectedStatsMonth.value = _selectedStatsMonth.value.minusMonths(1)
    }

    fun selectNextStatsMonth() {
        _selectedStatsMonth.value = _selectedStatsMonth.value.plusMonths(1)
    }

    fun selectCurrentStatsMonth() {
        _selectedStatsMonth.value = YearMonth.now()
    }

    // --- Database Operations ---
    fun selectChild(id: Int) {
        _selectedChildId.value = id
    }

    fun addChild(name: String, goal: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val newId = repository.createProfile(name.trim(), goal.trim())
                _selectedChildId.value = newId
            }
        }
    }

    fun deleteChild(id: Int) {
        viewModelScope.launch {
            repository.deleteProfile(id)
            val profiles = repository.getAllProfilesDirect()
            if (profiles.isNotEmpty()) {
                _selectedChildId.value = profiles.first().id
            } else {
                repository.ensureDefaultsExist()
                val reloaded = repository.getAllProfilesDirect()
                _selectedChildId.value = reloaded.firstOrNull()?.id ?: 1
            }
        }
    }

    fun updateProfile(name: String, goal: String) {
        viewModelScope.launch {
            repository.updateProfile(_selectedChildId.value, name, goal)
        }
    }

    fun addHabit(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addHabit(_selectedChildId.value, name.trim())
            }
        }
    }

    fun updateHabit(habit: HabitItem) {
        viewModelScope.launch {
            repository.updateHabit(habit)
        }
    }

    fun deleteHabit(habitId: Int) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun toggleDailyRecord(habitId: Int, date: LocalDate, completed: Boolean) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.toggleRecord(habitId, dateStr, completed)
        }
    }

    fun completeAllToday() {
        viewModelScope.launch {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            activeHabits.value.forEach { habit ->
                repository.toggleRecord(habit.id, dateStr, true)
            }
        }
    }

    fun updateRules(rewardRule: String, punishRule: String) {
        viewModelScope.launch {
            repository.updateRules(_selectedChildId.value, rewardRule, punishRule)
        }
    }

    // --- WebDAV Sync Triggers ---
    fun saveWebDavSettings(settings: SyncSettings) {
        syncManager.saveSyncSettings(settings)
        _webDavSettings.value = settings
    }

    fun performWebDavBackup() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMsg.value = "正在备份数据至云端 WebDAV..."
            when (val res = syncManager.backupToCloud()) {
                is SyncResult.Success -> {
                    _syncStatusMsg.value = "✅ " + res.message
                    _webDavSettings.value = syncManager.getSyncSettings()
                }
                is SyncResult.Error -> {
                    _syncStatusMsg.value = "❌ " + res.message
                }
            }
            _isSyncing.value = false
        }
    }

    fun performWebDavRestore() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMsg.value = "正在从云端 WebDAV 恢复数据..."
            when (val res = syncManager.restoreFromCloud()) {
                is SyncResult.Success -> {
                    _syncStatusMsg.value = "✅ " + res.message
                    _webDavSettings.value = syncManager.getSyncSettings()
                    val profiles = repository.getAllProfilesDirect()
                    if (profiles.isNotEmpty()) {
                        _selectedChildId.value = profiles.first().id
                    }
                }
                is SyncResult.Error -> {
                    _syncStatusMsg.value = "❌ " + res.message
                }
            }
            _isSyncing.value = false
        }
    }

    fun performWebDavSmartSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatusMsg.value = "正在进行智能双向合并同步..."
            when (val res = syncManager.smartSyncMerge()) {
                is SyncResult.Success -> {
                    _syncStatusMsg.value = "✅ " + res.message
                    _webDavSettings.value = syncManager.getSyncSettings()
                    val profiles = repository.getAllProfilesDirect()
                    if (profiles.isNotEmpty()) {
                        _selectedChildId.value = profiles.first().id
                    }
                }
                is SyncResult.Error -> {
                    _syncStatusMsg.value = "❌ " + res.message
                }
            }
            _isSyncing.value = false
        }
    }

    // Map helper for StateFlow to avoid recomposition triggers
    private fun <T, R> StateFlow<T>.mapStateFlow(transform: (T) -> R): StateFlow<R> {
        val initial = transform(this.value)
        val mutable = MutableStateFlow(initial)
        viewModelScope.launch {
            this@mapStateFlow.collect { value ->
                mutable.value = transform(value)
            }
        }
        return mutable.asStateFlow()
    }
}

class GrowthViewModelFactory(
    private val repository: GrowthRepository,
    private val syncManager: WebDavSyncManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GrowthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GrowthViewModel(repository, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
