# UI Overhaul & Tool Permission Improvements Plan

**Goal:** Modernize UI with dark/light themes, move tool permissions to inline chat bubbles, add edit title tool, and fix settings UX.

**Target Files:** Android app (`app/src/main/`)

---

## ✅ What This Plan Covers

1. **New Tool**: Edit todo title (10th tool)
2. **Tool Call UI**: Move from modal dialog to inline chat bubbles with dropdown
3. **Chat History**: Show tool calls in scrollable chat history
4. **Theme System**: Black (dark) and white (light) theme with flat design
5. **Settings UI**: Fix text input bugs, improve layout
6. **Per-Tool Permissions**: Dedicated settings screen for individual tool permissions

---

## 📋 Implementation Checklist

### Part 1: Add Edit Title Tool
- [ ] Add `editTitle` function to TodoTools.kt
- [ ] Wrap with permission + retry logic
- [ ] Test: "Change the title of todo #1 to 'New Title'"

### Part 2: Tool Calls as Chat Bubbles
- [ ] Create ToolCallMessage model (separate from regular Message)
- [ ] Add ToolCallBubble composable with expandable dropdown
- [ ] Move permission buttons into bubble (Always Allow, Allow Once, Deny)
- [ ] Store tool calls in chat history
- [ ] Remove floating ToolPermissionDialog

### Part 3: Theme System
- [ ] Create Theme.kt with dark and light themes
- [ ] Add theme toggle to settings
- [ ] Use Material 3 color schemes (black background, flat design)
- [ ] Apply theme throughout app

### Part 4: Settings UI Improvements
- [ ] Fix text field scrolling issues
- [ ] Better input components (OutlinedTextField with proper constraints)
- [ ] Clean layout with proper spacing

### Part 5: Per-Tool Permission Settings
- [ ] Create ToolPermissionsScreen.kt
- [ ] List all 9 (now 10) tools with individual toggles
- [ ] Save per-tool preferences to DataStore
- [ ] Add navigation to this screen from settings

### Part 6: Deny Button Behavior
- [ ] When "Deny" clicked: stop agent execution completely
- [ ] Don't let agent continue or ask follow-up questions
- [ ] User can then send new message to clarify

---

## 🔧 Detailed Implementation Steps

---

### STEP 1: Add Edit Title Tool

**File:** `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`

**Add new tool:**

```kotlin
@Tool
@LLMDescription("Edit the title of an existing todo")
suspend fun editTitle(
    @LLMDescription("Todo ID") todoId: String,
    @LLMDescription("New title") newTitle: String
): String = executeWithPermission(
    toolName = "editTitle",
    arguments = mapOf("todoId" to todoId, "newTitle" to newTitle)
) {
    retryableToolExecutor.executeWithRetry(
        request = RetryableToolExecutor.ToolExecutionRequest(
            toolName = "editTitle",
            arguments = mapOf("todoId" to todoId, "newTitle" to newTitle),
            executeFunction = {
                try {
                    val todo = repository.getTodoById(todoId)
                    if (todo != null) {
                        val updatedTodo = todo.copy(title = newTitle)
                        repository.updateTodo(updatedTodo)
                        "🔧 Tool Call: editTitle\n✅ Updated todo title to: $newTitle"
                    } else {
                        "🔧 Tool Call: editTitle\n❌ Todo with ID $todoId not found"
                    }
                } catch (e: Exception) {
                    "🔧 Tool Call: editTitle\n❌ Failed: ${e.message}"
                }
            }
        ),
        checkPermission = { true }
    ).result ?: throw Exception("Tool execution failed")
}
```

**Update system prompt in TodoAgent.kt:**

Add to capabilities:
```kotlin
private val systemPrompt = """
    // ... existing ...
    
    Your capabilities include:
    - Adding new todos with titles and descriptions
    - Editing todo titles                              // <- NEW
    - Editing existing todo descriptions
    - Marking todos as complete, in progress, or do later
    // ... rest ...
"""
```

**Testing:**
```
User: "Change the title of todo #1 to 'Buy groceries'"
Expected: Todo title updated successfully
```

---

### STEP 2: Tool Calls as Inline Chat Bubbles

#### 2.1: Create ToolCallMessage Model

