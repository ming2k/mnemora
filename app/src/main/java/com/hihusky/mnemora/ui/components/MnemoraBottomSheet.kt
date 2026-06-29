package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hihusky.mnemora.ui.theme.MnemoraSize

/**
 * The shared height ceiling for every drawer. A drawer is free to be shorter (wrapping its
 * content), but never grows past this — long content scrolls internally instead of taking over
 * the whole screen. Capping by a screen fraction keeps short devices comfortable while the
 * absolute cap keeps tall devices from feeling cavernous.
 */
@Composable
fun sheetMaxHeight(): Dp = minOf(
    MnemoraSize.SheetMaxHeight,
    (LocalConfiguration.current.screenHeightDp * MnemoraSize.SheetMaxHeightFraction).dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MnemoraBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 4.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        // Every drawer shares one height policy: wrap content, but never exceed the shared cap.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight()),
            content = content
        )
    }
}
