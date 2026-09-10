package com.luciansvon.anisachat.time

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

enum class DayPart {
    EARLY_MORNING,
    MORNING,
    MIDDAY,
    AFTERNOON,
    EVENING,
    LATE_NIGHT,
}

enum class InactivityBucket {
    FIRST_CONTACT,
    CONTINUOUS,
    SHORT_GAP,
    MEDIUM_GAP,
    LONG_GAP,
    VERY_LONG_GAP,
    CLOCK_ADJUSTMENT,
}

data class TimeContext(
    val now: Instant,
    val localTimeText: String,
    val zoneId: String,
    val dayPart: DayPart,
    val elapsedSinceLastUserMessage: Duration?,
    val inactivityBucket: InactivityBucket,
    val sameCalendarDay: Boolean,
    val crossedMidnight: Boolean,
)

class TimeContextEngine(
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    fun derive(lastUserMessageAt: Instant?): TimeContext {
        val now = clock.instant()
        val zone = clock.zone
        return derive(lastUserMessageAt, now, zone)
    }

    fun derive(
        lastUserMessageAt: Instant?,
        now: Instant,
        zone: ZoneId,
    ): TimeContext {
        val localNow = ZonedDateTime.ofInstant(now, zone)
        val previousLocal = lastUserMessageAt?.let { ZonedDateTime.ofInstant(it, zone) }
        val rawElapsed = lastUserMessageAt?.let { Duration.between(it, now) }
        val clockAdjusted = rawElapsed?.isNegative == true
        val elapsed = rawElapsed?.takeUnless { it.isNegative }

        val bucket = when {
            lastUserMessageAt == null -> InactivityBucket.FIRST_CONTACT
            clockAdjusted -> InactivityBucket.CLOCK_ADJUSTMENT
            elapsed == null -> InactivityBucket.FIRST_CONTACT
            elapsed < Duration.ofMinutes(30) -> InactivityBucket.CONTINUOUS
            elapsed < Duration.ofHours(2) -> InactivityBucket.SHORT_GAP
            elapsed < Duration.ofHours(4) -> InactivityBucket.MEDIUM_GAP
            elapsed < Duration.ofHours(8) -> InactivityBucket.LONG_GAP
            else -> InactivityBucket.VERY_LONG_GAP
        }

        val sameDay = previousLocal?.toLocalDate() == localNow.toLocalDate()

        return TimeContext(
            now = now,
            localTimeText = "%02d:%02d".format(localNow.hour, localNow.minute),
            zoneId = zone.id,
            dayPart = dayPart(localNow.hour),
            elapsedSinceLastUserMessage = elapsed,
            inactivityBucket = bucket,
            sameCalendarDay = sameDay,
            crossedMidnight = previousLocal != null && !sameDay,
        )
    }

    private fun dayPart(hour: Int): DayPart = when (hour) {
        in 4..6 -> DayPart.EARLY_MORNING
        in 7..10 -> DayPart.MORNING
        in 11..14 -> DayPart.MIDDAY
        in 15..17 -> DayPart.AFTERNOON
        in 18..21 -> DayPart.EVENING
        else -> DayPart.LATE_NIGHT
    }
}
