package com.example.contentanalyzer.presentation.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contentanalyzer.data.api.models.AIRequest
import com.example.contentanalyzer.data.api.models.HuggingFacePrediction
import com.example.contentanalyzer.data.api.models.ZeroShotRequest
import com.example.contentanalyzer.data.repository.AnalyzerRepository
import com.example.contentanalyzer.domain.utils.LabelMapper
import com.example.contentanalyzer.presentation.viewmodel.AnalyzerViewModel
import kotlinx.coroutines.launch

@Composable
fun LabelMappingDebugScreen(
    viewModel: AnalyzerViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    var debugInfo by remember { mutableStateOf("Click 'Test Mapping' to verify labels") }
    var isLoading by remember { mutableStateOf(false) }
    val repository = remember { AnalyzerRepository() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Label Mapping Debug Tool",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Test with Known Texts",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            debugInfo = testLabelMapping(repository)
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Text("Run Label Mapping Test")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Debug Information:",
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = debugInfo,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }

        // Manual test inputs
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Manual Label Verification",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Enter a label to see how it would be mapped:",
                    style = MaterialTheme.typography.bodySmall
                )

                var testLabel by remember { mutableStateOf("") }
                var testScore by remember { mutableStateOf("0.85") }

                OutlinedTextField(
                    value = testLabel,
                    onValueChange = { testLabel = it },
                    label = { Text("Label (e.g., LABEL_0, AI, Human)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = testScore,
                    onValueChange = { testScore = it },
                    label = { Text("Score (0-1)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val score = testScore.toFloatOrNull() ?: 0.5f
                        val result = LabelMapper.mapPrediction(testLabel, score)
                        debugInfo = """
                            Manual Mapping Result:
                            - Input: label="$testLabel", score=$score
                            - Mapped as: ${if (result.isAI) "AI" else "HUMAN"}
                            - Probability: ${result.probability}
                            - Mapping strategy: ${result.mappingStrategy}
                        """.trimIndent()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test This Label")
                }
            }
        }
    }
}

suspend fun testLabelMapping(repository: AnalyzerRepository): String {
    return try {
        // Test with known AI text
        val aiTestText = "The artificial intelligence system processes data through neural networks " +
                "and machine learning algorithms to generate coherent responses. " +
                "This text demonstrates typical patterns of AI-generated content including " +
                "repetitive structures and consistent formatting."

        // Test with known Human text
        val humanTestText = "I remember my first day at school like it was yesterday. " +
                "My mom packed my favorite lunch - peanut butter and jelly sandwich. " +
                "I was so nervous that I almost tripped walking into the classroom. " +
                "But then my teacher smiled at me and I felt much better. " +
                "It's funny how small moments can have such a big impact."

        // Make API calls using the repository
        val aiResponse = repository.makeAPICall(aiTestText)
        val humanResponse = repository.makeAPICall(humanTestText)

        val aiPrediction = aiResponse.body()?.firstOrNull()
        val humanPrediction = humanResponse.body()?.firstOrNull()

        buildString {
            appendLine("📊 LABEL MAPPING TEST RESULTS:")
            appendLine()
            appendLine("🔬 AI TEXT TEST:")
            appendLine("Input: \"${aiTestText.take(100)}...\"")
            if (aiPrediction != null) {
                appendLine("Raw Output: ${aiPrediction.label} = ${aiPrediction.score}")
                val result = LabelMapper.mapPrediction(aiPrediction.label, aiPrediction.score)
                appendLine("Mapped as: ${if (result.isAI) "✅ AI (correct)" else "❌ HUMAN (incorrect)"}")
                appendLine("Probability: ${(result.probability * 100).toInt()}%")
            } else {
                appendLine("❌ API Error - No prediction received")
            }
            appendLine()
            appendLine("👤 HUMAN TEXT TEST:")
            appendLine("Input: \"${humanTestText.take(100)}...\"")
            if (humanPrediction != null) {
                appendLine("Raw Output: ${humanPrediction.label} = ${humanPrediction.score}")
                val result = LabelMapper.mapPrediction(humanPrediction.label, humanPrediction.score)
                appendLine("Mapped as: ${if (!result.isAI) "✅ HUMAN (correct)" else "❌ AI (incorrect)"}")
                appendLine("Probability: ${(result.probability * 100).toInt()}%")
            } else {
                appendLine("❌ API Error - No prediction received")
            }
            appendLine()
            appendLine("💡 RECOMMENDATION:")
            appendLine(getRecommendation(aiPrediction, humanPrediction))
        }
    } catch (e: Exception) {
        "❌ Error during testing: ${e.message}\n\nStack trace: ${e.stackTraceToString().take(500)}"
    }
}

private fun getRecommendation(
    aiPrediction: HuggingFacePrediction?,
    humanPrediction: HuggingFacePrediction?
): String {
    if (aiPrediction == null || humanPrediction == null) {
        return "Unable to verify mapping due to API errors. Check:\n" +
                "• Internet connection\n" +
                "• API key in build.gradle\n" +
                "• Hugging Face service status"
    }

    return when {
        aiPrediction.label == "LABEL_1" && humanPrediction.label == "LABEL_0" ->
            "✅ Confirmed: LABEL_1 = AI, LABEL_0 = Human. Your mapping is correct."

        aiPrediction.label == "LABEL_0" && humanPrediction.label == "LABEL_1" ->
            "⚠️ Note: LABEL_0 = AI, LABEL_1 = Human. Update your mapping logic."

        aiPrediction.label == humanPrediction.label ->
            "⚠️ Warning: Model gives same label (${aiPrediction.label}) for both AI and Human text. Consider using a different model."

        else ->
            "📝 Unclear mapping pattern. Use the LabelMapper class to automatically detect correct mapping."
    }
}
