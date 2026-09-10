package com.luciansvon.anisachat.chat

import com.luciansvon.anisachat.domain.EmotionState
import com.luciansvon.anisachat.time.TimeContext
import java.time.Duration
import java.util.Locale

class DeterministicRouter {
    fun tryAnswer(
        userText: String,
        time: TimeContext,
        emotion: EmotionState,
    ): String? {
        val text = userText.lowercase(Locale.ROOT).trim()

        return when {
            asksCurrentTime(text) -> currentTimeReply(time, emotion)
            asksAbsenceDuration(text) -> absenceDurationReply(time)
            else -> null
        }
    }

    private fun asksCurrentTime(text: String): Boolean {
        val phrases = listOf(
            "jam berapa",
            "sekarang jam",
            "pukul berapa",
            "sekarang pukul",
        )
        return phrases.any(text::contains)
    }

    private fun asksAbsenceDuration(text: String): Boolean {
        val phrases = listOf(
            "berapa lama aku nggak chat",
            "berapa lama aku gak chat",
            "berapa lama aku ga chat",
            "udah berapa lama aku nggak chat",
            "sudah berapa lama aku tidak chat",
        )
        return phrases.any(text::contains)
    }

    private fun currentTimeReply(time: TimeContext, emotion: EmotionState): String = when {
        emotion.irritation >= 65 -> "sekarang jam ${time.localTimeText}."
        emotion.happiness >= 75 -> "sekarang jam ${time.localTimeText} nih."
        else -> "sekarang jam ${time.localTimeText}."
    }

    private fun absenceDurationReply(time: TimeContext): String {
        val elapsed = time.elapsedSinceLastUserMessage
            ?: return "aku belum punya timestamp chat sebelumnya yang bisa dibandingkan."

        return "sekitar ${formatDuration(elapsed)} sejak chat kamu sebelumnya."
    }

    private fun formatDuration(duration: Duration): String {
        val totalMinutes = duration.toMinutes().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "$hours jam $minutes menit"
            hours > 0 -> "$hours jam"
            else -> "$minutes menit"
        }
    }
}
