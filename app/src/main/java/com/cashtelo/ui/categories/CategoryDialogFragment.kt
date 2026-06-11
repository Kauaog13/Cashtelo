package com.cashtelo.ui.categories

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.cashtelo.data.entity.Category
import com.cashtelo.databinding.DialogCategoryBinding
import com.cashtelo.viewmodel.CategoryViewModel

class CategoryDialogFragment : DialogFragment() {

    private var _binding: DialogCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels({ requireParentFragment() })

    private var existingCategory: Category? = null

    companion object {
        private const val ARG_ID   = "id"
        private const val ARG_NAME = "name"

        fun newInstance(category: Category?): CategoryDialogFragment {
            val frag = CategoryDialogFragment()
            if (category != null) {
                frag.arguments = Bundle().apply {
                    putLong(ARG_ID, category.id)
                    putString(ARG_NAME, category.name)
                }
            }
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        if (args != null) {
            existingCategory = Category(
                id   = args.getLong(ARG_ID),
                name = args.getString(ARG_NAME, "")
            )
            binding.etCategoryName.setText(existingCategory!!.name)
            binding.tvDialogTitle.text = "Editar Categoria"
        } else {
            binding.tvDialogTitle.text = "Nova Categoria"
        }

        binding.btnSaveCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etCategoryName.error = "Digite um nome"
                return@setOnClickListener
            }

            val category = existingCategory?.copy(name = name)
                ?: Category(name = name)

            if (existingCategory != null) viewModel.update(category)
            else viewModel.insert(category)

            Toast.makeText(
                requireContext(),
                if (existingCategory != null) "Categoria atualizada!" else "Categoria criada!",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }

        binding.btnCancelCategory.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
