package com.cashtelo.data.repository

import androidx.lifecycle.LiveData
import com.cashtelo.data.dao.CategoryTotal
import com.cashtelo.data.dao.TransactionDao
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionType

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: LiveData<List<Transaction>> = dao.getAllTransactions()
    val totalReceitas: LiveData<Double> = dao.getTotalReceitas()
    val totalDespesas: LiveData<Double> = dao.getTotalDespesas()
    val despesasByCategory: LiveData<List<CategoryTotal>> = dao.getDespesasByCategory()

    suspend fun insert(transaction: Transaction) = dao.insert(transaction)
    suspend fun update(transaction: Transaction) = dao.update(transaction)
    suspend fun delete(transaction: Transaction) = dao.delete(transaction)
    suspend fun getById(id: Long) = dao.getById(id)

    fun getByType(type: TransactionType): LiveData<List<Transaction>> = dao.getByType(type)
}
