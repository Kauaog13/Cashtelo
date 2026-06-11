package com.cashtelo.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.cashtelo.data.database.CashteloDatabase
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionType
import com.cashtelo.data.repository.TransactionRepository
import kotlinx.coroutines.launch

enum class MoodState { FELIZ, NORMAL, TRISTE }

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val allTransactions: LiveData<List<Transaction>>
    val totalReceitas: LiveData<Double>
    val totalDespesas: LiveData<Double>
    val despesasByCategory = MediatorLiveData<List<com.cashtelo.data.dao.CategoryTotal>>()

    val saldo: MediatorLiveData<Double> = MediatorLiveData<Double>().apply {
        value = 0.0
    }

    val moodState: LiveData<MoodState> = saldo.map { balance ->
        when {
            balance >= 500.0 -> MoodState.FELIZ
            balance >= 0.0   -> MoodState.NORMAL
            else             -> MoodState.TRISTE
        }
    }

    init {
        val dao = CashteloDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(dao)
        allTransactions = repository.allTransactions
        totalReceitas   = repository.totalReceitas
        totalDespesas   = repository.totalDespesas

        fun updateSaldo() {
            val r = totalReceitas.value ?: 0.0
            val d = totalDespesas.value ?: 0.0
            saldo.value = r - d
        }

        saldo.addSource(totalReceitas) { updateSaldo() }
        saldo.addSource(totalDespesas) { updateSaldo() }

        despesasByCategory.addSource(repository.despesasByCategory) {
            despesasByCategory.value = it
        }
    }

    fun getSaldo(): LiveData<Double> = saldo

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun getByType(type: TransactionType) = repository.getByType(type)
}

class TransactionViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}