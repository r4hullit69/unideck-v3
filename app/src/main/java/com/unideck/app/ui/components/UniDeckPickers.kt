package com.unideck.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Material 3 (Material You) Time Picker Dialog with Clock Dial & Keyboard Input toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniDeckTimePickerDialog(
    title: String,
    initialMinutes: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (minutesFromMidnight: Int) -> Unit
) {
    val safeMinutes = initialMinutes.coerceIn(0, 1439)
    val state = rememberTimePickerState(
        initialHour = safeMinutes / 60,
        initialMinute = safeMinutes % 60,
        is24Hour = is24Hour
    )
    var useKeyboardInput by remember { mutableStateOf(false) }

    val pickerColors = TimePickerDefaults.colors(
        clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        selectorColor = MaterialTheme.colorScheme.primary,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .testTag("unideck_time_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                )

                if (useKeyboardInput) {
                    TimeInput(
                        state = state,
                        colors = pickerColors
                    )
                } else {
                    TimePicker(
                        state = state,
                        colors = pickerColors
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { useKeyboardInput = !useKeyboardInput },
                        modifier = Modifier.testTag("time_picker_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (useKeyboardInput) Icons.Filled.Schedule else Icons.Filled.Keyboard,
                            contentDescription = if (useKeyboardInput) "Switch to clock dial" else "Switch to keyboard input",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("time_picker_cancel")
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = { onConfirm(state.hour * 60 + state.minute) },
                            modifier = Modifier.testTag("time_picker_confirm")
                        ) {
                            Text(
                                text = "OK",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Material 3 (Material You) Date Picker Dialog accepting/returning LocalDate epochDay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniDeckDatePickerDialog(
    initialEpochDay: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val initialMillis = remember(initialEpochDay) {
        val date = initialEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(picked.toEpochDay())
                    }
                    onDismiss()
                },
                enabled = pickerState.selectedDateMillis != null,
                modifier = Modifier.testTag("date_picker_confirm")
            ) {
                Text("Select", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("date_picker_cancel")
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = pickerState,
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/**
 * Tappable Material You time pill that opens [UniDeckTimePickerDialog].
 */
@Composable
fun TimePickerPill(
    label: String,
    formattedTime: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTagName: String = ""
) {
    val ext = LocalUniDeckColors.current
    val shape = RoundedCornerShape(14.dp)

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .then(if (testTagName.isNotEmpty()) Modifier.testTag(testTagName) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = TABULAR_NUMS
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = "Pick $label",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Optional date picker field row with calendar icon and inline clear (×) button.
 */
@Composable
fun OptionalDatePickerRow(
    label: String,
    epochDay: Long?,
    onPickClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    testTagPrefix: String = "date_field"
) {
    val ext = LocalUniDeckColors.current
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
    val formatted = remember(epochDay) {
        epochDay?.let { LocalDate.ofEpochDay(it).format(dateFmt) }
    }
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (isError) MaterialTheme.colorScheme.error else ext.cardBorder

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .clickable(onClick = onPickClick)
            .testTag("${testTagPrefix}_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = formatted ?: "Optional",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (formatted != null) FontWeight.SemiBold else FontWeight.Normal,
                            fontFeatureSettings = TABULAR_NUMS
                        ),
                        color = if (formatted != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (epochDay != null) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("${testTagPrefix}_clear")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear $label",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Shared Add / Edit Semester dialog with optional Start Date & End Date Material You pickers
 * and validation enforcing endDateEpochDay >= startDateEpochDay.
 */
@Composable
fun SemesterEditorDialog(
    initial: SemesterEntity?,
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (name: String, startDateEpochDay: Long?, endDateEpochDay: Long?) -> Unit
) {
    var name by remember(initial, defaultName) { mutableStateOf(initial?.name ?: defaultName) }
    var startDate by remember(initial) { mutableStateOf(initial?.startDateEpochDay) }
    var endDate by remember(initial) { mutableStateOf(initial?.endDateEpochDay) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }

    val hasInvalidRange = startDate != null && endDate != null && endDate!! < startDate!!
    val canSave = name.isNotBlank() && !hasInvalidRange

    if (pickingStart) {
        UniDeckDatePickerDialog(
            initialEpochDay = startDate ?: endDate ?: LocalDate.now().toEpochDay(),
            onDismiss = { pickingStart = false },
            onDateSelected = { picked ->
                startDate = picked
                pickingStart = false
            }
        )
    }

    if (pickingEnd) {
        UniDeckDatePickerDialog(
            initialEpochDay = endDate ?: startDate ?: LocalDate.now().toEpochDay(),
            onDismiss = { pickingEnd = false },
            onDateSelected = { picked ->
                endDate = picked
                pickingEnd = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Semester" else "Edit Semester",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Semester Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("semester_name_input")
                )

                Text(
                    text = "Semester Dates (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Recurring classes and reminders will only appear within this window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OptionalDatePickerRow(
                    label = "Start Date",
                    epochDay = startDate,
                    onPickClick = { pickingStart = true },
                    onClearClick = { startDate = null },
                    isError = hasInvalidRange,
                    testTagPrefix = "semester_start_date"
                )

                OptionalDatePickerRow(
                    label = "End Date",
                    epochDay = endDate,
                    onPickClick = { pickingEnd = true },
                    onClearClick = { endDate = null },
                    isError = hasInvalidRange,
                    testTagPrefix = "semester_end_date"
                )

                if (hasInvalidRange) {
                    Text(
                        text = "End date must be on or after start date.",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("semester_date_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(name.trim(), startDate, endDate)
                        onDismiss()
                    }
                },
                enabled = canSave,
                modifier = Modifier.testTag("semester_editor_save")
            ) {
                Text(if (initial == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
