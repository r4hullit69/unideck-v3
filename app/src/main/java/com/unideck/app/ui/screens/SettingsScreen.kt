package com.unideck.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAcademic: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenBackup: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            item(key = "settings_row_appearance") {
                FlatSettingsRow(
                    icon = Icons.Filled.Palette,
                    title = "Appearance",
                    subtitle = "Theme, pure black, accent color, app icon, and font",
                    onClick = onOpenAppearance,
                    modifier = Modifier.testTag("settings_row_appearance")
                )
            }

            item(key = "settings_row_academic") {
                FlatSettingsRow(
                    icon = Icons.Filled.School,
                    title = "Academic & Semesters",
                    subtitle = "Attendance target, grade cutoffs, and semester dates",
                    onClick = onOpenAcademic,
                    modifier = Modifier.testTag("settings_row_academic")
                )
            }

            item(key = "settings_row_reminders") {
                FlatSettingsRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Reminders & Time",
                    subtitle = "Class alerts, deadline reminders, and 12h/24h format",
                    onClick = onOpenReminders,
                    modifier = Modifier.testTag("settings_row_reminders")
                )
            }

            item(key = "settings_row_backup") {
                FlatSettingsRow(
                    icon = Icons.Filled.Backup,
                    title = "Backup & Restore",
                    subtitle = "Manual JSON export/import and auto daily backup",
                    onClick = onOpenBackup,
                    modifier = Modifier.testTag("settings_row_backup")
                )
            }
        }
    }
}
