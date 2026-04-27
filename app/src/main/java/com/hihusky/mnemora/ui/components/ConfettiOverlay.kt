package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.delay

private val confettiColors = listOf(
    Color(0xFFE53935),
    Color(0xFF43A047),
    Color(0xFF1E88E5),
    Color(0xFFFDD835),
    Color(0xFF8E24AA),
    Color(0xFFFF7043),
    Color(0xFF00ACC1),
    Color(0xFFFF4081),
)

private data class Particle(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val size: Float,
    val color: Color,
    val isCircle: Boolean,
)

@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    durationMs: Long = 2500L,
    onFinished: () -> Unit = {}
) {
    val particles = remember {
        val rng = kotlin.random.Random(System.currentTimeMillis())
        List(70) {
            Particle(
                startX = rng.nextFloat(),
                startY = -rng.nextFloat() * 0.15f,
                vx = (rng.nextFloat() - 0.5f) * 0.25f,
                vy = rng.nextFloat() * 0.35f + 0.25f,
                rotation = rng.nextFloat() * 360f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 400f,
                size = rng.nextFloat() * 12f + 6f,
                color = confettiColors[rng.nextInt(confettiColors.size)],
                isCircle = rng.nextBoolean(),
            )
        }
    }

    val startTime = remember { System.currentTimeMillis() }
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (elapsed < durationMs) {
            delay(16)
            elapsed = System.currentTimeMillis() - startTime
        }
        onFinished()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = elapsed / 1000f
        val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
        val fadeAlpha = if (progress > 0.7f) 1f - (progress - 0.7f) / 0.3f else 1f

        particles.forEach { p ->
            val x = (p.startX + p.vx * t) * size.width
            val y = (p.startY + p.vy * t + 0.15f * t * t) * size.height
            if (y > size.height * 1.1f) return@forEach

            val rot = p.rotation + p.rotationSpeed * t
            val alpha = fadeAlpha.coerceIn(0f, 1f)

            withTransform({
                translate(x, y)
                rotate(rot)
            }) {
                if (p.isCircle) {
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size / 2f,
                    )
                } else {
                    drawRect(
                        color = p.color.copy(alpha = alpha),
                        topLeft = Offset(-p.size / 2f, -p.size / 4f),
                        size = Size(p.size, p.size / 2f),
                    )
                }
            }
        }
    }
}
