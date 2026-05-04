package com.example.contentanalyzer.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.contentanalyzer.presentation.viewmodel.AnalyticsData

@Composable
fun AnalyticsCard(analyticsData: AnalyticsData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📈 Analytics Summary",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = analyticsData.totalCount.toString(),
                    label = "Total",
                    color = Color(0xFF2196F3)
                )
                StatItem(
                    value = analyticsData.aiCount.toString(),
                    label = "AI",
                    color = Color(0xFFF44336)
                )
                StatItem(
                    value = analyticsData.realCount.toString(),
                    label = "Real/Human",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    value = analyticsData.uncertainCount.toString(),
                    label = "Uncertain",
                    color = Color(0xFFFF9800)
                )
            }

            HorizontalDivider()

            // Additional Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Avg Confidence",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${analyticsData.averageConfidence.toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "By Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "📝 Text: ${analyticsData.textCount} | 🖼️ Image: ${analyticsData.imageCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Progress Bar for AI vs Real ratio
            if (analyticsData.totalCount > 0) {
                val total = analyticsData.totalCount.toFloat()
                val aiRatio = analyticsData.aiCount / total
                val realRatio = analyticsData.realCount / total
                val uncertainRatio = analyticsData.uncertainCount / total

                Column {
                    Text(
                        "Detection Distribution",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (aiRatio > 0f) {
                            Surface(
                                modifier = Modifier
                                    .weight(aiRatio.coerceAtLeast(0.001f))
                                    .height(8.dp),
                                color = Color(0xFFF44336),
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    bottomStart = 4.dp,
                                    topEnd = if (realRatio <= 0f && uncertainRatio <= 0f) 4.dp else 0.dp,
                                    bottomEnd = if (realRatio <= 0f && uncertainRatio <= 0f) 4.dp else 0.dp
                                )
                            ) {}
                        }
                        if (realRatio > 0f) {
                            Surface(
                                modifier = Modifier
                                    .weight(realRatio.coerceAtLeast(0.001f))
                                    .height(8.dp),
                                color = Color(0xFF4CAF50),
                                shape = RoundedCornerShape(
                                    topStart = if (aiRatio <= 0f) 4.dp else 0.dp,
                                    bottomStart = if (aiRatio <= 0f) 4.dp else 0.dp,
                                    topEnd = if (uncertainRatio <= 0f) 4.dp else 0.dp,
                                    bottomEnd = if (uncertainRatio <= 0f) 4.dp else 0.dp
                                )
                            ) {}
                        }
                        if (uncertainRatio > 0f) {
                            Surface(
                                modifier = Modifier
                                    .weight(uncertainRatio.coerceAtLeast(0.001f))
                                    .height(8.dp),
                                color = Color(0xFFFF9800),
                                shape = RoundedCornerShape(
                                    topStart = if (aiRatio <= 0f && realRatio <= 0f) 4.dp else 0.dp,
                                    bottomStart = if (aiRatio <= 0f && realRatio <= 0f) 4.dp else 0.dp,
                                    topEnd = 4.dp,
                                    bottomEnd = 4.dp
                                )
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}