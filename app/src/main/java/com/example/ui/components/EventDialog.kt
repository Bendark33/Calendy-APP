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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CalendarEvent
import com.example.model.CalendyDateFormatter
import com.example.model.CalendyDay
import com.example.model.FamilyMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDialog(
    initialEvent: CalendarEvent?,
    defaultDay: CalendyDay,
    members: List<FamilyMember>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        title: String,
        description: String?,
        location: String?,
        startsAt: String,
        endsAt: String,
        allDay: Boolean,
        important: Boolean,
        confidential: Boolean,
        recurrence: String,
        repeatUntil: String?,
        attendees: List<String>
    ) -> Unit,
    onDelete: (String) -> Unit,
    onDuplicate: (CalendarEvent) -> Unit
) {
    val isEdit = initialEvent != null
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var allDay by remember { mutableStateOf(initialEvent?.allDay ?: false) }
    var important by remember { mutableStateOf(initialEvent?.important ?: false) }
    var confidential by remember { mutableStateOf(initialEvent?.confidential ?: false) }

    var recurrence by remember { mutableStateOf(initialEvent?.recurrence ?: "none") }
    var recurrenceExpanded by remember { mutableStateOf(false) }

    val initialStartYmd = if (initialEvent != null) CalendyDateFormatter.extractYmd(initialEvent.startsAt) else defaultDay.toYmd()
    val initialEndYmd = if (initialEvent != null) CalendyDateFormatter.extractYmd(initialEvent.endsAt) else defaultDay.toYmd()

    var startDateYmd by remember { mutableStateOf(initialStartYmd) }
    var endDateYmd by remember { mutableStateOf(initialEndYmd) }
    var startTime by remember { mutableStateOf(if (initialEvent != null) CalendyDateFormatter.formatTime(initialEvent.startsAt) else "09:00") }
    var endTime by remember { mutableStateOf(if (initialEvent != null) CalendyDateFormatter.formatTime(initialEvent.endsAt) else "10:00") }

    var repeatUntil by remember { mutableStateOf(initialEvent?.repeatUntil ?: "") }

    var selectedAttendees by remember {
        mutableStateOf(
            if (initialEvent != null) initialEvent.attendeeUserIds.toSet()
            else setOf(currentUserId)
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Modifier l'événement" else "Nouvel événement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'événement") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Options: All-day, Important, Confidential
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Toute la journée", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = allDay,
                        onCheckedChange = { allDay = it },
                        modifier = Modifier.testTag("event_all_day_switch")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !confidential) { important = !important },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = important && !confidential,
                        onCheckedChange = { if (!confidential) important = it },
                        enabled = !confidential,
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFF59E0B)),
                        modifier = Modifier.testTag("event_important_checkbox")
                    )
                    Text("★ Important", color = if (confidential) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            confidential = !confidential
                            if (confidential) {
                                important = false
                                selectedAttendees = setOf(currentUserId)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = confidential,
                        onCheckedChange = {
                            confidential = it
                            if (confidential) {
                                important = false
                                selectedAttendees = setOf(currentUserId)
                            }
                        },
                        modifier = Modifier.testTag("event_confidential_checkbox")
                    )
                    Text("🔒 Confidentiel (visible uniquement par moi)")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Recurrence dropdown
                ExposedDropdownMenuBox(
                    expanded = recurrenceExpanded,
                    onExpandedChange = { recurrenceExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (recurrence) {
                            "weekly" -> "Toutes les semaines"
                            "monthly" -> "Tous les mois"
                            else -> "Aucune répétition"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Répétition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = recurrenceExpanded,
                        onDismissRequest = { recurrenceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Aucune") },
                            onClick = { recurrence = "none"; recurrenceExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Toutes les semaines") },
                            onClick = { recurrence = "weekly"; recurrenceExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Tous les mois") },
                            onClick = { recurrence = "monthly"; recurrenceExpanded = false }
                        )
                    }
                }

                if (recurrence != "none") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = repeatUntil,
                        onValueChange = { repeatUntil = it },
                        label = { Text("Jusqu'au (AAAA-MM-JJ, optionnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Dates and times
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDateYmd,
                        onValueChange = { startDateYmd = it },
                        label = { Text("Date début") },
                        modifier = Modifier.weight(1f)
                    )
                    if (!allDay) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Heure") },
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endDateYmd,
                        onValueChange = { endDateYmd = it },
                        label = { Text("Date fin") },
                        modifier = Modifier.weight(1f)
                    )
                    if (!allDay) {
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Heure") },
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lieu (optionnel)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_location_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optionnel)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Attendees: "Qui est concerné"
                if (!confidential) {
                    Text(
                        text = "Qui est concerné",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        members.forEach { m ->
                            val isChecked = selectedAttendees.contains(m.userId)
                            val mColor = parseHexColor(m.color)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAttendees = if (isChecked) selectedAttendees - m.userId else selectedAttendees + m.userId
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedAttendees = if (checked) selectedAttendees + m.userId else selectedAttendees - m.userId
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(mColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = m.displayName, fontSize = 14.sp)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEdit) {
                        IconButton(
                            onClick = { onDelete(initialEvent.id) },
                            modifier = Modifier.testTag("event_delete_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(
                            onClick = { onDuplicate(initialEvent) },
                            modifier = Modifier.testTag("event_duplicate_button")
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Dupliquer", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    errorMessage = "Le titre est requis"
                                    return@Button
                                }
                                val sIso = "${startDateYmd}T${if (allDay) "00:00:00" else "$startTime:00"}"
                                val eIso = "${endDateYmd.ifBlank { startDateYmd }}T${if (allDay) "23:59:59" else "$endTime:00"}"
                                val attList = if (confidential) listOf(currentUserId) else selectedAttendees.toList()

                                onSave(
                                    initialEvent?.id,
                                    title.trim(),
                                    description.trim().ifEmpty { null },
                                    location.trim().ifEmpty { null },
                                    sIso,
                                    eIso,
                                    allDay,
                                    important && !confidential,
                                    confidential,
                                    recurrence,
                                    repeatUntil.trim().ifEmpty { null },
                                    attList
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("event_save_button")
                        ) {
                            Text("Enregistrer")
                        }
                    }
                }
            }
        }
    }
}
