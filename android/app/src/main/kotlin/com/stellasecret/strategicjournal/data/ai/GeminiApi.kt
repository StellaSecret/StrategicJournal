package com.stellasecret.strategicjournal.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// ──────────────────────────────────────────────
// Retrofit interface
// ──────────────────────────────────────────────

interface GeminiApi {
    /**
     * generateContent endpoint — Gemini 2.0 Flash (free tier).
     * Base URL: https://generativelanguage.googleapis.com/
     */
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest,
    ): GeminiResponse
}

// ──────────────────────────────────────────────
// Request models
// ──────────────────────────────────────────────

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GenerationConfig = GenerationConfig(),
)

@Serializable
data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>,
)

@Serializable
data class GeminiPart(
    val text: String,
)

@Serializable
data class GenerationConfig(
    val temperature: Float = 0.2f, // Low: analytical, not creative
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 2048,
    @SerialName("topP") val topP: Float = 0.8f,
)

// ──────────────────────────────────────────────
// Response models
// ──────────────────────────────────────────────

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    @SerialName("promptFeedback") val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent,
    @SerialName("finishReason") val finishReason: String = "",
)

@Serializable
data class PromptFeedback(
    @SerialName("blockReason") val blockReason: String? = null,
)

/** Extracts the text from the first candidate */
fun GeminiResponse.extractText(): String =
    candidates
        .firstOrNull()
        ?.content
        ?.parts
        ?.firstOrNull()
        ?.text ?: ""
