package com.riddle.diary.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riddle.diary.data.LlmProvider
import com.riddle.diary.ui.components.ParchmentBackground
import com.riddle.diary.ui.diary.DiaryViewModel
import com.riddle.diary.ui.theme.BloodInk
import com.riddle.diary.ui.theme.GoldFiligree
import com.riddle.diary.ui.theme.InkBlack
import com.riddle.diary.ui.theme.InkSepia
import com.riddle.diary.ui.theme.Leather
import com.riddle.diary.ui.theme.ParchmentDark
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: DiaryViewModel,
    onBack: () -> Unit,
    onOpenPersonality: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val container = rememberContainer()

    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var baseUrl by remember(state.settings.baseUrl) { mutableStateOf(state.settings.baseUrl) }
    var model by remember(state.settings.model) { mutableStateOf(state.settings.model) }
    var geminiModel by remember(state.settings.geminiModel) {
        mutableStateOf(state.settings.geminiModel)
    }
    var revealMs by remember(state.settings.revealMillisPerChar) {
        mutableFloatStateOf(state.settings.revealMillisPerChar.toFloat())
    }
    var status by remember { mutableStateOf<String?>(null) }

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
                Text("Awaken the diary", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "API keys are stored with EncryptedSharedPreferences on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSepia
            )
            Spacer(Modifier.height(16.dp))

            Text("Provider", style = MaterialTheme.typography.titleLarge, color = InkBlack)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProviderChip("OpenAI-compatible", state.settings.provider == LlmProvider.OPENAI_COMPATIBLE) {
                    scope.launch {
                        container.settingsRepository.update { it.copy(provider = LlmProvider.OPENAI_COMPATIBLE) }
                    }
                }
                ProviderChip("Gemini", state.settings.provider == LlmProvider.GEMINI) {
                    scope.launch {
                        container.settingsRepository.update { it.copy(provider = LlmProvider.GEMINI) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            DiaryField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = if (state.hasApiKey) "API key (saved — enter to replace)" else "API key",
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password
            )
            Row {
                TextButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "Hide" else "Show", color = GoldFiligree)
                }
                TextButton(
                    onClick = {
                        container.secureKeyStore.clearApiKey()
                        viewModel.refreshKeyStatus()
                        status = "Key cleared."
                    }
                ) {
                    Text("Clear key", color = BloodInk)
                }
            }

            if (state.settings.provider == LlmProvider.OPENAI_COMPATIBLE) {
                DiaryField(baseUrl, { baseUrl = it }, "Base URL (OpenAI / compatible)")
                DiaryField(model, { model = it }, "Model")
            } else {
                DiaryField(geminiModel, { geminiModel = it }, "Gemini model")
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Ink reveal speed: ${revealMs.toInt()} ms / character",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSepia
            )
            Slider(
                value = revealMs,
                onValueChange = { revealMs = it },
                valueRange = 20f..90f
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        if (apiKey.isNotBlank()) {
                            container.secureKeyStore.saveApiKey(apiKey)
                            apiKey = ""
                        }
                        container.settingsRepository.update {
                            it.copy(
                                baseUrl = baseUrl.trim(),
                                model = model.trim(),
                                geminiModel = geminiModel.trim(),
                                revealMillisPerChar = revealMs.toInt()
                            )
                        }
                        viewModel.refreshKeyStatus()
                        status = "Settings sealed into the diary."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Leather, contentColor = GoldFiligree),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenPersonality,
                colors = ButtonDefaults.buttonColors(containerColor = ParchmentDark, contentColor = InkBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Personality layer (custom instructions)")
            }

            status?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = BloodInk, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ProviderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Leather,
            selectedLabelColor = GoldFiligree,
            containerColor = ParchmentDark,
            labelColor = InkSepia
        )
    )
}

@Composable
private fun DiaryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

@Composable
private fun rememberContainer(): com.riddle.diary.AppContainer {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as com.riddle.diary.DiaryApplication
    return remember { app.container }
}
