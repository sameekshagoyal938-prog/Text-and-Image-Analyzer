package com.example.contentanalyzer.presentation.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.contentanalyzer.domain.models.AnalysisHistory

@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    history: AnalysisHistory?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog && history != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Record") },
            text = {
                Text(
                    "Are you sure you want to delete this analysis?\n\n" +
                            "Result: ${history.result}\n" +
                            "Confidence: ${history.confidence}%\n" +
                            "Date: ${history.formattedDate}"
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}