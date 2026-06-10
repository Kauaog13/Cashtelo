package com.cashtelo.ui.transactions

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cashtelo.R
import com.cashtelo.databinding.FragmentTransactionsBinding
import com.cashtelo.viewmodel.TransactionViewModel
import com.cashtelo.viewmodel.TransactionViewModelFactory

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_transactionsFragment_to_addTransactionFragment)
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onEdit = { transaction ->
                val action = TransactionsFragmentDirections
                    .actionTransactionsFragmentToAddTransactionFragment(transaction.id)
                findNavController().navigate(action)
            },
            onDelete = { transaction ->
                viewModel.delete(transaction)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            adapter.submitList(transactions)
            binding.tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
