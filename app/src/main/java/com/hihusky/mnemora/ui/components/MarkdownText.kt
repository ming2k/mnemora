package com.hihusky.mnemora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import coil3.compose.AsyncImage
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownParagraph
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.flavours.gfm.GFMElementTypes.HEADER
import org.intellij.markdown.flavours.gfm.GFMElementTypes.ROW
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.CELL
import java.io.File

// ------------------------------------------------------------------
// High-performance LRU Caches for parsed Markdown AST and Styled Spans.
// Eliminates main-thread re-parsing and layout lag during scroll.
// ------------------------------------------------------------------
private class SimpleLruCache<K, V>(
    private val maxSize: Int,
) {
    private val map =
        object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > maxSize
        }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(
        key: K,
        value: V,
    ): V? = map.put(key, value)
}

private val renderBlocksCache = SimpleLruCache<String, List<RenderBlock>>(300)
private val inlineMarkdownCache = SimpleLruCache<String, AnnotatedString>(1000)

// ------------------------------------------------------------------
// Render units produced by the paragraph-aware parser.
// One paragraph (\n\n-separated) maps to one or more RenderBlocks.
// ------------------------------------------------------------------
private sealed class RenderBlock {
    data class StandaloneMarkdown(
        val text: String,
    ) : RenderBlock()

    data class InlineFlow(
        val lines: List<List<InlinePart>>,
    ) : RenderBlock()

    data class DisplayMath(
        val formula: String,
    ) : RenderBlock()

    data class Image(
        val alt: String,
        val path: String,
    ) : RenderBlock()
}

private sealed class InlinePart {
    data class Text(
        val text: String,
        val isBold: Boolean = false,
    ) : InlinePart()

    data class Math(
        val formula: String,
        val isBold: Boolean = false,
    ) : InlinePart()
}

