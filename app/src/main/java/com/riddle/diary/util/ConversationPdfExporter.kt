package com.riddle.diary.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.riddle.diary.data.DiaryEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds a multi-page PDF of a scroll-mode conversation and opens the system share sheet
 * (Samsung Notes typically appears as a target for PDF / documents).
 */
object ConversationPdfExporter {

    fun formatPlainText(title: String, entries: List<DiaryEntry>): String {
        val body = entries.joinToString(separator = "\n\n") { entry ->
            val who = if (entry.role == DiaryEntry.Role.USER) "You" else "The diary"
            "$who:\n${entry.text.trim()}"
        }
        return buildString {
            appendLine(title.ifBlank { "The Diary" })
            appendLine()
            append(body)
        }.trim()
    }

    fun writePdf(
        context: Context,
        title: String,
        entries: List<DiaryEntry>
    ): File {
        require(entries.isNotEmpty()) { "Nothing to export yet — write a little first." }

        val pageWidth = 612 // US Letter points
        val pageHeight = 792
        val margin = 54f
        val contentWidth = (pageWidth - margin * 2).toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1A120B.toInt()
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF3B2A1A.toInt()
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF4A1515.toInt()
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1A120B.toInt()
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }

        val document = PdfDocument()
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin) {
                newPage()
            }
        }

        fun drawLayout(layout: StaticLayout, paintBaselineSkip: Float = 8f) {
            ensureSpace(layout.height.toFloat() + paintBaselineSkip)
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + paintBaselineSkip
        }

        val stamped = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val header = title.ifBlank { "The Diary" }
        drawLayout(
            StaticLayout.Builder.obtain(header, 0, header.length, titlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()
        )
        val meta = "Exported $stamped"
        drawLayout(
            StaticLayout.Builder.obtain(meta, 0, meta.length, metaPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build(),
            paintBaselineSkip = 18f
        )

        entries.forEach { entry ->
            val who = if (entry.role == DiaryEntry.Role.USER) "You" else "The diary"
            drawLayout(
                StaticLayout.Builder.obtain(who, 0, who.length, labelPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .build(),
                paintBaselineSkip = 4f
            )
            val text = entry.text.trim().ifBlank { "…" }
            drawLayout(
                StaticLayout.Builder.obtain(text, 0, text.length, bodyPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.2f)
                    .setIncludePad(false)
                    .build(),
                paintBaselineSkip = 16f
            )
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeTitle = header.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40).ifBlank { "diary" }
        val out = File(dir, "${safeTitle}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(out).use { document.writeTo(it) }
        document.close()
        return out
    }

    fun sharePdf(context: Context, pdfFile: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "The Diary" })
            putExtra(
                Intent.EXTRA_TEXT,
                "Conversation export from ${title.ifBlank { "The Diary" }}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(share, "Export conversation to PDF")
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
