package com.yourname.voicetodo.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.yourname.voicetodo.ui.navigation.NavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val llmBaseUrl by viewModel.llmBaseUrl.collectAsState()
    val llmApiKey by viewModel.llmApiKey.collectAsState()
    val llmModelName by viewModel.llmModelName.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val voiceInputEnabled by viewModel.voiceInputEnabled.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val ttsEnabled by viewModel.ttsEnabled.collectAsState()
    val autoExecute by viewModel.autoExecute.collectAsState()

    var showLlmApiKeyDialog by remember { mutableStateOf(false) }
    var tempLlmApiKey by remember { mutableStateOf("") }
    var showLlmApiKey by remember { mutableStateOf(false) }
    var showGeminiApiKey by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(16.dp)
        ) {
            // LLM Provider Settings
            SettingsSection(title = "LLM Provider Settings") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = llmBaseUrlText,
                        onValueChange = { llmBaseUrlText = it },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateLlmBaseUrl(llmBaseUrlText.trim())
                        },
                        enabled = llmBaseUrlText.trim() != llmBaseUrl
                    ) {
                        Text("Save")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = llmApiKey,
                    onValueChange = { },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your API key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showLlmApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            Button(onClick = { showLlmApiKey = !showLlmApiKey }) {
                                Text(if (showLlmApiKey) "Hide" else "Show")
                            }
                            Button(onClick = { showLlmApiKeyDialog = true }) {
                                Text("Set")
                            }
                        }
                    },
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = llmModelNameText,
                            onValueChange = { llmModelNameText = it },
                            label = { Text("Model Name") },
                            placeholder = { Text("gpt-4") },
                            modifier = Modifier.weight(1f),
                            isError = llmModelNameText.contains(" ")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateLlmModelName(llmModelNameText.trim())
                            },
                            enabled = llmModelNameText.trim() != llmModelName && !llmModelNameText.contains(" ")
                        ) {
                            Text("Save")
                        }
                    }
                    if (llmModelNameText.contains(" ")) {
                        Text(
                            text = "Model names should not contain spaces",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice-to-Text Settings
            SettingsSection(title = "Voice-to-Text Settings") {
                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = viewModel::updateGeminiApiKey,
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("Enter your Gemini API key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showGeminiApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                  Button(onClick = { showGeminiApiKey = !showGeminiApiKey }) {
                        Text(if (showGeminiApiKey) "Hide" else "Show")
                    }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Voice Input")
                    Switch(
                        checked = voiceInputEnabled,
                        onCheckedChange = viewModel::updateVoiceInputEnabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // General Settings
            SettingsSection(title = "General Settings") {
                // Theme selection
                ExposedDropdownMenuBox(
                    expanded = themeExpanded,
                    onExpandedChange = { themeExpanded = !themeExpanded }
                ) {
                    OutlinedTextField(
                        value = theme.replaceFirstChar { it.uppercase() },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("App Theme") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = themeExpanded,
                        onDismissRequest = { themeExpanded = false }
                    ) {
                        listOf("system", "light", "dark").forEach { themeOption ->
                            DropdownMenuItem(
                                text = { Text(themeOption.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    viewModel.updateTheme(themeOption)
                                    themeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TTS enabled
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Text-to-Speech")
                    Switch(
                        checked = ttsEnabled,
                        onCheckedChange = viewModel::updateTtsEnabled
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Auto-execute tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Auto-execute Tools")
                        Text(
                            text = "Automatically execute actions without confirmation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoExecute,
                        onCheckedChange = viewModel::updateAutoExecute
                    )
                }
            }
        }
    }

    // API Key Dialog
    if (showLlmApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showLlmApiKeyDialog = false },
            title = { Text("Set LLM API Key") },
            text = {
                OutlinedTextField(
                    value = tempLlmApiKey,
                    onValueChange = { tempLlmApiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showLlmApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                  Button(onClick = { showLlmApiKey = !showLlmApiKey }) {
                        Text(if (showLlmApiKey) "Hide" else "Show")
                    }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateLlmApiKey(tempLlmApiKey)
                        showLlmApiKeyDialog = false
                    }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}