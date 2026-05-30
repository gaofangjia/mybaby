package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChildProfile
import com.example.data.model.DailyRecord
import com.example.data.model.HabitItem
import com.example.data.model.Rule
import com.example.data.repository.GrowthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class GrowthViewModel(private val repository: GrowthRepository) : ViewModel() {

    // Current selected week's Monday (default is today's week Monday)
    private val _selectedWeekMonday = MutableStateFlow<LocalDate>(getMondayOfWeek(LocalDate.now()))
    val selectedWeekMonday: StateFlow<LocalDate> = _selectedWeekMonday.asStateFlow()

    // Current selected month for statistics (default is current month)
    private val _selectedStatsMonth = MutableStateFlow<YearMonth>(YearMonth.now())
    val selectedStatsMonth: StateFlow<YearMonth> = _selectedStatsMonth.asStateFlow()

    // Child profile flow
    val childProfile: StateFlow<ChildProfile?> = repository.childProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active habit items
    val activeHabits: StateFlow<List<HabitItem>> = repository.activeHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All completed daily records flow
    val allCompletedRecords: StateFlow<List<DailyRecord>> = repository.allCompletedRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rules flow
    val rules: StateFlow<Rule?> = repository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Cumulative flowers (this can just be total completions)
    val totalFlowersCount: StateFlow<Int> = repository.totalFlowersCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Ensure that default values are inserted into the database upon startup
        viewModelScope.launch {
            repository.ensureDefaultsExist()
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
    fun updateProfile(name: String, goal: String) {
        viewModelScope.launch {
            repository.updateProfile(name, goal)
        }
    }

    fun addHabit(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addHabit(name.trim())
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

    fun updateRules(rewardRule: String, punishRule: String) {
        viewModelScope.launch {
            repository.updateRules(rewardRule, punishRule)
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

class GrowthViewModelFactory(private val repository: GrowthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GrowthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GrowthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
