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
import com.cashtelo.data.entity.TransactionCategory
import com.cashtelo.data.entity.Transaction
import com.cashtelo.data.entity.TransactionType
import com.cashtelo.databinding.FragmentAddTransactionBinding
import com.cashtelo.viewmodel.CategoryViewModel
import com.cashtelo.viewmodel.TransactionViewModel
import com.cashtelo.viewmodel.TransactionViewModelFactory
import kotlinx.coroutines.launch

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    // CategoryViewModel compartilhado para ler categorias do banco Room
    private val categoryViewModel: CategoryViewModel by activityViewModels()

    private val args: AddTransactionFragmentArgs by navArgs()
    private var editingTransaction: Transaction? = null
    private var selectedType: TransactionType = TransactionType.RECEITA

    // Lista de categorias carregadas do Room
    private var categoryList: List<Category> = emptyList()
    private var selectedCategoryIndex: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeButtons()
        observeCategories()

        val editId = args.transactionId
        if (editId != -1L) {
            loadForEditing(editId)
        }

        binding.btnSave.setOnClickListener { saveTransaction() }
        binding.btnCancel.setOnClickListener { findNavController().navigateUp() }
    }

    private fun observeCategories() {
        categoryViewModel.allCategories.observe(viewLifecycleOwner) { list ->
            categoryList = list
            setupCategoryDropdown(list)

            // Se estiver em modo edição, re-seleciona a categoria correta após a lista carregar
            editingTransaction?.let { t ->
                val idx = list.indexOfFirst { it.name.equals(t.category.label, ignoreCase = true) }
                if (idx >= 0) {
                    selectedCategoryIndex = idx
                    binding.spinnerTransactionCategory.setText(list[idx].name, false)
                }
            }
        }
    }

    private fun setupCategoryDropdown(categories: List<Category>) {
        val labels = categories.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown_category,
            labels
        )
        binding.spinnerTransactionCategory.setAdapter(adapter)

        if (labels.isNotEmpty()) {
            binding.spinnerTransactionCategory.setText(labels[selectedCategoryIndex.coerceIn(0, labels.lastIndex)], false)
        }

        binding.spinnerTransactionCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryIndex = position
        }
    }

    private fun setupTypeButtons() {
        selectType(TransactionType.RECEITA)

        binding.btnReceita.setOnClickListener { selectType(TransactionType.RECEITA) }
        binding.btnDespesa.setOnClickListener { selectType(TransactionType.DESPESA) }
    }

    private fun selectType(type: TransactionType) {
        selectedType = type

        val green = ContextCompat.getColor(requireContext(), R.color.green_500)
        val red   = ContextCompat.getColor(requireContext(), R.color.red_500)
        val surface = ContextCompat.getColor(requireContext(), R.color.surface)
        val textPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary)

        if (type == TransactionType.RECEITA) {
            binding.btnReceita.setBackgroundColor(green)
            binding.btnReceita.setTextColor(android.graphics.Color.WHITE)
            binding.btnDespesa.setBackgroundColor(surface)
            binding.btnDespesa.setTextColor(textPrimary)
        } else {
            binding.btnDespesa.setBackgroundColor(red)
            binding.btnDespesa.setTextColor(android.graphics.Color.WHITE)
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

            // Tenta encontrar a categoria pelo nome na lista já carregada (se disponível)
            val idx = categoryList.indexOfFirst { it.name.equals(t.category.label, ignoreCase = true) }
            if (idx >= 0) {
                selectedCategoryIndex = idx
                binding.spinnerTransactionCategory.setText(categoryList[idx].name, false)
            }

            selectType(t.type)
            binding.btnSave.text = "Atualizar"
        }
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

        if (categoryList.isEmpty()) {
            Toast.makeText(requireContext(), "Aguarde as categorias carregarem", Toast.LENGTH_SHORT).show()
            return
        }

        // Mapeia a categoria do Room para o enum TransactionCategory pelo nome
        val selectedCategory = categoryList.getOrNull(selectedCategoryIndex)
        val category = selectedCategory?.let { cat ->
            TransactionCategory.values().firstOrNull { it.label.equals(cat.name, ignoreCase = true) }
        } ?: TransactionCategory.OUTROS

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
