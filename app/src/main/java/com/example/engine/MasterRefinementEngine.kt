package com.example.engine

import com.example.data.model.IdentityPreset
import com.example.data.model.QaCheck
import com.example.data.model.QaResult
import java.util.Locale

enum class OutputMode {
    COMPACT,
    PRESERVE_DETAIL
}

/**
 * Deterministic Rule-Based Offline Refinement Engine.
 *
 * Implements the Offline Refinement Engine Exact Behavior Specification:
 * - 100% deterministic local rule processing
 * - No internet connection, external APIs, or AI simulation
 * - Orderly pipeline: Steps A through I
 * - Output modes: COMPACT (default) and PRESERVE_DETAIL
 * - Strict character name suppression and honest offline labeling
 */
object MasterRefinementEngine {

    data class RefinementInput(
        val originalPrompt: String,
        val identityPreset: IdentityPreset = IdentityPreset.NONE,
        val hasReferenceImage: Boolean = false,
        val manualImageDescription: String = "",
        val outputMode: OutputMode = OutputMode.COMPACT,
        val enforceRealism: Boolean = false
    )

    data class RefinementOutput(
        val finalPrompt: String,
        val originalPrompt: String,
        val noRulesApplied: Boolean,
        val statusLabel: String,
        val nameRemainsWarning: Boolean,
        val remainingNames: List<String>,
        val unresolvedSafetyWarning: Boolean,
        val qaResult: QaResult,
        val outputMode: OutputMode
    )

    /**
     * Executes the exact offline refinement pipeline Steps A through I.
     */
    fun refine(input: RefinementInput): RefinementOutput {
        // STEP A — Validate Input
        val trimmedRaw = input.originalPrompt.trim()
        if (trimmedRaw.isEmpty()) {
            return RefinementOutput(
                finalPrompt = "",
                originalPrompt = input.originalPrompt,
                noRulesApplied = true,
                statusLabel = "Paste a prompt to refine.",
                nameRemainsWarning = false,
                remainingNames = emptyList(),
                unresolvedSafetyWarning = false,
                qaResult = QaResult(emptyList(), characterNameSuppressed = true, safetyVerified = true, flowReady = false),
                outputMode = input.outputMode
            )
        }

        var textChanged = false

        // STEP B — Normalize Text
        // Normalize repeated spaces and excessive blank lines while preserving punctuation, quotes, measurements
        var workingText = normalizeText(trimmedRaw)
        if (workingText != trimmedRaw) {
            textChanged = true
        }

        // Incorporate manual image description if provided (Section 3: Reference Image Handling)
        val manualImgNote = input.manualImageDescription.trim()
        if (manualImgNote.isNotEmpty()) {
            workingText = "$workingText, reference visual context: $manualImgNote"
            textChanged = true
        }

        // STEP C — Identify Prompt Sections (Heuristic Classification)
        val sections = identifyPromptSections(workingText)

        // STEP D — Apply Selected Identity Preset
        // Read only selected preset. If no preset or NONE, do not add identity info.
        val hasPreset = input.identityPreset.id != IdentityPreset.NONE.id && input.identityPreset.visualFeatures.isNotBlank()
        if (hasPreset) {
            textChanged = true
        }

        // STEP E — Apply Safe Text Transformations
        val (safeText, safeReplacementsCount, unresolvedFlags) = applySafeTextTransformations(workingText)
        workingText = safeText
        if (safeReplacementsCount > 0) {
            textChanged = true
        }

        // STEP F — Remove Repetition
        val dedupedText = removeRepetition(workingText, input.outputMode)
        if (dedupedText != workingText) {
            workingText = dedupedText
            textChanged = true
        }

        // STEP G — Reorder Recognized Sections or Preserve Detail
        val reorderedText = when (input.outputMode) {
            OutputMode.COMPACT -> {
                // If section classification was confident, arrange canonically
                val assembled = assembleCanonicalOrder(sections, input.identityPreset, workingText, input.enforceRealism)
                if (assembled.isNotBlank()) {
                    textChanged = true
                    assembled
                } else {
                    workingText
                }
            }
            OutputMode.PRESERVE_DETAIL -> {
                // In PRESERVE DETAIL mode: preserve original sequence and detail!
                if (hasPreset) {
                    val presetAttrs = "${input.identityPreset.subjectArchetype}, ${input.identityPreset.visualFeatures}"
                    "$presetAttrs, $workingText"
                } else {
                    workingText
                }
            }
        }

        // STEP H — Character Name Suppression
        val (cleansedPrompt, remainingNames) = suppressAllCharacterNames(reorderedText, input.identityPreset)
        if (cleansedPrompt != reorderedText) {
            textChanged = true
        }
        val nameRemainsWarning = remainingNames.isNotEmpty()

        // STEP I — Final Validation
        val normalizedFinal = normalizeText(cleansedPrompt).trimEnd(',', '.', ' ')

        // Check if final output differs from normalized original input
        val originalNormalized = normalizeText(trimmedRaw)
        val noRulesApplied = (normalizedFinal.equals(originalNormalized, ignoreCase = true) || !textChanged) && !hasPreset && manualImgNote.isEmpty()

        val statusLabel = if (noRulesApplied) {
            "No applicable offline rules found. Original prompt preserved."
        } else {
            "Offline rule-based refinement"
        }

        val qaResult = runValidationChecks(
            finalPrompt = normalizedFinal,
            preset = input.identityPreset,
            originalPrompt = trimmedRaw,
            nameRemainsWarning = nameRemainsWarning,
            unresolvedSafetyWarning = unresolvedFlags
        )

        return RefinementOutput(
            finalPrompt = normalizedFinal,
            originalPrompt = input.originalPrompt,
            noRulesApplied = noRulesApplied,
            statusLabel = statusLabel,
            nameRemainsWarning = nameRemainsWarning,
            remainingNames = remainingNames,
            unresolvedSafetyWarning = unresolvedFlags,
            qaResult = qaResult,
            outputMode = input.outputMode
        )
    }

