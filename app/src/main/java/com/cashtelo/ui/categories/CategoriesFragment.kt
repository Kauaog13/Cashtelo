package com.cashtelo.ui.categories

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cashtelo.data.entity.Category
import com.cashtelo.data.entity.TransactionCategory
import com.cashtelo.databinding.FragmentCategoriesBinding
import com.cashtelo.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(400).start()

        adapter = CategoryAdapter(
            onEdit = { showCategoryDialog(it) },
            onDelete = { confirmDelete(it) }
        )

        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        viewModel.allCategories.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                seedDefaultCategories()
            } else {
                adapter.submitList(list)
                binding.tvEmptyState.visibility = View.GONE
            }
        }

        binding.fabAddCategory.setOnClickListener {
            showCategoryDialog(null)
        }
    }

    private fun seedDefaultCategories() {
        lifecycleScope.launch {
            // Map the existing TransactionCategory enum into Room Category entities
            val defaults = listOf(
                Category(name = "Salário",      type = "RECEITA"),
                Category(name = "Freelance",    type = "RECEITA"),
                Category(name = "Investimento", type = "RECEITA"),
                Category(name = "Presente",     type = "RECEITA"),
                Category(name = "Alimentação",  type = "DESPESA"),
                Category(name = "Transporte",   type = "DESPESA"),
                Category(name = "Moradia",      type = "DESPESA"),
                Category(name = "Saúde",        type = "DESPESA"),
                Category(name = "Educação",     type = "DESPESA"),
                Category(name = "Lazer",        type = "DESPESA"),
                Category(name = "Roupas",       type = "DESPESA"),
                Category(name = "Outros",       type = "DESPESA")
            )
            defaults.forEach { viewModel.insert(it) }
        }
    }

    private fun showCategoryDialog(category: Category?) {
        val dialog = CategoryDialogFragment.newInstance(category)
        dialog.show(childFragmentManager, "CategoryDialog")
    }

    private fun confirmDelete(category: Category) {
        AlertDialog.Builder(requireContext())
            .setTitle("Excluir categoria")
            .setMessage("Deseja excluir \"${category.name}\"?")
            .setPositiveButton("Excluir") { _, _ ->
                viewModel.delete(category)
                Toast.makeText(requireContext(), "Categoria excluída", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