**File:** `app/src/main/java/com/yourname/voicetodo/domain/model/ToolCallMessage.kt` (NEW)

```kotlin
package com.yourname.voicetodo.domain.model

data class ToolCallMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: Map<String, Any?>,
    val status: ToolCallStatus,
    val result: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val approved: Boolean = false,
    val denied: Boolean = false
)

enum class ToolCallStatus {
    PENDING_APPROVAL,   // Waiting for user to approve/deny
    EXECUTING,          // User approved, executing now
    RETRYING,           // Failed, retrying
    SUCCESS,            // Completed successfully
    FAILED,             // Failed after retries
    DENIED              // User denied
}
```

#### 2.2: Update Message Model to Support Tool Calls

**File:** `app/src/main/java/com/yourname/voicetodo/domain/model/Message.kt`

**Option A: Add type field:**
```kotlin
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "session_id") val sessionId: String,
    val content: String,
    @ColumnInfo(name = "is_from_user") val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    
    // NEW: For tool call messages
    val messageType: MessageType = MessageType.TEXT,  // TEXT or TOOL_CALL
    val toolName: String? = null,
    val toolArguments: String? = null,  // JSON string
    val toolStatus: String? = null,      // ToolCallStatus as string
    val toolResult: String? = null
)

enum class MessageType {
    TEXT,
    TOOL_CALL
}
```

**Option B: Create separate table (CLEANER):**

Create `ToolCallMessageEntity.kt`:
```kotlin
@Entity(tableName = "tool_call_messages")
data class ToolCallMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "tool_name") val toolName: String,
    val arguments: String,  // JSON
    val status: String,     // ToolCallStatus.name
    val result: String?,
    val timestamp: Long = System.currentTimeMillis()
)
```

**Recommendation:** Use Option B (separate table) for cleaner separation.

#### 2.3: Create ToolCallBubble Composable

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/components/ToolCallBubble.kt` (NEW)

```kotlin
package com.yourname.voicetodo.ui.screens.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourname.voicetodo.domain.model.ToolCallMessage
import com.yourname.voicetodo.domain.model.ToolCallStatus

