package com.example.contentanalyzer.data.model

import com.google.gson.annotations.SerializedName

data class HuggingFacePrediction(
    @SerializedName("label")
    val label: String,

    @SerializedName("score")
    val score: Float
)