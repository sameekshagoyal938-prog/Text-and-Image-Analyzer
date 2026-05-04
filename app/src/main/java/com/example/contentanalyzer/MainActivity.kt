package com.example.contentanalyzer

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.contentanalyzer.domain.analyzers.AnalysisManager
import com.example.contentanalyzer.domain.models.AnalysisResult
import com.example.contentanalyzer.presentation.ui.ResultCard
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var analysisManager: AnalysisManager

    // UI State
    private var inputText by mutableStateOf("")
    private var selectedImageUri by mutableStateOf<Uri?>(null)
    private var analysisResult by mutableStateOf<AnalysisResult?>(null)
    private var isLoading by mutableStateOf(false)
    private var activeTab by mutableStateOf(0) // 0 = Text, 1 = Image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OpenCV (for image analysis)
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed - image analysis may not work")
        }

        analysisManager = AnalysisManager()

        setContent {
            val imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    selectedImageUri = it
                    analyzeImage(it)
                }
            }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab Row for switching between Text and Image analysis
                    TabRow(
                        selectedTabIndex = activeTab,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = {
                                activeTab = 0
                                // Clear previous results when switching
                                analysisResult = null
                                inputText = ""
                                selectedImageUri = null
                            },
                            text = { Text("📝 Text Analysis") },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = {
                                activeTab = 1
                                analysisResult = null
                                inputText = ""
                                selectedImageUri = null
                            },
                            text = { Text("🖼️ Image Analysis") },
                            icon = { Icon(Icons.Default.Image, contentDescription = null) }
                        )
                    }

                    when (activeTab) {
                        0 -> TextAnalysisUI(
                            inputText = inputText,
                            onTextChange = { inputText = it },
                            onAnalyze = { analyzeText() },
                            isLoading = isLoading
                        )
                        1 -> ImageAnalysisUI(
                            selectedImageUri = selectedImageUri,
                            onSelectImage = { imagePickerLauncher.launch("image/*") },
                            onAnalyze = { selectedImageUri?.let { analyzeImage(it) } },
                            isLoading = isLoading
                        )
                    }

                    // Results Section
                    analysisResult?.let { result ->
                        ResultCard(result = result)
                    }
                }
            }
        }
    }

    private fun analyzeText() {
        if (inputText.trim().isEmpty()) {
            analysisResult = AnalysisResult(
                type = AnalysisResult.AnalysisType.TEXT,
                classification = "NO INPUT",
                confidence = 0,
                details = "Please enter text to analyze",
                rawScore = 0.5f
            )
            return
        }

        lifecycleScope.launch {
            isLoading = true
            try {
                val result = analysisManager.analyzeText(inputText)
                analysisResult = result
                Log.d(TAG, "Text analysis complete: ${result.classification}")
            } catch (e: Exception) {
                Log.e(TAG, "Text analysis failed", e)
                analysisResult = AnalysisResult(
                    type = AnalysisResult.AnalysisType.TEXT,
                    classification = "ERROR",
                    confidence = 0,
                    details = "Analysis failed: ${e.message}",
                    rawScore = 0.5f
                )
            } finally {
                isLoading = false
            }
        }
    }

    private fun analyzeImage(uri: Uri) {
        lifecycleScope.launch {
            isLoading = true
            try {
                val result = analysisManager.analyzeImage(contentResolver, uri)
                analysisResult = result
                Log.d(TAG, "Image analysis complete: ${result.classification}")
            } catch (e: Exception) {
                Log.e(TAG, "Image analysis failed", e)
                analysisResult = AnalysisResult(
                    type = AnalysisResult.AnalysisType.IMAGE,
                    classification = "ERROR",
                    confidence = 0,
                    details = "Analysis failed: ${e.message}",
                    rawScore = 0.5f
                )
            } finally {
                isLoading = false
            }
        }
    }
}

@Composable
fun TextAnalysisUI(
    inputText: String,
    onTextChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    isLoading: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste or type your content here...") },
                    minLines = 8,
                    maxLines = 12
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAnalyze,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && inputText.trim().isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Icon(Icons.Default.Analytics, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Text")
                    }
                }
            }
        }

        Text(
            text = "💡 Tip: Longer text (100+ characters) provides more accurate results",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ImageAnalysisUI(
    selectedImageUri: Uri?,
    onSelectImage: () -> Unit,
    onAnalyze: () -> Unit,
    isLoading: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onSelectImage,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Image")
        }

        if (selectedImageUri != null) {
            Button(
                onClick = onAnalyze,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing Image...")
                } else {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Image")
                }
            }
        }

        Text(
            text = "💡 Tip: For best results, use clear, well-lit photos from actual cameras",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
