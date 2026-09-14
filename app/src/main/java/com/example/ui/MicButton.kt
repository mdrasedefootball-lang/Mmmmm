package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantState

@Composable
fun MicButton(
    state: AssistantState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_scale"
    )

    val (buttonGradient, borderColor, icon, statusText, subText) = when (state) {
        AssistantState.DISCONNECTED -> Quintuple(
            listOf(Color(0xFF1E1438), Color(0xFF120C24)),
            Color(0xFF8A2BE2).copy(alpha = 0.6f),
            Icons.Default.Mic,
            "Tap to talk",
            "Say 'Anisa, YouTube open koro'"
        )
        AssistantState.CONNECTING -> Quintuple(
            listOf(Color(0xFF0F2B48), Color(0xFF0B1B30)),
            Color(0xFF00E5FF),
            Icons.Default.Sync,
            "Connecting...",
            "Establishing live stream"
        )
        AssistantState.LISTENING -> Quintuple(
            listOf(Color(0xFF003852), Color(0xFF001F33)),
            Color(0xFF00E5FF),
            Icons.Default.GraphicEq,
            "Listening...",
            "Tap to stop"
        )
        AssistantState.SPEAKING -> Quintuple(
            listOf(Color(0xFF4A0E38), Color(0xFF2B0720)),
            Color(0xFFFF007F),
            Icons.Default.Stop,
            "Speaking...",
            "Tap to interrupt"
        )
        AssistantState.ERROR -> Quintuple(
            listOf(Color(0xFF3B1215), Color(0xFF240A0C)),
            Color(0xFFFF5252),
            Icons.Default.MicOff,
            "Tap to retry",
            "Connection error"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(86.dp)
                .then(
                    if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                        Modifier.scale(pulseScale)
                    } else Modifier
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(buttonGradient)
                )
                .border(2.dp, borderColor, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = borderColor),
                    onClick = onClick
                )
                .testTag("mic_button")
        ) {
            Icon(
                imageVector = icon,
                contentDescription = statusText,
                tint = if (state == AssistantState.DISCONNECTED) Color.White.copy(alpha = 0.85f) else Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = Pair(statusText, subText),
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "mic_labels"
        ) { labels ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = labels.first,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = labels.second,
                    color = Color(0xFF9E9EB8),
                    fontSize = 12.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