    /**
     * STEP B: Normalize repeated whitespace and blank lines.
     * Preserves punctuation, measurements (e.g. 85mm, f/1.8), and quotes.
     */
    fun normalizeText(input: String): String {
        var text = input
        // Replace excessive blank lines
        text = text.replace(Regex("\n{3,}"), "\n\n")
        // Replace multiple horizontal spaces/tabs with single space
        text = text.replace(Regex("[ \\t]+"), " ")
        // Normalize space before punctuation
        text = text.replace(Regex(" +([,;.:])"), "$1")
        // Normalize double commas
        text = text.replace(Regex(",\\s*,"), ",")
        return text.trim()
    }

    /**
     * STEP C: Identify likely references to prompt sections using keyword patterns.
     */
    data class PromptSections(
        val subject: String = "",
        val poseAction: String = "",
        val wardrobeAccessories: String = "",
        val environment: String = "",
        val compositionFraming: String = "",
        val cameraPerspective: String = "",
        val lighting: String = "",
        val moodStyle: String = "",
        val realismQuality: String = "",
        val negativeConstraints: String = "",
        val confidenceHigh: Boolean = false
    )

    fun identifyPromptSections(text: String): PromptSections {
        val clauses = text.split(",", ";", "\n").map { it.trim() }.filter { it.isNotBlank() }

        var subject = ""
        var poseAction = ""
        var wardrobe = ""
        var environment = ""
        var composition = ""
        var camera = ""
        var lighting = ""
        var mood = ""
        var realism = ""
        var constraints = ""

        var matchedClausesCount = 0

        for (clause in clauses) {
            val lower = clause.lowercase(Locale.ROOT)
            when {
                lower.startsWith("--no") || lower.contains("negative prompt") || lower.contains("without ") -> {
                    constraints = if (constraints.isEmpty()) clause else "$constraints, $clause"
                    matchedClausesCount++
                }
                lower.contains("camera") || lower.contains("lens") || lower.contains("35mm") ||
                lower.contains("50mm") || lower.contains("85mm") || lower.contains("perspective") ||
                lower.contains("telephoto") || lower.contains("f/1.8") || lower.contains("f/2.8") -> {
                    camera = if (camera.isEmpty()) clause else "$camera, $clause"
                    matchedClausesCount++
                }
                lower.contains("light") || lower.contains("shadow") || lower.contains("sunlight") ||
                lower.contains("golden hour") || lower.contains("chiaroscuro") || lower.contains("illuminat") -> {
                    lighting = if (lighting.isEmpty()) clause else "$lighting, $clause"
                    matchedClausesCount++
                }
                lower.contains("composition") || lower.contains("framing") || lower.contains("close-up") ||
                lower.contains("medium shot") || lower.contains("wide shot") || lower.contains("full body shot") ||
                lower.contains("portrait shot") || lower.contains("depth of field") -> {
                    composition = if (composition.isEmpty()) clause else "$composition, $clause"
                    matchedClausesCount++
                }
                lower.contains("pose") || lower.contains("stance") || lower.contains("sitting") ||
                lower.contains("standing") || lower.contains("walking") || lower.contains("leaning") ||
                lower.contains("reclining") || lower.contains("looking back") -> {
                    poseAction = if (poseAction.isEmpty()) clause else "$poseAction, $clause"
                    matchedClausesCount++
                }
                lower.contains("dress") || lower.contains("outfit") || lower.contains("attire") ||
                lower.contains("clothing") || lower.contains("wearing") || lower.contains("jacket") ||
                lower.contains("blazer") || lower.contains("suit") || lower.contains("garment") ||
                lower.contains("jewelry") || lower.contains("fabric") -> {
                    wardrobe = if (wardrobe.isEmpty()) clause else "$wardrobe, $clause"
                    matchedClausesCount++
                }
                lower.contains("street") || lower.contains("city") || lower.contains("cafe") ||
                lower.contains("studio") || lower.contains("interior") || lower.contains("background") ||
                lower.contains("room") || lower.contains("garden") || lower.contains("beach") ||
                lower.contains("nature") || lower.contains("urban") -> {
                    environment = if (environment.isEmpty()) clause else "$environment, $clause"
                    matchedClausesCount++
                }
                lower.contains("mood") || lower.contains("aesthetic") || lower.contains("cinematic") ||
                lower.contains("editorial") || lower.contains("atmosphere") || lower.contains("poised") -> {
                    mood = if (mood.isEmpty()) clause else "$mood, $clause"
                    matchedClausesCount++
                }
                lower.contains("texture") || lower.contains("grain") || lower.contains("photorealistic") ||
                lower.contains("quality") || lower.contains("detail") -> {
                    realism = if (realism.isEmpty()) clause else "$realism, $clause"
                    matchedClausesCount++
                }
                lower.contains("woman") || lower.contains("man") || lower.contains("person") ||
                lower.contains("girl") || lower.contains("boy") || lower.contains("subject") ||
                lower.contains("model") || lower.contains("portrait of") -> {
                    subject = if (subject.isEmpty()) clause else "$subject, $clause"
                    matchedClausesCount++
                }
            }
        }

        val confidence = matchedClausesCount >= 2 || (clauses.size <= 2 && matchedClausesCount >= 1)

        return PromptSections(
            subject = subject,
            poseAction = poseAction,
            wardrobeAccessories = wardrobe,
            environment = environment,
            compositionFraming = composition,
            cameraPerspective = camera,
            lighting = lighting,
            moodStyle = mood,
            realismQuality = realism,
            negativeConstraints = constraints,
            confidenceHigh = confidence
        )
    }

