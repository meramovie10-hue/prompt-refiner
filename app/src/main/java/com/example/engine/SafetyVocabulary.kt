package com.example.engine

/**
 * Safety vocabulary & visual transformation mappings for offline refinement.
 *
 * Implements Step E (Safe Text Transformations):
 * - Explicit, maintainable dictionary of approved phrase replacements.
 * - Word boundary matching only (never corrupting unrelated substrings).
 * - Safe aesthetic fashion and photographic rewrites.
 */
object SafetyVocabulary {

    // Explicit dictionary of approved phrase replacements
    val PHRASE_REPLACEMENTS = listOf(
        "provocative sexual pose" to "confident fashion-editorial pose",
        "sexually explicit pose" to "confident editorial pose",
        "explicit emphasis on intimate anatomy" to "elegant fashion-focused composition",
        "emphasis on intimate anatomy" to "tasteful balanced silhouette composition",
        "transparent clothing revealing the body" to "layered fashion styling that provides appropriate coverage",
        "see-through clothing" to "delicate layered translucent fabric detailing",
        "sheer clothing" to "sophisticated draped fabric ensemble",
        "scantily clad" to "tailored minimalist contemporary resortwear",
        "scanty clothes" to "chic modern minimalist attire",
        "erotic pose" to "poised fashion model stance",
        "erotic expression" to "enigmatic charismatic expression",
        "seductive look" to "captivating magnetic gaze",
        "seductive gaze" to "expressive confident gaze",
        "sexy pose" to "graceful editorial stance",
        "revealing outfit" to "tailored high-fashion silhouette",
        "deep cleavage" to "elegant tailored neckline",
        "bare chest" to "tailored open-collar styling",
        "bare body" to "sculptural artistic silhouette",
        "in the nude" to "in minimalist contemporary tailoring",
        "without clothes" to "in understated high-end wardrobe",
        "underwear only" to "tailored loungewear apparel",
        "lingerie" to "fine silk draped lounge garment",
        "bikini model" to "contemporary swimwear editorial portrait",
        "gore and blood" to "dramatic atmospheric chiaroscuro lighting",
        "blood splatters" to "high-contrast cinematic shadow accents",
        "mutilated" to "dramatic textured elements",
        "violent scene" to "tense cinematic composition",
        "weapon pointed at" to "dramatic focused hand gesture",
        "illegal substances" to "atmospheric vintage prop styling"
    )

    // Single-word safety replacements (matched strictly on \b word boundaries)
    val WORD_REPLACEMENTS = mapOf(
        "nude" to "elegantly draped",
        "naked" to "tailored",
        "topless" to "contemporary tailored attire",
        "nsfw" to "clean editorial",
        "pornographic" to "aesthetic editorial",
        "erotic" to "charismatic",
        "sensual" to "alluring",
        "seductive" to "poised",
        "sexy" to "striking",
        "lustful" to "expressive",
        "provocative" to "fashion-forward",
        "horrific" to "dramatic",
        "grotesque" to "striking sculptural",
        "slaughter" to "cinematic action",
        "corpse" to "still figure",
        "gruesome" to "somber"
    )

    // Redundant quality tokens to clean up when in COMPACT mode
    val REDUNDANT_QUALITY_TOKENS = listOf(
        "the ai should generate",
        "the ai should create",
        "the ai should",
        "ai should",
        "please generate",
        "please create",
        "make sure to include",
        "make sure to",
        "make sure that",
        "make sure",
        "ensure that",
        "generate an image of",
        "generate a picture of",
        "generate a photo of",
        "create an image of",
        "create a photo of",
        "hyperrealistic",
        "photorealistic 8k",
        "8k resolution",
        "4k resolution",
        "trending on artstation",
        "unreal engine 5",
        "octane render",
        "masterpiece",
        "best quality",
        "highly detailed",
        "award winning photo",
        "extremely detailed"
    )
}
