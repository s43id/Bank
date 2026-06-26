package com.banktracker.ui.smspicker

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.repository.AppRepository
import com.banktracker.data.repository.SmsThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SmsPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository(AppDatabase.getInstance(application))

    private val _threads = MutableStateFlow<List<SmsThread>>(emptyList())
    val threads: StateFlow<List<SmsThread>> = _threads.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    private val _importedCount = MutableStateFlow<Int?>(null)
    val importedCount: StateFlow<Int?> = _importedCount.asStateFlow()

    fun load(contentResolver: ContentResolver) {
        viewModelScope.launch {
            _loading.value = true
            _threads.value = repo.listAllThreads(contentResolver)
            _loading.value = false
        }
    }

    fun toggle(address: String) {
        val cur = _selected.value.toMutableSet()
        if (address in cur) cur.remove(address) else cur.add(address)
        _selected.value = cur
    }

    fun import(contentResolver: ContentResolver, sinceMs: Long) {
        val addresses = _selected.value
        if (addresses.isEmpty()) return
        viewModelScope.launch {
            val count = repo.importThreads(contentResolver, addresses, sinceMs)
            _importedCount.value = count
        }
    }

    fun clearResult() { _importedCount.value = null }
}