@Composable
fun ToolCallBubble(
    toolCall: ToolCallMessage,
    onApproveAlways: () -> Unit,
    onApproveOnce: () -> Unit,
    onDeny: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth(0.85f),
        colors = CardDefaults.cardColors(
            containerColor = when (toolCall.status) {
                ToolCallStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                ToolCallStatus.FAILED, ToolCallStatus.DENIED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Tool name + expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status icon
                    Icon(
                        imageVector = when (toolCall.status) {
                            ToolCallStatus.PENDING_APPROVAL -> Icons.Default.Lock
                            ToolCallStatus.EXECUTING -> Icons.Default.Refresh
                            ToolCallStatus.RETRYING -> Icons.Default.Refresh
                            ToolCallStatus.SUCCESS -> Icons.Default.Check
                            ToolCallStatus.FAILED -> Icons.Default.Close
                            ToolCallStatus.DENIED -> Icons.Default.Block
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = toolCall.toolName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getStatusText(toolCall.status),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Expand/collapse button
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp 
                                     else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            // Expandable details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    // Arguments
                    if (toolCall.arguments.isNotEmpty()) {
                        Text(
                            text = "Arguments:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        toolCall.arguments.forEach { (key, value) ->
                            Text(
                                text = "  $key: ${value?.toString() ?: "null"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Result (if completed)
                    if (toolCall.result != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Result:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = toolCall.result,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons (only show if pending approval)
            if (toolCall.status == ToolCallStatus.PENDING_APPROVAL) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDeny,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Deny")
                    }
                    TextButton(onClick = onApproveOnce) {
                        Text("Allow Once")
                    }
                    Button(onClick = onApproveAlways) {
                        Text("Always Allow")
                    }
                }
            }

            // Loading indicator
            if (toolCall.status == ToolCallStatus.EXECUTING || 
                toolCall.status == ToolCallStatus.RETRYING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun getStatusText(status: ToolCallStatus): String {
    return when (status) {
        ToolCallStatus.PENDING_APPROVAL -> "Waiting for approval..."
        ToolCallStatus.EXECUTING -> "Executing..."
        ToolCallStatus.RETRYING -> "Retrying..."
        ToolCallStatus.SUCCESS -> "Completed successfully"
        ToolCallStatus.FAILED -> "Failed"
        ToolCallStatus.DENIED -> "Denied by user"
    }
}
```

#### 2.4: Update ChatScreen to Show Tool Call Bubbles

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatScreen.kt`

**Changes:**

1. **Remove floating dialog:**
```kotlin
// REMOVE THIS:
showPermissionDialog?.let { activity ->
    ToolPermissionDialog(...)
}
```

2. **Add tool calls to message list:**
```kotlin
LazyColumn(
    modifier = Modifier.weight(1f),
    state = listState
) {
    items(messages) { message ->
        if (message.messageType == MessageType.TEXT) {
            // Regular message bubble
            MessageBubble(message = message)
        } else {
            // Tool call bubble
            val toolCall = message.toToolCallMessage()
            ToolCallBubble(
                toolCall = toolCall,
                onApproveAlways = {
                    viewModel.onToolCallApproveAlways(toolCall.id, toolCall.toolName)
                },
                onApproveOnce = {
                    viewModel.onToolCallApproveOnce(toolCall.id)
                },
                onDeny = {
                    viewModel.onToolCallDeny(toolCall.id)
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
```

#### 2.5: Update ChatViewModel to Handle Inline Tool Calls

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatViewModel.kt`

**Changes:**

1. **Remove old dialog state:**
```kotlin
// REMOVE:
private val _showPermissionDialog = MutableStateFlow<ToolActivity?>(null)
val showPermissionDialog: StateFlow<ToolActivity?> = _showPermissionDialog.asStateFlow()
```

2. **Add tool call to messages instead:**
```kotlin
init {
    viewModelScope.launch {
        ToolExecutionEvents.pendingRequests.collect { request ->
            // Add tool call message to chat
            addToolCallMessage(
                toolName = request.toolName,
                arguments = request.arguments,
                status = ToolCallStatus.PENDING_APPROVAL
            )
            pendingPermissionRequest = request
        }
    }
}

private suspend fun addToolCallMessage(
    toolName: String,
    arguments: Map<String, Any?>,
    status: ToolCallStatus
) {
    val toolCallMessage = Message(
        sessionId = currentSessionId,
        content = "", // Not used for tool calls
        isFromUser = false,
        messageType = MessageType.TOOL_CALL,
        toolName = toolName,
        toolArguments = Json.encodeToString(arguments),
        toolStatus = status.name
    )
    
    chatRepository.addMessage(toolCallMessage)
}
```

3. **Update permission handlers:**
```kotlin
fun onToolCallApproveOnce(messageId: String) {
    viewModelScope.launch {
        // Update message status
        updateToolCallMessageStatus(messageId, ToolCallStatus.EXECUTING)
        // Resume execution
        pendingPermissionRequest?.onResponse?.invoke(true)
        pendingPermissionRequest = null
    }
}

fun onToolCallApproveAlways(messageId: String, toolName: String) {
    viewModelScope.launch {
        permissionManager.setToolAlwaysAllowed(toolName, true)
        updateToolCallMessageStatus(messageId, ToolCallStatus.EXECUTING)
        pendingPermissionRequest?.onResponse?.invoke(true)
        pendingPermissionRequest = null
    }
}

fun onToolCallDeny(messageId: String) {
    viewModelScope.launch {
        updateToolCallMessageStatus(messageId, ToolCallStatus.DENIED)
        // IMPORTANT: Stop agent execution completely
        pendingPermissionRequest?.onResponse?.invoke(false)
        pendingPermissionRequest = null
        
        // Add system message explaining denial
        addMessage(
            content = "Tool execution denied. Please provide more details or rephrase your request.",
            isFromUser = false
        )
    }
}

private suspend fun updateToolCallMessageStatus(messageId: String, status: ToolCallStatus) {
    // Update in database
    chatRepository.updateToolCallMessageStatus(messageId, status.name)
}
```

---

### STEP 3: Theme System (Dark/Light with Flat Design)

#### 3.1: Create Theme Configuration

**File:** `app/src/main/java/com/yourname/voicetodo/ui/theme/Theme.kt`

**Update with proper dark/light themes:**

```kotlin
package com.yourname.voicetodo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark theme colors (BLACK background, flat design)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),           // Light blue
    onPrimary = Color(0xFF000000),         // Black text on primary
    primaryContainer = Color(0xFF1E1E1E),  // Dark gray container
    onPrimaryContainer = Color(0xFFE0E0E0),
    
    secondary = Color(0xFFB0BEC5),         // Gray blue
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF263238),
    onSecondaryContainer = Color(0xFFE0E0E0),
    
    background = Color(0xFF000000),        // Pure black
    onBackground = Color(0xFFFFFFFF),      // White text
    
    surface = Color(0xFF121212),           // Very dark gray
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1E1E1E),    // Slightly lighter
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    outline = Color(0xFF3A3A3A),           // Subtle borders
    outlineVariant = Color(0xFF2A2A2A)
)

// Light theme colors (WHITE background, flat design)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),           // Blue
    onPrimary = Color(0xFFFFFFFF),         // White text on primary
    primaryContainer = Color(0xFFE3F2FD),  // Light blue container
    onPrimaryContainer = Color(0xFF0D47A1),
    
    secondary = Color(0xFF546E7A),         // Blue gray
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECEFF1),
    onSecondaryContainer = Color(0xFF263238),
    
    background = Color(0xFFFFFFFF),        // Pure white
    onBackground = Color(0xFF000000),      // Black text
    
    surface = Color(0xFFFAFAFA),           // Very light gray
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF5F5F5),    // Slightly darker
    onSurfaceVariant = Color(0xFF5F5F5F),
    
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    
    outline = Color(0xFFDDDDDD),           // Subtle borders
    outlineVariant = Color(0xFFEEEEEE)
)

