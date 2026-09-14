package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.assistant.AssistantState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnisaOrb(
    state: AssistantState,
    audioAmplitude: Float,
    intensity: Float = 1.0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_anim")

    // Slow organic breathing
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Fast rotation for connecting and speaking rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    // Secondary reverse rotation
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing)
        ),
        label = "reverse_rotation"
    )

    // Pulsing outer aura
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura"
    )

    // Color palette based on Assistant state
    val (coreColors, ringColor, glowColor) = when (state) {
        AssistantState.DISCONNECTED -> Triple(
            listOf(Color(0xFF2A1B4E), Color(0xFF130E26), Color(0xFF0A0714)),
            Color(0xFF8A2BE2).copy(alpha = 0.4f),
            Color(0xFF6B11A8).copy(alpha = 0.25f)
        )
        AssistantState.CONNECTING -> Triple(
            listOf(Color(0xFF00E5FF), Color(0xFF7B1FA2), Color(0xFF0E0B1F)),
            Color(0xFF00E5FF),
            Color(0xFF00E5FF).copy(alpha = 0.5f)
        )
        AssistantState.LISTENING -> Triple(
            listOf(Color(0xFF00E5FF), Color(0xFF3D5AFE), Color(0xFF081226)),
            Color(0xFF00E5FF),
            Color(0xFF00B0FF).copy(alpha = 0.6f)
        )
        AssistantState.SPEAKING -> Triple(
            listOf(Color(0xFFFF007F), Color(0xFF9C27B0), Color(0xFF3F51B5)),
            Color(0xFFFF4081),
            Color(0xFFFF007F).copy(alpha = 0.7f)
        )
        AssistantState.ERROR -> Triple(
            listOf(Color(0xFFFF5252), Color(0xFFC62828), Color(0xFF310A0A)),
            Color(0xFFFF1744),
            Color(0xFFFF5252).copy(alpha = 0.5f)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(260.dp)
            .testTag("anisa_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(260.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.55f

            // Dynamic expansion based on audio amplitude & state
            val dynamicBoost = when (state) {
                AssistantState.LISTENING -> audioAmplitude * 40f * intensity
                AssistantState.SPEAKING -> audioAmplitude * 55f * intensity
                AssistantState.CONNECTING -> 8f
                else -> 0f
            }

            val currentRadius = (baseRadius * breathScale) + dynamicBoost

            // 1. Outermost Diffuse Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = currentRadius * 1.6f
                ),
                radius = currentRadius * 1.6f,
                center = center
            )

            // 2. Multi-Layer Rotating Orbit Rings
            if (state == AssistantState.CONNECTING || state == AssistantState.SPEAKING || state == AssistantState.LISTENING) {
                rotate(rotationAngle, center) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.45f),
                        radius = currentRadius * 1.22f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Orbital nodes on the ring
                    val nodeCount = if (state == AssistantState.CONNECTING) 4 else 3
                    for (i in 0 until nodeCount) {
                        val angle = (i * (360f / nodeCount)) * (Math.PI / 180.0)
                        val nx = center.x + (currentRadius * 1.22f) * cos(angle).toFloat()
                        val ny = center.y + (currentRadius * 1.22f) * sin(angle).toFloat()
                        drawCircle(
                            color = ringColor,
                            radius = 4.dp.toPx(),
                            center = Offset(nx, ny)
                        )
                    }
                }

                rotate(reverseRotation, center) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.25f),
                        radius = currentRadius * 1.38f,
                        center = center,
                        style = Stroke(
                            width = 1.5.dp.toPx()
                        )
                    )
                }
            }

            // 3. Audio Responsive Ripple Aura
            if (dynamicBoost > 3f) {
                drawCircle(
                    color = ringColor.copy(alpha = (auraPulse * (dynamicBoost / 30f)).coerceIn(0.1f, 0.45f)),
                    radius = currentRadius * 1.15f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // 4. Core Holographic Gradient Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = center,
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // 5. Specular Inner Highlight
            val highlightOffset = Offset(center.x - currentRadius * 0.32f, center.y - currentRadius * 0.32f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = highlightOffset,
                    radius = currentRadius * 0.5f
                ),
                radius = currentRadius * 0.5f,
                center = highlightOffset
            )
        }
    }
}
