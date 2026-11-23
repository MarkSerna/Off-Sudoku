package com.logicminigames.core.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.logicminigames.core.ui.theme.LocalBoardColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val xFraction: Float,
    val startYFraction: Float,
    val vx: Float,
    val vy: Float,
    val sizeDp: Float,
    val initialAngle: Float,
    val spinSpeed: Float,
    val colorIndex: Int,
)

/**
 * Discreet, battery-friendly celebratory confetti burst rendered on Canvas for ~3.4 seconds.
 */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    colors: List<Color> = LocalBoardColors.current.confettiColors,
) {
    val context = LocalContext.current
    val animatorScale = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
    }
    if (animatorScale <= 0f || colors.isEmpty()) return

    val particles = remember {
        val rng = Random(2026)
        List(44) { idx ->
            val angle = rng.nextFloat() * PI.toFloat() * 2f
            val speed = 0.15f + rng.nextFloat() * 0.35f
            ConfettiParticle(
                xFraction = 0.15f + rng.nextFloat() * 0.7f,
                startYFraction = -0.05f - rng.nextFloat() * 0.25f,
                vx = cos(angle) * speed * 0.35f,
                vy = 0.55f + rng.nextFloat() * 0.65f + sin(angle) * 0.08f,
                sizeDp = 5f + rng.nextFloat() * 5f,
                initialAngle = rng.nextFloat() * 360f,
                spinSpeed = (rng.nextFloat() - 0.5f) * 520f,
                colorIndex = idx,
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3400, easing = LinearEasing),
        )
    }

    val t = progress.value
    if (t >= 1f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = when {
            t < 0.75f -> 0.9f
            else -> ((1f - t) / 0.25f).coerceIn(0f, 0.9f)
        }
        val width = size.width
        val height = size.height

        for (p in particles) {
            val px = (p.xFraction + p.vx * t) * width
            val py = (p.startYFraction + p.vy * t + 0.35f * t * t) * height
            if (py > height + 32f) continue

            val side = p.sizeDp.dp.toPx()
            val color = colors[p.colorIndex % colors.size].copy(alpha = alpha)
            val rotation = p.initialAngle + p.spinSpeed * t
            rotate(degrees = rotation, pivot = Offset(px, py)) {
                drawRect(
                    color = color,
                    topLeft = Offset(px - side / 2f, py - side * 0.35f),
                    size = Size(side, side * 0.65f),
                )
            }
        }
    }
}
