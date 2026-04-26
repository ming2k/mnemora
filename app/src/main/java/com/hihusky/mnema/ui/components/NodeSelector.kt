package com.hihusky.mnema.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hihusky.mnema.data.model.Node
import com.hihusky.mnema.ui.theme.MnemaSpacing
import com.hihusky.mnema.ui.theme.MnemaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSelector(
    nodes: List<Node>,
    currentPartitionId: String,
    onNodeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = MnemaSpacing.Large, vertical = MnemaSpacing.Small)) {
            LazyColumn(contentPadding = PaddingValues(bottom = MnemaSpacing.Large)) {
                item {
                    NodeItem(
                        title = "All Nodes",
                        isSelected = currentPartitionId == "all",
                        onClick = { onNodeSelected("all") }
                    )
                    HorizontalDivider()
                }
                items(flattenNodes(nodes)) { flatNode ->
                    NodeItem(
                        title = if (flatNode.depth > 0) {
                            "  ".repeat(flatNode.depth) + flatNode.title
                        } else {
                            flatNode.title
                        },
                        isSelected = currentPartitionId == flatNode.id,
                        onClick = { onNodeSelected(flatNode.id) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun flattenNodes(nodes: List<Node>, depth: Int = 0): List<Node> {
    val result = mutableListOf<Node>()
    for (node in nodes) {
        result.add(node.copy(depth = depth))
        result.addAll(flattenNodes(node.children, depth + 1))
    }
    return result
}

@Composable
private fun NodeItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = MnemaSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(MnemaSpacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun NodeSelectorPreview() {
    MnemaTheme {
        NodeSelector(
            nodes = listOf(
                Node(id = "1", bookId = 1, title = "Algebra Basics", children = listOf(
                    Node(id = "1_0", bookId = 1, title = "Sub-topic 1.1", depth = 1)
                )),
                Node(id = "2", bookId = 1, title = "Linear Equations"),
                Node(id = "3", bookId = 1, title = "Quadratic Functions")
            ),
            currentPartitionId = "2",
            onNodeSelected = {},
            onDismiss = {}
        )
    }
}
