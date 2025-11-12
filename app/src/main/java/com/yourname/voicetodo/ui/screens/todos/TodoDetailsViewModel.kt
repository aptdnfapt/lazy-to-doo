package com.yourname.voicetodo.ui.screens.todos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.voicetodo.data.repository.TodoRepository
import com.yourname.voicetodo.data.repository.CategoryRepository
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoStatus
import com.yourname.voicetodo.domain.model.Category
import com.yourname.voicetodo.domain.model.Subtask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoDetailsViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val todoId: String = checkNotNull(savedStateHandle["todoId"])

    private val _todo = MutableStateFlow<Todo?>(null)
    val todo: StateFlow<Todo?> = _todo.asStateFlow()

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _markdownContent = MutableStateFlow("")
    val markdownContent: StateFlow<String> = _markdownContent.asStateFlow()

    init {
        loadTodo()
    }

    private fun loadTodo() {
        viewModelScope.launch {
            val loadedTodo = todoRepository.getTodoById(todoId)
            _todo.value = loadedTodo
            _markdownContent.value = loadedTodo?.description ?: ""

            loadedTodo?.let { todo ->
                val loadedCategory = categoryRepository.getCategoryById(todo.categoryId)
                _category.value = loadedCategory
            }
        }
    }

    fun updateStatus(status: TodoStatus) {
        val currentTodo = _todo.value ?: return
        val updatedTodo = currentTodo.copy(status = status)
        _todo.value = updatedTodo

        viewModelScope.launch {
            todoRepository.updateTodo(updatedTodo)
        }
    }

    fun updateMarkdownContent(content: String) {
        _markdownContent.value = content
    }

    fun saveMarkdown() {
        val currentTodo = _todo.value ?: return
        val updatedTodo = currentTodo.copy(description = _markdownContent.value)
        _todo.value = updatedTodo
        _isEditing.value = false

        viewModelScope.launch {
            todoRepository.updateTodo(updatedTodo)
        }
    }

    private fun persistMarkdownChange() {
        val currentTodo = _todo.value ?: return
        val updatedTodo = currentTodo.copy(description = _markdownContent.value)
        _todo.value = updatedTodo

        viewModelScope.launch {
            todoRepository.updateTodo(updatedTodo)
        }
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun toggleSubtask(index: Int, checked: Boolean) {
        val currentTodo = _todo.value ?: return
        val updatedSubtasks = currentTodo.subtasks.toMutableList()
        if (index in updatedSubtasks.indices) {
            updatedSubtasks[index] = updatedSubtasks[index].copy(completed = checked)
            val updatedTodo = currentTodo.copy(subtasks = updatedSubtasks)
            _todo.value = updatedTodo

            viewModelScope.launch {
                todoRepository.updateTodo(updatedTodo)
            }
        }
    }

    fun toggleCheckboxInMarkdown(lineIndex: Int, checked: Boolean) {
        val lines = _markdownContent.value.lines().toMutableList()
        if (lineIndex < lines.size) {
            val line = lines[lineIndex].trim()
            val newLine = if (checked) {
                if (line.startsWith("[]")) {
                    line.replaceFirst("[]", "[x]")
                } else {
                    line
                }
            } else {
                if (line.startsWith("[x]")) {
                    line.replaceFirst("[x]", "[]")
                } else if (line.startsWith("[X]")) {
                    line.replaceFirst("[X]", "[]")
                } else {
                    line
                }
            }
            lines[lineIndex] = newLine
            _markdownContent.value = lines.joinToString("\n")
            
            // Persist the checkbox change to database
            persistMarkdownChange()
        }
    }

    fun updateTitle(title: String) {
        val currentTodo = _todo.value ?: return
        val updatedTodo = currentTodo.copy(title = title)
        _todo.value = updatedTodo

        viewModelScope.launch {
            todoRepository.updateTodo(updatedTodo)
        }
    }
}