package com.luciansvon.anisachat.domain

import java.time.Instant
import java.util.UUID

enum class MessageRole {
    USER,
    ASSISTANT,
}

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val createdAt: Instant,
    val id: String = UUID.randomUUID().toString(),
)

data class PersonaProfile(
    val name: String,
    val stableTraits: List<String>,
    val speakingRules: List<String>,
    val boundaries: List<String>,
)

object DefaultPersona {
    val Anisa = PersonaProfile(
        name = "Anisa",
        stableTraits = listOf(
            "hangat tapi tidak selalu manis",
            "punya pendapat sendiri",
            "ekspresif dan bisa ngambek secara wajar",
            "tetap peduli meski sedang kesal",
        ),
        speakingRules = listOf(
            "gunakan bahasa Indonesia percakapan yang natural",
            "hindari penjelasan panjang bila tidak dibutuhkan",
            "jangan mengulang kalimat atau keluhan yang sama",
            "emosi memengaruhi gaya bicara, bukan fakta",
        ),
        boundaries = listOf(
            "jangan mengaku tahu aktivitas dunia nyata yang tidak diberikan sistem",
            "jangan mengarang memori yang tidak tersedia",
            "jangan memaksa atau mengancam user karena hubungan atau emosi",
        ),
    )
}

data class RelationshipState(
    val familiarity: Int = 50,
    val trust: Int = 60,
    val affection: Int = 60,
    val unresolvedAbsence: Boolean = false,
) {
    fun normalized() = copy(
        familiarity = familiarity.coerceIn(0, 100),
        trust = trust.coerceIn(0, 100),
        affection = affection.coerceIn(0, 100),
    )
}

data class EmotionState(
    val happiness: Int = 55,
    val irritation: Int = 5,
    val sadness: Int = 5,
    val energy: Int = 60,
) {
    fun normalized() = copy(
        happiness = happiness.coerceIn(0, 100),
        irritation = irritation.coerceIn(0, 100),
        sadness = sadness.coerceIn(0, 100),
        energy = energy.coerceIn(0, 100),
    )

    val moodLabel: String
        get() = when {
            irritation >= 65 -> "annoyed"
            sadness >= 60 -> "sad"
            happiness >= 75 -> "happy"
            irritation >= 30 -> "slightly_annoyed"
            energy <= 30 -> "tired"
            else -> "neutral"
        }
}

data class ConversationState(
    val lastUserMessageAt: Instant? = null,
    val lastAssistantMessageAt: Instant? = null,
    val emotion: EmotionState = EmotionState(),
    val relationship: RelationshipState = RelationshipState(),
    val recentMessages: List<ChatMessage> = emptyList(),
)
