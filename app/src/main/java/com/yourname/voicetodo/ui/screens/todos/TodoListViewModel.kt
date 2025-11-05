package com.yourname.voicetodo.ui.screens.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.voicetodo.data.repository.TodoRepository
import com.yourname.voicetodo.domain.model.Todo
import com.yourname.voicetodo.domain.model.TodoSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@HiltViewModel
class TodoListViewModel @Inject constructor(
    private val repository: TodoRepository
) : ViewModel() {

    val todos = repository.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _showDialog = MutableStateFlow(false)
    val showDialog = _showDialog.asStateFlow()

    private val _editingTodo = MutableStateFlow<Todo?>(null)
    val editingTodo = _editingTodo.asStateFlow()

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

    fun addTodo(title: String, description: String?, section: TodoSection) {
        viewModelScope.launch {
            repository.addTodo(
                title = title,
                description = description,
                section = section
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

    fun moveTodo(todoId: String, section: TodoSection) {
        viewModelScope.launch {
            repository.updateTodoSection(todoId, section)
        }
    }
}