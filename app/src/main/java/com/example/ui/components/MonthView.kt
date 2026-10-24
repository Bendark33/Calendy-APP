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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.model.CalendyDateFormatter
import com.example.model.CalendyDay
import com.example.model.FamilyMember
import com.example.ui.theme.CalendyTodayHighlight

@Composable
fun MonthView(
    selectedDay: CalendyDay,
    monthPivotDay: CalendyDay,
    events: List<CalendarEvent>,
    members: List<FamilyMember>,
    onSelectDay: (CalendyDay) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onAddEvent: (CalendyDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = CalendyDay.today()
    val memberMap = members.associateBy { it.userId }

    // Compute 42 days grid for the month view
    val firstOfMonth = CalendyDay(monthPivotDay.year, monthPivotDay.month, 1)
    val startDayOffset = firstOfMonth.dayOfWeek - 1 // 0 for Monday
    val gridStartDay = firstOfMonth.plusDays(-startDayOffset)

    val gridDays = (0 until 42).map { gridStartDay.plusDays(it) }

    // Selected day events
    val selectedYmd = selectedDay.toYmd()
    val selectedDayEvents = events.filter { ev ->
        val startYmd = CalendyDateFormatter.extractYmd(ev.startsAt)
        val endYmd = CalendyDateFormatter.extractYmd(ev.endsAt)
        startYmd <= selectedYmd && selectedYmd <= endYmd
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Weekday headers: L, M, M, J, V, S, D
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("L", "M", "M", "J", "V", "S", "D").forEach { label ->
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7x6 grid
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until 6) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (col in 0 until 7) {
                                    val dayIndex = row * 7 + col
                                    val day = gridDays[dayIndex]
                                    val isCurrentMonth = day.month == monthPivotDay.month
                                    val isToday = day == today
                                    val isSelected = day == selectedDay

                                    val dayYmd = day.toYmd()
                                    val dayEvs = events.filter { ev ->
                                        val startYmd = CalendyDateFormatter.extractYmd(ev.startsAt)
                                        val endYmd = CalendyDateFormatter.extractYmd(ev.endsAt)
                                        startYmd <= dayYmd && dayYmd <= endYmd
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            isToday -> CalendyTodayHighlight
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isCurrentMonth) 0.5f else 0.15f)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onSelectDay(day) }
                                            .testTag("month_cell_${day.toYmd()}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = day.day.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    !isCurrentMonth -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> Color(0xFF2A1A00)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )

                                            // Event dots or pills
                                            if (dayEvs.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 1.dp)
                                                ) {
                                                    dayEvs.take(3).forEach { ev ->
                                                        val creatorColor = memberMap[ev.createdBy]?.color?.let { parseHexColor(it) }
                                                            ?: MaterialTheme.colorScheme.primary
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(creatorColor)
                                                        )
                                                    }
                                                }
                                                if (dayEvs.size > 3) {
                                                    Text(
                                                        text = "+${dayEvs.size - 3}",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Details Card (matches Calendy's dayCard)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_day_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = CalendyDateFormatter.formatWeekdayLong(selectedDay).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedDay == today) {
                                Text(
                                    text = "Aujourd'hui",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = { onAddEvent(selectedDay) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_event_for_selected_day")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedDayEvents.isEmpty()) {
                        Text(
                            text = "Rien de prévu pour cette journée.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedDayEvents.forEach { ev ->
                                EventItemRow(
                                    event = ev,
                                    memberMap = memberMap,
                                    onClick = { onEventClick(ev) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
