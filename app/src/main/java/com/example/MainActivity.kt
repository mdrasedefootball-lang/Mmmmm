package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.assistant.AssistantViewModel
import com.example.ui.AnisaScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AssistantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF070810)
                ) {
                    AnisaScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // When activity goes to background, gracefully pause speaking or session if active
        if (viewModel.assistantState.value == com.example.assistant.AssistantState.SPEAKING) {
            viewModel.interruptAssistant()
        }
    }
}
