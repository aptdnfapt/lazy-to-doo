package com.yourname.voicetodo.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.yourname.voicetodo.data.repository.ChatRepository
import com.yourname.voicetodo.domain.model.ChatSession
import com.yourname.voicetodo.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val chatSessions: Flow<List<ChatSession>> = chatRepository.getAllChatSessions()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredSessions: Flow<List<ChatSession>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                chatSessions
            } else {
                kotlinx.coroutines.flow.flow {
                    val searched = chatRepository.searchChatSessions(query)
                    emit(searched)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createNewChatSession(navController: NavHostController) {
        viewModelScope.launch {
            try {
                val newSession = chatRepository.createChatSession("New Chat")
                navController.navigate(Screen.Chat.createRoute(newSession.id))
            } catch (e: Exception) {
                // Handle error - could show toast or snackbar
                // For now, just log the error
                e.printStackTrace()
            }
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteChatSession(sessionId)
            } catch (e: Exception) {
                // Handle error - could show toast or snackbar
                // For now, just log the error
                e.printStackTrace()
            }
        }
    }

    fun updateChatSessionTitle(sessionId: String, title: String) {
        viewModelScope.launch {
            try {
                chatRepository.updateChatSessionTitle(sessionId, title)
            } catch (e: Exception) {
                // Handle error - could show toast or snackbar
                // For now, just log the error
                e.printStackTrace()
            }
        }
    }
}