/**
 * Renders content that may contain Markdown, inline LaTeX (`$...$`),
 * display LaTeX (`$$...$$`) and embedded images.
 *
 * Parsing is paragraph-aware: the input is split on blank lines, and each
 * paragraph is classified independently — pure text → Markdown engine,
 * inline-math paragraph → FlowRow, display-math/image-only paragraph →
 * its own block.  This guarantees real vertical spacing between paragraphs
 * and prevents Chinese-text+formula paragraphs from collapsing into a
 * single misaligned row.
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    imageBasePath: String? = null,
    textStyle: TextStyle? = null,
    contentColor: Color? = null,
    format: String = "markdown",
) {
    if (content.isBlank()) return

    val resolvedTextStyle = textStyle ?: MaterialTheme.typography.bodyLarge
    val resolvedContentColor = contentColor ?: MaterialTheme.colorScheme.onSurface

    if (format == "html") {
        HtmlContent(
            html = content,
            imageBasePath = imageBasePath,
            textStyle = resolvedTextStyle,
            contentColor = resolvedContentColor,
            modifier = modifier,
        )
        return
    }

    val blocks =
        remember(content) {
            renderBlocksCache.get(content) ?: run {
                val parsed = parseRenderBlocks(content)
                renderBlocksCache.put(content, parsed)
                parsed
            }
        }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is RenderBlock.StandaloneMarkdown -> {
                    // Convert single \n to CommonMark hard break (two trailing
                    // spaces + \n) so that line breaks within a paragraph are
                    // visible — paragraphs themselves were already split on \n\n.
                    val withBreaks = block.text.replace("\n", "  \n")
                    val processed =
                        if (imageBasePath != null) {
                            resolveImagePaths(withBreaks, imageBasePath)
                        } else {
                            withBreaks
                        }
                    MarkdownBlock(
                        text = processed,
                        textStyle = resolvedTextStyle,
                        contentColor = resolvedContentColor,
                    )
                }

                is RenderBlock.InlineFlow -> {
                    InlineFlowParagraph(
                        lines = block.lines,
                        textStyle = resolvedTextStyle,
                        contentColor = resolvedContentColor,
                    )
                }

                is RenderBlock.DisplayMath -> {
                    Latex(
                        latex = block.formula,
                        modifier = Modifier.fillMaxWidth(),
                        config = latexDisplayConfig(resolvedTextStyle, resolvedContentColor),
                    )
                }

                is RenderBlock.Image -> {
                    MarkdownImage(
                        alt = block.alt,
                        path = block.path,
                        imageBasePath = imageBasePath,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineFlowParagraph(
    lines: List<List<InlinePart>>,
    textStyle: TextStyle,
    contentColor: Color,
) {
    val baseSpanStyle =
        SpanStyle(
            fontSize = textStyle.fontSize,
            fontFamily = textStyle.fontFamily,
        )
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val hasMath = line.any { it is InlinePart.Math }
            if (!hasMath) {
                // Fast-path: single pure-text line without any LaTeX formulas.
                // Renders in a single Text composable for native CJK line breaking and maximum performance.
                val lineText = line.filterIsInstance<InlinePart.Text>().joinToString("") { it.text }
                val isBold = line.filterIsInstance<InlinePart.Text>().any { it.isBold }
                val effectiveStyle = if (isBold) baseSpanStyle.copy(fontWeight = FontWeight.Bold) else baseSpanStyle
                val annotated =
                    remember(lineText, effectiveStyle) {
                        buildInlineMarkdown(lineText, effectiveStyle)
                    }
                Text(
                    text = annotated,
                    style = textStyle,
                    color = contentColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // Line contains inline LaTeX: render in FlowRow with text segments and math chips.
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    line.forEach { part ->
                        when (part) {
                            is InlinePart.Text -> {
                                if (part.text.isNotEmpty()) {
                                    val effectiveStyle =
                                        if (part.isBold) {
                                            baseSpanStyle.copy(
                                                fontWeight = FontWeight.Bold,
                                            )
                                        } else {
                                            baseSpanStyle
                                        }
                                    val annotated =
                                        remember(part.text, effectiveStyle) {
                                            buildInlineMarkdown(part.text, effectiveStyle)
                                        }
                                    Text(
                                        text = annotated,
                                        style = textStyle,
                                        color = contentColor,
                                        modifier = Modifier.align(Alignment.CenterVertically),
                                    )
                                }
                            }

                            is InlinePart.Math -> {
                                Latex(
                                    latex = part.formula,
                                    modifier =
                                        Modifier
                                            .wrapContentSize()
                                            .align(Alignment.CenterVertically),
                                    config = latexInlineConfig(textStyle, contentColor),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownImage(
    alt: String,
    path: String,
    imageBasePath: String?,
) {
    val model = remember(path, imageBasePath) { resolveImagePath(path, imageBasePath) }
    var showPreview by remember { mutableStateOf(false) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { showPreview = true }
                .padding(8.dp),
    ) {
        AsyncImage(
            model = model,
            contentDescription = alt.ifBlank { null },
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
        )
    }

    if (showPreview) {
        ImagePreviewDialog(
            model = model,
            contentDescription = alt.ifBlank { null },
            onDismiss = { showPreview = false },
        )
    }
}

@Composable
private fun ImagePreviewDialog(
    model: String,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismiss() })
                    },
        ) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }.pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = nextScale
                                offset = if (nextScale == 1f) Offset.Zero else offset + pan
                            }
                        },
            )
        }
    }
}

// ------------------------------------------------------------------
// Custom table: horizontal scroll, line borders, cell content supports
// inline LaTeX via FlowRow mixing Text + Latex composables.
// Column widths come from a SubcomposeLayout measurement pass — text
// columns cap at maxColWidth (wrapping), LaTeX columns use the
// formula's natural rendered width (no cap).
// ------------------------------------------------------------------
@Composable
private fun CustomMarkdownTable(
    content: String,
    node: org.intellij.markdown.ast.ASTNode,
    style: TextStyle,
) {
    val headerNode = node.findChildOfType(HEADER)
    val rows = node.children.filter { it.type == ROW }

    val headerCells =
        headerNode?.children?.filter { it.type == CELL }?.map { cell ->
            content.substring(cell.startOffset, cell.endOffset).trim()
        } ?: emptyList()

    val dataRows =
        rows.map { row ->
            row.children.filter { it.type == CELL }.map { cell ->
                content.substring(cell.startOffset, cell.endOffset).trim()
            }
        }

    if (headerCells.isEmpty()) return

    val columnCount = headerCells.size

    val maxColWidth =
        when {
            columnCount <= 2 -> 270.dp
            columnCount <= 4 -> 225.dp
            else -> 180.dp
        }
    val minColWidth = 60.dp
    val dividerThickness = 0.5.dp
    val cellPaddingH = 12.dp
    val cellPaddingV = 10.dp
    val tablePaddingV = 8.dp

    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val headerDividerColor = MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    val hasLatex = Regex("\\\$[^\\\$]+\\\$|\\\\\\([^)]+\\\\\\)")
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    SubcomposeLayout { constraints ->
        // ----- Phase 1: measure every cell at natural width -----
        data class Cell(
            val col: Int,
            val row: Int,
            val text: String,
            val isHeader: Boolean,
        )
        val allCells =
            headerCells.mapIndexed { col, t -> Cell(col, 0, t, true) } +
                dataRows.flatMapIndexed { rowIdx, row ->
                    row.mapIndexed { col, t -> Cell(col, rowIdx + 1, t, false) }
                }

        val measurements =
            allCells.map { cell ->
                val cellStyle = if (cell.isHeader) style.copy(fontWeight = FontWeight.SemiBold) else style
                val p =
                    subcompose("m_${cell.row}_${cell.col}") {
                        TableCellContent(cell.text, cellStyle, textColor)
                    }.first().measure(Constraints(maxWidth = Constraints.Infinity))
                Triple(cell.col, cell.row, p.width)
            }

        // ----- Phase 2: compute per-column widths -----
        val padHPx = with(density) { (cellPaddingH * 2).roundToPx() }
        val maxColWidthPx = with(density) { maxColWidth.roundToPx() }
        val minColWidthPx = with(density) { minColWidth.roundToPx() }

        val columnWidthsDp =
            (0 until columnCount).map { col ->
                val maxCellW = measurements.filter { it.first == col }.maxOf { it.third }
                val padded = maxCellW + padHPx
                val hasFormula = allCells.any { it.col == col && hasLatex.containsMatchIn(it.text) }
                val px =
                    if (hasFormula) {
                        padded.coerceAtLeast(minColWidthPx)
                    } else {
                        padded.coerceAtLeast(minColWidthPx).coerceAtMost(maxColWidthPx)
                    }
                with(density) { px.toDp() }
            }

        val tableWidth =
            (
                columnWidthsDp.map { it.value }.sum() +
                    (columnCount + 1).toFloat() * dividerThickness.value
            ).dp

        // ----- Phase 3: compose & measure the final table -----
        val tableMeasurable =
            subcompose("table") {
                Box(
                    modifier =
                        Modifier
                            .horizontalScroll(scrollState)
                            .padding(vertical = tablePaddingV),
                ) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.width(tableWidth),
                            color = outlineColor,
                            thickness = dividerThickness,
                        )
                        // Header row
                        Row(
                            modifier =
                                Modifier
                                    .width(tableWidth)
                                    .height(IntrinsicSize.Min),
                        ) {
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                color = outlineColor,
                                thickness = dividerThickness,
                            )
                            headerCells.forEachIndexed { col, cellText ->
                                Box(
                                    modifier =
                                        Modifier
                                            .width(columnWidthsDp[col])
                                            .padding(horizontal = cellPaddingH, vertical = cellPaddingV),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    TableCellContent(
                                        text = cellText,
                                        style = style.copy(fontWeight = FontWeight.SemiBold),
                                        contentColor = textColor,
                                    )
                                }
                                VerticalDivider(
                                    modifier = Modifier.fillMaxHeight(),
                                    color = outlineColor,
                                    thickness = dividerThickness,
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.width(tableWidth),
                            color = headerDividerColor,
                            thickness = dividerThickness,
                        )
                        // Data rows
                        dataRows.forEach { rowCells ->
                            Row(
                                modifier =
                                    Modifier
                                        .width(tableWidth)
                                        .height(IntrinsicSize.Min),
                            ) {
                                VerticalDivider(
                                    modifier = Modifier.fillMaxHeight(),
                                    color = outlineColor,
                                    thickness = dividerThickness,
                                )
                                rowCells.forEachIndexed { col, cellText ->
                                    Box(
                                        modifier =
                                            Modifier
                                                .width(columnWidthsDp[col])
                                                .padding(horizontal = cellPaddingH, vertical = cellPaddingV),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        TableCellContent(
                                            text = cellText,
                                            style = style,
                                            contentColor = textColor,
                                        )
                                    }
                                    VerticalDivider(
                                        modifier = Modifier.fillMaxHeight(),
                                        color = outlineColor,
                                        thickness = dividerThickness,
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.width(tableWidth),
                                color = outlineColor,
                                thickness = dividerThickness,
                            )
                        }
                    }
                }
            }
        val tableP = tableMeasurable.first().measure(constraints)
        layout(tableP.width, tableP.height) {
            tableP.placeRelative(0, 0)
        }
    }
}

/**
 * Renders a single table cell, mixing inline Markdown (bold/italic/code)
 * with inline LaTeX formulas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableCellContent(
    text: String,
    style: TextStyle,
    contentColor: Color,
) {
    val parts = remember(text) { parseInlinePartsForLine(text) }
    val baseSpanStyle =
        remember(style) {
            SpanStyle(
                fontSize = style.fontSize,
                fontFamily = style.fontFamily,
            )
        }

    FlowRow(modifier = Modifier.fillMaxWidth()) {
        parts.forEach { part ->
            when (part) {
                is InlinePart.Text -> {
                    if (part.text.isNotEmpty()) {
                        val effectiveStyle =
                            if (part.isBold) {
                                baseSpanStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                baseSpanStyle
                            }
                        val annotated = buildInlineMarkdown(part.text, effectiveStyle)
                        Text(
                            text = annotated,
                            style = style,
                            color = contentColor,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }

                is InlinePart.Math -> {
                    Latex(
                        latex = part.formula,
                        modifier =
                            Modifier
                                .wrapContentSize()
                                .align(Alignment.CenterVertically),
                        config =
                            LatexConfig(
                                fontSize = style.fontSize,
                                theme = LatexTheme.light(color = contentColor),
                            ),
                    )
                }
            }
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
    contentColor: Color,
) {
    val surfaceContainerLowest = MaterialTheme.colorScheme.surfaceContainerLowest
    val outline = MaterialTheme.colorScheme.outline
    val monoBodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

    val colors =
        remember(contentColor, surfaceContainerLowest, outline) {
            DefaultMarkdownColors(
                text = contentColor,
                codeBackground = surfaceContainerLowest,
                inlineCodeBackground = surfaceContainerLowest,
                dividerColor = outline,
                tableBackground = Color.Transparent,
            )
        }
    val baseSize = textStyle.fontSize.takeOrElse { 16.sp }
    val typography =
        remember(textStyle, contentColor, baseSize, monoBodyMedium) {
            DefaultMarkdownTypography(
                h1 = textStyle.copy(fontSize = baseSize * 1.25f, fontWeight = FontWeight.Bold),
                h2 = textStyle.copy(fontSize = baseSize * 1.15f, fontWeight = FontWeight.Bold),
                h3 = textStyle.copy(fontSize = baseSize * 1.05f, fontWeight = FontWeight.Bold),
                h4 = textStyle.copy(fontWeight = FontWeight.Bold),
                h5 = textStyle.copy(fontSize = baseSize * 0.9f, fontWeight = FontWeight.Bold),
                h6 = textStyle.copy(fontSize = baseSize * 0.85f, fontWeight = FontWeight.Bold),
                text = textStyle,
                code = monoBodyMedium,
                inlineCode = monoBodyMedium,
                quote = textStyle,
                paragraph = textStyle,
                ordered = textStyle,
                bullet = textStyle,
                list = textStyle,
                textLink =
                    TextLinkStyles(
                        style = SpanStyle(color = contentColor),
                    ),
                table = textStyle,
            )
        }
    val markdownState = rememberMarkdownState(text, retainState = true)
    val components =
        remember(colors, typography) {
            markdownComponents(
                paragraph = { model ->
                    val paragraphText = model.node.getUnescapedTextInNode(model.content)
                    if ('$' in paragraphText) {
                        val lines =
                            paragraphText
                                .split('\n')
                                .map { parseInlinePartsForLine(it) }
                                .filter { it.isNotEmpty() }
                        if (lines.isNotEmpty()) {
                            InlineFlowParagraph(
                                lines = lines,
                                textStyle = model.typography.paragraph,
                                contentColor = contentColor,
                            )
                        }
                    } else {
                        MarkdownParagraph(
                            content = model.content,
                            node = model.node,
                            style = model.typography.paragraph,
                        )
                    }
                },
                table = { model ->
                    CustomMarkdownTable(
                        content = model.content,
                        node = model.node,
                        style = model.typography.table,
                    )
                },
                codeFence = { model ->
                    MarkdownCodeFence(
                        content = model.content,
                        node = model.node,
                        style = model.typography.code,
                        block = { code, language, style ->
                            ModernCodeBlock(code = code, language = language, style = style)
                        },
                    )
                },
                codeBlock = { model ->
                    MarkdownCodeBlock(
                        content = model.content,
                        node = model.node,
                        style = model.typography.code,
                        block = { code, language, style ->
                            ModernCodeBlock(code = code, language = language, style = style)
                        },
                    )
                },
                blockQuote = { model ->
                    ModernBlockQuote(
                        content = model.content,
                        node = model.node,
                        style = model.typography.quote,
                    )
                },
            )
        }
    Markdown(
        markdownState = markdownState,
        colors = colors,
        typography = typography,
        components = components,
        imageTransformer = Coil3ImageTransformerImpl,
    )
}

// ------------------------------------------------------------------
// Modern code block: rounded card with accent bar
// ------------------------------------------------------------------
@Composable
private fun ModernCodeBlock(
    code: String,
    language: String?,
    style: TextStyle,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            // Accent bar on the left
            Box(
                modifier =
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                if (!language.isNullOrBlank()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                ).padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = language.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                Text(
                    text = code,
                    style = style,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// Modern block quote: accent bar + subtle background
// ------------------------------------------------------------------
@Composable
private fun ModernBlockQuote(
    content: String,
    node: org.intellij.markdown.ast.ASTNode,
    style: TextStyle,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(start = 0.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
                    .height(IntrinsicSize.Min),
        ) {
            // Accent bar
            Box(
                modifier =
                    Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary),
            )

            Column(modifier = Modifier.padding(start = 12.dp)) {
                val blockQuoteColor = MaterialTheme.colorScheme.onSurfaceVariant
                val quoteStyle = style.copy(color = blockQuoteColor)

                node.children.forEachIndexed { index, child ->
                    when (child.type) {
                        MarkdownElementTypes.BLOCK_QUOTE -> {
                            ModernBlockQuote(
                                content = content,
                                node = child,
                                style = quoteStyle,
                            )
                        }

                        MarkdownTokenTypes.EOL -> {
                            // Skip blank lines
                        }

                        else -> {
                            com.mikepenz.markdown.compose.MarkdownElement(
                                node = child,
                                components = com.mikepenz.markdown.compose.LocalMarkdownComponents.current,
                                content = content,
                                includeSpacer = false,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Parsing: split raw string into RenderBlocks (paragraph-aware).
// ------------------------------------------------------------------
private fun parseRenderBlocks(content: String): List<RenderBlock> {
    val result = mutableListOf<RenderBlock>()
    // Preprocess: ensure tables are preceded by an empty line
    val preprocessed = content.replace(Regex("([^\\n])\\n(\\|.*\\n\\|[-:| ]+\\|)"), "$1\n\n$2")
    val paragraphs = preprocessed.split(Regex("\\n{2,}"))
    for (rawPara in paragraphs) {
        val para = rawPara.trim()
        if (para.isEmpty()) continue
        result.addAll(parseParagraph(para))
    }
    return result
}

private val WHOLE_IMAGE = Regex("^!\\[([^\\]]*)]\\(([^)]+)\\)$")
private val WHOLE_DISPLAY_MATH = Regex("^\\\$\\\$([\\s\\S]+?)\\\$\\\$$|^\\\\\\[([\\s\\S]+?)\\\\\\]$")
private val WHOLE_INLINE_MATH = Regex("^\\\$([^\\\$]+?)\\\$$|^\\\\\\((?!\\s*\\\\\\))(.+?)\\\\\\)$")
private val ANCHOR_REGEX =
    Regex(
        "(!\\[[^\\]]*]\\([^)]+\\))" + // group 1: image
            "|(\\\$\\\$[\\s\\S]+?\\\$\\\$)" + // group 2: display math $$...$$
            "|(\\\\\\[[\\s\\S]+?\\\\\\])", // group 3: display math \[...\]
    )
private val IMAGE_PARTS = Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")

private fun parseParagraph(paragraph: String): List<RenderBlock> {
    WHOLE_IMAGE.matchEntire(paragraph)?.let {
        return listOf(RenderBlock.Image(it.groupValues[1], it.groupValues[2]))
    }
    WHOLE_DISPLAY_MATH.matchEntire(paragraph)?.let {
        val mathContent = if (it.groups[1] != null) it.groupValues[1] else it.groupValues[2]
        return listOf(RenderBlock.DisplayMath(mathContent.trim()))
    }
    // A whole-paragraph $...$ is treated as display math too — common in
    // imported textbook content where standalone formulas use single-$.
    WHOLE_INLINE_MATH.matchEntire(paragraph)?.let {
        val body = if (it.groups[1] != null) it.groupValues[1] else it.groupValues[2]
        if (!body.contains('\n')) {
            return listOf(RenderBlock.DisplayMath(body.trim()))
        }
    }

    val anchors = ANCHOR_REGEX.findAll(paragraph).toList()
    if (anchors.isEmpty()) {
        return parseTextSegment(paragraph)
    }

    val result = mutableListOf<RenderBlock>()
    var lastEnd = 0
    for (m in anchors) {
        val before = paragraph.substring(lastEnd, m.range.first)
        if (before.isNotBlank()) {
            result.addAll(parseTextSegment(before))
        }
        if (m.groups[1] != null) {
            val img = IMAGE_PARTS.matchEntire(m.value)
            if (img != null) {
                result.add(RenderBlock.Image(img.groupValues[1], img.groupValues[2]))
            }
        } else {
            val isEscapedBracket = m.value.startsWith("\\[")
            val mathContent =
                if (isEscapedBracket) {
                    m.value
                        .removePrefix("\\[")
                        .removeSuffix("\\]")
                        .trim()
                } else {
                    m.value
                        .removePrefix("$$")
                        .removeSuffix("$$")
                        .trim()
                }
            result.add(RenderBlock.DisplayMath(mathContent))
        }
        lastEnd = m.range.last + 1
    }
    val after = paragraph.substring(lastEnd)
    if (after.isNotBlank()) {
        result.addAll(parseTextSegment(after))
    }
    return result
}

private fun parseTextSegment(text: String): List<RenderBlock> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return emptyList()
    val lines =
        trimmed
            .split('\n')
            .map { parseInlinePartsForLine(it) }
            .filter { it.isNotEmpty() }
    if (lines.isEmpty()) return emptyList()
    val anyMath = lines.any { line -> line.any { it is InlinePart.Math } }

    // Paragraphs that contain tables, lists, blockquotes or code fences
    // should always go through the full Markdown engine — even if they
    // also contain LaTeX formulas.  The full renderer handles the
    // structural elements correctly; inline formulas inside them will
    // simply render as plain text (the $...$ syntax is not native
    // Markdown).
    val hasComplexStructure =
        trimmed.lines().any { line ->
            val t = line.trimStart()
            t.startsWith("|") || // table row
                Regex("^[-*+]\\s").containsMatchIn(t) || // unordered list
                Regex("^\\d+\\.\\s").containsMatchIn(t) || // ordered list
                t.startsWith(">") || // blockquote
                t.startsWith("```") // code fence
        }

    return if (anyMath && !hasComplexStructure) {
        listOf(RenderBlock.InlineFlow(lines))
    } else {
        // Pure-text paragraph (possibly multi-line). Defer to the Markdown
        // engine; the caller adds CommonMark hard breaks for single \n.
        listOf(RenderBlock.StandaloneMarkdown(trimmed))
    }
}

private val INLINE_MATH_REGEX =
    Regex("(?<!\\\$)\\\$(?!\\\$)([^\\n\\\$]+?)\\\$(?!\\\$)|\\\\\\((?!\\s*\\\\\\))([^\\n]+?)\\\\\\)")

private val BOLD_DELIMITER = Regex("\\*\\*")

private fun parseInlinePartsForLine(line: String): List<InlinePart> {
    if (line.isEmpty()) return emptyList()

    val mathRanges = INLINE_MATH_REGEX.findAll(line).map { it.range }.toList()

    val boldPositions =
        BOLD_DELIMITER
            .findAll(line)
            .map { it.range.first }
            .filter { pos -> mathRanges.none { pos in it } }
            .toList()

    if (boldPositions.size < 2) {
        return splitOnMath(line, bold = false)
    }

    val paired =
        boldPositions.chunked(2).mapNotNull { pair ->
            if (pair.size == 2) pair[0] to pair[1] else null
        }

    if (paired.isEmpty()) return splitOnMath(line, bold = false)

    val parts = mutableListOf<InlinePart>()
    var pos = 0

    for ((openPos, closePos) in paired) {
        if (openPos > pos) {
            parts.addAll(splitOnMath(line.substring(pos, openPos), bold = false))
        }
        val inner = line.substring(openPos + 2, closePos)
        parts.addAll(splitOnMath(inner, bold = true))
        pos = closePos + 2
    }
    if (pos < line.length) {
        parts.addAll(splitOnMath(line.substring(pos), bold = false))
    }

    return parts.ifEmpty { splitOnMath(line, bold = false) }
}

private fun splitOnMath(
    text: String,
    bold: Boolean,
): List<InlinePart> {
    if (text.isEmpty()) return emptyList()
    val parts = mutableListOf<InlinePart>()
    var lastEnd = 0
    for (m in INLINE_MATH_REGEX.findAll(text)) {
        if (m.range.first > lastEnd) {
            val t = text.substring(lastEnd, m.range.first)
            if (t.isNotEmpty()) parts.add(InlinePart.Text(t, isBold = bold))
        }
        val mathContent = if (m.groups[1] != null) m.groupValues[1] else m.groupValues[2]
        parts.add(InlinePart.Math(mathContent.trim(), isBold = bold))
        lastEnd = m.range.last + 1
    }
    if (lastEnd < text.length) {
        parts.add(InlinePart.Text(text.substring(lastEnd), isBold = bold))
    }
    return parts.ifEmpty {
        if (text.isNotEmpty()) listOf(InlinePart.Text(text, isBold = bold)) else emptyList()
    }
}

// ------------------------------------------------------------------
// Inline-markdown → AnnotatedString (bold / italic / code / strike)
// ------------------------------------------------------------------
private data class InlineMark(
    val start: Int,
    val end: Int,
    val innerStart: Int,
    val innerEnd: Int,
    val style: (SpanStyle) -> SpanStyle,
)

private fun collectInlineMarks(text: String): List<InlineMark> {
    val marks = mutableListOf<InlineMark>()

    val boldAsterisk = Regex("\\*\\*(.+?)\\*\\*")
    boldAsterisk.findAll(text).forEach { m ->
        marks.add(
            InlineMark(
                m.range.first,
                m.range.last + 1,
                m.range.first + 2,
                m.range.last - 1,
                { it.copy(fontWeight = FontWeight.Bold) },
            ),
        )
    }
    val boldUnderscore = Regex("__(.+?)__")
    boldUnderscore.findAll(text).forEach { m ->
        marks.add(
            InlineMark(
                m.range.first,
                m.range.last + 1,
                m.range.first + 2,
                m.range.last - 1,
                { it.copy(fontWeight = FontWeight.Bold) },
            ),
        )
    }
    val italicAsterisk = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    italicAsterisk.findAll(text).forEach { m ->
        marks.add(
            InlineMark(
                m.range.first,
                m.range.last + 1,
                m.range.first + 1,
                m.range.last,
                { it.copy(fontStyle = FontStyle.Italic) },
            ),
        )
    }
    val italicUnderscore = Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)")
    italicUnderscore.findAll(text).forEach { m ->
        marks.add(
            InlineMark(
                m.range.first,
                m.range.last + 1,
                m.range.first + 1,
                m.range.last,
                { it.copy(fontStyle = FontStyle.Italic) },
            ),
        )
    }
    val code = Regex("`([^`]+)`")
    code.findAll(text).forEach { m ->
        marks.add(
            InlineMark(
                m.range.first,
                m.range.last + 1,
                m.range.first + 1,
                m.range.last,
                { it.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
            ),
        )
    }
    val strike = Regex("~~(.+?)~~")
    strike.findAll(text).forEach { m ->
        marks.add(
            InlineMark(
                m.range.first,
                m.range.last + 1,
                m.range.first + 2,
                m.range.last - 1,
                { it.copy(textDecoration = TextDecoration.LineThrough) },
            ),
        )
    }

    return marks.sortedBy { it.start }
}

/** Build an [AnnotatedString] from a short Markdown fragment, stripping
 * delimiters and applying [SpanStyle] decorations for bold, italic,
 * inline code and strikethrough. */
