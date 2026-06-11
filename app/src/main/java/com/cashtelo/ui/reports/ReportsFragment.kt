package com.cashtelo.ui.reports

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cashtelo.R
import com.cashtelo.data.entity.Category
import com.cashtelo.data.entity.TransactionType
import com.cashtelo.databinding.FragmentReportsBinding
import com.cashtelo.viewmodel.TransactionViewModel
import com.cashtelo.viewmodel.TransactionViewModelFactory
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    private val currencyFmt = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    // Paleta de cores premium (Fintech dark mode)
    private val chartColors = listOf(
        Color.parseColor("#4DD68C"), // Slot 1 - Verde
        Color.parseColor("#FF6B6B"), // Slot 2 - Vermelho
        Color.parseColor("#5B9FFF"), // Slot 3 - Azul Claro
        Color.parseColor("#FFD166"), // Slot 4 - Dourado
        Color.parseColor("#AB7BFF"), // Slot 5 - Lilás
        Color.parseColor("#06D6A0"), // Slot 6 - Teal
        Color.parseColor("#F97316"), // Slot 7 - Laranja
        Color.parseColor("#EC4899"), // Slot 8 - Rosa
        Color.parseColor("#14B8A6"), // Slot 9 - Ciano
        Color.parseColor("#A78BFA")  // Slot 10- Roxo Claro
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
        setupPieChart()
        setupBarChart()
        observeViewModel()
        
        // Animação de entrada dos cards (Staggered fade+slide)
        val cards = listOf(
            binding.cardNetBalance,
            binding.cardDonutChart,
            binding.cardBarChart,
            binding.cardTopCategories
        )
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 50f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 100L)
                .setDuration(400L)
                .start()
        }
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false

            // Donut hole ampliado para um visual mais premium "Fintech"
            isDrawHoleEnabled = true
            holeRadius = 60f
            transparentCircleRadius = 60f
            setHoleColor(Color.TRANSPARENT)

            setDrawCenterText(true)
            setCenterTextSize(14f)
            setCenterTextColor(Color.WHITE)

            // Sem labels nas fatias, apenas percentual
            setDrawEntryLabels(false)

            // Legenda nativa configurada para wrap
            legend.apply {
                isEnabled = true
                textColor = Color.parseColor("#90A4AE")
                textSize = 12f
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                isWordWrapEnabled = true
                yEntrySpace = 8f
                xEntrySpace = 12f
            }
        }
    }

    private fun setupBarChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setFitBars(true)

            // Sem legenda, já está óbvio que são despesas
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#90A4AE")
                textSize = 11f
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1AFFFFFF")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#90A4AE")
                textSize = 10f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
        }
    }

    private fun observeViewModel() {
        // Observers for Net Balance
        viewModel.totalReceitas.observe(viewLifecycleOwner) { receitas ->
            val rec = receitas ?: 0.0
            binding.tvSummaryReceitas.text = currencyFmt.format(rec)
            updateNetBalance()
        }
        
        viewModel.totalDespesas.observe(viewLifecycleOwner) { despesas ->
            val desp = despesas ?: 0.0
            binding.tvSummaryDespesas.text = currencyFmt.format(desp)
            updateNetBalance()
        }

        // Observer for Categories (Donut & Top Categories)
        viewModel.despesasByCategory.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                binding.cardDonutChart.visibility = View.GONE
                binding.cardBarChart.visibility = View.GONE
                binding.cardTopCategories.visibility = View.GONE
                binding.viewEmptyState.visibility = View.VISIBLE
                return@observe
            }
            binding.cardDonutChart.visibility = View.VISIBLE
            binding.cardBarChart.visibility = View.VISIBLE
            binding.cardTopCategories.visibility = View.VISIBLE
            binding.viewEmptyState.visibility = View.GONE

            // Total despesas para o centro do Donut
            val totalDespesasVal = list.sumOf { it.total }
            binding.pieChart.centerText = "Despesas\n${currencyFmt.format(totalDespesasVal)}"
            
            val entries = list.mapNotNull { ct ->
                val cat = try { Category.valueOf(ct.category) } catch (e: Exception) { null }
                if (cat != null && ct.total > 0)
                    PieEntry(ct.total.toFloat(), cat.label)
                else null
            }

            val dataSet = PieDataSet(entries, "").apply {
                colors = chartColors.take(entries.size)
                valueFormatter = PercentFormatter(binding.pieChart)
                valueTextSize = 11f
                valueTextColor = Color.WHITE
                sliceSpace = 3f
                selectionShift = 8f
            }

            binding.pieChart.data = PieData(dataSet)
            binding.pieChart.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuart)

            // Populate Top Categories Card
            binding.layoutTopCategories.removeAllViews()
            val sortedList = list.sortedByDescending { it.total }.take(3)
            sortedList.forEachIndexed { index, ct ->
                val cat = try { Category.valueOf(ct.category) } catch (e: Exception) { null }
                val label = cat?.label ?: ct.category
                val color = chartColors.getOrElse(index) { Color.GRAY }
                val pct = if (totalDespesasVal > 0) ((ct.total / totalDespesasVal) * 100).toInt() else 0
                
                addTopCategoryItem(label, ct.total, pct, color)
            }
        }

        // Observer for BarChart (Últimos 6 meses processados na UI)
        viewModel.allTransactions.observe(viewLifecycleOwner) { trans ->
            if (trans.isNullOrEmpty()) return@observe

            val despesas = trans.filter { it.type == TransactionType.DESPESA }
            
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val monthNames = arrayOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
            
            val last6Months = mutableListOf<String>()
            for (i in 5 downTo 0) {
                var m = currentMonth - i
                var y = currentYear
                if (m < 0) {
                    m += 12
                    y -= 1
                }
                last6Months.add("${monthNames[m]} $y")
            }

            val map = mutableMapOf<String, Float>()
            last6Months.forEach { map[it] = 0f }

            despesas.forEach { t ->
                cal.timeInMillis = t.date
                val m = cal.get(Calendar.MONTH)
                val y = cal.get(Calendar.YEAR)
                val key = "${monthNames[m]} $y"
                if (map.containsKey(key)) {
                    map[key] = map[key]!! + t.amount.toFloat()
                }
            }

            val barEntries = mutableListOf<BarEntry>()
            val xAxisLabels = mutableListOf<String>()

            last6Months.forEachIndexed { index, key ->
                barEntries.add(BarEntry(index.toFloat(), map[key] ?: 0f))
                xAxisLabels.add(key.split(" ")[0]) // Apenas o nome do mês
            }

            val barDataSet = BarDataSet(barEntries, "Despesas").apply {
                color = Color.parseColor("#5B9FFF") // Azul corporativo suave
                valueTextColor = Color.WHITE
                valueTextSize = 10f
                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry?): String {
                        return if (barEntry?.y ?: 0f > 0) "R$ ${barEntry?.y?.toInt()}" else ""
                    }
                }
            }

            binding.barChart.data = BarData(barDataSet).apply {
                barWidth = 0.5f
            }
            binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
            binding.barChart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseInOutQuart)
        }
    }

    private fun updateNetBalance() {
        val r = viewModel.totalReceitas.value ?: 0.0
        val d = viewModel.totalDespesas.value ?: 0.0
        val net = r - d
        
        binding.tvNetBalance.text = currencyFmt.format(net)
        binding.tvNetBalance.setTextColor(
            if (net >= 0) ContextCompat.getColor(requireContext(), R.color.green_500)
            else ContextCompat.getColor(requireContext(), R.color.red_500)
        )

        val total = r + d
        val healthPct = if (total > 0) ((r / total) * 100).toInt() else 0
        
        ObjectAnimator.ofInt(binding.progressHealth, "progress", binding.progressHealth.progress, healthPct).apply {
            duration = 600
            start()
        }

        binding.progressHealth.progressTintList = ColorStateList.valueOf(
            if (net >= 0) ContextCompat.getColor(requireContext(), R.color.green_500)
            else ContextCompat.getColor(requireContext(), R.color.red_500)
        )
    }

    private fun addTopCategoryItem(label: String, value: Double, percentage: Int, color: Int) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 36)
            }
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvLabel = TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvValue = TextView(context).apply {
            text = currencyFmt.format(value)
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        header.addView(tvLabel)
        header.addView(tvValue)

        // Usamos um LinearLayout dividindo espaço para criar a barra de progresso customizada com cantos arredondados
        val barContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                16
            ).apply { setMargins(0, 16, 0, 0) }
            background = ContextCompat.getDrawable(context, R.drawable.bg_category_bar_track)
            weightSum = 100f
        }

        val barFill = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                percentage.toFloat()
            )
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 8f
                setColor(color)
            }
            background = drawable
        }
        
        val barEmpty = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                100f - percentage.toFloat()
            )
        }

        barContainer.addView(barFill)
        barContainer.addView(barEmpty)

        container.addView(header)
        container.addView(barContainer)

        binding.layoutTopCategories.addView(container)
        
        // Entrada animada da barra
        barFill.post {
            barFill.pivotX = 0f
            barFill.scaleX = 0f
            barFill.animate()
                .scaleX(1f)
                .setDuration(800)
                .setInterpolator(android.view.animation.DecelerateInterpolator(1.5f))
                .start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}