package com.hihusky.mnema.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.hihusky.mnema.ui.components.MnemaSettingsDivider
import com.hihusky.mnema.ui.components.MnemaSettingsDropdownRow
import com.hihusky.mnema.ui.components.MnemaSettingsGroup
import com.hihusky.mnema.ui.components.MnemaSettingsSectionHeader
import com.hihusky.mnema.ui.components.MnemaSettingsSwitchRow
import com.hihusky.mnema.ui.components.topappbar.MnemaCollapsibleTopAppBar
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.hihusky.mnema.ui.theme.MnemaSpacing
import com.hihusky.mnema.ui.theme.MnemaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState = uiState,
        onBack = onBack,
        onThemeModeSelect = { viewModel.setThemeMode(it) },
        onAutoAdvanceChange = { viewModel.setAutoAdvance(it) },
        onShowAnalysisChange = { viewModel.setShowAnalysis(it) },
        onSoundEffectsChange = { viewModel.setSoundEffects(it) },
        onHapticFeedbackChange = { viewModel.setHapticFeedback(it) },
        onConfettiEffectChange = { viewModel.setConfettiEffect(it) },
        onShowPracticeProgressChange = { viewModel.setShowPracticeProgress(it) },
        onAiProviderSelect = { viewModel.setAiProvider(it) },
        onAiApiKeyChange = { viewModel.setAiApiKey(it) },
        onAiProjectIdChange = { viewModel.setAiProjectId(it) },
        onAiLocationChange = { viewModel.setAiLocation(it) },
        onAiModelChange = { viewModel.setAiModel(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreenContent(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeSelect: (Int) -> Unit,
    onAutoAdvanceChange: (Boolean) -> Unit,
    onShowAnalysisChange: (Boolean) -> Unit,
    onShowPracticeProgressChange: (Boolean) -> Unit,
    onSoundEffectsChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onConfettiEffectChange: (Boolean) -> Unit,
    onAiProviderSelect: (String) -> Unit,
    onAiApiKeyChange: (String) -> Unit,
    onAiProjectIdChange: (String) -> Unit,
    onAiLocationChange: (String) -> Unit,
    onAiModelChange: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val scrollFraction by remember {
        derivedStateOf {
            (scrollState.value / 120f).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        topBar = {
            MnemaCollapsibleTopAppBar(title = "Settings", scrollFraction = scrollFraction)
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
            MnemaSettingsSectionHeader(title = "Appearance")
            val themeOptions = listOf("System" to Icons.Outlined.SettingsBrightness, "Light" to Icons.Outlined.LightMode, "Dark" to Icons.Outlined.DarkMode)
            val themeIndex = uiState.themeMode.coerceIn(0, 2)
            MnemaSettingsGroup {
                MnemaSettingsDropdownRow(
                    headline = "Theme",
                    supporting = themeOptions[themeIndex].first,
                    icon = Icons.Default.Contrast,
                    options = themeOptions.map { it.first },
                    selectedIndex = themeIndex,
                    onSelect = { onThemeModeSelect(it) }
                )
            }

            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(top = 20.dp))

            // ── Study Experience ──
            MnemaSettingsSectionHeader(title = "Study Experience")
            MnemaSettingsGroup {
                MnemaSettingsSwitchRow(
                    headline = "Auto Advance",
                    supporting = "Automatically go to next question after correct answer",
                    icon = Icons.Default.Speed,
                    checked = uiState.autoAdvance,
                    onCheckedChange = { onAutoAdvanceChange(it) }
                )
                MnemaSettingsDivider()
                MnemaSettingsSwitchRow(
                    headline = "Show Analysis",
                    supporting = "Display explanation after answering",
                    icon = Icons.Default.Visibility,
                    checked = uiState.showAnalysis,
                    onCheckedChange = { onShowAnalysisChange(it) }
                )
                MnemaSettingsDivider()
                MnemaSettingsSwitchRow(
                    headline = "Progress Bar",
                    supporting = "Show question progress while practicing",
                    icon = Icons.Default.LinearScale,
                    checked = uiState.showPracticeProgress,
                    onCheckedChange = { onShowPracticeProgressChange(it) }
                )
                MnemaSettingsDivider()
                MnemaSettingsSwitchRow(
                    headline = "Sound Effects",
                    icon = Icons.Default.MusicNote,
                    checked = uiState.soundEffects,
                    onCheckedChange = { onSoundEffectsChange(it) }
                )
                MnemaSettingsDivider()
                MnemaSettingsSwitchRow(
                    headline = "Haptic Feedback",
                    icon = Icons.Default.Vibration,
                    checked = uiState.hapticFeedback,
                    onCheckedChange = { onHapticFeedbackChange(it) }
                )
                MnemaSettingsDivider()
                MnemaSettingsSwitchRow(
                    headline = "Confetti Effect",
                    icon = Icons.Default.AutoFixHigh,
                    checked = uiState.confettiEffect,
                    onCheckedChange = { onConfettiEffectChange(it) }
                )
            }

            HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(top = 20.dp))

            // ── AI Settings ──
            MnemaSettingsSectionHeader(title = "AI Settings")

            val aiModels = listOf(
                "Gemini 3.1 Pro Preview" to "gemini-3.1-pro-preview",
                "Gemini 3.1 Flash Lite Preview" to "gemini-3.1-flash-lite-preview",
                "Gemini 2.5 Flash" to "gemini-2.5-flash",
                "Kimi K2.6" to "kimi-k2.6"
            )
            val aiProviders = when {
                uiState.aiModel.startsWith("kimi", ignoreCase = true) -> listOf(
                    "Moonshot API" to "kimi"
                )
                else -> listOf(
                    "Google AI Studio" to "gemini",
                    "GCP Vertex AI" to "vertex-ai"
                )
            }
            val currentProviderDisplay = aiProviders.find { it.second == uiState.aiProvider }?.first
                ?: aiProviders.first().first

            MnemaSettingsGroup {
                // Model
                MnemaSettingsDropdownRow(
                    headline = "Model",
                    supporting = aiModels.find { it.second == uiState.aiModel }?.first
                        ?: uiState.aiModel,
                    icon = Icons.Default.SmartButton,
                    options = aiModels.map { it.first },
                    selectedIndex = aiModels.indexOfFirst { it.second == uiState.aiModel }
                        .coerceAtLeast(0),
                    onSelect = { index ->
                        onAiModelChange(aiModels[index].second)
                    }
                )

                MnemaSettingsDivider()

                // Provider
                MnemaSettingsDropdownRow(
                    headline = "Provider",
                    supporting = currentProviderDisplay,
                    icon = Icons.Default.Speed,
                    options = aiProviders.map { it.first },
                    selectedIndex = aiProviders.indexOfFirst { it.second == uiState.aiProvider }
                        .coerceAtLeast(0),
                    onSelect = { index ->
                        onAiProviderSelect(aiProviders[index].second)
                    }
                )

                MnemaSettingsDivider()

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
                        .padding(horizontal = MnemaSpacing.Large, vertical = MnemaSpacing.Small)
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

                if (uiState.aiProvider.lowercase() == "vertex-ai") {
                    MnemaSettingsDivider()

                    // Project ID
                    val projectFocus = remember { BringIntoViewRequester() }
                    OutlinedTextField(
                        value = uiState.aiProjectId,
                        onValueChange = { onAiProjectIdChange(it) },
                        label = { Text("Project ID") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MnemaSpacing.Large, vertical = MnemaSpacing.Small)
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

                    MnemaSettingsDivider()

                    // Location
                    val locationFocus = remember { BringIntoViewRequester() }
                    OutlinedTextField(
                        value = uiState.aiLocation,
                        onValueChange = { onAiLocationChange(it) },
                        label = { Text("Location (e.g. us-central1)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MnemaSpacing.Large, vertical = MnemaSpacing.Small)
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

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
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
    MnemaTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(),
            onBack = {},
            onThemeModeSelect = {},
            onAutoAdvanceChange = {},
            onShowAnalysisChange = {},
            onShowPracticeProgressChange = {},
            onSoundEffectsChange = {},
            onHapticFeedbackChange = {},
            onConfettiEffectChange = {},
            onAiProviderSelect = {},
            onAiApiKeyChange = {},
            onAiProjectIdChange = {},
            onAiLocationChange = {},
            onAiModelChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreviewModified() {
    MnemaTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                themeMode = 2,
                autoAdvance = false,
                showAnalysis = false,
                soundEffects = false,
                hapticFeedback = true,
                confettiEffect = false,
                aiProvider = "kimi",
                aiApiKey = "sk-test123",
                aiModel = "kimi-k2.6"
            ),
            onBack = {},
            onThemeModeSelect = {},
            onAutoAdvanceChange = {},
            onShowAnalysisChange = {},
            onShowPracticeProgressChange = {},
            onSoundEffectsChange = {},
            onHapticFeedbackChange = {},
            onConfettiEffectChange = {},
            onAiProviderSelect = {},
            onAiApiKeyChange = {},
            onAiProjectIdChange = {},
            onAiLocationChange = {},
            onAiModelChange = {}
        )
    }
}
