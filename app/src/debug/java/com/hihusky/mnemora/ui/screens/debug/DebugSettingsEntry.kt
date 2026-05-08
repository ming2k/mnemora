package com.hihusky.mnemora.ui.screens.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hihusky.mnemora.ui.components.MnemoraSettingsGroup
import com.hihusky.mnemora.ui.components.MnemoraSettingsSectionHeader
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import androidx.compose.ui.unit.dp

/**
 * Debug-only settings section. Lives in `src/debug/`; the release variant
 * compiles the no-op version in `src/release/` instead.
 */
@Composable
fun DebugSettingsSection(onNavigateToMarkdownTest: () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider(thickness = 0.5.dp)
    Spacer(modifier = Modifier.height(16.dp))

    MnemoraSettingsSectionHeader(title = "Developer Tools")

    MnemoraSettingsGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToMarkdownTest)
                .padding(
                    horizontal = MnemoraSpacing.Large,
                    vertical = MnemoraSpacing.Medium
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Markdown Preview",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Test rendering of tables, formulas, and streaming",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
