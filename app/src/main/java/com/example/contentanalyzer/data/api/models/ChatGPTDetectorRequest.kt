package com.example.contentanalyzer.data.api.models

data class ChatGPTDetectorRequest(
    val inputs: String,
    val options: Map<String, Boolean> = mapOf("wait_for_model" to true)
)