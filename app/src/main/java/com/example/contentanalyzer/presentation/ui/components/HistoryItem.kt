package com.example.contentanalyzer.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.contentanalyzer.domain.models.AnalysisHistory

@Composable
fun HistoryItem(
    history: AnalysisHistory,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, icon) = when {
        history.result.contains("AI") ->
            Triple(Color(0xFFFFE0E0), Color(0xFFF44336), "🤖")
        history.result.contains("Real") || history.result.contains("Human") ->
            Triple(Color(0xFFE0FFE0), Color(0xFF4CAF50), "📸")
        else ->
            Triple(Color(0xFFFFF3E0), Color(0xFFFF9800), "⚠️")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon and Content
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = MaterialTheme.typography.titleLarge.fontSize)

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Result and Confidence
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            history.result,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = textColor
                        )
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = textColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "${history.confidence}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Preview
                    Text(
                        history.inputPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    // Metadata
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            history.formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (history.inputType == "Image" && history.metadataFound) {
                            Text(
                                "📷 Metadata",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            // Delete Button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}