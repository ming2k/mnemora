package com.hihusky.mnemora.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.hihusky.mnemora.ui.components.MnemoraSettingsRow
import com.hihusky.mnemora.ui.components.MnemoraSettingsDivider
import com.hihusky.mnemora.ui.components.MnemoraSettingsDropdownRow
import com.hihusky.mnemora.ui.components.MnemoraSettingsGroup
import com.hihusky.mnemora.ui.components.MnemoraSettingsSectionHeader
import com.hihusky.mnemora.ui.components.MnemoraSettingsSwitchRow
import com.hihusky.mnemora.ui.components.topappbar.MnemoraCollapsibleTopAppBar
import com.hihusky.mnemora.ui.screens.debug.DebugSettingsSection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.hihusky.mnemora.BuildConfig
import com.hihusky.mnemora.domain.service.AiProviderCatalog
import com.hihusky.mnemora.domain.service.AiProtocol
import com.hihusky.mnemora.ui.theme.MnemoraSpacing
import com.hihusky.mnemora.ui.theme.MnemoraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToMarkdownTest: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onThemeModeSelect = { viewModel.setThemeMode(it) },
        onLocaleSelect = { viewModel.setLocale(it) },
        onAutoAdvanceChange = { viewModel.setAutoAdvance(it) },
        onShowAnalysisChange = { viewModel.setShowAnalysis(it) },
        onSoundEffectsChange = { viewModel.setSoundEffects(it) },
        onHapticFeedbackChange = { viewModel.setHapticFeedback(it) },
        onContinuousFeedbackChange = { viewModel.setContinuousFeedback(it) },
        onConfettiEffectChange = { viewModel.setConfettiEffect(it) },
        onShowPracticeProgressChange = { viewModel.setShowPracticeProgress(it) },
        onAiProviderSelect = { viewModel.setAiProvider(it) },
        onAiApiKeyChange = { viewModel.setAiApiKey(it) },
        onAiProjectIdChange = { viewModel.setAiProjectId(it) },
        onAiLocationChange = { viewModel.setAiLocation(it) },
        onAiBaseUrlChange = { viewModel.setAiBaseUrl(it) },
        onAiModelChange = { viewModel.setAiModel(it) },
        onAiContextIncludeStemChange = { viewModel.setAiContextIncludeStem(it) },
        onAiContextIncludeOptionsChange = { viewModel.setAiContextIncludeOptions(it) },
        onAiContextIncludeAnswerChange = { viewModel.setAiContextIncludeAnswer(it) },
        onAiContextIncludeExplanationChange = { viewModel.setAiContextIncludeExplanation(it) },
        onAiThinkingModeChange = { viewModel.setAiThinkingMode(it) },
        onAiReasoningEffortChange = { viewModel.setAiReasoningEffort(it) },
        onAiSystemPromptChange = { viewModel.setAiSystemPrompt(it) },
        onNavigateToMarkdownTest = onNavigateToMarkdownTest
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onThemeModeSelect: (Int) -> Unit,
    onLocaleSelect: (String) -> Unit,
    onAutoAdvanceChange: (Boolean) -> Unit,
    onShowAnalysisChange: (Boolean) -> Unit,
    onShowPracticeProgressChange: (Boolean) -> Unit,
    onSoundEffectsChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onContinuousFeedbackChange: (Boolean) -> Unit,
    onConfettiEffectChange: (Boolean) -> Unit,
    onAiProviderSelect: (String) -> Unit,
    onAiApiKeyChange: (String) -> Unit,
    onAiProjectIdChange: (String) -> Unit,
    onAiLocationChange: (String) -> Unit,
    onAiBaseUrlChange: (String) -> Unit,
    onAiModelChange: (String) -> Unit,
    onAiContextIncludeStemChange: (Boolean) -> Unit,
    onAiContextIncludeOptionsChange: (Boolean) -> Unit,
    onAiContextIncludeAnswerChange: (Boolean) -> Unit,
    onAiContextIncludeExplanationChange: (Boolean) -> Unit,
    onAiThinkingModeChange: (String) -> Unit,
    onAiReasoningEffortChange: (String) -> Unit,
    onAiSystemPromptChange: (String) -> Unit,
    onNavigateToMarkdownTest: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scrollFraction by remember {
        derivedStateOf {
            (scrollState.value / 120f).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        topBar = {
            MnemoraCollapsibleTopAppBar(title = "Settings", scrollFraction = scrollFraction)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            // ── Appearance ──
            MnemoraSettingsSectionHeader(title = "Appearance")
            val themeOptions = listOf("System" to Icons.Outlined.SettingsBrightness, "Light" to Icons.Outlined.LightMode, "Dark" to Icons.Outlined.DarkMode)
            val themeIndex = uiState.themeMode.coerceIn(0, 2)
            val localeOptions = listOf(
                "" to "System Default",
                "en" to "English",
                "zh-CN" to "简体中文"
            )
            val localeIndex = localeOptions.indexOfFirst { it.first == uiState.locale }.coerceAtLeast(0)
            MnemoraSettingsGroup {
                MnemoraSettingsDropdownRow(
                    headline = "Theme",
                    supporting = themeOptions[themeIndex].first,
                    icon = Icons.Default.Contrast,
                    options = themeOptions.map { it.first },
                    selectedIndex = themeIndex,
                    onSelect = { onThemeModeSelect(it) }
                )
                MnemoraSettingsDivider()
                MnemoraSettingsDropdownRow(
                    headline = "Language",
                    supporting = localeOptions[localeIndex].second,
                    icon = Icons.Default.Language,
                    options = localeOptions.map { it.second },
                    selectedIndex = localeIndex,
                    onSelect = { onLocaleSelect(localeOptions[it].first) }
                )
            }

            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(top = 20.dp))

            // ── Study Experience ──
            MnemoraSettingsSectionHeader(title = "Study Experience")
            MnemoraSettingsGroup {
                MnemoraSettingsSwitchRow(
                    headline = "Auto Advance",
                    supporting = "Automatically go to next question after correct answer",
                    icon = Icons.Default.Speed,
                    checked = uiState.autoAdvance,
                    onCheckedChange = { onAutoAdvanceChange(it) }
                )
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Show Analysis",
                    supporting = "Display explanation after answering",
                    icon = Icons.Default.Visibility,
                    checked = uiState.showAnalysis,
                    onCheckedChange = { onShowAnalysisChange(it) }
                )
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Progress Bar",
                    supporting = "Show question progress while practicing",
                    icon = Icons.Default.LinearScale,
                    checked = uiState.showPracticeProgress,
                    onCheckedChange = { onShowPracticeProgressChange(it) }
                )
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Sound Effects",
                    icon = Icons.Default.MusicNote,
                    checked = uiState.soundEffects,
                    onCheckedChange = { onSoundEffectsChange(it) }
                )
                if (uiState.soundEffects) {
                    MnemoraSettingsDivider()
                    MnemoraSettingsSwitchRow(
                        headline = "Streak Sounds",
                        supporting = "Escalating audio as you answer correctly in a row",
                        icon = Icons.Default.Equalizer,
                        checked = uiState.continuousFeedback,
                        onCheckedChange = { onContinuousFeedbackChange(it) },
                        modifier = Modifier.padding(start = MnemoraSpacing.Large)
                    )
                }
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Haptic Feedback",
                    icon = Icons.Default.Vibration,
                    checked = uiState.hapticFeedback,
                    onCheckedChange = { onHapticFeedbackChange(it) }
                )
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Confetti Effect",
                    icon = Icons.Default.AutoFixHigh,
                    checked = uiState.confettiEffect,
                    onCheckedChange = { onConfettiEffectChange(it) }
                )
            }

            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(top = 20.dp))

            // ── AI Settings ──
            MnemoraSettingsSectionHeader(title = "AI Settings")

            // Providers own their models and connection shape, so configuration is
            // provider-first: pick a provider, then one of its models. There is no
            // grouping "company" layer — each (provider, model) pair keeps an
            // isolated connection profile.
            val aiProviders = AiProviderCatalog.providers
            val activeProvider = AiProviderCatalog.resolve(uiState.aiProvider)
            val providerIndex = aiProviders.indexOfFirst { it.id == activeProvider.id }.coerceAtLeast(0)
            val aiModels = AiProviderCatalog.modelsFor(activeProvider.id)
            val modelIndex = aiModels.indexOfFirst { it.id == uiState.aiModel }.coerceAtLeast(0)
            val currentProtocol = activeProvider.protocol

            MnemoraSettingsGroup {
                // Provider
                MnemoraSettingsDropdownRow(
                    headline = "Provider",
                    supporting = activeProvider.display,
                    icon = Icons.Default.Speed,
                    options = aiProviders.map { it.display },
                    selectedIndex = providerIndex,
                    onSelect = { index ->
                        onAiProviderSelect(aiProviders[index].id)
                    }
                )

                MnemoraSettingsDivider()

                // Model (scoped to the selected provider)
                MnemoraSettingsDropdownRow(
                    headline = "Model",
                    supporting = aiModels.find { it.id == uiState.aiModel }?.display
                        ?: uiState.aiModel,
                    icon = Icons.Default.SmartButton,
                    options = aiModels.map { it.display },
                    selectedIndex = modelIndex,
                    onSelect = { index ->
                        onAiModelChange(aiModels[index].id)
                    }
                )

                // Thinking Mode (only for Anthropic-protocol providers)
                if (currentProtocol == AiProtocol.ANTHROPIC) {
                    MnemoraSettingsDivider()
                    val thinkingOptions = listOf("Disabled" to "disabled", "Adaptive" to "adaptive", "Extended" to "enabled")
                    val thinkingIndex = thinkingOptions.indexOfFirst { it.second == uiState.aiThinkingMode }
                        .coerceAtLeast(0)
                    MnemoraSettingsDropdownRow(
                        headline = "Thinking Mode",
                        supporting = thinkingOptions[thinkingIndex].first,
                        icon = Icons.Default.AutoFixHigh,
                        options = thinkingOptions.map { it.first },
                        selectedIndex = thinkingIndex,
                        onSelect = { index ->
                            onAiThinkingModeChange(thinkingOptions[index].second)
                        }
                    )
                }

                // Reasoning Effort (only for OpenAI-protocol providers)
                if (currentProtocol == AiProtocol.OPENAI) {
                    MnemoraSettingsDivider()
                    val reasoningOptions = listOf("Provider default" to "") + listOf(
                        "None" to "none",
                        "Low" to "low",
                        "Medium" to "medium",
                        "High" to "high",
                        "Extra high" to "xhigh",
                    )
                    val reasoningIndex = reasoningOptions
                        .indexOfFirst { it.second == uiState.aiReasoningEffort }
                        .coerceAtLeast(0)
                    MnemoraSettingsDropdownRow(
                        headline = "Reasoning Effort",
                        supporting = reasoningOptions[reasoningIndex].first,
                        icon = Icons.Default.AutoFixHigh,
                        options = reasoningOptions.map { it.first },
                        selectedIndex = reasoningIndex,
                        onSelect = { index ->
                            onAiReasoningEffortChange(reasoningOptions[index].second)
                        },
                    )
                }

                MnemoraSettingsDivider()

                // API Key
                var apiKeyVisible by remember { mutableStateOf(false) }
                val apiKeyFocus = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()
                OutlinedTextField(
                    value = uiState.aiApiKey,
                    onValueChange = { onAiApiKeyChange(it) },
                    label = { Text("API Key") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small)
                        .bringIntoViewRequester(apiKeyFocus)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                scope.launch { delay(300); apiKeyFocus.bringIntoView() }
                            }
                        },
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = settingsTextFieldColors(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (apiKeyVisible) "Hide API Key" else "Show API Key",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                // Base URL (for providers without a fixed official endpoint)
                if (activeProvider.usesCustomHost) {
                    MnemoraSettingsDivider()

                    if (activeProvider.baseUrlHint.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small)
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                                .padding(MnemoraSpacing.Medium)
                        ) {
                            Text(
                                text = activeProvider.baseUrlHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val baseUrlFocus = remember { BringIntoViewRequester() }
                    OutlinedTextField(
                        value = uiState.aiBaseUrl,
                        onValueChange = { onAiBaseUrlChange(it) },
                        label = { Text("Base URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small)
                            .bringIntoViewRequester(baseUrlFocus)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { delay(300); baseUrlFocus.bringIntoView() }
                                }
                            },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = settingsTextFieldColors()
                    )
                }

                // Project ID / Location (Vertex AI only)
                if (currentProtocol == AiProtocol.VERTEX) {
                    MnemoraSettingsDivider()

                    val projectFocus = remember { BringIntoViewRequester() }
                    OutlinedTextField(
                        value = uiState.aiProjectId,
                        onValueChange = { onAiProjectIdChange(it) },
                        label = { Text("Project ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small)
                            .bringIntoViewRequester(projectFocus)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { delay(300); projectFocus.bringIntoView() }
                                }
                            },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = settingsTextFieldColors()
                    )

                    MnemoraSettingsDivider()

                    val locationFocus = remember { BringIntoViewRequester() }
                    OutlinedTextField(
                        value = uiState.aiLocation,
                        onValueChange = { onAiLocationChange(it) },
                        label = { Text("Location (e.g. us-central1)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Small)
                            .bringIntoViewRequester(locationFocus)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { delay(300); locationFocus.bringIntoView() }
                                }
                            },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = settingsTextFieldColors()
                    )
                }

            }

            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(top = 20.dp))

            // ── AI Chat Context ──
            MnemoraSettingsSectionHeader(title = "AI Chat Context")
            
            MnemoraSettingsGroup {
                MnemoraSettingsSwitchRow(
                    headline = "Include Stem",
                    supporting = "Send the question stem to the AI",
                    checked = uiState.aiContextIncludeStem,
                    onCheckedChange = onAiContextIncludeStemChange
                )
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Include Options",
                    supporting = "Send the answer choices to the AI",
                    checked = uiState.aiContextIncludeOptions,
                    onCheckedChange = onAiContextIncludeOptionsChange
                )
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Include Answer",
                    supporting = "Send the correct answer to the AI",
                    checked = uiState.aiContextIncludeAnswer,
                    onCheckedChange = onAiContextIncludeAnswerChange
                )
                MnemoraSettingsDivider()
                MnemoraSettingsSwitchRow(
                    headline = "Include Explanation",
                    supporting = "Send the existing explanation to the AI",
                    checked = uiState.aiContextIncludeExplanation,
                    onCheckedChange = onAiContextIncludeExplanationChange
                )
                
                MnemoraSettingsDivider()
                
                // System Prompt
                val promptFocus = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MnemoraSpacing.Large, vertical = MnemoraSpacing.Medium)
                ) {
                    Text(
                        text = "System Prompt",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(MnemoraSpacing.Small))
                    OutlinedTextField(
                        value = uiState.aiSystemPrompt,
                        onValueChange = onAiSystemPromptChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(promptFocus)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    scope.launch { delay(300); promptFocus.bringIntoView() }
                                }
                            },
                        minLines = 4,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = MaterialTheme.shapes.medium,
                        colors = settingsTextFieldColors()
                    )
                }
            }

            // ── App Info ──
            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(top = 20.dp))
            MnemoraSettingsSectionHeader(title = "App Info")
            MnemoraSettingsGroup {
                MnemoraSettingsRow(
                    headline = "Version",
                    supporting = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    icon = null,
                    trailing = {}
                )
                MnemoraSettingsDivider()
                MnemoraSettingsRow(
                    headline = "Build",
                    supporting = if (BuildConfig.DEBUG) "debug" else "release",
                    icon = null,
                    trailing = {}
                )
            }

            DebugSettingsSection(onNavigateToMarkdownTest = onNavigateToMarkdownTest)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun settingsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    focusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreviewDefault() {
    MnemoraTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            onThemeModeSelect = {},
            onLocaleSelect = {},
            onAutoAdvanceChange = {},
            onShowAnalysisChange = {},
            onShowPracticeProgressChange = {},
            onSoundEffectsChange = {},
            onHapticFeedbackChange = {},
            onContinuousFeedbackChange = {},
            onConfettiEffectChange = {},
            onAiProviderSelect = {},
            onAiApiKeyChange = {},
            onAiProjectIdChange = {},
            onAiLocationChange = {},
            onAiBaseUrlChange = {},
            onAiModelChange = {},
            onAiContextIncludeStemChange = {},
            onAiContextIncludeOptionsChange = {},
            onAiContextIncludeAnswerChange = {},
            onAiContextIncludeExplanationChange = {},
            onAiThinkingModeChange = {},
            onAiReasoningEffortChange = {},
            onAiSystemPromptChange = {},
            onNavigateToMarkdownTest = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreviewModified() {
    MnemoraTheme {
            SettingsScreenContent(
                uiState = SettingsUiState(
                    themeMode = 2,
                    autoAdvance = false,
                    showAnalysis = false,
                    soundEffects = false,
                    hapticFeedback = true,
                    continuousFeedback = false,
                    confettiEffect = false,
                    aiProvider = "antigravity-sub2api",
                    aiApiKey = "sk-test123",
                    aiModel = "gemini-3.6-flash-tiered",
                    aiBaseUrl = "https://your-relay.com"
                ),
            onThemeModeSelect = {},
            onLocaleSelect = {},
            onAutoAdvanceChange = {},
            onShowAnalysisChange = {},
            onShowPracticeProgressChange = {},
            onSoundEffectsChange = {},
            onHapticFeedbackChange = {},
            onContinuousFeedbackChange = {},
            onConfettiEffectChange = {},
            onAiProviderSelect = {},
            onAiApiKeyChange = {},
            onAiProjectIdChange = {},
            onAiLocationChange = {},
            onAiBaseUrlChange = {},
            onAiModelChange = {},
            onAiContextIncludeStemChange = {},
            onAiContextIncludeOptionsChange = {},
            onAiContextIncludeAnswerChange = {},
            onAiContextIncludeExplanationChange = {},
            onAiThinkingModeChange = {},
            onAiReasoningEffortChange = {},
            onAiSystemPromptChange = {},
            onNavigateToMarkdownTest = {}
        )
    }
}
