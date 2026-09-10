package com.luciansvon.anisachat.emotion

import com.luciansvon.anisachat.domain.EmotionState
import com.luciansvon.anisachat.domain.RelationshipState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionEngineTest {
    private val engine = EmotionEngine()

    @Test
    fun `very long same day absence raises irritation without reducing affection`() {
        val emotion = EmotionState(irritation = 5, happiness = 55)
        val relationship = RelationshipState(affection = 72)

        val result = engine.apply(
            emotion = emotion,
            relationship = relationship,
            event = InteractionEvent.SameDayVeryLongAbsence,
        )

        assertEquals(19, result.emotion.irritation)
        assertEquals(50, result.emotion.happiness)
        assertEquals(72, result.relationship.affection)
        assertTrue(result.relationship.unresolvedAbsence)
    }

    @Test
    fun `apology after absence resolves flag and cools irritation`() {
        val emotion = EmotionState(irritation = 42, happiness = 45)
        val relationship = RelationshipState(
            trust = 70,
            affection = 72,
            unresolvedAbsence = true,
        )

        val event = engine.detectFollowUpEvent("maaf tadi ketiduran", relationship)
        val result = engine.apply(emotion, relationship, event)

        assertEquals(InteractionEvent.ApologyAfterAbsence, event)
        assertEquals(24, result.emotion.irritation)
        assertEquals(50, result.emotion.happiness)
        assertEquals(72, result.relationship.trust)
        assertEquals(74, result.relationship.affection)
        assertFalse(result.relationship.unresolvedAbsence)
    }
}
