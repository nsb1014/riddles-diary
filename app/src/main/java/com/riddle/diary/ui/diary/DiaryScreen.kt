package com.riddle.diary.ui.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riddle.diary.data.DiaryEntry
import com.riddle.diary.data.DiaryMode
import com.riddle.diary.data.InputMode
import com.riddle.diary.ui.components.ParchmentBackground
import com.riddle.diary.ui.theme.BloodInk
import com.riddle.diary.ui.theme.CursiveFamily
import com.riddle.diary.ui.theme.GoldFiligree
import com.riddle.diary.ui.theme.HandFamily
import com.riddle.diary.ui.theme.InkBlack
import com.riddle.diary.ui.theme.InkSepia
import com.riddle.diary.ui.theme.Leather

data class DiaryScreenActions(
    val onMode: (DiaryMode) -> Unit = {},
    val onInput: (InputMode) -> Unit = {},
    val onSettings: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onExportPdf: () -> Unit = {},
    val onClearInk: () -> Unit = {},
    val onSend: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onTypedDraftChange: (String) -> Unit = {},
    val onCanvasSize: (Float, Float) -> Unit = { _, _ -> },
    val onStrokeCompleted: (InkStroke) -> Unit = {}
)

@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    DiaryScreenContent(
        state = state,
        actions = DiaryScreenActions(
            onMode = viewModel::setDiaryMode,
            onInput = viewModel::setInputMode,
            onSettings = {
                viewModel.refreshKeyStatus()
                onOpenSettings()
            },
            onClear = viewModel::clearConversation,
            onExportPdf = {
                viewModel.exportScrollConversationPdf(context)
            },
            onClearInk = viewModel::clearInk,
            onSend = viewModel::send,
            onDismissError = viewModel::dismissError,
            onTypedDraftChange = viewModel::onTypedDraftChange,
            onCanvasSize = viewModel::onCanvasSize,
            onStrokeCompleted = viewModel::addCompletedStroke
        )
    )
}

@Composable
fun DiaryScreenContent(
    state: DiaryUiState,
    actions: DiaryScreenActions = DiaryScreenActions()
) {
    val scroll = rememberScrollState()

    LaunchedEffect(state.entries.size, state.revealingText) {
        if (state.settings.diaryMode == DiaryMode.SCROLL) {
            scroll.animateScrollTo(scroll.maxValue)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ParchmentBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            DiaryTopBar(
                title = state.personality.name.ifBlank { "The Diary" },
                mode = state.settings.diaryMode,
                inputMode = state.settings.inputMode,
                hasKey = state.hasApiKey,
                canExport = state.settings.diaryMode == DiaryMode.SCROLL && state.entries.isNotEmpty(),
                onMode = actions.onMode,
                onInput = actions.onInput,
                onSettings = actions.onSettings,
                onClear = actions.onClear,
                onExportPdf = actions.onExportPdf
            )

            when (state.settings.diaryMode) {
                DiaryMode.SCROLL -> ScrollCanvasMode(
                    state = state,
                    scrollState = scroll,
                    actions = actions,
                    modifier = Modifier.weight(1f)
                )
                DiaryMode.VANISHING -> VanishingMode(
                    state = state,
                    actions = actions,
                    modifier = Modifier.weight(1f)
                )
            }

            DiaryActionBar(
                state = state,
                onClearInk = actions.onClearInk,
                onSend = actions.onSend
            )
        }
    }
}

