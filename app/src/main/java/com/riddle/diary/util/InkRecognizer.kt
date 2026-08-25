package com.riddle.diary.util

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Converts S Pen / finger ink strokes into text via ML Kit Digital Ink.
 */
class InkRecognizer(@Suppress("UNUSED_PARAMETER") context: android.content.Context) {
    private val model: DigitalInkRecognitionModel
    private val recognizer: DigitalInkRecognizer
    private val modelManager = RemoteModelManager.getInstance()

    init {
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            ?: error("English digital ink model unavailable")
        model = DigitalInkRecognitionModel.builder(identifier).build()
        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
        modelManager.download(model, DownloadConditions.Builder().build())
    }

    suspend fun ensureModel(): Unit = suspendCancellableCoroutine { cont ->
        modelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { if (cont.isActive) cont.resume(Unit) }
            .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }

    suspend fun recognize(ink: Ink, widthPx: Float, heightPx: Float): String {
        ensureModel()
        return suspendCancellableCoroutine { cont ->
            val recognitionContext = RecognitionContext.builder()
                .setPreContext("")
                .setWritingArea(WritingArea(widthPx.coerceAtLeast(1f), heightPx.coerceAtLeast(1f)))
                .build()
            recognizer.recognize(ink, recognitionContext)
                .addOnSuccessListener { result ->
                    val text = result.candidates.firstOrNull()?.text.orEmpty().trim()
                    if (cont.isActive) cont.resume(text)
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    fun close() {
        recognizer.close()
    }
}
