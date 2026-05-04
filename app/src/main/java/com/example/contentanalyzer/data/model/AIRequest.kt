package com.example.contentanalyzer.data.model

/**
 * Concrete data class for Hugging Face API request body
 */
data class AIRequest(
    val inputs: String,
    val options: Map<String, Boolean>? = mapOf("wait_for_model" to true)
)
