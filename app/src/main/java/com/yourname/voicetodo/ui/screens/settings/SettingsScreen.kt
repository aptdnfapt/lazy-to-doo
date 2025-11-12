package com.yourname.voicetodo.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.yourname.voicetodo.data.preferences.UserPreferences
import com.yourname.voicetodo.domain.model.LLMProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onNavigateToToolPermissions: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val llmProvider by viewModel.llmProvider.collectAsState()
    val llmBaseUrl by viewModel.llmBaseUrl.collectAsState()
    val llmApiKey by viewModel.llmApiKey.collectAsState()
    val llmModelName by viewModel.llmModelName.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val voiceInputEnabled by viewModel.voiceInputEnabled.collectAsState()
    val voiceEndpoint by viewModel.voiceEndpoint.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val autoExecute by viewModel.autoExecute.collectAsState()

    var showLlmApiKeyDialog by remember { mutableStateOf(false) }
    var tempLlmApiKey by remember { mutableStateOf("") }
    var showLlmApiKey by remember { mutableStateOf(false) }
    var showGeminiApiKey by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    // Local state for text fields to prevent cursor jumping
    var llmBaseUrlText by remember { mutableStateOf("") }
    var llmModelNameText by remember { mutableStateOf("") }

    LaunchedEffect(llmApiKey) {
        tempLlmApiKey = llmApiKey
    }

    LaunchedEffect(llmBaseUrl) {
        llmBaseUrlText = llmBaseUrl
    }

    LaunchedEffect(llmModelName) {
        llmModelNameText = llmModelName
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // LLM Provider Settings
            SettingsSection(title = "LLM Provider Settings") {
                // Provider Selection
                ExposedDropdownMenuBox(
                    expanded = providerExpanded,
                    onExpandedChange = { providerExpanded = !providerExpanded }
                ) {
                    OutlinedTextField(
                        value = when (llmProvider) {
                            LLMProvider.OPENAI -> "OpenAI"
                            LLMProvider.ANTHROPIC -> "Anthropic"
                            LLMProvider.GEMINI -> "Google Gemini"
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("LLM Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    DropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false }
                    ) {
                        listOf(
                            LLMProvider.OPENAI to "OpenAI",
                            LLMProvider.ANTHROPIC to "Anthropic", 
                            LLMProvider.GEMINI to "Google Gemini"
                        ).forEach { (provider, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateLlmProvider(provider)
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Provider Description
                Text(
                    text = when (llmProvider) {
                        LLMProvider.OPENAI -> "OpenAI's GPT models for advanced AI capabilities."
                        LLMProvider.ANTHROPIC -> "Anthropic's Claude models for safe and helpful AI."
                        LLMProvider.GEMINI -> "Google's Gemini models for multimodal AI."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Base URL (not for Gemini)
                if (llmProvider != LLMProvider.GEMINI) {
                    Column {
                        OutlinedTextField(
                            value = llmBaseUrlText,
                            onValueChange = { llmBaseUrlText = it },
                            label = { Text("Base URL") },
                            placeholder = {
                                Text(
                                    when (llmProvider) {
                                        LLMProvider.OPENAI -> "https://api.openai.com/v1"
                                        LLMProvider.ANTHROPIC -> "https://api.anthropic.com"
                                        else -> ""
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.updateLlmBaseUrl(llmBaseUrlText.trim())
                            },
                            enabled = llmBaseUrlText.trim() != llmBaseUrl,
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save URL")
                        }
                    }
                }

                Column {
                    OutlinedTextField(
                        value = if (llmApiKey.isNotBlank()) "••••••••••••••••" else "",
                        onValueChange = { },
                        label = {
                            Text(
                                when (llmProvider) {
                                    LLMProvider.OPENAI -> "OpenAI API Key"
                                    LLMProvider.ANTHROPIC -> "Anthropic API Key"
                                    LLMProvider.GEMINI -> "Gemini API Key"
                                }
                            )
                        },
                        placeholder = { Text("No API key set") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = VisualTransformation.None,
                        trailingIcon = {
                            Row {
                                OutlinedButton(
                                    onClick = { showLlmApiKey = !showLlmApiKey },
                                    modifier = Modifier.size(width = 60.dp, height = 32.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(if (showLlmApiKey) "Hide" else "Show", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                OutlinedButton(
                                    onClick = { showLlmApiKeyDialog = true },
                                    modifier = Modifier.size(width = 50.dp, height = 32.dp),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("Set", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        readOnly = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Column {
                    OutlinedTextField(
                        value = llmModelNameText,
                        onValueChange = { llmModelNameText = it },
                        label = { Text("Model Name") },
                        placeholder = {
                            Text(
                                when (llmProvider) {
                                    LLMProvider.OPENAI -> "gpt-4"
                                    LLMProvider.ANTHROPIC -> "claude-3-sonnet-20240229"
                                    LLMProvider.GEMINI -> "gemini-1.5-flash"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = llmModelNameText.contains(" "),
                        shape = RoundedCornerShape(8.dp)
                    )
                    if (llmModelNameText.contains(" ")) {
                        Text(
                            text = "Model names should not contain spaces",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.updateLlmModelName(llmModelNameText.trim())
                        },
                        enabled = llmModelNameText.trim() != llmModelName && !llmModelNameText.contains(" "),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Model")
                    }
                }
            }

            // Voice-to-Text Settings
            SettingsSection(title = "Voice-to-Text Settings") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = voiceEndpoint,
                        onValueChange = viewModel::updateVoiceEndpoint,
                        label = { Text("Voice Endpoint") },
                        placeholder = { Text("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    Text(
                        text = "Configure the voice transcription endpoint. Use Google's Gemini endpoint or any compatible service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = if (geminiApiKey.isNotBlank()) "••••••••••••••••" else "",
                        onValueChange = viewModel::updateGeminiApiKey,
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("No API key set") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = VisualTransformation.None,
                        trailingIcon = {
                            OutlinedButton(
                                onClick = { showGeminiApiKey = !showGeminiApiKey },
                                modifier = Modifier.size(width = 60.dp, height = 32.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(if (showGeminiApiKey) "Hide" else "Show", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        readOnly = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Voice Input",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Allow voice commands and dictation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = voiceInputEnabled,
                            onCheckedChange = viewModel::updateVoiceInputEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            // General Settings
            SettingsSection(title = "General Settings") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Theme selection
                    ExposedDropdownMenuBox(
                        expanded = themeExpanded,
                        onExpandedChange = { themeExpanded = !themeExpanded }
                    ) {
                        OutlinedTextField(
                            value = when (themeMode) {
                                UserPreferences.ThemeMode.LIGHT -> "Light"
                                UserPreferences.ThemeMode.DARK -> "Dark"
                                UserPreferences.ThemeMode.SYSTEM -> "System"
                            },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("App Theme") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false }
                        ) {
                            listOf(
                                UserPreferences.ThemeMode.SYSTEM to "System",
                                UserPreferences.ThemeMode.LIGHT to "Light",
                                UserPreferences.ThemeMode.DARK to "Dark"
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.updateThemeMode(mode)
                                        themeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // TTS enabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Text-to-Speech",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Read responses aloud",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = ttsEnabled,
                            onCheckedChange = viewModel::updateTtsEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    // Auto-execute tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-execute Tools",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Automatically execute actions without confirmation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoExecute,
                            onCheckedChange = viewModel::updateAutoExecute,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    // Tool Permissions
                    OutlinedButton(
                        onClick = onNavigateToToolPermissions,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tool Permissions",
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Navigate to tool permissions",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // API Key Dialog
    if (showLlmApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showLlmApiKeyDialog = false },
            title = { 
                Text(
                    text = "Set ${when (llmProvider) {
                        LLMProvider.OPENAI -> "OpenAI"
                        LLMProvider.ANTHROPIC -> "Anthropic"
                        LLMProvider.GEMINI -> "Gemini"
                    }} API Key",
                    fontWeight = FontWeight.Medium
                ) 
            },
            text = {
                Column {
                    Text(
                        text = "Enter your API key for ${when (llmProvider) {
                            LLMProvider.OPENAI -> "OpenAI"
                            LLMProvider.ANTHROPIC -> "Anthropic"
                            LLMProvider.GEMINI -> "Google Gemini"
                        }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = tempLlmApiKey,
                        onValueChange = { tempLlmApiKey = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showLlmApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            OutlinedButton(
                                onClick = { showLlmApiKey = !showLlmApiKey },
                                modifier = Modifier.size(width = 60.dp, height = 32.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(if (showLlmApiKey) "Hide" else "Show", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.updateLlmApiKey(tempLlmApiKey)
                        showLlmApiKeyDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLlmApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}