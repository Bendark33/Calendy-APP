package com.example

import com.example.data.CalendyRepository
import com.example.model.CalendarEvent
import com.example.model.CalendyDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun calendyDay_calculation_isCorrect() {
        val day = CalendyDay(2026, 10, 24)
        assertEquals("2026-10-24", day.toYmd())
        val nextDay = day.plusDays(1)
        assertEquals("2026-10-25", nextDay.toYmd())
    }

    @Test
    fun calendyRepository_expansion_isCorrect() {
        val repo = CalendyRepository()
        val testEvent = CalendarEvent(
            id = "test-1",
            familyId = "fam-1",
            createdBy = "user-1",
            title = "Réunion hebdomadaire",
            startsAt = "2026-10-05T09:00:00Z",
            endsAt = "2026-10-05T10:00:00Z",
            recurrence = "weekly"
        )
        repo.setBaseEventsForTesting(listOf(testEvent))
        val start = CalendyDay(2026, 10, 1)
        val end = CalendyDay(2026, 10, 31)
        val events = repo.getExpandedEvents(start, end)
        assertTrue(events.size >= 4)
    }
}
