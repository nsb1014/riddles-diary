package com.riddle.diary.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRatio
import com.android.resources.ScreenSize
import com.riddle.diary.data.AppSettings
import com.riddle.diary.data.DiaryEntry
import com.riddle.diary.data.DiaryMode
import com.riddle.diary.data.InputMode
import com.riddle.diary.data.PersonalityProfile
import com.riddle.diary.ui.diary.DiaryScreenContent
import com.riddle.diary.ui.diary.DiaryUiState
import com.riddle.diary.ui.settings.PersonalityScreenContent
import com.riddle.diary.ui.theme.DiaryTheme
import com.riddle.diary.ui.theme.Parchment
import org.junit.Rule
import org.junit.Test

/**
 * Renders real Compose UI for walkthrough screenshots.
 */
class UiScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 1440,
            screenHeight = 3120,
            density = Density.XXXHIGH,
            locale = "en",
            ratio = ScreenRatio.NOTLONG,
            size = ScreenSize.NORMAL
        ),
        theme = "android:Theme.Material.Light.NoActionBar",
        maxPercentDifference = 0.01
    )

    private val userInput = "Hello. Who writes back from these pages?"
    private val diaryOutput =
        "I am the diary. Ink and memory, nothing more. " +
            "You have opened me. That is rarely accidental. Tell me your name."

    @Test
    fun diary_scroll_mode_test_input_output() {
        val state = DiaryUiState(
            settings = AppSettings(
                diaryMode = DiaryMode.SCROLL,
                inputMode = InputMode.TYPE
            ),
            personality = PersonalityProfile(name = "The Diary"),
            entries = listOf(
                DiaryEntry(id = "1", role = DiaryEntry.Role.USER, text = userInput),
                DiaryEntry(id = "2", role = DiaryEntry.Role.DIARY, text = diaryOutput)
            ),
            typedDraft = "",
            revealingText = diaryOutput,
            revealComplete = true,
            hasApiKey = true
        )
        paparazzi.snapshot(name = "diary_test_io") {
            DiaryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Parchment) {
                    DiaryScreenContent(state = state)
                }
            }
        }
    }

    @Test
    fun personality_custom_instructions() {
        paparazzi.snapshot(name = "personality_custom_instructions") {
            DiaryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Parchment) {
                    PersonalityScreenContent(
                        name = "The Diary",
                        instructions = PersonalityProfile.DEFAULT_INSTRUCTIONS,
                        tone = PersonalityProfile.DEFAULT_TONE,
                        saved = false
                    )
                }
            }
        }
    }

    @Test
    fun diary_scroll_input_position() {
        paparazzi.snapshot(name = "diary_scroll_input_position") {
            DiaryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Parchment) {
                    DiaryScreenContent(
                        state = DiaryUiState(
                            settings = AppSettings(
                                diaryMode = DiaryMode.SCROLL,
                                inputMode = InputMode.TYPE
                            ),
                            personality = PersonalityProfile(name = "The Diary"),
                            hasApiKey = true,
                            revealComplete = true
                        )
                    )
                }
            }
        }
    }

    @Test
    fun diary_vanish_input_position() {
        paparazzi.snapshot(name = "diary_vanish_input_position") {
            DiaryTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Parchment) {
                    DiaryScreenContent(
                        state = DiaryUiState(
                            settings = AppSettings(
                                diaryMode = DiaryMode.VANISHING,
                                inputMode = InputMode.TYPE
                            ),
                            personality = PersonalityProfile(name = "The Diary"),
                            hasApiKey = true,
                            revealComplete = true
                        )
                    )
                }
            }
        }
    }
}
