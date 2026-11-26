package com.unideck.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.AttendanceStatus
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.ui.components.ArchivedSemesterBanner
import com.unideck.app.ui.components.AttendanceActionRow
import com.unideck.app.ui.components.AttendanceTrendSparkline
import com.unideck.app.ui.components.ExpressiveGroupedCard
import com.unideck.app.ui.components.HeroTintCard
import com.unideck.app.ui.components.UniformCardShape
import com.unideck.app.ui.components.healthColor
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    course: CourseEntity?,
    schedules: List<ClassScheduleEntity>,
    logs: List<AttendanceLogEntity>,
    preferences: UserPreferences,
    isReadOnly: Boolean = false,
    onUnlockArchived: () -> Unit = {},
    onBack: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onOpenAnalytics: (Long) -> Unit,
    onLogManualAttendance: (CourseEntity, String, String) -> Unit,
    onUpdateLogStatus: (Long, String) -> Unit,
    onDeleteLog: (Long) -> Unit
) {
    if (course == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Course not found")
        }
        return
    }

    val ext = LocalUniDeckColors.current
    val courseLogs = remember(logs, course.id) { logs.filter { it.courseId == course.id } }
    val courseSchedules = remember(schedules, course.id) { schedules.filter { it.courseId == course.id } }

    val stats = remember(course, courseLogs, preferences.minAttendanceThreshold) {
        AcademicCalculator.computeAttendanceStats(
            logs = courseLogs,
            thresholdPercent = preferences.minAttendanceThreshold,
            initialAttended = course.initialAttended,
            initialTotal = course.initialTotal
        )
    }
    val breakdowns = remember(courseLogs, courseSchedules) {
        AcademicCalculator.computeComponentBreakdowns(courseLogs, courseSchedules)
    }
    val trendPoints = remember(course, courseLogs) {
        AcademicCalculator.computeTrendPoints(
            logs = courseLogs,
            initialAttended = course.initialAttended,
            initialTotal = course.initialTotal
        )
    }
    val monthlyPct = remember(courseLogs) {
        AcademicCalculator.computeMonthlyPercentage(courseLogs)
    }
    var expandedLogId by remember { mutableStateOf<Long?>(null) }

    val pagerState = rememberPagerState(pageCount = { breakdowns.size })
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    // Build upcoming scheduled classes sorted from today forward
    val upcoming = remember(courseSchedules) {
        val today = LocalDate.now()
        (0..6).flatMap { offset ->
            val date = today.plusDays(offset.toLong())
            courseSchedules
                .filter { it.dayOfWeek == date.dayOfWeek.value }
                .sortedBy { it.startTime }
                .map { slot -> date to slot }
        }.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(course.code, style = MaterialTheme.typography.titleLarge)
                        Text(
                            course.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isReadOnly) {
                        IconButton(onClick = { onEditCourse(course) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Course")
                        }
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isReadOnly) {
                item(key = "detail_archived_banner") {
                    ArchivedSemesterBanner(onUnlock = onUnlockArchived)
                }
            }
            // 1. Hero Header with Split-Decimal 44sp + 20sp Typography
            item(key = "hero_header") {
                val wholePart = if (stats.totalCounted == 0) "--" else stats.percentage.toInt().toString()
                val decimalPart = if (stats.totalCounted == 0) "" else {
                    ".%02d%%".format(((stats.percentage - stats.percentage.toInt()) * 100).toInt().coerceIn(0, 99))
                }
                val accent = if (stats.totalCounted == 0) MaterialTheme.colorScheme.primary else healthColor(stats.health)

                HeroTintCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "ATTENDANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            fontSize = 44.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = (-0.75).sp,
                                            fontFeatureSettings = TABULAR_NUMS,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        append(wholePart)
                                    }
                                    withStyle(
                                        SpanStyle(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFeatureSettings = TABULAR_NUMS,
                                            color = accent
                                        )
                                    ) {
                                        append(decimalPart)
                                    }
                                }
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${stats.present}/${stats.totalCounted} classes",
                                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Required: ${preferences.minAttendanceThreshold}%",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (course.initialTotal > 0) {
                                Text(
                                    text = "Base offset: ${course.initialAttended}/${course.initialTotal}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { if (stats.totalCounted == 0) 0f else (stats.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stats.statusHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    AttendanceActionRow(
                        activeStatus = null,
                        enabled = !isReadOnly,
                        onSelectStatus = { st ->
                            val comp = breakdowns.getOrNull(pagerState.currentPage)?.componentType
                                ?: if (course.isLab) ComponentType.LAB else ComponentType.LECTURE
                            onLogManualAttendance(course, st, comp)
                        },
                        testTagPrefix = "detail_manual"
                    )
                }
            }

            // 2. Swipeable Components Card (LECTURE / LAB / TUTORIAL)
            item(key = "swipeable_components") {
                Surface(
                    shape = UniformCardShape,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Component Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                breakdowns.indices.forEach { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (pagerState.currentPage == i) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (pagerState.currentPage == i) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline
                                                }
                                            )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalPager(state = pagerState) { page ->
                            val item = breakdowns[page]
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.componentType,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Slot Weight ${item.slotWeight}x",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (item.totalCounted == 0) "--" else "%.1f%%".format(item.percentage),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFeatureSettings = TABULAR_NUMS
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        MetricMiniCol("Timetable", "${item.timetablePresent}/${item.timetableTotal}")
                                        MetricMiniCol("Manual", "${item.manualPresent}/${item.manualTotal}")
                                        MetricMiniCol("Total Ratio", "${item.totalPresent}/${item.totalCounted}")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Attendance Trend Card (Tap to open Full Interactive Analytics)
            item(key = "trend_card") {
                Surface(
                    shape = UniformCardShape,
                    color = ext.cardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(UniformCardShape)
                        .clickable { onOpenAnalytics(course.id) }
                        .testTag("open_analytics_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Attendance Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (monthlyPct != null) {
                                            "This month: ${"%.1f%%".format(monthlyPct)}"
                                        } else {
                                            "This month: --"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Analytics",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Open full analytics",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        AttendanceTrendSparkline(
                            points = trendPoints,
                            thresholdPercent = preferences.minAttendanceThreshold
                        )
                    }
                }
            }

            // 4. Upcoming Classes
            item(key = "upcoming_header") {
                Text(
                    text = "Upcoming Classes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (upcoming.isEmpty()) {
                item(key = "no_upcoming") {
                    Surface(
                        shape = UniformCardShape,
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No weekly timetable slots configured for ${course.code} yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                item(key = "upcoming_list") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        upcoming.forEachIndexed { idx, (date, slot) ->
                            ExpressiveGroupedCard(index = idx, count = upcoming.size) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "${DayOfWeek.of(slot.dayOfWeek).getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${date.format(dateFmt)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${AcademicCalculator.formatMinutes(slot.startTime, preferences.use24HourFormat)} – ${AcademicCalculator.formatMinutes(slot.endTime, preferences.use24HourFormat)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = slot.componentType,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (slot.roomNumber.isNotBlank()) {
                                            Text(
                                                text = "Room ${slot.roomNumber}",
                                                style = MaterialTheme.typography.labelSmall,
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

            // 5. Complete Per-Subject Attendance Log History (Tap any row for inline edit/delete)
            item(key = "log_history_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log History (${courseLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (courseLogs.isNotEmpty() && !isReadOnly) {
                        Text(
                            text = "Tap any entry to edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (courseLogs.isEmpty()) {
                item(key = "empty_log_history") {
                    Surface(
                        shape = UniformCardShape,
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No attendance logs recorded for ${course.code} yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                item(key = "log_history_group") {
                    val allHistory = remember(courseLogs) {
                        courseLogs.sortedWith(
                            compareByDescending<AttendanceLogEntity> { it.dateEpochDay }.thenByDescending { it.id }
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        allHistory.forEachIndexed { idx, log ->
                            val isExpanded = !isReadOnly && expandedLogId == log.id
                            val (pillFill, pillContent, statusLabel) = when (log.status) {
                                AttendanceStatus.PRESENT -> Triple(ext.presentFill, ext.presentContent, "Present")
                                AttendanceStatus.ABSENT -> Triple(ext.absentFill, ext.absentContent, "Absent")
                                else -> Triple(ext.cancelledFill, ext.cancelledContent, "Cancelled")
                            }
                            ExpressiveGroupedCard(
                                index = idx,
                                count = allHistory.size,
                                onClick = if (isReadOnly) null else {
                                    { expandedLogId = if (isExpanded) null else log.id }
                                },
                                modifier = Modifier.testTag("log_history_row_${log.id}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = LocalDate.ofEpochDay(log.dateEpochDay).format(dateFmt),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${log.componentType.lowercase().replaceFirstChar { it.uppercase() }} • ${if (log.isManualEntry) "Manual" else "Timetable"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = pillFill,
                                            border = BorderStroke(1.dp, pillContent.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = pillContent,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        if (!isReadOnly) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                contentDescription = if (isExpanded) "Collapse editor" else "Edit log entry",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            InlineLogActionChip(
                                                label = "Present",
                                                selected = log.status == AttendanceStatus.PRESENT,
                                                fill = ext.presentFill,
                                                content = ext.presentContent,
                                                onClick = {
                                                    onUpdateLogStatus(log.id, AttendanceStatus.PRESENT)
                                                    expandedLogId = null
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("edit_log_present_${log.id}")
                                            )
                                            InlineLogActionChip(
                                                label = "Absent",
                                                selected = log.status == AttendanceStatus.ABSENT,
                                                fill = ext.absentFill,
                                                content = ext.absentContent,
                                                onClick = {
                                                    onUpdateLogStatus(log.id, AttendanceStatus.ABSENT)
                                                    expandedLogId = null
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("edit_log_absent_${log.id}")
                                            )
                                            InlineLogActionChip(
                                                label = "Cancelled",
                                                selected = log.status == AttendanceStatus.CANCELLED,
                                                fill = ext.cancelledFill,
                                                content = ext.cancelledContent,
                                                onClick = {
                                                    onUpdateLogStatus(log.id, AttendanceStatus.CANCELLED)
                                                    expandedLogId = null
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("edit_log_cancelled_${log.id}")
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        onDeleteLog(log.id)
                                                        expandedLogId = null
                                                    }
                                                    .testTag("delete_log_${log.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Delete,
                                                        contentDescription = "Delete log entry",
                                                        tint = MaterialTheme.colorScheme.error,
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
            }
        }
    }
}

@Composable
private fun InlineLogActionChip(
    label: String,
    selected: Boolean,
    fill: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) fill else fill.copy(alpha = 0.45f),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) content else content.copy(alpha = 0.35f)),
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = content,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun MetricMiniCol(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = TABULAR_NUMS),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
