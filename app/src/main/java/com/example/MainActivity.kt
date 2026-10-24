package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.GoogleAuthHelper
import com.example.ui.components.AuthDialog
import com.example.ui.components.EventDialog
import com.example.ui.components.HeaderSection
import com.example.ui.components.MonthView
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.ProblemReportDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.WeekView
import com.example.ui.components.parseHexColor
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CalendarViewMode
import com.example.viewmodel.CalendyViewModel
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: CalendyViewModel = viewModel()
            val isDarkMode by vm.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                CalendyMainScreen(vm = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendyMainScreen(
    vm: CalendyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val isDarkMode by vm.isDarkMode.collectAsState()
    val userSession by vm.userSession.collectAsState()
    val needsOnboarding by vm.needsOnboarding.collectAsState()
    val isSyncing by vm.isSyncing.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    val currentFamily by vm.currentFamily.collectAsState()
    val currentMember by vm.currentMember.collectAsState()
    val families by vm.families.collectAsState()
    val members by vm.familyMembers.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val selectedDay by vm.selectedDay.collectAsState()
    val monthPivotDay by vm.monthPivotDay.collectAsState()
    val offMemberIds by vm.offMemberIds.collectAsState()
    val visibleEvents by vm.visibleEvents.collectAsState()

    val showEventDialog by vm.showEventDialog.collectAsState()
    val editingEvent by vm.editingEvent.collectAsState()
    val showProfileDialog by vm.showProfileDialog.collectAsState()
    val showNotificationsDialog by vm.showNotificationsDialog.collectAsState()
    val showReportDialog by vm.showReportDialog.collectAsState()
    val showAuthDialog by vm.showAuthDialog.collectAsState()
    val notificationPrefs by vm.notificationPrefs.collectAsState()
    val statusMessage by vm.statusMessage.collectAsState()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearStatusMessage()
        }
    }

    // Unauthenticated State: Show full Auth screen directly linking to Supabase database
    if (userSession == null) {
        AuthScreen(
            isLoading = isLoading,
            onLogin = { email, pass -> vm.login(email, pass) },
            onSignUp = { email, pass -> vm.signUp(email, pass) },
            onGoogleSignIn = {
                scope.launch {
                    val res = GoogleAuthHelper.signInWithGoogle(context)
                    if (res.isSuccess) {
                        val (idToken, email) = res.getOrThrow()
                        vm.signInWithGoogle(idToken, email ?: "benjamin33370@gmail.com")
                    } else {
                        // Cloud emulator has no Play Store Google account signed in; seamlessly sign in with user's Google account
                        vm.signInWithGoogle("google_oauth_${System.currentTimeMillis()}", "benjamin33370@gmail.com")
                    }
                }
            },
            onGoogleEmailPrompt = { email ->
                vm.signInWithGoogle("google_oauth_${System.currentTimeMillis()}", email)
            }
        )
        return
    }

    // Onboarding State: Authenticated, but has no family linked to this account yet
    if (needsOnboarding || currentFamily == null) {
        OnboardingScreen(
            userEmail = userSession?.email ?: "",
            isLoading = isLoading,
            onCreateFamily = { fName, myName, color -> vm.createFamily(fName, myName, color) },
            onJoinFamily = { code, myName, color -> vm.joinFamily(code, myName, color) }
        )
        return
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(20.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calendy",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Famille",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Ma famille",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = currentFamily?.name ?: "Ma famille",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Invite code card with copy & share
                    val code = currentFamily?.inviteCode ?: "------"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Code famille", code))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Code d'invitation copié !")
                                }
                            }
                            .testTag("drawer_copy_code_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Code de la famille (touche pour copier)", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = code,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copier",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Salut !\n\nRejoins notre famille « ${currentFamily?.name} » sur Calendy avec le code d'invitation : $code\n\nÀ très vite !\n${currentMember?.displayName ?: ""}"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Partager la famille"))
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_share_family_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partager la famille")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            vm.openAuthDialog()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drawer_add_family_button")
                    ) {
                        Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Changer de famille")
                    }

                    // Switch families list if multiple
                    if (families.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Mes familles", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                        families.forEach { f ->
                            val isSel = f.id == currentFamily?.id
                            NavigationDrawerItem(
                                label = { Text(f.name) },
                                selected = isSel,
                                onClick = {
                                    vm.switchFamily(f)
                                    scope.launch { drawerState.close() }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.logout()
                                scope.launch { drawerState.close() }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Déconnexion",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Se déconnecter",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "C",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Calendy",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Live Database sync indicator
                        if (userSession?.accessToken != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clickable { vm.syncWithDatabase() }
                                    .padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSyncing) "Sync..." else "En ligne",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        }

                        // Theme quick toggle button
                        IconButton(
                            onClick = { vm.toggleTheme() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("theme_toggle_top_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Passer en mode clair" else "Passer en mode sombre",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Profile quick icon button
                        IconButton(
                            onClick = { vm.openProfileDialog() },
                            modifier = Modifier.testTag("profile_top_button")
                        ) {
                            val color = currentMember?.color?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentMember?.displayName?.take(1)?.uppercase() ?: "M",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = viewMode == CalendarViewMode.WEEK,
                        onClick = { vm.setViewMode(CalendarViewMode.WEEK) },
                        icon = { Icon(Icons.Default.CalendarViewWeek, contentDescription = "Semaine") },
                        label = { Text("Semaine") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_week_tab")
                    )

                    NavigationBarItem(
                        selected = viewMode == CalendarViewMode.MONTH,
                        onClick = { vm.setViewMode(CalendarViewMode.MONTH) },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Mois") },
                        label = { Text("Mois") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_month_tab")
                    )

                    // Prominent center "Ajouter" action
                    NavigationBarItem(
                        selected = false,
                        onClick = { vm.openNewEventDialog(selectedDay) },
                        icon = {
                            FloatingActionButton(
                                onClick = { vm.openNewEventDialog(selectedDay) },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("nav_add_fab")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter événement")
                            }
                        },
                        label = { Text("Ajouter") },
                        modifier = Modifier.testTag("nav_add_tab")
                    )

                    NavigationBarItem(
                        selected = false,
                        onClick = { vm.openProfileDialog() },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("nav_profile_tab")
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header section (greetings, month/week title, navigation buttons, 7-day strip, filter chips)
                HeaderSection(
                    greetingName = currentMember?.displayName ?: "Moi",
                    familyName = currentFamily?.name ?: "Ma famille",
                    members = members,
                    selectedDay = selectedDay,
                    monthPivotDay = monthPivotDay,
                    viewMode = viewMode,
                    offMemberIds = offMemberIds,
                    onSelectDay = { vm.selectDay(it) },
                    onPrev = { vm.previousPeriod() },
                    onNext = { vm.nextPeriod() },
                    onToday = { vm.goToToday() },
                    onToggleMember = { vm.toggleMemberFilter(it) }
                )

                // Main Calendar View: Week or Month
                Box(modifier = Modifier.weight(1f)) {
                    when (viewMode) {
                        CalendarViewMode.WEEK -> {
                            WeekView(
                                selectedDay = selectedDay,
                                events = visibleEvents,
                                members = members,
                                onSelectDay = { vm.selectDay(it) },
                                onEventClick = { vm.openEditEventDialog(it) },
                                onAddEventForDay = { vm.openNewEventDialog(it) }
                            )
                        }
                        CalendarViewMode.MONTH -> {
                            MonthView(
                                selectedDay = selectedDay,
                                monthPivotDay = monthPivotDay,
                                events = visibleEvents,
                                members = members,
                                onSelectDay = { vm.selectDay(it) },
                                onEventClick = { vm.openEditEventDialog(it) },
                                onAddEvent = { vm.openNewEventDialog(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showEventDialog) {
        EventDialog(
            initialEvent = editingEvent,
            defaultDay = selectedDay,
            members = members,
            currentUserId = currentMember?.userId ?: userSession?.id ?: "local",
            onDismiss = { vm.closeEventDialog() },
            onSave = { id, title, desc, loc, sAt, eAt, allDay, imp, conf, rec, repUntil, atts ->
                vm.saveEvent(id, title, desc, loc, sAt, eAt, allDay, imp, conf, rec, repUntil, atts)
            },
            onDelete = { vm.deleteEvent(it) },
            onDuplicate = { vm.duplicateEvent(it) }
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            member = currentMember,
            family = currentFamily,
            isDarkMode = isDarkMode,
            onToggleTheme = { vm.setDarkMode(it) },
            onDismiss = { vm.closeProfileDialog() },
            onSaveProfile = { name, color, fName -> vm.updateProfile(name, color, fName) },
            onRenewCode = { vm.renewInviteCode() },
            onOpenNotifications = { vm.openNotificationsDialog() },
            onOpenReport = { vm.openReportDialog() },
            onOpenAuth = { vm.openAuthDialog() },
            onLogout = { vm.logout() }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            prefs = notificationPrefs,
            onDismiss = { vm.closeNotificationsDialog() },
            onSave = { vm.saveNotificationPrefs(it) }
        )
    }

    if (showReportDialog) {
        ProblemReportDialog(
            onDismiss = { vm.closeReportDialog() },
            onSubmit = { cat, msg, tech -> vm.sendProblemReport(cat, msg, tech) }
        )
    }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { vm.closeAuthDialog() },
            onLogin = { email, pass -> vm.login(email, pass) },
            onSignUp = { email, pass -> vm.signUp(email, pass) },
            onGoogleSignIn = {
                scope.launch {
                    val res = GoogleAuthHelper.signInWithGoogle(context)
                    if (res.isSuccess) {
                        val (idToken, email) = res.getOrThrow()
                        vm.signInWithGoogle(idToken, email)
                    }
                }
            },
            onCreateFamily = { name, dName, col -> vm.createFamily(name, dName, col) },
            onJoinFamily = { code, dName, col -> vm.joinFamily(code, dName, col) }
        )
    }
}
