package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hihusky.mnemora.data.model.QuestionStatus
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme
import com.hihusky.mnemora.ui.theme.SuccessColor
import com.hihusky.mnemora.ui.theme.WarningColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewSheet(
    totalQuestions: Int,
    currentIndex: Int,
    getStatus: (Int) -> QuestionStatus,
    onQuestionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    val targetIndex = currentIndex.coerceIn(0, (totalQuestions - 1).coerceAtLeast(0))
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = targetIndex)
    LaunchedEffect(targetIndex, totalQuestions) {
        if (totalQuestions > 0) {
            gridState.scrollToItem(targetIndex)
        }
    }

    MnemoraBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(
                start = MnemoraSpacing.Large,
                top = MnemoraSpacing.Small,
                end = MnemoraSpacing.Large,
                bottom = MnemoraSpacing.Large
            )
        ) {
            OverviewStats(totalQuestions = totalQuestions, getStatus = getStatus)
            Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
            OverviewLegend()
            Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 48.dp),
                contentPadding = PaddingValues(MnemoraSpacing.XSmall),
                horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small)
            ) {
                items(totalQuestions) { index ->
                    val status = getStatus(index)
                    val color = when (status) {
                        QuestionStatus.Correct -> SuccessColor
                        QuestionStatus.Wrong -> MaterialTheme.colorScheme.error
                        QuestionStatus.Marked -> WarningColor
                        QuestionStatus.Unanswered -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                    val isCurrent = index == currentIndex
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isCurrent) 2.dp else 0.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else color,
                                shape = CircleShape
                            )
                            .clickable { onQuestionSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (status == QuestionStatus.Unanswered)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                androidx.compose.ui.graphics.Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStats(
    totalQuestions: Int,
    getStatus: (Int) -> QuestionStatus
) {
    val counts = remember(totalQuestions, getStatus) {
        (0 until totalQuestions).groupingBy { getStatus(it) }.eachCount()
    }
    val correct = counts[QuestionStatus.Correct] ?: 0
    val wrong = counts[QuestionStatus.Wrong] ?: 0
    val marked = counts[QuestionStatus.Marked] ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(label = "Correct", value = correct.toString(), color = SuccessColor)
        StatItem(label = "Wrong", value = wrong.toString(), color = MaterialTheme.colorScheme.error)
        StatItem(label = "Marked", value = marked.toString(), color = WarningColor)
        StatItem(
            label = "Total",
            value = totalQuestions.toString(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OverviewLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("Current", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary)
        LegendItem("Correct", SuccessColor)
        LegendItem("Wrong", MaterialTheme.colorScheme.error)
        LegendItem("Marked", WarningColor)
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    borderColor: Color = color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, borderColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun OverviewSheetPreview() {
    MnemoraTheme {
        OverviewSheet(
            totalQuestions = 12,
            currentIndex = 3,
            getStatus = { index ->
                when (index % 4) {
                    0 -> QuestionStatus.Correct
                    1 -> QuestionStatus.Wrong
                    2 -> QuestionStatus.Marked
                    else -> QuestionStatus.Unanswered
                }
            },
            onQuestionSelected = {},
            onDismiss = {}
        )
    }
}
