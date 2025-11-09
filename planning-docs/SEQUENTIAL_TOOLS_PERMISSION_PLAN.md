# Implementation Plan: Sequential Tool Calls + Permission System + Retry Logic

**Goal:** Enable sequential tool calling with user permission dialogs and retry mechanism for failed tool calls.

**Target Files:** Android app (`app/src/main/`)

---

## ✅ What We've Proven (from standalone test)

1. **Sequential tool calling WORKS** - Koog agent automatically chains tools
2. **Your 9 TodoTools are compatible** with sequential calling
3. **Need to add:**
   - Permission UI before tool execution
   - Retry mechanism for 500 errors
   - Real-time tool call display

---

## 📋 Implementation Checklist

### Part 1: Sequential Tool Calling (Already Works!)
- [ ] Verify `TodoAgent.kt` has `maxIterations = 15` (or higher)
- [ ] Ensure all 9 tools in `TodoTools.kt` are properly registered
- [ ] Test with prompts like: "Add 3 todos: A, B, C then mark B complete"

### Part 2: Tool Call Permission System
- [ ] Create permission dialog UI
- [ ] Intercept tool calls before execution
- [ ] Show tool name + arguments in dialog
- [ ] Add "Allow Once", "Always Allow", "Deny" buttons
- [ ] Store permission preferences in DataStore
- [ ] Implement permission checking logic

### Part 3: Retry Mechanism
- [ ] Wrap tool execution with retry logic
- [ ] Retry 3 times on 500 errors with exponential backoff
- [ ] Show retry attempts in UI
- [ ] Log failures after max retries

### Part 4: Real-time Tool Call Display
- [ ] Add "Agent Activity" section to chat UI
- [ ] Show tool calls as they happen
- [ ] Display tool arguments and results
- [ ] Add loading states during execution

---

## 🔧 Detailed Implementation Steps

### STEP 1: Update TodoAgent.kt for Sequential Calls

**File:** `app/src/main/java/com/yourname/voicetodo/ai/agent/TodoAgent.kt`

**Changes:**

1. **Ensure maxIterations is set high enough:**

```kotlin
suspend fun createAgent(): AIAgent<String, String> {
    // ... existing code ...
    
    return AIAgent(
        promptExecutor = executor,
        llmModel = model,
        systemPrompt = systemPrompt,
        toolRegistry = ToolRegistry {
            tool(SayToUser)
            tool(AskUser)
            tools(todoTools)
        },
        maxIterations = 20  // <-- INCREASE THIS (was probably lower)
    )
}
```

**Why:** Higher maxIterations allows more sequential tool calls in one conversation turn.

---

### STEP 2: Create Permission Dialog UI

**New File:** `app/src/main/java/com/yourname/voicetodo/ui/components/ToolPermissionDialog.kt`

**Purpose:** Show dialog before tool execution asking for permission.

**Implementation:**

```kotlin
@Composable
fun ToolPermissionDialog(
    toolName: String,
    toolArguments: Map<String, Any?>,
    onDismiss: () -> Unit,
    onAllowOnce: () -> Unit,
    onAlwaysAllow: () -> Unit,
    onDeny: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tool Permission Required")
            }
        },
        text = {
            Column {
                Text(
                    text = "The agent wants to execute:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tool name
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = toolName,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Arguments
                if (toolArguments.isNotEmpty()) {
                    Text(
                        text = "With arguments:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            toolArguments.forEach { (key, value) ->
                                Row {
                                    Text(
                                        text = "$key: ",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = value?.toString() ?: "null",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDeny) {
                    Text("Deny")
                }
                TextButton(onClick = onAllowOnce) {
                    Text("Allow Once")
                }
                Button(onClick = onAlwaysAllow) {
                    Text("Always Allow")
                }
            }
        }
    )
}
```

**UI Preview:**
```
┌─────────────────────────────────────┐
│ ⚠️  Tool Permission Required        │
├─────────────────────────────────────┤
│ The agent wants to execute:         │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │  addTodo                        │ │
│ └─────────────────────────────────┘ │
│                                     │
│ With arguments:                     │
│ ┌─────────────────────────────────┐ │
│ │ title: Buy milk                 │ │
│ │ section: todo                   │ │
│ └─────────────────────────────────┘ │
│                                     │
│  [Deny] [Allow Once] [Always Allow]│
└─────────────────────────────────────┘
```

---

### STEP 3: Create Tool Permission Manager

**New File:** `app/src/main/java/com/yourname/voicetodo/ai/permission/ToolPermissionManager.kt`

