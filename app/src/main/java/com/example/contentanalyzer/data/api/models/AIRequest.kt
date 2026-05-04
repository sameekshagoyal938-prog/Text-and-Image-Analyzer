package com.example.contentanalyzer.data.api.models

/**
 * Request model for Hugging Face API
 * Must be in the same package as other API models
 */
data class AIRequest(
    val inputs: String,
    val options: Map<String, Boolean>? = mapOf("wait_for_model" to true)
)