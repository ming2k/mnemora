package com.hihusky.mnema.ui.components

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
import com.hihusky.mnema.ui.theme.MnemaSpacing
import com.hihusky.mnema.ui.theme.subtleContainer

@Composable
fun MnemaMetricCard(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    MnemaCard(
        modifier = modifier,
        containerColor = color.subtleContainer(),
        contentPadding = PaddingValues(MnemaSpacing.Medium)
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
