package com.riddle.diary.ai

import com.riddle.diary.data.DiaryEntry
import com.riddle.diary.data.LlmProvider
import com.riddle.diary.data.PersonalityRepository
import com.riddle.diary.data.SecureKeyStore
import com.riddle.diary.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class LlmClient(
    private val keyStore: SecureKeyStore,
    private val personalityRepository: PersonalityRepository,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun reply(history: List<DiaryEntry>, userMessage: String): Result<String> =
        withContext(Dispatchers.IO) {
            val apiKey = keyStore.getApiKey()
                ?: return@withContext Result.failure(IllegalStateException("Add an API key in Settings to awaken the diary."))
            val settings = settingsRepository.current()
            val personality = personalityRepository.current()
            val system = buildSystemPrompt(personality.name, personality.customInstructions, personality.toneNotes)

            runCatching {
                when (settings.provider) {
                    LlmProvider.OPENAI_COMPATIBLE -> openAiChat(
                        baseUrl = settings.baseUrl,
                        apiKey = apiKey,
                        model = settings.model,
                        system = system,
                        history = history,
                        userMessage = userMessage
                    )
                    LlmProvider.GEMINI -> geminiChat(
                        apiKey = apiKey,
                        model = settings.geminiModel,
                        system = system,
                        history = history,
                        userMessage = userMessage
                    )
                }
            }
        }

    private fun buildSystemPrompt(name: String, instructions: String, tone: String): String = """
Name / identity: $name

Custom instructions (like a Gem personality layer — follow these closely):
$instructions

Tone & style notes:
$tone
""".trimIndent()

    private fun openAiChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        system: String,
        history: List<DiaryEntry>,
        userMessage: String
    ): String {
        val messages = buildList {
            add(OpenAiMessage(role = "system", content = system))
            history.takeLast(24).forEach { entry ->
                add(
                    OpenAiMessage(
                        role = if (entry.role == DiaryEntry.Role.USER) "user" else "assistant",
                        content = entry.text
                    )
                )
            }
            add(OpenAiMessage(role = "user", content = userMessage))
        }
        val body = OpenAiRequest(model = model, messages = messages, temperature = 0.85)
        val root = baseUrl.trimEnd('/')
        val url = if (root.endsWith("/v1")) "$root/chat/completions" else "$root/v1/chat/completions"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(body).toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Diary connection failed (${response.code}): ${raw.take(280)}")
            }
            val parsed = json.decodeFromString<OpenAiResponse>(raw)
            return parsed.choices.firstOrNull()?.message?.content?.trim().orEmpty()
                .ifBlank { error("The ink remained blank.") }
        }
    }

    private fun geminiChat(
        apiKey: String,
        model: String,
        system: String,
        history: List<DiaryEntry>,
        userMessage: String
    ): String {
        val contents = buildList {
            history.takeLast(24).forEach { entry ->
                add(
                    GeminiContent(
                        role = if (entry.role == DiaryEntry.Role.USER) "user" else "model",
                        parts = listOf(GeminiPart(entry.text))
                    )
                )
            }
            add(GeminiContent(role = "user", parts = listOf(GeminiPart(userMessage))))
        }
        val body = GeminiRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(system))),
            contents = contents,
            generationConfig = GeminiGenerationConfig(temperature = 0.85)
        )
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(body).toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Diary connection failed (${response.code}): ${raw.take(280)}")
            }
            val parsed = json.decodeFromString<GeminiResponse>(raw)
            return parsed.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim().orEmpty()
                .ifBlank { error("The ink remained blank.") }
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
private data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double
)

@Serializable
private data class OpenAiMessage(val role: String, val content: String)

@Serializable
private data class OpenAiResponse(val choices: List<OpenAiChoice> = emptyList())

@Serializable
private data class OpenAiChoice(val message: OpenAiMessage? = null)

@Serializable
private data class GeminiRequest(
    @SerialName("system_instruction") val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig
)

@Serializable
private data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
private data class GeminiPart(val text: String)

@Serializable
private data class GeminiGenerationConfig(val temperature: Double)

@Serializable
private data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList())

@Serializable
private data class GeminiCandidate(val content: GeminiContent? = null)
