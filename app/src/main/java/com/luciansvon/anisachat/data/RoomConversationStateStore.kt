package com.luciansvon.anisachat.data

import com.luciansvon.anisachat.data.local.AnisaDao
import com.luciansvon.anisachat.data.local.EmotionStateEntity
import com.luciansvon.anisachat.data.local.MessageEntity
import com.luciansvon.anisachat.data.local.RelationshipStateEntity
import com.luciansvon.anisachat.data.local.RuntimeMetadataEntity
import com.luciansvon.anisachat.domain.ChatMessage
import com.luciansvon.anisachat.domain.ConversationState
import com.luciansvon.anisachat.domain.EmotionState
import com.luciansvon.anisachat.domain.MessageRole
import com.luciansvon.anisachat.domain.RelationshipState
import java.time.Instant

class RoomConversationStateStore(
    private val dao: AnisaDao,
) : ConversationStateStore {
    override suspend fun read(): ConversationState {
        val runtime = dao.readRuntimeMetadata()
        val emotion = dao.readEmotionState()?.toDomain() ?: EmotionState()
        val relationship = dao.readRelationshipState()?.toDomain() ?: RelationshipState()

        return ConversationState(
            lastUserMessageAt = runtime?.lastUserMessageAtEpochMs?.let(Instant::ofEpochMilli),
            lastAssistantMessageAt = runtime?.lastAssistantMessageAtEpochMs?.let(Instant::ofEpochMilli),
            emotion = emotion.normalized(),
            relationship = relationship.normalized(),
            recentMessages = recentMessages(CONTEXT_MESSAGE_LIMIT),
        )
    }

    override suspend fun write(state: ConversationState) {
        val emotion = state.emotion.normalized()
        val relationship = state.relationship.normalized()

        dao.persistConversationState(
            runtimeMetadata = RuntimeMetadataEntity(
                lastUserMessageAtEpochMs = state.lastUserMessageAt?.toEpochMilli(),
                lastAssistantMessageAtEpochMs = state.lastAssistantMessageAt?.toEpochMilli(),
            ),
            emotionState = EmotionStateEntity(
                happiness = emotion.happiness,
                irritation = emotion.irritation,
                sadness = emotion.sadness,
                energy = emotion.energy,
            ),
            relationshipState = RelationshipStateEntity(
                familiarity = relationship.familiarity,
                trust = relationship.trust,
                affection = relationship.affection,
                unresolvedAbsence = relationship.unresolvedAbsence,
            ),
            messages = state.recentMessages.map { message -> message.toEntity() },
        )
    }

    override suspend fun recentMessages(limit: Int): List<ChatMessage> {
        if (limit <= 0) return emptyList()

        return dao
            .readRecentMessagesDescending(limit.coerceAtMost(MAX_HISTORY_READ))
            .asReversed()
            .mapNotNull { entity -> entity.toDomainOrNull() }
    }

    private fun EmotionStateEntity.toDomain() = EmotionState(
        happiness = happiness,
        irritation = irritation,
        sadness = sadness,
        energy = energy,
    )

    private fun RelationshipStateEntity.toDomain() = RelationshipState(
        familiarity = familiarity,
        trust = trust,
        affection = affection,
        unresolvedAbsence = unresolvedAbsence,
    )

    private fun ChatMessage.toEntity() = MessageEntity(
        messageId = id,
        role = role.name,
        content = content,
        createdAtEpochMs = createdAt.toEpochMilli(),
    )

    private fun MessageEntity.toDomainOrNull(): ChatMessage? {
        val parsedRole = runCatching { MessageRole.valueOf(role) }.getOrNull() ?: return null
        return ChatMessage(
            role = parsedRole,
            content = content,
            createdAt = Instant.ofEpochMilli(createdAtEpochMs),
            id = messageId,
        )
    }

    private companion object {
        const val CONTEXT_MESSAGE_LIMIT = 12
        const val MAX_HISTORY_READ = 500
    }
}
