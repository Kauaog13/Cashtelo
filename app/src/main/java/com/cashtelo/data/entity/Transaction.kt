package com.cashtelo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { RECEITA, DESPESA }

enum class TransactionCategory(val label: String, val emoji: String) {
    SALARIO("Salário", "💼"),
    FREELANCE("Freelance", "💻"),
    INVESTIMENTO("Investimento", "📈"),
    PRESENTE("Presente", "🎁"),
    ALIMENTACAO("Alimentação", "🍔"),
    TRANSPORTE("Transporte", "🚗"),
    MORADIA("Moradia", "🏠"),
    SAUDE("Saúde", "💊"),
    EDUCACAO("Educação", "📚"),
    LAZER("Lazer", "🎮"),
    ROUPAS("Roupas", "👕"),
    OUTROS("Outros", "📦")
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val description: String = "",
    val date: Long = System.currentTimeMillis()
)
