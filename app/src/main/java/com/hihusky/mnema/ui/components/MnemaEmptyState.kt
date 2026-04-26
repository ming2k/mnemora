package com.hihusky.mnema.ui.components

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
import com.hihusky.mnema.ui.theme.MnemaAlpha
import com.hihusky.mnema.ui.theme.MnemaSize
import com.hihusky.mnema.ui.theme.MnemaSpacing

@Composable
fun MnemaEmptyState(
    icon: ImageVector,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    action: (@Composable ColumnScope.() -> Unit)? = null
) {
    val color = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier.padding(MnemaSpacing.XXLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(MnemaSize.EmptyStateIcon),
            tint = color.copy(alpha = if (isError) MnemaAlpha.Strong else MnemaAlpha.Disabled)
        )
        Spacer(modifier = Modifier.height(MnemaSpacing.Large))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            textAlign = TextAlign.Center
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(MnemaSpacing.Small))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = color.copy(alpha = MnemaAlpha.Muted),
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(MnemaSpacing.XLarge))
            action()
        }
    }
}
