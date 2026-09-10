package com.luciansvon.anisachat.chat

import com.luciansvon.anisachat.data.ConversationStateStore
import com.luciansvon.anisachat.domain.ChatMessage
import com.luciansvon.anisachat.domain.DefaultPersona
import com.luciansvon.anisachat.domain.MessageRole
import com.luciansvon.anisachat.domain.PersonaProfile
import com.luciansvon.anisachat.emotion.EmotionEngine
import com.luciansvon.anisachat.emotion.InteractionEvent
import com.luciansvon.anisachat.inference.InferenceRequest
import com.luciansvon.anisachat.inference.ModelSessionManager
import com.luciansvon.anisachat.time.TimeContext
import com.luciansvon.anisachat.time.TimeContextEngine

data class ChatResult(
    val reply: String,
    val timeContext: TimeContext,
    val event: InteractionEvent,
)

class ChatOrchestrator(
    private val store: ConversationStateStore,
    private val timeEngine: TimeContextEngine,
    private val emotionEngine: EmotionEngine,
    private val contextBuilder: SystemContextBuilder,
    private val modelSession: ModelSessionManager,
    private val persona: PersonaProfile = DefaultPersona.Anisa,
) {
    suspend fun send(text: String): ChatResult {
        require(text.isNotBlank()) { "Message cannot be blank." }

        val previous = store.read()
        val time = timeEngine.derive(previous.lastUserMessageAt)

        val timeEvent = emotionEngine.eventFrom(time)
        val afterTime = emotionEngine.apply(
            emotion = previous.emotion,
            relationship = previous.relationship,
            event = timeEvent,
        )

        val followUpEvent = emotionEngine.detectFollowUpEvent(
            text = text,
            relationship = afterTime.relationship,
        )

        val transition = if (followUpEvent != InteractionEvent.None) {
            emotionEngine.apply(
                emotion = afterTime.emotion,
                relationship = afterTime.relationship,
                event = followUpEvent,
            )
        } else {
            afterTime
        }

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text.trim(),
            createdAt = time.now,
        )

        val contextMessages = (previous.recentMessages + userMessage).takeLast(MAX_CONTEXT_MESSAGES)
        val systemContext = contextBuilder.build(
            persona = persona,
            emotion = transition.emotion,
            relationship = transition.relationship,
            time = time,
        )

        val reply = modelSession.generate(
            InferenceRequest(
                systemContext = systemContext,
                messages = contextMessages,
            ),
        ).trim()

        val assistantMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = reply,
            createdAt = time.now,
        )

        store.write(
            previous.copy(
                lastUserMessageAt = time.now,
                lastAssistantMessageAt = time.now,
                emotion = transition.emotion,
                relationship = transition.relationship,
                recentMessages = (contextMessages + assistantMessage).takeLast(MAX_CONTEXT_MESSAGES),
            ),
        )

        return ChatResult(
            reply = reply,
            timeContext = time,
            event = if (followUpEvent != InteractionEvent.None) followUpEvent else timeEvent,
        )
    }

    private companion object {
        const val MAX_CONTEXT_MESSAGES = 12
    }
}