@Composable
fun VoiceTodoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColors: Boolean = false,  // Set to false for custom themes
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,  // Flat design shapes
        content = content
    )
}
```

**File:** `app/src/main/java/com/yourname/voicetodo/ui/theme/Shape.kt`

**Update for flat design:**

```kotlin
package com.yourname.voicetodo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Flat design with minimal rounding
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
```

#### 3.2: Add Theme Toggle to Settings

**File:** `app/src/main/java/com/yourname/voicetodo/data/preferences/UserPreferences.kt`

**Add theme preference:**

```kotlin
// Theme preference
fun getThemeMode(): Flow<ThemeMode> = dataStore.data.map { preferences ->
    val mode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    ThemeMode.valueOf(mode)
}

suspend fun setThemeMode(mode: ThemeMode) {
    dataStore.edit { preferences ->
        preferences[PreferencesKeys.THEME_MODE] = mode.name
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM  // Follow system theme
}
```

**File:** `app/src/main/java/com/yourname/voicetodo/data/preferences/PreferencesKeys.kt`

**Add key:**

```kotlin
val THEME_MODE = stringPreferencesKey("theme_mode")
```

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/settings/SettingsScreen.kt`

**Add theme selector:**

```kotlin
// Theme selection
Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeModeButton(
                label = "Light",
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeButton(
                label = "Dark",
                selected = themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeButton(
                label = "System",
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(label)
    }
}
```

**File:** `app/src/main/java/com/yourname/voicetodo/ui/MainActivity.kt`

**Apply theme:**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val userPreferences = remember { UserPreferences(dataStore) }
            val themeMode by userPreferences.getThemeMode().collectAsState(initial = ThemeMode.SYSTEM)
            
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            VoiceTodoTheme(darkTheme = darkTheme) {
                // App content
            }
        }
    }
}
```

---

### STEP 4: Fix Settings UI Issues

#### 4.1: Fix Text Field Scrolling

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/settings/SettingsScreen.kt`

**Replace all TextField with OutlinedTextField with proper modifiers:**

```kotlin
// OLD (buggy):
TextField(
    value = llmBaseUrl,
    onValueChange = { viewModel.setLlmBaseUrl(it) },
    label = { Text("Base URL") }
)

// NEW (fixed):
OutlinedTextField(
    value = llmBaseUrl,
    onValueChange = { viewModel.setLlmBaseUrl(it) },
    label = { Text("Base URL") },
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp),  // Proper minimum height
    singleLine = false,  // Allow wrapping for long URLs
    maxLines = 3,        // Max 3 lines before scrolling
    textStyle = MaterialTheme.typography.bodyMedium,
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )
)
```

