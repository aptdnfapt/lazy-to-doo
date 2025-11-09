# Voice-Controlled AI Todo App - Project Plan

> **🤖 AI-Agent Friendly Document**
> This plan is structured for both human developers and AI coding assistants.
> All commands are testable via terminal. External documentation links are provided throughout.

## 🎯 Project Overview

**App Name:** Voice Todo (working title)

**Platform:** Native Android (Kotlin)

**Target:** Android 7.0+ (API 24+)

**Core Concept:** Manage todo lists through natural voice conversations with an AI agent

**Repository Structure:**
```bash
# Current location
pwd
├── whisper-gemini-to-input/     # Existing voice-to-text project (reference)
└── PROJECT_PLAN.md              # This file
```

---

## 📱 Core User Flow

```
User speaks → Gemini 2.0 Flash transcribes → LLM processes with tool calls
→ Execute action → Show result in chat → Optional voice confirmation
```

---

## ✨ MVP Features

### 1. Voice Input System
- [ ] Mic button in chat interface
- [ ] Real-time recording indicator
- [ ] Audio recording via MediaRecorder (reuse existing code)
- [ ] Speech-to-text via Gemini 2.0 Flash API

### 2. AI Agent with Tool Calling
- [ ] Multi-provider LLM support (OpenAI, Anthropic, Gemini)
- [ ] 9 core tools for todo management
- [ ] Natural language understanding
- [ ] Context-aware responses

### 3. Todo Management Tools

| # | Tool | Description |
|---|------|-------------|
| 1 | `add_todo` | Add new todo item |
| 2 | `remove_todo` | Delete todo item |
| 3 | `edit_description` | Modify todo description |
| 4 | `mark_complete` | Move to completed section |
| 5 | `mark_in_progress` | Move to in-progress section |
| 6 | `mark_do_later` | Move to do-later section |
| 7 | `create_section` | Create custom section |
| 8 | `set_reminder` | Schedule notification |
| 9 | `read_out_loud` | Selective text-to-speech |

### 4. Permission System
- [ ] Confirmation dialog before executing actions
- [ ] Git-style diff view showing changes
- [ ] "Auto-approve" toggle in settings
- [ ] Per-tool permission settings

### 5. Chat Interface
- [ ] Scrollable message history
- [ ] User/Agent message bubbles
- [ ] Voice recording controls
- [ ] Loading states and indicators
- [ ] Error handling UI

### 6. Todo List View
- [ ] Sectioned list (Todo, In Progress, Done, Do Later)
- [ ] Visual todo items
- [ ] Section headers
- [ ] Empty states
- [ ] Pull to refresh

### 7. Local Storage
- [ ] Room database for todos
- [ ] Persistent chat history
- [ ] User preferences (DataStore)
- [ ] Encrypted API key storage

### 8. Text-to-Speech
- [ ] Selective reading (not everything)
- [ ] Agent decides what to read aloud
- [ ] Configurable voice and speed
- [ ] Skip structural content (lists, charts)

### 9. Notifications
- [ ] Reminder notifications (via set_reminder tool)
- [ ] WorkManager for scheduling
- [ ] Notification actions
- [ ] Handle notification taps

### 10. Settings
- [ ] LLM provider selection
- [ ] API key configuration
- [ ] TTS settings
- [ ] Permission preferences
- [ ] Theme selection

---

## 🏗️ Tech Stack

### Core Technologies
```
Language:        Kotlin
Min SDK:         24 (Android 7.0)
Target SDK:      34+
Build System:    Gradle (Kotlin DSL)
```

### UI Layer
```
Framework:       Jetpack Compose
Material:        Material 3
Navigation:      Compose Navigation
Theme:           Dynamic theming support
```

### Architecture
```
Pattern:         MVVM (Model-View-ViewModel)
DI:              Hilt (Dagger)
State:           StateFlow / SharedFlow
Async:           Kotlin Coroutines
```

### AI/LLM Integration
```
Voice-to-Text:   Gemini 2.0 Flash API (existing code)
Agent Framework: Koog 0.5.2 (JetBrains)
Tool Calling:    Koog @Tool annotations
LLM Providers:   OpenAI, Anthropic, Gemini (user choice)
```

### Data Layer
```
Database:        Room 2.6.1
Preferences:     DataStore
Key Storage:     EncryptedSharedPreferences
```

### Audio
```
Recording:       Android MediaRecorder (existing)
Transcription:   Gemini API (existing)
TTS:             Android TextToSpeech
```

### Networking
```
HTTP Client:     Ktor (via Koog dependency)
JSON:            kotlinx.serialization
```

### Background Work
```
Scheduling:      WorkManager
Notifications:   AndroidX Notification API
```

---

## 📁 Project Structure

