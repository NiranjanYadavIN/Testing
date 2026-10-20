package com.auravoice.tts.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auravoice.tts.ui.theme.AccentCyan
import com.auravoice.tts.ui.theme.PrimaryViolet

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    isGenerating: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animFactor"
    )

    val barCount = 32
    val primaryColor = if (isGenerating) AccentCyan else PrimaryViolet

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / (barCount * 1.8f)
        val gap = barWidth * 0.8f

        val startX = (width - (barCount * (barWidth + gap))) / 2f

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount
            val baseHeight = Math.sin(normalizedIndex * Math.PI).toFloat().coerceAtLeast(0.15f)
            val dynamicMultiplier = if (isPlaying || isGenerating) {
                val phaseOffset = (i % 5) * 0.2f
                ((animFactor + phaseOffset) % 1.0f).coerceAtLeast(0.2f)
            } else {
                0.2f
            }

            val barH = (height * baseHeight * dynamicMultiplier).coerceAtLeast(6.dp.toPx())
            val x = startX + i * (barWidth + gap)
            val y = (height - barH) / 2f

            drawRoundRect(
                color = primaryColor.copy(alpha = if (isPlaying || isGenerating) 0.9f else 0.4f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
