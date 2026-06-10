package com.cashtelo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cashtelo.R
import com.cashtelo.databinding.FragmentHomeBinding
import com.cashtelo.viewmodel.MoodState
import com.cashtelo.viewmodel.TransactionViewModel
import com.cashtelo.viewmodel.TransactionViewModelFactory
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewModel.totalReceitas.observe(viewLifecycleOwner) { receitas ->
            binding.tvReceitas.text = currencyFormatter.format(receitas ?: 0.0)
        }

        viewModel.totalDespesas.observe(viewLifecycleOwner) { despesas ->
            binding.tvDespesas.text = currencyFormatter.format(despesas ?: 0.0)
        }

        // Observe saldo via MediatorLiveData
        val saldoLive = viewModel.getSaldo()
        saldoLive.observe(viewLifecycleOwner) { saldo ->
            val s = saldo ?: 0.0
            binding.tvSaldo.text = currencyFormatter.format(s)
            binding.tvSaldo.setTextColor(
                if (s >= 0) requireContext().getColor(R.color.green_500)
                else requireContext().getColor(R.color.red_500)
            )
        }

        viewModel.moodState.observe(viewLifecycleOwner) { mood ->
            binding.pixelMoodView.setMood(mood)
            binding.tvMoodLabel.text = when (mood) {
                MoodState.FELIZ -> "😄 Suas finanças estão ótimas!"
                MoodState.NORMAL -> "😐 Suas finanças estão ok."
                MoodState.TRISTE -> "😢 Cuidado com seus gastos!"
            }
        }

        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            val count = transactions?.size ?: 0
            binding.tvTransactionCount.text = "$count transação(ões) registrada(s)"
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addTransactionFragment)
        }
        binding.btnVerTransacoes.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_transactionsFragment)
        }
        binding.btnVerRelatorio.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_reportsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
