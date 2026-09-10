package com.luciansvon.anisachat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luciansvon.anisachat.chat.ChatOrchestrator
import com.luciansvon.anisachat.chat.DeterministicRouter
import com.luciansvon.anisachat.chat.SystemContextBuilder
import com.luciansvon.anisachat.data.RoomConversationStateStore
import com.luciansvon.anisachat.data.local.AnisaDatabase
import com.luciansvon.anisachat.domain.ChatMessage
import com.luciansvon.anisachat.domain.MessageRole
import com.luciansvon.anisachat.emotion.EmotionEngine
import com.luciansvon.anisachat.inference.DevelopmentInferenceRuntime
import com.luciansvon.anisachat.inference.ModelSessionManager
import com.luciansvon.anisachat.memory.RoomMemoryRepository
import com.luciansvon.anisachat.time.TimeContextEngine
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val debugEvent: String? = null,
)

class ChatViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val database = AnisaDatabase.open(application)
    private val store = RoomConversationStateStore(database.dao())
    private val memoryRepository = RoomMemoryRepository(database.dao())
    private val runtime = DevelopmentInferenceRuntime()
    private val session = ModelSessionManager(
        runtime = runtime,
        scope = viewModelScope,
    )
    private val orchestrator = ChatOrchestrator(
        store = store,
        memoryRepository = memoryRepository,
        timeEngine = TimeContextEngine(),
        emotionEngine = EmotionEngine(),
        deterministicRouter = DeterministicRouter(),
        contextBuilder = SystemContextBuilder(),
        modelSession = session,
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { store.recentMessages(UI_HISTORY_LIMIT) }
                .onSuccess { messages ->
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            debugEvent = if (messages.isEmpty()) null else "storage:restored",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(debugEvent = "storage-error:${error::class.simpleName}")
                    }
                }
        }
    }

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun send() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isGenerating) return

        _uiState.update {
            it.copy(
                input = "",
                isGenerating = true,
                messages = it.messages + ChatMessage(
                    role = MessageRole.USER,
                    content = text,
                    createdAt = Instant.now(),
                ),
            )
        }

        viewModelScope.launch {
            runCatching { orchestrator.send(text) }
                .onSuccess { result ->
                    _uiState.update {
                        val route = if (result.usedModel) "model" else "script"
                        it.copy(
                            isGenerating = false,
                            debugEvent = "$route:${result.event::class.simpleName}",
                            messages = it.messages + ChatMessage(
                                role = MessageRole.ASSISTANT,
                                content = result.reply,
                                createdAt = Instant.now(),
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            debugEvent = "error:${error::class.simpleName}",
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        viewModelScope.launch {
            session.unloadNow()
        }
        super.onCleared()
    }

    private companion object {
        const val UI_HISTORY_LIMIT = 100
    }
}
