package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendyDateFormatter
import com.example.model.CalendyDay
import com.example.model.FamilyMember
import com.example.ui.theme.CalendyTodayHighlight
import com.example.viewmodel.CalendarViewMode

@Composable
fun HeaderSection(
    greetingName: String,
    familyName: String,
    members: List<FamilyMember>,
    selectedDay: CalendyDay,
    monthPivotDay: CalendyDay,
    viewMode: CalendarViewMode,
    offMemberIds: Set<String>,
    onSelectDay: (CalendyDay) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onToggleMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = CalendyDay.today()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Greetings and Member Avatars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Salut $greetingName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "La semaine de $familyName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Member Avatars row
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                members.forEach { member ->
                    val color = parseHexColor(member.color)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.displayName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Bar: Period Title + Previous / Today / Next controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val periodTitle = if (viewMode == CalendarViewMode.WEEK) {
                val weekStart = selectedDay.plusDays(-(selectedDay.dayOfWeek - 1))
                val weekEnd = weekStart.plusDays(6)
                CalendyDateFormatter.formatWeekRange(weekStart, weekEnd)
            } else {
                CalendyDateFormatter.formatMonthYear(monthPivotDay)
            }

            Text(
                text = periodTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.testTag("nav_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Période précédente",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = onToday,
                    modifier = Modifier.testTag("nav_today_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Aujourd'hui",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.testTag("nav_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Période suivante",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 7-Day horizontal strip for fast jumping (L M M J V S D)
        val currentWeekStart = selectedDay.plusDays(-(selectedDay.dayOfWeek - 1))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..6) {
                val day = currentWeekStart.plusDays(i)
                val isToday = day == today
                val isSelected = day == selectedDay

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        isToday -> CalendyTodayHighlight
                        else -> MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectDay(day) }
                        .testTag("day_strip_${day.toYmd()}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = CalendyDateFormatter.formatShortWeekday(day),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = day.day.toString(),
                            fontSize = 16.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Member Filter Chips row (Avatar dot + Name)
        if (members.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(members) { member ->
                    val isOff = offMemberIds.contains(member.userId)
                    val memberColor = parseHexColor(member.color)

                    AssistChip(
                        onClick = { onToggleMember(member.userId) },
                        label = {
                            Text(
                                text = member.displayName,
                                color = if (isOff) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isOff) FontWeight.Normal else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isOff) Color.Gray else memberColor)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isOff) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("filter_chip_${member.userId}")
                    )
                }
            }
        }
    }
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF3B82F6)): Color {
    return try {
        val clean = hex.removePrefix("#")
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
