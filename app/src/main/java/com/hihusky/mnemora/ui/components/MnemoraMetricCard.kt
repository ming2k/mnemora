package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.subtleContainer

@Composable
fun MnemoraMetricCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    MnemoraCard(
        modifier = modifier,
        containerColor = color.subtleContainer(),
        contentPadding = PaddingValues(MnemoraSpacing.Medium)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
