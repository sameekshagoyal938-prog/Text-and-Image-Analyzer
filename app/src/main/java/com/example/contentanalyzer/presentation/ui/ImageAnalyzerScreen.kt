package com.example.contentanalyzer.presentation.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.contentanalyzer.domain.models.ForensicsResult
import com.example.contentanalyzer.presentation.viewmodel.ImageAnalysisUiState
import com.example.contentanalyzer.presentation.viewmodel.ImageAnalyzerViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageAnalyzerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: ImageAnalyzerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val jsonOutput by viewModel.jsonOutput.collectAsState()

    var showRawJson by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Image Forensics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
            // Image Selection
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is ImageAnalysisUiState.Loading
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image Preview
            if (selectedImageUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.analyzeImage(context.contentResolver) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState !is ImageAnalysisUiState.Loading
                ) {
                    if (uiState is ImageAnalysisUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Icon(Icons.Default.Analytics, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Forensics Analysis")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle for JSON view
            if (jsonOutput != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showRawJson = !showRawJson }) {
                        Icon(
                            if (showRawJson) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showRawJson) "Hide JSON" else "Show Raw JSON")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results Section
            when (val state = uiState) {
                is ImageAnalysisUiState.Success -> {
                    if (showRawJson) {
                        // Raw JSON Output
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E1E1E)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "📋 JSON OUTPUT",
                                    color = Color(0xFF4ECDC4),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = jsonOutput ?: "",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        // Formatted Result Card
                        ForensicsResultCard(result = state.result)
                    }
                }
                is ImageAnalysisUiState.Error -> {
                    ImageErrorCard(
                        message = state.message,
                        onRetry = { viewModel.analyzeImage(context.contentResolver) }
                    )
                }
                ImageAnalysisUiState.Loading -> {
                    ImageLoadingCard()
                }
                else -> {
                    if (selectedImageUri == null) {
                        ImageWelcomeCard()
                    }
                }
            }
        }
    }
}

@Composable
fun ForensicsResultCard(result: ForensicsResult) {
    val isAI = result.classification == "AI GENERATED"
    val backgroundColor = if (isAI) Color(0xFFFFE0E0) else Color(0xFFE0FFE0)
    val textColor = if (isAI) Color(0xFFF44336) else Color(0xFF4CAF50)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isAI) "🤖" else "📸", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            result.classification,
                            fontSize = 22.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = textColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "${result.confidenceScore}% confident",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = textColor
                        )
                    }
                }

                // Confidence Bar
                LinearProgressIndicator(
                    progress = { result.confidenceScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = textColor,
                    trackColor = textColor.copy(alpha = 0.2f)
                )

                Divider()

                // Analysis Details
                Text(
                    "🔍 FORENSICS ANALYSIS",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                DetailRow("📷 Metadata", result.metadataStatus)
                DetailRow("🔬 PRNU", result.prnuStatus)

                Text(
                    "📊 DETAILED FINDINGS",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    "• ${result.metadataAnalysis.take(150)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "• ${result.prnuAnalysis.take(150)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "• ${result.compressionIntegrity.take(150)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Divider()

                // Final Justification
                Text(
                    "⚖️ FINAL VERDICT",
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    result.finalJustification,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )

                // Bias Declaration
                Text(
                    result.biasDeclaration,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        Text(
            value,
            fontSize = 13.sp,
            color = when (value) {
                "Detected", "Available" -> Color(0xFF4CAF50)
                "Not Detected", "Not Available" -> Color(0xFFF44336)
                else -> Color(0xFFFF9800)
            }
        )
    }
}

@Composable
fun ImageErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("❌ Error", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

@Composable
fun ImageLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Running forensic analysis...")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Analyzing PRNU noise pattern and metadata",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ImageWelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔬", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Image Forensics Engine",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Select an image to analyze PRNU noise pattern and metadata",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}
