package com.example.contentanalyzer.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.contentanalyzer.presentation.viewmodel.TextAnalysisUiState
import com.example.contentanalyzer.presentation.viewmodel.AnalyzerViewModel

@Composable
fun ResponseDebugScreen(
    viewModel: AnalyzerViewModel = hiltViewModel()
) {
    val inputText by viewModel.inputText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "API Response Debug",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = { viewModel.updateInputText(it) },
            label = { Text("Test Text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.analyzeText() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test API Response")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display response details
        when (val state = uiState) {
            is TextAnalysisUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("✅ API Response Parsed Successfully", style = MaterialTheme.typography.titleMedium)
                        }
                        item {
                            Text("📊 Results:", style = MaterialTheme.typography.titleSmall)
                            Text("  • Label: ${state.result.optString("label")}")
                            Text("  • AI: ${state.result.optInt("ai_probability")}%")
                            Text("  • Human: ${state.result.optInt("human_probability")}%")
                            Text("  • Confidence: ${state.result.optInt("confidence")}%")
                            Text("  • Model: ${state.result.optString("model_used")}")
                        }
                        item {
                            Text("📝 Reasoning:", style = MaterialTheme.typography.titleSmall)
                            Text(state.result.optString("reasoning").take(1000))
                        }
                    }
                }
            }
            is TextAnalysisUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("❌ Error", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(state.message)
                    }
                }
            }
            TextAnalysisUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            TextAnalysisUiState.Idle -> {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text("Enter text and click 'Test API Response'")
                    }
                }
            }
        }
    }
}