```
pwd
├── src/main/
│   ├── java/com/yourname/voicetodo/
│   │   ├── VoiceTodoApp.kt                    # Application class
│   │   │
│   │   ├── data/
│   │   │   ├── local/
│   │   │   │   ├── TodoDatabase.kt            # Room database
│   │   │   │   ├── TodoDao.kt                 # Database access
│   │   │   │   ├── TodoEntity.kt              # Database entity
│   │   │   │   └── Converters.kt              # Type converters
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── TodoRepository.kt          # Todo data operations
│   │   │   │   └── TodoRepositoryImpl.kt
│   │   │   │
│   │   │   └── preferences/
│   │   │       ├── UserPreferences.kt         # DataStore wrapper
│   │   │       └── PreferencesKeys.kt
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Todo.kt                    # Todo domain model
│   │   │   │   ├── TodoSection.kt             # Section enum
│   │   │   │   ├── Message.kt                 # Chat message model
│   │   │   │   └── LLMProvider.kt             # Provider enum
│   │   │   │
│   │   │   └── usecase/
│   │   │       └── ManageTodosUseCase.kt      # Business logic
│   │   │
│   │   ├── ai/
│   │   │   ├── agent/
│   │   │   │   ├── TodoAgent.kt               # Koog agent setup
│   │   │   │   └── AgentConfig.kt             # Agent configuration
│   │   │   │
│   │   │   ├── tools/
│   │   │   │   ├── TodoTools.kt               # 9 tool implementations
│   │   │   │   ├── TTSTools.kt                # Text-to-speech tool
│   │   │   │   └── ToolUtils.kt               # Helper functions
│   │   │   │
│   │   │   ├── transcription/
│   │   │   │   ├── VoiceTranscriber.kt        # Gemini API (existing)
│   │   │   │   ├── RecorderManager.kt         # Audio recording (existing)
│   │   │   │   └── TranscriptionResult.kt
│   │   │   │
│   │   │   └── providers/
│   │   │       ├── LLMProviderFactory.kt      # Multi-provider factory
│   │   │       └── ProviderConfig.kt
│   │   │
│   │   ├── ui/
│   │   │   ├── MainActivity.kt                # Single activity
│   │   │   │
│   │   │   ├── screens/
│   │   │   │   ├── chat/
│   │   │   │   │   ├── ChatScreen.kt          # Main chat UI
│   │   │   │   │   ├── ChatViewModel.kt       # Chat logic
│   │   │   │   │   └── components/
│   │   │   │   │       ├── MessageBubble.kt   # Message UI
│   │   │   │   │       ├── MicButton.kt       # Recording button
│   │   │   │   │       ├── RecordingIndicator.kt
│   │   │   │   │       └── ChatInput.kt
│   │   │   │   │
│   │   │   │   ├── todos/
│   │   │   │   │   ├── TodoListScreen.kt      # Todo list view
│   │   │   │   │   ├── TodoListViewModel.kt
│   │   │   │   │   └── components/
│   │   │   │   │       ├── TodoItem.kt        # Individual todo
│   │   │   │   │       ├── SectionHeader.kt   # Section divider
│   │   │   │   │       └── EmptyState.kt
│   │   │   │   │
│   │   │   │   ├── settings/
│   │   │   │   │   ├── SettingsScreen.kt      # Settings UI
│   │   │   │   │   ├── SettingsViewModel.kt
│   │   │   │   │   └── components/
│   │   │   │   │       ├── ProviderSelector.kt
│   │   │   │   │       ├── ApiKeyInput.kt
│   │   │   │   │       └── TTSSettings.kt
│   │   │   │   │
│   │   │   │   └── permission/
│   │   │   │       ├── PermissionDialog.kt    # Tool confirmation
│   │   │   │       └── DiffView.kt            # Git-style diff
│   │   │   │
│   │   │   ├── navigation/
│   │   │   │   ├── NavGraph.kt                # Navigation setup
│   │   │   │   └── Screen.kt                  # Screen routes
│   │   │   │
│   │   │   └── theme/
│   │   │       ├── Color.kt                   # Color definitions
│   │   │       ├── Theme.kt                   # Theme setup
│   │   │       └── Type.kt                    # Typography
│   │   │
│   │   ├── util/
│   │   │   ├── PermissionManager.kt           # Runtime permissions
│   │   │   ├── NotificationHelper.kt          # Notification utils
│   │   │   ├── TTSManager.kt                  # TTS wrapper
│   │   │   ├── Extensions.kt                  # Kotlin extensions
│   │   │   └── Constants.kt                   # App constants
│   │   │
│   │   └── di/
│   │       ├── AppModule.kt                   # App-level DI
│   │       ├── DatabaseModule.kt              # Room DI
│   │       ├── AIModule.kt                    # Koog/AI DI
│   │       └── RepositoryModule.kt            # Repository DI
│   │
│   ├── res/
│   │   ├── values/
│   │   │   ├── strings.xml                    # String resources
│   │   │   └── themes.xml                     # Material themes
│   │   │
│   │   └── xml/
│   │       └── backup_rules.xml               # Backup configuration
│   │
│   └── AndroidManifest.xml                    # App manifest
│
├── build.gradle.kts                           # App build config
└── proguard-rules.pro                         # ProGuard rules
```

---

