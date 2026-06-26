package com.banktracker.ui.transactions

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.model.Transaction
import com.banktracker.data.repository.AppRepository
import com.banktracker.util.JalaliUtil
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository(AppDatabase.getInstance(application))

    private val _selectedDate = MutableStateFlow(JalaliUtil.toDateString(JalaliUtil.todayJalali()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val transactions: StateFlow<List<Transaction>> = _selectedDate.flatMapLatest {
        repo.getTransactionsByDate(it)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableDates: StateFlow<List<String>> = repo.getDistinctDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _scanResult = MutableStateFlow<Int?>(null)
    val scanResult: StateFlow<Int?> = _scanResult.asStateFlow()

    fun selectDate(date: String) { _selectedDate.value = date }

    fun scanInbox(contentResolver: ContentResolver) = viewModelScope.launch {
        _scanning.value = true
        val count = repo.scanInbox(contentResolver)
        _scanning.value = false
        _scanResult.value = count
    }

    fun clearScanResult() { _scanResult.value = null }
}
