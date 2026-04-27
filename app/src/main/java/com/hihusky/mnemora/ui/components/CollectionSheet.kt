package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hihusky.mnemora.data.model.Collection
import com.hihusky.mnemora.data.model.CollectionBehavior
import com.hihusky.mnemora.data.model.CollectionKind
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionSheet(
    collections: List<Collection>,
    questionCollectionIds: Set<Int>,
    onToggle: (Int) -> Unit,
    onCreate: (String) -> Unit,
    onDelete: (Int) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    var newName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    MnemoraBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = MnemoraSpacing.XLarge)) {
            Text(
                text = "Collections",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small)
            )

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (collections.isEmpty() && !isCreating) {
                Text(
                    text = "No collections yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.XLarge)
                )
            } else {
                collections.forEach { collection ->
                    CollectionRow(
                        name = collection.name,
                        isSelected = collection.id in questionCollectionIds,
                        onToggle = { onToggle(collection.id) },
                        onDelete = { onDelete(collection.id) }
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (isCreating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Small)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("Collection name") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newName.isNotBlank()) {
                                onCreate(newName.trim())
                                newName = ""
                                isCreating = false
                            }
                        }),
                        shape = MaterialTheme.shapes.medium
                    )
                    FilledTonalIconButton(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onCreate(newName.trim())
                                newName = ""
                                isCreating = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Create")
                    }
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                TextButton(
                    onClick = { isCreating = true },
                    modifier = Modifier.padding(horizontal = MnemoraSpacing.Small)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(MnemoraSpacing.XSmall))
                    Text("New collection")
                }
            }
        }
    }
}

@Composable
private fun CollectionRow(
    name: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MnemoraSpacing.Medium)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Delete collection",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CollectionSheetPreview() {
    MnemoraTheme {
        CollectionSheet(
            collections = listOf(
                Collection(id = 1, bookId = 1, kind = CollectionKind.Custom,
                    behavior = CollectionBehavior.Manual, name = "Exam Prep", createdAt = 0L),
                Collection(id = 2, bookId = 1, kind = CollectionKind.Custom,
                    behavior = CollectionBehavior.Manual, name = "Wrong Answers", createdAt = 0L)
            ),
            questionCollectionIds = setOf(1),
            onToggle = {},
            onCreate = {},
            onDelete = {},
            onDismiss = {}
        )
    }
}
