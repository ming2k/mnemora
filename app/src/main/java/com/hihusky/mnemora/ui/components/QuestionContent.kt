package com.hihusky.mnemora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hihusky.mnemora.data.model.Question
import com.hihusky.mnemora.data.model.QuestionChoice
import com.hihusky.mnemora.data.model.QuestionType
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme

@Composable
fun QuestionContent(
    question: Question,
    selectedOption: String?,
    showAnswer: Boolean,
    modifier: Modifier = Modifier,
    onOptionSelected: ((String) -> Unit)? = null,
    imageBasePath: String? = null,
) {
    val textAnswer = remember(question.id) { mutableStateOf("") }
    var flashcardRevealed by remember(question.id, showAnswer) { mutableStateOf(showAnswer) }

    // Flashcard auto-reveals when showAnswer becomes true
    if (showAnswer && !flashcardRevealed) {
        flashcardRevealed = true
    }
    val questionTextStyle =
        MaterialTheme.typography.bodyLarge.copy(
            fontSize = 17.sp,
            lineHeight = 26.sp,
        )
    val explanationTextStyle =
        MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = MnemoraSpacing.Small),
    ) {
        // Question content
        MarkdownText(
            content = question.content,
            modifier = Modifier.fillMaxWidth(),
            imageBasePath = imageBasePath,
            textStyle = questionTextStyle,
            format = question.format,
        )

        if (question.parentContent != null) {
            Spacer(modifier = Modifier.height(MnemoraSpacing.Medium))
            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth(0.92f)
                        .align(Alignment.CenterHorizontally),
                thickness = 0.5.dp,
            )
            Spacer(modifier = Modifier.height(MnemoraSpacing.Medium))
            MarkdownText(
                content = question.parentContent,
                modifier =
                    Modifier
                        .fillMaxWidth(0.92f)
                        .align(Alignment.CenterHorizontally)
                        .alpha(0.7f),
                imageBasePath = imageBasePath,
                textStyle = questionTextStyle,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                format = question.format,
            )
        }

        Spacer(modifier = Modifier.height(MnemoraSpacing.Large))

        // Render based on question type and available choices
        when {
            question.questionType == QuestionType.Flashcard -> {
                FlashcardContent(
                    question = question,
                    revealed = flashcardRevealed,
                    showAnswer = showAnswer,
                    onReveal = {
                        flashcardRevealed = true
                        if (onOptionSelected != null) {
                            onOptionSelected.invoke("REVEALED")
                        }
                    },
                )
            }

            question.choices.isEmpty() -> {
                // FillBlank, Cloze, or any text-based question
                if (!showAnswer) {
                    Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
                    OutlinedTextField(
                        value = textAnswer.value,
                        onValueChange = { textAnswer.value = it },
                        label = { Text("Your answer") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = onOptionSelected != null,
                        singleLine = false,
                        minLines = 2,
                    )
                    Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
                    Button(
                        onClick = { onOptionSelected?.invoke(textAnswer.value.trim()) },
                        enabled = onOptionSelected != null && textAnswer.value.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Submit")
                    }
                } else {
                    Text(
                        text = "Answer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
                    Text(
                        text = question.answer,
                        style = questionTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                // MultipleChoice, TrueFalse, Unknown — render choices with slight
                // left indent to visually denote they are subordinate to the stem.
                question.choices.forEachIndexed { index, choice ->
                    ChoiceItem(
                        choice = choice,
                        isSelected = selectedOption == choice.key,
                        showAnswer = showAnswer,
                        isCorrectChoice = choice.key.uppercase() == question.answer.uppercase(),
                        onClick = { onOptionSelected?.invoke(choice.key) },
                        enabled = onOptionSelected != null,
                        imageBasePath = imageBasePath,
                        textStyle = questionTextStyle,
                        modifier =
                            Modifier
                                .fillMaxWidth(0.95f)
                                .align(Alignment.CenterHorizontally),
                    )
                    if (index < question.choices.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Explanation
        AnimatedVisibility(
            visible = showAnswer && question.questionType != QuestionType.Flashcard,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
        ) {
            Column {
                Spacer(modifier = Modifier.height(MnemoraSpacing.Large))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(MnemoraSpacing.Large))

                if (question.explanation.isNotBlank()) {
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
                    MarkdownText(
                        content = question.explanation,
                        modifier = Modifier.fillMaxWidth(),
                        imageBasePath = imageBasePath,
                        textStyle = explanationTextStyle,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        format = question.format,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceItem(
    choice: QuestionChoice,
    isSelected: Boolean,
    showAnswer: Boolean,
    isCorrectChoice: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    imageBasePath: String? = null,
    textStyle: androidx.compose.ui.text.TextStyle,
) {
    val contentColor by animateColorAsState(
        targetValue =
            when {
                showAnswer && isCorrectChoice -> MaterialTheme.colorScheme.tertiary
                showAnswer && isSelected && !isCorrectChoice -> MaterialTheme.colorScheme.error
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
        animationSpec = tween(300),
    )

    val iconTint by animateColorAsState(
        targetValue =
            when {
                showAnswer && isCorrectChoice -> MaterialTheme.colorScheme.tertiary
                showAnswer && isSelected && !isCorrectChoice -> MaterialTheme.colorScheme.error
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            },
        animationSpec = tween(300),
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Status indicator — all outlined (stroke) style for visual consistency.
        // Correct = green check, Wrong = red cross, Selected = blue check, Unselected = grey circle.
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                showAnswer && isCorrectChoice -> {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Correct",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }

                showAnswer && isSelected && !isCorrectChoice -> {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Wrong",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }

                isSelected -> {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }

                else -> {
                    Icon(
                        imageVector = Icons.Outlined.Circle,
                        contentDescription = "Unselected",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        MarkdownText(
            content = "${choice.key}. ${choice.content}",
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = MnemoraSpacing.Large),
            imageBasePath = imageBasePath,
            textStyle = textStyle,
            contentColor = contentColor,
        )
    }
}

@Composable
private fun FlashcardContent(
    question: Question,
    revealed: Boolean,
    showAnswer: Boolean,
    onReveal: () -> Unit,
) {
    val density = LocalDensity.current
    val flipRotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(tween(300))
                .graphicsLayer {
                    rotationY = flipRotation
                    cameraDistance = 8f * density.density
                },
    ) {
        if (flipRotation <= 90f) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = question.displayFront,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onReveal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reveal Answer")
                }
            }
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer { rotationY = 180f },
            ) {
                Text(
                    text = question.displayFront,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = question.displayBack,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// Previews
// ────────────────────────────────────────────────────────────

private val previewQuestion =
    Question(
        id = 1,
        bookId = 1,
        content = "What is the capital of **France**?",
        choices =
            listOf(
                QuestionChoice("A", "London"),
                QuestionChoice("B", "Paris"),
                QuestionChoice("C", "Berlin"),
                QuestionChoice("D", "Madrid"),
            ),
        answer = "B",
        explanation = "Paris is the capital and most populous city of France.",
        questionType = QuestionType.MultipleChoice,
    )

@Preview(showBackground = true)
@Composable
private fun QuestionContentUnansweredPreview() {
    MnemoraTheme {
        QuestionContent(
            question = previewQuestion,
            selectedOption = null,
            showAnswer = false,
            onOptionSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuestionContentSelectedPreview() {
    MnemoraTheme {
        QuestionContent(
            question = previewQuestion,
            selectedOption = "B",
            showAnswer = false,
            onOptionSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuestionContentAnsweredPreview() {
    MnemoraTheme {
        QuestionContent(
            question = previewQuestion,
            selectedOption = "A",
            showAnswer = true,
            onOptionSelected = {},
        )
    }
}
