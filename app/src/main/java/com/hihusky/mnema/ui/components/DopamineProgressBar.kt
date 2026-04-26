package com.hihusky.mnema.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.hihusky.mnema.ui.theme.MnemaSize
import com.hihusky.mnema.ui.theme.MnemaSpacing
import com.hihusky.mnema.ui.theme.MnemaTheme

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
            .height(MnemaSize.ProgressTrack)
            .padding(horizontal = MnemaSpacing.Large),
        color = color,
        trackColor = trackColor
    )
}

@Preview(showBackground = true)
@Composable
private fun DopamineProgressBarPreview() {
    MnemaTheme {
        DopamineProgressBar(progress = 0.65f)
    }
}
