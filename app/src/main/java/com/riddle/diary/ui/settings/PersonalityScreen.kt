package com.riddle.diary.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riddle.diary.DiaryApplication
import com.riddle.diary.data.PersonalityProfile
import com.riddle.diary.ui.components.ParchmentBackground
import com.riddle.diary.ui.diary.DiaryViewModel
import com.riddle.diary.ui.theme.BloodInk
import com.riddle.diary.ui.theme.GoldFiligree
import com.riddle.diary.ui.theme.InkBlack
import com.riddle.diary.ui.theme.InkSepia
import com.riddle.diary.ui.theme.Leather
import kotlinx.coroutines.launch

/**
 * Gemini-Gems-style custom instructions layer for Tom Riddle's personality.
 */
@Composable
fun PersonalityScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val app = LocalContext.current.applicationContext as DiaryApplication
    val scope = rememberCoroutineScope()

    var name by remember(state.personality.name) { mutableStateOf(state.personality.name) }
    var instructions by remember(state.personality.customInstructions) {
        mutableStateOf(state.personality.customInstructions)
    }
    var tone by remember(state.personality.toneNotes) {
        mutableStateOf(state.personality.toneNotes)
    }
    var saved by remember { mutableStateOf(false) }

    PersonalityScreenContent(
        name = name,
        instructions = instructions,
        tone = tone,
        saved = saved,
        onNameChange = { name = it },
        onInstructionsChange = { instructions = it },
        onToneChange = { tone = it },
        onBack = onBack,
        onSave = {
            scope.launch {
                app.container.personalityRepository.save(
                    PersonalityProfile(
                        name = name.trim().ifBlank { "Tom Riddle" },
                        customInstructions = instructions.trim(),
                        toneNotes = tone.trim()
                    )
                )
                saved = true
            }
        },
        onReset = {
            scope.launch {
                app.container.personalityRepository.reset()
                name = PersonalityProfile().name
                instructions = PersonalityProfile.DEFAULT_INSTRUCTIONS
                tone = PersonalityProfile.DEFAULT_TONE
                saved = true
            }
        }
    )
}

@Composable
fun PersonalityScreenContent(
    name: String,
    instructions: String,
    tone: String,
    saved: Boolean = false,
    onNameChange: (String) -> Unit = {},
    onInstructionsChange: (String) -> Unit = {},
    onToneChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) {
        ParchmentBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = InkBlack)
                }
                Text("Personality layer", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Like Gemini custom Gems: these instructions shape how the diary speaks. " +
                    "They are prepended as the system personality for every reply.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSepia
            )
            Spacer(Modifier.height(16.dp))

            PersonalityField(name, onNameChange, "Identity name", singleLine = true)
            PersonalityField(
                instructions,
                onInstructionsChange,
                "Custom instructions",
                minLines = 8
            )
            PersonalityField(
                tone,
                onToneChange,
                "Tone & style notes",
                minLines = 4
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = Leather, contentColor = GoldFiligree),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save personality")
            }
            TextButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset to Tom Riddle defaults", color = BloodInk)
            }
            if (saved) {
                Text("Personality sealed.", color = BloodInk, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PersonalityField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        singleLine = singleLine,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldFiligree,
            unfocusedBorderColor = InkSepia.copy(alpha = 0.4f),
            focusedLabelColor = BloodInk,
            cursorColor = BloodInk,
            focusedTextColor = InkBlack,
            unfocusedTextColor = InkBlack
        )
    )
}