**Purpose:** Manage tool permissions (which tools are always allowed, which need approval).

**Implementation:**

```kotlin
@Singleton
class ToolPermissionManager @Inject constructor(
    private val userPreferences: UserPreferences
) {
    
    // Store which tools are always allowed
    private val alwaysAllowedTools = mutableSetOf<String>()
    
    suspend fun init() {
        // Load saved permissions from DataStore
        userPreferences.getAllowedTools().first().let { savedTools ->
            alwaysAllowedTools.addAll(savedTools)
        }
    }
    
    suspend fun isToolAlwaysAllowed(toolName: String): Boolean {
        return alwaysAllowedTools.contains(toolName)
    }
    
    suspend fun setToolAlwaysAllowed(toolName: String, allowed: Boolean) {
        if (allowed) {
            alwaysAllowedTools.add(toolName)
        } else {
            alwaysAllowedTools.remove(toolName)
        }
        // Save to DataStore
        userPreferences.setAllowedTools(alwaysAllowedTools.toList())
    }
    
    suspend fun clearAllPermissions() {
        alwaysAllowedTools.clear()
        userPreferences.setAllowedTools(emptyList())
    }
}
```

---

### STEP 4: Add Permission Preferences to DataStore

**File:** `app/src/main/java/com/yourname/voicetodo/data/preferences/UserPreferences.kt`

**Add these methods:**

```kotlin
@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // ... existing code ...
    
    // NEW: Tool permissions
    suspend fun getAllowedTools(): Flow<List<String>> = dataStore.data.map { preferences ->
        val json = preferences[PreferencesKeys.ALLOWED_TOOLS] ?: "[]"
        Json.decodeFromString<List<String>>(json)
    }
    
    suspend fun setAllowedTools(tools: List<String>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALLOWED_TOOLS] = Json.encodeToString(tools)
        }
    }
}
```

**File:** `app/src/main/java/com/yourname/voicetodo/data/preferences/PreferencesKeys.kt`

**Add key:**

```kotlin
object PreferencesKeys {
    // ... existing keys ...
    val ALLOWED_TOOLS = stringPreferencesKey("allowed_tools")
}
```

---

### STEP 5: Create Tool Execution Wrapper with Retry

**New File:** `app/src/main/java/com/yourname/voicetodo/ai/execution/RetryableToolExecutor.kt`

**Purpose:** Wrap tool execution with retry logic and permission checks.

**Implementation:**

```kotlin
@Singleton
class RetryableToolExecutor @Inject constructor(
    private val permissionManager: ToolPermissionManager
) {
    
    data class ToolExecutionRequest(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val executeFunction: suspend () -> String
    )
    
    data class ToolExecutionResult(
        val success: Boolean,
        val result: String? = null,
        val error: String? = null,
        val retryCount: Int = 0,
        val permissionDenied: Boolean = false
    )
    
    suspend fun executeWithRetry(
        request: ToolExecutionRequest,
        maxRetries: Int = 3,
        checkPermission: suspend (ToolExecutionRequest) -> Boolean
    ): ToolExecutionResult {
        
        // Check permission first
        val hasPermission = checkPermission(request)
        if (!hasPermission) {
            return ToolExecutionResult(
                success = false,
                error = "Permission denied by user",
                permissionDenied = true
            )
        }
        
        // Execute with retry
        var lastError: String? = null
        var retryCount = 0
        
        repeat(maxRetries) { attempt ->
            try {
                val result = request.executeFunction()
                return ToolExecutionResult(
                    success = true,
                    result = result,
                    retryCount = attempt
                )
            } catch (e: Exception) {
                retryCount = attempt + 1
                lastError = e.message ?: "Unknown error"
                
                // Check if it's a retryable error (500, network issues)
                if (isRetryableError(e) && attempt < maxRetries - 1) {
                    // Exponential backoff: 1s, 2s, 4s
                    val delayMs = (1000L * (1 shl attempt))
                    kotlinx.coroutines.delay(delayMs)
                    // Log retry attempt
                    android.util.Log.w(
                        "RetryableToolExecutor",
                        "Retry attempt ${attempt + 1}/$maxRetries for ${request.toolName}: $lastError"
                    )
                } else {
                    // Not retryable or max retries reached
                    break
                }
            }
        }
        
        return ToolExecutionResult(
            success = false,
            error = lastError,
            retryCount = retryCount
        )
    }
    
    private fun isRetryableError(error: Exception): Boolean {
        val message = error.message?.lowercase() ?: ""
        return message.contains("500") ||
               message.contains("502") ||
               message.contains("503") ||
               message.contains("504") ||
               message.contains("timeout") ||
               message.contains("connection")
    }
}
```

