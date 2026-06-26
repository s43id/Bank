package com.banktracker.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.repository.AppRepository
import com.banktracker.util.JalaliUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DayStats(
    val date: String,
    val label: String,
    val totalDeposit: Long = 0L,
    val totalWithdrawal: Long = 0L
) {
    val net: Long get() = totalDeposit - totalWithdrawal
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository(AppDatabase.getInstance(application))

    private val todayStr = JalaliUtil.toDateString(JalaliUtil.todayJalali())
    private val yesterdayStr = JalaliUtil.toDateString(JalaliUtil.yesterdayJalali())

    val todayStats: StateFlow<DayStats> = combine(
        repo.sumDepositByDate(todayStr),
        repo.sumWithdrawalByDate(todayStr)
    ) { dep, with -> DayStats(todayStr, "امروز", dep, with) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayStats(todayStr, "امروز"))

    val yesterdayStats: StateFlow<DayStats> = combine(
        repo.sumDepositByDate(yesterdayStr),
        repo.sumWithdrawalByDate(yesterdayStr)
    ) { dep, with -> DayStats(yesterdayStr, "دیروز", dep, with) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayStats(yesterdayStr, "دیروز"))

    private val _selectedDate = MutableStateFlow(todayStr)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val selectedStats: StateFlow<DayStats> = _selectedDate.flatMapLatest { date ->
        combine(
            repo.sumDepositByDate(date),
            repo.sumWithdrawalByDate(date)
        ) { dep, with -> DayStats(date, JalaliUtil.toPrettyDate(date), dep, with) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayStats(todayStr, ""))

    val availableDates: StateFlow<List<String>> = repo.getDistinctDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectDate(date: String) { _selectedDate.value = date }
}
