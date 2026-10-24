package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.model.CalendyDateFormatter
import com.example.model.CalendyDay
import com.example.model.FamilyMember
import com.example.ui.theme.CalendyTodayHighlight
import com.example.ui.theme.DayPastels

@Composable
fun WeekView(
    selectedDay: CalendyDay,
    events: List<CalendarEvent>,
    members: List<FamilyMember>,
    onSelectDay: (CalendyDay) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onAddEventForDay: (CalendyDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = CalendyDay.today()
    val weekStart = selectedDay.plusDays(-(selectedDay.dayOfWeek - 1))
    val memberMap = members.associateBy { it.userId }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(7) { index ->
            val day = weekStart.plusDays(index)
            val isToday = day == today
            val isSelected = day == selectedDay
            val dayYmd = day.toYmd()

            // Filter events happening on this specific day
            val dayEvents = events.filter { ev ->
                val startYmd = CalendyDateFormatter.extractYmd(ev.startsAt)
                val endYmd = CalendyDateFormatter.extractYmd(ev.endsAt)
                startYmd <= dayYmd && dayYmd <= endYmd
            }

            val cardColor = if (isToday) {
                CalendyTodayHighlight
            } else {
                DayPastels[index % DayPastels.size]
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 0.5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectDay(day) }
                    .testTag("week_day_card_${day.toYmd()}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Day Card Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isToday) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "Aujourd'hui",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = CalendyDateFormatter.formatWeekdayWithMonth(day).replaceFirstChar { it.uppercase() },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF2A1A00)
                            )
                        }

                        IconButton(
                            onClick = { onAddEventForDay(day) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("add_event_for_day_${day.toYmd()}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Ajouter événement pour ce jour",
                                tint = Color(0xFF2A1A00).copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (dayEvents.isEmpty()) {
                        Text(
                            text = "Rien de prévu",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF2A1A00).copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dayEvents.forEach { event ->
                                EventItemRow(
                                    event = event,
                                    memberMap = memberMap,
                                    onClick = { onEventClick(event) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventItemRow(
    event: CalendarEvent,
    memberMap: Map<String, FamilyMember>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect attendees + creator colors for left gradient bar
    val involvedUserIds = (listOf(event.createdBy) + event.attendeeUserIds).distinct()
    val involvedColors = involvedUserIds.mapNotNull { memberMap[it]?.color?.let { c -> parseHexColor(c) } }
        .ifEmpty { listOf(MaterialTheme.colorScheme.primary) }

    val barBrush = if (involvedColors.size > 1) {
        Brush.verticalGradient(involvedColors)
    } else {
        Brush.verticalGradient(listOf(involvedColors.first(), involvedColors.first()))
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("event_item_${event.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored gradient bar indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barBrush)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Time or All-day badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = if (event.allDay) "Journée" else CalendyDateFormatter.formatTime(event.startsAt),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Status Icons
                    if (event.important) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Important",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp)
                        )
                    }
                    if (event.confidential) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Confidentiel",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp)
                        )
                    }
                    if (event.recurrence.isNotEmpty() && event.recurrence != "none") {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Répétitif",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 2.dp)
                        )
                    }

                    Text(
                        text = event.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }

                if (!event.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = event.location,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Involved members mini avatars
            Row(
                horizontalArrangement = Arrangement.spacedBy((-4).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                involvedUserIds.take(3).forEach { uid ->
                    val mem = memberMap[uid]
                    if (mem != null) {
                        val color = parseHexColor(mem.color)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mem.displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
