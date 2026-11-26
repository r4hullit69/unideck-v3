package com.unideck.app.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.local.formatDateRange
import com.unideck.app.data.prefs.GradeCutoffs
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.notifications.ReminderScheduler
import com.unideck.app.ui.UniDeckViewModel
import com.unideck.app.ui.components.SemesterEditorDialog
import com.unideck.app.ui.theme.TABULAR_NUMS
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicSettingsScreen(
    preferences: UserPreferences,
    semesters: List<SemesterEntity>,
    onBack: () -> Unit,
    onSetMinAttendance: (Int) -> Unit,
    onSetGradeCutoffs: (GradeCutoffs) -> Unit,
    onAddSemester: (String, Long?, Long?) -> Unit,
    onUpdateSemester: (SemesterEntity) -> Unit,
    onSwitchSemester: (Long) -> Unit,
    onToggleSemesterCompleted: (Long) -> Unit,
    onDeleteSemester: (SemesterEntity) -> Unit
) {
    var showAttendanceDialog by remember { mutableStateOf(false) }
    var showCutoffsDialog by remember { mutableStateOf(false) }
    var showSemestersDialog by remember { mutableStateOf(false) }

    if (showAttendanceDialog) {
        MinAttendanceTargetDialog(
            initialThreshold = preferences.minAttendanceThreshold,
            onDismiss = { showAttendanceDialog = false },
            onSave = {
                onSetMinAttendance(it)
                showAttendanceDialog = false
            }
        )
    }

    if (showCutoffsDialog) {
        GradeCutoffsDialog(
            initial = preferences.gradeCutoffs,
            onDismiss = { showCutoffsDialog = false },
            onSave = {
                onSetGradeCutoffs(it)
                showCutoffsDialog = false
            }
        )
    }

    if (showSemestersDialog) {
        ManageSemestersDialog(
            semesters = semesters,
            completedSemesterIds = preferences.completedSemesterIds,
            onDismiss = { showSemestersDialog = false },
            onAdd = onAddSemester,
            onUpdate = onUpdateSemester,
            onSwitch = onSwitchSemester,
            onToggleCompleted = onToggleSemesterCompleted,
            onDelete = onDeleteSemester
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Academic & Semesters", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("academic_settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "academic_header") {
                FlatSettingsSectionHeader("Academic")
            }

            item(key = "row_min_attendance") {
                FlatSettingsRow(
                    icon = Icons.Filled.Percent,
                    title = "Minimum attendance target",
                    subtitle = "${preferences.minAttendanceThreshold}% • Used for safe-bunk and recovery math",
                    onClick = { showAttendanceDialog = true },
                    modifier = Modifier.testTag("open_min_attendance_dialog"),
                    trailing = {
                        Text(
                            text = "${preferences.minAttendanceThreshold}%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            item(key = "row_grade_cutoffs") {
                val c = preferences.gradeCutoffs
                FlatSettingsRow(
                    icon = Icons.Filled.Grade,
                    title = "10-point grade cutoffs",
                    subtitle = "A+:${c.aPlusMin}+ • A:${c.aMin}+ • B:${c.bMin}+ • C:${c.cMin}+ • D:${c.dMin}+ • E:${c.eMin}+",
                    onClick = { showCutoffsDialog = true },
                    modifier = Modifier.testTag("open_grade_cutoffs_dialog")
                )
            }

            item(key = "semesters_header") {
                FlatSettingsSectionHeader("Semesters")
            }

            item(key = "row_manage_semesters") {
                FlatSettingsRow(
                    icon = Icons.Filled.FolderCopy,
                    title = "Manage semesters",
                    subtitle = "${semesters.size} ${if (semesters.size == 1) "semester" else "semesters"} • Start new, edit dates, or archive",
                    onClick = { showSemestersDialog = true },
                    modifier = Modifier.testTag("open_manage_semesters")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSettingsScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSetReminderLeadMinutes: (Int) -> Unit,
    onSetUse24Hour: (Boolean) -> Unit,
    onSetLiveClassNotificationEnabled: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showReminderDialog by remember { mutableStateOf(false) }
    var pendingLeadMinutes by remember { mutableStateOf<Int?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val minutes = pendingLeadMinutes
        pendingLeadMinutes = null
        if (granted && minutes != null) {
            onSetReminderLeadMinutes(minutes)
        } else if (!granted) {
            onSetReminderLeadMinutes(0)
        }
    }

    if (showReminderDialog) {
        val options = listOf(
            0 to "Off",
            5 to "5 minutes before",
            10 to "10 minutes before",
            15 to "15 minutes before",
            30 to "30 minutes before"
        )
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Class reminders", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    options.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showReminderDialog = false
                                    if (mins == 0) {
                                        onSetReminderLeadMinutes(0)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !ReminderScheduler.hasNotificationPermission(context)
                                    ) {
                                        pendingLeadMinutes = mins
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onSetReminderLeadMinutes(mins)
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                                .testTag("reminder_lead_$mins"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.reminderLeadMinutes == mins,
                                onClick = {
                                    showReminderDialog = false
                                    if (mins == 0) {
                                        onSetReminderLeadMinutes(0)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !ReminderScheduler.hasNotificationPermission(context)
                                    ) {
                                        pendingLeadMinutes = mins
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onSetReminderLeadMinutes(mins)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReminderDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders & Time", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reminders_settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "reminders_header") {
                FlatSettingsSectionHeader("Notifications")
            }

            item(key = "row_class_reminders") {
                val leadText = if (preferences.reminderLeadMinutes <= 0) {
                    "Off • Tap to enable offline class alerts"
                } else {
                    "${preferences.reminderLeadMinutes} min before class • Local AlarmManager alerts"
                }
                FlatSettingsRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Class reminder lead time",
                    subtitle = leadText,
                    onClick = { showReminderDialog = true },
                    modifier = Modifier.testTag("open_reminder_lead_dialog")
                )
            }

            item(key = "row_live_class_notification") {
                FlatSettingsSwitchRow(
                    icon = Icons.Filled.Schedule,
                    title = "Live ongoing-class notification",
                    subtitle = "Show real-time progress bar while a scheduled class is in session",
                    checked = preferences.liveClassNotificationEnabled,
                    onCheckedChange = onSetLiveClassNotificationEnabled,
                    switchTestTag = "live_class_notification_switch"
                )
            }

            item(key = "time_header") {
                FlatSettingsSectionHeader("Time")
            }

            item(key = "row_24h") {
                FlatSettingsSwitchRow(
                    icon = Icons.Filled.AccessTime,
                    title = "24-hour time format",
                    subtitle = if (preferences.use24HourFormat) "Showing 14:30 format" else "Showing 2:30 PM format",
                    checked = preferences.use24HourFormat,
                    onCheckedChange = onSetUse24Hour,
                    switchTestTag = "time_format_switch"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    preferences: UserPreferences,
    isBackingUpNow: Boolean,
    onBack: () -> Unit,
    onSetAutoBackupEnabled: (Boolean) -> Unit,
    onAutoBackupFolderSelected: (Uri) -> Unit,
    onRunAutoBackupNow: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit
) {
    val hasFolder = !preferences.autoBackupFolderUri.isNullOrBlank()
    val lastBackupText = remember(preferences.lastAutoBackupTimestamp, preferences.use24HourFormat) {
        val ts = preferences.lastAutoBackupTimestamp
        if (ts == null) {
            "Never"
        } else {
            val pattern = if (preferences.use24HourFormat) "MMM d, HH:mm" else "MMM d, h:mm a"
            Instant.ofEpochMilli(ts)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern))
        }
    }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            onAutoBackupFolderSelected(treeUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("backup_settings_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "auto_backup_header") {
                FlatSettingsSectionHeader("Automatic Local Backup")
            }

            item(key = "row_auto_backup_switch") {
                FlatSettingsSwitchRow(
                    icon = Icons.Filled.Schedule,
                    title = "Auto daily backup",
                    subtitle = "Rolling 3-day offline JSON backup every 24 hours",
                    checked = preferences.autoBackupEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasFolder) {
                            backupFolderLauncher.launch(null)
                        } else {
                            onSetAutoBackupEnabled(enabled)
                        }
                    },
                    switchTestTag = "auto_backup_switch"
                )
            }

            item(key = "row_backup_folder") {
                FlatSettingsRow(
                    icon = Icons.Filled.Folder,
                    title = "Backup folder",
                    subtitle = preferences.autoBackupFolderName ?: "Tap to choose a local folder",
                    onClick = { backupFolderLauncher.launch(null) },
                    modifier = Modifier.testTag("auto_backup_folder_row"),
                    trailing = {
                        Text(
                            text = if (hasFolder) "Change" else "Choose",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            item(key = "row_backup_now") {
                FlatSettingsRow(
                    icon = Icons.Filled.Backup,
                    title = if (isBackingUpNow) "Saving backup…" else "Back up now",
                    subtitle = "Last backed up: $lastBackupText",
                    enabled = hasFolder && !isBackingUpNow,
                    onClick = onRunAutoBackupNow,
                    modifier = Modifier.testTag("backup_now_button")
                )
            }

            item(key = "manual_backup_header") {
                FlatSettingsSectionHeader("Manual JSON Backup")
            }

            item(key = "row_export_json") {
                FlatSettingsRow(
                    icon = Icons.Filled.Download,
                    title = "Export JSON",
                    subtitle = "Save all semesters, courses, timetable slots, logs, and settings",
                    onClick = onExportJson,
                    modifier = Modifier.testTag("export_json_button")
                )
            }

            item(key = "row_import_json") {
                FlatSettingsRow(
                    icon = Icons.Filled.Upload,
                    title = "Import JSON",
                    subtitle = "Restore from a previously saved UniDeck backup file",
                    onClick = onImportJson,
                    modifier = Modifier.testTag("import_json_button")
                )
            }
        }
    }
}

@Composable
private fun MinAttendanceTargetDialog(
    initialThreshold: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember(initialThreshold) { mutableIntStateOf(initialThreshold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Minimum Attendance Target", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { value = (value - 1).coerceAtLeast(50) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("min_att_decrement")
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease target")
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        text = "$value%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFeatureSettings = TABULAR_NUMS
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    FilledTonalIconButton(
                        onClick = { value = (value + 1).coerceAtMost(99) },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("min_att_increment")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase target")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(65, 75, 80, 85).forEach { preset ->
                        FilterChip(
                            selected = value == preset,
                            onClick = { value = preset },
                            label = {
                                Text(
                                    text = "$preset%",
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(value) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun GradeCutoffsDialog(
    initial: GradeCutoffs,
    onDismiss: () -> Unit,
    onSave: (GradeCutoffs) -> Unit
) {
    var aPlusMin by remember { mutableIntStateOf(initial.aPlusMin) }
    var aMin by remember { mutableIntStateOf(initial.aMin) }
    var bMin by remember { mutableIntStateOf(initial.bMin) }
    var cMin by remember { mutableIntStateOf(initial.cMin) }
    var dMin by remember { mutableIntStateOf(initial.dMin) }
    var eMin by remember { mutableIntStateOf(initial.eMin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("10-Point Grade Cutoffs", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CutoffStepperRow("A+ (10 pts)", aPlusMin, { aPlusMin = (aPlusMin - 1).coerceAtLeast(aMin + 1) }, { aPlusMin = (aPlusMin + 1).coerceAtMost(100) })
                CutoffStepperRow("A (9 pts)", aMin, { aMin = (aMin - 1).coerceAtLeast(bMin + 1) }, { aMin = (aMin + 1).coerceAtMost(aPlusMin - 1) })
                CutoffStepperRow("B (8 pts)", bMin, { bMin = (bMin - 1).coerceAtLeast(cMin + 1) }, { bMin = (bMin + 1).coerceAtMost(aMin - 1) })
                CutoffStepperRow("C (7 pts)", cMin, { cMin = (cMin - 1).coerceAtLeast(dMin + 1) }, { cMin = (cMin + 1).coerceAtMost(bMin - 1) })
                CutoffStepperRow("D (6 pts)", dMin, { dMin = (dMin - 1).coerceAtLeast(eMin + 1) }, { dMin = (dMin + 1).coerceAtMost(cMin - 1) })
                CutoffStepperRow("E (5 pts)", eMin, { eMin = (eMin - 1).coerceAtLeast(1) }, { eMin = (eMin + 1).coerceAtMost(dMin - 1) })
                TextButton(
                    onClick = {
                        val def = GradeCutoffs()
                        aPlusMin = def.aPlusMin; aMin = def.aMin; bMin = def.bMin
                        cMin = def.cMin; dMin = def.dMin; eMin = def.eMin
                    }
                ) { Text("Reset to Defaults (90/80/70/60/50/40)") }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(GradeCutoffs(aPlusMin, aMin, bMin, cMin, dMin, eMin)) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CutoffStepperRow(
    label: String,
    value: Int,
    onDec: () -> Unit,
    onInc: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onDec) { Text("-") }
            Text(
                text = "≥ $value%",
                style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = TABULAR_NUMS),
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = onInc) { Text("+") }
        }
    }
}

@Composable
internal fun ManageSemestersDialog(
    semesters: List<SemesterEntity>,
    completedSemesterIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (String, Long?, Long?) -> Unit,
    onUpdate: (SemesterEntity) -> Unit,
    onSwitch: (Long) -> Unit,
    onToggleCompleted: (Long) -> Unit,
    onDelete: (SemesterEntity) -> Unit
) {
    var showAddEditor by remember { mutableStateOf(false) }
    var editingSemester by remember { mutableStateOf<SemesterEntity?>(null) }

    if (showAddEditor) {
        SemesterEditorDialog(
            initial = null,
            defaultName = "Sem ${semesters.size + 1}",
            onDismiss = { showAddEditor = false },
            onSave = { name, start, end ->
                onAdd(name, start, end)
                showAddEditor = false
            }
        )
    }

    editingSemester?.let { existing ->
        SemesterEditorDialog(
            initial = existing,
            defaultName = existing.name,
            onDismiss = { editingSemester = null },
            onSave = { name, start, end ->
                onUpdate(
                    existing.copy(
                        name = name,
                        startDateEpochDay = start,
                        endDateEpochDay = end
                    )
                )
                editingSemester = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Semesters", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = { showAddEditor = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_semesters_add_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start New Semester")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

                semesters.forEach { sem ->
                    val isManuallyCompleted = sem.id in completedSemesterIds
                    val isArchived = UniDeckViewModel.isSemesterArchived(sem, completedSemesterIds)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSwitch(sem.id) }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = sem.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (sem.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (sem.isCurrent) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Active",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (isArchived) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                                    ) {
                                        Text(
                                            text = if (isManuallyCompleted) "Completed" else "Archived",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = sem.formatDateRange(),
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onToggleCompleted(sem.id) },
                                modifier = Modifier.testTag("toggle_complete_semester_${sem.id}")
                            ) {
                                Icon(
                                    imageVector = if (isManuallyCompleted) Icons.Filled.LockOpen else Icons.Filled.CheckCircle,
                                    contentDescription = if (isManuallyCompleted) "Reopen ${sem.name}" else "Mark ${sem.name} completed",
                                    tint = if (isManuallyCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { editingSemester = sem },
                                modifier = Modifier.testTag("edit_semester_${sem.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit ${sem.name}",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (semesters.size > 1) {
                                IconButton(
                                    onClick = { onDelete(sem) },
                                    modifier = Modifier.testTag("delete_semester_${sem.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete ${sem.name}",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