private fun buildInlineMarkdown(
    text: String,
    baseStyle: SpanStyle,
): AnnotatedString {
    val cacheKey = "${text.hashCode()}_${baseStyle.fontWeight?.weight ?: 0}_${baseStyle.fontSize.value}"
    inlineMarkdownCache.get(cacheKey)?.let { return it }

    val marks = collectInlineMarks(text)
    val result =
        if (marks.isEmpty()) {
            AnnotatedString(text, baseStyle)
        } else {
            buildAnnotatedString {
                var cursor = 0
                for (m in marks) {
                    if (m.start < cursor) continue
                    if (cursor < m.start) {
                        withStyle(baseStyle) { append(text, cursor, m.start) }
                    }
                    withStyle(m.style(baseStyle)) {
                        append(text, m.innerStart, m.innerEnd)
                    }
                    cursor = m.end
                }
                if (cursor < text.length) {
                    withStyle(baseStyle) { append(text, cursor, text.length) }
                }
            }
        }
    inlineMarkdownCache.put(cacheKey, result)
    return result
}

// ------------------------------------------------------------------
// HTML content handler
// ------------------------------------------------------------------
private val HTML_IMG_REGEX =
    Regex(
        """<img[^>]*src=["']([^"']*)["'][^>]*/?>""",
        RegexOption.IGNORE_CASE,
    )

