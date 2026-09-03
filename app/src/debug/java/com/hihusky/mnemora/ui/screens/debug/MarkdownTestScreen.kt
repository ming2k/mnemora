package com.hihusky.mnemora.ui.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hihusky.mnemora.ui.components.MarkdownText
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TEST_CASES =
    listOf(
        "table_formula" to (
            "| Feature | Formula | Description | Units |\n" +
                "|---------|---------|-------------|-------|\n" +
                "| Area | ${'$'}A = \\pi r^2${'$'} | The total surface enclosed within a circular boundary, " +
                "fundamental in geometry and physics calculations involving circular objects | m² |\n" +
                "| Volume | ${'$'}V = \\frac{4}{3}\\pi r^3${'$'} | The three-dimensional space occupied by a " +
                "spherical object, essential in fluid dynamics and material science | m³ |\n" +
                "| Velocity | ${'$'}v = \\frac{ds}{dt}${'$'} | The rate of change of position with respect to time, representing both speed and direction of motion in kinematics | m/s |"
        ),
        "math_no_spaces" to (
            "测试中文环境下公式${'$'}E=mc^2${'$'}前后没有空格的情况，以及英文环境The formula\\(x=1\\)is valid.\n\n" +
                "The formula${'$'}E=mc^2${'$'}without spaces.\n" +
                "Block math without spaces:\n" +
                "text\$\$\\int_0^1 x dx\$\$text\n" +
                "Escaped bracket \\[ y=2 \\] test."
        ),
        "empty_brackets" to (
            "Empty brackets \\(\\) and \\( \\) should not render as math formulas.\n" +
                "They often appear in LLM text as normal punctuation like fill-in-the-blank blanks.\n" +
                "But actual math \\( x = 1 \\) works fine."
        ),
        "table_no_newline" to (
            "This is a paragraph immediately followed by a table without an empty line.\n" +
                "| A | B |\n" +
                "|---|---|\n" +
                "| 1 | 2 |"
        ),
        "list_bold" to (
            "Key points:\n" +
                "- **Newton's First Law**: object in motion stays in motion\n" +
                "- **Second Law**: ${'$'}F = ma${'$'} relates force and acceleration\n" +
                "- **Third Law**: action **reaction** pairs"
        ),
        "mixed_paragraphs" to (
            "This is paragraph one.\n\n" +
                "This is paragraph two.\n" +
                "It has a soft break.\n\n" +
                "> Blockquote time\n" +
                "> with ${'$'}x=2${'$'} inside."
        ),
        "code_block" to (
            "Example Kotlin code:\n" +
                "```kotlin\n" +
                "fun main() {\n" +
                "    val x = 42\n" +
                "    println(\"Value: ${'$'}x\")\n" +
                "}\n" +
                "```\n" +
                "Inline code: `val result = compute()`"
        ),
        "headings" to (
            "# Main Title\n" +
                "## Section A\n" +
                "Some text here.\n" +
                "### Subsection 1.1\n" +
                "More details.\n" +
                "## Section B\n" +
                "Final thoughts."
        ),
        "table_code" to (
            "| Language | Example | Notes |\n" +
                "|----------|---------|-------|\n" +
                "| Kotlin | `val x = 42` | Immutable |\n" +
                "| Python | `x = 42` | Dynamic |\n" +
                "| Rust | `let x = 42` | **Ownership**: borrow checker ensures safety |"
        ),
        "bold_punct" to (
            "**Important**: This is bold followed by colon.\n" +
                "**Note**; semicolon also works.\n" +
                "**Warning**. period too.\n" +
                "**Key**, comma as well."
        ),
        "bold_latex" to (
            "**${'$'}E=mc^2${'$'}** is the most famous equation.\n\n" +
                "The **formula ${'$'}F=ma${'$'}** relates force and acceleration.\n\n" +
                "**Key concept**: the energy ${'$'}E${'$'} equals mass ${'$'}m${'$'} times ${'$'}c^2${'$'}."
        ),
        "streaming" to (
            "The **quick** brown ${'$'}f(x)${'$'} jumps over the lazy dog. " +
                "This simulates how ${'$'}\\frac{1}{2}${'$'} appears during token streaming."
        ),
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarkdownTestScreen(onBack: () -> Unit) {
    var rawText by remember { mutableStateOf(TEST_CASES.first().second) }
    var streamingText by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamingJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    var moreExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Markdown Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Preset buttons — 3 rows max, overflow in "More" popup
            val visibleCount = 9
            val visibleCases = TEST_CASES.take(visibleCount)
            val hiddenCases = TEST_CASES.drop(visibleCount)

            FlowRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                visibleCases.forEach { (label, content) ->
                    Button(
                        onClick = {
                            rawText = content
                            streamingText = ""
                            isStreaming = false
                            streamingJob?.cancel()
                            streamingJob = null
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(label.replace("_", " "), style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (hiddenCases.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { moreExpanded = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("More…", style = MaterialTheme.typography.labelSmall)
                        }
                        DropdownMenu(
                            expanded = moreExpanded,
                            onDismissRequest = { moreExpanded = false },
                        ) {
                            hiddenCases.forEach { (label, content) ->
                                DropdownMenuItem(
                                    text = { Text(label.replace("_", " ")) },
                                    onClick = {
                                        rawText = content
                                        streamingText = ""
                                        isStreaming = false
                                        streamingJob?.cancel()
                                        streamingJob = null
                                        moreExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // Streaming simulation toggle
            if (isStreaming) {
                OutlinedButton(
                    onClick = {
                        streamingJob?.cancel()
                        streamingJob = null
                        streamingText = ""
                        isStreaming = false
                    },
                    modifier = Modifier.padding(horizontal = MnemoraSpacing.Large),
                ) {
                    Text("Stop Streaming")
                }
            } else {
                Button(
                    onClick = {
                        isStreaming = true
                        streamingText = ""
                        val source = rawText
                        streamingJob =
                            scope.launch {
                                source.forEach { ch ->
                                    streamingText += ch
                                    delay(30)
                                }
                                isStreaming = false
                                streamingJob = null
                            }
                    },
                    modifier = Modifier.padding(horizontal = MnemoraSpacing.Large),
                ) {
                    Text("Simulate Streaming")
                }
            }

            Spacer(modifier = Modifier.height(MnemoraSpacing.Medium))

            // Raw input
            OutlinedTextField(
                value = rawText,
                onValueChange = {
                    rawText = it
                    streamingText = ""
                    streamingJob?.cancel()
                    streamingJob = null
                    isStreaming = false
                },
                label = { Text("Raw Markdown") },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MnemoraSpacing.Large)
                        .height(160.dp),
                textStyle = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(MnemoraSpacing.Medium))

            // Rendered preview
            val displayText = streamingText.ifBlank { rawText }
            if (displayText.isNotBlank()) {
                MarkdownText(
                    content = displayText,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MnemoraSpacing.Large),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
