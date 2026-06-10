package com.cashtelo.ui.addtransaction

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cashtelo.R
import com.cashtelo.data.entity.Category
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionType
import com.cashtelo.databinding.FragmentAddTransactionBinding
import com.cashtelo.viewmodel.TransactionViewModel
import com.cashtelo.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    private val args: AddTransactionFragmentArgs by navArgs()
    private var editingTransaction: Transaction? = null

    // Estado do tipo selecionado
    private var selectedType: TransactionType = TransactionType.RECEITA

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setupTypeButtons()

        val editId = args.transactionId
        if (editId != -1L) {
            loadForEditing(editId)
        }

        binding.btnSave.setOnClickListener { saveTransaction() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupTypeButtons() {
        // Estado inicial: Receita selecionada
        selectType(TransactionType.RECEITA)

        binding.btnReceita.setOnClickListener {
            selectType(TransactionType.RECEITA)
        }
        binding.btnDespesa.setOnClickListener {
            selectType(TransactionType.DESPESA)
        }
    }

    private fun selectType(type: TransactionType) {
        selectedType = type

        val green = ContextCompat.getColor(requireContext(), R.color.green_500)
        val red   = ContextCompat.getColor(requireContext(), R.color.red_500)
        val surface = ContextCompat.getColor(requireContext(), R.color.surface)
        val textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary)

        if (type == TransactionType.RECEITA) {
            // Receita: fundo verde, texto branco
            binding.btnReceita.setBackgroundColor(green)
            binding.btnReceita.setTextColor(android.graphics.Color.WHITE)
            // Despesa: fundo neutro, texto secundário
            binding.btnDespesa.setBackgroundColor(surface)
            binding.btnDespesa.setTextColor(textPrimary)
        } else {
            // Despesa: fundo vermelho, texto branco
            binding.btnDespesa.setBackgroundColor(red)
            binding.btnDespesa.setTextColor(android.graphics.Color.WHITE)
            // Receita: fundo neutro
            binding.btnReceita.setBackgroundColor(surface)
            binding.btnReceita.setTextColor(textPrimary)
        }
    }

    private fun loadForEditing(id: Long) {
        lifecycleScope.launch {
            val dao = com.cashtelo.data.database.CashteloDatabase
                .getDatabase(requireActivity().application)
                .transactionDao()
            val t = dao.getById(id) ?: return@launch
            editingTransaction = t
            binding.etTitle.setText(t.title)
            binding.etAmount.setText(t.amount.toString())
            binding.etDescription.setText(t.description)

            val categoryIndex = Category.values().indexOf(t.category)
            if (categoryIndex >= 0) binding.spinnerCategory.setSelection(categoryIndex)

            selectType(t.type)
            binding.btnSave.text = "Atualizar"
        }
    }

    private fun setupCategorySpinner() {
        // Categorias sem emoji
        val categories = Category.values().map { it.label }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Informe o título"
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAmount.error = "Informe um valor válido"
            return
        }

        val categoryIndex = binding.spinnerCategory.selectedItemPosition
        val category = Category.values()[categoryIndex]

        val existing = editingTransaction
        if (existing != null) {
            val updated = existing.copy(
                title = title,
                amount = amount,
                type = selectedType,
                category = category,
                description = description
            )
            viewModel.update(updated)
            Toast.makeText(requireContext(), "Transação atualizada!", Toast.LENGTH_SHORT).show()
        } else {
            val transaction = Transaction(
                title = title,
                amount = amount,
                type = selectedType,
                category = category,
                description = description
            )
            viewModel.insert(transaction)
            Toast.makeText(requireContext(), "Transação adicionada!", Toast.LENGTH_SHORT).show()
        }

        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}