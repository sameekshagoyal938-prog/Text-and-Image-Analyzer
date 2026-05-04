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
import com.example.contentanalyzer.domain.utils.TextInputValidator
import com.example.contentanalyzer.ui.theme.warning
import com.example.contentanalyzer.ui.theme.warningContainer

@Composable
fun InputValidationCard(
    validationResult: TextInputValidator.ValidationResult?,
    modifier: Modifier = Modifier
) {
    validationResult?.let { validation ->
        AnimatedVisibility(
            visible = !validation.isValid || validation.needsMoreText,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (validation.status) {
                        TextInputValidator.ValidationStatus.EMPTY ->
                            MaterialTheme.colorScheme.errorContainer
                        TextInputValidator.ValidationStatus.TOO_SHORT_ABSOLUTE,
                        TextInputValidator.ValidationStatus.TOO_SHORT_LOW_CONFIDENCE ->
                            MaterialTheme.colorScheme.errorContainer
                        TextInputValidator.ValidationStatus.TOO_SHORT_ACCEPTABLE ->
                            MaterialTheme.colorScheme.warningContainer
                        else ->
                            MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ValidationIcon(validation.status)
                        Text(
                            text = getStatusTitle(validation.status),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    // Message
                    Text(
                        text = validation.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Word count indicator
                    WordCountIndicator(
                        wordCount = validation.wordCount,
                        status = validation.status
                    )

                    // Recommendation
                    if (validation.recommendation.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = validation.recommendation,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationIcon(status: TextInputValidator.ValidationStatus) {
    val (icon, color) = when (status) {
        TextInputValidator.ValidationStatus.EMPTY ->
            Icons.Default.Warning to MaterialTheme.colorScheme.error
        TextInputValidator.ValidationStatus.TOO_SHORT_ABSOLUTE,
        TextInputValidator.ValidationStatus.TOO_SHORT_LOW_CONFIDENCE ->
            Icons.Default.Error to MaterialTheme.colorScheme.error
        TextInputValidator.ValidationStatus.TOO_SHORT_ACCEPTABLE ->
            Icons.Default.Warning to MaterialTheme.colorScheme.warning
        else ->
            Icons.Default.Info to MaterialTheme.colorScheme.secondary
    }

    Icon(
        icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
fun WordCountIndicator(wordCount: Int, status: TextInputValidator.ValidationStatus) {
    val (color, progress) = when {
        wordCount >= 200 -> Color(0xFF4CAF50) to 1.0f
        wordCount >= 100 -> Color(0xFF8BC34A) to 0.8f
        wordCount >= 50 -> Color(0xFFFFC107) to 0.6f
        wordCount >= 20 -> Color(0xFFFF9800) to 0.4f
        else -> Color(0xFFF44336) to 0.2f
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Text Length: $wordCount words",
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Text(
                text = "${(progress * 100).toInt()}% of optimal",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = getTargetMessage(wordCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getStatusTitle(status: TextInputValidator.ValidationStatus): String {
    return when (status) {
        TextInputValidator.ValidationStatus.EMPTY -> "No Text Entered"
        TextInputValidator.ValidationStatus.TOO_SHORT_ABSOLUTE -> "Text Too Short"
        TextInputValidator.ValidationStatus.TOO_SHORT_LOW_CONFIDENCE -> "Insufficient Length"
        TextInputValidator.ValidationStatus.TOO_SHORT_ACCEPTABLE -> "Minimum Length Not Met"
        TextInputValidator.ValidationStatus.MINIMUM_MET -> "Minimum Requirements Met"
        TextInputValidator.ValidationStatus.RECOMMENDED -> "Good Text Length"
        TextInputValidator.ValidationStatus.OPTIMAL -> "Optimal Text Length"
    }
}

private fun getTargetMessage(wordCount: Int): String {
    return when {
        wordCount < 20 -> "Add ${20 - wordCount} more words for basic analysis"
        wordCount < 50 -> "Add ${50 - wordCount} more words for acceptable results"
        wordCount < 100 -> "Add ${100 - wordCount} more words for good accuracy"
        wordCount < 200 -> "Add ${200 - wordCount} more words for optimal accuracy"
        else -> "Excellent! Text length is optimal for analysis"
    }
}
