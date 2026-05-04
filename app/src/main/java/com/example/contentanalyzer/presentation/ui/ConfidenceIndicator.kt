package com.example.contentanalyzer.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.contentanalyzer.domain.utils.ConfidenceCalculator

@Composable
fun ConfidenceIndicator(
    confidence: Float,
    confidenceLevel: ConfidenceCalculator.ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val (color, icon, message) = getConfidenceAttributes(confidenceLevel)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Confidence Score",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Animated progress bar
                var animatedProgress by remember { mutableStateOf(0f) }
                LaunchedEffect(confidence) {
                    animatedProgress = 0f
                    animate(
                        initialValue = 0f,
                        targetValue = confidence,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        )
                    ) { value, _ ->
                        animatedProgress = value
                    }
                }

                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${(confidence * 100).toInt()}% - $message",
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Animated confidence icon
            AnimatedIcon(
                icon = icon,
                color = color,
                confidence = confidence
            )
        }
    }
}

@Composable
fun AnimatedIcon(icon: String, color: Color, confidence: Float) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (confidence < 0.5f) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.2f),
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(if (confidence < 0.5f) Modifier.rotate(rotation) else Modifier)
        ) {
            when (icon) {
                "VERY_HIGH" -> Text("✓✓", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                "HIGH" -> Text("✓", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                "MEDIUM" -> Text("~", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                "LOW" -> Text("?", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                else -> Text("⚠️", fontSize = MaterialTheme.typography.headlineMedium.fontSize)
            }
        }
    }
}

private fun getConfidenceAttributes(level: ConfidenceCalculator.ConfidenceLevel): Triple<Color, String, String> {
    return when (level) {
        ConfidenceCalculator.ConfidenceLevel.VERY_HIGH -> Triple(
            Color(0xFF2E7D32),  // Dark Green
            "VERY_HIGH",
            "Very High Confidence"
        )
        ConfidenceCalculator.ConfidenceLevel.HIGH -> Triple(
            Color(0xFF4CAF50),  // Green
            "HIGH",
            "High Confidence"
        )
        ConfidenceCalculator.ConfidenceLevel.MEDIUM -> Triple(
            Color(0xFFFFC107),  // Amber
            "MEDIUM",
            "Moderate Confidence"
        )
        ConfidenceCalculator.ConfidenceLevel.LOW -> Triple(
            Color(0xFFFF9800),  // Orange
            "LOW",
            "Low Confidence"
        )
        ConfidenceCalculator.ConfidenceLevel.VERY_LOW -> Triple(
            Color(0xFFF44336),  // Red
            "VERY_LOW",
            "Very Low Confidence"
        )
    }
}
