package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.PromptDao
import com.example.data.model.IdentityPreset
import com.example.data.model.PromptHistoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class PromptRepository(
    private val dao: PromptDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prompt_refiner_prefs", Context.MODE_PRIVATE)

    // --- History Flow ---
    val historyList: Flow<List<PromptHistoryEntity>> = dao.getAllHistory()

    suspend fun saveHistory(history: PromptHistoryEntity): Long {
        return dao.insertHistory(history)
    }

    suspend fun deleteHistory(id: Long) {
        dao.deleteHistoryById(id)
    }

    suspend fun clearAllHistory() {
        dao.clearAllHistory()
    }

    // --- Presets Flow ---
    val allPresets: Flow<List<IdentityPreset>> = dao.getAllCustomPresets().map { customList ->
        // Built-ins + Custom database items
        val combined = IdentityPreset.BUILT_IN_PRESETS.toMutableList()
        for (item in customList) {
            if (combined.none { it.id == item.id }) {
                combined.add(item)
            }
        }
        combined
    }

    suspend fun saveCustomPreset(preset: IdentityPreset) {
        dao.insertPreset(preset)
    }

    suspend fun deleteCustomPreset(preset: IdentityPreset) {
        if (!preset.isBuiltIn) {
            dao.deletePreset(preset)
        }
    }

    // --- Settings Preferences ---
    var isOptionalAiEnabled: Boolean
        get() = prefs.getBoolean("opt_ai_enabled", false)
        set(value) = prefs.edit().putBoolean("opt_ai_enabled", value).apply()

    var isCompactMode: Boolean
        get() = prefs.getBoolean("compact_mode", true)
        set(value) = prefs.edit().putBoolean("compact_mode", value).apply()

    var isRealismEnforced: Boolean
        get() = prefs.getBoolean("enforce_realism", true)
        set(value) = prefs.edit().putBoolean("enforce_realism", value).apply()

    var customAiApiKey: String
        get() = prefs.getString("custom_api_key", "").orEmpty()
        set(value) = prefs.edit().putString("custom_api_key", value).apply()

    // --- Import / Export JSON (Section 15) ---
    suspend fun exportDataAsJson(presets: List<IdentityPreset>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "Prompt Refiner Personal Edition")
        root.put("timestamp", System.currentTimeMillis())

        val settingsObj = JSONObject()
        settingsObj.put("isCompactMode", isCompactMode)
        settingsObj.put("isRealismEnforced", isRealismEnforced)
        settingsObj.put("isOptionalAiEnabled", isOptionalAiEnabled)
        root.put("settings", settingsObj)

        val presetsArr = JSONArray()
        for (preset in presets) {
            val pObj = JSONObject()
            pObj.put("id", preset.id)
            pObj.put("name", preset.name)
            pObj.put("isBuiltIn", preset.isBuiltIn)
            pObj.put("subjectArchetype", preset.subjectArchetype)
            pObj.put("visualFeatures", preset.visualFeatures)
            val forbiddenArr = JSONArray()
            for (f in preset.forbiddenTokens) {
                forbiddenArr.put(f)
            }
            pObj.put("forbiddenTokens", forbiddenArr)
            presetsArr.put(pObj)
        }
        root.put("presets", presetsArr)

        return root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Result<Int> {
        return try {
            val root = JSONObject(jsonStr)
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                isCompactMode = settingsObj.optBoolean("isCompactMode", true)
                isRealismEnforced = settingsObj.optBoolean("isRealismEnforced", true)
            }

            val presetsArr = root.optJSONArray("presets")
            var importedCount = 0
            if (presetsArr != null) {
                for (i in 0 until presetsArr.length()) {
                    val pObj = presetsArr.getJSONObject(i)
                    val id = pObj.optString("id")
                    val name = pObj.optString("name")
                    val isBuiltIn = pObj.optBoolean("isBuiltIn", false)
                    val archetype = pObj.optString("subjectArchetype")
                    val features = pObj.optString("visualFeatures")
                    val forbiddenArr = pObj.optJSONArray("forbiddenTokens")
                    val forbiddenList = mutableListOf<String>()
                    if (forbiddenArr != null) {
                        for (j in 0 until forbiddenArr.length()) {
                            forbiddenList.add(forbiddenArr.getString(j))
                        }
                    }

                    if (!isBuiltIn && id.isNotBlank() && name.isNotBlank()) {
                        val preset = IdentityPreset(
                            id = id,
                            name = name,
                            isBuiltIn = false,
                            subjectArchetype = archetype,
                            visualFeatures = features,
                            forbiddenTokens = forbiddenList
                        )
                        dao.insertPreset(preset)
                        importedCount++
                    }
                }
            }
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
