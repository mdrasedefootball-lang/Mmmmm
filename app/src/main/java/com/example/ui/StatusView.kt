package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantState

@Composable
fun StatusView(
    state: AssistantState,
    errorMessage: String?,
    lastActionMessage: String?,
    selectedLanguage: String,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor) = when (state) {
        AssistantState.DISCONNECTED -> Pair("Anisa is idle", Color(0xFF9E9EB8))
        AssistantState.CONNECTING -> Pair("Connecting to Gemini Live...", Color(0xFF00E5FF))
        AssistantState.LISTENING -> Pair("Listening to you...", Color(0xFF00E5FF))
        AssistantState.SPEAKING -> Pair("Anisa is speaking...", Color(0xFFFF007F))
        AssistantState.ERROR -> Pair("Error occurred", Color(0xFFFF5252))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag("status_view")
    ) {
        // Status indicator pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14142B).copy(alpha = 0.8f))
                .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }

        // Action Feedback banner (e.g. "YouTube opened successfully")
        AnimatedVisibility(
            visible = !lastActionMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A2B1D).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = lastActionMessage.orEmpty(),
                    color = Color(0xFFB9F6CA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Error message banner
        AnimatedVisibility(
            visible = !errorMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF330E14).copy(alpha = 0.9f))
                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = Color(0xFFFFCDD2),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
