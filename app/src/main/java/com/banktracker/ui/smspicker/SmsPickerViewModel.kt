package com.banktracker.ui.smspicker

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.repository.AppRepository
import com.banktracker.data.repository.SmsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SmsPickerState {
    object Idle : SmsPickerState()
    object Loading : SmsPickerState()
    data class Loaded(val items: List<SmsItem>) : SmsPickerState()
    data class Imported(val count: Int) : SmsPickerState()
}

class SmsPickerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository(AppDatabase.getInstance(application))

    private val _state = MutableStateFlow<SmsPickerState>(SmsPickerState.Idle)
    val state: StateFlow<SmsPickerState> = _state.asStateFlow()

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    fun loadSms(contentResolver: ContentResolver) {
        _state.value = SmsPickerState.Loading
        viewModelScope.launch {
            val items = repo.listBankSms(contentResolver)
            _state.value = SmsPickerState.Loaded(items)
            _selected.value = emptySet()
        }
    }

    fun toggleSelection(index: Long) {
        val current = _selected.value.toMutableSet()
        if (index in current) current.remove(index) else current.add(index)
        _selected.value = current
    }

    fun selectAll(items: List<SmsItem>) {
        _selected.value = items.indices.map { it.toLong() }.toSet()
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun importSelected(contentResolver: ContentResolver, items: List<SmsItem>) {
        val toImport = _selected.value.map { items[it.toInt()] }
        if (toImport.isEmpty()) return
        viewModelScope.launch {
            val count = repo.importSmsItems(contentResolver, toImport)
            _state.value = SmsPickerState.Imported(count)
        }
    }
}
