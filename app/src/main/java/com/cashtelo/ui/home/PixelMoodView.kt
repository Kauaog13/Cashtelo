package com.cashtelo.ui.home

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.cashtelo.viewmodel.MoodState

class PixelMoodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var currentMood: MoodState = MoodState.NORMAL
    private var bounceOffset = 0f
    private var bounceAnimator: ValueAnimator? = null

    // Paleta medieval
    private val helmetColor   = Color.parseColor("#8C9EAD") // aço azulado
    private val helmetDark    = Color.parseColor("#546E7A") // sombra do capacete
    private val helmetVisor   = Color.parseColor("#37474F") // viseira
    private val visorSlitColor= Color.parseColor("#00BCD4") // fenda brilhante
    private val armorBody     = Color.parseColor("#78909C") // corpo da armadura
    private val armorDark     = Color.parseColor("#455A64") // detalhe escuro
    private val armorGold     = Color.parseColor("#FFD54F") // detalhes dourados
    private val cloakHappy    = Color.parseColor("#1565C0") // manto azul real (feliz)
    private val cloakNormal   = Color.parseColor("#4A148C") // manto roxo (normal)
    private val cloakSad      = Color.parseColor("#37474F") // manto cinza (triste)
    private val gloveColor    = Color.parseColor("#546E7A") // luvas
    private val bootColor     = Color.parseColor("#3E2723") // botas marrons
    private val bootSole      = Color.parseColor("#212121") // sola
    private val skinColor     = Color.parseColor("#FDBCB4") // pele (visível p/ triste)
    private val eyeColor      = Color.parseColor("#E0F7FA") // olho brilhante
    private val tearColor     = Color.parseColor("#64B5F6") // lágrima
    private val starColor     = Color.parseColor("#FFD700") // estrela (feliz)
    private val shieldColor   = Color.parseColor("#C62828") // escudo (feliz)
    private val shieldGold    = Color.parseColor("#FFD54F") // detalhe escudo

    // Grade: 12 colunas x 18 linhas
    private fun getPixels(mood: MoodState): List<Triple<Int, Int, Int>> {
        val pixels = mutableListOf<Triple<Int, Int, Int>>()
        val cloak = when (mood) {
            MoodState.FELIZ   -> cloakHappy
            MoodState.NORMAL  -> cloakNormal
            MoodState.TRISTE  -> cloakSad
        }

        // ---- CAPACETE (linhas 0-4) ----
        // topo arredondado
        for (c in 3..8)  pixels.add(Triple(c, 0, helmetColor))
        for (c in 2..9)  pixels.add(Triple(c, 1, helmetColor))
        pixels.add(Triple(2, 1, helmetDark)); pixels.add(Triple(9, 1, helmetDark))
        for (c in 2..9)  pixels.add(Triple(c, 2, helmetColor))
        pixels.add(Triple(2, 2, helmetDark)); pixels.add(Triple(9, 2, helmetDark))
        // detalhe dourado no topo
        pixels.add(Triple(5, 0, armorGold)); pixels.add(Triple(6, 0, armorGold))

        // ---- VISEIRA (linhas 3-5) ----
        for (c in 2..9)  pixels.add(Triple(c, 3, helmetVisor))
        for (c in 2..9)  pixels.add(Triple(c, 4, helmetVisor))
        for (c in 2..9)  pixels.add(Triple(c, 5, helmetVisor))
        // fendas da viseira (olhos)
        when (mood) {
            MoodState.FELIZ -> {
                // fenda larga — olhar confiante
                for (c in 3..4) pixels.add(Triple(c, 4, visorSlitColor))
                for (c in 7..8) pixels.add(Triple(c, 4, visorSlitColor))
            }
            MoodState.NORMAL -> {
                pixels.add(Triple(3, 4, visorSlitColor)); pixels.add(Triple(4, 4, visorSlitColor))
                pixels.add(Triple(7, 4, visorSlitColor)); pixels.add(Triple(8, 4, visorSlitColor))
            }
            MoodState.TRISTE -> {
                // fenda pequena/caída — capacete um pouco aberto revelando olhos
                pixels.add(Triple(3, 4, visorSlitColor))
                pixels.add(Triple(7, 4, visorSlitColor))
                // lágrimas saindo da viseira
                pixels.add(Triple(3, 5, tearColor)); pixels.add(Triple(3, 6, tearColor))
                pixels.add(Triple(8, 5, tearColor)); pixels.add(Triple(8, 6, tearColor))
            }
        }
        // borda lateral do capacete
        pixels.add(Triple(1, 3, helmetDark)); pixels.add(Triple(10, 3, helmetDark))
        pixels.add(Triple(1, 4, helmetDark)); pixels.add(Triple(10, 4, helmetDark))
        pixels.add(Triple(1, 5, helmetDark)); pixels.add(Triple(10, 5, helmetDark))

        // ---- PESCOÇO / GORJAL (linha 6) ----
        for (c in 4..7)  pixels.add(Triple(c, 6, armorDark))

        // ---- OMBROS (linha 7) ----
        pixels.add(Triple(0, 7, armorDark));  pixels.add(Triple(1, 7, helmetColor))
        pixels.add(Triple(10, 7, helmetColor)); pixels.add(Triple(11, 7, armorDark))
        for (c in 2..9) pixels.add(Triple(c, 7, armorBody))
        pixels.add(Triple(2, 7, armorGold)); pixels.add(Triple(9, 7, armorGold)) // espauletes dourados

        // ---- CORPO DA ARMADURA (linhas 8-12) ----
        for (r in 8..12) {
            for (c in 2..9) pixels.add(Triple(c, r, armorBody))
            pixels.add(Triple(2, r, armorDark)); pixels.add(Triple(9, r, armorDark))
        }
        // detalhe central dourado (brasão)
        pixels.add(Triple(5, 9, armorGold));  pixels.add(Triple(6, 9, armorGold))
        pixels.add(Triple(5, 10, armorGold)); pixels.add(Triple(6, 10, armorGold))
        // manto atrás dos braços
        pixels.add(Triple(0, 8, cloak)); pixels.add(Triple(1, 8, cloak))
        pixels.add(Triple(10, 8, cloak)); pixels.add(Triple(11, 8, cloak))
        pixels.add(Triple(0, 9, cloak)); pixels.add(Triple(1, 9, cloak))
        pixels.add(Triple(10, 9, cloak)); pixels.add(Triple(11, 9, cloak))

        // ---- BRAÇOS / LUVAS (linhas 8-12) ----
        pixels.add(Triple(1, 10, gloveColor)); pixels.add(Triple(0, 11, gloveColor))
        pixels.add(Triple(1, 11, gloveColor)); pixels.add(Triple(0, 12, gloveColor))
        pixels.add(Triple(10, 10, gloveColor)); pixels.add(Triple(11, 11, gloveColor))
        pixels.add(Triple(10, 11, gloveColor)); pixels.add(Triple(11, 12, gloveColor))

        // ---- ESCUDO (feliz — lado esquerdo) ----
        if (mood == MoodState.FELIZ) {
            for (r in 8..13) pixels.add(Triple(-1, r, shieldColor)) // fora da view, só decorativo
            // escudo visível ao lado esquerdo
            pixels.add(Triple(0, 9, shieldColor));  pixels.add(Triple(0, 10, shieldColor))
            pixels.add(Triple(0, 11, shieldColor)); pixels.add(Triple(0, 12, shieldColor))
            pixels.add(Triple(0, 13, shieldColor))
            pixels.add(Triple(0, 10, shieldGold))   // emblema
        }

        // ---- SAIA DA ARMADURA / MANTO (linhas 13-14) ----
        for (c in 2..9) {
            pixels.add(Triple(c, 13, cloak))
            pixels.add(Triple(c, 14, cloak))
        }
        pixels.add(Triple(2, 13, armorDark)); pixels.add(Triple(9, 13, armorDark))

        // ---- PERNAS (linhas 15-16) ----
        for (c in 2..5)  { pixels.add(Triple(c, 15, armorDark)); pixels.add(Triple(c, 16, armorDark)) }
        for (c in 6..9)  { pixels.add(Triple(c, 15, armorDark)); pixels.add(Triple(c, 16, armorDark)) }

        // ---- BOTAS (linha 17) ----
        for (c in 1..5)  pixels.add(Triple(c, 17, bootColor))
        for (c in 6..10) pixels.add(Triple(c, 17, bootColor))
        for (c in 1..5)  pixels.add(Triple(c, 17, bootSole))
        for (c in 6..10) pixels.add(Triple(c, 17, bootSole))

        // ---- ESTRELAS (feliz) ----
        if (mood == MoodState.FELIZ) {
            pixels.add(Triple(0, 1, starColor))
            pixels.add(Triple(11, 0, starColor))
            pixels.add(Triple(11, 2, starColor))
        }

        return pixels
    }

    fun setMood(mood: MoodState) {
        if (mood != currentMood) {
            currentMood = mood
            startBounceAnimation(mood)
            invalidate()
        }
    }

    private fun startBounceAnimation(mood: MoodState) {
        bounceAnimator?.cancel()
        when (mood) {
            MoodState.FELIZ -> {
                bounceAnimator = ValueAnimator.ofFloat(0f, -10f, 0f).apply {
                    duration = 600
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        bounceOffset = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            MoodState.TRISTE -> {
                bounceAnimator = ValueAnimator.ofFloat(0f, 4f, 0f).apply {
                    duration = 2200
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        bounceOffset = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            MoodState.NORMAL -> {
                bounceOffset = 0f
                invalidate()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bounceAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cols = 12
        val rows = 18
        val pixelSize = minOf(width / cols, height / rows).toFloat()
        val offsetX = (width - cols * pixelSize) / 2f
        val offsetY = (height - rows * pixelSize) / 2f + bounceOffset

        val pixels = getPixels(currentMood)
        for ((col, row, color) in pixels) {
            if (col < 0 || col >= cols) continue // skip off-grid decorations
            paint.color = color
            canvas.drawRect(
                offsetX + col * pixelSize,
                offsetY + row * pixelSize,
                offsetX + col * pixelSize + pixelSize - 1f,
                offsetY + row * pixelSize + pixelSize - 1f,
                paint
            )
        }
    }
}