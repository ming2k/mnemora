package com.hihusky.mnema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.coil2.Coil2ImageTransformerImpl
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import java.io.File

// ------------------------------------------------------------------
// Content blocks produced by parsing mixed Markdown + LaTeX input
// ------------------------------------------------------------------
private sealed class ContentBlock {
    data class MarkdownText(val text: String) : ContentBlock()
    data class InlineMath(val formula: String) : ContentBlock()
    data class DisplayMath(val formula: String) : ContentBlock()
}

private sealed class RichContentBlock {
    data class Text(val text: String) : RichContentBlock()
    data class Image(val alt: String, val path: String) : RichContentBlock()
}

/**
 * Renders content that may contain **Markdown**, **inline LaTeX** (`$...$`)
 * and **display LaTeX** (`$$...$$`).
 *
 * The text is split into blocks; each block is rendered with the most
 * appropriate engine (mikepenz Markdown for prose, huarangmeng Latex
 * for formulas).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    imageBasePath: String? = null,
    textStyle: TextStyle? = null,
    contentColor: Color? = null
) {
    val resolvedTextStyle = textStyle ?: MaterialTheme.typography.bodyLarge
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        val richBlocks = remember(content) { parseRichContentBlocks(content) }
        richBlocks.forEach { block ->
            when (block) {
                is RichContentBlock.Text -> RenderTextContent(
                    content = block.text,
                    imageBasePath = imageBasePath,
                    textStyle = resolvedTextStyle,
                    contentColor = resolvedContentColor
                )
                is RichContentBlock.Image -> MarkdownImage(
                    alt = block.alt,
                    path = block.path,
                    imageBasePath = imageBasePath
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RenderTextContent(
    content: String,
    imageBasePath: String?,
    textStyle: TextStyle,
    contentColor: Color
) {
    if (content.isBlank()) return

    val blocks = remember(content) { parseContentBlocks(content) }
    val grouped = remember(blocks) { groupInlineBlocks(blocks) }

    grouped.forEach { group ->
        when (group) {
            is GroupedBlock.StandaloneMarkdown -> {
                val processed = if (imageBasePath != null) {
                    resolveImagePaths(group.text, imageBasePath)
                } else group.text
                MarkdownBlock(
                    text = processed,
                    textStyle = textStyle,
                    contentColor = contentColor
                )
            }

            is GroupedBlock.InlineGroup -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    group.parts.forEach { part ->
                        when (part) {
                            is ContentBlock.MarkdownText -> {
                                val processed = if (imageBasePath != null) {
                                    resolveImagePaths(part.text, imageBasePath)
                                } else part.text
                                Text(
                                    text = processed,
                                    style = textStyle,
                                    color = contentColor
                                )
                            }

                            is ContentBlock.InlineMath -> {
                                Latex(
                                    latex = part.formula,
                                    config = latexInlineConfig(contentColor)
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }

            is GroupedBlock.DisplayMath -> {
                Latex(
                    latex = group.formula,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    config = latexDisplayConfig(contentColor)
                )
            }
        }
    }
}

@Composable
private fun MarkdownImage(
    alt: String,
    path: String,
    imageBasePath: String?
) {
    val model = remember(path, imageBasePath) { resolveImagePath(path, imageBasePath) }
    var showPreview by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { showPreview = true }
            .padding(8.dp)
    ) {
        AsyncImage(
            model = model,
            contentDescription = alt.ifBlank { null },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
        )
    }

    if (showPreview) {
        ImagePreviewDialog(
            model = model,
            contentDescription = alt.ifBlank { null },
            onDismiss = { showPreview = false }
        )
    }
}

@Composable
private fun ImagePreviewDialog(
    model: String,
    contentDescription: String?,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                }
        ) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = nextScale
                            offset = if (nextScale == 1f) Offset.Zero else offset + pan
                        }
                    }
            )
        }
    }
}

// ------------------------------------------------------------------
// Internal: pure Markdown block (no inline math inside)
// ------------------------------------------------------------------
@Composable
private fun MarkdownBlock(
    text: String,
    textStyle: TextStyle,
    contentColor: Color
) {
    val colors = DefaultMarkdownColors(
        text = contentColor,
        codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
        inlineCodeBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
        dividerColor = MaterialTheme.colorScheme.outline,
        tableBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    )
    val typography = DefaultMarkdownTypography(
        h1 = MaterialTheme.typography.headlineLarge,
        h2 = MaterialTheme.typography.headlineMedium,
        h3 = MaterialTheme.typography.headlineSmall,
        h4 = MaterialTheme.typography.titleLarge,
        h5 = MaterialTheme.typography.titleMedium,
        h6 = MaterialTheme.typography.titleSmall,
        text = textStyle,
        code = MaterialTheme.typography.bodyMedium,
        inlineCode = MaterialTheme.typography.bodyMedium,
        quote = textStyle,
        paragraph = textStyle,
        ordered = textStyle,
        bullet = textStyle,
        list = textStyle,
        textLink = TextLinkStyles(
            style = SpanStyle(color = contentColor)
        ),
        table = textStyle
    )
    Markdown(
        content = text,
        colors = colors,
        typography = typography,
        imageTransformer = Coil2ImageTransformerImpl
    )
}

// ------------------------------------------------------------------
// Parsing: split raw string into ContentBlocks
// ------------------------------------------------------------------
private fun parseRichContentBlocks(content: String): List<RichContentBlock> {
    val imageRegex = Regex("!\\[([^\\]]*)]\\(([^)]*)\\)")
    val blocks = mutableListOf<RichContentBlock>()
    var lastEnd = 0

    for (match in imageRegex.findAll(content)) {
        val before = content.substring(lastEnd, match.range.first)
        if (before.isNotBlank()) {
            blocks.add(RichContentBlock.Text(before))
        }
        blocks.add(
            RichContentBlock.Image(
                alt = match.groupValues[1],
                path = match.groupValues[2]
            )
        )
        lastEnd = match.range.last + 1
    }

    val after = content.substring(lastEnd)
    if (after.isNotBlank()) {
        blocks.add(RichContentBlock.Text(after))
    }
    return blocks.ifEmpty { listOf(RichContentBlock.Text(content)) }
}

private fun parseContentBlocks(content: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val displayRegex = Regex("\\\$\\\$([\\s\\S]*?)\\\$\\\$")
    val matches = displayRegex.findAll(content).toList()

    var lastEnd = 0
    for (match in matches) {
        val before = content.substring(lastEnd, match.range.first)
        blocks.addAll(parseInlineMathBlocks(before))
        blocks.add(ContentBlock.DisplayMath(match.groupValues[1].trim()))
        lastEnd = match.range.last + 1
    }

    val after = content.substring(lastEnd)
    blocks.addAll(parseInlineMathBlocks(after))
    return blocks
}

private fun parseInlineMathBlocks(text: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    // Match $...$ but not $$...$$
    val inlineRegex = Regex("(?<!\\\$)\\\$(?!\\\$)([^\\n\\\$]+?)\\\$(?!\\\$)")
    val matches = inlineRegex.findAll(text).toList()

    var lastEnd = 0
    for (match in matches) {
        val before = text.substring(lastEnd, match.range.first)
        if (before.isNotEmpty()) {
            blocks.add(ContentBlock.MarkdownText(before))
        }
        blocks.add(ContentBlock.InlineMath(match.groupValues[1].trim()))
        lastEnd = match.range.last + 1
    }

    val after = text.substring(lastEnd)
    if (after.isNotEmpty()) {
        blocks.add(ContentBlock.MarkdownText(after))
    }
    return blocks
}

// ------------------------------------------------------------------
// Grouping: merge consecutive MarkdownText + InlineMath so they flow
// together in a single FlowRow.  Standalone MarkdownText stays as a
// full Markdown block.
// ------------------------------------------------------------------
private sealed class GroupedBlock {
    data class StandaloneMarkdown(val text: String) : GroupedBlock()
    data class InlineGroup(val parts: List<ContentBlock>) : GroupedBlock()
    data class DisplayMath(val formula: String) : GroupedBlock()
}

private fun groupInlineBlocks(blocks: List<ContentBlock>): List<GroupedBlock> {
    val result = mutableListOf<GroupedGroup>()
    var currentInline = mutableListOf<ContentBlock>()

    for (block in blocks) {
        when (block) {
            is ContentBlock.DisplayMath -> {
                flushInline(currentInline, result)
                currentInline = mutableListOf()
                result.add(GroupedGroup.DisplayMath(block.formula))
            }

            is ContentBlock.InlineMath,
            is ContentBlock.MarkdownText -> {
                currentInline.add(block)
            }
        }
    }
    flushInline(currentInline, result)

    return result.map { group ->
        when (group) {
            is GroupedGroup.StandaloneMarkdown ->
                GroupedBlock.StandaloneMarkdown(group.text)

            is GroupedGroup.InlineGroup ->
                GroupedBlock.InlineGroup(group.parts)

            is GroupedGroup.DisplayMath ->
                GroupedBlock.DisplayMath(group.formula)
        }
    }
}

private sealed class GroupedGroup {
    data class StandaloneMarkdown(val text: String) : GroupedGroup()
    data class InlineGroup(val parts: List<ContentBlock>) : GroupedGroup()
    data class DisplayMath(val formula: String) : GroupedGroup()
}

private fun flushInline(
    buffer: MutableList<ContentBlock>,
    out: MutableList<GroupedGroup>
) {
    if (buffer.isEmpty()) return

    // If the buffer contains any InlineMath, treat the whole sequence as an
    // InlineGroup (FlowRow).  Otherwise it's pure Markdown text.
    val hasMath = buffer.any { it is ContentBlock.InlineMath }
    if (hasMath) {
        out.add(GroupedGroup.InlineGroup(buffer.toList()))
    } else {
        val combined = buffer.joinToString("") { (it as ContentBlock.MarkdownText).text }
        out.add(GroupedGroup.StandaloneMarkdown(combined))
    }
    buffer.clear()
}

// ------------------------------------------------------------------
// LaTeX config helpers
// ------------------------------------------------------------------
@Composable
private fun latexInlineConfig(contentColor: Color): LatexConfig {
    return LatexConfig(
        fontSize = 16.sp,
        color = contentColor,
        darkColor = contentColor
    )
}

@Composable
private fun latexDisplayConfig(contentColor: Color): LatexConfig {
    return LatexConfig(
        fontSize = 18.sp,
        color = contentColor,
        darkColor = contentColor
    )
}

// ------------------------------------------------------------------
// Image path resolution
// ------------------------------------------------------------------
private fun resolveImagePaths(content: String, basePath: String): String {
    val regex = "!\\[(.*?)]\\((.*?)\\)".toRegex()
    return regex.replace(content) { matchResult ->
        val alt = matchResult.groupValues[1]
        val path = matchResult.groupValues[2]
        "![$alt](${resolveImagePath(path, basePath)})"
    }
}

private fun resolveImagePath(path: String, basePath: String?): String {
    if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://")) {
        return path
    }
    return if (basePath == null) {
        path
    } else {
        "file://${File(basePath, path).absolutePath}"
    }
}
