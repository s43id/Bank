package com.banktracker.ui.banks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.banktracker.data.db.AppDatabase
import com.banktracker.data.model.Bank
import com.banktracker.data.repository.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BanksViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository(AppDatabase.getInstance(application))

    val banks: StateFlow<List<Bank>> = repo.allBanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBank(name: String, senderNumber: String) = viewModelScope.launch {
        repo.insertBank(Bank(name = name.trim(), senderNumber = senderNumber.trim()))
    }

    fun deleteBank(bank: Bank) = viewModelScope.launch {
        repo.deleteBank(bank)
    }

    fun updateBank(bank: Bank) = viewModelScope.launch {
        repo.updateBank(bank)
    }
}
