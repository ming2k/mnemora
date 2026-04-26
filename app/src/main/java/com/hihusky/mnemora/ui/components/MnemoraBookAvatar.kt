package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.hihusky.mnemora.ui.theme.MnemoraSize
import com.hihusky.mnemora.ui.theme.bookColor
import com.hihusky.mnemora.ui.theme.identityContainer

@Composable
fun MnemoraBookAvatar(
    bookId: Int,
    displayName: String,
    iconName: String?,
    modifier: Modifier = Modifier,
    size: Dp = MnemoraSize.AvatarLarge
) {
    val color = bookColor(bookId)
    val iconVector = resolveBookIcon(iconName)

    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.medium)
            .background(color.identityContainer()),
        contentAlignment = Alignment.Center
    ) {
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(MnemoraSize.IconMedium),
                tint = color
            )
        } else {
            Text(
                text = displayName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}
