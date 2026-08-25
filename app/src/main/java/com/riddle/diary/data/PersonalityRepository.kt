package com.riddle.diary.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.personalityDataStore: DataStore<Preferences> by preferencesDataStore("personality")

data class PersonalityProfile(
    /** Display name only — keep product UI free of franchise branding. */
    val name: String = "The Diary",
    val customInstructions: String = DEFAULT_INSTRUCTIONS,
    val toneNotes: String = DEFAULT_TONE
) {
    companion object {
        /**
         * Sole place where the default persona is defined. Swap or clear these
         * instructions to change who the diary is; the rest of the app stays generic.
         */
        val DEFAULT_INSTRUCTIONS = """
You are Tom Marvolo Riddle as he appears in the enchanted diary — charming, articulate, curious, and quietly dangerous.
Speak in first person as Tom Riddle. Address the writer as if they have opened your diary.
Be conversational and intimate, as ink answering ink. Never break character as an AI.
Never mention being a language model, APIs, or modern technology.
Keep replies diary-length: a few thoughtful sentences unless asked for more.
""".trimIndent()

        val DEFAULT_TONE = """
Elegant mid-20th-century diction. Warm at first, probing, with an undertone of secrecy.
Prefer questions that draw secrets out. Sign nothing; you are the diary itself.
""".trimIndent()
    }
}

class PersonalityRepository(private val context: Context) {
    private val nameKey = stringPreferencesKey("name")
    private val instructionsKey = stringPreferencesKey("instructions")
    private val toneKey = stringPreferencesKey("tone")

    val profile: Flow<PersonalityProfile> = context.personalityDataStore.data.map { prefs ->
        PersonalityProfile(
            name = prefs[nameKey] ?: PersonalityProfile().name,
            customInstructions = prefs[instructionsKey] ?: PersonalityProfile.DEFAULT_INSTRUCTIONS,
            toneNotes = prefs[toneKey] ?: PersonalityProfile.DEFAULT_TONE
        )
    }

    suspend fun current(): PersonalityProfile = profile.first()

    suspend fun save(profile: PersonalityProfile) {
        context.personalityDataStore.edit { prefs ->
            prefs[nameKey] = profile.name
            prefs[instructionsKey] = profile.customInstructions
            prefs[toneKey] = profile.toneNotes
        }
    }

    suspend fun reset() {
        save(PersonalityProfile())
    }
}
