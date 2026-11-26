package com.unideck.app.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.ui.components.ArchivedSemesterBanner
import com.unideck.app.ui.components.ExpressiveGroupedCard
import com.unideck.app.ui.components.ProfessorAutocompleteField
import com.unideck.app.ui.components.TimePickerPill
import com.unideck.app.ui.components.UniDeckTimePickerDialog
import com.unideck.app.ui.components.UniformCardShape
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    semesterId: Long,
    semesterName: String,
    courses: List<CourseEntity>,
    schedules: List<ClassScheduleEntity>,
    savedProfessors: List<String>,
    preferences: UserPreferences,
    pdfFile: File?,
    pdfVersion: Int,
    isReadOnly: Boolean = false,
    isEmbeddedInHome: Boolean = false,
    onUnlockArchived: () -> Unit = {},
    onBack: () -> Unit,
    onSaveSchedule: (ClassScheduleEntity) -> Unit,
    onDeleteSchedule: (ClassScheduleEntity) -> Unit,
    onUpdateCourseProfessor: (Long, String) -> Unit = { _, _ -> },
    onImportPdf: (Uri) -> Unit,
    onRemovePdf: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedDay by remember {
        val todayIso = LocalDate.now().dayOfWeek.value
        mutableIntStateOf(if (todayIso in 1..6) todayIso else 1)
    }
    var editingSchedule by remember { mutableStateOf<ClassScheduleEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportPdf(uri)
    }

    val coursesById = remember(courses) { courses.associateBy { it.id } }
    val daySlots = remember(schedules, selectedDay) {
        schedules.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startTime }
    }

    if (!isReadOnly && (showAddDialog || editingSchedule != null)) {
        ScheduleEditorDialog(
            initial = editingSchedule,
            defaultDay = selectedDay,
            semesterId = semesterId,
            courses = courses,
            savedProfessors = savedProfessors,
            use24Hour = preferences.use24HourFormat,
            onDismiss = {
                showAddDialog = false
                editingSchedule = null
            },
            onSave = { entity, professor ->
                onUpdateCourseProfessor(entity.courseId, professor)
                onSaveSchedule(entity)
                showAddDialog = false
                editingSchedule = null
            },
            onDelete = editingSchedule?.let { existing ->
                {
                    onDeleteSchedule(existing)
                    editingSchedule = null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (!isEmbeddedInHome) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Timetable", style = MaterialTheme.typography.titleLarge)
                            Text(
                                semesterName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("timetable_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        floatingActionButton = {
            if (!isReadOnly && selectedTab == 0 && courses.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Class") },
                    modifier = Modifier.testTag("add_schedule_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isReadOnly && !isEmbeddedInHome) {
                ArchivedSemesterBanner(
                    onUnlock = onUnlockArchived,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("In-App Timetable") },
                    modifier = Modifier.testTag("timetable_tab_grid")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Timetable PDF") },
                    modifier = Modifier.testTag("timetable_tab_pdf")
                )
            }

            if (selectedTab == 0) {
                InAppTimetableTab(
                    selectedDay = selectedDay,
                    onSelectDay = { selectedDay = it },
                    courses = courses,
                    coursesById = coursesById,
                    daySlots = daySlots,
                    use24Hour = preferences.use24HourFormat,
                    isReadOnly = isReadOnly,
                    onEditSlot = { if (!isReadOnly) editingSchedule = it }
                )
            } else {
                OfflinePdfViewerTab(
                    pdfFile = pdfFile,
                    pdfVersion = pdfVersion,
                    isReadOnly = isReadOnly,
                    onPickPdf = { pdfLauncher.launch(arrayOf("application/pdf")) },
                    onRemovePdf = onRemovePdf
                )
            }
        }
    }
}

@Composable
private fun InAppTimetableTab(
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    courses: List<CourseEntity>,
    coursesById: Map<Long, CourseEntity>,
    daySlots: List<ClassScheduleEntity>,
    use24Hour: Boolean,
    isReadOnly: Boolean,
    onEditSlot: (ClassScheduleEntity) -> Unit
) {
    val ext = LocalUniDeckColors.current
    val days = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "day_pills") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { (dayInt, label) ->
                    FilterChip(
                        selected = selectedDay == dayInt,
                        onClick = { onSelectDay(dayInt) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("timetable_day_$dayInt")
                    )
                }
            }
        }

        if (courses.isEmpty()) {
            item(key = "no_courses_timetable") {
                Surface(
                    shape = UniformCardShape,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Add at least one course in the Attendance tab first, then schedule your Mon–Sat classes here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else if (daySlots.isEmpty()) {
            item(key = "no_slots_day") {
                Surface(
                    shape = UniformCardShape,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No classes scheduled on this day. Tap + Add Class to build your weekly grid.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            item(key = "day_slots_group") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    daySlots.forEachIndexed { idx, slot ->
                        val course = coursesById[slot.courseId]
                        ExpressiveGroupedCard(
                            index = idx,
                            count = daySlots.size,
                            onClick = if (isReadOnly) null else { { onEditSlot(slot) } }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${AcademicCalculator.formatMinutes(slot.startTime, use24Hour)} – ${AcademicCalculator.formatMinutes(slot.endTime, use24Hour)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = TABULAR_NUMS
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${slot.componentType} • ${slot.weight}x",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = course?.let { "${it.code} • ${it.name}" } ?: "Course",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val meta = listOfNotNull(
                                slot.roomNumber.takeIf { it.isNotBlank() }?.let { "Room $it" },
                                course?.professorName?.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (meta.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflinePdfViewerTab(
    pdfFile: File?,
    pdfVersion: Int,
    isReadOnly: Boolean,
    onPickPdf: () -> Unit,
    onRemovePdf: () -> Unit
) {
    val ext = LocalUniDeckColors.current

    if (pdfFile == null || !pdfFile.exists()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = UniformCardShape,
                color = ext.cardSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Offline Timetable PDF",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import your official semester timetable PDF. UniDeck saves a private local copy per semester with pinch-to-zoom viewing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = onPickPdf,
                        enabled = !isReadOnly,
                        modifier = Modifier.testTag("import_pdf_button")
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import PDF")
                    }
                }
            }
        }
        return
    }

    var pageIndex by remember(pdfFile, pdfVersion) { mutableIntStateOf(0) }
    var pageCount by remember(pdfFile, pdfVersion) { mutableIntStateOf(1) }
    var renderedBitmap by remember(pdfFile, pdfVersion, pageIndex) { mutableStateOf<Bitmap?>(null) }
    var scale by remember(pageIndex, pdfVersion) { mutableFloatStateOf(1f) }
    var offset by remember(pageIndex, pdfVersion) { mutableStateOf(Offset.Zero) }

    DisposableEffect(pdfFile, pdfVersion, pageIndex) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount.coerceAtLeast(1)
            val safePage = pageIndex.coerceIn(0, pageCount - 1)
            renderer.openPage(safePage).use { page ->
                val bmpWidth = (page.width * 2).coerceAtMost(2400)
                val bmpHeight = (page.height * 2).coerceAtMost(3200)
                val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                renderedBitmap = bitmap
            }
        } catch (_: Exception) {
            renderedBitmap = null
        } finally {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onPickPdf, enabled = !isReadOnly) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Replace")
                }
                OutlinedButton(onClick = onRemovePdf, enabled = !isReadOnly) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remove")
                }
            }
            if (scale > 1.05f) {
                IconButton(onClick = {
                    scale = 1f
                    offset = Offset.Zero
                }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset zoom")
                }
            }
        }

        if (pageCount > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { if (pageIndex > 0) pageIndex-- },
                    enabled = pageIndex > 0
                ) { Text("Prev Page") }
                Text(
                    text = "Page ${pageIndex + 1} / $pageCount",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_NUMS)
                )
                TextButton(
                    onClick = { if (pageIndex < pageCount - 1) pageIndex++ },
                    enabled = pageIndex < pageCount - 1
                ) { Text("Next Page") }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = newScale
                        offset = if (newScale > 1f) offset + pan else Offset.Zero
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = renderedBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Timetable PDF Page ${pageIndex + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            } else {
                Text(
                    text = "Unable to render PDF page",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
private fun ScheduleEditorDialog(
    initial: ClassScheduleEntity?,
    defaultDay: Int,
    semesterId: Long,
    courses: List<CourseEntity>,
    savedProfessors: List<String>,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (ClassScheduleEntity, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val initialCourseId = initial?.courseId ?: courses.firstOrNull()?.id ?: 0L
    var selectedCourseId by remember(initial, courses) {
        mutableStateOf(initialCourseId)
    }
    var professor by remember(initial, courses) {
        mutableStateOf(courses.firstOrNull { it.id == initialCourseId }?.professorName ?: "")
    }
    var dayOfWeek by remember(initial, defaultDay) { mutableIntStateOf(initial?.dayOfWeek ?: defaultDay) }
    var startMinutes by remember(initial) { mutableIntStateOf(initial?.startTime ?: 540) } // 9:00 AM
    var endMinutes by remember(initial) { mutableIntStateOf(initial?.endTime ?: 600) } // 10:00 AM
    var room by remember(initial) { mutableStateOf(initial?.roomNumber ?: "") }
    var componentType by remember(initial) { mutableStateOf(initial?.componentType ?: ComponentType.LECTURE) }
    var weight by remember(initial) { mutableIntStateOf(initial?.weight ?: 1) }
    var pickingStartTime by remember { mutableStateOf(false) }
    var pickingEndTime by remember { mutableStateOf(false) }

    val days = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S")

    if (pickingStartTime) {
        UniDeckTimePickerDialog(
            title = "Select Start Time",
            initialMinutes = startMinutes,
            is24Hour = use24Hour,
            onDismiss = { pickingStartTime = false },
            onConfirm = { picked ->
                startMinutes = picked
                if (endMinutes <= picked) {
                    endMinutes = (picked + 60).coerceAtMost(1439)
                }
                pickingStartTime = false
            }
        )
    }

    if (pickingEndTime) {
        UniDeckTimePickerDialog(
            title = "Select End Time",
            initialMinutes = endMinutes,
            is24Hour = use24Hour,
            onDismiss = { pickingEndTime = false },
            onConfirm = { picked ->
                endMinutes = if (picked <= startMinutes) {
                    (startMinutes + 15).coerceAtMost(1439)
                } else {
                    picked
                }
                pickingEndTime = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Timetable Slot" else "Edit Timetable Slot",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Course", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(courses) { course ->
                        FilterChip(
                            selected = selectedCourseId == course.id,
                            onClick = {
                                selectedCourseId = course.id
                                professor = course.professorName
                            },
                            label = { Text(course.code, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                Text("Day", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (d, lbl) ->
                        val selected = dayOfWeek == d
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { dayOfWeek = d }
                                .testTag("dialog_day_$d")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = lbl,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Material You Start & End Time Picker Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimePickerPill(
                        label = "Start",
                        formattedTime = AcademicCalculator.formatMinutes(startMinutes, use24Hour),
                        onClick = { pickingStartTime = true },
                        modifier = Modifier.weight(1f),
                        testTagName = "schedule_start_time_pill"
                    )
                    TimePickerPill(
                        label = "End",
                        formattedTime = AcademicCalculator.formatMinutes(endMinutes, use24Hour),
                        onClick = { pickingEndTime = true },
                        modifier = Modifier.weight(1f),
                        testTagName = "schedule_end_time_pill"
                    )
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Lab Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ProfessorAutocompleteField(
                    value = professor,
                    onValueChange = { professor = it },
                    savedProfessors = savedProfessors,
                    label = "Professor (Optional)",
                    testTagName = "schedule_professor_input"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ComponentType.all.forEach { comp ->
                        val shortLabel = when (comp) {
                            ComponentType.LAB -> "Lab"
                            ComponentType.TUTORIAL -> "Tut"
                            else -> "Lec"
                        }
                        FilterChip(
                            selected = componentType == comp,
                            onClick = { componentType = comp },
                            label = {
                                Text(
                                    text = shortLabel,
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weight",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3).forEach { w ->
                            FilterChip(
                                selected = weight == w,
                                onClick = { weight = w },
                                label = {
                                    Text(
                                        text = "${w}x",
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

                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Slot", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedCourseId != 0L) {
                        val entity = (initial ?: ClassScheduleEntity(
                            courseId = selectedCourseId,
                            semesterId = semesterId,
                            dayOfWeek = dayOfWeek,
                            startTime = startMinutes,
                            endTime = endMinutes
                        )).copy(
                            courseId = selectedCourseId,
                            dayOfWeek = dayOfWeek,
                            startTime = startMinutes,
                            endTime = endMinutes.coerceAtLeast(startMinutes + 15),
                            roomNumber = room.trim(),
                            componentType = componentType,
                            weight = weight
                        )
                        onSave(entity, professor)
                    }
                },
                enabled = selectedCourseId != 0L,
                modifier = Modifier.testTag("save_schedule_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
