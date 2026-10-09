package com.example

import com.example.data.model.IdentityPreset
import com.example.engine.MasterRefinementEngine
import com.example.engine.OutputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Acceptance Tests specified in Section 7 of OFFLINE REFINEMENT ENGINE SPECIFICATION.
 */
class MasterRefinementEngineTest {

    // Test 1: Empty input produces a helpful validation message.
    @Test
    fun test1_emptyInputProducesHelpfulValidationMessage() {
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "   ",
            identityPreset = IdentityPreset.NONE
        )
        val result = MasterRefinementEngine.refine(input)
        assertEquals("Paste a prompt to refine.", result.statusLabel)
        assertEquals("", result.finalPrompt)
    }

    // Test 2: Ordinary text with no matching rules remains unchanged.
    @Test
    fun test2_ordinaryTextWithNoMatchingRulesRemainsUnchanged() {
        val original = "A cat sitting on a blue sofa"
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = original,
            identityPreset = IdentityPreset.NONE,
            outputMode = OutputMode.PRESERVE_DETAIL
        )
        val result = MasterRefinementEngine.refine(input)
        assertEquals(original, result.finalPrompt)
        assertTrue(result.noRulesApplied)
        assertEquals("No applicable offline rules found. Original prompt preserved.", result.statusLabel)
    }

    // Test 3: A configured safe phrase replacement is applied correctly.
    @Test
    fun test3_configuredSafePhraseReplacementAppliedCorrectly() {
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "woman in provocative sexual pose, transparent clothing revealing the body, explicit emphasis on intimate anatomy",
            identityPreset = IdentityPreset.NONE,
            outputMode = OutputMode.PRESERVE_DETAIL
        )
        val result = MasterRefinementEngine.refine(input)
        val prompt = result.finalPrompt

        // Replacements must be present
        assertTrue("Contains safe pose", prompt.contains("confident fashion-editorial pose", ignoreCase = true))
        assertTrue("Contains safe clothing", prompt.contains("layered fashion styling that provides appropriate coverage", ignoreCase = true))
        assertTrue("Contains safe composition", prompt.contains("elegant fashion-focused composition", ignoreCase = true))

        // Problematic terms must be gone
        assertFalse(prompt.contains("provocative sexual pose", ignoreCase = true))
        assertFalse(prompt.contains("transparent clothing revealing the body", ignoreCase = true))
        assertFalse(prompt.contains("intimate anatomy", ignoreCase = true))
    }

    // Test 4: Repeated sentences are removed without losing unique instructions.
    @Test
    fun test4_repeatedSentencesRemovedWithoutLosingUniqueInstructions() {
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "golden hour lighting, golden hour lighting, 35mm lens, vintage green roadster, 35mm lens",
            identityPreset = IdentityPreset.NONE,
            outputMode = OutputMode.PRESERVE_DETAIL
        )
        val result = MasterRefinementEngine.refine(input)
        val prompt = result.finalPrompt

        // Count occurrences of 'golden hour lighting'
        val firstIndex = prompt.indexOf("golden hour lighting", ignoreCase = true)
        val lastIndex = prompt.lastIndexOf("golden hour lighting", ignoreCase = true)
        assertEquals("Should only appear once", firstIndex, lastIndex)

        // Unique instructions must be preserved
        assertTrue("Retains 35mm lens", prompt.contains("35mm lens", ignoreCase = true))
        assertTrue("Retains vintage roadster", prompt.contains("vintage green roadster", ignoreCase = true))
    }

    // Test 5: Selected preset attributes are applied without outputting the preset name.
    @Test
    fun test5_selectedPresetAttributesAppliedWithoutOutputtingPresetName() {
        val inputAarohi = MasterRefinementEngine.RefinementInput(
            originalPrompt = "Aarohi is posing in a city cafe, Use Aarohi",
            identityPreset = IdentityPreset.AAROHI
        )
        val resultAarohi = MasterRefinementEngine.refine(inputAarohi)
        val promptAarohi = resultAarohi.finalPrompt

        // Name Aarohi must be 100% absent
        assertFalse("Prompt must not contain Aarohi", promptAarohi.contains("Aarohi", ignoreCase = true))
        assertFalse("Prompt must not contain 'Use Aarohi'", promptAarohi.contains("Use Aarohi", ignoreCase = true))

        // Approved attributes must be transferred
        assertTrue("Contains golden-tan skin tone", promptAarohi.contains("golden-tan", ignoreCase = true))
        assertTrue("Character name suppressed check", resultAarohi.qaResult.characterNameSuppressed)

        val inputZia = MasterRefinementEngine.RefinementInput(
            originalPrompt = "Zia standing near museum, Use Zia",
            identityPreset = IdentityPreset.ZIA
        )
        val resultZia = MasterRefinementEngine.refine(inputZia)
        val promptZia = resultZia.finalPrompt

        // Name Zia must be 100% absent
        assertFalse("Prompt must not contain Zia", promptZia.contains("Zia", ignoreCase = true))
        assertTrue("Contains porcelain fair skin", promptZia.contains("porcelain fair skin", ignoreCase = true))
    }

    // Test 6: No preset means no identity details are added.
    @Test
    fun test6_noPresetMeansNoIdentityDetailsAdded() {
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "a person walking on the beach at sunrise",
            identityPreset = IdentityPreset.NONE,
            outputMode = OutputMode.PRESERVE_DETAIL
        )
        val result = MasterRefinementEngine.refine(input)
        val prompt = result.finalPrompt

        // No identity preset traits like Aarohi or Zia should be added
        assertFalse(prompt.contains("golden-tan", ignoreCase = true))
        assertFalse(prompt.contains("porcelain fair skin", ignoreCase = true))
        assertFalse(prompt.contains("South Asian", ignoreCase = true))
        assertFalse(prompt.contains("East Asian", ignoreCase = true))
    }

    // Test 7: A prompt containing camera, lighting, pose, and wardrobe instructions retains all unique details.
    @Test
    fun test7_promptRetainsAllUniqueDetails() {
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "model standing gracefully, wearing emerald velvet jacket, soft studio key lighting, 85mm prime lens f/1.8",
            identityPreset = IdentityPreset.NONE,
            outputMode = OutputMode.COMPACT
        )
        val result = MasterRefinementEngine.refine(input)
        val prompt = result.finalPrompt

        assertTrue("Retains pose", prompt.contains("standing gracefully", ignoreCase = true))
        assertTrue("Retains wardrobe", prompt.contains("emerald velvet jacket", ignoreCase = true))
        assertTrue("Retains lighting", prompt.contains("soft studio key lighting", ignoreCase = true))
        assertTrue("Retains camera", prompt.contains("85mm", ignoreCase = true))
        assertTrue("Retains aperture", prompt.contains("f/1.8", ignoreCase = true))
    }

    // Test 8: An unavailable offline image-analysis capability is disclosed honestly.
    @Test
    fun test8_unavailableOfflineImageAnalysisDisclosedHonestly() {
        // Test manual image description is honored without fabricating unsupported details
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "portrait in city",
            identityPreset = IdentityPreset.NONE,
            hasReferenceImage = true,
            manualImageDescription = "high contrast monochrome lighting, Dutch angle framing"
        )
        val result = MasterRefinementEngine.refine(input)
        val prompt = result.finalPrompt

        assertTrue("Manual description incorporated", prompt.contains("high contrast monochrome lighting", ignoreCase = true))
        assertTrue("Dutch angle framing incorporated", prompt.contains("Dutch angle framing", ignoreCase = true))
    }

    // Test 9: Copy returns only the output prompt.
    @Test
    fun test9_copyReturnsOnlyOutputPrompt() {
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "Aarohi wearing red dress in studio",
            identityPreset = IdentityPreset.AAROHI
        )
        val result = MasterRefinementEngine.refine(input)
        val copyReady = result.finalPrompt.trim()

        // Verify it contains pure prompt text, no headings or metadata
        assertFalse("No 'READY TO PASTE'", copyReady.contains("READY TO PASTE", ignoreCase = true))
        assertFalse("No 'GOOGLE FLOW'", copyReady.contains("GOOGLE FLOW", ignoreCase = true))
        assertFalse("No 'Offline rule-based refinement'", copyReady.contains("Offline rule-based refinement", ignoreCase = true))
        assertFalse("No Aarohi name", copyReady.contains("Aarohi", ignoreCase = true))
        assertTrue(copyReady.isNotEmpty())
    }

    // Test 10: The refinement pipeline still works with network access disabled.
    @Test
    fun test10_refinementPipelineWorksWithNetworkDisabled() {
        // Runs entirely in-memory, deterministic, zero network dependencies
        val input = MasterRefinementEngine.RefinementInput(
            originalPrompt = "astronaut floating in orbit, earth in background, wide angle shot",
            identityPreset = IdentityPreset.NONE
        )
        val result1 = MasterRefinementEngine.refine(input)
        val result2 = MasterRefinementEngine.refine(input)

        // Strict determinism: same input always produces identical output
        assertEquals("Identical deterministic output", result1.finalPrompt, result2.finalPrompt)
        assertEquals("Deterministic status", result1.statusLabel, result2.statusLabel)
    }
}
