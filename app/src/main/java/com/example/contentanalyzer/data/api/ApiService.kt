package com.example.contentanalyzer.data.api

import com.example.contentanalyzer.data.api.models.AIRequest
import com.example.contentanalyzer.data.api.models.ChatGPTDetectorRequest
import com.example.contentanalyzer.data.api.models.ChatGPTDetectorResponse
import com.example.contentanalyzer.data.api.models.HuggingFacePrediction
import com.example.contentanalyzer.data.api.models.ImageAnalysisResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // TEXT ANALYSIS ENDPOINTS (existing)
    @Headers("Content-Type: application/json")
    @POST("models/Hello-SimpleAI/chatgpt-detector-roberta")
    suspend fun detectWithChatGPTDetector(
        @Header("Authorization") authorization: String,
        @Body request: ChatGPTDetectorRequest
    ): Response<List<ChatGPTDetectorResponse>>

    @Headers("Content-Type: application/json")
    @POST("models/roberta-base-openai-detector")
    suspend fun detectWithRoBERTa(
        @Header("Authorization") authorization: String,
        @Body request: AIRequest
    ): Response<List<HuggingFacePrediction>>

    // IMAGE ANALYSIS ENDPOINT

    // Using CLIP model for zero-shot image classification
    @Headers("Content-Type: application/json")
    @POST("models/openai/clip-vit-base-patch32")
    suspend fun classifyImageWithCLIP(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, Any>
    ): Response<ImageAnalysisResponse>

    // Fallback: Using Vision Transformer
    @Multipart
    @POST("models/google/vit-base-patch16-224")
    suspend fun detectWithViT(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<ImageAnalysisResponse>

    // Google Cloud Vision API
    @POST("https://vision.googleapis.com/v1/images:annotate")
    suspend fun annotateImage(
        @Query("key") apiKey: String,
        @Body request: Map<String, Any>
    ): Response<Map<String, Any>>
}
