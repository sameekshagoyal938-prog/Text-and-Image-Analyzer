package com.example.contentanalyzer.data.api.models

import com.google.gson.annotations.SerializedName

data class ChatGPTDetectorResponse(
    @SerializedName("label")
    val label: String,

    @SerializedName("score")
    val score: Float
)