private data class HtmlSegment(
    val text: String,
    val imagePath: String? = null,
)

private fun parseHtmlSegments(html: String): List<HtmlSegment> {
    val segments = mutableListOf<HtmlSegment>()
    var lastEnd = 0
    for (m in HTML_IMG_REGEX.findAll(html)) {
        val before = html.substring(lastEnd, m.range.first)
        if (before.isNotBlank()) {
            segments.add(HtmlSegment(text = before))
        }
        segments.add(HtmlSegment(text = m.value, imagePath = m.groupValues[1]))
        lastEnd = m.range.last + 1
    }
    val after = html.substring(lastEnd)
    if (after.isNotBlank()) {
        segments.add(HtmlSegment(text = after))
    }
    return segments.ifEmpty { listOf(HtmlSegment(text = html)) }
}

@Composable
private fun HtmlContent(
    html: String,
    imageBasePath: String?,
    textStyle: TextStyle,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val segments = remember(html) { parseHtmlSegments(html) }
    Column(modifier = modifier) {
        segments.forEach { seg ->
            if (seg.imagePath != null) {
                MarkdownImage(
                    alt = "",
                    path = seg.imagePath,
                    imageBasePath = imageBasePath,
                )
            } else {
                val plainText =
                    remember(seg.text) {
                        HtmlCompat
                            .fromHtml(seg.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                            .toString()
                            .trimEnd()
                    }
                Text(
                    text = plainText,
                    style = textStyle,
                    color = contentColor,
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// LaTeX config helpers
// ------------------------------------------------------------------
private fun latexInlineConfig(
    textStyle: TextStyle,
    contentColor: Color,
): LatexConfig {
    val size = textStyle.fontSize.takeOrElse { 16.sp }
    return LatexConfig(
        fontSize = size,
        theme = LatexTheme.light(color = contentColor),
    )
}

private fun latexDisplayConfig(
    textStyle: TextStyle,
    contentColor: Color,
): LatexConfig {
    val size = textStyle.fontSize.takeOrElse { 16.sp } * 1.1f
    return LatexConfig(
        fontSize = size,
        theme = LatexTheme.light(color = contentColor),
    )
}

// ------------------------------------------------------------------
// Image path resolution
// ------------------------------------------------------------------
private fun resolveImagePaths(
    content: String,
    basePath: String,
): String {
    val regex = "!\\[(.*?)]\\((.*?)\\)".toRegex()
    return regex.replace(content) { matchResult ->
        val alt = matchResult.groupValues[1]
        val path = matchResult.groupValues[2]
        "![$alt](${resolveImagePath(path, basePath)})"
    }
}

private fun resolveImagePath(
    path: String,
    basePath: String?,
): String {
    if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://")) {
        return path
    }
    return if (basePath == null) {
        path
    } else {
        "file://${File(basePath, path).absolutePath}"
    }
}
