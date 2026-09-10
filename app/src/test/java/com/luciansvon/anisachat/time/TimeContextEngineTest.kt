package com.luciansvon.anisachat.time

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeContextEngineTest {
    private val engine = TimeContextEngine()
    private val jakarta = ZoneId.of("Asia/Jakarta")

    @Test
    fun `09 to 17 same day is very long absence`() {
        val previous = localInstant(2026, 9, 10, 9, 0)
        val now = localInstant(2026, 9, 10, 17, 0)

        val result = engine.derive(previous, now, jakarta)

        assertEquals(InactivityBucket.VERY_LONG_GAP, result.inactivityBucket)
        assertTrue(result.sameCalendarDay)
        assertFalse(result.crossedMidnight)
        assertEquals(DayPart.AFTERNOON, result.dayPart)
    }

    @Test
    fun `overnight return is marked as crossing midnight`() {
        val previous = localInstant(2026, 9, 10, 23, 0)
        val now = localInstant(2026, 9, 11, 7, 0)

        val result = engine.derive(previous, now, jakarta)

        assertTrue(result.crossedMidnight)
        assertFalse(result.sameCalendarDay)
        assertEquals(DayPart.MORNING, result.dayPart)
    }

    @Test
    fun `manual clock rollback does not create negative inactivity`() {
        val previous = localInstant(2026, 9, 10, 17, 0)
        val now = localInstant(2026, 9, 10, 16, 0)

        val result = engine.derive(previous, now, jakarta)

        assertEquals(InactivityBucket.CLOCK_ADJUSTMENT, result.inactivityBucket)
        assertEquals(null, result.elapsedSinceLastUserMessage)
    }

    private fun localInstant(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, jakarta).toInstant()
}
