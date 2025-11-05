# Agent Development Guide - Voice Todo App

## Build Commands
- **Build:** `cd voice-todo-app && ./gradlew assembleDebug`
- **Test (all):** `./gradlew test`
- **Test (single):** `./gradlew test --tests "com.yourname.voicetodo.ExampleUnitTest"`
- **Instrumented test:** `./gradlew connectedDebugAndroidTest`
- **Lint:** `./gradlew lint`
- **Clean:** `./gradlew clean`

## Code Style

### Language & Structure
- **Language:** Kotlin (JVM target 17)
- **Architecture:** MVVM with Hilt DI, Repository pattern, Unidirectional data flow
- **Package:** `com.yourname.voicetodo` → organized as `data/`, `domain/`, `ai/`, `ui/`, `util/`, `di/`

### Imports & Formatting
- Group imports: Android → AndroidX → Third-party → Kotlin → Java
- Use explicit imports, avoid wildcards (no `import foo.*`)
- 4-space indentation, no tabs
- Max line length: 120 characters

### Types & Nullability
- Prefer `data class` for models, sealed classes for states
- Always specify nullability explicitly (`String?` vs `String`)
- Use Room annotations: `@Entity`, `@Dao`, `@Database`
- Use Hilt annotations: `@HiltAndroidApp`, `@HiltViewModel`, `@Module`, `@Inject`
- Compose: `@Composable`, `@Preview`

### Naming Conventions
- Classes: PascalCase (`TodoViewModel`, `ChatScreen`)
- Functions: camelCase (`getTodoById`, `markComplete`)
- Constants: UPPER_SNAKE_CASE (`const val MAX_RETRIES = 3`)
- Private properties: `_mutableState` (public as `state`)

### Async & State
- Use coroutines: `viewModelScope.launch`, `suspend fun`
- StateFlow for UI state: `StateFlow<T>` (immutable), `MutableStateFlow<T>` (internal)
- Room queries return `Flow<T>` for reactive updates
- Collect with `collectAsStateWithLifecycle()` in Compose

### Error Handling
- Wrap risky operations in `try-catch`
- Use `Result<T>` or sealed classes for operation results
- Log errors with context: `Log.e("TodoViewModel", "Failed to load", exception)`
- Show user-friendly messages, never crash silently

### Testing
- Unit tests: `src/test/kotlin/` with JUnit 4, Mockito
- Instrumented: `src/androidTest/kotlin/` with AndroidJUnit4
- Test file naming: `ClassNameTest.kt` (e.g., `TodoDaoTest.kt`)
