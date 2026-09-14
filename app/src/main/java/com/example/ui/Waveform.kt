package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.assistant.AssistantState
import kotlin.math.sin

@Composable
fun Waveform(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val animatedAmp by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 80),
        label = "waveform_amp"
    )

    val barColors = when (state) {
        AssistantState.SPEAKING -> listOf(
            Color(0xFFFF007F),
            Color(0xFFBA68C8),
            Color(0xFF7C4DFF)
        )
        AssistantState.LISTENING -> listOf(
            Color(0xFF00E5FF),
            Color(0xFF2979FF),
            Color(0xFF00E5FF)
        )
        AssistantState.CONNECTING -> listOf(
            Color(0xFF651FFF),
            Color(0xFF00E5FF)
        )
        AssistantState.ERROR -> listOf(
            Color(0xFFFF5252),
            Color(0xFFFF1744)
        )
        else -> listOf(
            Color(0xFF4A3E6D).copy(alpha = 0.4f),
            Color(0xFF281E45).copy(alpha = 0.4f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("waveform_view")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val barCount = 32
            val spacing = size.width / (barCount + 1)
            val barWidth = (spacing * 0.48f).coerceAtLeast(3.dp.toPx())
            val centerY = size.height / 2f
            val maxBarHeight = size.height * 0.9f
            val minBarHeight = 4.dp.toPx()

            val gradientBrush = Brush.verticalGradient(
                colors = barColors,
                startY = 0f,
                endY = size.height
            )

            for (i in 0 until barCount) {
                val x = (i + 1) * spacing
                // Symmetrical wave curve
                val normalizedIdx = (i - (barCount / 2f)) / (barCount / 2f)
                val curveEnvelope = 1f - (normalizedIdx * normalizedIdx).coerceIn(0f, 1f)

                val effectiveAmp = when (state) {
                    AssistantState.LISTENING, AssistantState.SPEAKING -> animatedAmp
                    AssistantState.CONNECTING -> 0.25f * (0.5f + 0.5f * sin((i * 0.35f).toDouble()).toFloat())
                    else -> 0.05f
                }

                // Dynamic height calculation
                val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * effectiveAmp * curveEnvelope
                val top = centerY - (barHeight / 2f)

                drawRoundRect(
                    brush = gradientBrush,
                    topLeft = Offset(x - barWidth / 2f, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
