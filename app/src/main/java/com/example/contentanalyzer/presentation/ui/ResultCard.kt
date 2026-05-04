package com.example.contentanalyzer.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contentanalyzer.domain.models.AnalysisResult

@Composable
fun ResultCard(result: AnalysisResult) {
    val isHuman = result.classification.contains("HUMAN")
    val isAI = result.classification.contains("AI")

    val backgroundColor = when {
        isHuman -> Color(0xFFE8F5E9)
        isAI -> Color(0xFFFFEBEE)
        else -> Color(0xFFFFF8E1)
    }

    val accentColor = when {
        isHuman -> Color(0xFF4CAF50)
        isAI -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        isHuman -> "✅ HUMAN GENERATED"
                        isAI -> "🤖 AI GENERATED"
                        else -> "❓ INCONCLUSIVE"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = accentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${result.confidence}% confident",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = accentColor,
                        fontSize = 12.sp
                    )
                }
            }

            // Confidence Bar
            LinearProgressIndicator(
                progress = { result.confidence / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f)
            )

            HorizontalDivider(thickness = 0.5.dp, color = accentColor.copy(alpha = 0.3f))

            // Details
            Text(
                text = result.details,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color.Black
            )

            HorizontalDivider(thickness = 0.5.dp, color = accentColor.copy(alpha = 0.3f))

            // Footer
            Text(
                text = "Analysis Type: ${result.type.name}",
                fontSize = 10.sp,
                color = Color.DarkGray
            )
        }
    }
}
