package com.hayirlicumalarsil.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val rotationTurns: Float,
    val wobble: Float,
    val width: Float,
    val height: Float,
    val color: Color,
)

private val ConfettiColors = listOf(
    Color(0xFF6C3DF4),
    Color(0xFFE91E63),
    Color(0xFFFF6D00),
    Color(0xFFFFC107),
    Color(0xFF00C853),
    Color(0xFF00B0FF),
)

/** Silme başarısında ekrana yağan konfeti animasyonu. */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 3500, easing = LinearEasing))
    }
    val particles = remember {
        val random = Random(1907)
        List(120) {
            ConfettiParticle(
                x = random.nextFloat(),
                startY = -random.nextFloat() * 0.6f,
                speed = 1.0f + random.nextFloat() * 0.9f,
                rotationTurns = 1f + random.nextFloat() * 3f,
                wobble = 1f + random.nextFloat() * 3f,
                width = 12f + random.nextFloat() * 14f,
                height = 6f + random.nextFloat() * 10f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
            )
        }
    }

    Canvas(modifier = modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val alpha = (1.2f - t).coerceIn(0f, 1f)
        particles.forEach { p ->
            val y = (p.startY + t * p.speed) * size.height
            val x = (p.x + sin(t * p.wobble * 2f * PI.toFloat()) * 0.04f) * size.width
            if (y in 0f..size.height) {
                rotate(degrees = t * p.rotationTurns * 360f, pivot = Offset(x, y)) {
                    drawRoundRect(
                        color = p.color,
                        topLeft = Offset(x, y),
                        size = Size(p.width, p.height),
                        cornerRadius = CornerRadius(3f, 3f),
                        alpha = alpha,
                    )
                }
            }
        }
    }
}
