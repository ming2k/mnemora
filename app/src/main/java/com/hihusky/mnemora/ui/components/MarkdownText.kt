package com.hihusky.mnemora.ui.components

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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.mikepenz.markdown.coil2.Coil2ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import java.io.File

// ------------------------------------------------------------------
// Render units produced by the paragraph-aware parser.
// One paragraph (\n\n-separated) maps to one or more RenderBlocks.
// ------------------------------------------------------------------
private sealed class RenderBlock {
    data class StandaloneMarkdown(val text: String) : RenderBlock()
    data class InlineFlow(val lines: List<List<InlinePart>>) : RenderBlock()
    data class DisplayMath(val formula: String) : RenderBlock()
    data class Image(val alt: String, val path: String) : RenderBlock()
}

private sealed class InlinePart {
    data class Text(val text: String) : InlinePart()
    data class Math(val formula: String) : InlinePart()
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
    format: String = "markdown"
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
            modifier = modifier
        )
        return
    }

    val blocks = remember(content) { parseRenderBlocks(content) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is RenderBlock.StandaloneMarkdown -> {
                    // Convert single \n to CommonMark hard break (two trailing
                    // spaces + \n) so that line breaks within a paragraph are
                    // visible — paragraphs themselves were already split on \n\n.
                    val withBreaks = block.text.replace("\n", "  \n")
                    val processed = if (imageBasePath != null) {
                        resolveImagePaths(withBreaks, imageBasePath)
                    } else withBreaks
                    MarkdownBlock(
                        text = processed,
                        textStyle = resolvedTextStyle,
                        contentColor = resolvedContentColor
                    )
                }

                is RenderBlock.InlineFlow -> InlineFlowParagraph(
                    lines = block.lines,
                    textStyle = resolvedTextStyle,
                    contentColor = resolvedContentColor
                )

                is RenderBlock.DisplayMath -> Latex(
                    latex = block.formula,
                    modifier = Modifier.fillMaxWidth(),
                    config = latexDisplayConfig(resolvedTextStyle, resolvedContentColor)
                )

                is RenderBlock.Image -> MarkdownImage(
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
private fun InlineFlowParagraph(
    lines: List<List<InlinePart>>,
    textStyle: TextStyle,
    contentColor: Color
) {
    val baseSpanStyle = SpanStyle(
        fontSize = textStyle.fontSize,
        fontFamily = textStyle.fontFamily
    )
    // Each input line is its own FlowRow — single \n inside the paragraph
    // forces a visible line break, while the FlowRow still wraps a single
    // line's content when it overflows the screen width.
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            FlowRow(modifier = Modifier.fillMaxWidth()) {
                line.forEach { part ->
                    when (part) {
                        is InlinePart.Text -> {
                            // Splitting on punctuation/whitespace lets FlowRow
                            // wrap each phrase independently instead of treating
                            // the whole sentence as one block (which would push
                            // following formulas onto a new row).
                            splitIntoPhrases(part.text).forEach { phrase ->
                                val annotated = remember(phrase) {
                                    buildInlineMarkdown(phrase, baseSpanStyle)
                                }
                                Text(
                                    text = annotated,
                                    style = textStyle,
                                    color = contentColor,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }

                        is InlinePart.Math -> {
                            Latex(
                                latex = part.formula,
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.CenterVertically),
                                config = latexInlineConfig(textStyle, contentColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun splitIntoPhrases(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    // Each CJK character (incl. full-width punctuation) becomes its own
    // FlowRow item, so a long Chinese run can wrap at any character — and
    // an inline formula can sit on the same row as whatever fits beside it
    // instead of pushing the trailing text onto a new row. Latin runs stay
    // grouped per word (split on whitespace) so words don't break mid-word.
    val result = mutableListOf<String>()
    val buf = StringBuilder()
    for (ch in text) {
        when {
            isCjk(ch) -> {
                if (buf.isNotEmpty()) {
                    result.add(buf.toString())
                    buf.clear()
                }
                result.add(ch.toString())
            }
            ch.isWhitespace() -> {
                buf.append(ch)
                result.add(buf.toString())
                buf.clear()
            }
            else -> buf.append(ch)
        }
    }
    if (buf.isNotEmpty()) result.add(buf.toString())
    return result
}

private fun isCjk(ch: Char): Boolean {
    val code = ch.code
    return code in 0x4E00..0x9FFF ||   // CJK Unified Ideographs
        code in 0x3400..0x4DBF ||      // CJK Extension A
        code in 0xF900..0xFAFF ||      // CJK Compatibility Ideographs
        code in 0x3000..0x303F ||      // CJK Symbols and Punctuation
        code in 0xFF00..0xFFEF         // Halfwidth/Fullwidth (incl 全角标点)
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
        h1 = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        h2 = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        h3 = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        h4 = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        h5 = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        h6 = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
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
    val markdownState = rememberMarkdownState(text, retainState = true)
    Markdown(
        markdownState = markdownState,
        colors = colors,
        typography = typography,
        imageTransformer = Coil2ImageTransformerImpl
    )
}

// ------------------------------------------------------------------
// Parsing: split raw string into RenderBlocks (paragraph-aware).
// ------------------------------------------------------------------
private fun parseRenderBlocks(content: String): List<RenderBlock> {
    val result = mutableListOf<RenderBlock>()
    val paragraphs = content.split(Regex("\\n{2,}"))
    for (rawPara in paragraphs) {
        val para = rawPara.trim()
        if (para.isEmpty()) continue
        result.addAll(parseParagraph(para))
    }
    return result
}

private val WHOLE_IMAGE = Regex("^!\\[([^\\]]*)]\\(([^)]+)\\)$")
private val WHOLE_DISPLAY_MATH = Regex("^\\\$\\\$([\\s\\S]+?)\\\$\\\$$")
private val WHOLE_INLINE_MATH = Regex("^\\\$([^\\\$]+?)\\\$$")
private val ANCHOR_REGEX = Regex(
    "(!\\[[^\\]]*]\\([^)]+\\))" +    // group 1: image
        "|(\\\$\\\$[\\s\\S]+?\\\$\\\$)"  // group 2: display math
)
private val IMAGE_PARTS = Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")

private fun parseParagraph(paragraph: String): List<RenderBlock> {
    WHOLE_IMAGE.matchEntire(paragraph)?.let {
        return listOf(RenderBlock.Image(it.groupValues[1], it.groupValues[2]))
    }
    WHOLE_DISPLAY_MATH.matchEntire(paragraph)?.let {
        return listOf(RenderBlock.DisplayMath(it.groupValues[1].trim()))
    }
    // A whole-paragraph $...$ is treated as display math too — common in
    // imported textbook content where standalone formulas use single-$.
    WHOLE_INLINE_MATH.matchEntire(paragraph)?.let {
        val body = it.groupValues[1]
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
        if (m.groupValues[1].isNotEmpty()) {
            val img = IMAGE_PARTS.matchEntire(m.value)
            if (img != null) {
                result.add(RenderBlock.Image(img.groupValues[1], img.groupValues[2]))
            }
        } else {
            val mathContent = m.value.removePrefix("$$").removeSuffix("$$").trim()
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
    val lines = trimmed.split('\n').map { parseInlinePartsForLine(it) }
        .filter { it.isNotEmpty() }
    if (lines.isEmpty()) return emptyList()
    val anyMath = lines.any { line -> line.any { it is InlinePart.Math } }

    // Paragraphs that contain tables, lists, blockquotes or code fences
    // should always go through the full Markdown engine — even if they
    // also contain LaTeX formulas.  The full renderer handles the
    // structural elements correctly; inline formulas inside them will
    // simply render as plain text (the $...$ syntax is not native
    // Markdown).
    val hasComplexStructure = trimmed.lines().any { line ->
        val t = line.trimStart()
        t.startsWith("|") ||          // table row
            t.matches(Regex("^[-*+\\d+.]\\s")) || // list item
            t.startsWith(">") ||      // blockquote
            t.startsWith("```")       // code fence
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
    Regex("(?<!\\\$)\\\$(?!\\\$)([^\\n\\\$]+?)\\\$(?!\\\$)")

private fun parseInlinePartsForLine(line: String): List<InlinePart> {
    if (line.isEmpty()) return emptyList()
    val parts = mutableListOf<InlinePart>()
    var lastEnd = 0
    for (m in INLINE_MATH_REGEX.findAll(line)) {
        val before = line.substring(lastEnd, m.range.first)
        if (before.isNotEmpty()) {
            parts.add(InlinePart.Text(before))
        }
        parts.add(InlinePart.Math(m.groupValues[1].trim()))
        lastEnd = m.range.last + 1
    }
    val after = line.substring(lastEnd)
    if (after.isNotEmpty()) {
        parts.add(InlinePart.Text(after))
    }
    return parts
}

// ------------------------------------------------------------------
// Inline-markdown → AnnotatedString (bold / italic / code / strike)
// ------------------------------------------------------------------
private data class InlineMark(
    val start: Int,
    val end: Int,
    val innerStart: Int,
    val innerEnd: Int,
    val style: (SpanStyle) -> SpanStyle
)

private fun collectInlineMarks(text: String): List<InlineMark> {
    val marks = mutableListOf<InlineMark>()

    val boldAsterisk = Regex("\\*\\*(.+?)\\*\\*")
    boldAsterisk.findAll(text).forEach { m ->
        marks.add(InlineMark(m.range.first, m.range.last + 1,
            m.range.first + 2, m.range.last - 1,
            { it.copy(fontWeight = FontWeight.Bold) }))
    }
    val boldUnderscore = Regex("__(.+?)__")
    boldUnderscore.findAll(text).forEach { m ->
        marks.add(InlineMark(m.range.first, m.range.last + 1,
            m.range.first + 2, m.range.last - 1,
            { it.copy(fontWeight = FontWeight.Bold) }))
    }
    val italicAsterisk = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    italicAsterisk.findAll(text).forEach { m ->
        marks.add(InlineMark(m.range.first, m.range.last + 1,
            m.range.first + 1, m.range.last,
            { it.copy(fontStyle = FontStyle.Italic) }))
    }
    val italicUnderscore = Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)")
    italicUnderscore.findAll(text).forEach { m ->
        marks.add(InlineMark(m.range.first, m.range.last + 1,
            m.range.first + 1, m.range.last,
            { it.copy(fontStyle = FontStyle.Italic) }))
    }
    val code = Regex("`([^`]+)`")
    code.findAll(text).forEach { m ->
        marks.add(InlineMark(m.range.first, m.range.last + 1,
            m.range.first + 1, m.range.last,
            { it.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }))
    }
    val strike = Regex("~~(.+?)~~")
    strike.findAll(text).forEach { m ->
        marks.add(InlineMark(m.range.first, m.range.last + 1,
            m.range.first + 2, m.range.last - 1,
            { it.copy(textDecoration = TextDecoration.LineThrough) }))
    }

    return marks.sortedBy { it.start }
}

/** Build an [AnnotatedString] from a short Markdown fragment, stripping
 * delimiters and applying [SpanStyle] decorations for bold, italic,
 * inline code and strikethrough. */
private fun buildInlineMarkdown(
    text: String,
    baseStyle: SpanStyle
): AnnotatedString {
    val marks = collectInlineMarks(text)
    if (marks.isEmpty()) return AnnotatedString(text, baseStyle)

    return buildAnnotatedString {
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

// ------------------------------------------------------------------
// HTML content handler
// ------------------------------------------------------------------
private val HTML_IMG_REGEX = Regex(
    """<img[^>]*src=["']([^"']*)["'][^>]*/?>""",
    RegexOption.IGNORE_CASE
)

private data class HtmlSegment(val text: String, val imagePath: String? = null)

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
    modifier: Modifier = Modifier
) {
    val segments = remember(html) { parseHtmlSegments(html) }
    Column(modifier = modifier) {
        segments.forEach { seg ->
            if (seg.imagePath != null) {
                MarkdownImage(
                    alt = "",
                    path = seg.imagePath,
                    imageBasePath = imageBasePath
                )
            } else {
                val plainText = remember(seg.text) {
                    HtmlCompat.fromHtml(seg.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        .toString()
                        .trimEnd()
                }
                Text(
                    text = plainText,
                    style = textStyle,
                    color = contentColor
                )
            }
        }
    }
}

// ------------------------------------------------------------------
// LaTeX config helpers
// ------------------------------------------------------------------
private fun latexInlineConfig(textStyle: TextStyle, contentColor: Color): LatexConfig {
    val size = textStyle.fontSize.takeOrElse { 16.sp }
    return LatexConfig(
        fontSize = size,
        color = contentColor,
        darkColor = contentColor
    )
}

private fun latexDisplayConfig(textStyle: TextStyle, contentColor: Color): LatexConfig {
    val size = textStyle.fontSize.takeOrElse { 16.sp } * 1.1f
    return LatexConfig(
        fontSize = size,
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
