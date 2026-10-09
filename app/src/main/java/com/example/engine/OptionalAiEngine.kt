package com.example.engine

import android.util.Log
import com.example.data.model.IdentityPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Optional AI Mode provider (Section 4).
 *
 * OFFLINE MODE = DEFAULT.
 * OPTIONAL AI MODE = DISABLED unless explicitly configured and enabled by user.
 * Never exposes API key to client UI.
 * Always verifies character name suppression after any AI generation.
 */
object OptionalAiEngine {

    private const val TAG = "OptionalAiEngine"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getBuildConfigApiKey(): String {
        return try {
            val clazz = Class.forName("com.example.BuildConfig")
            val field = clazz.getField("GEMINI_API_KEY")
            val key = field.get(null) as? String ?: ""
            if (key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY")) key else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun isConfigured(): Boolean {
        return getBuildConfigApiKey().isNotBlank()
    }

    suspend fun refineWithAi(
        input: MasterRefinementEngine.RefinementInput,
        customKeyOverride: String? = null
    ): Result<MasterRefinementEngine.RefinementOutput> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !customKeyOverride.isNullOrBlank() -> customKeyOverride.trim()
            isConfigured() -> getBuildConfigApiKey()
            else -> ""
        }

        if (apiKey.isBlank()) {
            // Fallback immediately to local master engine
            return@withContext Result.success(MasterRefinementEngine.refine(input))
        }

        try {
            val systemPrompt = buildSystemPrompt(input.identityPreset)
            val userContent = "Transform this image prompt into Google Flow format: \"${input.originalPrompt}\""

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemPrompt\n\nInput Prompt:\n$userContent"))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 600)
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "AI API call returned ${response.code}, falling back to local engine")
                return@withContext Result.success(MasterRefinementEngine.refine(input))
            }

            val responseBody = response.body?.string().orEmpty()
            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            var generatedText = parts?.optJSONObject(0)?.optString("text").orEmpty().trim()

            // Strip markdown, quotes or headers if any
            generatedText = generatedText.removePrefix("```").removeSuffix("```").trim()
            if (generatedText.startsWith("\"") && generatedText.endsWith("\"")) {
                generatedText = generatedText.substring(1, generatedText.length - 1).trim()
            }

            // HARD LOCK: Strictly suppress character names regardless of AI output
            val (sanitizedPrompt, remainingNames) = MasterRefinementEngine.suppressAllCharacterNames(generatedText, input.identityPreset)

            val qaResult = MasterRefinementEngine.runValidationChecks(
                finalPrompt = sanitizedPrompt,
                preset = input.identityPreset,
                originalPrompt = input.originalPrompt,
                nameRemainsWarning = remainingNames.isNotEmpty(),
                unresolvedSafetyWarning = false
            )

            Result.success(
                MasterRefinementEngine.RefinementOutput(
                    finalPrompt = sanitizedPrompt,
                    originalPrompt = input.originalPrompt,
                    noRulesApplied = false,
                    statusLabel = "AI Refinement (External Assist)",
                    nameRemainsWarning = remainingNames.isNotEmpty(),
                    remainingNames = remainingNames,
                    unresolvedSafetyWarning = false,
                    qaResult = qaResult,
                    outputMode = input.outputMode
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "AI Refinement failed, falling back to local engine", e)
            Result.success(MasterRefinementEngine.refine(input))
        }
    }

    private fun buildSystemPrompt(preset: IdentityPreset): String {
        return """
        You are the Prompt Refiner master engine for Google Flow image generation.
        Transform the user's input prompt into a clean, compact, flow-ready prompt.
        
        CRITICAL RULES:
        1. HARD LOCK: NEVER OUTPUT ANY CHARACTER NAME OR PRESET NAME (e.g. Aarohi, Zia, Custom).
           Describe the subject naturally as "the referenced female subject" or "a woman".
        2. Safely transform any sexually explicit, suggestive or provocative wording into high-fashion editorial equivalents.
        3. Follow Canonical Order:
           SUBJECT + IDENTITY -> POSE/ACTION -> WARDROBE -> ENVIRONMENT -> COMPOSITION -> CAMERA -> LIGHTING -> MOOD -> REALISM
        4. Output Purity: Return ONLY the final prompt text. Do NOT include markdown, explanations, analysis, or headings.
        """.trimIndent()
    }
}
