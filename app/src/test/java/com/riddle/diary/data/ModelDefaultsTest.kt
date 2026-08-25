package com.riddle.diary.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelDefaultsTest {
    @Test
    fun upgradesRetiredGeminiDefault() {
        assertEquals(
            AppSettings.DEFAULT_GEMINI_MODEL,
            AppSettings.normalizeGeminiModel("gemini-2.0-flash")
        )
    }

    @Test
    fun upgradesRetiredOpenAiDefault() {
        assertEquals(
            AppSettings.DEFAULT_OPENAI_MODEL,
            AppSettings.normalizeOpenAiModel("gpt-4o-mini")
        )
    }

    @Test
    fun keepsCurrentCustomModels() {
        assertEquals("gemini-3.6-pro", AppSettings.normalizeGeminiModel("gemini-3.6-pro"))
        assertEquals("gpt-5.6-sol", AppSettings.normalizeOpenAiModel("gpt-5.6-sol"))
    }
}
