package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hihusky.mnemora.ui.theme.MnemoraAlpha
import com.hihusky.mnemora.ui.theme.MnemoraElevation
import com.hihusky.mnemora.ui.theme.MnemoraSpacing

@Composable
fun MnemoraSettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = MnemoraAlpha.Strong),
        modifier =
            modifier.padding(
                start = MnemoraSpacing.Large,
                top = MnemoraSpacing.XLarge,
                bottom = MnemoraSpacing.Small,
            ),
    )
}

@Composable
fun MnemoraSettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MnemoraCard(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MnemoraSpacing.Large),
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(0.dp),
        content = content,
    )
}

@Composable
fun MnemoraSettingsDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = MnemoraSpacing.Large),
    )
}

@Composable
fun MnemoraSettingsSwitchRow(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
) {
    MnemoraSettingsRow(
        headline = headline,
        supporting = supporting,
        icon = icon,
        modifier = modifier.clickable { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        },
    )
}

@Composable
fun MnemoraSettingsDropdownRow(
    headline: String,
    supporting: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        MnemoraSettingsRow(
            headline = headline,
            supporting = null,
            icon = icon,
            modifier = Modifier.clickable { expanded = true },
            trailing = {
                Box {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.XSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = supporting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.medium,
                        shadowElevation = MnemoraElevation.Resting,
                        modifier = Modifier.widthIn(min = 180.dp),
                    ) {
                        options.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSelect(index)
                                    expanded = false
                                },
                                leadingIcon =
                                    if (index == selectedIndex) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
fun MnemoraSettingsRow(
    headline: String,
    supporting: String?,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}