---

### STEP 6: Add Tool Activity Display to Chat UI

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatViewModel.kt`

**Add state for tool activities:**

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // ... existing dependencies ...
) : ViewModel() {
    
    // ... existing state ...
    
    // NEW: Tool activity tracking
    data class ToolActivity(
        val id: String = UUID.randomUUID().toString(),
        val toolName: String,
        val arguments: Map<String, Any?>,
        val status: ToolStatus,
        val result: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class ToolStatus {
        PENDING_PERMISSION,
        EXECUTING,
        RETRYING,
        SUCCESS,
        FAILED,
        DENIED
    }
    
    private val _toolActivities = MutableStateFlow<List<ToolActivity>>(emptyList())
    val toolActivities: StateFlow<List<ToolActivity>> = _toolActivities.asStateFlow()
    
    private val _showPermissionDialog = MutableStateFlow<ToolActivity?>(null)
    val showPermissionDialog: StateFlow<ToolActivity?> = _showPermissionDialog.asStateFlow()
    
    // NEW: Add tool activity
    fun addToolActivity(toolName: String, arguments: Map<String, Any?>) {
        val activity = ToolActivity(
            toolName = toolName,
            arguments = arguments,
            status = ToolStatus.PENDING_PERMISSION
        )
        _toolActivities.value = _toolActivities.value + activity
        _showPermissionDialog.value = activity
    }
    
    // NEW: Update tool activity status
    fun updateToolActivity(activityId: String, status: ToolStatus, result: String? = null) {
        _toolActivities.value = _toolActivities.value.map { activity ->
            if (activity.id == activityId) {
                activity.copy(status = status, result = result)
            } else {
                activity
            }
        }
    }
    
    // NEW: Handle permission responses
    fun onPermissionAllowOnce(activityId: String) {
        updateToolActivity(activityId, ToolStatus.EXECUTING)
        _showPermissionDialog.value = null
        // Continue with tool execution
    }
    
    fun onPermissionAlwaysAllow(activityId: String, toolName: String) {
        viewModelScope.launch {
            permissionManager.setToolAlwaysAllowed(toolName, true)
            updateToolActivity(activityId, ToolStatus.EXECUTING)
            _showPermissionDialog.value = null
            // Continue with tool execution
        }
    }
    
    fun onPermissionDeny(activityId: String) {
        updateToolActivity(activityId, ToolStatus.DENIED, result = "Permission denied by user")
        _showPermissionDialog.value = null
    }
}
```

---

