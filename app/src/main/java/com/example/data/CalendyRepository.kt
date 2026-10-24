package com.example.data

import com.example.model.CalendarEvent
import com.example.model.CalendyDateFormatter
import com.example.model.CalendyDay
import com.example.model.Family
import com.example.model.FamilyMember
import com.example.model.NotificationPrefs
import com.example.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class CalendyRepository(
    private val preferences: CalendyPreferences? = null
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _userSession = MutableStateFlow<UserSession?>(null)
    val userSession: StateFlow<UserSession?> = _userSession.asStateFlow()

    private val _families = MutableStateFlow<List<Family>>(emptyList())
    val families: StateFlow<List<Family>> = _families.asStateFlow()

    private val _currentFamily = MutableStateFlow<Family?>(null)
    val currentFamily: StateFlow<Family?> = _currentFamily.asStateFlow()

    private val _currentMember = MutableStateFlow<FamilyMember?>(null)
    val currentMember: StateFlow<FamilyMember?> = _currentMember.asStateFlow()

    private val _familyMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val familyMembers: StateFlow<List<FamilyMember>> = _familyMembers.asStateFlow()

    private val _baseEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val baseEvents: StateFlow<List<CalendarEvent>> = _baseEvents.asStateFlow()

    private val _notificationPrefs = MutableStateFlow(NotificationPrefs())
    val notificationPrefs: StateFlow<NotificationPrefs> = _notificationPrefs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _needsOnboarding = MutableStateFlow(false)
    val needsOnboarding: StateFlow<Boolean> = _needsOnboarding.asStateFlow()

    init {
        val hasSession = preferences?.hasValidSession() == true
        if (hasSession) {
            val session = UserSession(
                id = preferences!!.userId!!,
                email = preferences.userEmail ?: "",
                accessToken = preferences.accessToken
            )
            _userSession.value = session
            coroutineScope.launch {
                syncUserData(session)
            }
        } else {
            _userSession.value = null
        }
    }

    suspend fun signIn(email: String, pass: String): Result<Unit> {
        _isSyncing.value = true
        val res = SupabaseApi.signIn(email, pass)
        _isSyncing.value = false
        return if (res.isSuccess) {
            val session = res.getOrThrow()
            _userSession.value = session
            preferences?.accessToken = session.accessToken
            preferences?.userId = session.id
            preferences?.userEmail = session.email

            syncUserData(session)
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Erreur de connexion"))
        }
    }

    suspend fun signInWithGoogle(idToken: String, email: String?): Result<Unit> {
        _isSyncing.value = true
        val res = SupabaseApi.signInWithGoogle(idToken, email)
        _isSyncing.value = false
        return if (res.isSuccess) {
            val session = res.getOrThrow()
            _userSession.value = session
            preferences?.accessToken = session.accessToken
            preferences?.userId = session.id
            preferences?.userEmail = session.email

            syncUserData(session)
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Erreur connexion Google"))
        }
    }

    suspend fun signUp(email: String, pass: String): Result<String> {
        return SupabaseApi.signUp(email, pass)
    }

    suspend fun syncUserData(session: UserSession): Result<Unit> {
        val token = session.accessToken ?: return Result.failure(Exception("Pas de jeton"))
        _isSyncing.value = true
        val membersRes = SupabaseApi.loadFamilyMembers(session.id, token)
        _isSyncing.value = false
        if (membersRes.isSuccess) {
            val (members, families) = membersRes.getOrThrow()
            _families.value = families
            if (families.isEmpty()) {
                _needsOnboarding.value = true
                _currentFamily.value = null
                _currentMember.value = null
            } else {
                _needsOnboarding.value = false
                val preferredFamId = preferences?.activeFamilyId
                val activeFam = families.firstOrNull { it.id == preferredFamId } ?: families.first()
                _currentFamily.value = activeFam
                _currentMember.value = members.firstOrNull { it.familyId == activeFam.id }
                preferences?.activeFamilyId = activeFam.id
                refreshFamilyEvents(activeFam.id, token)
            }
            return Result.success(Unit)
        } else {
            // If user has no families linked yet, activate onboarding immediately
            _needsOnboarding.value = true
            _currentFamily.value = null
            _currentMember.value = null
            _families.value = emptyList()
            return Result.success(Unit)
        }
    }

    suspend fun refreshFamilyEvents(familyId: String, token: String? = null) {
        val t = token ?: _userSession.value?.accessToken
        if (t != null) {
            _isSyncing.value = true
            val membersRes = SupabaseApi.loadAllMembersOfFamily(familyId, t)
            if (membersRes.isSuccess) {
                _familyMembers.value = membersRes.getOrThrow()
            }
            val eventsRes = SupabaseApi.loadEvents(familyId, t)
            if (eventsRes.isSuccess) {
                _baseEvents.value = eventsRes.getOrThrow()
            }
            _isSyncing.value = false
        }
    }

    suspend fun createFamily(name: String, displayName: String, color: String): Result<Unit> {
        val session = _userSession.value
        val token = session?.accessToken
        if (token != null) {
            val res = SupabaseApi.createFamily(name, displayName, color, token, session.id)
            return if (res.isSuccess) {
                syncUserData(session)
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Erreur création famille"))
            }
        }
        return Result.failure(Exception("Non connecté"))
    }

    suspend fun joinFamily(inviteCode: String, displayName: String, color: String): Result<Unit> {
        val session = _userSession.value
        val token = session?.accessToken
        if (token != null) {
            val res = SupabaseApi.joinFamily(inviteCode, displayName, color, token, session.id)
            return if (res.isSuccess) {
                syncUserData(session)
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Code invalide"))
            }
        }
        return Result.failure(Exception("Non connecté"))
    }

    fun switchFamily(family: Family) {
        _currentFamily.value = family
        preferences?.activeFamilyId = family.id
        _currentMember.value = _familyMembers.value.firstOrNull { it.familyId == family.id }
        val token = _userSession.value?.accessToken
        if (token != null) {
            coroutineScope.launch {
                refreshFamilyEvents(family.id, token)
            }
        }
    }

    suspend fun saveEvent(event: CalendarEvent, isNew: Boolean): Result<Unit> {
        val token = _userSession.value?.accessToken
        if (token != null) {
            val res = SupabaseApi.saveEvent(event, token, isNew)
            return if (res.isSuccess) {
                val famId = _currentFamily.value?.id ?: event.familyId
                refreshFamilyEvents(famId, token)
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Erreur sauvegarde"))
            }
        }
        return Result.failure(Exception("Non connecté"))
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> {
        val token = _userSession.value?.accessToken
        if (token != null) {
            val res = SupabaseApi.deleteEvent(eventId, token)
            return if (res.isSuccess) {
                val famId = _currentFamily.value?.id ?: ""
                refreshFamilyEvents(famId, token)
                Result.success(Unit)
            } else {
                Result.failure(res.exceptionOrNull() ?: Exception("Erreur suppression"))
            }
        }
        return Result.failure(Exception("Non connecté"))
    }

    fun updateProfile(displayName: String, color: String, familyName: String?) {
        val member = _currentMember.value ?: return
        val updatedMember = member.copy(displayName = displayName, color = color)
        _currentMember.value = updatedMember
        _familyMembers.value = _familyMembers.value.map { if (it.userId == member.userId) updatedMember else it }

        val token = _userSession.value?.accessToken
        if (token != null) {
            coroutineScope.launch {
                SupabaseApi.updateFamilyMember(member.familyId, member.userId, displayName, color, token)
            }
        }

        if (!familyName.isNullOrBlank()) {
            val fam = _currentFamily.value
            if (fam != null) {
                val updatedFam = fam.copy(name = familyName)
                _currentFamily.value = updatedFam
                _families.value = _families.value.map { if (it.id == fam.id) updatedFam else it }
                if (token != null) {
                    coroutineScope.launch {
                        SupabaseApi.updateFamilyName(fam.id, familyName, token)
                    }
                }
            }
        }
    }

    fun renewInviteCode(): String {
        val code = (10000000..99999999).random().toString(16).take(8)
        val fam = _currentFamily.value
        if (fam != null) {
            val updated = fam.copy(inviteCode = code)
            _currentFamily.value = updated
            _families.value = _families.value.map { if (it.id == fam.id) updated else it }
            val token = _userSession.value?.accessToken
            if (token != null) {
                coroutineScope.launch {
                    SupabaseApi.updateInviteCode(fam.id, code, token)
                }
            }
        }
        return code
    }

    fun updateNotificationPrefs(prefs: NotificationPrefs) {
        _notificationPrefs.value = prefs
    }

    suspend fun sendProblemReport(category: String, message: String, includeTech: Boolean): Result<Unit> {
        val token = _userSession.value?.accessToken ?: return Result.failure(Exception("Non connecté"))
        val obj = JSONObject().apply {
            put("user_id", _userSession.value?.id ?: "unknown")
            put("email", _userSession.value?.email ?: "")
            put("category", category)
            put("message", message)
            if (includeTech) {
                put("tech", JSONObject().apply {
                    put("plateforme", "Android Native")
                    put("version", "1.0")
                    put("famille", _currentFamily.value?.id ?: "")
                })
            }
        }
        return SupabaseApi.sendProblemReport(obj, token)
    }

    fun logout() {
        preferences?.clear()
        _userSession.value = null
        _needsOnboarding.value = false
        _families.value = emptyList()
        _currentFamily.value = null
        _currentMember.value = null
        _familyMembers.value = emptyList()
        _baseEvents.value = emptyList()
    }

    fun setBaseEventsForTesting(events: List<CalendarEvent>) {
        _baseEvents.value = events
    }

    // Expand recurring events for a given date window
    fun getExpandedEvents(startDay: CalendyDay, endDay: CalendyDay): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        val startYmd = startDay.toYmd()
        val endYmd = endDay.toYmd()

        for (e in _baseEvents.value) {
            val eventStartYmd = CalendyDateFormatter.extractYmd(e.startsAt)
            val eventEndYmd = CalendyDateFormatter.extractYmd(e.endsAt)

            if (e.recurrence.isEmpty() || e.recurrence == "none") {
                if (eventStartYmd <= endYmd && eventEndYmd >= startYmd) {
                    result.add(e)
                }
                continue
            }

            var occurrenceDay = CalendyDay.fromYmd(eventStartYmd)
            val stepDays = if (e.recurrence == "weekly") 7 else 0
            val repeatUntilYmd = e.repeatUntil

            for (step in 0..52) {
                if (occurrenceDay > endDay) break
                val occYmd = occurrenceDay.toYmd()
                if (repeatUntilYmd != null && occYmd > repeatUntilYmd) break

                if (occYmd >= startYmd && occYmd <= endYmd) {
                    val timeStart = CalendyDateFormatter.formatTime(e.startsAt)
                    val timeEnd = CalendyDateFormatter.formatTime(e.endsAt)
                    result.add(
                        e.copy(
                            startsAt = "${occYmd}T$timeStart:00Z",
                            endsAt = "${occYmd}T$timeEnd:00Z"
                        )
                    )
                }

                occurrenceDay = if (e.recurrence == "weekly") {
                    occurrenceDay.plusDays(stepDays)
                } else {
                    occurrenceDay.plusMonths(1)
                }
            }
        }
        return result
    }
}
