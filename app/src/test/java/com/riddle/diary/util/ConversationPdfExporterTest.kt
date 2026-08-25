package com.riddle.diary.util

import com.riddle.diary.data.DiaryEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationPdfExporterTest {
    @Test
    fun formatPlainTextIncludesRolesAndBody() {
        val text = ConversationPdfExporter.formatPlainText(
            title = "The Diary",
            entries = listOf(
                DiaryEntry(id = "1", role = DiaryEntry.Role.USER, text = "Hello there"),
                DiaryEntry(id = "2", role = DiaryEntry.Role.DIARY, text = "I am listening.")
            )
        )
        assertTrue(text.startsWith("The Diary"))
        assertTrue(text.contains("You:\nHello there"))
        assertTrue(text.contains("The diary:\nI am listening."))
        assertFalse(text.contains("Tom Riddle"))
    }
}
