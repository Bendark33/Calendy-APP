package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Family
import com.example.model.FamilyMember
import com.example.model.NotificationPrefs
import com.example.ui.theme.MemberColorPalette

@Composable
fun ProfileDialog(
    member: FamilyMember?,
    family: Family?,
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, colorHex: String, familyName: String?) -> Unit,
    onRenewCode: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenAuth: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(member?.displayName ?: "") }
    var selectedColor by remember { mutableStateOf(member?.color ?: "#3B82F6") }
    var familyName by remember { mutableStateOf(family?.name ?: "") }
    var copiedFeedback by remember { mutableStateOf(false) }

    val isAdmin = member?.role in listOf("owner", "admin")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mon profil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Apparence : Mode Clair / Mode Sombre
                Text("Apparence", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggleTheme(false) }
                            .testTag("theme_light_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "☀️ Clair",
                                fontWeight = FontWeight.Bold,
                                color = if (!isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggleTheme(true) }
                            .testTag("theme_dark_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌙 Sombre",
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Prénom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Ma couleur dans le calendrier", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MemberColorPalette.forEach { color ->
                        val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Family code box
                val code = family?.inviteCode ?: "------"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Code d'invitation famille", fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = code, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 2.sp)
                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Code famille", code))
                                        copiedFeedback = true
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copier")
                                }
                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Rejoins notre famille sur Calendy avec le code d'invitation : $code"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Partager la famille"))
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Partager")
                                }
                            }
                        }
                        if (copiedFeedback) {
                            Text("Code copié dans le presse-papiers !", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text("Nom de la famille") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onRenewCode,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Renouveler le code d'invitation")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rappels", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onOpenReport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Signaler", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenAuth,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mon compte (Connexion / Inscription)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Déconnexion")
                    }

                    Button(
                        onClick = { onSaveProfile(displayName, selectedColor, if (isAdmin) familyName else null) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsDialog(
    prefs: NotificationPrefs,
    onDismiss: () -> Unit,
    onSave: (NotificationPrefs) -> Unit
) {
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var notifyNew by remember { mutableStateOf(prefs.notifyNew) }
    var notifyChanges by remember { mutableStateOf(prefs.notifyChanges) }
    var onlyImportant by remember { mutableStateOf(prefs.onlyImportant) }
    var reminderOffsets by remember { mutableStateOf(prefs.reminderOffsets.toSet()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Notifications et Rappels", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Activer les notifications")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notifyNew, onCheckedChange = { notifyNew = it }, enabled = enabled)
                    Text("Nouvel événement créé")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notifyChanges, onCheckedChange = { notifyChanges = it }, enabled = enabled)
                    Text("Événement modifié")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = onlyImportant, onCheckedChange = { onlyImportant = it }, enabled = enabled)
                    Text("Seulement les événements importants (★)")
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Rappels avant l'événement", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                val offsetOptions = listOf(60 to "1 heure", 1440 to "1 jour", 10080 to "1 semaine", 43200 to "1 mois")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    offsetOptions.forEach { (mins, label) ->
                        val selected = reminderOffsets.contains(mins)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                reminderOffsets = if (selected) reminderOffsets - mins else reminderOffsets + mins
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Fermer") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                prefs.copy(
                                    enabled = enabled,
                                    notifyNew = notifyNew,
                                    notifyChanges = notifyChanges,
                                    onlyImportant = onlyImportant,
                                    reminderOffsets = reminderOffsets.toList()
                                )
                            )
                        }
                    ) { Text("Enregistrer") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (category: String, message: String, tech: Boolean) -> Unit
) {
    var category by remember { mutableStateOf("bug") }
    var message by remember { mutableStateOf("") }
    var includeTech by remember { mutableStateOf(true) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Signaler un problème", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                    OutlinedTextField(
                        value = when (category) {
                            "bug" -> "Un problème (bug)"
                            "suggestion" -> "Une idée ou suggestion"
                            else -> "Autre"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                        DropdownMenuItem(text = { Text("Un problème (bug)") }, onClick = { category = "bug"; dropdownExpanded = false })
                        DropdownMenuItem(text = { Text("Une idée ou suggestion") }, onClick = { category = "suggestion"; dropdownExpanded = false })
                        DropdownMenuItem(text = { Text("Autre") }, onClick = { category = "autre"; dropdownExpanded = false })
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Décris ce qui se passe") },
                    placeholder = { Text("Que faisais-tu ? Que s'est-il passé ?") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeTech, onCheckedChange = { includeTech = it })
                    Text("Joindre des infos techniques (modèle, version)", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Fermer") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (message.length >= 5) onSubmit(category, message, includeTech) },
                        enabled = message.length >= 5
                    ) { Text("Envoyer") }
                }
            }
        }
    }
}

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onLogin: (email: String, pass: String) -> Unit,
    onSignUp: (email: String, pass: String) -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onCreateFamily: (name: String, displayName: String, color: String) -> Unit,
    onJoinFamily: (code: String, displayName: String, color: String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var familyName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Connexion") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Inscription") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Famille") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) { err = "Remplis tous les champs"; return@Button }
                                onLogin(email.trim(), password)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Se connecter") }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onGoogleSignIn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continuer avec Google")
                        }
                    }
                    1 -> {
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe (6 car. min)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirmer le mot de passe") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (password != confirmPassword) { err = "Les mots de passe ne correspondent pas"; return@Button }
                                onSignUp(email.trim(), password)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Créer mon compte") }
                    }
                    2 -> {
                        OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Ton prénom") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = familyName, onValueChange = { familyName = it }, label = { Text("Nom de la famille (création)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onCreateFamily(familyName.ifBlank { "Ma famille" }, displayName.ifBlank { "Moi" }, "#3B82F6") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Créer une nouvelle famille") }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(value = inviteCode, onValueChange = { inviteCode = it }, label = { Text("Code d'invitation (rejoindre)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onJoinFamily(inviteCode.trim(), displayName.ifBlank { "Moi" }, "#EC4899") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Rejoindre la famille avec ce code") }
                    }
                }

                if (err != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onDismiss) { Text("Fermer") }
                }
            }
        }
    }
}