@Composable
private fun DiaryTopBar(
    title: String,
    mode: DiaryMode,
    inputMode: InputMode,
    hasKey: Boolean,
    canExport: Boolean,
    onMode: (DiaryMode) -> Unit,
    onInput: (InputMode) -> Unit,
    onSettings: () -> Unit,
    onClear: () -> Unit,
    onExportPdf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
                    color = InkBlack
                )
                Text(
                    text = if (hasKey) "The diary listens…" else "Bind an API key to awaken it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSepia.copy(alpha = 0.8f)
                )
            }
            if (canExport) {
                IconButton(onClick = onExportPdf) {
                    Icon(
                        Icons.Outlined.IosShare,
                        contentDescription = "Export conversation as PDF",
                        tint = GoldFiligree
                    )
                }
            }
            IconButton(onClick = onClear) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Clear diary", tint = InkSepia)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = GoldFiligree)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ModeChip("Scroll", mode == DiaryMode.SCROLL) { onMode(DiaryMode.SCROLL) }
            ModeChip("Vanish", mode == DiaryMode.VANISHING) { onMode(DiaryMode.VANISHING) }
            Spacer(Modifier.width(8.dp))
            ModeChip("S Pen", inputMode == InputMode.HANDWRITE) { onInput(InputMode.HANDWRITE) }
            ModeChip("Type", inputMode == InputMode.TYPE) { onInput(InputMode.TYPE) }
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) BloodInk else InkSepia.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ScrollCanvasMode(
    state: DiaryUiState,
    scrollState: androidx.compose.foundation.ScrollState,
    actions: DiaryScreenActions,
    modifier: Modifier = Modifier
) {
    // Writing pad stays fixed (not inside the scroll) so S Pen isn't stolen.
    // Pad is pinned near the top so Scroll and Vanish share the same input position.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        InputSurface(
            state = state,
            actions = actions,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            state.entries.forEach { entry ->
                val isLiveReveal =
                    entry.role == DiaryEntry.Role.DIARY &&
                        entry == state.entries.lastOrNull() &&
                        !state.revealComplete
                val text = when {
                    isLiveReveal -> state.revealingText
                    entry.role == DiaryEntry.Role.DIARY &&
                        entry == state.entries.lastOrNull() &&
                        state.revealingText.isNotEmpty() -> state.revealingText
                    else -> entry.text
                }
                DiaryMessage(
                    entry = entry.copy(text = text),
                    opacity = 1f
                )
                Spacer(Modifier.height(18.dp))
            }

            if (state.isThinking) {
                ThinkingInk()
                Spacer(Modifier.height(18.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VanishingMode(
    state: DiaryUiState,
    actions: DiaryScreenActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        InputSurface(
            state = state,
            actions = actions,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .alpha(if (state.revealComplete && !state.isThinking) 1f else 0.35f)
        )
        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .alpha(state.vanishingOpacity)
        ) {
            when {
                state.isThinking -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        ThinkingInk()
                    }
                }
                state.revealingText.isNotEmpty() -> {
                    Text(
                        text = state.revealingText,
                        style = TextStyle(
                            fontFamily = CursiveFamily,
                            fontSize = 34.sp,
                            color = BloodInk,
                            lineHeight = 42.sp
                        ),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, end = 12.dp)
                    )
                }
                else -> {
                    Text(
                        text = "Write… then watch the ink answer, and vanish.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSepia.copy(alpha = 0.45f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InputSurface(
    state: DiaryUiState,
    actions: DiaryScreenActions,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, GoldFiligree.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        when (state.settings.inputMode) {
            InputMode.HANDWRITE -> {
                HandwritingCanvas(
                    strokes = state.strokes,
                    enabled = state.revealComplete && !state.isThinking,
                    onSize = actions.onCanvasSize,
                    onStrokeCompleted = actions.onStrokeCompleted
                )
                if (state.strokes.isEmpty()) {
                    Text(
                        text = "Write with S Pen…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSepia.copy(alpha = 0.35f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    )
                }
                if (state.recognizedPreview.isNotEmpty()) {
                    Text(
                        text = state.recognizedPreview,
                        style = MaterialTheme.typography.bodyMedium.copy(color = InkSepia.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }
            }
            InputMode.TYPE -> {
                BasicTextField(
                    value = state.typedDraft,
                    onValueChange = actions.onTypedDraftChange,
                    enabled = state.revealComplete && !state.isThinking,
                    textStyle = TextStyle(
                        fontFamily = HandFamily,
                        fontSize = 24.sp,
                        color = InkBlack,
                        lineHeight = 32.sp
                    ),
                    cursorBrush = SolidColor(BloodInk),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    decorationBox = { inner ->
                        if (state.typedDraft.isEmpty()) {
                            Text(
                                "Type your words…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSepia.copy(alpha = 0.35f)
                            )
                        }
                        inner()
                    }
                )
            }
        }
    }
}

@Composable
private fun DiaryMessage(entry: DiaryEntry, opacity: Float) {
    val isDiary = entry.role == DiaryEntry.Role.DIARY
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(opacity)
            .padding(start = if (isDiary) 8.dp else 28.dp, end = if (isDiary) 28.dp else 8.dp)
    ) {
        Text(
            text = if (isDiary) "the diary" else "you",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
            color = if (isDiary) BloodInk.copy(alpha = 0.7f) else GoldFiligree.copy(alpha = 0.7f)
        )
        Text(
            text = entry.text,
            style = TextStyle(
                fontFamily = if (isDiary) CursiveFamily else HandFamily,
                fontSize = if (isDiary) 32.sp else 22.sp,
                color = if (isDiary) BloodInk else InkBlack,
                lineHeight = if (isDiary) 40.sp else 30.sp
            )
        )
    }
}

@Composable
private fun ThinkingInk() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier
                .height(18.dp)
                .width(18.dp),
            strokeWidth = 2.dp,
            color = BloodInk
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Ink gathers…",
            style = TextStyle(fontFamily = CursiveFamily, fontSize = 28.sp, color = BloodInk)
        )
    }
}

@Composable
private fun DiaryActionBar(
    state: DiaryUiState,
    onClearInk: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = state.error.orEmpty(),
                color = BloodInk,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.settings.inputMode == InputMode.HANDWRITE) {
                TextButton(onClick = onClearInk, enabled = !state.isThinking) {
                    Text("Clear ink", color = InkSepia)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            TextButton(
                onClick = onSend,
                enabled = !state.isThinking && state.revealComplete && !state.isRecognizing
            ) {
                Text(
                    text = when {
                        state.isRecognizing -> "Reading ink…"
                        state.isThinking -> "Waiting…"
                        else -> "Seal & Send"
                    },
                    color = Leather,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
                )
            }
        }
    }
}
