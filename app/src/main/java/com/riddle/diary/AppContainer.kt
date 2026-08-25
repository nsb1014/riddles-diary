package com.riddle.diary

import android.content.Context
import com.riddle.diary.ai.LlmClient
import com.riddle.diary.data.ConversationRepository
import com.riddle.diary.data.PersonalityRepository
import com.riddle.diary.data.SecureKeyStore
import com.riddle.diary.data.SettingsRepository
import com.riddle.diary.util.InkRecognizer

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val secureKeyStore = SecureKeyStore(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val personalityRepository = PersonalityRepository(appContext)
    val conversationRepository = ConversationRepository(appContext)
    val llmClient = LlmClient(secureKeyStore, personalityRepository, settingsRepository)
    val inkRecognizer = InkRecognizer(appContext)
}
