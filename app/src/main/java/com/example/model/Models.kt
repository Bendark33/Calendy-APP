package com.example.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class UserSession(
    val id: String,
    val email: String,
    val accessToken: String? = null
)

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String
)

data class FamilyMember(
    val userId: String,
    val familyId: String,
    val displayName: String,
    val color: String,
    val role: String = "member" // "owner", "admin", "member"
)

data class CalendarEvent(
    val id: String,
    val familyId: String,
    val createdBy: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startsAt: String, // ISO string or YYYY-MM-DDTHH:mm
    val endsAt: String,
    val allDay: Boolean = false,
    val important: Boolean = false,
    val confidential: Boolean = false,
    val recurrence: String = "none", // "none", "weekly", "monthly"
    val repeatUntil: String? = null, // YYYY-MM-DD
    val attendeeUserIds: List<String> = emptyList()
)

data class NotificationPrefs(
    val userId: String = "",
    val enabled: Boolean = true,
    val notifyNew: Boolean = true,
    val notifyChanges: Boolean = true,
    val onlyImportant: Boolean = false,
    val reminderOffsets: List<Int> = listOf(1440) // 1 day by default
)

data class ProblemReport(
    val category: String, // "bug", "suggestion", "autre"
    val message: String,
    val email: String,
    val includeTech: Boolean = true
)

// Helper Date representation for clean, safe handling across Android versions
data class CalendyDay(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int
) : Comparable<CalendyDay> {
    fun toYmd(): String = String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)

    fun toCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun toDate(): Date = toCalendar().time

    // 1 = Monday, 7 = Sunday (ISO standard)
    val dayOfWeek: Int
        get() {
            val cal = toCalendar()
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            return if (dow == Calendar.SUNDAY) 7 else dow - 1
        }

    fun plusDays(days: Int): CalendyDay {
        val cal = toCalendar()
        cal.add(Calendar.DAY_OF_MONTH, days)
        return fromCalendar(cal)
    }

    fun plusMonths(months: Int): CalendyDay {
        val cal = toCalendar()
        cal.add(Calendar.MONTH, months)
        return fromCalendar(cal)
    }

    override fun compareTo(other: CalendyDay): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        fun today(): CalendyDay = fromCalendar(Calendar.getInstance())

        fun fromCalendar(cal: Calendar): CalendyDay {
            return CalendyDay(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun fromYmd(ymd: String): CalendyDay {
            val parts = ymd.take(10).split("-")
            return CalendyDay(
                year = parts.getOrNull(0)?.toIntOrNull() ?: 2026,
                month = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                day = parts.getOrNull(2)?.toIntOrNull() ?: 1
            )
        }
    }
}

object CalendyDateFormatter {
    private val frenchLocale = Locale.FRENCH

    fun formatShortWeekday(day: CalendyDay): String {
        val cal = day.toCalendar()
        return SimpleDateFormat("E", frenchLocale).format(cal.time).take(1).uppercase()
    }

    fun formatWeekdayLong(day: CalendyDay): String {
        val cal = day.toCalendar()
        return SimpleDateFormat("EEEE d MMMM", frenchLocale).format(cal.time)
    }

    fun formatWeekdayWithMonth(day: CalendyDay): String {
        val cal = day.toCalendar()
        return SimpleDateFormat("EEEE d MMM", frenchLocale).format(cal.time)
    }

    fun formatMonthYear(day: CalendyDay): String {
        val cal = day.toCalendar()
        return SimpleDateFormat("MMMM yyyy", frenchLocale).format(cal.time).replaceFirstChar { it.uppercase() }
    }

    fun formatWeekRange(startDay: CalendyDay, endDay: CalendyDay): String {
        val sdfStart = SimpleDateFormat("d MMM", frenchLocale)
        val sdfEnd = SimpleDateFormat("d MMM yyyy", frenchLocale)
        return "${sdfStart.format(startDay.toDate())} – ${sdfEnd.format(endDay.toDate())}"
    }

    fun formatTime(timeIso: String): String {
        return try {
            val trimmed = timeIso.substringAfter("T").take(5)
            if (trimmed.length == 5 && trimmed[2] == ':') trimmed else "09:00"
        } catch (_: Exception) {
            "09:00"
        }
    }

    fun extractYmd(timeIso: String): String {
        return if (timeIso.length >= 10) timeIso.substring(0, 10) else timeIso
    }
}
