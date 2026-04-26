package com.hihusky.mnemora.ui.components.topappbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.MnemoraSpacing

/**
 * 可折叠的 TopAppBar。
 *
 * - 展开时：标题左对齐，较大字体（24sp），高度 64dp
 * - 收缩后：标题居中，较小字体（18sp），高度 48dp
 * - 背景色始终与主题背景一致
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemoraCollapsibleTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollFraction: Float = 0f,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background
    )
) {
    val coercedFraction = scrollFraction.coerceIn(0f, 1f)

    // Use Material 3 SmallTopAppBar standard height (56dp) for expanded state
    val expandedHeight = MnemoraSize.TopBarExpanded
    val collapsedHeight = MnemoraSize.TopBarCollapsed
    val currentHeight = lerp(expandedHeight, collapsedHeight, coercedFraction)

    val expandedTitleSize = 22.sp
    val collapsedTitleSize = 18.sp
    val currentTitleSize = lerp(expandedTitleSize, collapsedTitleSize, coercedFraction)

    val contentAlignment = if (coercedFraction > 0.5f) {
        Alignment.Center
    } else {
        Alignment.CenterStart
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // All overlays share one Box so total height = currentHeight, not 3x
        Box(modifier = Modifier.fillMaxWidth().height(currentHeight)) {
            // Title layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MnemoraSpacing.Large),
                contentAlignment = contentAlignment
            ) {
                Text(
                    text = title,
                    fontSize = currentTitleSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Actions layer (top-right)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = MnemoraSpacing.Small),
                contentAlignment = Alignment.CenterEnd
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    actions()
                }
            }

            // Navigation icon layer (top-left)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = MnemoraSpacing.XSmall),
                contentAlignment = Alignment.CenterStart
            ) {
                navigationIcon()
            }
        }

        // Divider to separate top bar from content (always visible)
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
