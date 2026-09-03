package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.hihusky.mnemora.ui.theme.MnemoraAlpha
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing

@Composable
fun MnemoraEmptyState(
    icon: ImageVector,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val color =
        if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Column(
        modifier = modifier.padding(MnemoraSpacing.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(MnemoraSize.EmptyStateIcon),
            tint = color.copy(alpha = if (isError) MnemoraAlpha.Strong else MnemoraAlpha.Disabled),
        )
        Spacer(modifier = Modifier.height(MnemoraSpacing.Large))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = color.copy(alpha = MnemoraAlpha.Muted),
                textAlign = TextAlign.Center,
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(MnemoraSpacing.XLarge))
            action()
        }
    }
}
