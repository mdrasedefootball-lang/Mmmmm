package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantState
import com.example.assistant.AssistantViewModel
import com.example.permissions.PermissionManager

@Composable
fun AnisaScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.assistantState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lastActionMessage by viewModel.lastActionMessage.collectAsState()
    val micAmplitude by viewModel.micAmplitude.collectAsState()
    val aiAmplitude by viewModel.aiAmplitude.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val animationIntensity by viewModel.animationIntensity.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var hasMicPermission by remember {
        mutableStateOf(PermissionManager.hasRecordAudioPermission(context))
    }
    var showPermissionBanner by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        showPermissionBanner = !granted
        if (granted) {
            viewModel.startAssistant()
        }
    }

    // Background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF070810),
            Color(0xFF0C0E1C),
            Color(0xFF070912)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("anisa_screen")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP BAR: App Title & Settings Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Column {
                    Text(
                        text = "ANISA AI",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "PERSONAL VOICE ASSISTANT",
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }

                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16192E).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFF2E3354), CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // CENTER: Interactive AI Orb
            val activeAmplitude = when (state) {
                AssistantState.LISTENING -> micAmplitude
                AssistantState.SPEAKING -> aiAmplitude
                else -> 0f
            }

            AnisaOrb(
                state = state,
                audioAmplitude = activeAmplitude,
                intensity = animationIntensity,
                onClick = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(PermissionManager.RECORD_AUDIO)
                    } else {
                        viewModel.toggleAssistant()
                    }
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Waveform reactive visualizer
            Waveform(
                state = state,
                amplitude = activeAmplitude,
                modifier = Modifier.padding(horizontal = 36.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Status and action result messages
            StatusView(
                state = state,
                errorMessage = errorMessage,
                lastActionMessage = lastActionMessage,
                selectedLanguage = selectedLanguage
            )

            Spacer(modifier = Modifier.weight(0.2f))

            // BOTTOM: Large Circular Microphone Button
            MicButton(
                state = state,
                onClick = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(PermissionManager.RECORD_AUDIO)
                    } else {
                        viewModel.toggleAssistant()
                    }
                },
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }

        // Permission Banner Overlay if denied
        AnimatedVisibility(
            visible = showPermissionBanner,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF131526))
                    .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = PermissionManager.PERMISSION_DENIED_MESSAGE_BANGLA,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(PermissionManager.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("request_permission_button")
                    ) {
                        Text(
                            text = "Permission দিন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Settings Sheet
        if (showSettings) {
            SettingsSheet(
                selectedLanguage = selectedLanguage,
                animationIntensity = animationIntensity,
                onLanguageSelected = { lang ->
                    viewModel.setLanguage(lang)
                },
                onIntensityChanged = { intensity ->
                    viewModel.setAnimationIntensity(intensity)
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}