**Apply to all text fields:**
- API Key field
- Base URL field
- Model name field
- Gemini API key field

#### 4.2: Better Layout with Proper Spacing

```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)  // Consistent spacing
    ) {
        // Theme section
        item {
            SettingsSection(title = "Appearance") {
                // Theme selector
            }
        }
        
        // LLM configuration section
        item {
            SettingsSection(title = "LLM Configuration") {
                // API key, base URL, model name fields
            }
        }
        
        // Voice configuration
        item {
            SettingsSection(title = "Voice Transcription") {
                // Gemini API key
            }
        }
        
        // Tool permissions
        item {
            SettingsSection(title = "Tool Permissions") {
                // Navigation to tool permissions screen
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
```

---

### STEP 5: Per-Tool Permission Settings Screen

#### 5.1: Create Tool Permissions Screen

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/settings/ToolPermissionsScreen.kt` (NEW)

```kotlin
package com.yourname.voicetodo.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

data class ToolPermissionItem(
    val toolName: String,
    val displayName: String,
    val description: String,
    val isAllowed: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolPermissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ToolPermissionsViewModel = hiltViewModel()
) {
    val toolPermissions by viewModel.toolPermissions.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tool Permissions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manage Tool Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enable 'Always Allow' for tools you trust. Disabled tools will require approval each time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Tool list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(toolPermissions) { tool ->
                    ToolPermissionCard(
                        tool = tool,
                        onToggle = { viewModel.toggleToolPermission(tool.toolName) }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolPermissionCard(
    tool: ToolPermissionItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = tool.isAllowed,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
```

#### 5.2: Create ViewModel for Tool Permissions

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/settings/ToolPermissionsViewModel.kt` (NEW)

```kotlin
package com.yourname.voicetodo.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.voicetodo.ai.permission.ToolPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToolPermissionsViewModel @Inject constructor(
    private val permissionManager: ToolPermissionManager
) : ViewModel() {
    
    private val _toolPermissions = MutableStateFlow<List<ToolPermissionItem>>(emptyList())
    val toolPermissions: StateFlow<List<ToolPermissionItem>> = _toolPermissions.asStateFlow()
    
    // Define all 10 tools
    private val allTools = listOf(
        ToolPermissionItem(
            toolName = "addTodo",
            displayName = "Add Todo",
            description = "Create new todo items",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "editTitle",
            displayName = "Edit Title",
            description = "Change todo titles",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "editDescription",
            displayName = "Edit Description",
            description = "Update todo descriptions",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "removeTodo",
            displayName = "Remove Todo",
            description = "Delete todo items",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "markComplete",
            displayName = "Mark Complete",
            description = "Mark todos as done",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "markInProgress",
            displayName = "Mark In Progress",
            description = "Mark todos as in progress",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "markDoLater",
            displayName = "Mark Do Later",
            description = "Mark todos to do later",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "setReminder",
            displayName = "Set Reminder",
            description = "Schedule reminders for todos",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "listTodos",
            displayName = "List Todos",
            description = "View all todos",
            isAllowed = false
        ),
        ToolPermissionItem(
            toolName = "readOutLoud",
            displayName = "Read Out Loud",
            description = "Text-to-speech for todos",
            isAllowed = false
        )
    )
    
    init {
        loadPermissions()
    }
    
    private fun loadPermissions() {
        viewModelScope.launch {
            permissionManager.init()
            _toolPermissions.value = allTools.map { tool ->
                tool.copy(isAllowed = permissionManager.isToolAlwaysAllowed(tool.toolName))
            }
        }
    }
    
    fun toggleToolPermission(toolName: String) {
        viewModelScope.launch {
            val currentPermission = permissionManager.isToolAlwaysAllowed(toolName)
            permissionManager.setToolAlwaysAllowed(toolName, !currentPermission)
            loadPermissions()  // Reload to update UI
        }
    }
}
```

#### 5.3: Add Navigation to Tool Permissions Screen

**File:** `app/src/main/java/com/yourname/voicetodo/ui/navigation/Screen.kt`

**Add new screen:**

```kotlin
sealed class Screen(val route: String) {
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    object Settings : Screen("settings")
    object ToolPermissions : Screen("tool_permissions")  // NEW
    object TodoList : Screen("todos")
    // ... existing screens
}
```

**File:** `app/src/main/java/com/yourname/voicetodo/ui/navigation/NavGraph.kt`

**Add route:**

```kotlin
NavHost(navController = navController, startDestination = Screen.Chat.route) {
    // ... existing routes
    
    composable(Screen.ToolPermissions.route) {
        ToolPermissionsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}
```

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/settings/SettingsScreen.kt`

**Add navigation button:**

```kotlin
// In settings screen, add this card:
SettingsSection(title = "Tool Permissions") {
    OutlinedButton(
        onClick = { onNavigateToToolPermissions() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Settings, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Manage Individual Tool Permissions")
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ArrowForward, contentDescription = null)
    }
}
```

---

### STEP 6: Update Deny Button Behavior

**Current behavior:** Agent might continue or ask follow-up questions after denial.

**New behavior:** Complete stop. User must send new message.

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatViewModel.kt`

**Update onToolCallDeny:**

```kotlin
fun onToolCallDeny(messageId: String) {
    viewModelScope.launch {
        updateToolCallMessageStatus(messageId, ToolCallStatus.DENIED)
        
        // IMPORTANT: Stop agent execution immediately
        pendingPermissionRequest?.onResponse?.invoke(false)
        pendingPermissionRequest = null
        
        // Clear any ongoing processing state
        _isProcessing.value = false
        
        // Add system message explaining denial and next steps
        addMessage(
            content = "⛔ Tool execution denied. The agent has stopped processing. Please provide more details or rephrase your request to continue.",
            isFromUser = false
        )
    }
}
```

**In TodoTools.kt, handle denial properly:**

```kotlin
private suspend fun <T> executeWithPermission(
    toolName: String,
    arguments: Map<String, Any?>,
    block: suspend () -> T
): T {
    // Check if always allowed
    if (permissionManager.isToolAlwaysAllowed(toolName)) {
        return block()
    }

    // Request permission
    val granted = ToolExecutionEvents.requestPermission(toolName, arguments)
    if (!granted) {
        // User denied - throw exception to stop agent
        throw SecurityException("Permission denied for tool: $toolName")
    }

    return block()
}
```

**In TodoAgent.kt, handle SecurityException:**

```kotlin
suspend fun runAgent(userMessage: String, chatHistory: List<Message> = emptyList()): String {
    val agent = createAgent()
    
    return try {
        agent.run(conversationContext)
    } catch (e: SecurityException) {
        // Permission denied - return error message and stop
        "⛔ Action cancelled: ${e.message}"
    } catch (e: Exception) {
        "Sorry, I encountered an error: ${e.message}"
    }
}
```

---

## 📊 Database Migration

**File:** `app/src/main/java/com/yourname/voicetodo/data/local/TodoDatabase.kt`

**Update version and add migration:**

```kotlin
@Database(
    entities = [TodoEntity::class, MessageEntity::class, ChatSessionEntity::class, ToolCallMessageEntity::class],
    version = 2,  // Increment version
    exportSchema = true
)
abstract class TodoDatabase : RoomDatabase() {
    // ... existing
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add tool call messages table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS tool_call_messages (
                id TEXT PRIMARY KEY NOT NULL,
                session_id TEXT NOT NULL,
                tool_name TEXT NOT NULL,
                arguments TEXT NOT NULL,
                status TEXT NOT NULL,
                result TEXT,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
            )
        """)
        
        // Add messageType to messages table
        database.execSQL("ALTER TABLE messages ADD COLUMN messageType TEXT DEFAULT 'TEXT'")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolName TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolArguments TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolStatus TEXT")
        database.execSQL("ALTER TABLE messages ADD COLUMN toolResult TEXT")
    }
}
```

---

## 🧪 Testing Plan

### Test 1: Edit Title Tool
```
User: "Change the title of todo #1 to 'Buy groceries'"
Expected: Title updated successfully
```

### Test 2: Tool Call as Chat Bubble
```
User: "Add todo: Buy milk"
Expected:
- Tool call bubble appears in chat (left side)
- Has dropdown to show details
- Shows 3 buttons: Deny, Allow Once, Always Allow
- NOT a floating modal
```

### Test 3: Tool Call History
```
User: "Add 3 todos: A, B, C"
Expected:
- 3 tool call bubbles appear in chat
- User can scroll up to see previous tool calls
- Tool calls remain in chat history after session
```

### Test 4: Deny Stops Execution
```
User: "Delete all my todos"
→ Tap "Deny"
Expected:
- Agent stops completely
- Shows message: "Tool execution denied. Please provide more details..."
- User can send new message
- Agent doesn't ask follow-up questions
```

### Test 5: Dark Theme
```
Settings → Theme → Dark
Expected:
- Pure black background
- White text
- Flat design with minimal shadows
- Good contrast
```

### Test 6: Light Theme
```
Settings → Theme → Light
Expected:
- Pure white background
- Black text
- Clean, minimal design
```

### Test 7: Settings Text Fields
```
Settings → LLM Config → Paste long URL
Expected:
- Can scroll within text field to see all text
- Text wraps properly
- No cut-off text
```

### Test 8: Per-Tool Permissions
```
Settings → Tool Permissions → Toggle "Add Todo" ON
User: "Add todo: Test"
Expected:
- No permission bubble (executes immediately)
- Other tools still require approval
```

---

## 📁 Files to Create/Modify

### New Files (8):
1. `domain/model/ToolCallMessage.kt`
2. `domain/model/ToolCallStatus.kt`
3. `data/local/ToolCallMessageEntity.kt`
4. `data/local/ToolCallMessageDao.kt`
5. `ui/screens/chat/components/ToolCallBubble.kt`
6. `ui/screens/settings/ToolPermissionsScreen.kt`
7. `ui/screens/settings/ToolPermissionsViewModel.kt`
8. `data/local/migrations/Migration_1_2.kt`

### Modified Files (12):
1. `ai/tools/TodoTools.kt` - Add editTitle tool
2. `ai/agent/TodoAgent.kt` - Update system prompt, handle SecurityException
3. `domain/model/Message.kt` - Add messageType and tool fields
4. `ui/theme/Theme.kt` - Dark/light themes
5. `ui/theme/Shape.kt` - Flat design shapes
6. `ui/screens/chat/ChatScreen.kt` - Tool call bubbles, remove modal
7. `ui/screens/chat/ChatViewModel.kt` - Handle inline tool calls
8. `ui/screens/settings/SettingsScreen.kt` - Fix text fields, add theme selector
9. `ui/navigation/Screen.kt` - Add tool permissions route
10. `ui/navigation/NavGraph.kt` - Add route navigation
11. `data/preferences/UserPreferences.kt` - Add theme preference
12. `data/local/TodoDatabase.kt` - Migration for new fields

---

## 🚀 Implementation Order

1. **Phase 1:** Add editTitle tool (30 min)
2. **Phase 2:** Dark/Light theme system (1 hour)
3. **Phase 3:** Fix settings text fields (30 min)
4. **Phase 4:** Create tool permissions screen (1 hour)
5. **Phase 5:** Create ToolCallBubble component (1 hour)
6. **Phase 6:** Migrate tool calls from modal to inline (2 hours)
7. **Phase 7:** Database migration (30 min)
8. **Phase 8:** Update deny behavior (30 min)
9. **Phase 9:** Testing (1 hour)

**Total Estimated Time:** 8-10 hours

---

## ✅ Success Criteria

- ✅ New editTitle tool works
- ✅ Tool calls appear as chat bubbles (not modals)
- ✅ Tool call history visible in scrollable chat
- ✅ Dark theme: pure black, flat design
- ✅ Light theme: pure white, clean design
- ✅ Settings text fields scroll properly
- ✅ Per-tool permission settings screen works
- ✅ Deny button stops agent completely
- ✅ All 10 tools have individual permission toggles
- ✅ UI feels modern, sleek, and functional

---

**End of Plan**

This plan is ready for your fast agent to implement!
