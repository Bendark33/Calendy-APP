package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalendyPreferences
import com.example.data.CalendyRepository
import com.example.model.CalendarEvent
import com.example.model.CalendyDay
import com.example.model.Family
import com.example.model.FamilyMember
import com.example.model.NotificationPrefs
import com.example.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class CalendarViewMode {
    WEEK,
    MONTH
}

class CalendyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferences = CalendyPreferences(application)
    val repository: CalendyRepository = CalendyRepository(preferences)

    val userSession: StateFlow<UserSession?> = repository.userSession
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val needsOnboarding: StateFlow<Boolean> = repository.needsOnboarding

    val currentFamily: StateFlow<Family?> = repository.currentFamily
    val currentMember: StateFlow<FamilyMember?> = repository.currentMember
    val families: StateFlow<List<Family>> = repository.families
    val familyMembers: StateFlow<List<FamilyMember>> = repository.familyMembers
    val notificationPrefs: StateFlow<NotificationPrefs> = repository.notificationPrefs

    private val _isDarkMode = MutableStateFlow(preferences.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        preferences.isDarkMode = enabled
        _statusMessage.value = if (enabled) "Mode Sombre activé" else "Mode Clair activé"
    }

    fun toggleTheme() {
        setDarkMode(!_isDarkMode.value)
    }

    private val _viewMode = MutableStateFlow(CalendarViewMode.WEEK)
    val viewMode: StateFlow<CalendarViewMode> = _viewMode.asStateFlow()

    private val _selectedDay = MutableStateFlow(CalendyDay.today())
    val selectedDay: StateFlow<CalendyDay> = _selectedDay.asStateFlow()

    private val _monthPivotDay = MutableStateFlow(CalendyDay(CalendyDay.today().year, CalendyDay.today().month, 1))
    val monthPivotDay: StateFlow<CalendyDay> = _monthPivotDay.asStateFlow()

    private val _offMemberIds = MutableStateFlow<Set<String>>(emptySet())
    val offMemberIds: StateFlow<Set<String>> = _offMemberIds.asStateFlow()

    // Dialogs state
    private val _editingEvent = MutableStateFlow<CalendarEvent?>(null)
    val editingEvent: StateFlow<CalendarEvent?> = _editingEvent.asStateFlow()

    private val _showEventDialog = MutableStateFlow(false)
    val showEventDialog: StateFlow<Boolean> = _showEventDialog.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog: StateFlow<Boolean> = _showProfileDialog.asStateFlow()

    private val _showNotificationsDialog = MutableStateFlow(false)
    val showNotificationsDialog: StateFlow<Boolean> = _showNotificationsDialog.asStateFlow()

    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog: StateFlow<Boolean> = _showReportDialog.asStateFlow()

    private val _showAuthDialog = MutableStateFlow(false)
    val showAuthDialog: StateFlow<Boolean> = _showAuthDialog.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Compute visible events expanded by recurrence and filtered by member toggles
    val visibleEvents: StateFlow<List<CalendarEvent>> = combine(
        repository.baseEvents,
        _selectedDay,
        _monthPivotDay,
        _viewMode,
        _offMemberIds
    ) { _, sel, monthPivot, mode, offIds ->
        val (start, end) = if (mode == CalendarViewMode.WEEK) {
            val weekStart = sel.plusDays(-(sel.dayOfWeek - 1))
            val weekEnd = weekStart.plusDays(6)
            Pair(weekStart, weekEnd)
        } else {
            val monthStart = CalendyDay(monthPivot.year, monthPivot.month, 1).plusDays(-7)
            val monthEnd = CalendyDay(monthPivot.year, monthPivot.month, 1).plusMonths(1).plusDays(7)
            Pair(monthStart, monthEnd)
        }
        val expanded = repository.getExpandedEvents(start, end)
        expanded.filter { ev ->
            val involved = (listOf(ev.createdBy) + ev.attendeeUserIds).toSet()
            involved.isEmpty() || involved.any { !offIds.contains(it) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setViewMode(mode: CalendarViewMode) {
        _viewMode.value = mode
    }

    fun selectDay(day: CalendyDay) {
        _selectedDay.value = day
        if (_viewMode.value == CalendarViewMode.MONTH && (day.month != _monthPivotDay.value.month || day.year != _monthPivotDay.value.year)) {
            _monthPivotDay.value = CalendyDay(day.year, day.month, 1)
        }
    }

    fun previousPeriod() {
        if (_viewMode.value == CalendarViewMode.WEEK) {
            val newDay = _selectedDay.value.plusDays(-7)
            _selectedDay.value = newDay
            _monthPivotDay.value = CalendyDay(newDay.year, newDay.month, 1)
        } else {
            val prevMonth = _monthPivotDay.value.plusMonths(-1)
            _monthPivotDay.value = CalendyDay(prevMonth.year, prevMonth.month, 1)
        }
    }

    fun nextPeriod() {
        if (_viewMode.value == CalendarViewMode.WEEK) {
            val newDay = _selectedDay.value.plusDays(7)
            _selectedDay.value = newDay
            _monthPivotDay.value = CalendyDay(newDay.year, newDay.month, 1)
        } else {
            val nextMonth = _monthPivotDay.value.plusMonths(1)
            _monthPivotDay.value = CalendyDay(nextMonth.year, nextMonth.month, 1)
        }
    }

    fun goToToday() {
        val today = CalendyDay.today()
        _selectedDay.value = today
        _monthPivotDay.value = CalendyDay(today.year, today.month, 1)
    }

    fun toggleMemberFilter(userId: String) {
        val current = _offMemberIds.value
        _offMemberIds.value = if (current.contains(userId)) {
            current - userId
        } else {
            current + userId
        }
    }

    fun openNewEventDialog(day: CalendyDay? = null) {
        if (day != null) _selectedDay.value = day
        _editingEvent.value = null
        _showEventDialog.value = true
    }

    fun openEditEventDialog(event: CalendarEvent) {
        _editingEvent.value = event
        _showEventDialog.value = true
    }

    fun closeEventDialog() {
        _showEventDialog.value = false
        _editingEvent.value = null
    }

    fun saveEvent(
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
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val famId = currentFamily.value?.id ?: "fam-1"
            val myId = currentMember.value?.userId ?: userSession.value?.id ?: "local"
            val event = CalendarEvent(
                id = id ?: UUID.randomUUID().toString(),
                familyId = famId,
                createdBy = myId,
                title = title,
                description = description,
                location = location,
                startsAt = startsAt,
                endsAt = endsAt,
                allDay = allDay,
                important = important,
                confidential = confidential,
                recurrence = recurrence,
                repeatUntil = repeatUntil,
                attendeeUserIds = attendees
            )
            val res = repository.saveEvent(event, isNew = (id == null))
            _isLoading.value = false
            if (res.isSuccess) {
                closeEventDialog()
                _statusMessage.value = if (id == null) "Événement créé" else "Événement mis à jour"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Erreur d'enregistrement"
            }
        }
    }

    fun duplicateEvent(event: CalendarEvent) {
        _editingEvent.value = event.copy(id = UUID.randomUUID().toString(), title = "Copie : ${event.title}")
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteEvent(eventId)
            _isLoading.value = false
            if (res.isSuccess) {
                closeEventDialog()
                _statusMessage.value = "Événement supprimé"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Erreur de suppression"
            }
        }
    }

    fun openProfileDialog() {
        _showProfileDialog.value = true
    }

    fun closeProfileDialog() {
        _showProfileDialog.value = false
    }

    fun updateProfile(displayName: String, color: String, familyName: String?) {
        repository.updateProfile(displayName, color, familyName)
        closeProfileDialog()
        _statusMessage.value = "Profil mis à jour"
    }

    fun renewInviteCode(): String {
        val code = repository.renewInviteCode()
        _statusMessage.value = "Nouveau code d'invitation : $code"
        return code
    }

    fun openNotificationsDialog() {
        _showProfileDialog.value = false
        _showNotificationsDialog.value = true
    }

    fun closeNotificationsDialog() {
        _showNotificationsDialog.value = false
    }

    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        repository.updateNotificationPrefs(prefs)
        closeNotificationsDialog()
        _statusMessage.value = "Préférences de notifications enregistrées"
    }

    fun openReportDialog() {
        _showProfileDialog.value = false
        _showReportDialog.value = true
    }

    fun closeReportDialog() {
        _showReportDialog.value = false
    }

    fun sendProblemReport(category: String, message: String, includeTech: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.sendProblemReport(category, message, includeTech)
            _isLoading.value = false
            if (res.isSuccess) {
                closeReportDialog()
                _statusMessage.value = "Merci, ton signalement a bien été envoyé !"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Erreur d'envoi"
            }
        }
    }

    fun openAuthDialog() {
        _showProfileDialog.value = false
        _showAuthDialog.value = true
    }

    fun closeAuthDialog() {
        _showAuthDialog.value = false
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.signIn(email, pass)
            _isLoading.value = false
            if (res.isSuccess) {
                closeAuthDialog()
                _statusMessage.value = "Connexion réussie !"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Erreur de connexion"
            }
        }
    }

    fun signUp(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.signUp(email, pass)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = res.getOrNull() ?: "Compte créé !"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Erreur d'inscription"
            }
        }
    }

    fun signInWithGoogle(idToken: String, email: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.signInWithGoogle(idToken, email)
            _isLoading.value = false
            if (res.isSuccess) {
                closeAuthDialog()
                _statusMessage.value = "Connecté avec Google !"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Erreur de connexion Google"
            }
        }
    }

    fun syncWithDatabase() {
        val session = userSession.value
        if (session?.accessToken != null) {
            viewModelScope.launch {
                _isLoading.value = true
                val res = repository.syncUserData(session)
                _isLoading.value = false
                if (res.isSuccess) {
                    _statusMessage.value = "Calendrier actualisé"
                } else {
                    _statusMessage.value = "Erreur de synchronisation"
                }
            }
        } else {
            _statusMessage.value = "Connecte-toi pour synchroniser le calendrier"
        }
    }

    fun logout() {
        repository.logout()
        closeProfileDialog()
        _statusMessage.value = "Déconnecté"
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun switchFamily(family: Family) {
        repository.switchFamily(family)
        _statusMessage.value = "Famille active : ${family.name}"
    }

    fun createFamily(name: String, displayName: String, color: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createFamily(name, displayName, color)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = "Famille « $name » créée !"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Erreur création famille"
            }
        }
    }

    fun joinFamily(inviteCode: String, displayName: String, color: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.joinFamily(inviteCode, displayName, color)
            _isLoading.value = false
            if (res.isSuccess) {
                _statusMessage.value = "Famille rejointe avec succès !"
            } else {
                _statusMessage.value = res.exceptionOrNull()?.message ?: "Code d'invitation invalide"
            }
        }
    }
}
