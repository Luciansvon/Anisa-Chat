package com.luciansvon.anisachat.chat

import com.luciansvon.anisachat.domain.EmotionState
import com.luciansvon.anisachat.domain.PersonaProfile
import com.luciansvon.anisachat.domain.RelationshipState
import com.luciansvon.anisachat.time.TimeContext

class SystemContextBuilder {
    fun build(
        persona: PersonaProfile,
        emotion: EmotionState,
        relationship: RelationshipState,
        time: TimeContext,
        memories: List<String> = emptyList(),
    ): String = buildString {
        appendLine("Kamu adalah ${persona.name}.")
        appendLine("Identitas stabil: ${persona.stableTraits.joinToString("; ")}.")
        appendLine("Aturan bicara: ${persona.speakingRules.joinToString("; ")}.")
        appendLine("Batasan: ${persona.boundaries.joinToString("; ")}.")
        appendLine("Mood saat ini: ${emotion.moodLabel}.")
        appendLine(
            "State emosi: happiness=${emotion.happiness}, irritation=${emotion.irritation}, " +
                "sadness=${emotion.sadness}, energy=${emotion.energy}.",
        )
        appendLine(
            "Hubungan: familiarity=${relationship.familiarity}, trust=${relationship.trust}, " +
                "affection=${relationship.affection}, unresolvedAbsence=${relationship.unresolvedAbsence}.",
        )
        appendLine(
            "Waktu perangkat: ${time.localTimeText}, ${time.dayPart}; " +
                "gap=${time.inactivityBucket}, crossedMidnight=${time.crossedMidnight}.",
        )
        if (memories.isNotEmpty()) {
            appendLine("Memori relevan: ${memories.joinToString(" | ")}.")
        }
        appendLine("Gunakan state di atas untuk gaya respons. Jangan mengarang fakta yang tidak diberikan.")
    }.trim()
}
