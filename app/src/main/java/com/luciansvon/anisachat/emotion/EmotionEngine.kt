package com.luciansvon.anisachat.emotion

import com.luciansvon.anisachat.domain.EmotionState
import com.luciansvon.anisachat.domain.RelationshipState
import com.luciansvon.anisachat.time.InactivityBucket
import com.luciansvon.anisachat.time.TimeContext

sealed interface InteractionEvent {
    data object None : InteractionEvent
    data object SameDayLongAbsence : InteractionEvent
    data object SameDayVeryLongAbsence : InteractionEvent
    data object OvernightReturn : InteractionEvent
    data object ClockAdjusted : InteractionEvent
    data object ApologyAfterAbsence : InteractionEvent
}

data class StateTransition(
    val event: InteractionEvent,
    val emotion: EmotionState,
    val relationship: RelationshipState,
)

class EmotionEngine {
    fun eventFrom(time: TimeContext): InteractionEvent = when {
        time.inactivityBucket == InactivityBucket.CLOCK_ADJUSTMENT -> InteractionEvent.ClockAdjusted
        time.crossedMidnight -> InteractionEvent.OvernightReturn
        time.sameCalendarDay && time.inactivityBucket == InactivityBucket.VERY_LONG_GAP ->
            InteractionEvent.SameDayVeryLongAbsence
        time.sameCalendarDay && time.inactivityBucket == InactivityBucket.LONG_GAP ->
            InteractionEvent.SameDayLongAbsence
        else -> InteractionEvent.None
    }

    fun apply(
        emotion: EmotionState,
        relationship: RelationshipState,
        event: InteractionEvent,
    ): StateTransition {
        val nextEmotion: EmotionState
        val nextRelationship: RelationshipState

        when (event) {
            InteractionEvent.SameDayLongAbsence -> {
                nextEmotion = emotion.copy(
                    irritation = emotion.irritation + 10,
                    happiness = emotion.happiness - 3,
                )
                nextRelationship = relationship.copy(unresolvedAbsence = true)
            }

            InteractionEvent.SameDayVeryLongAbsence -> {
                nextEmotion = emotion.copy(
                    irritation = emotion.irritation + 14,
                    happiness = emotion.happiness - 5,
                )
                nextRelationship = relationship.copy(unresolvedAbsence = true)
            }

            InteractionEvent.ApologyAfterAbsence -> {
                nextEmotion = emotion.copy(
                    irritation = emotion.irritation - 18,
                    happiness = emotion.happiness + 5,
                )
                nextRelationship = relationship.copy(
                    trust = relationship.trust + 2,
                    affection = relationship.affection + 2,
                    unresolvedAbsence = false,
                )
            }

            InteractionEvent.OvernightReturn -> {
                nextEmotion = emotion.copy(
                    irritation = emotion.irritation - 2,
                    energy = emotion.energy + 3,
                )
                nextRelationship = relationship
            }

            InteractionEvent.ClockAdjusted,
            InteractionEvent.None -> {
                nextEmotion = decay(emotion)
                nextRelationship = relationship
            }
        }

        return StateTransition(
            event = event,
            emotion = nextEmotion.normalized(),
            relationship = nextRelationship.normalized(),
        )
    }

    fun detectFollowUpEvent(
        text: String,
        relationship: RelationshipState,
    ): InteractionEvent {
        if (!relationship.unresolvedAbsence) return InteractionEvent.None

        val normalized = text.lowercase()
        val apologySignals = listOf("maaf", "sorry", "ketiduran", "tadi sibuk", "tadi kerja", "tadi kuliah")
        return if (apologySignals.any(normalized::contains)) {
            InteractionEvent.ApologyAfterAbsence
        } else {
            InteractionEvent.None
        }
    }

    private fun decay(state: EmotionState): EmotionState = state.copy(
        irritation = (state.irritation - 1).coerceAtLeast(0),
        sadness = (state.sadness - 1).coerceAtLeast(0),
        happiness = when {
            state.happiness < 55 -> state.happiness + 1
            state.happiness > 55 -> state.happiness - 1
            else -> state.happiness
        },
    )
}
