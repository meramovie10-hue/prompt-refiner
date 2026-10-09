package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.PromptDatabase
import com.example.data.model.IdentityPreset
import com.example.data.model.PromptHistoryEntity
import com.example.data.model.QaResult
import com.example.data.repository.PromptRepository
import com.example.engine.MasterRefinementEngine
import com.example.engine.OptionalAiEngine
import com.example.engine.OutputMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    REFINER,
    PRESETS,
    HISTORY,
    SETTINGS
}

data class PromptUiState(
    val originalPrompt: String = "",
    val refinedPrompt: String = "",
    val selectedPreset: IdentityPreset = IdentityPreset.NONE,
    val referenceImageUri: String? = null,
    val manualImageDescription: String = "",
    val outputMode: OutputMode = OutputMode.COMPACT,
    val isRefining: Boolean = false,
    val isOutputEditable: Boolean = false,
    val qaResult: QaResult? = null,
    val statusMessage: String? = null,
    val statusLabel: String = "Offline rule-based refinement",
    val noRulesApplied: Boolean = false,
    val nameRemainsWarning: Boolean = false,
    val remainingNames: List<String> = emptyList(),
    val unresolvedSafetyWarning: Boolean = false,
    val currentTab: AppTab = AppTab.REFINER,
    val isAiModeEnabled: Boolean = false,
    val isCompactMode: Boolean = true,
    val isRealismEnforced: Boolean = false
)

class PromptViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PromptRepository

    private val _uiState = MutableStateFlow(PromptUiState())
    val uiState: StateFlow<PromptUiState> = _uiState.asStateFlow()

    val historyList: StateFlow<List<PromptHistoryEntity>>
    val allPresets: StateFlow<List<IdentityPreset>>

    init {
        val database = PromptDatabase.getInstance(application)
        repository = PromptRepository(database.promptDao(), application)

        historyList = repository.historyList.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allPresets = repository.allPresets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            IdentityPreset.BUILT_IN_PRESETS
        )

        _uiState.update {
            it.copy(
                isAiModeEnabled = repository.isOptionalAiEnabled,
                isCompactMode = repository.isCompactMode,
                isRealismEnforced = repository.isRealismEnforced,
                outputMode = if (repository.isCompactMode) OutputMode.COMPACT else OutputMode.PRESERVE_DETAIL
            )
        }
    }

    fun setTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun onOriginalPromptChanged(text: String) {
        _uiState.update { it.copy(originalPrompt = text) }
    }

    fun onRefinedPromptChanged(text: String) {
        _uiState.update { it.copy(refinedPrompt = text) }
    }

    fun onSelectPreset(preset: IdentityPreset) {
        _uiState.update { it.copy(selectedPreset = preset) }
    }

    fun onReferenceImageSelected(uriString: String?) {
        _uiState.update { it.copy(referenceImageUri = uriString) }
    }

    fun onManualImageDescriptionChanged(description: String) {
        _uiState.update { it.copy(manualImageDescription = description) }
    }

    fun setOutputMode(mode: OutputMode) {
        _uiState.update {
            it.copy(
                outputMode = mode,
                isCompactMode = mode == OutputMode.COMPACT
            )
        }
        repository.isCompactMode = (mode == OutputMode.COMPACT)
    }

    fun toggleOutputEditable() {
        _uiState.update { it.copy(isOutputEditable = !it.isOutputEditable) }
    }

    /**
     * RESTORE ORIGINAL:
     * Recovers the exact original input, including its original wording.
     */
    fun restoreOriginal() {
        val original = _uiState.value.originalPrompt
        _uiState.update {
            it.copy(
                refinedPrompt = original,
                statusMessage = "Original prompt restored.",
                isOutputEditable = false
            )
        }
    }

    fun clearAll() {
        _uiState.update {
            it.copy(
                originalPrompt = "",
                refinedPrompt = "",
                referenceImageUri = null,
                manualImageDescription = "",
                qaResult = null,
                isOutputEditable = false,
                statusMessage = "Cleared",
                noRulesApplied = false,
                nameRemainsWarning = false,
                unresolvedSafetyWarning = false
            )
        }
    }

    /**
     * Executes the deterministic offline refinement engine.
     * Implements Steps A through I of the exact behavior specification.
     */
    fun refine() {
        val state = _uiState.value
        val inputPrompt = state.originalPrompt.trim()

        // STEP A Validation
        if (inputPrompt.isEmpty()) {
            _uiState.update {
                it.copy(
                    statusMessage = "Paste a prompt to refine.",
                    statusLabel = "Paste a prompt to refine.",
                    refinedPrompt = ""
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRefining = true, statusMessage = null) }

            val engineInput = MasterRefinementEngine.RefinementInput(
                originalPrompt = inputPrompt,
                identityPreset = state.selectedPreset,
                hasReferenceImage = state.referenceImageUri != null,
                manualImageDescription = state.manualImageDescription,
                outputMode = state.outputMode,
                enforceRealism = state.isRealismEnforced
            )

            val output = MasterRefinementEngine.refine(engineInput)

            _uiState.update {
                it.copy(
                    refinedPrompt = output.finalPrompt,
                    qaResult = output.qaResult,
                    isRefining = false,
                    isOutputEditable = false,
                    statusMessage = output.statusLabel,
                    statusLabel = output.statusLabel,
                    noRulesApplied = output.noRulesApplied,
                    nameRemainsWarning = output.nameRemainsWarning,
                    remainingNames = output.remainingNames,
                    unresolvedSafetyWarning = output.unresolvedSafetyWarning
                )
            }

            // Save to local Room history
            if (output.finalPrompt.isNotBlank()) {
                val historyItem = PromptHistoryEntity(
                    originalPrompt = inputPrompt,
                    refinedPrompt = output.finalPrompt,
                    presetId = state.selectedPreset.id,
                    presetName = state.selectedPreset.name,
                    hasReferenceImage = state.referenceImageUri != null,
                    referenceImageUri = state.referenceImageUri,
                    qaChecksPassed = output.qaResult.passedCount
                )
                repository.saveHistory(historyItem)
            }
        }
    }

    /**
     * PURE COPY (Section 5):
     * Copies ONLY the final refined prompt.
     * Does NOT copy headings, explanations, analysis, or metadata.
     */
    fun copyToClipboard(context: Context) {
        val textToCopy = _uiState.value.refinedPrompt.trim()
        if (textToCopy.isBlank()) return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Flow Prompt", textToCopy)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(statusMessage = "Copied refined prompt.") }
    }

    fun reopenFromHistory(item: PromptHistoryEntity) {
        val matchedPreset = allPresets.value.find { it.id == item.presetId } ?: IdentityPreset.NONE
        _uiState.update {
            it.copy(
                originalPrompt = item.originalPrompt,
                refinedPrompt = item.refinedPrompt,
                selectedPreset = matchedPreset,
                referenceImageUri = item.referenceImageUri,
                currentTab = AppTab.REFINER,
                statusMessage = "Restored from history"
            )
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _uiState.update { it.copy(statusMessage = "History cleared") }
        }
    }

    fun saveCustomPreset(preset: IdentityPreset) {
        viewModelScope.launch {
            repository.saveCustomPreset(preset)
            _uiState.update { it.copy(statusMessage = "Preset '${preset.name}' saved") }
        }
    }

    fun deletePreset(preset: IdentityPreset) {
        viewModelScope.launch {
            repository.deleteCustomPreset(preset)
            if (_uiState.value.selectedPreset.id == preset.id) {
                _uiState.update { it.copy(selectedPreset = IdentityPreset.NONE) }
            }
        }
    }

    fun toggleAiMode(enabled: Boolean) {
        repository.isOptionalAiEnabled = enabled
        _uiState.update { it.copy(isAiModeEnabled = enabled) }
    }

    fun toggleCompactMode(enabled: Boolean) {
        repository.isCompactMode = enabled
        _uiState.update {
            it.copy(
                isCompactMode = enabled,
                outputMode = if (enabled) OutputMode.COMPACT else OutputMode.PRESERVE_DETAIL
            )
        }
    }

    fun toggleRealism(enabled: Boolean) {
        repository.isRealismEnforced = enabled
        _uiState.update { it.copy(isRealismEnforced = enabled) }
    }

    fun setCustomApiKey(key: String) {
        repository.customAiApiKey = key
    }

    suspend fun exportJson(): String {
        return repository.exportDataAsJson(allPresets.value)
    }

    suspend fun importJson(json: String): Result<Int> {
        return repository.importDataFromJson(json)
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
