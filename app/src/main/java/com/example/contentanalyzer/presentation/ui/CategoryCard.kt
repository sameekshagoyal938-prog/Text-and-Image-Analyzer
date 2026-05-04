package com.example.contentanalyzer.presentation.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.contentanalyzer.domain.utils.AIThresholdClassifier
import com.example.contentanalyzer.domain.models.BinaryAnalysisResult
import com.example.contentanalyzer.presentation.ui.components.ScoreBar

@Composable
fun CategoryResultCard(result: BinaryAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(result.categoryColor).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category Header with Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryIcon(result.category)
                    Column {
                        Text(
                            text = result.category.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color(result.categoryColor)
                        )
                        Text(
                            text = "Based on ${result.aiPercentage}% AI probability",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Confidence Badge - fixed: pass Int as it's what result.confidenceScore is
                ConfidenceBadge(confidence = result.confidenceScore.toFloat() / 100f)
            }

            // Progress Bars
            ScoreBar(
                label = "AI Probability",
                percentage = result.aiPercentage,
                color = Color(result.categoryColor)
            )

            ScoreBar(
                label = "Human Probability",
                percentage = result.humanPercentage,
                color = Color(0xFF4CAF50)
            )

            HorizontalDivider()

            // Explanation Message
            Text(
                text = result.categoryMessage,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Recommendation
            if (result.recommendation.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = result.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryIcon(category: AIThresholdClassifier.AICategory) {
    val (icon, color) = when (category) {
        AIThresholdClassifier.AICategory.HIGHLY_LIKELY_AI ->
            Icons.Default.Warning to Color(0xFFF44336)
        AIThresholdClassifier.AICategory.LIKELY_AI ->
            Icons.Default.Android to Color(0xFFFF5722)
        AIThresholdClassifier.AICategory.POSSIBLY_AI ->
            Icons.Default.Smartphone to Color(0xFFFF9800)
        AIThresholdClassifier.AICategory.UNCERTAIN ->
            Icons.Default.Help to Color(0xFF9E9E9E)
        AIThresholdClassifier.AICategory.POSSIBLY_HUMAN ->
            Icons.Default.Person to Color(0xFF4CAF50)
        AIThresholdClassifier.AICategory.LIKELY_HUMAN ->
            Icons.Default.Face to Color(0xFF8BC34A)
        AIThresholdClassifier.AICategory.HIGHLY_LIKELY_HUMAN ->
            Icons.Default.Star to Color(0xFF00BCD4)
        else -> Icons.Default.Help to Color(0xFF9E9E9E)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: Float) {
    val (text, color) = when {
        confidence >= 0.8f -> "High" to Color(0xFF4CAF50)
        confidence >= 0.6f -> "Medium" to Color(0xFFFFC107)
        else -> "Low" to Color(0xFFF44336)
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = "$text Confidence",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
