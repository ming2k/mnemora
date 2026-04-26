package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme

@Composable
fun DopamineProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(MnemoraSize.ProgressTrack)
            .padding(horizontal = MnemoraSpacing.Large),
        color = color,
        trackColor = trackColor
    )
}

@Preview(showBackground = true)
@Composable
private fun DopamineProgressBarPreview() {
    MnemoraTheme {
        DopamineProgressBar(progress = 0.65f)
    }
}
