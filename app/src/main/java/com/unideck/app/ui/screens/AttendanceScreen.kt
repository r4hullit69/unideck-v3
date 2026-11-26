package com.unideck.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.AttendanceStatus
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.OneOffClassEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.local.formatDateRange
import com.unideck.app.data.local.isDateWithinSemester
import com.unideck.app.data.prefs.AcademicMilestoneItem
import com.unideck.app.data.prefs.AcademicNoteItem
import com.unideck.app.data.prefs.AcademicTaskItem
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.domain.AttendanceHealth
import com.unideck.app.ui.components.AttendanceActionRow
import com.unideck.app.ui.components.CircularAttendanceRing
import com.unideck.app.ui.components.Cookie9SidedShape
import com.unideck.app.ui.components.DailyClassTimelineRow
import com.unideck.app.ui.components.DailyTimelineItem
import com.unideck.app.ui.components.ExpressiveConnectedButtonGroup
import com.unideck.app.ui.components.ExpressiveGroupedCard
import com.unideck.app.ui.components.HeroTintCard
import com.unideck.app.ui.components.ProfessorAutocompleteField
import com.unideck.app.ui.components.UniDeckDatePickerDialog
import com.unideck.app.ui.components.UniformCardShape
import com.unideck.app.ui.components.healthColor
import com.unideck.app.ui.components.rememberUniDeckHaptics
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    activeSemester: SemesterEntity?,
    courses: List<CourseEntity>,
    schedules: List<ClassScheduleEntity>,
    oneOffClasses: List<OneOffClassEntity>,
    allLogs: List<AttendanceLogEntity>,
    selectedEpochDay: Long,
    selectedSubTab: Int,
    preferences: UserPreferences,
    onSelectSubTab: (Int) -> Unit,
    onSelectDate: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    onMarkScheduledClass: (CourseEntity, Long, String, String) -> Unit,
    onMarkOneOffClass: (CourseEntity, OneOffClassEntity, String) -> Unit,
    onDeleteOneOffClass: (OneOffClassEntity) -> Unit,
    onBulkMarkDay: (String) -> Unit,
    onLogManualAttendance: (CourseEntity, String, String) -> Unit,
    onOpenCourseDetail: (Long) -> Unit,
    onOpenAddCourse: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onOpenTimetable: () -> Unit,
    onImportBackup: () -> Unit,
    onUpdateNotes: (List<AcademicNoteItem>) -> Unit = {},
    onUpdateTasks: (List<AcademicTaskItem>) -> Unit = {},
    onUpdateMilestones: (List<AcademicMilestoneItem>) -> Unit = {},
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val selectedDate = remember(selectedEpochDay) { LocalDate.ofEpochDay(selectedEpochDay) }
    val today = remember { LocalDate.now() }
    var isMonthExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val haptics = rememberUniDeckHaptics()

    val academicWeekNumber = remember(activeSemester, selectedEpochDay) {
        val startEpoch = activeSemester?.startDateEpochDay
            ?: selectedDate.with(TemporalAdjusters.firstDayOfMonth()).toEpochDay()
        (((selectedEpochDay - startEpoch) / 7L) + 1L).coerceIn(1L, 52L).toInt()
    }

    val activeCourseIds = remember(courses) { courses.map { it.id }.toSet() }
    val semesterLogs = remember(allLogs, activeCourseIds) {
        allLogs.filter { it.courseId in activeCourseIds }
    }
    val logsByDate = remember(semesterLogs) { semesterLogs.groupBy { it.dateEpochDay } }
    val coursesById = remember(courses) { courses.associateBy { it.id } }

    val isWithinSemester = remember(activeSemester, selectedEpochDay) {
        activeSemester?.isDateWithinSemester(selectedEpochDay) ?: true
    }

    val overallStats = remember(courses, semesterLogs, preferences.minAttendanceThreshold) {
        val totalInitAttended = courses.sumOf { it.initialAttended.coerceAtLeast(0) }
        val totalInitClasses = courses.sumOf { it.initialTotal.coerceAtLeast(0) }
        AcademicCalculator.computeAttendanceStats(
            logs = semesterLogs,
            thresholdPercent = preferences.minAttendanceThreshold,
            initialAttended = totalInitAttended,
            initialTotal = totalInitClasses
        )
    }

    val courseStatsMap = remember(courses, semesterLogs, preferences.minAttendanceThreshold) {
        courses.associate { course ->
            val cLogs = semesterLogs.filter { it.courseId == course.id }
            course.id to AcademicCalculator.computeAttendanceStats(
                logs = cLogs,
                thresholdPercent = preferences.minAttendanceThreshold,
                initialAttended = course.initialAttended,
                initialTotal = course.initialTotal
            )
        }
    }

    val lowCoursesCount = remember(courseStatsMap) {
        courseStatsMap.values.count { it.totalCounted > 0 && it.health == AttendanceHealth.DANGER }
    }

    val logsById = remember(semesterLogs) { semesterLogs.associateBy { it.id } }

    val dayOneOffs = remember(oneOffClasses, selectedEpochDay) {
        oneOffClasses.filter { it.dateEpochDay == selectedEpochDay }
    }

    val timelineItems = remember(
        schedules,
        dayOneOffs,
        coursesById,
        selectedDate,
        selectedEpochDay,
        logsByDate,
        logsById,
        isWithinSemester
    ) {
        val recurring = if (isWithinSemester) {
            schedules
                .filter { it.dayOfWeek == selectedDate.dayOfWeek.value }
                .mapNotNull { slot ->
                    val course = coursesById[slot.courseId] ?: return@mapNotNull null
                    val existingLog = logsByDate[selectedEpochDay]?.firstOrNull {
                        it.courseId == course.id && !it.isManualEntry && it.componentType == slot.componentType
                    }
                    DailyTimelineItem(
                        key = "sched_${slot.id}",
                        course = course,
                        startTime = slot.startTime,
                        endTime = slot.endTime,
                        roomNumber = slot.roomNumber,
                        componentType = slot.componentType,
                        activeStatus = existingLog?.status,
                        isOneOff = false
                    )
                }
        } else {
            emptyList()
        }

        val extras = dayOneOffs.mapNotNull { oneOff ->
            val course = coursesById[oneOff.courseId] ?: return@mapNotNull null
            val linkedLog = oneOff.attendanceLogId?.let { logsById[it] }
            DailyTimelineItem(
                key = "oneoff_${oneOff.id}",
                course = course,
                startTime = oneOff.startTime,
                endTime = oneOff.endTime,
                roomNumber = oneOff.roomNumber,
                componentType = oneOff.componentType,
                activeStatus = linkedLog?.status,
                isOneOff = true,
                oneOffEntity = oneOff
            )
        }

        (recurring + extras).sortedBy { it.startTime }
    }

    val unlinkedManualLogs = remember(logsByDate, selectedEpochDay, dayOneOffs, isWithinSemester) {
        val oneOffLogIds = dayOneOffs.mapNotNull { it.attendanceLogId }.toSet()
        logsByDate[selectedEpochDay].orEmpty().filter { log ->
            log.id !in oneOffLogIds && (log.isManualEntry || !isWithinSemester)
        }
    }

    if (showDatePicker) {
        UniDeckDatePickerDialog(
            initialEpochDay = selectedEpochDay,
            onDismiss = { showDatePicker = false },
            onDateSelected = { pickedEpochDay ->
                onSelectDate(pickedEpochDay)
                showDatePicker = false
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Expandable Month/Week Strip at Top
            item(key = "calendar_card") {
                ExpandableCalendarCard(
                    selectedDate = selectedDate,
                    isMonthExpanded = isMonthExpanded,
                    logsByDate = logsByDate,
                    onSelectDate = {
                        haptics.tick()
                        onSelectDate(it.toEpochDay())
                    },
                    onToggleExpand = {
                        haptics.tick()
                        isMonthExpanded = !isMonthExpanded
                    }
                )
            }

            // 2. Material 3 Expressive Connected Pill Button Group: Today | Courses
            item(key = "attendance_subtab_toggle") {
                ExpressiveConnectedButtonGroup(
                    items = listOf("Today", "Courses"),
                    selectedIndex = selectedSubTab.coerceIn(0, 1),
                    onSelect = onSelectSubTab,
                    testTags = listOf(
                        "attendance_subtab_today",
                        "attendance_subtab_courses"
                    ),
                    modifier = Modifier.testTag("attendance_subtab_row")
                )
            }

            when (selectedSubTab.coerceIn(0, 1)) {
                0 -> {
                    // TODAY VIEW: Outside-semester banner + Bulk pills + Vertical Time-Rail & Connected Cards
                    if (!isWithinSemester && activeSemester != null) {
                        item(key = "outside_semester_banner") {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("outside_semester_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Outside semester dates (${activeSemester.formatDateRange()})",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFeatureSettings = TABULAR_NUMS
                                        ),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (timelineItems.isEmpty()) {
                        if (isWithinSemester) {
                            item(key = "no_classes_today") {
                                val ext = LocalUniDeckColors.current
                                val emptyText = remember(selectedDate) {
                                    if (selectedDate == LocalDate.now()) {
                                        "No classes scheduled today"
                                    } else {
                                        "No classes on ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}"
                                    }
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = ext.cardSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("empty_schedule_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.EventAvailable,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = emptyText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TextButton(
                                            onClick = onOpenTimetable,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("manage_timetable_link")
                                        ) {
                                            Text(
                                                text = "Edit",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Slim Header Row with Date Label + Compact Low-Profile Bulk Pills + Edit
                        item(key = "day_schedule_header") {
                            val ext = LocalUniDeckColors.current
                            val dateLabel = remember(selectedDate) {
                                if (selectedDate == LocalDate.now()) {
                                    "Today"
                                } else {
                                    selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(15.dp),
                                        color = ext.presentFill.copy(alpha = if (isReadOnly) 0.4f else 0.85f),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .clickable(enabled = !isReadOnly) {
                                                haptics.confirm()
                                                onBulkMarkDay(AttendanceStatus.PRESENT)
                                            }
                                            .testTag("bulk_mark_all_present")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DoneAll,
                                                contentDescription = "Mark all present",
                                                tint = ext.presentContent.copy(alpha = if (isReadOnly) 0.45f else 1f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "All Pre",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ext.presentContent.copy(alpha = if (isReadOnly) 0.45f else 1f),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(15.dp),
                                        color = ext.cancelledFill.copy(alpha = if (isReadOnly) 0.4f else 0.85f),
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .clickable(enabled = !isReadOnly) {
                                                haptics.confirm()
                                                onBulkMarkDay(AttendanceStatus.CANCELLED)
                                            }
                                            .testTag("bulk_mark_holiday")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.EventBusy,
                                                contentDescription = "Mark holiday",
                                                tint = ext.cancelledContent.copy(alpha = if (isReadOnly) 0.45f else 1f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Holiday",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ext.cancelledContent.copy(alpha = if (isReadOnly) 0.45f else 1f),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(15.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(15.dp))
                                            .clickable(onClick = onOpenTimetable)
                                            .testTag("manage_timetable_link")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Schedule,
                                                contentDescription = "Edit timetable",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Edit",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "daily_vertical_timeline") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                timelineItems.forEachIndexed { idx, item ->
                                    val stats = courseStatsMap[item.course.id]
                                        ?: AcademicCalculator.computeAttendanceStats(emptyList(), preferences.minAttendanceThreshold)
                                    DailyClassTimelineRow(
                                        item = item,
                                        stats = stats,
                                        thresholdPercent = preferences.minAttendanceThreshold,
                                        use24Hour = preferences.use24HourFormat,
                                        isFirst = idx == 0,
                                        isLast = idx == timelineItems.lastIndex,
                                        index = idx,
                                        totalCount = timelineItems.size,
                                        selectedDate = selectedDate,
                                        cutCorners = preferences.cutCorners,
                                        isReadOnly = isReadOnly,
                                        onOpenCourseDetail = { onOpenCourseDetail(item.course.id) },
                                        onSelectStatus = { status ->
                                            val oneOff = item.oneOffEntity
                                            if (item.isOneOff && oneOff != null) {
                                                onMarkOneOffClass(item.course, oneOff, status)
                                            } else {
                                                onMarkScheduledClass(item.course, selectedEpochDay, item.componentType, status)
                                            }
                                        },
                                        onDeleteOneOff = item.oneOffEntity?.let { entity ->
                                            { onDeleteOneOffClass(entity) }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (unlinkedManualLogs.isNotEmpty()) {
                        item(key = "manual_logs_section") {
                            val ext = LocalUniDeckColors.current
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = ext.cardSurface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Logged on this date",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    unlinkedManualLogs.forEach { log ->
                                        val course = coursesById[log.courseId]
                                        val statusColor = when (log.status) {
                                            AttendanceStatus.PRESENT -> ext.presentContent
                                            AttendanceStatus.ABSENT -> ext.absentContent
                                            else -> ext.cancelledContent
                                        }
                                        val statusBg = when (log.status) {
                                            AttendanceStatus.PRESENT -> ext.presentFill
                                            AttendanceStatus.ABSENT -> ext.absentFill
                                            else -> ext.cancelledFill
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { if (course != null) onOpenCourseDetail(course.id) }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = course?.name ?: "Course",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${course?.code ?: ""} • ${log.componentType.lowercase().replaceFirstChar { it.uppercase() }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = statusBg
                                            ) {
                                                Text(
                                                    text = log.status.lowercase().replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = statusColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // COURSES VIEW: Overall Attendance Hero Card + Courses list
                    if (courses.isEmpty()) {
                        item(key = "welcome_empty_state") {
                            HeroTintCard {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Your semester workspace is ready",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Add your courses to track equal-weight attendance, plan safe bunks, record exam scores, and simulate SGPA — 100% offline.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onOpenAddCourse,
                                        enabled = !isReadOnly,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("empty_add_course_button")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Course")
                                    }
                                    FilledTonalButton(
                                        onClick = onImportBackup,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("empty_import_backup_button")
                                    ) {
                                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import JSON")
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "overall_hero") {
                            val ext = LocalUniDeckColors.current
                            HeroTintCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "OVERALL ATTENDANCE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (overallStats.totalCounted == 0) "--" else "%.2f%%".format(overallStats.percentage),
                                            style = MaterialTheme.typography.displayMedium.copy(
                                                fontFeatureSettings = TABULAR_NUMS
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${overallStats.present}/${overallStats.totalCounted} classes • Target ${preferences.minAttendanceThreshold}%",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (lowCoursesCount > 0) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = ext.absentFill,
                                                border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Warning,
                                                        contentDescription = null,
                                                        tint = ext.danger,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "$lowCoursesCount ${if (lowCoursesCount == 1) "course" else "courses"} below ${preferences.minAttendanceThreshold}%",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = ext.danger
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    CircularAttendanceRing(
                                        percentage = overallStats.percentage,
                                        health = overallStats.health,
                                        hasData = overallStats.totalCounted > 0,
                                        size = 76.dp,
                                        strokeWidth = 7.dp
                                    )
                                }
                            }
                        }

                        item(key = "courses_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Courses (${courses.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(
                                    onClick = onOpenAddCourse,
                                    enabled = !isReadOnly,
                                    modifier = Modifier.testTag("add_course_inline_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Course", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }

                        item(key = "course_group_list") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                courses.forEachIndexed { idx, course ->
                                    val stats = courseStatsMap[course.id]
                                        ?: AcademicCalculator.computeAttendanceStats(emptyList(), preferences.minAttendanceThreshold)
                                    ExpressiveGroupedCard(
                                        index = idx,
                                        count = courses.size,
                                        onClick = { onOpenCourseDetail(course.id) },
                                        modifier = Modifier.testTag("course_card_${course.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = course.code,
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        val crLabel = AcademicCalculator.formatCredits(course.credits)
                                                        Text(
                                                            text = if (course.isLab) "LAB • $crLabel CR" else "$crLabel CR",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = course.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${stats.present}/${stats.totalCounted} • ${stats.statusHint}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                                    color = if (stats.totalCounted == 0) {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    } else {
                                                        healthColor(stats.health)
                                                    }
                                                )
                                            }
                                            if (!isReadOnly) {
                                                IconButton(
                                                    onClick = { onEditCourse(course) },
                                                    modifier = Modifier.testTag("edit_course_${course.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = "Edit ${course.name}",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            CircularAttendanceRing(
                                                percentage = stats.percentage,
                                                health = stats.health,
                                                hasData = stats.totalCounted > 0,
                                                size = 54.dp,
                                                strokeWidth = 5.dp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        AttendanceActionRow(
                                            activeStatus = null,
                                            enabled = !isReadOnly,
                                            onSelectStatus = { status ->
                                                val comp = if (course.isLab) ComponentType.LAB else ComponentType.LECTURE
                                                onLogManualAttendance(course, status, comp)
                                            },
                                            testTagPrefix = "course_${course.id}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating "Jump to today" Extended Primary Pill whenever viewing a non-today date on Today tab
        AnimatedVisibility(
            visible = selectedSubTab == 0 && selectedDate != today,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        haptics.confirm()
                        onJumpToToday()
                    }
                    .testTag("floating_jump_today_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Jump to today",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

private const val CALENDAR_PAGE_COUNT = 2400
private const val CALENDAR_ANCHOR_PAGE = 1200

@Composable
private fun ExpandableCalendarCard(
    selectedDate: LocalDate,
    isMonthExpanded: Boolean,
    logsByDate: Map<Long, List<AttendanceLogEntity>>,
    onSelectDate: (LocalDate) -> Unit,
    onToggleExpand: () -> Unit
) {
    val ext = LocalUniDeckColors.current
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM") }
    val today = remember { LocalDate.now() }
    val anchorMonday = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val anchorMonth = remember { YearMonth.from(today) }
    val dayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val coroutineScope = rememberCoroutineScope()
    val latestSelectedDate by rememberUpdatedState(selectedDate)

    fun weekPageFor(date: LocalDate): Int {
        val dateMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeksDiff = ChronoUnit.WEEKS.between(anchorMonday, dateMonday).toInt()
        return (CALENDAR_ANCHOR_PAGE + weeksDiff).coerceIn(0, CALENDAR_PAGE_COUNT - 1)
    }

    fun monthPageFor(date: LocalDate): Int {
        val monthsDiff = ChronoUnit.MONTHS.between(anchorMonth, YearMonth.from(date)).toInt()
        return (CALENDAR_ANCHOR_PAGE + monthsDiff).coerceIn(0, CALENDAR_PAGE_COUNT - 1)
    }

    val weekPagerState = rememberPagerState(
        initialPage = weekPageFor(selectedDate),
        pageCount = { CALENDAR_PAGE_COUNT }
    )
    val monthPagerState = rememberPagerState(
        initialPage = monthPageFor(selectedDate),
        pageCount = { CALENDAR_PAGE_COUNT }
    )

    // Keep newly revealed pager synced when expanding/collapsing
    LaunchedEffect(isMonthExpanded) {
        if (isMonthExpanded) {
            val target = monthPageFor(latestSelectedDate)
            if (monthPagerState.currentPage != target) {
                monthPagerState.scrollToPage(target)
            }
        } else {
            val target = weekPageFor(latestSelectedDate)
            if (weekPagerState.currentPage != target) {
                weekPagerState.scrollToPage(target)
            }
        }
    }

    // Swipe -> State & Haptics (Collapsed Week View)
    LaunchedEffect(weekPagerState, isMonthExpanded) {
        if (isMonthExpanded) return@LaunchedEffect
        snapshotFlow { weekPagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                val currentSelected = latestSelectedDate
                val expectedPage = weekPageFor(currentSelected)
                if (settled != expectedPage) {
                    val targetWeekStart = anchorMonday.plusWeeks((settled - CALENDAR_ANCHOR_PAGE).toLong())
                    val dayOffset = (currentSelected.dayOfWeek.value - 1).toLong()
                    onSelectDate(targetWeekStart.plusDays(dayOffset))
                }
            }
    }

    // Swipe -> State & Haptics (Expanded Month View)
    LaunchedEffect(monthPagerState, isMonthExpanded) {
        if (!isMonthExpanded) return@LaunchedEffect
        snapshotFlow { monthPagerState.settledPage }
            .distinctUntilChanged()
            .collect { settled ->
                val currentSelected = latestSelectedDate
                val expectedPage = monthPageFor(currentSelected)
                if (settled != expectedPage) {
                    val targetYm = anchorMonth.plusMonths((settled - CALENDAR_ANCHOR_PAGE).toLong())
                    val safeDay = currentSelected.dayOfMonth.coerceAtMost(targetYm.lengthOfMonth())
                    onSelectDate(targetYm.atDay(safeDay))
                }
            }
    }

    // External selectedDate change (e.g. "Jump to today", Date Picker) -> Pager Sync
    LaunchedEffect(selectedDate, isMonthExpanded) {
        if (!isMonthExpanded) {
            val target = weekPageFor(selectedDate)
            if (weekPagerState.currentPage != target && !weekPagerState.isScrollInProgress) {
                if (abs(weekPagerState.currentPage - target) <= 4) {
                    weekPagerState.animateScrollToPage(target)
                } else {
                    weekPagerState.scrollToPage(target)
                }
            }
        } else {
            val target = monthPageFor(selectedDate)
            if (monthPagerState.currentPage != target && !monthPagerState.isScrollInProgress) {
                if (abs(monthPagerState.currentPage - target) <= 3) {
                    monthPagerState.animateScrollToPage(target)
                } else {
                    monthPagerState.scrollToPage(target)
                }
            }
        }
    }

    // Live displayed month title synced with the active pager page
    val headerMonthTitle = if (isMonthExpanded) {
        val visibleYm = anchorMonth.plusMonths((monthPagerState.currentPage - CALENDAR_ANCHOR_PAGE).toLong())
        visibleYm.atDay(1).format(monthFormatter)
    } else {
        val visiblePage = weekPagerState.currentPage
        if (visiblePage == weekPageFor(selectedDate)) {
            selectedDate.format(monthFormatter)
        } else {
            val weekStart = anchorMonday.plusWeeks((visiblePage - CALENDAR_ANCHOR_PAGE).toLong())
            val dayOffset = (selectedDate.dayOfWeek.value - 1).toLong()
            weekStart.plusDays(dayOffset).format(monthFormatter)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = ext.cardSurface,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Expandable Month Header (e.g., "November ▾" / "November ▲") taking remaining width
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onToggleExpand)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("calendar_expand_toggle")
                ) {
                    Text(
                        text = headerMonthTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isMonthExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isMonthExpanded) "Collapse to Week" else "Expand to Month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: Clean 36.dp circular previous (<) and next (>) navigation buttons synced with HorizontalPager
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    if (isMonthExpanded) {
                                        val target = (monthPagerState.currentPage - 1).coerceAtLeast(0)
                                        monthPagerState.animateScrollToPage(target)
                                    } else {
                                        val target = (weekPagerState.currentPage - 1).coerceAtLeast(0)
                                        weekPagerState.animateScrollToPage(target)
                                    }
                                }
                            }
                            .testTag("calendar_prev_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ChevronLeft,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    if (isMonthExpanded) {
                                        val target = (monthPagerState.currentPage + 1).coerceAtMost(CALENDAR_PAGE_COUNT - 1)
                                        monthPagerState.animateScrollToPage(target)
                                    } else {
                                        val target = (weekPagerState.currentPage + 1).coerceAtMost(CALENDAR_PAGE_COUNT - 1)
                                        weekPagerState.animateScrollToPage(target)
                                    }
                                }
                            }
                            .testTag("calendar_next_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekday Header Row (Mon - Sun)
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (!isMonthExpanded) {
                HorizontalPager(
                    state = weekPagerState,
                    userScrollEnabled = true,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_week_pager")
                ) { page ->
                    val startOfWeek = anchorMonday.plusWeeks((page - CALENDAR_ANCHOR_PAGE).toLong())
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (i in 0..6) {
                            val d = startOfWeek.plusDays(i.toLong())
                            CalendarDayCell(
                                date = d,
                                isSelected = d == selectedDate,
                                isToday = d == today,
                                dayLogs = logsByDate[d.toEpochDay()].orEmpty(),
                                onClick = { onSelectDate(d) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                HorizontalPager(
                    state = monthPagerState,
                    userScrollEnabled = true,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
                        .testTag("calendar_month_pager")
                ) { page ->
                    val ym = anchorMonth.plusMonths((page - CALENDAR_ANCHOR_PAGE).toLong())
                    val firstDay = ym.atDay(1)
                    val leadingEmpty = (firstDay.dayOfWeek.value - 1) // Mon=0 .. Sun=6
                    val daysInMonth = ym.lengthOfMonth()
                    val totalCells = leadingEmpty + daysInMonth
                    val rows = (totalCells + 6) / 7

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (r in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (c in 0..6) {
                                    val cellIdx = r * 7 + c
                                    val dayNum = cellIdx - leadingEmpty + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val d = ym.atDay(dayNum)
                                        CalendarDayCell(
                                            date = d,
                                            isSelected = d == selectedDate,
                                            isToday = d == today,
                                            dayLogs = logsByDate[d.toEpochDay()].orEmpty(),
                                            onClick = { onSelectDate(d) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    dayLogs: List<AttendanceLogEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalUniDeckColors.current
    val cs = MaterialTheme.colorScheme

    val badgeShape = CircleShape
    val badgeBg = when {
        isSelected -> cs.primary
        isToday -> cs.tertiaryContainer
        else -> cs.surface.copy(alpha = 0f)
    }
    val badgeFg = when {
        isSelected -> cs.onPrimary
        isToday -> cs.onTertiaryContainer
        else -> cs.onSurface
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(badgeShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontFeatureSettings = TABULAR_NUMS
                ),
                color = badgeFg
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.height(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statuses = dayLogs.map { it.status }.distinct().take(3)
            statuses.forEach { st ->
                val dotColor = when (st) {
                    AttendanceStatus.PRESENT -> ext.safe
                    AttendanceStatus.ABSENT -> ext.danger
                    else -> ext.cancelledContent
                }
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

/**
 * 100% Offline Academic Hub (Faculty & Course Notes, Task & Admin Checklist, and Semester Milestones)
 * displayed inside the Events tab.
 */
@Composable
private fun OfflineEventsAndAcademicHubSection(
    semesterId: Long,
    courses: List<CourseEntity>,
    notes: List<AcademicNoteItem>,
    tasks: List<AcademicTaskItem>,
    milestones: List<AcademicMilestoneItem>,
    isReadOnly: Boolean,
    onUpdateNotes: (List<AcademicNoteItem>) -> Unit,
    onUpdateTasks: (List<AcademicTaskItem>) -> Unit,
    onUpdateMilestones: (List<AcademicMilestoneItem>) -> Unit
) {
    val ext = LocalUniDeckColors.current
    val cs = MaterialTheme.colorScheme
    val haptics = rememberUniDeckHaptics()

    val categories = remember { listOf("All", "Pinned", "Assignments", "Faculty", "Exam") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }

    val filteredNotes = remember(notes, selectedCategory, semesterId) {
        val semNotes = notes.filter { it.semesterId == 0L || it.semesterId == semesterId }
        when (selectedCategory) {
            "Pinned" -> semNotes.filter { it.pinned || it.category == "Pinned" }
            "All" -> semNotes
            else -> semNotes.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Assignments") }
        var courseCode by remember { mutableStateOf(courses.firstOrNull()?.code ?: "") }
        var pinned by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Event / Note", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g. OS Lab Viva / Prof Office Hours)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_note_title_input")
                    )
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Details / Room / Deadline") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hub_note_body_input")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Assignments", "Exam", "Faculty", "Pinned")) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    if (courses.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(courses) { c ->
                                FilterChip(
                                    selected = courseCode == c.code,
                                    onClick = { courseCode = c.code },
                                    label = { Text(c.code) }
                                )
                            }
                        }
                    }
                    FilterChip(
                        selected = pinned,
                        onClick = { pinned = !pinned },
                        leadingIcon = {
                            Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Pin to top") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            haptics.confirm()
                            val newItem = AcademicNoteItem(
                                id = System.currentTimeMillis(),
                                semesterId = semesterId,
                                title = title.trim(),
                                body = body.trim(),
                                category = category,
                                courseCode = courseCode,
                                pinned = pinned
                            )
                            onUpdateNotes(listOf(newItem) + notes)
                            showAddNoteDialog = false
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("hub_save_note_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Checklist Task", style = MaterialTheme.typography.headlineSmall) },
            text = {
                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task (e.g. Submit Scholarship Form)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hub_task_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            haptics.confirm()
                            val item = AcademicTaskItem(
                                id = System.currentTimeMillis(),
                                semesterId = semesterId,
                                title = taskTitle.trim(),
                                completed = false
                            )
                            onUpdateTasks(tasks + item)
                            showAddTaskDialog = false
                        }
                    },
                    enabled = taskTitle.isNotBlank(),
                    modifier = Modifier.testTag("hub_save_task_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddMilestoneDialog) {
        var yearText by remember { mutableStateOf(LocalDate.now().year.toString()) }
        var msTitle by remember { mutableStateOf("") }
        var msSubtitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("Add Academic Milestone", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Year (e.g. 2026)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = msTitle,
                        onValueChange = { msTitle = it },
                        label = { Text("Milestone (e.g. Summer Internship / Capstone)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = msSubtitle,
                        onValueChange = { msSubtitle = it },
                        label = { Text("Target / Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val yr = yearText.toIntOrNull() ?: LocalDate.now().year
                        if (msTitle.isNotBlank()) {
                            haptics.confirm()
                            val item = AcademicMilestoneItem(
                                id = System.currentTimeMillis(),
                                year = yr,
                                title = msTitle.trim(),
                                subtitle = msSubtitle.trim()
                            )
                            onUpdateMilestones((milestones + item).sortedBy { it.year })
                            showAddMilestoneDialog = false
                        }
                    },
                    enabled = msTitle.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Faculty & Course Notes / Events Section
        Surface(
            shape = MaterialTheme.shapes.large,
            color = ext.cardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Events & Course Notes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurface
                        )
                    }
                    if (!isReadOnly) {
                        FilledTonalButton(
                            onClick = { showAddNoteDialog = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("hub_add_note_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                haptics.tick()
                                selectedCategory = cat
                            },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("hub_filter_${cat.lowercase()}")
                        )
                    }
                }

                if (filteredNotes.isEmpty()) {
                    Text(
                        text = "No $selectedCategory events or faculty notes yet. Tap Add to save exam dates, assignment deadlines, or faculty cabin details offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                } else {
                    filteredNotes.forEach { note ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cs.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = cs.primaryContainer
                                        ) {
                                            Text(
                                                text = note.category,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = cs.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (note.courseCode.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = cs.secondaryContainer
                                            ) {
                                                Text(
                                                    text = note.courseCode,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = cs.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = note.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = cs.onSurface
                                    )
                                    if (note.body.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = note.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = cs.onSurfaceVariant
                                        )
                                    }
                                }
                                if (!isReadOnly) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                haptics.tick()
                                                onUpdateNotes(
                                                    notes.map {
                                                        if (it.id == note.id) it.copy(pinned = !it.pinned) else it
                                                    }
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PushPin,
                                                contentDescription = "Pin",
                                                tint = if (note.pinned) cs.primary else cs.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                haptics.reject()
                                                onUpdateNotes(notes.filterNot { it.id == note.id })
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete note",
                                                tint = cs.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Task & Admin Checklist
        Surface(
            shape = MaterialTheme.shapes.large,
            color = ext.cardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckBox,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Task & Admin Checklist",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurface
                        )
                    }
                    if (!isReadOnly) {
                        TextButton(
                            onClick = { showAddTaskDialog = true },
                            modifier = Modifier.testTag("hub_add_task_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Task")
                        }
                    }
                }

                if (tasks.isEmpty()) {
                    Text(
                        text = "Keep track of exam registrations, lab records, and admin submissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                } else {
                    tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isReadOnly) {
                                    haptics.confirm()
                                    onUpdateTasks(
                                        tasks.map {
                                            if (it.id == task.id) it.copy(completed = !it.completed) else it
                                        }
                                    )
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (task.completed) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (task.completed) cs.primary else cs.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (task.completed) cs.onSurfaceVariant else cs.onSurface
                                )
                            }
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = {
                                        haptics.reject()
                                        onUpdateTasks(tasks.filterNot { it.id == task.id })
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete task",
                                        tint = cs.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Semester Milestone Timeline
        Surface(
            shape = MaterialTheme.shapes.large,
            color = ext.cardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = cs.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Academic Milestones",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurface
                        )
                    }
                    if (!isReadOnly) {
                        TextButton(
                            onClick = { showAddMilestoneDialog = true },
                            modifier = Modifier.testTag("hub_add_milestone_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Milestone")
                        }
                    }
                }

                if (milestones.isEmpty()) {
                    Text(
                        text = "Map your multi-year academic roadmap (2025 → 2028) offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                } else {
                    milestones.forEach { ms ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = cs.tertiaryContainer
                            ) {
                                Text(
                                    text = ms.year.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFeatureSettings = TABULAR_NUMS
                                    ),
                                    color = cs.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ms.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = cs.onSurface
                                )
                                if (ms.subtitle.isNotBlank()) {
                                    Text(
                                        text = ms.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurfaceVariant
                                    )
                                }
                            }
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = {
                                        haptics.reject()
                                        onUpdateMilestones(milestones.filterNot { it.id == ms.id })
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete milestone",
                                        tint = cs.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourseEditorDialog(
    initialCourse: CourseEntity?,
    semesterId: Long,
    savedProfessors: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CourseEntity) -> Unit,
    onDelete: ((CourseEntity) -> Unit)? = null
) {
    val presetCredits = remember { listOf(0.0, 1.5, 2.0, 3.0, 4.0) }
    val initialCreditsVal = initialCourse?.credits ?: 3.0
    val matchesPreset = remember(initialCourse) {
        presetCredits.any { abs(it - initialCreditsVal) < 0.0001 }
    }

    var code by remember(initialCourse) { mutableStateOf(initialCourse?.code ?: "") }
    var name by remember(initialCourse) { mutableStateOf(initialCourse?.name ?: "") }
    var professor by remember(initialCourse) { mutableStateOf(initialCourse?.professorName ?: "") }
    var selectedPresetCredit by remember(initialCourse) {
        mutableStateOf(if (matchesPreset) presetCredits.first { abs(it - initialCreditsVal) < 0.0001 } else null)
    }
    var isCustomCredits by remember(initialCourse) { mutableStateOf(!matchesPreset) }
    var customCreditsText by remember(initialCourse) {
        mutableStateOf(if (!matchesPreset) AcademicCalculator.formatCredits(initialCreditsVal) else "")
    }
    var isLab by remember(initialCourse) { mutableStateOf(initialCourse?.isLab ?: false) }
    var initialAttendedText by remember(initialCourse) {
        mutableStateOf(
            if ((initialCourse?.initialAttended ?: 0) == 0 && (initialCourse?.initialTotal ?: 0) == 0) ""
            else (initialCourse?.initialAttended ?: 0).toString()
        )
    }
    var initialTotalText by remember(initialCourse) {
        mutableStateOf(
            if ((initialCourse?.initialTotal ?: 0) == 0) ""
            else (initialCourse?.initialTotal ?: 0).toString()
        )
    }

    val resolvedCredits: Double? = if (isCustomCredits) {
        customCreditsText.trim().toDoubleOrNull()?.takeIf { it >= 0.0 && it.isFinite() }
    } else {
        selectedPresetCredit ?: 3.0
    }
    val creditsValid = resolvedCredits != null && resolvedCredits >= 0.0

    val parsedInitAttended = initialAttendedText.toIntOrNull() ?: 0
    val parsedInitTotal = initialTotalText.toIntOrNull() ?: 0
    val offsetValid = parsedInitAttended <= parsedInitTotal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCourse == null) "Add Course" else "Edit Course",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(12) },
                    label = { Text("Course Code (e.g. CS301)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_code_input")
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_name_input")
                )
                ProfessorAutocompleteField(
                    value = professor,
                    onValueChange = { professor = it },
                    savedProfessors = savedProfessors,
                    label = "Professor (Optional)",
                    testTagName = "course_professor_input"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Credits", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        presetCredits.forEach { cr ->
                            val label = AcademicCalculator.formatCredits(cr)
                            val isSelected = !isCustomCredits && selectedPresetCredit != null && abs(selectedPresetCredit!! - cr) < 0.0001
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isCustomCredits = false
                                    selectedPresetCredit = cr
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                modifier = Modifier.testTag("credit_preset_$label")
                            )
                        }
                        FilterChip(
                            selected = isCustomCredits,
                            onClick = {
                                if (!isCustomCredits) {
                                    isCustomCredits = true
                                    if (customCreditsText.isBlank()) {
                                        customCreditsText = selectedPresetCredit?.let { AcademicCalculator.formatCredits(it) } ?: ""
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = "Custom",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            modifier = Modifier.testTag("credit_preset_custom")
                        )
                    }

                    if (isCustomCredits) {
                        OutlinedTextField(
                            value = customCreditsText,
                            onValueChange = { raw ->
                                val filtered = buildString {
                                    var dotSeen = false
                                    for (ch in raw) {
                                        if (ch.isDigit()) {
                                            append(ch)
                                        } else if (ch == '.' && !dotSeen) {
                                            dotSeen = true
                                            append(ch)
                                        }
                                    }
                                }.take(6)
                                customCreditsText = filtered
                            },
                            label = { Text("Custom Credits") },
                            placeholder = { Text("e.g. 0.5, 1.5, 2.5") },
                            singleLine = true,
                            isError = !creditsValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                            supportingText = if (!creditsValid) {
                                { Text("Enter a valid credit value ≥ 0") }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_credits_input")
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isLab,
                        onClick = { isLab = false },
                        label = { Text("Theory (50/50)", maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isLab,
                        onClick = { isLab = true },
                        label = { Text("Lab (60/40)", maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Starting Attendance (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = initialAttendedText,
                            onValueChange = { raw ->
                                initialAttendedText = raw.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Initial Attended", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("0") },
                            singleLine = true,
                            isError = !offsetValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("course_initial_attended_input")
                        )
                        OutlinedTextField(
                            value = initialTotalText,
                            onValueChange = { raw ->
                                initialTotalText = raw.filter { it.isDigit() }.take(4)
                            },
                            label = { Text("Initial Total", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("0") },
                            singleLine = true,
                            isError = !offsetValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("course_initial_total_input")
                        )
                    }
                    if (!offsetValid) {
                        Text(
                            text = "Attended classes cannot exceed total classes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "Sync past classes if starting mid-semester",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (initialCourse != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            onDelete(initialCourse)
                            onDismiss()
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Course", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedCode = code.trim()
                    val trimmedName = name.trim()
                    val finalCredits = resolvedCredits
                    if (trimmedCode.isNotEmpty() && trimmedName.isNotEmpty() && offsetValid && finalCredits != null) {
                        val updated = (initialCourse ?: CourseEntity(
                            semesterId = semesterId,
                            code = trimmedCode,
                            name = trimmedName
                        )).copy(
                            code = trimmedCode,
                            name = trimmedName,
                            professorName = professor.trim(),
                            credits = finalCredits,
                            isLab = isLab,
                            initialAttended = parsedInitAttended,
                            initialTotal = parsedInitTotal
                        )
                        onSave(updated)
                        onDismiss()
                    }
                },
                enabled = code.isNotBlank() && name.isNotBlank() && offsetValid && creditsValid,
                modifier = Modifier.testTag("save_course_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
