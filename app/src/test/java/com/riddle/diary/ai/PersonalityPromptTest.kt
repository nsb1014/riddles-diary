package com.riddle.diary.ai

import com.riddle.diary.data.PersonalityProfile
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityPromptTest {
    @Test
    fun defaultInstructionsStayInCharacter() {
        val profile = PersonalityProfile()
        assertTrue(profile.customInstructions.contains("Tom Marvolo Riddle"))
        assertTrue(profile.customInstructions.contains("Never break character"))
        assertTrue(profile.toneNotes.contains("diary"))
    }

    @Test
    fun customInstructionsCanOverrideIdentityName() {
        val custom = PersonalityProfile(
            name = "The Diary",
            customInstructions = "Speak only in riddles.",
            toneNotes = "Cryptic."
        )
        assertTrue(custom.name == "The Diary")
        assertTrue(custom.customInstructions.contains("riddles"))
    }
}
