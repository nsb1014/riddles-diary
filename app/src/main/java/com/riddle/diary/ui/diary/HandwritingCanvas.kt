package com.riddle.diary.ui.diary

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import com.riddle.diary.ui.theme.BloodInk
import com.riddle.diary.ui.theme.InkBlack

/**
 * S Pen–optimized handwriting surface.
 *
 * In-progress points stay in local Compose state (not ViewModel StateFlow) so
 * high-rate S Pen samples are not dropped. A native touch overlay calls
 * requestDisallowInterceptTouchEvent so parent scrollers cannot steal mid-stroke.
 */
@Composable
fun HandwritingCanvas(
    strokes: List<InkStroke>,
    enabled: Boolean,
    onSize: (width: Float, height: Float) -> Unit,
    onStrokeCompleted: (InkStroke) -> Unit,
    modifier: Modifier = Modifier,
    inkColor: Color = InkBlack
) {
    var lastSize by remember { mutableStateOf(IntSize.Zero) }
    val activePoints = remember { mutableStateListOf<StrokePoint>() }
    val enabledState = rememberUpdatedState(enabled)
    val onCompletedState = rememberUpdatedState(onStrokeCompleted)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                if (size != lastSize) {
                    lastSize = size
                    onSize(size.width.toFloat(), size.height.toFloat())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            fun drawStroke(stroke: InkStroke, color: Color) {
                if (stroke.points.isEmpty()) return
                if (stroke.points.size < 2) {
                    val p = stroke.points.first()
                    drawCircle(
                        color = color,
                        radius = 2.2f + p.pressure * 5.5f,
                        center = Offset(p.x, p.y)
                    )
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
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(
                        width = 2.4f + avgPressure * 6.2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            strokes.forEach { drawStroke(it, inkColor) }
            if (activePoints.isNotEmpty()) {
                drawStroke(InkStroke(activePoints.toList()), BloodInk.copy(alpha = 0.9f))
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                object : View(context) {
                    init {
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        isClickable = true
                        isFocusable = false
                    }

                    @SuppressLint("ClickableViewAccessibility")
                    override fun onTouchEvent(event: MotionEvent): Boolean {
                        if (!enabledState.value) return false
                        parent?.requestDisallowInterceptTouchEvent(true)

                        val tool = event.getToolType(0)
                        val isStylus = tool == MotionEvent.TOOL_TYPE_STYLUS ||
                            tool == MotionEvent.TOOL_TYPE_ERASER
                        val pressure = when {
                            event.pressure in 0.01f..1f -> event.pressure
                            isStylus -> 0.55f
                            else -> 0.4f
                        }

                        fun pointAt(x: Float, y: Float, p: Float, t: Long) = StrokePoint(
                            x = x,
                            y = y,
                            pressure = p,
                            t = t,
                            fromStylus = isStylus
                        )

                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                                activePoints.clear()
                                activePoints.add(pointAt(event.x, event.y, pressure, event.eventTime))
                                return true
                            }
                            MotionEvent.ACTION_MOVE -> {
                                // Historical samples first (chronological), then current.
                                for (i in 0 until event.historySize) {
                                    val hp = event.getHistoricalPressure(i).takeIf { it > 0f } ?: pressure
                                    activePoints.add(
                                        pointAt(
                                            event.getHistoricalX(i),
                                            event.getHistoricalY(i),
                                            hp,
                                            event.getHistoricalEventTime(i)
                                        )
                                    )
                                }
                                activePoints.add(pointAt(event.x, event.y, pressure, event.eventTime))
                                return true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                                val finished = activePoints.toList()
                                activePoints.clear()
                                if (finished.isNotEmpty()) {
                                    onCompletedState.value(InkStroke(finished))
                                }
                                parent?.requestDisallowInterceptTouchEvent(false)
                                return true
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                val finished = activePoints.toList()
                                activePoints.clear()
                                if (finished.size >= 2) {
                                    onCompletedState.value(InkStroke(finished))
                                }
                                parent?.requestDisallowInterceptTouchEvent(false)
                                return true
                            }
                            else -> return false
                        }
                    }
                }
            },
            update = { view ->
                view.isEnabled = enabled
                view.isClickable = enabled
            }
        )
    }
}
