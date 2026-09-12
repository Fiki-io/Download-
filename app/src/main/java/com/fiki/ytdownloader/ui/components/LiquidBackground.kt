package com.fiki.ytdownloader.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.fiki.ytdownloader.ui.theme.GlassBackground
import com.fiki.ytdownloader.ui.theme.NeonBlue
import com.fiki.ytdownloader.ui.theme.NeonCyan
import com.fiki.ytdownloader.ui.theme.NeonEmerald
import com.fiki.ytdownloader.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidAnimation")

    val floatAnim1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Orb1Motion"
    )

    val floatAnim2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Orb2Motion"
    )

    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GlassBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1: Neon Cyan (Floating top-right to center)
            val orb1X = width * 0.72f + cos(floatAnim1) * (width * 0.16f)
            val orb1Y = height * 0.22f + sin(floatAnim1) * (height * 0.10f)
            val orb1Radius = (width * 0.55f) * pulseAnim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.28f),
                        NeonCyan.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(orb1X, orb1Y)
            )

            // Orb 2: Neon Purple (Floating center-left to lower-mid)
            val orb2X = width * 0.25f + sin(floatAnim2) * (width * 0.14f)
            val orb2Y = height * 0.52f + cos(floatAnim2) * (height * 0.12f)
            val orb2Radius = (width * 0.65f) * (2f - pulseAnim)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.24f),
                        NeonPurple.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(orb2X, orb2Y)
            )

            // Orb 3: Neon Emerald (Breathing bottom-right to bottom-center)
            val orb3X = width * 0.68f + cos(floatAnim2 * 0.8f) * (width * 0.15f)
            val orb3Y = height * 0.82f + sin(floatAnim1 * 0.8f) * (height * 0.08f)
            val orb3Radius = (width * 0.50f) * pulseAnim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonEmerald.copy(alpha = 0.22f),
                        NeonEmerald.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = orb3Radius
                ),
                radius = orb3Radius,
                center = Offset(orb3X, orb3Y)
            )

            // Orb 4: Subtle Deep Neon Blue (Top-left accent)
            val orb4X = width * 0.15f
            val orb4Y = height * 0.08f
            val orb4Radius = width * 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonBlue.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(orb4X, orb4Y),
                    radius = orb4Radius
                ),
                radius = orb4Radius,
                center = Offset(orb4X, orb4Y)
            )
        }

        content()
    }
}
