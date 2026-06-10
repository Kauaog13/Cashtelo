package com.cashtelo.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionType

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'RECEITA'")
    fun getTotalReceitas(): LiveData<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = 'DESPESA'")
    fun getTotalDespesas(): LiveData<Double>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: TransactionType): LiveData<List<Transaction>>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE type = 'DESPESA' 
        GROUP BY category
    """)
    fun getDespesasByCategory(): LiveData<List<CategoryTotal>>
}

data class CategoryTotal(
    val category: String,
    val total: Double
)
