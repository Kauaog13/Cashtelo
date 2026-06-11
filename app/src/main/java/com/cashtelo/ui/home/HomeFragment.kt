package com.cashtelo.ui.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cashtelo.R
import com.cashtelo.databinding.FragmentHomeBinding
import com.cashtelo.viewmodel.MoodState
import com.cashtelo.viewmodel.TransactionViewModel
import com.cashtelo.viewmodel.TransactionViewModelFactory
import android.graphics.BitmapFactory
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels {
        TransactionViewModelFactory(requireActivity().application)
    }

    private val userViewModel: com.cashtelo.viewmodel.UserViewModel by activityViewModels {
        com.cashtelo.viewmodel.UserViewModelFactory(requireActivity().application)
    }

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    // Controla se é a primeira vez que os cards entram na tela
    private var hasAnimatedEntrance = false

    // Valor anterior do saldo para o CountUp animado
    private var previousSaldo = 0.0
    private var saldoAnimator: ValueAnimator? = null

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

        // Animação de entrada só na primeira exibição do fragment
        if (!hasAnimatedEntrance) {
            hasAnimatedEntrance = true
            playEntranceAnimation()
        }
    }

    // ────────────────────────────────────────────────────────────
    // Animação de Entrada dos Cards — stagger com fade+slide
    // Cada elemento entra com um delay escalonado para criar
    // o efeito cascata fluido típico de apps financeiros premium.
    // ────────────────────────────────────────────────────────────
    private fun playEntranceAnimation() {
        // Elementos que entram em sequência
        val elementsWithDelay = listOf(
            binding.tvGreeting       to 0L,
            binding.tvAppTitle       to 80L,
            binding.cardHeader       to 160L,
            binding.tvSectionSummary to 320L,
            binding.cardReceitas     to 400L,
            binding.cardDespesas     to 480L,
            binding.tvSectionActions to 560L,
            binding.btnVerTransacoes to 620L,
            binding.btnVerRelatorio  to 700L,
            binding.fabAddTransaction to 780L
        )

        elementsWithDelay.forEach { (view, delay) ->
            // Estado inicial: invisível e deslocado para baixo
            view.alpha = 0f
            view.translationY = 40f

            val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 450
                startDelay = delay
                interpolator = DecelerateInterpolator(2f)
            }
            val slideUp = ObjectAnimator.ofFloat(view, "translationY", 40f, 0f).apply {
                duration = 450
                startDelay = delay
                interpolator = DecelerateInterpolator(2f)
            }

            AnimatorSet().apply {
                playTogether(fadeIn, slideUp)
                start()
            }
        }

        // O FAB principal recebe um toque especial de scale com overshoot
        binding.fabAddTransaction.scaleX = 0.85f
        binding.fabAddTransaction.scaleY = 0.85f
        ObjectAnimator.ofFloat(binding.fabAddTransaction, "scaleX", 0.85f, 1f).apply {
            duration = 500
            startDelay = 820L
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
        ObjectAnimator.ofFloat(binding.fabAddTransaction, "scaleY", 0.85f, 1f).apply {
            duration = 500
            startDelay = 820L
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
    }

    // ────────────────────────────────────────────────────────────
    // observeViewModel — LÓGICA PRESERVADA INTEGRALMENTE
    // Adicionamos apenas micro-animações de atualização de valor
    // e transição suave do mood label.
    // ────────────────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.totalReceitas.observe(viewLifecycleOwner) { receitas ->
            // Atualização com pequeno fade para indicar mudança
            binding.tvReceitas.animateValueChange {
                binding.tvReceitas.text = currencyFormatter.format(receitas ?: 0.0)
            }
        }

        viewModel.totalDespesas.observe(viewLifecycleOwner) { despesas ->
            binding.tvDespesas.animateValueChange {
                binding.tvDespesas.text = currencyFormatter.format(despesas ?: 0.0)
            }
        }

        // Saldo com CountUp animado — o número "sobe" ou "desce" suavemente
        val saldoLive = viewModel.getSaldo()
        saldoLive.observe(viewLifecycleOwner) { saldo ->
            val targetSaldo = saldo ?: 0.0
            animateSaldoCountUp(previousSaldo, targetSaldo)
            previousSaldo = targetSaldo

            // Cor do saldo: verde para positivo, vermelho para negativo (PRESERVADO)
            binding.tvSaldo.setTextColor(
                if (targetSaldo >= 0) ContextCompat.getColor(requireContext(), R.color.green_500)
                else ContextCompat.getColor(requireContext(), R.color.red_500)
            )
        }

        // Mood: atualiza robô e label com transição suave (LÓGICA PRESERVADA)
        viewModel.moodState.observe(viewLifecycleOwner) { mood ->
            binding.pixelMoodView.setMood(mood)

            // Label do mood com crossfade suave (sem emojis — visual financeiro clean)
            binding.tvMoodLabel.fadeUpdateText(when (mood) {
                MoodState.FELIZ  -> "Suas finanças estão ótimas!"
                MoodState.NORMAL -> "Suas finanças estão ok."
                MoodState.TRISTE -> "Cuidado com seus gastos!"
            })
        }

        viewModel.allTransactions.observe(viewLifecycleOwner) { transactions ->
            val count = transactions?.size ?: 0
            binding.tvTransactionCount.text = "$count transação(ões) registrada(s)"
        }

        userViewModel.user.observe(viewLifecycleOwner) { user ->
            val greeting = if (user != null && user.name.isNotBlank()) {
                "Bem vindo ${user.name} ao Cashtelo."
            } else {
                "Bem vindo ao Cashtelo."
            }
            binding.tvGreeting.fadeUpdateText(greeting)

            if (user != null) {
                val avatarPath = user.avatarUri
                if (!avatarPath.isNullOrEmpty() && File(avatarPath).exists()) {
                    val bitmap = BitmapFactory.decodeFile(avatarPath)
                    binding.ivHomeAvatar.setImageBitmap(bitmap)
                    binding.ivHomeAvatar.visibility = View.VISIBLE
                    binding.tvHomeAvatarInitials.visibility = View.GONE
                } else if (user.name.isNotEmpty()) {
                    val initial = user.name.first().uppercaseChar().toString()
                    binding.tvHomeAvatarInitials.text = initial
                    binding.tvHomeAvatarInitials.visibility = View.VISIBLE
                    binding.ivHomeAvatar.visibility = View.GONE
                } else {
                    binding.ivHomeAvatar.visibility = View.VISIBLE
                    binding.tvHomeAvatarInitials.visibility = View.GONE
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // CountUp animado para o saldo
    // Interpola suavemente entre o valor anterior e o novo.
    // ────────────────────────────────────────────────────────────
    private fun animateSaldoCountUp(from: Double, to: Double) {
        saldoAnimator?.cancel()
        saldoAnimator = ValueAnimator.ofFloat(from.toFloat(), to.toFloat()).apply {
            duration = if (kotlin.math.abs(to - from) < 0.01) 0L else 600L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val current = (anim.animatedValue as Float).toDouble()
                binding.tvSaldo.text = currencyFormatter.format(current)
            }
            start()
        }
    }

    // ────────────────────────────────────────────────────────────
    // setupClickListeners — PRESERVADO + feedback háptico
    // ────────────────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.fabAddTransaction.setOnClickListener {
            hapticFeedback()
            // Micro-bounce no FAB antes de navegar
            it.animate()
                .scaleX(0.93f).scaleY(0.93f)
                .setDuration(80)
                .withEndAction {
                    it.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(OvershootInterpolator())
                        .withEndAction {
                            findNavController().navigate(
                                R.id.action_homeFragment_to_addTransactionFragment
                            )
                        }
                        .start()
                }
                .start()
        }

        binding.btnVerTransacoes.setOnClickListener {
            hapticFeedback()
            findNavController().navigate(R.id.action_homeFragment_to_transactionsFragment)
        }

        binding.btnVerRelatorio.setOnClickListener {
            hapticFeedback()
            findNavController().navigate(R.id.action_homeFragment_to_reportsFragment)
        }
    }

    // ────────────────────────────────────────────────────────────
    // Helpers de micro-animação
    // ────────────────────────────────────────────────────────────

    /**
     * Crossfade de texto: faz fade-out, atualiza conteúdo, depois fade-in.
     */
    private fun View.animateValueChange(updateContent: () -> Unit) {
        animate()
            .alpha(0f)
            .setDuration(120)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                updateContent()
                animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    /**
     * Fade crossfade de texto para o TextView de mood label.
     */
    private fun android.widget.TextView.fadeUpdateText(newText: String) {
        if (text.toString() == newText) return
        animate()
            .alpha(0f)
            .translationY(-6f)
            .setDuration(150)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                text = newText
                translationY = 6f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator(2f))
                    .start()
            }
            .start()
    }

    /**
     * Feedback háptico leve nos toques de botão.
     * Compatível com API 26+ (minSdk do projeto).
     */
    private fun hapticFeedback() {
        val vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE)
                as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    // ────────────────────────────────────────────────────────────
    // Lifecycle cleanup
    // ────────────────────────────────────────────────────────────
    override fun onDestroyView() {
        super.onDestroyView()
        saldoAnimator?.cancel()
        _binding = null
    }
}
