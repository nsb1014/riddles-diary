package com.riddle.diary.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import com.riddle.diary.ui.theme.Parchment
import com.riddle.diary.ui.theme.ParchmentDark
import com.riddle.diary.ui.theme.ParchmentStain
import com.riddle.diary.ui.theme.SoftShadow

@Composable
fun ParchmentBackground(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "parchment")
    val shift by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Parchment,
                        ParchmentDark.copy(alpha = 0.85f + 0.1f * shift),
                        ParchmentStain.copy(alpha = 0.55f),
                        Parchment
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 48f
            val effect = PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f)
            var y = lineSpacing
            while (y < size.height) {
                drawLine(
                    color = Color(0x332C1810),
                    start = Offset(32f, y),
                    end = Offset(size.width - 32f, y),
                    strokeWidth = 1.2f,
                    pathEffect = effect
                )
                y += lineSpacing
            }
            // Left margin rule
            drawLine(
                color = Color(0x55B08D3A),
                start = Offset(72f, 24f),
                end = Offset(72f, size.height - 24f),
                strokeWidth = 2f
            )
            // Soft vignette corners
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, SoftShadow),
                    center = Offset(0f, 0f),
                    radius = size.minDimension * 0.55f
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(0f, 0f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, SoftShadow),
                    center = Offset(size.width, size.height),
                    radius = size.minDimension * 0.6f
                ),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width, size.height)
            )
        }
    }
}
