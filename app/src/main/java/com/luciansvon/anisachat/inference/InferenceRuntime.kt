package com.luciansvon.anisachat.inference

import com.luciansvon.anisachat.domain.ChatMessage

data class InferenceRequest(
    val systemContext: String,
    val messages: List<ChatMessage>,
    val maxOutputTokens: Int = 160,
    val temperature: Float = 0.85f,
)

interface InferenceRuntime {
    val isLoaded: Boolean

    suspend fun load()

    suspend fun generate(request: InferenceRequest): String

    suspend fun unload()
}

class DevelopmentInferenceRuntime : InferenceRuntime {
    override var isLoaded: Boolean = false
        private set

    override suspend fun load() {
        isLoaded = true
    }

    override suspend fun generate(request: InferenceRequest): String {
        check(isLoaded) { "Runtime must be loaded before generate()." }
        val latest = request.messages.lastOrNull()?.content.orEmpty()
        return "[runtime belum terhubung] Aku nerima: $latest"
    }

    override suspend fun unload() {
        isLoaded = false
    }
}
