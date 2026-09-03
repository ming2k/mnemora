package com.hihusky.mnemora.ui.components.topappbar

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 项目统一的 [CenterAlignedTopAppBar] 封装。
 *
 * 已内置以下默认行为，各 Screen 无需重复声明：
 * - `windowInsets = WindowInsets(0, 0, 0, 0)`（消除不同设备状态栏高度差异）
 * - `colors.containerColor = MaterialTheme.colorScheme.background`
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemoraCenterTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        ),
) {
    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = colors,
        windowInsets = WindowInsets(0, 0, 0, 0),
    )
}
