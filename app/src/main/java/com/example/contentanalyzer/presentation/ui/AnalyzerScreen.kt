package com.example.contentanalyzer.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.contentanalyzer.presentation.viewmodel.TextAnalysisUiState
import com.example.contentanalyzer.presentation.viewmodel.AnalyzerViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(
    onNavigateToImageAnalysis: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: AnalyzerViewModel = hiltViewModel()
) {
    val inputText by viewModel.inputText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Content Analyzer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    // History Button
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Image Analysis Button
                    IconButton(onClick = onNavigateToImageAnalysis) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Analyze Images",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Feature Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Text Analysis Button (Active)
                    FilterChip(
                        selected = true,
                        onClick = { },
                        label = { Text("📝 Text Analysis") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Image Analysis Button (Navigate)
                    FilterChip(
                        selected = false,
                        onClick = onNavigateToImageAnalysis,
                        label = { Text("🖼️ Image Analysis") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Enter Text to Analyze",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste or type your content here...") },
                        minLines = 6,
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.analyzeText() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState !is TextAnalysisUiState.Loading
                    ) {
                        if (uiState is TextAnalysisUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing...")
                        } else {
                            Text("Analyze Content")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Results Section
            when (val state = uiState) {
                is TextAnalysisUiState.Success -> {
                    ResultCard(result = state.result)
                }
                is TextAnalysisUiState.Error -> {
                    ErrorCard(
                        message = state.message,
                        onRetry = { viewModel.analyzeText() }
                    )
                }
                TextAnalysisUiState.Loading -> {
                    LoadingCard(message = "Analyzing content...")
                }
                TextAnalysisUiState.Idle -> {
                    WelcomeCard(
                        icon = "🔍",
                        title = "Ready to Analyze",
                        description = "Enter text above to detect AI-generated or human-written content"
                    )
                }
            }
        }
    }
}

@Composable
fun ResultCard(result: JSONObject) {
    val label = result.getString("label")
    val aiProb = result.getInt("ai_probability")
    val humanProb = result.getInt("human_probability")
    val confidence = result.getInt("confidence")
    val reasoning = result.getString("reasoning")
    val modelUsed = result.optString("model_used", "unknown")

    val (backgroundColor, textColor) = when {
        label.contains("AI", ignoreCase = true) ->
            Color(0xFFFFE0E0) to Color(0xFFF44336)
        label.contains("Human", ignoreCase = true) ->
            Color(0xFFE0FFE0) to Color(0xFF4CAF50)
        else ->
            Color(0xFFFFF3E0) to Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        label.contains("AI") -> "🤖 $label"
                        label.contains("Human") -> "👤 $label"
                        else -> "⚠️ $label"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = textColor
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = textColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Confidence: $confidence%",
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Probability Bars
            ProbabilityBar("AI Probability", aiProb, Color(0xFFF44336))
            ProbabilityBar("Human Probability", humanProb, Color(0xFF4CAF50))

            Divider()

            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (modelUsed != "unknown") {
                Text(
                    text = "Model: $modelUsed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProbabilityBar(label: String, percentage: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$percentage%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        var animatedProgress by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(percentage) {
            animatedProgress = percentage / 100f
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}
