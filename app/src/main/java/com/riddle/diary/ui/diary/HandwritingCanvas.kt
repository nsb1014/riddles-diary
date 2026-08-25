package com.riddle.diary.ui.diary

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.riddle.diary.ui.theme.InkBlack
import com.riddle.diary.ui.theme.BloodInk

/**
 * S Pen–optimized handwriting surface.
 * Prefers stylus input (Galaxy S25 Ultra) with pressure-sensitive stroke width,
 * while still accepting finger/mouse for convenience.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HandwritingCanvas(
    strokes: List<InkStroke>,
    activeStroke: InkStroke?,
    stylusPreferred: Boolean,
    enabled: Boolean,
    onSize: (width: Float, height: Float) -> Unit,
    onStart: (StrokePoint) -> Unit,
    onMove: (StrokePoint) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    inkColor: Color = InkBlack
) {
    var lastSize by remember { mutableStateOf(IntSize.Zero) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size != lastSize) {
                    lastSize = size
                    onSize(size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInteropFilter { event ->
                if (!enabled) return@pointerInteropFilter false
                val tool = event.getToolType(0)
                val isStylus = tool == MotionEvent.TOOL_TYPE_STYLUS ||
                    tool == MotionEvent.TOOL_TYPE_ERASER
                val isFinger = tool == MotionEvent.TOOL_TYPE_FINGER ||
                    tool == MotionEvent.TOOL_TYPE_MOUSE ||
                    tool == MotionEvent.TOOL_TYPE_UNKNOWN
                if (stylusPreferred && !isStylus && isFinger && event.actionMasked == MotionEvent.ACTION_DOWN) {
                    // Allow finger but mark as non-stylus; user can disable stylus-preferred in settings.
                }
                val pressure = when {
                    event.pressure in 0.01f..1f -> event.pressure
                    isStylus -> 0.55f
                    else -> 0.4f
                }
                val point = StrokePoint(
                    x = event.x,
                    y = event.y,
                    pressure = pressure,
                    t = event.eventTime,
                    fromStylus = isStylus
                )
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        onStart(point)
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        onMove(point)
                        // Process historical points for smoother S Pen ink
                        for (i in 0 until event.historySize) {
                            val hp = StrokePoint(
                                x = event.getHistoricalX(i),
                                y = event.getHistoricalY(i),
                                pressure = event.getHistoricalPressure(i).takeIf { it > 0f } ?: pressure,
                                t = event.getHistoricalEventTime(i),
                                fromStylus = isStylus
                            )
                            onMove(hp)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        onEnd()
                        true
                    }
                    else -> false
                }
            }
    ) {
        fun drawStroke(stroke: InkStroke, color: Color) {
            if (stroke.points.size < 2) {
                stroke.points.firstOrNull()?.let { p ->
                    val r = (2.2f + p.pressure * 5.5f)
                    drawCircle(color = color, radius = r, center = Offset(p.x, p.y))
                }
                return
            }
            val path = Path()
            val first = stroke.points.first()
            path.moveTo(first.x, first.y)
            for (i in 1 until stroke.points.size) {
                val prev = stroke.points[i - 1]
                val curr = stroke.points[i]
                val mid = Offset((prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
                path.quadraticTo(prev.x, prev.y, mid.x, mid.y)
            }
            val avgPressure = stroke.points.map { it.pressure }.average().toFloat()
            val width = 2.4f + avgPressure * 6.2f
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        strokes.forEach { drawStroke(it, inkColor) }
        activeStroke?.let { drawStroke(it, BloodInk.copy(alpha = 0.9f)) }
    }
}
