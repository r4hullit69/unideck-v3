package com.unideck.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.data.local.AttendanceStatus
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.OneOffClassEntity
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.domain.AttendanceHealth
import com.unideck.app.domain.AttendanceStats
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.sin

data class DailyTimelineItem(
    val key: String,
    val course: CourseEntity,
    val startTime: Int,
    val endTime: Int,
    val roomNumber: String,
    val componentType: String,
    val activeStatus: String?,
    val isOneOff: Boolean,
    val oneOffEntity: OneOffClassEntity? = null
)

fun componentBadgeLetter(componentType: String): String = when (componentType) {
    ComponentType.LAB -> "P"
    ComponentType.TUTORIAL -> "T"
    else -> "L"
}

fun componentTypeDisplayLabel(componentType: String, isLabCourse: Boolean): String = when {
    componentType == ComponentType.LAB || isLabCourse -> "Lab"
    componentType == ComponentType.TUTORIAL -> "Tutorial"
    else -> "Theory"
}

fun componentUnitLabel(componentType: String): String = when (componentType) {
    ComponentType.LAB -> "Lab(s)"
    ComponentType.TUTORIAL -> "Tut(s)"
    else -> "Lec(s)"
}

fun formatTimelineBunkLine(stats: AttendanceStats, componentType: String, thresholdPercent: Int): String {
    val unit = componentUnitLabel(componentType)
    return when {
        stats.totalCounted == 0 -> "No classes logged yet."
        stats.percentage < thresholdPercent -> "Attend next ${stats.classesNeeded} $unit."
        stats.safeBunks == 0 -> "On edge — don't miss next ${unit.removeSuffix("(s)")}."
        else -> "Can miss up to ${stats.safeBunks} $unit."
    }
}

private enum class TimelineSlotTemporalState {
    COMPLETED,
    LIVE,
    UPCOMING
}

