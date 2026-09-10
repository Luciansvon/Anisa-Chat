package com.luciansvon.anisachat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luciansvon.anisachat.chat.ChatOrchestrator
import com.luciansvon.anisachat.chat.SystemContextBuilder
import com.luciansvon.anisachat.data.InMemoryConversationStateStore
import com.luciansvon.anisachat.domain.ChatMessage
import com.luciansvon.anisachat.domain.MessageRole
import com.luciansvon.anisachat.emotion.EmotionEngine
import com.luciansvon.anisachat.inference.DevelopmentInferenceRuntime
import com.luciansvon.anisachat.inference.ModelSessionManager
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

class ChatViewModel : ViewModel() {
    private val runtime = DevelopmentInferenceRuntime()
    private val session = ModelSessionManager(
        runtime = runtime,
        scope = viewModelScope,
    )
    private val orchestrator = ChatOrchestrator(
        store = InMemoryConversationStateStore(),
        timeEngine = TimeContextEngine(),
        emotionEngine = EmotionEngine(),
        contextBuilder = SystemContextBuilder(),
        modelSession = session,
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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
                        it.copy(
                            isGenerating = false,
                            debugEvent = result.event::class.simpleName,
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
}
