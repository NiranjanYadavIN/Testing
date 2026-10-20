package com.auravoice.tts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auravoice.tts.data.model.TtsEngineType
import com.auravoice.tts.ui.theme.AccentCyan

@Composable
fun EngineBadge(
    engine: TtsEngineType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentCyan.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("hd_quality_badge")
    ) {
        Text(
            text = "HD Voice",
            color = AccentCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