@Composable
fun DailyClassTimelineRow(
    item: DailyTimelineItem,
    stats: AttendanceStats,
    thresholdPercent: Int,
    use24Hour: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    index: Int = 0,
    totalCount: Int = 1,
    selectedDate: LocalDate = LocalDate.now(),
    cutCorners: Boolean = false,
    onOpenCourseDetail: () -> Unit,
    onSelectStatus: (String) -> Unit,
    onDeleteOneOff: (() -> Unit)? = null,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ext = LocalUniDeckColors.current
    val cs = MaterialTheme.colorScheme
    val primary = cs.primary
    val haptics = rememberUniDeckHaptics()

    val today = remember { LocalDate.now() }
    val nowMinutes = remember { LocalTime.now().let { it.hour * 60 + it.minute } }
    val temporalState = remember(selectedDate, today, nowMinutes, item.startTime, item.endTime, item.activeStatus) {
        when {
            selectedDate.isBefore(today) -> TimelineSlotTemporalState.COMPLETED
            selectedDate.isAfter(today) -> TimelineSlotTemporalState.UPCOMING
            nowMinutes in item.startTime..item.endTime -> TimelineSlotTemporalState.LIVE
            nowMinutes > item.endTime || item.activeStatus != null -> TimelineSlotTemporalState.COMPLETED
            else -> TimelineSlotTemporalState.UPCOMING
        }
    }
    val isLive = temporalState == TimelineSlotTemporalState.LIVE

    val wavePhase by rememberInfiniteTransition(label = "liveWave").animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val startFormatted = remember(item.startTime, use24Hour) {
        AcademicCalculator.formatMinutes(item.startTime, use24Hour)
    }
    val endFormatted = remember(item.endTime, use24Hour) {
        AcademicCalculator.formatMinutes(item.endTime, use24Hour)
    }
    val bunkLine = remember(stats, item.componentType, thresholdPercent) {
        formatTimelineBunkLine(stats, item.componentType, thresholdPercent)
    }
    val bunkColor = when {
        isLive -> cs.onPrimaryContainer.copy(alpha = 0.9f)
        stats.totalCounted == 0 -> cs.onSurfaceVariant
        stats.health == AttendanceHealth.DANGER -> ext.danger
        stats.health == AttendanceHealth.MARGIN -> ext.margin
        else -> ext.safe
    }

    val cardShape = expressiveConnectedCardShape(
        index = index,
        count = totalCount,
        cutCorners = cutCorners
    )
    val cardContainerColor = if (isLive) cs.primaryContainer else ext.cardSurface
    val cardTitleColor = if (isLive) cs.onPrimaryContainer else cs.onSurface
    val cardSubColor = if (isLive) cs.onPrimaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(bottom = if (isLast) 0.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Left Time-Rail (52.dp width): Start time at top, vertical line / sine-wave in center, end time at bottom
        Column(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight()
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = startFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    fontFeatureSettings = TABULAR_NUMS
                ),
                color = if (isLive) primary else cs.onSurface,
                maxLines = 1,
                softWrap = false
            )

            val upcomingRailColor = cs.outlineVariant.copy(alpha = 0.4f)
            Canvas(
                modifier = Modifier
                    .width(18.dp)
                    .weight(1f)
                    .padding(vertical = 4.dp)
            ) {
                val centerX = size.width / 2f
                val strokePx = 2.4.dp.toPx()
                when (temporalState) {
                    TimelineSlotTemporalState.LIVE -> {
                        val path = Path()
                        val amp = 3.2.dp.toPx()
                        val wavelength = 18.dp.toPx().coerceAtLeast(8f)
                        val steps = 36
                        for (s in 0..steps) {
                            val y = (s.toFloat() / steps) * size.height
                            val angle = (y / wavelength) * (2f * PI.toFloat()) + wavePhase
                            val x = centerX + amp * sin(angle)
                            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = primary,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                    TimelineSlotTemporalState.COMPLETED -> {
                        drawLine(
                            color = primary,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    TimelineSlotTemporalState.UPCOMING -> {
                        drawLine(
                            color = upcomingRailColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Text(
                text = endFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    fontFeatureSettings = TABULAR_NUMS
                ),
                color = cs.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 2. Right Connected Class Card (4.dp gap & grouped container corners)
        Surface(
            shape = cardShape,
            color = cardContainerColor,
            modifier = Modifier
                .weight(1f)
                .clip(cardShape)
                .testTag("timeline_card_${item.key}")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Top Row: Leading Faculty/Course Icon + Course Title + Professor + Attendance Ring
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenCourseDetail),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLive) cs.primary else cs.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = if (isLive) cs.onPrimary else cs.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.course.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = cardTitleColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isLive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = cs.primary
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.5.sp
                                        ),
                                        color = cs.onPrimary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (item.isOneOff) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = cs.primaryContainer
                                ) {
                                    Text(
                                        text = "Extra",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        val profLine = item.course.professorName.ifBlank { item.course.code }
                        Text(
                            text = "$profLine • $bunkLine",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = if (stats.totalCounted > 0) bunkColor else cardSubColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (item.isOneOff && onDeleteOneOff != null && !isReadOnly) {
                        IconButton(
                            onClick = onDeleteOneOff,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove extra class",
                                tint = cardSubColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    CircularAttendanceRing(
                        percentage = stats.percentage,
                        health = stats.health,
                        hasData = stats.totalCounted > 0,
                        size = 44.dp,
                        strokeWidth = 4.5.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: Bottom-left tonal pills for Type & Room | Bottom-right one-tap Pre / Abs / Can pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bottom-Left: Type & Room tonal pills
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        val typeLabel = componentTypeDisplayLabel(item.componentType, item.course.isLab)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLive) cs.surface.copy(alpha = 0.32f) else cs.secondaryContainer
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = if (isLive) cs.onPrimaryContainer else cs.onSecondaryContainer,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (item.roomNumber.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isLive) cs.surface.copy(alpha = 0.25f) else cs.surfaceContainerHigh
                            ) {
                                Text(
                                    text = item.roomNumber,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp,
                                        fontFeatureSettings = TABULAR_NUMS
                                    ),
                                    color = if (isLive) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Bottom-Right: Larger one-tap Attendance status pills (Can | Abs | Pre)
                    val statusPills = listOf(
                        AttendanceStatus.CANCELLED to "Can",
                        AttendanceStatus.ABSENT to "Abs",
                        AttendanceStatus.PRESENT to "Pre"
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(if (isReadOnly) 0.45f else 1f)
                    ) {
                        statusPills.forEach { (statusKey, label) ->
                            val isSelected = item.activeStatus == statusKey
                            val pillBg = when {
                                isSelected && statusKey == AttendanceStatus.PRESENT -> ext.presentFill
                                isSelected && statusKey == AttendanceStatus.ABSENT -> ext.absentFill
                                isSelected -> ext.cancelledFill
                                isLive -> cs.surface.copy(alpha = 0.30f)
                                else -> cs.surfaceContainerHigh
                            }
                            val pillFg = when {
                                isSelected && statusKey == AttendanceStatus.PRESENT -> ext.presentContent
                                isSelected && statusKey == AttendanceStatus.ABSENT -> ext.absentContent
                                isSelected -> ext.cancelledContent
                                isLive -> cs.onPrimaryContainer
                                else -> cs.onSurfaceVariant
                            }
                            val pillRadius by androidx.compose.animation.core.animateDpAsState(
                                targetValue = if (isSelected) 20.dp else 12.dp,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = 0.8f,
                                    stiffness = 380f
                                ),
                                label = "statusPillRadius"
                            )
                            val pillShape = RoundedCornerShape(pillRadius)

                            Surface(
                                shape = pillShape,
                                color = pillBg,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 54.dp, minHeight = 40.dp)
                                    .clip(pillShape)
                                    .clickable(enabled = !isReadOnly) {
                                        haptics.forAttendanceStatus(statusKey)
                                        onSelectStatus(statusKey)
                                    }
                                    .testTag("timeline_${item.key}_${label.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = pillFg,
                                        maxLines = 1,
                                        softWrap = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOneOffClassBottomSheet(
    selectedDate: LocalDate,
    courses: List<CourseEntity>,
    savedProfessors: List<String>,
    use24Hour: Boolean,
    onDismiss: () -> Unit,
    onCreateCourseInstead: () -> Unit,
    onUpdateCourseProfessor: (Long, String) -> Unit = { _, _ -> },
    onAddClass: (courseId: Long, startTime: Int, endTime: Int, roomNumber: String, componentType: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    var selectedCourseId by remember(courses) { mutableStateOf(courses.firstOrNull()?.id ?: 0L) }
    var componentType by remember { mutableStateOf(ComponentType.LECTURE) }
    var startMinutes by remember { mutableIntStateOf(600) } // 10:00 AM
    var endMinutes by remember { mutableIntStateOf(660) }   // 11:00 AM
    var room by remember { mutableStateOf("") }
    var professor by remember(courses) { mutableStateOf(courses.firstOrNull()?.professorName ?: "") }
    var pickingStartTime by remember { mutableStateOf(false) }
    var pickingEndTime by remember { mutableStateOf(false) }

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Add Extra / Rescheduled Class",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "For ${selectedDate.format(dateFmt)} only • Does not alter your weekly timetable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (courses.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Create a course first to schedule a one-off class.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                onDismiss()
                                onCreateCourseInstead()
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Course")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Subject", style = MaterialTheme.typography.labelLarge)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(courses) { course ->
                            FilterChip(
                                selected = selectedCourseId == course.id,
                                onClick = {
                                    selectedCourseId = course.id
                                    professor = course.professorName
                                    if (course.isLab) componentType = ComponentType.LAB
                                },
                                label = { Text("${course.code} • ${course.name}", maxLines = 1) },
                                modifier = Modifier.testTag("oneoff_course_${course.id}")
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Component Type", style = MaterialTheme.typography.labelLarge)
                    val compOptions = listOf(
                        ComponentType.LECTURE to "Lecture (L)",
                        ComponentType.LAB to "Lab (P)",
                        ComponentType.TUTORIAL to "Tutorial (T)"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        compOptions.forEachIndexed { idx, (type, label) ->
                            SegmentedButton(
                                selected = componentType == type,
                                onClick = { componentType = type },
                                shape = SegmentedButtonDefaults.itemShape(idx, compOptions.size),
                                label = { Text(label, maxLines = 1) }
                            )
                        }
                    }
                }

                // Material You Start & End Time Picker Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimePickerPill(
                        label = "Start Time",
                        formattedTime = AcademicCalculator.formatMinutes(startMinutes, use24Hour),
                        onClick = { pickingStartTime = true },
                        modifier = Modifier.weight(1f),
                        testTagName = "oneoff_start_time_pill"
                    )
                    TimePickerPill(
                        label = "End Time",
                        formattedTime = AcademicCalculator.formatMinutes(endMinutes, use24Hour),
                        onClick = { pickingEndTime = true },
                        modifier = Modifier.weight(1f),
                        testTagName = "oneoff_end_time_pill"
                    )
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Lab (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("oneoff_room_input")
                )

                ProfessorAutocompleteField(
                    value = professor,
                    onValueChange = { professor = it },
                    savedProfessors = savedProfessors,
                    label = "Professor (Optional)",
                    testTagName = "oneoff_professor_input"
                )

                Button(
                    onClick = {
                        if (selectedCourseId != 0L) {
                            onUpdateCourseProfessor(selectedCourseId, professor)
                            onAddClass(selectedCourseId, startMinutes, endMinutes, room, componentType)
                            onDismiss()
                        }
                    },
                    enabled = selectedCourseId != 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("oneoff_save_button")
                ) {
                    Text("Add to ${selectedDate.format(dateFmt)}")
                }
            }
        }
    }
}
