package com.cashtelo.ui.home

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.cashtelo.viewmodel.MoodState
import kotlin.math.*
import kotlin.random.Random

/**
 * PixelMoodView — Robô em pixel art que reage ao estado financeiro.
 *
 * MELHORIAS VISUAIS (sem alterar a lógica de MoodState):
 * ─────────────────────────────────────────────────────
 * 1. Cache de pixels por estado → sem recalcular em cada frame
 * 2. Glow nos olhos (visor) via BlurMaskFilter + RadialGradient
 * 3. Sombra projetada sob o robô
 * 4. Animação de "ventilação" (scale suave 1.00 → 1.02 → 1.00) com AccelDecel
 * 5. Bounce com OvershootInterpolator (FELIZ) e AccelDecel (TRISTE) — 60fps
 * 6. CrossFade de cores entre estados via ArgbEvaluator
 * 7. Sistema de partículas leve:
 * - FELIZ  → estrelinhas douradas que sobem e somem
 * - TRISTE → gotículas que caem
 * - NORMAL → sem partículas
 * 8. Efeito de "flash" translucido branco quando o mood muda
 */
class PixelMoodView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ────────────────────────────────────────────────────────────
    // Paints
    // ────────────────────────────────────────────────────────────
    private val pixelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#66000000")
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // ────────────────────────────────────────────────────────────
    // Estado atual e alvo (para crossfade)
    // ────────────────────────────────────────────────────────────
    private var currentMood: MoodState = MoodState.NORMAL
    private var previousMood: MoodState = MoodState.NORMAL

    // ────────────────────────────────────────────────────────────
    // Animação: valores atuais
    // ────────────────────────────────────────────────────────────
    private var bounceOffset  = 0f   // translação vertical do robô
    private var breathScale   = 1f   // escala suave de ventilação/respiração
    private var flashAlpha    = 0f   // 0..1 — flash branco de transição
    private var crossfadeFrac = 1f   // 0..1 — fração de interpolação de cor (1 = no estado atual)
    private var glowRadius    = 4f   // raio do glow nos olhos
    private var glowAlpha     = 200  // alpha do glow (pulsante)

    // ────────────────────────────────────────────────────────────
    // Animadores
    // ────────────────────────────────────────────────────────────
    private var bounceAnimator:  ValueAnimator? = null
    private var breathAnimator:  ValueAnimator? = null
    private var flashAnimator:   ValueAnimator? = null
    private var crossfadeAnimator: ValueAnimator? = null
    private var glowAnimator:    ValueAnimator? = null

    // ────────────────────────────────────────────────────────────
    // Paleta do Robô (PRESERVADA do original + novos glow colors)
    // ────────────────────────────────────────────────────────────
    private val headColor         = Color.parseColor("#8C9EAD")
    private val headDark          = Color.parseColor("#546E7A")
    private val visorColor        = Color.parseColor("#37474F")
    private val eyeLedColor       = Color.parseColor("#00BCD4")
    private val mainMetalColor    = Color.parseColor("#78909C")
    private val bodyDark          = Color.parseColor("#455A64")
    private val circuitGold       = Color.parseColor("#FFD54F")
    private val auraHappy         = Color.parseColor("#1565C0")
    private val auraNormal        = Color.parseColor("#4A148C")
    private val auraSad           = Color.parseColor("#37474F")
    private val clawColor         = Color.parseColor("#546E7A")
    private val propulsorColor    = Color.parseColor("#3E2723")
    private val propulsorBase     = Color.parseColor("#212121")
    private val tearColor         = Color.parseColor("#64B5F6")
    private val starColor         = Color.parseColor("#FFD700")
    private val chestDisplayColor = Color.parseColor("#C62828")
    private val chestDisplayGold  = Color.parseColor("#FFD54F")

    // Glow por estado
    private val glowColorHappy  = Color.parseColor("#5B9FFF")
    private val glowColorNormal = Color.parseColor("#AB7BFF")
    private val glowColorSad    = Color.parseColor("#6BAED6")

    // ────────────────────────────────────────────────────────────
    // Cache de pixels por estado (evita recalcular em cada frame)
    // ────────────────────────────────────────────────────────────
    private val pixelCache = mutableMapOf<MoodState, List<Triple<Int, Int, Int>>>()

    // ────────────────────────────────────────────────────────────
    // Sistema de partículas
    // ────────────────────────────────────────────────────────────
    private data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var alpha: Float, var size: Float,
        var color: Int, var life: Float   // life: 0..1 (1 = vivo, 0 = morto)
    )

    private val particles = mutableListOf<Particle>()
    private var lastParticleTime = 0L
    private val PARTICLE_INTERVAL_HAPPY = 350L  // ms entre spawns (FELIZ)
    private val PARTICLE_INTERVAL_SAD   = 500L  // ms entre spawns (TRISTE)

    // ────────────────────────────────────────────────────────────
    // ArgbEvaluator para crossfade suave entre paletas de cor
    // ────────────────────────────────────────────────────────────
    private val argbEvaluator = ArgbEvaluator()

    // ────────────────────────────────────────────────────────────
    // Grid: 12 colunas × 18 linhas (PRESERVADO do original)
    // ────────────────────────────────────────────────────────────
    private val COLS = 12
    private val ROWS = 18

    // ────────────────────────────────────────────────────────────
    // getPixels — Lógica de desenho integralmente preservada
    // Nomenclaturas adaptadas para a anatomia de um robô
    // ────────────────────────────────────────────────────────────
    private fun getPixels(mood: MoodState): List<Triple<Int, Int, Int>> {
        pixelCache[mood]?.let { return it }

        val pixels = mutableListOf<Triple<Int, Int, Int>>()
        val aura = when (mood) {
            MoodState.FELIZ  -> auraHappy
            MoodState.NORMAL -> auraNormal
            MoodState.TRISTE -> auraSad
        }

        // ---- CABEÇA / TOPO (linhas 0-4) ----
        for (c in 3..8)  pixels.add(Triple(c, 0, headColor))
        for (c in 2..9)  pixels.add(Triple(c, 1, headColor))
        pixels.add(Triple(2, 1, headDark)); pixels.add(Triple(9, 1, headDark))
        for (c in 2..9)  pixels.add(Triple(c, 2, headColor))
        pixels.add(Triple(2, 2, headDark)); pixels.add(Triple(9, 2, headDark))
        // detalhe dourado no topo (antena/processador)
        pixels.add(Triple(5, 0, circuitGold)); pixels.add(Triple(6, 0, circuitGold))

        // ---- VISOR / TELA (linhas 3-5) ----
        for (c in 2..9)  pixels.add(Triple(c, 3, visorColor))
        for (c in 2..9)  pixels.add(Triple(c, 4, visorColor))
        for (c in 2..9)  pixels.add(Triple(c, 5, visorColor))
        // fendas do visor (olhos de LED) — LÓGICA DE ESTADO PRESERVADA
        when (mood) {
            MoodState.FELIZ -> {
                for (c in 3..4) pixels.add(Triple(c, 4, eyeLedColor))
                for (c in 7..8) pixels.add(Triple(c, 4, eyeLedColor))
            }
            MoodState.NORMAL -> {
                pixels.add(Triple(3, 4, eyeLedColor)); pixels.add(Triple(4, 4, eyeLedColor))
                pixels.add(Triple(7, 4, eyeLedColor)); pixels.add(Triple(8, 4, eyeLedColor))
            }
            MoodState.TRISTE -> {
                // LED pequeno/caído
                pixels.add(Triple(3, 4, eyeLedColor))
                pixels.add(Triple(7, 4, eyeLedColor))
                // fuga de fluido refrigerante (lágrimas)
                pixels.add(Triple(3, 5, tearColor)); pixels.add(Triple(3, 6, tearColor))
                pixels.add(Triple(8, 5, tearColor)); pixels.add(Triple(8, 6, tearColor))
            }
        }
        // borda lateral da cabeça
        pixels.add(Triple(1, 3, headDark)); pixels.add(Triple(10, 3, headDark))
        pixels.add(Triple(1, 4, headDark)); pixels.add(Triple(10, 4, headDark))
        pixels.add(Triple(1, 5, headDark)); pixels.add(Triple(10, 5, headDark))

        // ---- PESCOÇO / ARTICULAÇÃO (linha 6) ----
        for (c in 4..7)  pixels.add(Triple(c, 6, bodyDark))

        // ---- OMBROS (linha 7) ----
        pixels.add(Triple(0, 7, bodyDark));  pixels.add(Triple(1, 7, headColor))
        pixels.add(Triple(10, 7, headColor)); pixels.add(Triple(11, 7, bodyDark))
        for (c in 2..9) pixels.add(Triple(c, 7, mainMetalColor))
        pixels.add(Triple(2, 7, circuitGold)); pixels.add(Triple(9, 7, circuitGold))

        // ---- CHASSI / TRONCO (linhas 8-12) ----
        for (r in 8..12) {
            for (c in 2..9) pixels.add(Triple(c, r, mainMetalColor))
            pixels.add(Triple(2, r, bodyDark)); pixels.add(Triple(9, r, bodyDark))
        }
        // detalhe central dourado (núcleo / painel)
        pixels.add(Triple(5, 9, circuitGold));  pixels.add(Triple(6, 9, circuitGold))
        pixels.add(Triple(5, 10, circuitGold)); pixels.add(Triple(6, 10, circuitGold))
        
        // painel traseiro / aura projetada
        pixels.add(Triple(0, 8, aura)); pixels.add(Triple(1, 8, aura))
        pixels.add(Triple(10, 8, aura)); pixels.add(Triple(11, 8, aura))
        pixels.add(Triple(0, 9, aura)); pixels.add(Triple(1, 9, aura))
        pixels.add(Triple(10, 9, aura)); pixels.add(Triple(11, 9, aura))

        // ---- BRAÇOS / GARRAS (linhas 8-12) ----
        pixels.add(Triple(1, 10, clawColor)); pixels.add(Triple(0, 11, clawColor))
        pixels.add(Triple(1, 11, clawColor)); pixels.add(Triple(0, 12, clawColor))
        pixels.add(Triple(10, 10, clawColor)); pixels.add(Triple(11, 11, clawColor))
        pixels.add(Triple(10, 11, clawColor)); pixels.add(Triple(11, 12, clawColor))

        // ---- BATERIA EXTRA / DISPLAY (feliz) ----
        if (mood == MoodState.FELIZ) {
            pixels.add(Triple(0, 9, chestDisplayColor));  pixels.add(Triple(0, 10, chestDisplayColor))
            pixels.add(Triple(0, 11, chestDisplayColor)); pixels.add(Triple(0, 12, chestDisplayColor))
            pixels.add(Triple(0, 13, chestDisplayColor))
            pixels.add(Triple(0, 10, chestDisplayGold))
        }

        // ---- BASE DO CHASSI / AURA (linhas 13-14) ----
        for (c in 2..9) {
            pixels.add(Triple(c, 13, aura))
            pixels.add(Triple(c, 14, aura))
        }
        pixels.add(Triple(2, 13, bodyDark)); pixels.add(Triple(9, 13, bodyDark))

        // ---- PERNAS MECÂNICAS (linhas 15-16) ----
        for (c in 2..5)  { pixels.add(Triple(c, 15, bodyDark)); pixels.add(Triple(c, 16, bodyDark)) }
        for (c in 6..9)  { pixels.add(Triple(c, 15, bodyDark)); pixels.add(Triple(c, 16, bodyDark)) }

        // ---- PÉS / PROPULSORES (linha 17) ----
        for (c in 1..5)  pixels.add(Triple(c, 17, propulsorColor))
        for (c in 6..10) pixels.add(Triple(c, 17, propulsorColor))
        for (c in 1..5)  pixels.add(Triple(c, 17, propulsorBase))
        for (c in 6..10) pixels.add(Triple(c, 17, propulsorBase))

        // ---- ESTRELAS / FAÍSCAS (feliz) ----
        if (mood == MoodState.FELIZ) {
            pixels.add(Triple(0, 1, starColor))
            pixels.add(Triple(11, 0, starColor))
            pixels.add(Triple(11, 2, starColor))
        }

        pixelCache[mood] = pixels
        return pixels
    }

    // ────────────────────────────────────────────────────────────
    // setMood — ponto de entrada público. Lógica de estado intacta.
    // ────────────────────────────────────────────────────────────
    fun setMood(mood: MoodState) {
        if (mood == currentMood) return

        previousMood = currentMood
        currentMood = mood

        // 1. Flash de transição (breve clarão branco)
        triggerFlash()

        // 2. CrossFade de cores entre o estado anterior e o novo
        startCrossfade()

        // 3. Bounce/movimento principal muda conforme o novo estado
        startBounceAnimation(mood)

        // 4. Reinicia partículas do novo estado
        particles.clear()
        lastParticleTime = System.currentTimeMillis()

        // 5. Reinicia animação de ventilação com energia adequada ao estado
        startBreathAnimation(mood)

        // 6. Ajusta glow
        startGlowAnimation(mood)
    }

    // ────────────────────────────────────────────────────────────
    // Bounce — LÓGICA DE ESTADO PRESERVADA, interpoladores melhorados
    // ────────────────────────────────────────────────────────────
    private fun startBounceAnimation(mood: MoodState) {
        bounceAnimator?.cancel()
        when (mood) {
            MoodState.FELIZ -> {
                // Bounce energético mas contido — parece que o robô pula de alegria
                bounceAnimator = ValueAnimator.ofFloat(0f, -10f, 0f).apply {
                    duration = 700
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener {
                        bounceOffset = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            MoodState.TRISTE -> {
                // Oscilação lenta e pesada — robô está cabisbaixo (bateria fraca)
                bounceAnimator = ValueAnimator.ofFloat(0f, 5f, 0f).apply {
                    duration = 2800
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener {
                        bounceOffset = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
            MoodState.NORMAL -> {
                // Sem bounce; apenas ventilação padrão
                bounceAnimator = ValueAnimator.ofFloat(bounceOffset, 0f).apply {
                    duration = 400
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        bounceOffset = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Ventilação / Respiração — scale suave, energia varia por estado
    // ────────────────────────────────────────────────────────────
    private fun startBreathAnimation(mood: MoodState) {
        breathAnimator?.cancel()
        val (scaleMax, dur) = when (mood) {
            MoodState.FELIZ  -> 1.04f to 800L   // ventila rápido de empolgação
            MoodState.NORMAL -> 1.02f to 1400L  // operação estável
            MoodState.TRISTE -> 1.015f to 2200L // operação lenta e pesada
        }
        breathAnimator = ValueAnimator.ofFloat(1f, scaleMax, 1f).apply {
            duration = dur
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                breathScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // ────────────────────────────────────────────────────────────
    // Glow pulsante nos LEDs — raio e alpha variam por estado
    // ────────────────────────────────────────────────────────────
    private fun startGlowAnimation(mood: MoodState) {
        glowAnimator?.cancel()
        val (minR, maxR, minA, maxA, dur) = when (mood) {
            MoodState.FELIZ  -> GlowParams(3f, 8f, 180, 255, 700L)
            MoodState.NORMAL -> GlowParams(2f, 5f, 120, 200, 1400L)
            MoodState.TRISTE -> GlowParams(1f, 3f, 80, 140, 2200L)
        }
        glowAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = dur
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                // Ping-pong manual: vai e volta
                val t = if (f < 0.5f) f * 2f else (1f - f) * 2f
                glowRadius = minR + (maxR - minR) * t
                glowAlpha  = (minA + (maxA - minA) * t).toInt()
                invalidate()
            }
            start()
        }
    }

    private data class GlowParams(
        val minRadius: Float, val maxRadius: Float,
        val minAlpha: Int, val maxAlpha: Int,
        val duration: Long
    )

    // ────────────────────────────────────────────────────────────
    // Flash de transição — breve clarão branco ao mudar estado
    // ────────────────────────────────────────────────────────────
    private fun triggerFlash() {
        flashAnimator?.cancel()
        flashAnimator = ValueAnimator.ofFloat(0.25f, 0f).apply {
            duration = 350
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener {
                flashAlpha = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // ────────────────────────────────────────────────────────────
    // CrossFade de cores entre estados (cor da aura, por exemplo)
    // ────────────────────────────────────────────────────────────
    private fun startCrossfade() {
        crossfadeAnimator?.cancel()
        crossfadeFrac = 0f
        crossfadeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                crossfadeFrac = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    // ────────────────────────────────────────────────────────────
    // Gerador de partículas
    // ────────────────────────────────────────────────────────────
    private fun spawnParticles(pixelSize: Float, offsetX: Float, offsetY: Float) {
        val now = System.currentTimeMillis()
        val interval = when (currentMood) {
            MoodState.FELIZ  -> PARTICLE_INTERVAL_HAPPY
            MoodState.TRISTE -> PARTICLE_INTERVAL_SAD
            MoodState.NORMAL -> return
        }
        if (now - lastParticleTime < interval) return
        lastParticleTime = now

        when (currentMood) {
            MoodState.FELIZ -> {
                // Estrelinhas/faíscas douradas sobem do topo da cabeça
                repeat(2) {
                    val cx = offsetX + (3 + Random.nextInt(6)) * pixelSize
                    val cy = offsetY + Random.nextFloat() * 2f * pixelSize
                    particles.add(Particle(
                        x = cx, y = cy,
                        vx = (Random.nextFloat() - 0.5f) * 1.5f,
                        vy = -1.5f - Random.nextFloat() * 1.5f,
                        alpha = 1f, size = pixelSize * 0.35f,
                        color = starColor, life = 1f
                    ))
                }
            }
            MoodState.TRISTE -> {
                // Fugas de fluido refrigerante (lágrimas)
                val tearX = listOf(
                    offsetX + 3.5f * pixelSize,
                    offsetX + 7.5f * pixelSize
                )
                tearX.forEach { cx ->
                    particles.add(Particle(
                        x = cx, y = offsetY + 5f * pixelSize,
                        vx = 0f, vy = 1.2f + Random.nextFloat() * 0.8f,
                        alpha = 0.8f, size = pixelSize * 0.25f,
                        color = tearColor, life = 1f
                    ))
                }
            }
            MoodState.NORMAL -> {}
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.life -= dt * 0.008f
            p.alpha = p.life.coerceIn(0f, 1f)
            if (p.life <= 0f) iter.remove()
        }
    }

    // ────────────────────────────────────────────────────────────
    // onDraw — redesenhado com todas as melhorias visuais
    // ────────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dimensões do pixel com base na menor proporção disponível
        val pixelSize = minOf(width.toFloat() / COLS, height.toFloat() / ROWS)

        // Ponto de origem centralizado + bounce + ventilação
        val totalW = COLS * pixelSize
        val totalH = ROWS * pixelSize
        val baseX = (width  - totalW) / 2f
        val baseY = (height - totalH) / 2f + bounceOffset

        // Ponto central para escala de ventilação
        val cx = baseX + totalW / 2f
        val cy = baseY + totalH / 2f

        // ── Sombra projetada (elipse achatada sob os propulsores) ──────
        val shadowW = totalW * 0.6f * breathScale
        val shadowH = pixelSize * 0.6f
        val shadowX = cx
        val shadowY = baseY + totalH + pixelSize * 0.2f
        canvas.save()
        canvas.scale(1f, 0.3f, shadowX, shadowY)
        canvas.drawOval(
            shadowX - shadowW / 2f, shadowY - shadowH / 2f,
            shadowX + shadowW / 2f, shadowY + shadowH / 2f,
            shadowPaint
        )
        canvas.restore()

        // ── Aplica transformação de escala (ventilação) ao robô ──
        canvas.save()
        canvas.scale(breathScale, breathScale, cx, cy)

        // ── Pixels do robô ──────────────────────────────────
        val currPixels = getPixels(currentMood)
        val prevPixels = if (crossfadeFrac < 1f) getPixels(previousMood) else null

        // Mapa de posição → cor anterior (para interpolação)
        val prevColorMap: Map<Pair<Int,Int>, Int>? = prevPixels?.associate { (c, r, color) ->
            Pair(c, r) to color
        }

        for ((col, row, targetColor) in currPixels) {
            if (col < 0 || col >= COLS) continue

            // CrossFade: interpola entre a cor anterior e a atual
            val drawColor = if (prevColorMap != null && crossfadeFrac < 1f) {
                val prevColor = prevColorMap[Pair(col, row)] ?: targetColor
                argbEvaluator.evaluate(crossfadeFrac, prevColor, targetColor) as Int
            } else {
                targetColor
            }

            val left   = baseX + col * pixelSize
            val top    = baseY + row * pixelSize
            val right  = left  + pixelSize - 1.2f
            val bottom = top   + pixelSize - 1.2f

            pixelPaint.color = drawColor
            canvas.drawRoundRect(left, top, right, bottom, 1.5f, 1.5f, pixelPaint)
        }

        // ── Glow nos LEDs (visor) ──────────────────────────────
        drawGlowEyes(canvas, currentMood, pixelSize, baseX, baseY)

        canvas.restore() // fim da escala de ventilação

        // ── Partículas (fora da escala para efeito mais natural) ──
        val dt = 16f // ~60fps
        spawnParticles(pixelSize, baseX, baseY)
        updateParticles(dt)
        for (p in particles) {
            particlePaint.color = p.color
            particlePaint.alpha = (p.alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.size, particlePaint)
        }

        // ── Flash de transição ────────────────────────────────────
        if (flashAlpha > 0f) {
            flashPaint.alpha = (flashAlpha * 255).toInt()
            canvas.drawRoundRect(
                baseX, baseY,
                baseX + totalW, baseY + totalH,
                8f, 8f, flashPaint
            )
        }
    }

    /**
     * Desenha o efeito de glow nos pixels do LED (visor) do robô.
     * Usa RadialGradient centrado em cada fenda para simular luz emitida.
     */
    private fun drawGlowEyes(
        canvas: Canvas, mood: MoodState,
        pixelSize: Float, offsetX: Float, offsetY: Float
    ) {
        val glowColor = when (mood) {
            MoodState.FELIZ  -> glowColorHappy
            MoodState.NORMAL -> glowColorNormal
            MoodState.TRISTE -> glowColorSad
        }

        // Posições dos "olhos/LEDs" conforme o estado — PRESERVADAS do original
        val eyePositions: List<Pair<Float, Float>> = when (mood) {
            MoodState.FELIZ -> listOf(
                Pair(3.5f, 4.5f), Pair(7.5f, 4.5f)  // LED largo
            )
            MoodState.NORMAL -> listOf(
                Pair(3.5f, 4.5f), Pair(7.5f, 4.5f)
            )
            MoodState.TRISTE -> listOf(
                Pair(3f, 4.5f), Pair(7f, 4.5f)       // LED pequeno
            )
        }

        val gr = glowRadius * pixelSize
        for ((col, row) in eyePositions) {
            val ex = offsetX + col * pixelSize
            val ey = offsetY + row * pixelSize

            val radialGrad = RadialGradient(
                ex, ey, gr,
                intArrayOf(
                    Color.argb(glowAlpha, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)),
                    Color.argb(0, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor))
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = radialGrad
            canvas.drawCircle(ex, ey, gr, glowPaint)
        }
        glowPaint.shader = null
    }

    // ────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Inicia animações do estado padrão (NORMAL) ao aparecer no ecrã
        startBounceAnimation(currentMood)
        startBreathAnimation(currentMood)
        startGlowAnimation(currentMood)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bounceAnimator?.cancel()
        breathAnimator?.cancel()
        flashAnimator?.cancel()
        crossfadeAnimator?.cancel()
        glowAnimator?.cancel()
    }
}
