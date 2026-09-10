package com.luciansvon.anisachat.data

import com.luciansvon.anisachat.domain.ChatMessage
import com.luciansvon.anisachat.domain.ConversationState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ConversationStateStore {
    suspend fun read(): ConversationState
    suspend fun write(state: ConversationState)

    suspend fun recentMessages(limit: Int = 100): List<ChatMessage> {
        if (limit <= 0) return emptyList()
        return read().recentMessages.takeLast(limit)
    }
}

class InMemoryConversationStateStore(
    initial: ConversationState = ConversationState(),
) : ConversationStateStore {
    private val mutex = Mutex()
    private var state = initial

    override suspend fun read(): ConversationState = mutex.withLock { state }

    override suspend fun write(state: ConversationState) {
        mutex.withLock {
            this.state = state
        }
    }
}
