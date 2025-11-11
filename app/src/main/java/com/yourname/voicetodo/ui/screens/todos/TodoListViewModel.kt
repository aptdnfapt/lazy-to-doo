package com.yourname.voicetodo.ui.screens.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yourname.voicetodo.data.repository.TodoRepository
import com.yourname.voicetodo.data.repository.CategoryRepository
import com.yourname.voicetodo.data.repository.ChatRepository
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoStatus
import com.yourname.voicetodo.domain.model.Category
import com.yourname.voicetodo.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val todos = repository.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _showDialog = MutableStateFlow(false)
    val showDialog = _showDialog.asStateFlow()

    private val _editingTodo = MutableStateFlow<Todo?>(null)
    val editingTodo = _editingTodo.asStateFlow()

    // NEW: Category-related state
    val categories = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _expandedCategories = MutableStateFlow<Set<String>>(emptySet())
    val expandedCategories = _expandedCategories.asStateFlow()

    private val _selectedStatusByCategory = MutableStateFlow<Map<String, TodoStatus>>(emptyMap())
    val selectedStatusByCategory = _selectedStatusByCategory.asStateFlow()

    private val _isFabExpanded = MutableStateFlow(false)
    val isFabExpanded = _isFabExpanded.asStateFlow()

    private val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog = _showCategoryDialog.asStateFlow()

    // Group todos by category and status
    val todosByCategory = repository.getAllTodos()
        .combine(categories) { todos, categories ->
            categories.associate { category ->
                category.id to TodoStatus.values().associate { status ->
                    status to todos.filter { it.categoryId == category.id && it.status == status }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun showAddDialog() {
        _editingTodo.value = null
        _showDialog.value = true
    }

    fun editTodo(todo: Todo) {
        _editingTodo.value = todo
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
        _editingTodo.value = null
    }

    fun addTodo(title: String, description: String?, categoryId: String, status: TodoStatus = TodoStatus.TODO) {
        viewModelScope.launch {
            // Ensure the category exists, fallback to first available category or create work category
            val validCategoryId = if (categories.value.any { it.id == categoryId }) {
                categoryId
            } else {
                categories.value.firstOrNull()?.id ?: run {
                    // Create work category if no categories exist
                    categoryRepository.createCategory("Work", "Work", "#137fec", null)
                    "work"
                }
            }

            repository.addTodo(
                title = title,
                description = description,
                categoryId = validCategoryId,
                status = status
            )
            hideDialog()
        }
    }

    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            repository.updateTodo(todo)
            hideDialog()
        }
    }

    fun deleteTodo(todoId: String) {
        viewModelScope.launch {
            repository.deleteTodoById(todoId)
        }
    }

    fun moveTodo(todoId: String, status: TodoStatus) {
        viewModelScope.launch {
            repository.updateTodoStatus(todoId, status)
        }
    }

    // NEW: Category accordion methods
    fun toggleCategoryExpanded(categoryId: String) {
        val current = _expandedCategories.value
        _expandedCategories.value = if (current.contains(categoryId)) {
            current - categoryId
        } else {
            current + categoryId
        }
    }

    fun selectStatusForCategory(categoryId: String, status: TodoStatus) {
        val current = _selectedStatusByCategory.value.toMutableMap()
        current[categoryId] = status
        _selectedStatusByCategory.value = current
    }

    fun moveTodoToCategory(todoId: String, targetCategoryId: String) {
        viewModelScope.launch {
            repository.updateTodoCategory(todoId, targetCategoryId)
        }
    }

    // NEW: FAB methods
    fun toggleFabExpanded() {
        _isFabExpanded.value = !_isFabExpanded.value
    }

    fun showNewTodoDialog() {
        _editingTodo.value = null
        _showDialog.value = true
        _isFabExpanded.value = false
    }

    fun showNewCategoryDialog() {
        _showCategoryDialog.value = true
        _isFabExpanded.value = false
    }

    fun hideCategoryDialog() {
        _showCategoryDialog.value = false
    }

    fun createCategory(name: String, color: String) {
        viewModelScope.launch {
            categoryRepository.createCategory(name, name, color, null)
            hideCategoryDialog()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            // First delete all todos in the category
            repository.deleteTodosByCategory(categoryId)
            // Then delete the category
            categoryRepository.deleteCategory(categoryId)
        }
    }

    fun createNewChatSession(navController: NavHostController) {
        viewModelScope.launch {
            try {
                val newSession = chatRepository.createChatSession("New Chat")
                navController.navigate(Screen.Chat.createRoute(newSession.id)) {
                    popUpTo(navController.graph.startDestinationRoute ?: Screen.ChatList.route) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                // Handle error - could show toast or snackbar
                // For now, just log the error
                e.printStackTrace()
            }
        }
    }
}