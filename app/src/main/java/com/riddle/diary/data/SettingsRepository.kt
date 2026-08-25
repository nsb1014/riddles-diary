package com.riddle.diary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

enum class DiaryMode { VANISHING, SCROLL }
enum class InputMode { HANDWRITE, TYPE }
enum class LlmProvider { OPENAI_COMPATIBLE, GEMINI }

data class AppSettings(
    val diaryMode: DiaryMode = DiaryMode.SCROLL,
    val inputMode: InputMode = InputMode.HANDWRITE,
    val provider: LlmProvider = LlmProvider.OPENAI_COMPATIBLE,
    val baseUrl: String = "https://api.openai.com/v1/",
    val model: String = DEFAULT_OPENAI_MODEL,
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val revealMillisPerChar: Int = 45
) {
    companion object {
        /** Balanced GPT-5.6 tier — good default for chat volume. */
        const val DEFAULT_OPENAI_MODEL = "gpt-5.6-terra"

        /** Current Google Gemini Flash workhorse. */
        const val DEFAULT_GEMINI_MODEL = "gemini-3.6-flash"

        private val RETIRED_OPENAI_MODELS = setOf(
            "gpt-4o-mini",
            "gpt-4o",
            "gpt-4.1-mini",
            "gpt-4.1",
            "gpt-5-mini",
            "gpt-5-nano"
        )

        private val RETIRED_GEMINI_MODELS = setOf(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-3.5-flash"
        )

        fun normalizeOpenAiModel(model: String?): String {
            val value = model?.trim().orEmpty()
            if (value.isEmpty() || value in RETIRED_OPENAI_MODELS) {
                return DEFAULT_OPENAI_MODEL
            }
            return value
        }

        fun normalizeGeminiModel(model: String?): String {
            val value = model?.trim().orEmpty()
            if (value.isEmpty() || value in RETIRED_GEMINI_MODELS) {
                return DEFAULT_GEMINI_MODEL
            }
            return value
        }
    }
}

class SettingsRepository(private val context: Context) {
    private val diaryModeKey = stringPreferencesKey("diary_mode")
    private val inputModeKey = stringPreferencesKey("input_mode")
    private val providerKey = stringPreferencesKey("provider")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val modelKey = stringPreferencesKey("model")
    private val geminiModelKey = stringPreferencesKey("gemini_model")
    private val revealKey = stringPreferencesKey("reveal_ms")
    private val stylusOnlyKey = booleanPreferencesKey("stylus_preferred")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            diaryMode = prefs[diaryModeKey]?.let { runCatching { DiaryMode.valueOf(it) }.getOrNull() }
                ?: DiaryMode.SCROLL,
            inputMode = prefs[inputModeKey]?.let { runCatching { InputMode.valueOf(it) }.getOrNull() }
                ?: InputMode.HANDWRITE,
            provider = prefs[providerKey]?.let { runCatching { LlmProvider.valueOf(it) }.getOrNull() }
                ?: LlmProvider.OPENAI_COMPATIBLE,
            baseUrl = prefs[baseUrlKey] ?: "https://api.openai.com/v1/",
            model = AppSettings.normalizeOpenAiModel(prefs[modelKey]),
            geminiModel = AppSettings.normalizeGeminiModel(prefs[geminiModelKey]),
            revealMillisPerChar = prefs[revealKey]?.toIntOrNull() ?: 45
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(current())
        context.settingsDataStore.edit { prefs ->
            prefs[diaryModeKey] = next.diaryMode.name
            prefs[inputModeKey] = next.inputMode.name
            prefs[providerKey] = next.provider.name
            prefs[baseUrlKey] = next.baseUrl
            prefs[modelKey] = AppSettings.normalizeOpenAiModel(next.model)
            prefs[geminiModelKey] = AppSettings.normalizeGeminiModel(next.geminiModel)
            prefs[revealKey] = next.revealMillisPerChar.toString()
        }
    }

    fun stylusPreferred(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[stylusOnlyKey] ?: true }

    suspend fun setStylusPreferred(preferred: Boolean) {
        context.settingsDataStore.edit { it[stylusOnlyKey] = preferred }
    }
}
