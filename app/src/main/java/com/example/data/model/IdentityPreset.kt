package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Internal identity preset definition.
 *
 * CRITICAL HARD LOCK:
 * The [name] is an internal identifier ONLY.
 * It must NEVER be output in the refined prompt.
 */
@Entity(tableName = "identity_presets")
data class IdentityPreset(
    @PrimaryKey
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val subjectArchetype: String,       // e.g. "female subject", "woman"
    val visualFeatures: String,         // transferred visual characteristics
    val forbiddenTokens: List<String> = emptyList() // tokens strictly wiped from final output
) {
    companion object {
        val NONE = IdentityPreset(
            id = "none",
            name = "None",
            isBuiltIn = true,
            subjectArchetype = "",
            visualFeatures = "",
            forbiddenTokens = emptyList()
        )

        val AAROHI = IdentityPreset(
            id = "aarohi",
            name = "Aarohi",
            isBuiltIn = true,
            subjectArchetype = "the referenced South Asian female subject in her mid-20s",
            visualFeatures = "warm golden-tan skin tone with natural healthy texture, almond-shaped deep warm brown eyes, naturally arched well-defined eyebrows, delicate straight nose bridge, naturally contoured lips, softly sculpted jawline, long glossy dark wavy hair parted slightly off-center with natural volume, graceful poised facial symmetry",
            forbiddenTokens = listOf("aarohi", "Aarohi", "AAROHI")
        )

        val ZIA = IdentityPreset(
            id = "zia",
            name = "Zia",
            isBuiltIn = true,
            subjectArchetype = "the referenced East Asian female subject in her late-20s",
            visualFeatures = "luminous porcelain fair skin with neutral undertones and authentic skin micro-texture, expressive dark espresso eyes with gentle double eyelids, elegant soft eyebrows, slender nose bridge, softly defined natural lips, smooth refined oval jawline, sleek obsidian-black shoulder-length hair with clean straight texture, chic poised aesthetic",
            forbiddenTokens = listOf("zia", "Zia", "ZIA")
        )

        val DEFAULT_CUSTOM = IdentityPreset(
            id = "custom",
            name = "Custom",
            isBuiltIn = true,
            subjectArchetype = "the referenced subject",
            visualFeatures = "distinctive balanced facial proportions, natural authentic skin texture, sharp expressive eyes, tailored hair styling, cohesive aesthetic harmony",
            forbiddenTokens = listOf("custom", "Custom")
        )

        val BUILT_IN_PRESETS = listOf(NONE, AAROHI, ZIA, DEFAULT_CUSTOM)

        // Master list of all known character names that must NEVER appear in prompt output
        val ALL_FORBIDDEN_NAMES = setOf(
            "aarohi", "zia", "custom",
            "aarohi's", "zia's",
            "character", "preset"
        )
    }
}