## 🔧 Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Koog AI Framework
    implementation("ai.koog:koog-agents:0.5.2")
    
    // Ktor (required by Koog)
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Security (Encrypted SharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // WorkManager (for notifications)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Accompanist (permissions)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

---

## 📅 Implementation Timeline

> **🤖 Testing Instructions:** Each phase includes terminal commands to verify progress

### Phase 1: Project Setup (Day 1)
**Goal:** Working project structure with all dependencies

**Tasks:**
- [ ] Create new Android project (Empty Compose Activity)
- [ ] Setup package structure
- [ ] Add all dependencies (Koog, Room, Hilt, etc.)
- [ ] Configure Hilt (@HiltAndroidApp)
- [ ] Setup basic navigation
- [ ] Create stub screens
- [ ] Test build and run

**Testing Commands:**
```bash
# Navigate to project directory
cd /path/to/voice-todo-app

# Verify Gradle setup
./gradlew tasks --all | grep "build"

# Check dependencies resolve
./gradlew dependencies --configuration implementation

# Build project (should succeed)
./gradlew assembleDebug

# Check for compilation errors
./gradlew compileDebugKotlin

# List available tasks
./gradlew tasks

# Generate project structure report
find app/src -type f -name "*.kt" | head -20
```

**Verification Checklist:**
- [ ] `./gradlew build` completes without errors
- [ ] Koog dependency appears in `./gradlew dependencies` output
- [ ] Room annotation processor (ksp) configured
- [ ] Hilt dependency present
- [ ] APK builds successfully: `app/build/outputs/apk/debug/app-debug.apk` exists

**Expected Output:**
```
BUILD SUCCESSFUL in 45s
12 actionable tasks: 12 executed
```

**Deliverable:** App launches with empty screens

---

### Phase 2: Voice Integration (Day 2)
**Goal:** Voice recording and transcription working

**Tasks:**
- [ ] Port `RecorderManager.kt` from existing project
- [ ] Port `WhisperTranscriber.kt` from existing project
- [ ] Adapt for new package structure
- [ ] Add audio permissions handling
- [ ] Create `VoiceTranscriber` wrapper
- [ ] Test: Record → Gemini API → Get text

**Reference Files to Copy:**
```bash
# View existing voice code
cat /home/idc/proj/app/whisper-gemini-to-input/android/app/src/main/java/com/example/whispertoinput/recorder/RecorderManager.kt

cat /home/idc/proj/app/whisper-gemini-to-input/android/app/src/main/java/com/example/whispertoinput/WhisperTranscriber.kt
```

**Testing Commands:**
```bash
# Check if audio permission is in manifest
grep -n "RECORD_AUDIO" app/src/main/AndroidManifest.xml

# Verify voice-related classes exist
find app/src -name "*Recorder*.kt" -o -name "*Transcri*.kt"

# Check for Gemini API integration
grep -r "gemini" app/src --include="*.kt"

# Build and check for audio-related errors
./gradlew assembleDebug 2>&1 | grep -i "audio\|record\|permission"
```

**Unit Test Example:**
```bash
# Run voice transcription test (if created)
./gradlew test --tests "*TranscriberTest"

# Check test results
cat app/build/reports/tests/testDebugUnitTest/index.html
```

**Verification Checklist:**
- [ ] RecorderManager.kt exists in new project
- [ ] WhisperTranscriber.kt exists in new project
- [ ] RECORD_AUDIO permission in AndroidManifest.xml
- [ ] Gemini API key configuration present
- [ ] No compilation errors related to audio

**Deliverable:** Voice recording transcribes to text

---

### Phase 3: Database & Repository (Day 3)
**Goal:** Local data storage working

**Tasks:**
- [ ] Create `TodoEntity` with Room annotations
- [ ] Create `TodoDao` with CRUD operations
- [ ] Setup `TodoDatabase`
- [ ] Create domain models (`Todo`, `TodoSection`)
- [ ] Implement `TodoRepository`
- [ ] Add type converters for enums/dates
- [ ] Write unit tests for repository
- [ ] Test: Save/retrieve todos from DB

**Documentation to Fetch:**
```bash
# Room documentation (for AI agents to fetch)
# https://developer.android.com/training/data-storage/room
# https://developer.android.com/reference/androidx/room/Entity
# https://developer.android.com/reference/androidx/room/Dao
```

**Testing Commands:**
```bash
# Check Room setup
grep -r "@Entity\|@Dao\|@Database" app/src --include="*.kt"

# Verify database schema
./gradlew app:kspDebugKotlin

# Check generated Room files
find app/build/generated/ksp -name "*_Impl.java" 2>/dev/null | head -5

# Run Room tests
./gradlew test --tests "*TodoRepositoryTest" --tests "*TodoDaoTest"

# Check for Room version conflicts
./gradlew dependencies | grep "room"

# Test database creation (via instrumentation test)
./gradlew connectedAndroidTest --tests "*DatabaseTest"
```

**Verification Checklist:**
- [ ] TodoEntity.kt has @Entity annotation
- [ ] TodoDao.kt has @Dao annotation
- [ ] TodoDatabase.kt has @Database annotation
- [ ] Room compiler (ksp) generates implementation files
- [ ] Repository unit tests pass
- [ ] No Room migration warnings in logs

**Database Schema Verification:**
```bash
# View generated schema (if schema export enabled)
cat app/schemas/com.yourname.voicetodo.data.local.TodoDatabase/1.json
```

**Deliverable:** Todos persist in database

---

### Phase 4: Koog Agent Setup (Day 4)
**Goal:** AI agent with tool calling functional

**Tasks:**
- [ ] Create `TodoTools.kt` class with all 9 tools
- [ ] Add `@Tool` and `@LLMDescription` annotations
- [ ] Create `TTSTools.kt` for read_out_loud
- [ ] Setup `TodoAgent` with Koog
- [ ] Configure multi-provider support
- [ ] Create `LLMProviderFactory`
- [ ] Add API key configuration
- [ ] Test: Send prompt → Tool executed → Response

**Documentation to Fetch:**
```bash
# Koog official documentation
# https://docs.koog.ai/core-concepts/tools/
# https://docs.koog.ai/core-concepts/agents/
# https://docs.koog.ai/core-concepts/executors/
# https://github.com/JetBrains/koog/tree/main/examples
```

**Testing Commands:**
```bash
# Check Koog dependency
./gradlew dependencies | grep "koog"

# Verify @Tool annotations exist
grep -r "@Tool" app/src --include="*.kt"

# Check agent configuration
grep -r "AIAgent\|ToolRegistry\|simpleOpenAIExecutor" app/src --include="*.kt"

# Test tool execution (unit test)
./gradlew test --tests "*TodoToolsTest"

# Check if all 9 tools are defined
grep -c "@Tool" app/src/main/java/**/ai/tools/TodoTools.kt

# Verify Koog classes are accessible
./gradlew compileDebugKotlin 2>&1 | grep -i "unresolved\|cannot find"

# Test agent initialization
./gradlew test --tests "*TodoAgentTest"
```

**Quick Koog Verification:**
```kotlin
// Minimal test to verify Koog works
// Create file: app/src/test/kotlin/KoogSmokeTest.kt
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import org.junit.Test

class KoogSmokeTest {
    @Test
    fun `koog dependency is available`() {
        // If this compiles, Koog is working
        val registry = ToolRegistry { }
        assert(registry != null)
    }
}
```

**Run Smoke Test:**
```bash
./gradlew test --tests "KoogSmokeTest"
```

**Verification Checklist:**
- [ ] Koog dependency resolves (check with `./gradlew dependencies`)
- [ ] All 9 @Tool annotated functions exist
- [ ] AIAgent can be instantiated
- [ ] ToolRegistry properly configured
- [ ] Multi-provider executors (OpenAI, Anthropic, Gemini) available
- [ ] No Koog-related compilation errors

**API Provider Test:**
```bash
# Test if API keys are loaded (without exposing them)
./gradlew test --tests "*ProviderConfigTest"

# Check for API key configuration classes
grep -r "apiKey\|API_KEY" app/src --include="*.kt" | grep -v "// TODO"
```

**Deliverable:** Agent executes tools from text input

---

### Phase 5: Chat UI (Days 5-6)
**Goal:** Complete chat interface with voice input

#### Day 5: Chat Screen Layout
**Tasks:**
- [ ] Create `ChatScreen.kt` composable
- [ ] Add LazyColumn for messages
- [ ] Create `MessageBubble` component
- [ ] Add `MicButton` component
- [ ] Add `RecordingIndicator` animation
- [ ] Implement message input field
- [ ] Add loading states

**Testing Commands:**
```bash
# Check Compose UI files exist
find app/src -name "*ChatScreen*.kt" -o -name "*MessageBubble*.kt"

# Verify @Composable annotations
grep -r "@Composable" app/src/main/**/ui/screens/chat/ --include="*.kt"

# Build and check for Compose errors
./gradlew assembleDebug 2>&1 | grep -i "compose\|@composable"

# Check for Material3 usage
grep -r "Material3\|androidx.compose.material3" app/src --include="*.kt"
```

#### Day 6: Chat ViewModel & Logic
**Tasks:**
- [ ] Create `ChatViewModel`
- [ ] Integrate voice recording flow
- [ ] Connect to agent for responses
- [ ] Handle message state
- [ ] Add error handling
- [ ] Implement retry logic
- [ ] Test: Voice → Transcribe → Agent → Display

**Testing Commands:**
```bash
# Check ViewModel exists and extends properly
grep -r "class.*ViewModel.*:" app/src --include="*.kt"

# Verify Hilt injection in ViewModel
grep -r "@HiltViewModel" app/src --include="*.kt"

# Check StateFlow usage
grep -r "StateFlow\|MutableStateFlow" app/src --include="*.kt"

# Run ViewModel unit tests
./gradlew test --tests "*ChatViewModelTest"

# Check for coroutines usage
grep -r "viewModelScope\|launch\|async" app/src --include="*.kt"

# Verify voice integration
grep -r "VoiceTranscriber\|RecorderManager" app/src/main/**/ui/ --include="*.kt"
```

**Integration Test:**
```bash
# Create simple integration test
# File: app/src/androidTest/kotlin/ChatIntegrationTest.kt

# Run on emulator/device
./gradlew connectedAndroidTest --tests "*ChatIntegrationTest"
```

**Verification Checklist:**
- [ ] ChatScreen.kt compiles without errors
- [ ] MessageBubble displays user/agent messages correctly
- [ ] MicButton triggers recording
- [ ] ViewModel properly manages state
- [ ] Voice recording → transcription flow works
- [ ] Agent responses display in chat

**Deliverable:** Working chat interface with voice input

---

### Phase 6: Todo List View (Day 7)
**Goal:** Visual todo list screen

**Tasks:**
- [ ] Create `TodoListScreen.kt`
- [ ] Implement sectioned list (TODO, IN_PROGRESS, etc.)
- [ ] Create `TodoItem` component
- [ ] Add `SectionHeader` component
- [ ] Implement empty states
- [ ] Add pull-to-refresh
- [ ] Create `TodoListViewModel`
- [ ] Connect to repository
- [ ] Test: Todos display correctly by section

**Testing Commands:**
```bash
# Verify todo list components exist
find app/src -path "*/ui/screens/todos/*.kt"

# Check Room integration in ViewModel
grep -r "TodoRepository\|todoRepository" app/src/main/**/todos/ --include="*.kt"

# Check Flow collection for reactive updates
grep -r "collectAsStateWithLifecycle\|collectAsState" app/src/main/**/todos/ --include="*.kt"

# Run ViewModel tests
./gradlew test --tests "*TodoListViewModelTest"

# Build and check for errors
./gradlew assembleDebug
```

**Verification Checklist:**
- [ ] TodoListScreen displays todos from database
- [ ] Sections properly separate todos by state
- [ ] TodoItem UI matches design
- [ ] Empty state shows when no todos
- [ ] Pull-to-refresh updates list
- [ ] Navigation to/from chat works

**Deliverable:** Visual list of todos organized by section

---

### Phase 7: Settings Screen (Day 8)
**Goal:** User configuration working

**Tasks:**
- [ ] Create `SettingsScreen.kt`
- [ ] Add LLM provider selector
- [ ] Create API key input fields
- [ ] Add encrypted key storage
- [ ] Implement TTS settings (voice, speed)
- [ ] Add permission preferences
- [ ] Create theme selector
- [ ] Setup `UserPreferences` DataStore
- [ ] Test: Settings persist and affect behavior

**Testing Commands:**
```bash
# Check DataStore setup
grep -r "dataStore\|DataStore\|Preferences" app/src --include="*.kt"

# Verify encrypted storage
grep -r "EncryptedSharedPreferences\|MasterKey" app/src --include="*.kt"

# Check settings screen exists
find app/src -name "*Settings*.kt"

# Test preferences persistence
./gradlew test --tests "*UserPreferencesTest"

# Build and verify
./gradlew assembleDebug
```

**Verification Checklist:**
- [ ] Settings screen accessible from navigation
- [ ] API keys stored encrypted
- [ ] Provider selection updates agent configuration
- [ ] Settings persist across app restarts
- [ ] TTS settings affect voice output

**Deliverable:** Settings screen with persistent preferences

---

### Phase 8: Permission System (Day 9)
**Goal:** Tool confirmation dialogs working

**Tasks:**
- [ ] Create `PermissionDialog` composable
- [ ] Implement diff view (show changes)
- [ ] Add Allow/Deny actions
- [ ] Create "Don't ask again" toggle
- [ ] Integrate into tool execution flow
- [ ] Add per-tool permission settings
- [ ] Test: Tool asks permission → User approves → Executes

**Testing Commands:**
```bash
# Check permission dialog exists
find app/src -name "*Permission*.kt"

# Verify dialog integration with tools
grep -r "showPermissionDialog\|PermissionDialog" app/src --include="*.kt"

# Check for approval state management
grep -r "approved\|permitted\|allowed" app/src/main/**/ai/ --include="*.kt"

# Build and test
./gradlew assembleDebug
```

**Verification Checklist:**
- [ ] Dialog shows before tool execution
- [ ] Diff view displays intended changes
- [ ] User can approve/deny
- [ ] "Don't ask again" persists preference
- [ ] Tools execute only after approval

**Deliverable:** Permission dialogs before tool execution

---

### Phase 9: Notifications (Day 10)
**Goal:** Reminder notifications working

**Tasks:**
- [ ] Setup WorkManager
- [ ] Create notification channel
- [ ] Implement `set_reminder` tool logic
- [ ] Create notification builder
- [ ] Add notification actions
- [ ] Handle notification taps
- [ ] Add permission request (POST_NOTIFICATIONS)
- [ ] Test: Set reminder → Notification fires

**Documentation to Fetch:**
```bash
# WorkManager documentation
# https://developer.android.com/topic/libraries/architecture/workmanager
# https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work
```

**Testing Commands:**
```bash
# Check WorkManager setup
grep -r "WorkManager\|Worker\|WorkRequest" app/src --include="*.kt"

# Verify notification channel creation
grep -r "NotificationChannel\|createNotificationChannel" app/src --include="*.kt"

# Check POST_NOTIFICATIONS permission
grep -n "POST_NOTIFICATIONS" app/src/main/AndroidManifest.xml

# Test WorkManager scheduling
./gradlew test --tests "*ReminderWorkerTest"

# Build and verify
./gradlew assembleDebug
```

**Manual Testing:**
```bash
# After deploying to device, check scheduled work
adb shell dumpsys jobscheduler | grep -A 20 "voicetodo"

# Check notification channels
adb shell cmd notification list_channels com.yourname.voicetodo

# Trigger notification manually for testing
adb shell cmd notification post -t "Test Todo" com.yourname.voicetodo tag1
```

**Verification Checklist:**
- [ ] WorkManager dependency added
- [ ] Notification channel created
- [ ] set_reminder tool schedules work
- [ ] Notifications appear at scheduled time
- [ ] Notification taps open app correctly

**Deliverable:** Reminders trigger notifications at scheduled time

---

### Phase 10: Testing & Polish (Days 11-12)
**Goal:** Bug-free, polished MVP

#### Day 11: Testing
**Tasks:**
- [ ] End-to-end voice flow testing
- [ ] All 9 tools tested individually
- [ ] Multi-provider switching tested
- [ ] Permission flow tested
- [ ] Database operations verified
- [ ] Notification timing verified
- [ ] Error cases handled
- [ ] Memory leak check

**Comprehensive Testing Commands:**
```bash
# Run all unit tests
./gradlew test

# Run all instrumentation tests
./gradlew connectedAndroidTest

# Generate test report
./gradlew test
open app/build/reports/tests/testDebugUnitTest/index.html

# Check code coverage
./gradlew jacocoTestReport
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# Memory leak detection (if LeakCanary added)
./gradlew connectedAndroidTest --tests "*LeakTest"

# APK size check
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Method count (for 64K limit)
./gradlew assembleDebug
# Use dex-method-counts tool or check build output
```

**Manual Testing Checklist:**
```bash
# Install on device
./gradlew installDebug

# Launch app
adb shell am start -n com.yourname.voicetodo/.ui.MainActivity

# Check logs while testing
adb logcat | grep "VoiceTodo"

# Test each tool
adb shell input text "Add todo buy milk"
adb shell input text "Mark todo 1 complete"
# ... etc for all 9 tools

# Check database contents
adb shell "run-as com.yourname.voicetodo cat /data/data/com.yourname.voicetodo/databases/todo_database"

# Monitor memory usage
adb shell dumpsys meminfo com.yourname.voicetodo

# Test multi-provider switching
# (Test with OpenAI, then Anthropic, then Gemini)
```

**Error Scenario Testing:**
```bash
# Test with airplane mode (network errors)
adb shell svc wifi disable
adb shell svc data disable

# Test with revoked permissions
adb shell pm revoke com.yourname.voicetodo android.permission.RECORD_AUDIO

# Test with low storage
adb shell sm set-virtual-disk false

# Test with invalid API key
# (Manually set invalid key in settings)
```

#### Day 12: Polish
**Tasks:**
- [ ] Add animations and transitions
- [ ] Improve loading states
- [ ] Add haptic feedback
- [ ] Improve error messages
- [ ] Add empty state illustrations
- [ ] Optimize performance
- [ ] Add app icon and splash screen
- [ ] Final UI tweaks

**Testing Commands:**
```bash
# Check for hardcoded strings
grep -r '"[A-Z].*"' app/src/main/**/ui/ --include="*.kt" | grep -v "// OK" | head -20

# Verify all strings in resources
grep -r "getString\|stringResource" app/src --include="*.kt"

# Check for lint warnings
./gradlew lint
open app/build/reports/lint-results-debug.html

# Optimize images (if any)
find app/src/main/res -name "*.png" -exec optipng -o7 {} \;

# Check ProGuard configuration
cat app/proguard-rules.pro

# Build release APK
./gradlew assembleRelease

# Verify R8/ProGuard worked
ls -lh app/build/outputs/apk/release/app-release-unsigned.apk
```

**Performance Profiling:**
```bash
# Profile app on device
adb shell am start -W com.yourname.voicetodo/.ui.MainActivity

# Generate CPU profile
adb shell am profile start com.yourname.voicetodo /data/local/tmp/profile.trace
# Use app for 30 seconds
adb shell am profile stop com.yourname.voicetodo
adb pull /data/local/tmp/profile.trace

# Check for ANRs (App Not Responding)
adb shell dumpsys dropbox --print system_app_anr

# Measure startup time
adb shell am start -S -W com.yourname.voicetodo/.ui.MainActivity | grep "TotalTime"
```

**Final Verification:**
```bash
# Create signed APK
./gradlew assembleRelease

# Install and test release build
./gradlew installRelease

# Verify app works in release mode
adb shell am start -n com.yourname.voicetodo/.ui.MainActivity

# Check no debug logs in release
adb logcat | grep -i "debug\|verbose" | grep "VoiceTodo"
```

**Deliverable:** Production-ready MVP

---

## 🎯 Success Criteria

The MVP is complete when:

- [ ] User can speak and have speech transcribed
- [ ] Agent correctly interprets intent and calls tools
- [ ] All 9 tools work as expected
- [ ] Todos persist across app restarts
- [ ] User can switch between LLM providers
- [ ] Permission system works correctly
- [ ] Notifications fire at scheduled times
- [ ] TTS reads appropriate responses
- [ ] No crashes during normal usage
- [ ] Settings persist correctly
- [ ] UI is intuitive and responsive

---

## 📊 Estimated Costs (Monthly)

**For 1000 voice commands/month:**

| Service | Usage | Cost |
|---------|-------|------|
| Gemini 2.0 Flash (voice-to-text) | ~1000 transcriptions | $0.50 |
| OpenAI GPT-4o (tool calling) | ~1000 requests | $3-5 |
| **Total** | | **~$5/month** |

*Note: Costs reduce if using only Gemini for both transcription and tool calling*

---

## 🚀 Post-MVP Features (Future)

### Phase 11: Web Version (Kotlin Multiplatform)
- Share business logic
- Web UI with Compose HTML or React
- Cloud sync

### Phase 12: iOS Version (Kotlin Multiplatform)
- Native iOS app
- Shared codebase with Android

### Phase 13: Advanced Features
- Widget for quick voice input
- Wear OS companion app
- Location-based reminders
- Collaboration/sharing
- Voice shortcuts
- Offline mode with local LLMs
- Analytics dashboard

---

## 📚 Key Documentation Sources

> **🤖 For AI Agents:** Fetch these URLs during development for up-to-date API references

### Koog Framework (Primary)
- **Main Documentation:** https://docs.koog.ai
  - Getting Started: https://docs.koog.ai/getting-started/
  - Tool Creation: https://docs.koog.ai/core-concepts/tools/
  - Agent Configuration: https://docs.koog.ai/core-concepts/agents/
  - Multi-Provider Setup: https://docs.koog.ai/core-concepts/executors/
- **GitHub Repository:** https://github.com/JetBrains/koog
  - Examples folder: https://github.com/JetBrains/koog/tree/main/examples
  - Simple agent examples: Check examples/build.gradle.kts for runnable demos
- **API Reference:** https://api.koog.ai
- **Kotlin AI Examples:** https://github.com/Kotlin/Kotlin-AI-Examples

### Android Development
- **Jetpack Compose:**
  - Official Guide: https://developer.android.com/jetpack/compose
  - Tutorial: https://developer.android.com/jetpack/compose/tutorial
  - State Management: https://developer.android.com/jetpack/compose/state
- **Room Database:**
  - Guide: https://developer.android.com/training/data-storage/room
  - Migration: https://developer.android.com/training/data-storage/room/migrating-db-versions
- **WorkManager:**
  - Guide: https://developer.android.com/topic/libraries/architecture/workmanager
  - Scheduling: https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work
- **Hilt Dependency Injection:**
  - Guide: https://developer.android.com/training/dependency-injection/hilt-android
  - Android: https://developer.android.com/training/dependency-injection/hilt-android

### LLM Provider APIs
- **Gemini API (Voice-to-Text):**
  - Main: https://ai.google.dev/gemini-api/docs
  - Audio: https://ai.google.dev/gemini-api/docs/audio
  - Function Calling: https://ai.google.dev/gemini-api/docs/function-calling
- **OpenAI API:**
  - Main: https://platform.openai.com/docs
  - Chat Completions: https://platform.openai.com/docs/guides/chat-completions
  - Function Calling: https://platform.openai.com/docs/guides/function-calling
- **Anthropic Claude API:**
  - Main: https://docs.anthropic.com
  - Tool Use: https://docs.anthropic.com/en/docs/tool-use
  - Best Practices: https://docs.anthropic.com/en/docs/build-with-claude/tool-use

### Reference Implementation
- **Your Existing Voice Code:** `/home/idc/proj/app/whisper-gemini-to-input/`
  - RecorderManager: `android/app/src/main/java/com/example/whispertoinput/recorder/RecorderManager.kt`
  - WhisperTranscriber: `android/app/src/main/java/com/example/whispertoinput/WhisperTranscriber.kt`

---

## 🛠️ Development Environment Setup

### Required Tools
- **IDE:** Android Studio (latest stable)
- **JDK:** JDK 17 or higher
- **Android SDK:** API 24+ (minimum), API 34 (target)
- **Emulator:** Android 10+ device or physical device

### API Keys Needed
1. **Gemini API Key** (for voice-to-text)
   - Get from: https://makersuite.google.com/app/apikey
   
2. **LLM Provider API Key** (choose one or all):
   - OpenAI: https://platform.openai.com/api-keys
   - Anthropic: https://console.anthropic.com
   - Gemini: (same as above)

### Initial Setup Commands
```bash
# Clone your existing voice project for reference
cd ~/projects
git clone https://github.com/aptdnfapt/whisper-gemini-to-input

# Create new Android project
# (Use Android Studio: New Project > Empty Compose Activity)

# Verify environment
./gradlew --version
```

---

## 🐛 Common Issues & Solutions

### Issue: Koog dependency not resolving
**Solution:** Ensure `mavenCentral()` is in repositories

### Issue: Room database migration errors
**Solution:** Use `fallbackToDestructiveMigration()` during development

### Issue: API key security
**Solution:** Use EncryptedSharedPreferences, never commit keys

### Issue: Voice permissions denied
**Solution:** Handle permission denial gracefully, show explanation

### Issue: Large APK size
**Solution:** Enable R8/ProGuard, use APK splits

---

## 📝 Notes

### Design Decisions
- **Why Koog?** Official JetBrains support, multi-provider, clean API
- **Why Native Android?** Best performance, reuse existing voice code
- **Why Room?** Offline-first, reactive queries with Flow
- **Why Compose?** Modern UI toolkit, less boilerplate

### Architecture Principles
- Single Activity with Compose Navigation
- Unidirectional data flow (UDF)
- Repository pattern for data access
- Dependency injection for testability
- Separation of concerns (UI/Domain/Data)

### Code Style
- Follow official Kotlin conventions
- Use meaningful variable names
- Document complex logic with comments
- Keep functions small and focused
- Use coroutines for async operations

---

## ✅ Pre-Launch Checklist

### Code Quality
- [ ] No hardcoded strings (use strings.xml)
- [ ] No API keys in code
- [ ] Error handling on all network calls
- [ ] Loading states on all async operations
- [ ] Input validation on all user inputs

### Testing
- [ ] Manual testing on multiple devices
- [ ] Test with different API providers
- [ ] Test with poor network conditions
- [ ] Test permission denial scenarios
- [ ] Test low storage scenarios

### Performance
- [ ] No memory leaks (use LeakCanary)
- [ ] Smooth 60fps scrolling
- [ ] Fast app startup (<2s)
- [ ] Efficient battery usage
- [ ] Reasonable APK size (<50MB)

### Security
- [ ] API keys encrypted
- [ ] No sensitive data in logs
- [ ] HTTPS for all network calls
- [ ] ProGuard rules configured
- [ ] Permissions requested only when needed

### UX
- [ ] Consistent design language
- [ ] Clear error messages
- [ ] Loading indicators on all waits
- [ ] Empty states with guidance
- [ ] Confirmation on destructive actions

---

## 📞 Support & Contact

**Project Repository:** [To be created]

**Issues/Questions:** [To be defined]

**Documentation:** This file + inline code comments

---

**Last Updated:** 2025-10-31

**Version:** 1.0 (MVP Plan)

**Status:** Ready to implement

---

## 🤖 AI Agent Development Guide

### For AI Coding Assistants

This section provides specific guidance for AI agents helping to build this project.

#### How to Approach Development

**1. Always Fetch Latest Documentation**
Before implementing any feature, fetch the relevant documentation:

```bash
# Example: Before implementing Koog agent
# Fetch: https://docs.koog.ai/core-concepts/agents/
# Fetch: https://docs.koog.ai/core-concepts/tools/
# Fetch: https://github.com/JetBrains/koog/tree/main/examples
```

**2. Reference Existing Code**
The existing voice-to-text project is available at:
```bash
/home/idc/proj/app/whisper-gemini-to-input/
```

Use these commands to explore it:
```bash
# View project structure
tree /home/idc/proj/app/whisper-gemini-to-input/android/app/src/main -L 3

# Read specific files
cat /home/idc/proj/app/whisper-gemini-to-input/android/app/src/main/java/com/example/whispertoinput/WhisperTranscriber.kt

# Search for patterns
grep -r "transcribe\|gemini" /home/idc/proj/app/whisper-gemini-to-input/android --include="*.kt"
```

**3. Verify Before Proceeding**
After each implementation step, run verification commands:

```bash
# Compilation check
./gradlew compileDebugKotlin

# Dependency check
./gradlew dependencies --configuration implementation | grep -i "koog\|room\|hilt"

# Find files you created
find app/src -name "*.kt" -newer /tmp/timestamp

# Count lines of code
find app/src -name "*.kt" | xargs wc -l | tail -1
```

**4. Test Incrementally**
Don't wait until the end to test. After each phase:

```bash
# Quick compilation test
./gradlew assembleDebug

# Run relevant unit tests
./gradlew test --tests "*NewFeatureTest"

# Check for errors
./gradlew assembleDebug 2>&1 | grep -i "error\|failed"
```

#### Common Commands Reference

**Project Navigation:**
```bash
# Current project location
cd /home/idc/proj/app

# List project structure
tree -L 2 -I 'build|.git|.gradle'

# Find specific files
find . -name "*.kt" -path "*/main/*" | head -20
```

**Code Search:**
```bash
# Find all @Tool annotations
grep -r "@Tool" app/src --include="*.kt" -n

# Find all ViewModels
find app/src -name "*ViewModel.kt"

# Search for specific patterns
grep -r "AIAgent\|TodoTools" app/src --include="*.kt"
```

**Dependency Management:**
```bash
# Check what dependencies are used
./gradlew dependencies --configuration implementation

# Check for version conflicts
./gradlew dependencyInsight --dependency room

# Refresh dependencies
./gradlew --refresh-dependencies
```

**Build and Test:**
```bash
# Clean build
./gradlew clean assembleDebug

# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "com.yourname.voicetodo.TodoToolsTest"

# Check test results
cat app/build/test-results/testDebugUnitTest/*.xml
```

**Debugging:**
```bash
# View build errors in detail
./gradlew assembleDebug --stacktrace

# Check for unresolved references
./gradlew compileDebugKotlin 2>&1 | grep "Unresolved"

# List all compilation errors
./gradlew assembleDebug 2>&1 | grep "^e: "
```

#### Decision-Making Framework

When you encounter ambiguity, follow this decision tree:

**Q: Which LLM provider should I default to?**
A: Use Gemini (simplest - same for voice + tools)

**Q: How to structure the project?**
A: Follow the structure in "📁 Project Structure" section above

**Q: Should I use feature X from Koog?**
A: Check if it's in the MVP features list. If not, skip it.

**Q: Database migration strategy?**
A: Use `fallbackToDestructiveMigration()` during development

**Q: Dependency version conflicts?**
A: Use versions specified in "🔧 Dependencies" section

**Q: Testing approach?**
A: Start with unit tests, add integration tests for critical paths

**Q: Error handling pattern?**
A: Use Result<T> or sealed classes for operation results

#### File Creation Order

Follow this order to minimize compilation errors:

1. **Data Layer** (bottom-up)
   ```
   TodoEntity.kt → TodoDao.kt → TodoDatabase.kt → TodoRepository.kt
   ```

2. **Domain Layer**
   ```
   Todo.kt → TodoSection.kt → ManageTodosUseCase.kt
   ```

3. **AI Layer**
   ```
   TodoTools.kt → TodoAgent.kt → LLMProviderFactory.kt
   ```

4. **UI Layer** (feature by feature)
   ```
   ChatViewModel.kt → ChatScreen.kt → components/*.kt
   ```

5. **Integration**
   ```
   Hilt modules → Navigation → MainActivity
   ```

#### Validation Checklist

Before moving to the next phase, verify:

```bash
# ✅ Code compiles
./gradlew compileDebugKotlin

# ✅ No lint errors (warnings OK)
./gradlew lintDebug

# ✅ Tests pass
./gradlew test

# ✅ APK builds
./gradlew assembleDebug

# ✅ Required files exist
# (Check against project structure)
```

#### Getting Unstuck

If you encounter issues:

**1. Compilation Errors:**
```bash
# Clean and rebuild
./gradlew clean compileDebugKotlin --stacktrace

# Check for missing imports
grep -r "import.*\$" app/src --include="*.kt"

# Verify dependency versions
./gradlew dependencies | less
```

**2. Runtime Errors:**
```bash
# Check logs
adb logcat | grep -i "exception\|error"

# Check crash reports
adb shell run-as com.yourname.voicetodo cat /data/data/com.yourname.voicetodo/files/crashes.log
```

**3. Test Failures:**
```bash
# Run with detailed output
./gradlew test --info

# Check test report
open app/build/reports/tests/testDebugUnitTest/index.html
```

**4. Documentation Needed:**
```bash
# Fetch relevant docs using webfetch tool
# Example URLs from "📚 Key Documentation Sources" section
```

---

## 📊 Progress Tracking

Create a simple progress tracker:

```bash
# Create progress file
cat > /home/idc/proj/app/PROGRESS.md << 'PROGRESS'
# Development Progress

## Phase 1: Project Setup
- [ ] Project created
- [ ] Dependencies added
- [ ] First build successful

## Phase 2: Voice Integration
- [ ] RecorderManager ported
- [ ] WhisperTranscriber ported
- [ ] Voice recording works

## Phase 3: Database
- [ ] TodoEntity created
- [ ] TodoDao created
- [ ] Room tests pass

## Phase 4: Koog Agent
- [ ] TodoTools implemented
- [ ] Agent configured
- [ ] Tool calling works

## Phase 5: Chat UI
- [ ] ChatScreen created
- [ ] ChatViewModel working
- [ ] Voice input functional

## Phase 6: Todo List
- [ ] TodoListScreen created
- [ ] List displays todos
- [ ] Sections work

## Phase 7: Settings
- [ ] SettingsScreen created
- [ ] Preferences persist
- [ ] API keys stored

## Phase 8: Permissions
- [ ] Permission dialog works
- [ ] Diff view shows changes
- [ ] Approval flow works

## Phase 9: Notifications
- [ ] WorkManager setup
- [ ] Reminders schedule
- [ ] Notifications fire

## Phase 10: Testing & Polish
- [ ] All tests pass
- [ ] No memory leaks
- [ ] UI polished
- [ ] Release APK built

PROGRESS

# Check progress
cat /home/idc/proj/app/PROGRESS.md
```

Update this file as you complete tasks:
```bash
# Mark task complete
sed -i 's/\[ \] Project created/[x] Project created/' /home/idc/proj/app/PROGRESS.md
```

---

## 🎯 Success Metrics

The MVP is successful when all these commands pass:

```bash
# 1. Build succeeds
./gradlew assembleDebug
echo "✅ Build: $?"

# 2. Tests pass
./gradlew test
echo "✅ Tests: $?"

# 3. Lint warnings minimal
./gradlew lintDebug | grep "0 errors" && echo "✅ Lint passed"

# 4. APK size reasonable (<50MB)
APK_SIZE=$(stat -f%z app/build/outputs/apk/debug/app-debug.apk 2>/dev/null || stat -c%s app/build/outputs/apk/debug/app-debug.apk)
[ $APK_SIZE -lt 52428800 ] && echo "✅ APK size: $(($APK_SIZE/1048576))MB"

# 5. All features implemented
grep -c "\[x\]" /home/idc/proj/app/PROGRESS.md && echo "✅ Features complete"
```

---

## 📞 Quick Reference Card

**Essential Commands:**
```bash
# Build
./gradlew assembleDebug

# Test
./gradlew test

# Install
./gradlew installDebug

# Clean
./gradlew clean

# Logs
adb logcat | grep VoiceTodo
```

**Essential Files:**
```
app/build.gradle.kts           - Dependencies
app/src/main/AndroidManifest.xml - Permissions
app/src/main/java/.../         - Source code
app/src/test/kotlin/.../       - Unit tests
```

**Essential URLs:**
```
Koog: https://docs.koog.ai
Room: https://developer.android.com/training/data-storage/room
Compose: https://developer.android.com/jetpack/compose
Gemini: https://ai.google.dev/gemini-api/docs
```

---

**Document Version:** 1.1 (AI-Agent Enhanced)

**Last Updated:** 2025-10-31

**Status:** Ready for AI-assisted development
