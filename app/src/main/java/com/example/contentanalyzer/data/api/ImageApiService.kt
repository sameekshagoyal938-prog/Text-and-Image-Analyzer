package com.example.contentanalyzer.data.api

import com.example.contentanalyzer.data.api.models.ImageAnalysisResponse
import com.example.contentanalyzer.data.api.models.ZeroShotResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ImageApiService {

    @Headers("Content-Type: application/json")
    @POST("models/openai/clip-vit-base-patch32")
    suspend fun classifyImageWithCLIP(
        @Header("Authorization") authorization: String,
        @Body request: Map<String, Any>
    ): Response<ZeroShotResponse>

    @Multipart
    @POST("models/google/vit-base-patch16-224")
    suspend fun detectWithViT(
        @Header("Authorization") authorization: String,
        @Part image: MultipartBody.Part
    ): Response<ImageAnalysisResponse>
}