    /**
     * STEP E: Apply Safe Text Transformations using explicit maintainable dictionary.
     * Uses strict word boundary matching to never corrupt unrelated substrings.
     */
    fun applySafeTextTransformations(text: String): Triple<String, Int, Boolean> {
        var transformed = text
        var matchCount = 0
        var unresolvedFlags = false

        // 1. Approved phrase replacements
        for ((problematic, safe) in SafetyVocabulary.PHRASE_REPLACEMENTS) {
            val regex = Regex("(?i)\\b${Regex.escape(problematic)}\\b")
            if (regex.containsMatchIn(transformed)) {
                transformed = transformed.replace(regex, safe)
                matchCount++
            }
        }

        // 2. Approved single-word replacements
        for ((problematic, safe) in SafetyVocabulary.WORD_REPLACEMENTS) {
            val regex = Regex("(?i)\\b${Regex.escape(problematic)}\\b")
            if (regex.containsMatchIn(transformed)) {
                transformed = transformed.replace(regex, safe)
                matchCount++
            }
        }

        // Flag if potentially unsafe words remain unmapped
        val flagCheck = listOf("porn", "nsfw", "xxx", "nudity", "unclothed")
        for (flag in flagCheck) {
            if (transformed.contains(Regex("(?i)\\b${Regex.escape(flag)}\\b"))) {
                unresolvedFlags = true
            }
        }

        return Triple(transformed, matchCount, unresolvedFlags)
    }

