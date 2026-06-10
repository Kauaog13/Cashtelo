package com.cashtelo.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionType
import com.cashtelo.databinding.ItemTransactionBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onEdit: (Transaction) -> Unit,
    private val onDelete: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    private val currencyFmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

    inner class ViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.tvTitle.text = transaction.title
            binding.tvCategory.text = "${transaction.category.emoji} ${transaction.category.label}"
            binding.tvDate.text = dateFmt.format(Date(transaction.date))
            binding.tvDescription.text = transaction.description.ifEmpty { "—" }

            val amountStr = currencyFmt.format(transaction.amount)
            if (transaction.type == TransactionType.RECEITA) {
                binding.tvAmount.text = "+ $amountStr"
                binding.tvAmount.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
            } else {
                binding.tvAmount.text = "- $amountStr"
                binding.tvAmount.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
            }

            binding.btnEdit.setOnClickListener { onEdit(transaction) }
            binding.btnDelete.setOnClickListener { onDelete(transaction) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) =
            oldItem == newItem
    }
}
