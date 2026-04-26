package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.hihusky.mnemora.ui.theme.MnemoraSize

@Composable
fun MnemoraProgressLine(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MnemoraSize.ProgressTrack)
            .clip(MaterialTheme.shapes.small)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(MnemoraSize.ProgressTrack)
                .clip(MaterialTheme.shapes.small)
                .background(color)
        )
    }
}
