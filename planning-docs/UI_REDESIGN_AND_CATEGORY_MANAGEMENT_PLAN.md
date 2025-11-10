# Voice Todo App - UI Redesign & Category Management Plan

## Executive Summary
This plan outlines a comprehensive redesign of the Voice Todo App to introduce **category-based organization** (Work/Life/Study/etc.) while maintaining the existing status system (TODO/IN_PROGRESS/DONE/DO_LATER). The redesign includes new UI mockups, markdown-based task details, section-aware AI chat, and expanded tool capabilities.

---

## Table of Contents
1. [Current Architecture Analysis](#1-current-architecture-analysis)
2. [New Data Model & Architecture](#2-new-data-model--architecture)
3. [UI Redesign Specifications](#3-ui-redesign-specifications)
4. [AI Tool Call Updates](#4-ai-tool-call-updates)
5. [Implementation Phases](#5-implementation-phases)
6. [File Changes Checklist](#6-file-changes-checklist)

---

## 1. Current Architecture Analysis

### 1.1 Existing Data Structure

**Current TodoEntity:**
```kotlin
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val section: String, // Maps to TodoSection enum (TODO/IN_PROGRESS/DONE/DO_LATER)
    val createdAt: Long,
    val completedAt: Long?,
    val reminderTime: Long?
)
```

**Current TodoSection Enum:**
```kotlin
enum class TodoSection {
    TODO,          // To-Do status
    IN_PROGRESS,   // In Progress status
    DONE,          // Done status
    DO_LATER       // Do Later status
}
```

**Problem:** Current design conflates "status" (workflow state) with "category" (life area).

### 1.2 Current UI Screens
- **TodoListScreen:** Shows todos grouped by status sections (TODO/IN_PROGRESS/DONE/DO_LATER)
- **ChatScreen:** Generic chat interface with AI, no section filtering
- No chat history screen
- Basic text input for todo description

### 1.3 Current Tool Calls
- `addTodo(title, description, section)` - section param uses status values
- `listTodos(section)` - filters by status
- `markComplete/markInProgress/markDoLater(todoId)` - changes status
- `createSection(name)` - Currently returns error since sections are enum

---

## 2. New Data Model & Architecture

### 2.1 Conceptual Model

**Two-Level Hierarchy:**
1. **Category** (top-level): Work, Life, Study, Personal, etc. (user-definable)
2. **Status** (sub-level): TODO, IN_PROGRESS, DONE, DO_LATER (fixed workflow states)

**Example Structure:**
```
📁 Work (Category)
   ├── 📝 TODO
   │   └── "Draft Q3 report"
   ├── 🔄 IN_PROGRESS
   │   └── "Plan team offsite"
   └── ✅ DONE
       └── "Submit expenses"

📁 Life (Category)
   ├── 📝 TODO
   │   └── "Book dentist"
   └── ⏰ DO_LATER
       └── "Organize garage"
```

### 2.2 New Database Schema

#### 2.2.1 Create CategoryEntity

**New File:** `app/src/main/java/com/yourname/voicetodo/data/local/CategoryEntity.kt`

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,              // "Work", "Life", "Study"
    val displayName: String,       // User-friendly name
    val color: String = "#137fec", // Hex color for UI
    val icon: String? = null,      // Optional emoji or icon name
    val sortOrder: Int = 0,        // Display order
    val isDefault: Boolean = false,// System default categories
    val createdAt: Long = System.currentTimeMillis()
)
```

#### 2.2.2 Update TodoEntity

**Modify File:** `app/src/main/java/com/yourname/voicetodo/data/local/TodoEntity.kt`

```kotlin
@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,       // CHANGE: Will be markdown formatted
    val categoryId: String,        // NEW: Foreign key to CategoryEntity
    val status: String,            // RENAME: from 'section' to 'status' (TodoStatus enum)
    val createdAt: Long,
    val completedAt: Long?,
    val reminderTime: Long?,
    val subtasks: String? = null   // NEW: JSON array of subtask objects
)
```

#### 2.2.3 Rename TodoSection to TodoStatus

**Rename File:** `TodoSection.kt` → `TodoStatus.kt`

```kotlin
enum class TodoStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    DO_LATER
}
```

#### 2.2.4 Update Domain Model

**Modify File:** `app/src/main/java/com/yourname/voicetodo/domain/model/Todo.kt`

```kotlin
data class Todo(
    val id: String,
    val title: String,
    val description: String? = null,  // Markdown content
    val categoryId: String,            // NEW
    val status: TodoStatus = TodoStatus.TODO,  // RENAME from 'section'
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null,
    val subtasks: List<Subtask> = emptyList()  // NEW
)

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val completed: Boolean = false
)
```

**New Domain Model File:** `app/src/main/java/com/yourname/voicetodo/domain/model/Category.kt`

```kotlin
data class Category(
    val id: String,
    val name: String,
    val displayName: String,
    val color: String = "#137fec",
    val icon: String? = null,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val todoCount: Int = 0  // Computed property
)
```

### 2.3 Database Migration

**⚠️ ALPHA VERSION - NO MIGRATION NEEDED**

Since the app is in alpha/MVP stage with no production users:

1. **Update database schema** with new `CategoryEntity` and modified `TodoEntity`
2. **Bump database version** number in `TodoDatabase.kt`
3. **Add `.fallbackToDestructiveMigration()`** to database builder
4. **Users must clear app data** before installing new version

**Database Builder Configuration:**

```kotlin
@Database(
    entities = [TodoEntity::class, CategoryEntity::class, MessageEntity::class, ChatSessionEntity::class],
    version = 2,  // Bump version
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {
    // DAOs...
    
    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                .fallbackToDestructiveMigration()  // ← Alpha: Just recreate DB
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert default categories on first run
                        db.execSQL("""
                            INSERT INTO categories (id, name, displayName, color, sortOrder, isDefault, createdAt)
                            VALUES 
                                ('work', 'WORK', 'Work', '#137fec', 0, 1, ${System.currentTimeMillis()}),
                                ('life', 'LIFE', 'Life', '#4caf50', 1, 1, ${System.currentTimeMillis()}),
                                ('study', 'STUDY', 'Study', '#ff9800', 2, 1, ${System.currentTimeMillis()})
                        """)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**For Beta/Production:** Implement proper migration with data preservation.

### 2.4 DAO Updates

**Create New DAO:** `app/src/main/java/com/yourname/voicetodo/data/local/CategoryDao.kt`

```kotlin
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?
    
    @Query("SELECT * FROM categories WHERE isDefault = 1")
    fun getDefaultCategories(): Flow<List<CategoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE id = :id AND isDefault = 0")
    suspend fun deleteCategoryById(id: String)
}
```

**Update TodoDao:** `app/src/main/java/com/yourname/voicetodo/data/local/TodoDao.kt`

```kotlin
@Dao
interface TodoDao {
    @Query("SELECT * FROM todos ORDER BY createdAt DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>
    
    // NEW: Filter by category
    @Query("SELECT * FROM todos WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getTodosByCategory(categoryId: String): Flow<List<TodoEntity>>
    
    // UPDATED: Rename from getTodosBySection
    @Query("SELECT * FROM todos WHERE status = :status ORDER BY createdAt DESC")
    fun getTodosByStatus(status: String): Flow<List<TodoEntity>>
    
    // NEW: Filter by category AND status
    @Query("SELECT * FROM todos WHERE categoryId = :categoryId AND status = :status ORDER BY createdAt DESC")
    fun getTodosByCategoryAndStatus(categoryId: String, status: String): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: String): TodoEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)
    
    @Update
    suspend fun updateTodo(todo: TodoEntity)
    
    @Query("UPDATE todos SET status = :status WHERE id = :id")
    suspend fun updateTodoStatus(id: String, status: String)
    
    // NEW: Update category
    @Query("UPDATE todos SET categoryId = :categoryId WHERE id = :id")
    suspend fun updateTodoCategory(id: String, categoryId: String)
    
    @Delete
    suspend fun deleteTodo(todo: TodoEntity)
    
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: String)
}
```

### 2.5 Repository Updates

**Create New Repository:** `app/src/main/java/com/yourname/voicetodo/data/repository/CategoryRepository.kt`

```kotlin
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
        .map { entities -> entities.map { it.toDomainModel() } }
    
    fun getDefaultCategories(): Flow<List<Category>> = categoryDao.getDefaultCategories()
        .map { entities -> entities.map { it.toDomainModel() } }
    
    suspend fun getCategoryById(id: String): Category? =
        categoryDao.getCategoryById(id)?.toDomainModel()
    
    suspend fun createCategory(name: String, displayName: String, color: String, icon: String?): Category {
        val entity = CategoryEntity(
            name = name.uppercase(),
            displayName = displayName,
            color = color,
            icon = icon
        )
        categoryDao.insertCategory(entity)
        return entity.toDomainModel()
    }
    
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }
    
    suspend fun deleteCategory(id: String) {
        if (categoryDao.getCategoryById(id)?.isDefault == false) {
            categoryDao.deleteCategoryById(id)
        } else {
            throw IllegalArgumentException("Cannot delete default category")
        }
    }
}
```

**Update TodoRepository:** `app/src/main/java/com/yourname/voicetodo/data/repository/TodoRepository.kt`

```kotlin
class TodoRepository @Inject constructor(
    private val todoDao: TodoDao
) {
    // UPDATED: Add categoryId parameter
    suspend fun addTodo(
        title: String,
        description: String? = null,
        categoryId: String,
        status: TodoStatus = TodoStatus.TODO
    ): Todo {
        val todo = TodoEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description ?: "",
            categoryId = categoryId,
            status = status.name,
            createdAt = System.currentTimeMillis(),
            completedAt = null,
            reminderTime = null
        )
        todoDao.insertTodo(todo)
        return todo.toDomainModel()
    }
    
    // NEW methods
    fun getTodosByCategory(categoryId: String): Flow<List<Todo>> =
        todoDao.getTodosByCategory(categoryId).map { it.map { entity -> entity.toDomainModel() } }
    
    fun getTodosByCategoryAndStatus(categoryId: String, status: TodoStatus): Flow<List<Todo>> =
        todoDao.getTodosByCategoryAndStatus(categoryId, status.name)
            .map { it.map { entity -> entity.toDomainModel() } }
    
    suspend fun updateTodoCategory(todoId: String, categoryId: String) {
        todoDao.updateTodoCategory(todoId, categoryId)
    }
    
    // Existing methods with status rename
    fun getTodosByStatus(status: TodoStatus): Flow<List<Todo>> =
        todoDao.getTodosByStatus(status.name).map { it.map { entity -> entity.toDomainModel() } }
    
    suspend fun updateTodoStatus(todoId: String, status: TodoStatus) {
        todoDao.updateTodoStatus(todoId, status.name)
    }
    
    // ... other existing methods
}
```

---

## 3. UI Redesign Specifications

### 3.1 Chat Screen Redesign

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatScreen.kt`

#### Key Changes:
1. **Add Category Dropdown** in top bar after "AI Assistant" title
2. **Update input bar** styling to match mockup (rounded, integrated mic button)
3. **Improve message bubbles** styling

#### New Components:

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/components/CategoryDropdown.kt`
```kotlin
@Composable
fun CategoryDropdown(
    selectedCategoryId: String?,  // null = "All"
    categories: List<Category>,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // Dropdown trigger button showing current selection
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedCategoryId == null) {
                    "All Sections"
                } else {
                    categories.find { it.id == selectedCategoryId }?.displayName ?: "All Sections"
                }
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        
        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All Sections") },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { 
                        Row {
                            if (category.icon != null) {
                                Text("${category.icon} ")
                            }
                            Text(category.displayName)
                        }
                    },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

#### Updated ChatScreen Layout:
```kotlin
@Composable
fun ChatScreen(
    sessionId: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with category dropdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI Assistant", style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = { /* settings */ }) {
                    Icon(Icons.Default.Settings, null)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category selection dropdown
            CategoryDropdown(
                selectedCategoryId = selectedCategoryId,
                categories = categories,
                onCategorySelected = { viewModel.setSelectedCategory(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            items(messages) { message ->
                // Message bubbles
            }
        }
        
        // Typing indicator with animation
        AnimatedVisibility(visible = isProcessing) {
            TypingIndicator()
        }
        
        // Input bar (redesigned)
        ChatInputBar(
            textInput = textInput,
            onTextChange = { textInput = it },
            onSendClick = { viewModel.sendTextMessage(it) },
            onMicClick = { viewModel.toggleRecording() },
            isRecording = isRecording,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
```

**New Component:** `ChatInputBar.kt`
```kotlin
@Composable
fun ChatInputBar(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSendClick: (String) -> Unit,
    onMicClick: () -> Unit,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input field
            BasicTextField(
                value = textInput,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (textInput.isEmpty()) {
                        Text(
                            "Add a task or ask a question...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            
            // Mic button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint = if (isRecording) MaterialTheme.colorScheme.error 
                           else MaterialTheme.colorScheme.primary
                )
            }
            
            // Send button
            IconButton(
                onClick = { if (textInput.isNotBlank()) onSendClick(textInput) },
                enabled = textInput.isNotBlank(),
                modifier = Modifier.size(40.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (textInput.isNotBlank()) 
                            MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = if (textInput.isNotBlank()) 
                               MaterialTheme.colorScheme.onPrimary 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
```

### 3.2 Chat History Screen (NEW)

**New File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatHistoryScreen.kt`

```kotlin
@Composable
fun ChatHistoryScreen(
    navController: NavHostController,
    viewModel: ChatHistoryViewModel = hiltViewModel()
) {
    val sessions by viewModel.chatSessions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat History") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Session list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    ChatSessionItem(
                        session = session,
                        onClick = { navController.navigate("chat/${session.id}") }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatSessionItem(
    session: ChatSession,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Session info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title ?: "Untitled chat",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = session.lastMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Timestamp
            Text(
                text = formatTimestamp(session.lastMessageTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 3.3 Dashboard (Todo List) Redesign

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/TodoListScreen.kt`

#### Key Changes:
1. **Accordion-style category sections** (Work/Life/Study can expand/collapse)
2. **Status tabs within each category** (To-Do/In Progress/Done/Later)
3. **Replace AI FAB with expandable Plus FAB**
4. **3-dot menu on each todo** for moving between categories

#### New Layout:

```kotlin
@Composable
fun TodoListScreen(
    navController: NavHostController,
    viewModel: TodoListViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val todosByCategory by viewModel.todosByCategory.collectAsState()
    val expandedCategories by viewModel.expandedCategories.collectAsState()
    val selectedStatusByCategory by viewModel.selectedStatusByCategory.collectAsState()
    val isFabExpanded by viewModel.isFabExpanded.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { /* menu */ }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* profile */ }) {
                        Icon(Icons.Default.AccountCircle, null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExpandableFab(
                expanded = isFabExpanded,
                onExpandedChange = { viewModel.toggleFabExpanded() },
                onNewTodoClick = { viewModel.showNewTodoDialog() },
                onNewCategoryClick = { viewModel.showNewCategoryDialog() },
                onNewChatClick = { navController.navigate("chat/new") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                CategoryAccordion(
                    category = category,
                    todos = todosByCategory[category.id] ?: emptyMap(),
                    isExpanded = expandedCategories.contains(category.id),
                    selectedStatus = selectedStatusByCategory[category.id] ?: TodoStatus.TODO,
                    onExpandChange = { viewModel.toggleCategoryExpanded(category.id) },
                    onStatusChange = { status -> viewModel.selectStatusForCategory(category.id, status) },
                    onTodoClick = { todo -> navController.navigate("todo/${todo.id}") },
                    onMoveToCategory = { todo, targetCategoryId ->
                        viewModel.moveTodoToCategory(todo.id, targetCategoryId)
                    }
                )
            }
        }
    }
}
```

**New Component:** `CategoryAccordion.kt`

```kotlin
@Composable
fun CategoryAccordion(
    category: Category,
    categories: List<Category>,  // All categories for move menu
    todos: Map<TodoStatus, List<Todo>>,  // Todos grouped by status
    isExpanded: Boolean,
    selectedStatus: TodoStatus,
    onExpandChange: () -> Unit,
    onStatusChange: (TodoStatus) -> Unit,
    onTodoClick: (Todo) -> Unit,
    onMoveToCategory: (Todo, String) -> Unit,
    onDeleteTodo: (Todo) -> Unit,
    onDeleteCategory: () -> Unit
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.3f)),
        color = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.05f)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onExpandChange),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${category.todoCount} active tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
                
                // 3-dot menu for category
                Box {
                    IconButton(onClick = { showCategoryMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Category options")
                    }
                    
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        if (!category.isDefault) {  // Can't delete default categories
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete category",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showDeleteCategoryDialog = true
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Delete category confirmation
            if (showDeleteCategoryDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteCategoryDialog = false },
                    title = { Text("Delete ${category.displayName}?") },
                    text = { 
                        Text(
                            "This will permanently delete ${category.todoCount} todos in this category. " +
                            "This action cannot be undone."
                        ) 
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteCategory()
                                showDeleteCategoryDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteCategoryDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Expanded content
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    // Status tabs
                    ScrollableTabRow(
                        selectedTabIndex = TodoStatus.values().indexOf(selectedStatus),
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp
                    ) {
                        TodoStatus.values().forEach { status ->
                            Tab(
                                selected = selectedStatus == status,
                                onClick = { onStatusChange(status) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(getStatusDisplayName(status))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${todos[status]?.size ?: 0}",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier
                                                .background(
                                                    if (selectedStatus == status) 
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                    else Color.Transparent,
                                                    CircleShape
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    
                    // Todo list for selected status
                    val statusTodos = todos[selectedStatus] ?: emptyList()
                    if (statusTodos.isEmpty()) {
                        EmptyStatusMessage(status = selectedStatus)
                    } else {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            statusTodos.forEach { todo ->
                                TodoCard(
                                    todo = todo,
                                    categories = categories,
                                    onClick = { onTodoClick(todo) },
                                    onMoveToCategory = { targetId -> onMoveToCategory(todo, targetId) },
                                    onDelete = { onDeleteTodo(todo) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**New Component:** `ExpandableFab.kt`

```kotlin
@Composable
fun ExpandableFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNewTodoClick: () -> Unit,
    onNewCategoryClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mini FABs (visible when expanded)
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniFab(
                    icon = Icons.Default.Add,
                    label = "New Todo",
                    onClick = {
                        onNewTodoClick()
                        onExpandedChange(false)
                    }
                )
                MiniFab(
                    icon = Icons.Default.CreateNewFolder,
                    label = "New Section",
                    onClick = {
                        onNewCategoryClick()
                        onExpandedChange(false)
                    }
                )
                MiniFab(
                    icon = Icons.Default.Chat,
                    label = "New Chat",
                    onClick = {
                        onNewChatClick()
                        onExpandedChange(false)
                    }
                )
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Close" else "Add",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun MiniFab(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
        
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}
```

**New Component:** `TodoCard.kt` (with 3-dot menu)

```kotlin
@Composable
fun TodoCard(
    todo: Todo,
    categories: List<Category>,
    onClick: () -> Unit,
    onMoveToCategory: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
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
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                todo.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                if (todo.reminderTime != null) {
                    Text(
                        text = "Due: ${formatTimestamp(todo.reminderTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Move to section
                    Text(
                        "Move to...",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    categories.filter { it.id != todo.categoryId }.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row {
                                    category.icon?.let { Text("$it ") }
                                    Text(category.displayName)
                                }
                            },
                            onClick = {
                                onMoveToCategory(category.id)  // No confirmation - instant move
                                showMenu = false
                            }
                        )
                    }
                    
                    Divider()
                    
                    // Delete option
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showDeleteDialog = true
                            showMenu = false
                        }
                    )
                }
            }
        }
        
        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Todo?") },
                text = { Text("Are you sure you want to delete \"${todo.title}\"? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            onDelete()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
```

### 3.4 Task Details Screen with Markdown Editor

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/TodoDetailsScreen.kt`

#### Key Changes:
1. **Replace plain text description with markdown-capable editor**
2. **Add keyboard toolbar** (always visible) with formatting buttons (checkbox, bold, italic, list)
3. **Status toggle bar** at bottom (To-Do/In Progress/Done/To Later)
4. **Render markdown content** with checkboxes for subtasks
5. **Title editing** via 3-dot menu (not inline)
6. **Markdown is for subtasks and notes only** - title stays separate

```kotlin
@Composable
fun TodoDetailsScreen(
    todoId: String,
    navController: NavHostController,
    viewModel: TodoDetailsViewModel = hiltViewModel()
) {
    val todo by viewModel.todo.collectAsState()
    val category by viewModel.category.collectAsState()
    val isEditing by viewModel.isEditing.collectAsState()
    val markdownContent by viewModel.markdownContent.collectAsState()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit title") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showEditTitleDialog = true
                                    showOptionsMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            TodoStatusBar(
                currentStatus = todo?.status ?: TodoStatus.TODO,
                onStatusChange = { viewModel.updateStatus(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category tag
            category?.let { cat ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(android.graphics.Color.parseColor(cat.color)).copy(alpha = 0.2f),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Label,
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(cat.color)),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sector: ${cat.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(android.graphics.Color.parseColor(cat.color))
                        )
                    }
                }
            }
            
            // AI update indicator
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI updated 5 mins ago. Saved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Markdown editor/viewer
            if (isEditing) {
                MarkdownEditor(
                    content = markdownContent,
                    onContentChange = { viewModel.updateMarkdownContent(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
            } else {
                MarkdownRenderer(
                    content = markdownContent,
                    onCheckboxToggle = { index, checked ->
                        viewModel.toggleSubtask(index, checked)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
            
            // Edit/Save button
            Button(
                onClick = {
                    if (isEditing) viewModel.saveMarkdown() else viewModel.startEditing()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(if (isEditing) "Save" else "Edit")
            }
        }
        
        // Edit title dialog
        if (showEditTitleDialog) {
            var titleText by remember { mutableStateOf(todo?.title ?: "") }
            AlertDialog(
                onDismissRequest = { showEditTitleDialog = false },
                title = { Text("Edit Title") },
                text = {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Todo title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateTitle(titleText)
                            showEditTitleDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTitleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
```

**New Component:** `MarkdownEditor.kt` (with keyboard toolbar)

```kotlin
@Composable
fun MarkdownEditor(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(modifier = modifier) {
        // Markdown toolbar
        MarkdownToolbar(
            onInsertCheckbox = {
                val newContent = "$content\n- [ ] "
                onContentChange(newContent)
            },
            onInsertBold = {
                val newContent = "$content**bold text**"
                onContentChange(newContent)
            },
            onInsertItalic = {
                val newContent = "$content*italic text*"
                onContentChange(newContent)
            },
            onInsertList = {
                val newContent = "$content\n- "
                onContentChange(newContent)
            },
            onInsertHeading = {
                val newContent = "$content\n## Heading"
                onContentChange(newContent)
            }
        )
        
        // Text editor
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (content.isEmpty()) {
                        Text(
                            "Add task details, checklists, notes...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
fun MarkdownToolbar(
    onInsertCheckbox: () -> Unit,
    onInsertBold: () -> Unit,
    onInsertItalic: () -> Unit,
    onInsertList: () -> Unit,
    onInsertHeading: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolbarButton(icon = Icons.Default.CheckBox, tooltip = "Checkbox", onClick = onInsertCheckbox)
            ToolbarButton(icon = Icons.Default.FormatBold, tooltip = "Bold", onClick = onInsertBold)
            ToolbarButton(icon = Icons.Default.FormatItalic, tooltip = "Italic", onClick = onInsertItalic)
            ToolbarButton(icon = Icons.Default.FormatListBulleted, tooltip = "List", onClick = onInsertList)
            ToolbarButton(icon = Icons.Default.Title, tooltip = "Heading", onClick = onInsertHeading)
        }
    }
}

@Composable
fun ToolbarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Icon(
            icon, 
            contentDescription = label, 
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}
```

**New Component:** `MarkdownRenderer.kt`

```kotlin
@Composable
fun MarkdownRenderer(
    content: String,
    onCheckboxToggle: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedLines = remember(content) { parseMarkdown(content) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        parsedLines.forEachIndexed { index, line ->
            when (line) {
                is MarkdownLine.Heading -> {
                    Text(
                        text = line.text,
                        style = when (line.level) {
                            1 -> MaterialTheme.typography.headlineMedium
                            2 -> MaterialTheme.typography.headlineSmall
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
                is MarkdownLine.Checkbox -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = line.checked,
                            onCheckedChange = { onCheckboxToggle(index, it) }
                        )
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (line.checked) TextDecoration.LineThrough else null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                is MarkdownLine.ListItem -> {
                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyLarge)
                        Text(line.text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is MarkdownLine.Plain -> {
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// Markdown parsing utility
sealed class MarkdownLine {
    data class Heading(val level: Int, val text: String) : MarkdownLine()
    data class Checkbox(val checked: Boolean, val text: String) : MarkdownLine()
    data class ListItem(val text: String) : MarkdownLine()
    data class Plain(val text: String) : MarkdownLine()
}

fun parseMarkdown(content: String): List<MarkdownLine> {
    return content.lines().mapNotNull { line ->
        when {
            line.startsWith("# ") -> MarkdownLine.Heading(1, line.removePrefix("# "))
            line.startsWith("## ") -> MarkdownLine.Heading(2, line.removePrefix("## "))
            line.startsWith("### ") -> MarkdownLine.Heading(3, line.removePrefix("### "))
            line.trim().startsWith("- [ ]") -> MarkdownLine.Checkbox(false, line.trim().removePrefix("- [ ]").trim())
            line.trim().startsWith("- [x]") || line.trim().startsWith("- [X]") -> 
                MarkdownLine.Checkbox(true, line.trim().removePrefix("- [x]").removePrefix("- [X]").trim())
            line.trim().startsWith("- ") -> MarkdownLine.ListItem(line.trim().removePrefix("- "))
            line.isNotBlank() -> MarkdownLine.Plain(line)
            else -> null
        }
    }
}
```

**New Component:** `TodoStatusBar.kt`

```kotlin
@Composable
fun TodoStatusBar(
    currentStatus: TodoStatus,
    onStatusChange: (TodoStatus) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TodoStatus.values().forEach { status ->
                StatusButton(
                    status = status,
                    isSelected = currentStatus == status,
                    onClick = { onStatusChange(status) }
                )
            }
        }
    }
}

@Composable
fun StatusButton(
    status: TodoStatus,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
    ) {
        Text(
            text = getStatusDisplayName(status),
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getStatusDisplayName(status: TodoStatus): String {
    return when (status) {
        TodoStatus.TODO -> "To-Do"
        TodoStatus.IN_PROGRESS -> "In Progress"
        TodoStatus.DONE -> "Done"
        TodoStatus.DO_LATER -> "To Later"
    }
}
```

---

## 4. AI Tool Call Updates

### 4.1 How Tool Error Handling Works

**Based on Koog Framework Documentation:**

When a tool executes, it returns a `String` result that gets sent back to the AI. The framework handles errors automatically:

#### Success Flow:
```kotlin
@Tool
suspend fun addTodo(...): String {
    try {
        val todo = repository.addTodo(...)
        return """
        Tool: addTodo
        Status: SUCCESS
        Result: Added todo "${todo.title}" to ${categoryName}
        Todo ID: ${todo.id}
        """.trimIndent()
    } catch (e: Exception) {
        return """
        Tool: addTodo
        Status: FAILED
        Error: ${e.message}
        Suggestion: Please check that the categoryId exists and try again
        """.trimIndent()
    }
}
```

#### What Happens:
1. **AI makes tool call** → `ToolExecutionStartingEvent` fired
2. **Tool executes** → Returns `String` result (success or error message)
3. **Result sent to AI** → `ToolExecutionCompletedEvent` with the result string
4. **AI reads result** → Sees "SUCCESS" or "FAILED" and responds accordingly
5. **AI can self-correct** → If it used wrong ID, it will apologize and ask for correct info

#### If Tool Throws Uncaught Exception:
- Framework catches it → `ToolExecutionFailedEvent` fired
- Error automatically sent to AI as tool result
- AI sees the failure and responds

**Key Insight:** Structured error messages help AI understand what went wrong and how to fix it.

### 4.2 New Tool Calls Required

**File:** `app/src/main/java/com/yourname/voicetodo/ai/tools/CategoryTools.kt`

```kotlin
@LLMDescription("Manage todo categories (Work, Life, Study, etc.)")
@Singleton
class CategoryTools @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val permissionManager: ToolPermissionManager,
    private val retryableToolExecutor: RetryableToolExecutor
) : ToolSet {

    @Tool
    @LLMDescription("Create a new category")
    suspend fun createCategory(
        @LLMDescription("Category name (e.g., 'Work', 'Personal')") name: String,
        @LLMDescription("Display name") displayName: String = name,
        @LLMDescription("Hex color code") color: String = "#137fec",
        @LLMDescription("Optional icon emoji") icon: String? = null
    ): String {
        return try {
            val category = categoryRepository.createCategory(name, displayName, color, icon)
            "✅ Created category: ${category.displayName}"
        } catch (e: Exception) {
            "❌ Failed to create category: ${e.message}"
        }
    }
    
    @Tool
    @LLMDescription("List all categories")
    suspend fun listCategories(): String {
        return try {
            val categories = categoryRepository.getAllCategories().first()
            if (categories.isEmpty()) {
                "📁 No categories found"
            } else {
                val list = categories.joinToString("\n") { category ->
                    "${category.icon ?: "📁"} ${category.displayName} [${category.id}] - ${category.todoCount} todos"
                }
                "📋 Categories:\n$list"
            }
        } catch (e: Exception) {
            "❌ Failed to list categories: ${e.message}"
        }
    }
    
    @Tool
    @LLMDescription("Delete a category (cannot delete default categories)")
    suspend fun deleteCategory(
        @LLMDescription("Category ID") categoryId: String
    ): String {
        return try {
            categoryRepository.deleteCategory(categoryId)
            "✅ Deleted category"
        } catch (e: IllegalArgumentException) {
            "❌ Cannot delete default category"
        } catch (e: Exception) {
            "❌ Failed to delete category: ${e.message}"
        }
    }
}
```

### 4.2 Update Existing TodoTools

**File:** `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`

Key changes:
1. **Update `addTodo`** to require `categoryId` parameter (✅ **description already exists as optional param**)
2. **Rename `editDescription` → `updateTodoContent`** with strong prompting to prevent unwanted changes
3. **Update `listTodos`** to support filtering by both category and status
4. **Add `moveTodoToCategory`** method
5. **Remove `createSection`** (replaced by CategoryTools.createCategory)

```kotlin
@Tool
@LLMDescription("Add a new todo item")
suspend fun addTodo(
    @LLMDescription("Title of the todo") title: String,
    @LLMDescription("Optional markdown description with subtasks") description: String? = null,  // ✅ Already exists!
    @LLMDescription("Category ID (use listCategories to see available categories)") categoryId: String,
    @LLMDescription("Status: todo, in_progress, done, do_later") status: String = "todo"
): String {
    return try {
        val todoStatus = try {
            TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
        } catch (e: IllegalArgumentException) {
            TodoStatus.TODO
        }
        
        val todo = repository.addTodo(
            title = title,
            description = description,
            categoryId = categoryId,
            status = todoStatus
        )
        
        """
        Tool: addTodo
        Status: SUCCESS
        Result: Added todo "${todo.title}" to ${categoryId}
        Todo ID: ${todo.id}
        """.trimIndent()
    } catch (e: Exception) {
        """
        Tool: addTodo
        Status: FAILED
        Error: ${e.message}
        Suggestion: Verify categoryId exists using listCategories
        """.trimIndent()
    }
}

@Tool
@LLMDescription("List todos with optional filtering")
suspend fun listTodos(
    @LLMDescription("Category ID to filter by, or 'all'") categoryId: String = "all",
    @LLMDescription("Status to filter by: todo, in_progress, done, do_later, or 'all'") status: String = "all"
): String {
    return try {
        val todos = when {
            categoryId == "all" && status == "all" -> {
                runBlocking { repository.getAllTodos().first() }
            }
            categoryId == "all" -> {
                val todoStatus = TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
                runBlocking { repository.getTodosByStatus(todoStatus).first() }
            }
            status == "all" -> {
                runBlocking { repository.getTodosByCategory(categoryId).first() }
            }
            else -> {
                val todoStatus = TodoStatus.valueOf(status.uppercase().replace(" ", "_"))
                runBlocking { repository.getTodosByCategoryAndStatus(categoryId, todoStatus).first() }
            }
        }
        
        if (todos.isEmpty()) {
            return "📝 No todos found"
        }
        
        val todoList = todos.joinToString("\n") { todo ->
            val statusIcon = when (todo.status) {
                TodoStatus.TODO -> "📝"
                TodoStatus.IN_PROGRESS -> "🔄"
                TodoStatus.DONE -> "✅"
                TodoStatus.DO_LATER -> "⏰"
            }
            val desc = todo.description?.let { "\n   Description: \"${it.take(50)}...\"" } ?: ""
            "$statusIcon ${todo.title} [${todo.id}] (Category: ${todo.categoryId})$desc"
        }
        
        "📋 Todos:\n$todoList"
    } catch (e: Exception) {
        "❌ Failed to list todos: ${e.message}"
    }
}

@Tool
@LLMDescription("Move a todo to a different category")
suspend fun moveTodoToCategory(
    @LLMDescription("Todo ID") todoId: String,
    @LLMDescription("Target category ID") targetCategoryId: String
): String {
    return try {
        val todo = repository.getTodoById(todoId)
        if (todo != null) {
            repository.updateTodoCategory(todoId, targetCategoryId)
            "✅ Moved todo '${todo.title}' to category $targetCategoryId"
        } else {
            "❌ Todo with ID $todoId not found"
        }
    } catch (e: Exception) {
        "❌ Failed to move todo: ${e.message}"
    }
}
```

### 4.3 Update AI Agent Configuration

**File:** `app/src/main/java/com/yourname/voicetodo/di/AIModule.kt`

Add CategoryTools to tool registry:

```kotlin
@Provides
@Singleton
fun provideToolRegistry(
    todoTools: TodoTools,
    categoryTools: CategoryTools  // NEW
): ToolRegistry {
    return ToolRegistry()
        .addToolSet(todoTools)
        .addToolSet(categoryTools)  // NEW
}
```

### 4.4 Update ViewModel for Category Selection

**File:** `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatViewModel.kt`

```kotlin
class ChatViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,  // NEW
    private val chatRepository: ChatRepository,
    private val agent: TodoAgent
) : ViewModel() {
    
    // NEW: Category selection state
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    private val _selectedCategoryId = MutableStateFlow<String?>(null)  // null = "All"
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()
    
    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { _categories.value = it }
        }
    }
    
    fun setSelectedCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        // Update agent context with selected category
        agent.setActiveCategory(categoryId)
    }
    
    // ... rest of existing methods
}
```

**Update TodoAgent to be category-aware:**

**File:** `app/src/main/java/com/yourname/voicetodo/ai/agent/TodoAgent.kt`

```kotlin
class TodoAgent @Inject constructor(
    // ... existing dependencies
) {
    private var activeCategoryId: String? = null
    
    fun setActiveCategory(categoryId: String?) {
        activeCategoryId = categoryId
    }
    
    suspend fun processMessage(userMessage: String): String {
        // Add category context to system prompt
        val systemPrompt = buildString {
            append("You are a helpful todo assistant. ")
            if (activeCategoryId != null) {
                append("Currently working in category: $activeCategoryId. ")
                append("When creating or listing todos, focus on this category unless user specifies otherwise. ")
            } else {
                append("Currently viewing all categories. ")
            }
            append("Use the available tools to help the user manage their todos.")
        }
        
        // ... rest of agent logic
    }
}
```

---

## 5. Implementation Phases

### Phase 1: Database & Data Layer (Week 1)
**Priority: CRITICAL**

1. Create `CategoryEntity` and `CategoryDao`
2. Create database migration
3. Update `TodoEntity` schema
4. Rename `TodoSection` to `TodoStatus`
5. Create `Category` domain model
6. Create `CategoryRepository`
7. Update `TodoRepository` with new methods
8. Run migration and test with sample data

**Deliverables:**
- All database changes working
- No data loss from existing todos
- Default categories (Work/Life/Study) auto-created

---

### Phase 2: Tool Calls & AI Integration (Week 2)
**Priority: HIGH**

1. Create `CategoryTools` class
2. Update `TodoTools` with:
   - `addTodo` (new categoryId param)
   - `listTodos` (category filter)
   - `moveTodoToCategory`
   - Remove `createSection`
3. Register tools in `AIModule`
4. Update `TodoAgent` with category-awareness
5. Test tool calls manually

**Deliverables:**
- AI can create/list/delete categories
- AI can add todos to specific categories
- AI respects category filter in chat

---

### Phase 3: Dashboard UI (Week 3)
**Priority: HIGH**

1. Create `CategoryAccordion` component
2. Create `ExpandableFab` component
3. Create `TodoCard` with 3-dot menu
4. Update `TodoListScreen` layout
5. Create `TodoListViewModel` with:
   - Category expansion state
   - Status selection per category
   - Todo grouping logic
6. Wire up navigation

**Deliverables:**
- Dashboard shows todos grouped by category
- Accordion expand/collapse works
- Status tabs work within each category
- FAB expands with 3 options

---

### Phase 4: Chat UI (Week 4)
**Priority: MEDIUM**

1. Create `CategoryDropdown` component
2. Create `ChatInputBar` component
3. Update `ChatScreen` layout
4. Update `ChatViewModel` with category state
5. Create `ChatHistoryScreen` (new)
6. Create `ChatHistoryViewModel`
7. Update navigation routes

**Deliverables:**
- Chat has category dropdown
- Input bar matches design mockup
- Chat history screen works
- AI respects selected category

---

### Phase 5: Task Details with Markdown (Week 5)
**Priority: MEDIUM**

1. Create `MarkdownEditor` component
2. Create `MarkdownRenderer` component
3. Create `MarkdownToolbar` component
4. Create `TodoStatusBar` component
5. Create `TodoDetailsScreen`
6. Create `TodoDetailsViewModel`
7. Implement markdown parsing logic
8. Add subtask toggle functionality

**Deliverables:**
- Can edit todos with markdown
- Markdown renders with checkboxes
- Checkboxes are interactive
- Status can be changed from details

---

### Phase 6: Polish & Testing (Week 6)
**Priority: LOW**

1. Add animations to accordion
2. Add ripple effects
3. Improve error handling
4. Add loading states
5. Optimize performance
6. Write unit tests
7. Write integration tests
8. Fix bugs from user testing

**Deliverables:**
- Smooth animations
- No crashes
- Good UX feedback
- Tests passing

---

## 6. File Changes Checklist

### New Files to Create

#### Data Layer
- [ ] `app/src/main/java/com/yourname/voicetodo/data/local/CategoryEntity.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/data/local/CategoryDao.kt`
- [ ] ~~`app/src/main/java/com/yourname/voicetodo/data/local/Migration_X_to_Y.kt`~~ (SKIPPED - Alpha version)
- [ ] `app/src/main/java/com/yourname/voicetodo/domain/model/Category.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/data/repository/CategoryRepository.kt`

#### AI Tools
- [ ] `app/src/main/java/com/yourname/voicetodo/ai/tools/CategoryTools.kt`

#### UI Components - Chat
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/components/CategoryDropdown.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/components/ChatInputBar.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatHistoryScreen.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatHistoryViewModel.kt`

#### UI Components - Dashboard
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/CategoryAccordion.kt` (with 3-dot menu + delete dialog)
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/ExpandableFab.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/TodoCard.kt` (with 3-dot menu + delete dialog)
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/EmptyStatusMessage.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/ConfirmDeleteDialog.kt`

#### UI Components - Task Details
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/TodoDetailsScreen.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/TodoDetailsViewModel.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/MarkdownEditor.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/MarkdownRenderer.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/MarkdownToolbar.kt`
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/TodoStatusBar.kt`

#### Utilities
- [ ] `app/src/main/java/com/yourname/voicetodo/util/MarkdownParser.kt`

### Files to Modify

#### Data Layer
- [ ] `app/src/main/java/com/yourname/voicetodo/data/local/TodoEntity.kt`
  - Add `categoryId` field
  - Add `subtasks` field
  - Rename `section` to `status`
  - Add foreign key constraint
- [ ] `app/src/main/java/com/yourname/voicetodo/data/local/TodoDao.kt`
  - Add category-based queries
  - Rename section methods to status methods
- [ ] `app/src/main/java/com/yourname/voicetodo/data/local/TodoDatabase.kt`
  - Add `CategoryEntity`
  - Add migration
  - Bump version number
- [ ] `app/src/main/java/com/yourname/voicetodo/domain/model/Todo.kt`
  - Add `categoryId` field
  - Add `subtasks` field
  - Rename `section` to `status`
- [ ] `app/src/main/java/com/yourname/voicetodo/data/repository/TodoRepository.kt`
  - Update `addTodo` signature
  - Add category methods
  - Rename section methods

#### Rename Files
- [ ] Rename `TodoSection.kt` to `TodoStatus.kt` and update enum name

#### AI Layer
- [ ] `app/src/main/java/com/yourname/voicetodo/ai/tools/TodoTools.kt`
  - Update `addTodo` with `categoryId` param (description already exists ✅)
  - Rename `editDescription` → `updateTodoContent` with strong prompting
  - Update tool responses to use structured format (Tool/Status/Result/Error)
  - Update `listTodos` with category filter
  - Add `moveTodoToCategory`
  - Remove `createSection`
- [ ] `app/src/main/java/com/yourname/voicetodo/ai/agent/TodoAgent.kt`
  - Add category context awareness
  - Update system prompt
- [ ] `app/src/main/java/com/yourname/voicetodo/di/AIModule.kt`
  - Register `CategoryTools`

#### UI Layer
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatScreen.kt`
  - Add category dropdown
  - Update input bar design
  - Add history navigation
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/chat/ChatViewModel.kt`
  - Add category state management
  - Add category selection logic
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/TodoListScreen.kt`
  - Complete redesign with accordions
  - Add expandable FAB
  - Group by categories
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/TodoListViewModel.kt`
  - Add category expansion state
  - Add status selection per category
  - Add todo grouping logic
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/screens/todos/components/TodoItem.kt`
  - Add 3-dot menu
  - Add move to category option
- [ ] `app/src/main/java/com/yourname/voicetodo/ui/navigation/NavGraph.kt`
  - Add TodoDetailsScreen route
  - Add ChatHistoryScreen route

#### DI Layer
- [ ] `app/src/main/java/com/yourname/voicetodo/di/DatabaseModule.kt`
  - Provide `CategoryDao`
- [ ] `app/src/main/java/com/yourname/voicetodo/di/RepositoryModule.kt`
  - Provide `CategoryRepository`

---

## 7. Key Architectural Decisions

### 7.1 Two-Level Hierarchy
- **Categories** (Work/Life/Study) = top-level grouping
- **Status** (TODO/IN_PROGRESS/DONE/DO_LATER) = workflow state
- Each todo belongs to ONE category and has ONE status

### 7.2 Markdown for Descriptions
- All todo descriptions stored as markdown strings
- Renderer parses markdown on-the-fly for display
- Checkboxes in markdown sync with subtask state
- Simple markdown subset supported (headings, lists, checkboxes)

### 7.3 Category-Aware AI
- Chat interface has category filter dropdown
- AI system prompt includes active category context
- Tool calls default to active category when applicable
- "All" category shows cross-category view

### 7.4 Database Migration Strategy
- Default categories created on first run
- Existing todos assigned to "Life" category by default
- Section → Status rename handled via table recreation
- Foreign key constraints maintain data integrity

---

## 8. Testing Strategy

### 8.1 Unit Tests
- CategoryRepository CRUD operations
- TodoRepository with category filtering
- Markdown parsing logic
- ViewModel state management

### 8.2 Integration Tests
- Database migration from old to new schema
- Tool call execution with categories
- AI agent category awareness

### 8.3 UI Tests
- Category accordion expand/collapse
- Status tab switching
- FAB expansion
- Markdown editor/renderer
- Category dropdown selection

---

## 9. Decisions Made ✅

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Max categories | ∞ Unlimited | User flexibility, no artificial limit |
| Category reordering | ✅ Yes, drag handle | Better UX, user control |
| Delete category | ⚠️ CASCADE delete all todos | Simple for MVP, with confirmation dialog |
| Markdown parser | ✅ Custom parser | Lightweight, no dependencies, full control |
| Keyboard toolbar | ✅ Always visible (part of UI) | Not keyboard overlay, always accessible |
| Database migration | ❌ Skip for alpha | Manual clear app data, add for beta |
| Title editing | ✅ 3-dot menu | Keeps markdown for subtasks/notes only |
| Delete confirmations | ✅ Yes for todos & categories | Prevent accidental deletion |
| Move confirmation | ❌ No | Instant move, user can undo easily |
| Empty state | ✅ Show icon + message | Better UX feedback |
| Tool error handling | ✅ Structured error strings | AI can self-correct |
| `addTodo` description | ✅ Already exists | No changes needed |
| `editDescription` rename | ✅ → `updateTodoContent` | More accurate name |

---

## 10. Risk & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Database migration fails on user devices | HIGH | LOW | Extensive testing, backup mechanism |
| Markdown rendering performance issues | MEDIUM | MEDIUM | Lazy rendering, pagination |
| AI tools don't respect category context | MEDIUM | LOW | Thorough prompt engineering, testing |
| Complex accordion state management | LOW | MEDIUM | Use simple expand/collapse boolean map |
| Markdown editor UX confusing | MEDIUM | MEDIUM | Add help text, examples, tutorial |

---

## 11. Future Enhancements (Post-MVP)

1. **Rich markdown support** (images, links, tables)
2. **Category templates** (preset categories with icons/colors)
3. **Smart category suggestions** (AI suggests category for new todo)
4. **Category analytics** (time spent, completion rate per category)
5. **Subtask extraction** (AI converts markdown checklist to subtasks)
6. **Drag-and-drop** todo reordering
7. **Category sharing** (share category with others)
8. **Custom status workflows** (beyond TODO/IN_PROGRESS/DONE/DO_LATER)

---

## 12. Implementation Notes

### Key Points for Developer:
1. **No migration needed** - Clear app data before running new version
2. **Tool errors automatically handled** - Just return structured error strings
3. **Confirmation dialogs** - Required for all delete operations
4. **No confirmation** - Moving todos between categories is instant
5. **Markdown toolbar** - Always visible, not keyboard overlay
6. **Title editing** - Via 3-dot menu, not inline in markdown
7. **Strong prompting** - Critical for `updateTodoContent` to prevent unwanted changes

### Alpha Testing Checklist:
- [ ] Clear app data before installation
- [ ] Test category creation (Work/Life/Study auto-created)
- [ ] Test AI category-aware queries ("add todo to work")
- [ ] Test markdown editing with toolbar
- [ ] Test checkbox toggling in rendered markdown
- [ ] Test 3-dot menus (todo and category)
- [ ] Test delete confirmations
- [ ] Test accordion expand/collapse
- [ ] Test FAB expansion
- [ ] Test status tabs within categories
- [ ] Test AI tool error handling (wrong IDs, etc.)

## 13. Conclusion

This plan provides a comprehensive roadmap for redesigning the Voice Todo App with category management and improved UI. The phased approach allows for incremental delivery while maintaining app stability.

**Estimated Total Effort:** 6 weeks (1 developer)

**Key Success Metrics:**
- ✅ No crashes on fresh install (with cleared data)
- ✅ No regression in existing AI features
- ✅ All new UI components render smoothly (60fps)
- ✅ User can create/manage categories via AI
- ✅ Markdown editor supports all specified syntax
- ✅ Tool errors inform AI for self-correction

---

**Next Steps:**
1. ✅ Plan reviewed and approved
2. Begin Phase 1: Database & Data Layer (Week 1)
3. Implement Phase 2: Tool Calls & AI Integration (Week 2)
4. Continue through Phase 6 per schedule