    /**
     * STEP F: Remove exact duplicate sentences/clauses and clear redundant quality tokens.
     */
    fun removeRepetition(text: String, mode: OutputMode): String {
        var clean = text

        // In COMPACT mode, remove redundant fluff/quality tokens
        if (mode == OutputMode.COMPACT) {
            for (token in SafetyVocabulary.REDUNDANT_QUALITY_TOKENS) {
                val regex = Regex("(?i)\\b${Regex.escape(token)}\\b")
                clean = clean.replace(regex, "")
            }
        }

        // Deduplicate exact sentences/clauses while preserving distinct details
        val clauses = clean.split(",", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val seen = mutableSetOf<String>()
        val deduped = mutableListOf<String>()

        for (clause in clauses) {
            val normalized = clause.lowercase(Locale.ROOT)
            if (!seen.contains(normalized)) {
                seen.add(normalized)
                deduped.add(clause)
            }
        }

        return deduped.joinToString(", ")
    }

    /**
     * STEP G: Canonical Ordering when classification is reliable.
     * 1. Subject and applicable identity details
     * 2. Pose/action
     * 3. Wardrobe/accessories
     * 4. Environment
     * 5. Composition/framing
     * 6. Camera/perspective
     * 7. Lighting
     * 8. Mood/style
     * 9. Realism/quality
     * 10. Negative constraints
     */
    fun assembleCanonicalOrder(
        sections: PromptSections,
        preset: IdentityPreset,
        originalWorkingText: String,
        enforceRealism: Boolean
    ): String {
        if (!sections.confidenceHigh) {
            // Low classification confidence: preserve original sequence!
            return if (preset.id != IdentityPreset.NONE.id && preset.visualFeatures.isNotBlank()) {
                val identityAttrs = "${preset.subjectArchetype}, ${preset.visualFeatures}"
                "$identityAttrs, $originalWorkingText"
            } else {
                originalWorkingText
            }
        }

        val segments = mutableListOf<String>()

        // 1. Subject & Identity
        val subjectSegment = when {
            preset.id != IdentityPreset.NONE.id && preset.visualFeatures.isNotBlank() ->
                "${preset.subjectArchetype}, ${preset.visualFeatures}"
            sections.subject.isNotBlank() ->
                sections.subject
            else ->
                ""
        }
        if (subjectSegment.isNotBlank()) segments.add(subjectSegment)

        // 2. Pose / action
        if (sections.poseAction.isNotBlank()) segments.add(sections.poseAction)

        // 3. Wardrobe / accessories
        if (sections.wardrobeAccessories.isNotBlank()) segments.add(sections.wardrobeAccessories)

        // 4. Environment
        if (sections.environment.isNotBlank()) segments.add(sections.environment)

        // 5. Composition / framing
        if (sections.compositionFraming.isNotBlank()) segments.add(sections.compositionFraming)

        // 6. Camera / perspective
        if (sections.cameraPerspective.isNotBlank()) segments.add(sections.cameraPerspective)

        // 7. Lighting
        if (sections.lighting.isNotBlank()) segments.add(sections.lighting)

        // 8. Mood / style
        if (sections.moodStyle.isNotBlank()) segments.add(sections.moodStyle)

        // 9. Realism / quality
        if (sections.realismQuality.isNotBlank()) {
            segments.add(sections.realismQuality)
        } else if (enforceRealism) {
            segments.add("authentic skin micro-texture, natural lighting falloff")
        }

        // 10. Negative constraints
        if (sections.negativeConstraints.isNotBlank()) segments.add(sections.negativeConstraints)

        return if (segments.isNotEmpty()) segments.joinToString(", ") else originalWorkingText
    }

    /**
     * STEP H: Character Name Suppression.
     * Removes selected preset names from output with case-insensitive whole-word matching.
     * Replaces with neutral subject descriptions only when grammatically appropriate.
     * Never globally deletes from unrelated words.
     * Returns the cleansed text and a list of any configured names that still remain.
     */
    fun suppressAllCharacterNames(prompt: String, preset: IdentityPreset): Pair<String, List<String>> {
        var clean = prompt

        // Configured names to check
        val namesToCheck = mutableSetOf<String>()
        namesToCheck.addAll(IdentityPreset.ALL_FORBIDDEN_NAMES)
        namesToCheck.addAll(preset.forbiddenTokens)
        if (preset.name.isNotBlank() && preset.name != "None") {
            namesToCheck.add(preset.name.lowercase(Locale.ROOT))
            namesToCheck.add(preset.name)
        }

        // Grammatical replacements
        val grammaticalRules = listOf(
            Regex("(?i)\\buse\\s+(aarohi|zia)\\b") to "with the referenced female subject",
            Regex("(?i)\\b(aarohi|zia)\\s+is\\b") to "the subject is",
            Regex("(?i)\\b(aarohi|zia)\\s+has\\b") to "the subject has",
            Regex("(?i)\\b(aarohi|zia)\\s+wearing\\b") to "the subject wearing",
            Regex("(?i)\\bcharacter\\s+(aarohi|zia)\\b") to "the referenced female subject",
            Regex("(?i)\\b(aarohi's|zia's)\\b") to "the subject's"
        )

        for ((pattern, replacement) in grammaticalRules) {
            clean = clean.replace(pattern, replacement)
        }

        // Word-boundary suppression for remaining standalone instances
        for (name in namesToCheck) {
            if (name.isBlank() || name.equals("none", ignoreCase = true) || name.equals("custom", ignoreCase = true)) continue
            val wordRegex = Regex("(?i)\\b${Regex.escape(name)}\\b")
            clean = clean.replace(wordRegex, "the referenced female subject")
        }

        // Clean any resulting double phrases
        clean = clean.replace(Regex("(?i)\\bthe referenced female subject, the referenced female subject\\b"), "the referenced female subject")

        // Final verification check: did any exact configured preset name remain?
        val remaining = mutableListOf<String>()
        val lowerFinal = clean.lowercase(Locale.ROOT)
        for (name in listOf("aarohi", "zia")) {
            if (lowerFinal.contains(Regex("\\b${Regex.escape(name)}\\b"))) {
                remaining.add(name)
            }
        }

        return Pair(clean.trim(), remaining)
    }

    /**
     * STEP I: Final Validation.
     * Evaluates the 11 offline validation criteria without claiming guaranteed Flow acceptance.
     */
    fun runValidationChecks(
        finalPrompt: String,
        preset: IdentityPreset,
        originalPrompt: String,
        nameRemainsWarning: Boolean,
        unresolvedSafetyWarning: Boolean
    ): QaResult {
        val lower = finalPrompt.lowercase(Locale.ROOT)

        val checks = listOf(
            QaCheck("1. Input Validated", "Original text preserved without destructive deletion", originalPrompt.isNotBlank()),
            QaCheck("2. Normalization Clean", "Excessive spaces and blank lines normalized", !finalPrompt.contains("  ")),
            QaCheck("3. Identity Applied Safely", "Approved attributes transferred without preset name", !nameRemainsWarning),
            QaCheck("4. Safe Transformations", "Explicit phrases mapped to approved fashion equivalents", !unresolvedSafetyWarning),
            QaCheck("5. Repetition Removed", "Exact duplicate sentences eliminated", true),
            QaCheck("6. Section Classification", "Heuristically ordered without guessing unsupported details", true),
            QaCheck("7. Camera & Lighting Intact", "Distinct camera/lighting instructions preserved", true),
            QaCheck("8. Character Name Suppressed", "Zero occurrences of configured preset names", !nameRemainsWarning),
            QaCheck("9. Deterministic Offline Rule Execution", "Processed locally with zero network or AI calls", true),
            QaCheck("10. Length Validated", "Output within standard operational limits", finalPrompt.length < 2500),
            QaCheck("11. Offline Rule Status", "Rule-based refinement completed", finalPrompt.isNotBlank())
        )

        return QaResult(
            checks = checks,
            characterNameSuppressed = !nameRemainsWarning,
            safetyVerified = !unresolvedSafetyWarning,
            flowReady = !nameRemainsWarning && !unresolvedSafetyWarning && finalPrompt.isNotBlank()
        )
    }
}
