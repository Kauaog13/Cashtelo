package com.cashtelo.ui.reports

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cashtelo.data.entity.Category
import com.cashtelo.databinding.FragmentReportsBinding
import com.cashtelo.viewmodel.TransactionViewModel
import com.cashtelo.viewmodel.TransactionViewModelFactory
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.Locale

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    private val currencyFmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private val chartColors = listOf(
        Color.parseColor("#FF6384"),
        Color.parseColor("#36A2EB"),
        Color.parseColor("#FFCE56"),
        Color.parseColor("#4BC0C0"),
        Color.parseColor("#9966FF"),
        Color.parseColor("#FF9F40"),
        Color.parseColor("#71B37C"),
        Color.parseColor("#F7464A"),
        Color.parseColor("#46BFBD"),
        Color.parseColor("#FDB45C"),
        Color.parseColor("#949FB1"),
        Color.parseColor("#A78BFA")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeViewModel()
    }

    private fun setupChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false

            // Sem anel transparente — visual mais limpo
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 40f  // igual ao holeRadius = sem anel
            setHoleColor(Color.parseColor("#1A2840")) // cor do surface

            setDrawCenterText(true)
            centerText = "Despesas"
            setCenterTextSize(14f)
            setCenterTextColor(Color.WHITE)

            // Sem labels nas fatias — percentual só
            setDrawEntryLabels(false)

            // Legenda abaixo, horizontal
            legend.apply {
                isEnabled = true
                textColor = Color.parseColor("#90A4AE")
                textSize = 11f
                form = Legend.LegendForm.CIRCLE
                formSize = 10f
                orientation = Legend.LegendOrientation.HORIZONTAL
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                isWordWrapEnabled = true
            }

            animateY(900)
        }
    }

    private fun observeViewModel() {
        viewModel.totalReceitas.observe(viewLifecycleOwner) { receitas ->
            binding.tvTotalReceitas.text = currencyFmt.format(receitas ?: 0.0)
        }
        viewModel.totalDespesas.observe(viewLifecycleOwner) { despesas ->
            binding.tvTotalDespesas.text = currencyFmt.format(despesas ?: 0.0)
        }

        viewModel.despesasByCategory.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.pieChart.visibility = View.GONE
                return@observe
            }
            binding.tvNoData.visibility = View.GONE
            binding.pieChart.visibility = View.VISIBLE

            val entries = list.mapNotNull { ct ->
                val cat = try { Category.valueOf(ct.category) } catch (e: Exception) { null }
                if (cat != null && ct.total > 0)
                    PieEntry(ct.total.toFloat(), cat.label) // sem emoji na legenda
                else null
            }

            val dataSet = PieDataSet(entries, "").apply {
                colors = chartColors.take(entries.size)
                valueFormatter = PercentFormatter(binding.pieChart)
                valueTextSize = 12f
                valueTextColor = Color.WHITE
                sliceSpace = 2f          // espaço entre as fatias
                selectionShift = 6f      // quanto cresce ao tocar
            }

            binding.pieChart.data = PieData(dataSet)
            binding.pieChart.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}