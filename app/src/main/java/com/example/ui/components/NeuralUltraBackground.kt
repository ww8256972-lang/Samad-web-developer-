package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Futuristic "Neural Ultra" Background Animation:
 * Features slow-moving glowing server cables, interconnected fiber nodes, and subtle cyber
 * pulses in deep black, electric blue (#00E5FF), and neon pink (#FF2A85).
 * Designed for ultra battery efficiency and non-distracting subtle motion behind cards.
 */
@Composable
fun NeuralUltraBackground(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neural_ultra_flow")

    // Slow organic drift phase
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Slow cable pulse motion
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Base background colors
    val bgStart = if (isDark) Color(0xFF070B14) else Color(0xFFF3F6FC)
    val bgEnd = if (isDark) Color(0xFF0A0F1D) else Color(0xFFE7ECF5)

    // Cable & glow colors
    val blueCable = if (isDark) Color(0x3300D2FF) else Color(0x220088FF)
    val pinkCable = if (isDark) Color(0x33FF2A85) else Color(0x22D81B60)
    val deepBlueCable = if (isDark) Color(0x250A84FF) else Color(0x181565C0)
    val serverNodeColor = if (isDark) Color(0x5500F0FF) else Color(0x330288D1)
    val pinkNodeColor = if (isDark) Color(0x55FF4081) else Color(0x33C2185B)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Base gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(bgStart, bgEnd)
            )
        )

        // 2. Server Cable 1 - Electric Blue Flow (Cubic Bezier sweeping across)
        val cablePath1 = Path().apply {
            val startY = height * 0.15f + sin(phase) * 40f
            moveTo(-50f, startY)
            val cp1X = width * 0.35f
            val cp1Y = height * 0.28f + cos(phase) * 60f
            val cp2X = width * 0.65f
            val cp2Y = height * 0.08f + sin(phase + 1f) * 50f
            val endY = height * 0.35f + cos(phase * 0.8f) * 40f
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, width + 50f, endY)
        }
        drawPath(
            path = cablePath1,
            color = blueCable,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // 3. Server Cable 2 - Neon Pink Flow
        val cablePath2 = Path().apply {
            val startY = height * 0.55f + cos(phase) * 45f
            moveTo(-50f, startY)
            val cp1X = width * 0.30f
            val cp1Y = height * 0.42f + sin(phase * 0.9f) * 55f
            val cp2X = width * 0.75f
            val cp2Y = height * 0.68f + cos(phase + 2f) * 60f
            val endY = height * 0.50f + sin(phase * 0.7f) * 35f
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, width + 50f, endY)
        }
        drawPath(
            path = cablePath2,
            color = pinkCable,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )

        // 4. Server Cable 3 - Deep Server Bus Line
        val cablePath3 = Path().apply {
            val startY = height * 0.82f + sin(phase * 1.1f) * 35f
            moveTo(-50f, startY)
            val cp1X = width * 0.45f
            val cp1Y = height * 0.72f + cos(phase * 0.8f) * 45f
            val cp2X = width * 0.80f
            val cp2Y = height * 0.92f + sin(phase + 1.5f) * 40f
            val endY = height * 0.78f + cos(phase) * 30f
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, width + 50f, endY)
        }
        drawPath(
            path = cablePath3,
            color = deepBlueCable,
            style = Stroke(width = 4.0f, cap = StrokeCap.Round)
        )

        // 5. Cross-Connecting Server Bus Connectors (Vertical subtle links)
        val connectorX1 = width * 0.25f + sin(phase * 0.5f) * 20f
        val connectorX2 = width * 0.60f + cos(phase * 0.6f) * 25f
        val connectorX3 = width * 0.82f + sin(phase * 0.4f) * 15f

        drawLine(
            color = if (isDark) Color(0x1800E5FF) else Color(0x100088FF),
            start = Offset(connectorX1, height * 0.18f),
            end = Offset(connectorX1, height * 0.52f),
            strokeWidth = 2f
        )
        drawLine(
            color = if (isDark) Color(0x18FF2A85) else Color(0x10D81B60),
            start = Offset(connectorX2, height * 0.48f),
            end = Offset(connectorX2, height * 0.82f),
            strokeWidth = 2f
        )

        // 6. Slow traveling data packet / node along cables
        val packet1X = (width * ((pulseProgress + 0.15f) % 1f))
        val packet1Y = height * 0.22f + sin(phase + packet1X / width * 3f) * 40f
        drawCircle(
            color = serverNodeColor,
            radius = 5.5f,
            center = Offset(packet1X, packet1Y)
        )
        drawCircle(
            color = serverNodeColor.copy(alpha = 0.25f),
            radius = 14f,
            center = Offset(packet1X, packet1Y)
        )

        val packet2X = (width * (1f - ((pulseProgress + 0.45f) % 1f)))
        val packet2Y = height * 0.58f + cos(phase + packet2X / width * 2.5f) * 45f
        drawCircle(
            color = pinkNodeColor,
            radius = 6.0f,
            center = Offset(packet2X, packet2Y)
        )
        drawCircle(
            color = pinkNodeColor.copy(alpha = 0.22f),
            radius = 16f,
            center = Offset(packet2X, packet2Y)
        )

        // 7. Fixed network junction nodes
        val junctions = listOf(
            Offset(width * 0.25f, height * 0.24f) to serverNodeColor,
            Offset(width * 0.65f, height * 0.14f) to serverNodeColor,
            Offset(width * 0.30f, height * 0.50f) to pinkNodeColor,
            Offset(width * 0.72f, height * 0.60f) to pinkNodeColor,
            Offset(width * 0.48f, height * 0.80f) to serverNodeColor,
            Offset(width * 0.85f, height * 0.86f) to pinkNodeColor
        )

        for ((center, nodeCol) in junctions) {
            val pulsedR = 3.5f + sin(phase * 2f + center.x) * 1.2f
            drawCircle(
                color = nodeCol,
                radius = pulsedR,
                center = center
            )
        }
    }
}
