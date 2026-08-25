package com.riddle.diary.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.riddle.diary.AppContainer
import com.riddle.diary.data.AppSettings
import com.riddle.diary.data.DiaryEntry
import com.riddle.diary.data.DiaryMode
import com.riddle.diary.data.InputMode
import com.riddle.diary.data.PersonalityProfile
import com.riddle.diary.util.ConversationPdfExporter
import com.riddle.diary.util.InkRecognizer
import android.content.Context
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val t: Long,
    val fromStylus: Boolean
)

data class InkStroke(val points: List<StrokePoint>)

data class DiaryUiState(
    val settings: AppSettings = AppSettings(),
    val personality: PersonalityProfile = PersonalityProfile(),
    val entries: List<DiaryEntry> = emptyList(),
    val strokes: List<InkStroke> = emptyList(),
    val typedDraft: String = "",
    val recognizedPreview: String = "",
    val isThinking: Boolean = false,
    val isRecognizing: Boolean = false,
    val revealingText: String = "",
    val revealComplete: Boolean = true,
    val vanishingOpacity: Float = 1f,
    val error: String? = null,
    val hasApiKey: Boolean = false,
    val canvasWidth: Float = 1f,
    val canvasHeight: Float = 1f
)

class DiaryViewModel(
    private val container: AppContainer,
    private val inkRecognizer: InkRecognizer
) : ViewModel() {

    private val _ui = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _ui.asStateFlow()

    private var revealJob: Job? = null
    private var vanishJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                container.settingsRepository.settings,
                container.personalityRepository.profile,
                container.conversationRepository.entries
            ) { settings, personality, entries ->
                Triple(settings, personality, entries)
            }.collect { (settings, personality, entries) ->
                _ui.update {
                    it.copy(
                        settings = settings,
                        personality = personality,
                        entries = entries,
                        hasApiKey = container.secureKeyStore.hasApiKey()
                    )
                }
            }
        }
    }

    fun refreshKeyStatus() {
        _ui.update { it.copy(hasApiKey = container.secureKeyStore.hasApiKey()) }
    }

    fun setDiaryMode(mode: DiaryMode) {
        viewModelScope.launch {
            container.settingsRepository.update { it.copy(diaryMode = mode) }
        }
    }

    fun setInputMode(mode: InputMode) {
        viewModelScope.launch {
            container.settingsRepository.update { it.copy(inputMode = mode) }
        }
    }

    fun onTypedDraftChange(value: String) {
        _ui.update { it.copy(typedDraft = value, error = null) }
    }

    fun onCanvasSize(width: Float, height: Float) {
        _ui.update { it.copy(canvasWidth = width, canvasHeight = height) }
    }

    fun addCompletedStroke(stroke: InkStroke) {
        if (stroke.points.isEmpty()) return
        _ui.update {
            it.copy(strokes = it.strokes + stroke, error = null)
        }
    }

    fun clearInk() {
        _ui.update {
            it.copy(strokes = emptyList(), recognizedPreview = "")
        }
    }

    fun dismissError() {
        _ui.update { it.copy(error = null) }
    }

    fun exportScrollConversationPdf(context: Context) {
        val state = _ui.value
        if (state.settings.diaryMode != DiaryMode.SCROLL) {
            _ui.update { it.copy(error = "Switch to Scroll mode to export the conversation.") }
            return
        }
        if (state.entries.isEmpty()) {
            _ui.update { it.copy(error = "Nothing to export yet — write a little first.") }
            return
        }
        val title = state.personality.name.ifBlank { "The Diary" }
        val entries = state.entries
        viewModelScope.launch {
            runCatching {
                val pdf = withContext(Dispatchers.IO) {
                    ConversationPdfExporter.writePdf(
                        context = context.applicationContext,
                        title = title,
                        entries = entries
                    )
                }
                ConversationPdfExporter.sharePdf(context, pdf, title)
            }.onFailure { e ->
                _ui.update {
                    it.copy(error = e.message ?: "Could not export the conversation.")
                }
            }
        }
    }

    fun send() {
        val state = _ui.value
        if (state.isThinking || !state.revealComplete) return
        viewModelScope.launch {
            val message = when (state.settings.inputMode) {
                InputMode.TYPE -> state.typedDraft.trim()
                InputMode.HANDWRITE -> recognizeCurrentInk()
            }
            if (message.isBlank()) {
                _ui.update { it.copy(error = "The page waits for your words.") }
                return@launch
            }
            submitMessage(message)
        }
    }

    private suspend fun recognizeCurrentInk(): String {
        val state = _ui.value
        val allStrokes = state.strokes
        if (allStrokes.isEmpty()) return ""
        _ui.update { it.copy(isRecognizing = true, error = null) }
        return try {
            val ink = buildInk(allStrokes)
            val text = inkRecognizer.recognize(ink, state.canvasWidth, state.canvasHeight)
            _ui.update { it.copy(recognizedPreview = text, isRecognizing = false) }
            text
        } catch (e: Exception) {
            _ui.update {
                it.copy(
                    isRecognizing = false,
                    error = e.message ?: "Could not read the ink."
                )
            }
            ""
        }
    }

    private suspend fun submitMessage(message: String) {
        val priorHistory = _ui.value.entries
        val userEntry = DiaryEntry(
            id = UUID.randomUUID().toString(),
            role = DiaryEntry.Role.USER,
            text = message,
            vanishing = _ui.value.settings.diaryMode == DiaryMode.VANISHING
        )
        container.conversationRepository.append(userEntry)
        _ui.update {
            it.copy(
                typedDraft = "",
                strokes = emptyList(),
                recognizedPreview = "",
                isThinking = true,
                error = null,
                vanishingOpacity = 1f
            )
        }

        val result = container.llmClient.reply(priorHistory, message)
        _ui.update { it.copy(isThinking = false) }

        result.onSuccess { reply ->
            val diaryEntry = DiaryEntry(
                id = UUID.randomUUID().toString(),
                role = DiaryEntry.Role.DIARY,
                text = reply,
                vanishing = _ui.value.settings.diaryMode == DiaryMode.VANISHING
            )
            // In vanishing mode we still store briefly for context, then fade UI.
            container.conversationRepository.append(diaryEntry)
            revealText(reply)
        }.onFailure { e ->
            _ui.update { it.copy(error = e.message ?: "The diary would not answer.") }
        }
    }

    private fun revealText(full: String) {
        revealJob?.cancel()
        vanishJob?.cancel()
        val perChar = _ui.value.settings.revealMillisPerChar.coerceIn(15, 120)
        revealJob = viewModelScope.launch {
            _ui.update { it.copy(revealingText = "", revealComplete = false, vanishingOpacity = 1f) }
            val builder = StringBuilder()
            for (ch in full) {
                builder.append(ch)
                _ui.update { it.copy(revealingText = builder.toString()) }
                delay(perChar.toLong())
            }
            _ui.update { it.copy(revealComplete = true, revealingText = full) }
            if (_ui.value.settings.diaryMode == DiaryMode.VANISHING) {
                startVanishing()
            }
        }
    }

    private fun startVanishing() {
        vanishJob?.cancel()
        vanishJob = viewModelScope.launch {
            delay(2200)
            var opacity = 1f
            while (opacity > 0f) {
                opacity = (opacity - 0.04f).coerceAtLeast(0f)
                _ui.update { it.copy(vanishingOpacity = opacity) }
                delay(40)
            }
            // Clear conversation visually and from storage for true vanishing.
            container.conversationRepository.clear()
            _ui.update {
                it.copy(
                    revealingText = "",
                    vanishingOpacity = 1f,
                    strokes = emptyList(),
                    typedDraft = ""
                )
            }
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            revealJob?.cancel()
            vanishJob?.cancel()
            container.conversationRepository.clear()
            _ui.update {
                it.copy(
                    revealingText = "",
                    revealComplete = true,
                    vanishingOpacity = 1f,
                    strokes = emptyList(),
                    typedDraft = "",
                    error = null
                )
            }
        }
    }

    override fun onCleared() {
        inkRecognizer.close()
        super.onCleared()
    }

    private fun buildInk(strokes: List<InkStroke>): Ink {
        val builder = Ink.builder()
        strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val strokeBuilder = Ink.Stroke.builder()
            stroke.points.forEach { p ->
                strokeBuilder.addPoint(Ink.Point.create(p.x, p.y, p.t))
            }
            builder.addStroke(strokeBuilder.build())
        }
        return builder.build()
    }

    class Factory(
        private val container: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiaryViewModel(container, container.inkRecognizer) as T
        }
    }
}
