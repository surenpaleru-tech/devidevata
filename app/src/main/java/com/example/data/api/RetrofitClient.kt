package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

// --- Remote Server Config Model ---
@JsonClass(generateAdapter = true)
data class ServerConfig(
    @Json(name = "activeModel") val activeModel: String = "gemini-3.5-flash",
    @Json(name = "llmTuningPrompt") val llmTuningPrompt: String = "You are an advanced spiritual AI Vedic scholar. Answer questions about Indian mythology, scriptures, gods, temples, and stotrams based on authentic Puranas and Vedas. Keep answers insightful, warm, and highly informative.",
    @Json(name = "imgServerBaseUrl") val imgServerBaseUrl: String = "https://api.devidevata.com/",
    @Json(name = "pushAlertMessage") val pushAlertMessage: String = "Maha Shivaratri worship: Fasting, chanting Om Namah Shivaya and Rudra Shaktipat.",
    @Json(name = "llmOverrideUrl") val llmOverrideUrl: String? = null // Admin can route chat requests directly to Cloud custom API
)

// --- Retrofit Data Models for Server Sync ---
@JsonClass(generateAdapter = true)
data class RemoteGodCategory(
    val id: Int,
    val name: String,
    val description: String,
    val thumbnail: String,
    val defaultColor: String
)

@JsonClass(generateAdapter = true)
data class RemoteGodImage(
    val id: Int,
    val categoryId: Int,
    val title: String,
    val url: String,
    val thumbUrl: String,
    val credit: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class RemoteStotramPuja(
    val id: Int,
    val categoryId: Int,
    val type: String,
    val title: String,
    val sanskritText: String,
    val translation: String,
    val benefits: String
)

@JsonClass(generateAdapter = true)
data class RemoteTempleInfo(
    val id: Int,
    val categoryId: Int,
    val name: String,
    val location: String,
    val history: String,
    val significance: String,
    val timing: String,
    val imageUrl: String
)

@JsonClass(generateAdapter = true)
data class RemoteFestival(
    val id: Int,
    val title: String,
    val dateStr: String,
    val dayOfWeek: String,
    val Month: String,
    val tithi: String,
    val nakshatra: String,
    val deityCategoryId: Int,
    val description: String,
    val rituals: String
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val version: Int,
    val categories: List<RemoteGodCategory>,
    val images: List<RemoteGodImage>,
    val stotrams: List<RemoteStotramPuja>,
    val temples: List<RemoteTempleInfo>,
    val festivals: List<RemoteFestival>
)

// --- Gemini API Models (Retrofit-compatible) ---
@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

// --- Custom Backend Chat Request ---
@JsonClass(generateAdapter = true)
data class CustomChatRequest(
    val prompt: String,
    val systemInstruction: String,
    val model: String
)

@JsonClass(generateAdapter = true)
data class CustomChatResponse(
    val response: String
)

// --- Retrofit API Interface ---
interface DeviDevataApiService {

    // 1. Fetch Remote Config from Cloud/Custom Server
    @GET
    suspend fun getRemoteConfig(@Url url: String): ServerConfig

    // 2. Full synchronization from Cloud Server
    @GET
    suspend fun syncAllData(@Url url: String): SyncResponse

    // 3. Direct Gemini API call (Standard REST endpoint)
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateGeminiContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    // 4. Custom Server LLM Gateway endpoint
    @POST
    suspend fun generateCustomServerContent(
        @Url url: String,
        @Body request: CustomChatRequest
    ): CustomChatResponse
}

// --- Retrofit Client Singleton ---
object RetrofitClient {
    private const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: DeviDevataApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(DeviDevataApiService::class.java)
    }
}