### STEP 7: Update ChatScreen to Show Tool Activities

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatScreen.kt`

**Add tool activity display:**

```kotlin
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    // ... existing code ...
    
    val toolActivities by viewModel.toolActivities.collectAsStateWithLifecycle()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Messages list
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            // ... existing message items ...
            
            // NEW: Tool activities section
            item {
                if (toolActivities.isNotEmpty()) {
                    ToolActivitiesSection(
                        activities = toolActivities,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        // ... existing input area ...
    }
    
    // NEW: Permission dialog
    showPermissionDialog?.let { activity ->
        ToolPermissionDialog(
            toolName = activity.toolName,
            toolArguments = activity.arguments,
            onDismiss = { viewModel.onPermissionDeny(activity.id) },
            onAllowOnce = { viewModel.onPermissionAllowOnce(activity.id) },
            onAlwaysAllow = { viewModel.onPermissionAlwaysAllow(activity.id, activity.toolName) },
            onDeny = { viewModel.onPermissionDeny(activity.id) }
        )
    }
}
```

**New Composable for Tool Activities:**

```kotlin
@Composable
fun ToolActivitiesSection(
    activities: List<ChatViewModel.ToolActivity>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Agent Activity",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        activities.forEach { activity ->
            ToolActivityItem(activity = activity)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ToolActivityItem(activity: ChatViewModel.ToolActivity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (activity.status) {
                ChatViewModel.ToolStatus.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                ChatViewModel.ToolStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                ChatViewModel.ToolStatus.DENIED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = when (activity.status) {
                    ChatViewModel.ToolStatus.PENDING_PERMISSION -> Icons.Default.Lock
                    ChatViewModel.ToolStatus.EXECUTING -> Icons.Default.Refresh
                    ChatViewModel.ToolStatus.RETRYING -> Icons.Default.Refresh
                    ChatViewModel.ToolStatus.SUCCESS -> Icons.Default.Check
                    ChatViewModel.ToolStatus.FAILED -> Icons.Default.Close
                    ChatViewModel.ToolStatus.DENIED -> Icons.Default.Block
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.toolName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = when (activity.status) {
                        ChatViewModel.ToolStatus.PENDING_PERMISSION -> "Waiting for permission..."
                        ChatViewModel.ToolStatus.EXECUTING -> "Executing..."
                        ChatViewModel.ToolStatus.RETRYING -> "Retrying..."
                        ChatViewModel.ToolStatus.SUCCESS -> activity.result ?: "Success"
                        ChatViewModel.ToolStatus.FAILED -> activity.result ?: "Failed"
                        ChatViewModel.ToolStatus.DENIED -> "Permission denied"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (activity.status == ChatViewModel.ToolStatus.EXECUTING ||
                activity.status == ChatViewModel.ToolStatus.RETRYING
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
```

---

### STEP 8: Integrate Permission System into TodoAgent

**File:** `app/src/main/java/com/yourname/voicetodo/ai/agent/TodoAgent.kt`

**Modify `runAgent` to intercept tool calls:**

```kotlin
@Singleton
class TodoAgent @Inject constructor(
    private val todoTools: TodoTools,
    private val userPreferences: UserPreferences,
    private val permissionManager: ToolPermissionManager,
    private val retryableToolExecutor: RetryableToolExecutor
) {
    
    // ... existing code ...
    
    suspend fun runAgentWithPermissions(
        userMessage: String,
        chatHistory: List<Message> = emptyList(),
        onToolCallRequested: suspend (String, Map<String, Any?>) -> Boolean
    ): String {
        val agent = createAgent()
        
        // Wrap tool execution with permission checks
        // This requires modifying how tools are called
        // We'll need to use a custom tool wrapper
        
        return try {
            agent.run(userMessage)
        } catch (e: Exception) {
            "Sorry, I encountered an error: ${e.message}"
        }
    }
}
```

**Note:** Koog doesn't expose tool execution hooks directly, so we have two options:

**Option A (Simpler):** Wrap each tool function with permission logic
**Option B (Advanced):** Create a custom PromptExecutor wrapper that intercepts tool calls

For this implementation, use **Option A**:

---

### STEP 9: Wrap TodoTools with Permission Checks

**File:** `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`

**Modify to add permission helper:**

```kotlin
@Singleton
class TodoTools @Inject constructor(
    private val repository: TodoRepository,
    // NEW: Add these
    private val permissionManager: ToolPermissionManager,
    private val retryableToolExecutor: RetryableToolExecutor,
    private val onToolCallRequested: suspend (String, Map<String, Any?>) -> Boolean
) : ToolSet {
    
    // Helper function for permission-aware execution
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
        val granted = onToolCallRequested(toolName, arguments)
        if (!granted) {
            throw SecurityException("Permission denied for tool: $toolName")
        }
        
        return block()
    }
    
    @Tool
    @LLMDescription("Add a new todo item")
    suspend fun addTodo(
        @LLMDescription("Title of the todo") title: String,
        @LLMDescription("Optional description") description: String? = null,
        @LLMDescription("Section") section: String = "todo"
    ): String = executeWithPermission(
        toolName = "addTodo",
        arguments = mapOf("title" to title, "description" to description, "section" to section)
    ) {
        // Original implementation with retry
        retryableToolExecutor.executeWithRetry(
            request = RetryableToolExecutor.ToolExecutionRequest(
                toolName = "addTodo",
                arguments = mapOf("title" to title, "description" to description, "section" to section),
                executeFunction = {
                    try {
                        val todoSection = TodoSection.valueOf(section.uppercase().replace(" ", "_"))
                        val todo = repository.addTodo(title, description, todoSection)
                        "✅ Added todo: ${todo.title} in ${todoSection.name.lowercase()}"
                    } catch (e: Exception) {
                        "❌ Failed: ${e.message}"
                    }
                }
            ),
            checkPermission = { true } // Already checked above
        ).result ?: throw Exception("Tool execution failed")
    }
    
    // Repeat for all other tools...
}
```

**PROBLEM:** This approach is complex because we need to pass callbacks through Hilt.

**BETTER APPROACH:** Use a simpler event-based system:

---

### STEP 10: Event-Based Permission System (RECOMMENDED)

**New File:** `app/src/main/java/com/yourname/voicetodo/ai/events/ToolExecutionEvents.kt`

```kotlin
object ToolExecutionEvents {
    
    data class ToolCallRequest(
        val toolName: String,
        val arguments: Map<String, Any?>,
        val onResponse: (Boolean) -> Unit
    )
    
    private val _pendingRequests = MutableSharedFlow<ToolCallRequest>()
    val pendingRequests: SharedFlow<ToolCallRequest> = _pendingRequests.asSharedFlow()
    
    suspend fun requestPermission(toolName: String, arguments: Map<String, Any?>): Boolean {
        return suspendCancellableCoroutine { continuation ->
            viewModelScope.launch {
                _pendingRequests.emit(
                    ToolCallRequest(
                        toolName = toolName,
                        arguments = arguments,
                        onResponse = { granted ->
                            continuation.resume(granted)
                        }
                    )
                )
            }
        }
    }
}
```

---

## 🎯 Testing Plan

After implementation, test these scenarios:

### Test 1: Sequential Tool Calls
```
User: "Add todo 'Buy milk', then add todo 'Walk dog', then list all todos"
Expected: 3 tool calls in sequence (add, add, list)
```

### Test 2: Permission Dialog
```
User: "Add todo 'Test'"
Expected: Dialog shows "addTodo" with arguments, user can approve/deny
```

### Test 3: Always Allow
```
User: Mark "Always Allow" for addTodo
User: "Add todo 'Another test'"
Expected: No dialog, executes immediately
```

### Test 4: Retry on Failure
```
Simulate: 500 error on first call
Expected: Automatic retry 3 times with backoff, show "Retrying..." in UI
```

### Test 5: Complex Multi-Step
```
User: "Add 3 todos: A, B, C, then mark B as in progress, then list all"
Expected: 5 tool calls, all with permission dialogs (unless always allowed)
```

---

## 📁 Files to Create/Modify

### New Files:
1. `ui/components/ToolPermissionDialog.kt`
2. `ai/permission/ToolPermissionManager.kt`
3. `ai/execution/RetryableToolExecutor.kt`
4. `ai/events/ToolExecutionEvents.kt` (if using event-based approach)

### Modified Files:
1. `ai/agent/TodoAgent.kt` - Add maxIterations, permission integration
2. `ai/tools/TodoTools.kt` - Add retry wrapper to each tool
3. `ui/screens/chat/ChatViewModel.kt` - Add tool activity state
4. `ui/screens/chat/ChatScreen.kt` - Display tool activities + permission dialog
5. `data/preferences/UserPreferences.kt` - Add allowed tools storage
6. `data/preferences/PreferencesKeys.kt` - Add ALLOWED_TOOLS key

---

## 🚀 Deployment Order

1. **Phase 1:** Update TodoAgent maxIterations (verify sequential calls work)
2. **Phase 2:** Add permission dialog UI (non-functional, just UI)
3. **Phase 3:** Add ToolPermissionManager + DataStore
4. **Phase 4:** Add RetryableToolExecutor
5. **Phase 5:** Integrate permission checks into TodoTools
6. **Phase 6:** Add tool activity display to UI
7. **Phase 7:** Test end-to-end
8. **Phase 8:** Polish + handle edge cases

---

## ⚠️ Important Notes

1. **Sequential calling already works!** We proved this in standalone test. Just need to ensure `maxIterations` is high enough (15-20).

2. **Permission system is the complex part** - needs careful integration without breaking Koog's flow.

3. **Retry logic** should be applied at the tool level, not the agent level, so individual tool failures don't stop the whole conversation.

4. **UI updates must be non-blocking** - show tool activities in real-time without freezing the chat.

5. **DataStore operations are async** - use proper coroutine scopes.

---

## 📝 Success Criteria

✅ User can say "Add 3 todos" and agent adds all 3 sequentially
✅ Permission dialog appears before each tool call (unless always allowed)
✅ Failed tool calls retry up to 3 times automatically
✅ UI shows real-time tool execution status
✅ User can set "Always Allow" for trusted tools
✅ Permissions persist across app restarts

---

## 🔍 Verification Steps for Code Review

When another agent implements this, verify:

1. **maxIterations** in TodoAgent is >= 15
2. **ToolPermissionDialog** has all 3 buttons (Allow Once, Always, Deny)
3. **RetryableToolExecutor** uses exponential backoff (1s, 2s, 4s)
4. **Tool activities** display correctly in chat UI
5. **DataStore** properly saves/loads allowed tools
6. **All 9 TodoTools** are wrapped with retry logic
7. **Error handling** for permission denial doesn't crash app
8. **UI thread** not blocked during tool execution

---

**End of Plan**

This plan is ready for implementation by a fast coding agent. Review the generated code against this spec.
