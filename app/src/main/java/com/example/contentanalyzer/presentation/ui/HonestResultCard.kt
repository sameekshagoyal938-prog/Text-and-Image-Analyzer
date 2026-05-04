package com.example.contentanalyzer.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.contentanalyzer.domain.models.BinaryAnalysisResult
import com.example.contentanalyzer.domain.utils.AIThresholdClassifier
import com.example.contentanalyzer.presentation.ui.components.ScoreBar

@Composable
fun HonestResultCard(result: BinaryAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Analysis Results",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Senior Logic: Don't show misleading 50/50 bars
            if (result.category == AIThresholdClassifier.AICategory.UNCERTAIN) {
                UncertaintyView()
            } else {
                ScoreBar(
                    label = "🤖 AI-Generated",
                    percentage = result.aiPercentage,
                    color = MaterialTheme.colorScheme.primary
                )

                ScoreBar(
                    label = "👤 Human-Written",
                    percentage = result.humanPercentage,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Confidence indicator
            ConfidenceIndicator(confidence = result.confidenceScore.toFloat() / 100f)

            HorizontalDivider()

            // Honest explanation
            Text(
                text = result.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Important disclaimer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "⚠️ Important: This model only detects AI vs Human text. " +
                            "It cannot detect 'fakeness' or manipulation without specialized training.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun UncertaintyView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🤔 Result: Uncertain",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.outline,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
        Text(
            "The model cannot clearly distinguish the origin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConfidenceIndicator(confidence: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Model Confidence", style = MaterialTheme.typography.bodySmall)
        Text(
            "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
    LinearProgressIndicator(
        progress = { confidence },
        modifier = Modifier.fillMaxWidth(),
        color = when {
            confidence > 0.7f -> MaterialTheme.colorScheme.primary
            confidence > 0.5f -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        },
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
