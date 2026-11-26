package com.unideck.app.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.unideck.app.MainActivity
import com.unideck.app.R
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.UniDeckDatabase
import com.unideck.app.data.local.isDateWithinSemester
import com.unideck.app.data.prefs.ThemeMode
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.data.prefs.UserPreferencesRepository
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.domain.DailyAttendanceSnapshot
import com.unideck.app.domain.NextClassInfo
import com.unideck.app.ui.theme.PitchBlackBackground
import com.unideck.app.ui.theme.PitchBlackCard
import com.unideck.app.ui.theme.PureWhiteBackground
import com.unideck.app.ui.theme.PureWhiteCard
import com.unideck.app.ui.theme.buildDarkScheme
import com.unideck.app.ui.theme.buildLightScheme
import com.unideck.app.ui.theme.resolveUniDeckPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

@Composable
private fun UniDeckGlanceTheme(
    context: Context,
    preferences: UserPreferences,
    content: @Composable () -> Unit
) {
    if (preferences.dynamicMonetEnabled &&
        !preferences.pitchBlackEnabled &&
        preferences.themeMode == ThemeMode.SYSTEM &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        GlanceTheme(content = content)
        return
    }

    val palette = resolveUniDeckPalette(preferences.accentKey)
    val lightScheme = if (preferences.dynamicMonetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val base = dynamicLightColorScheme(context)
        if (preferences.pitchBlackEnabled) {
            base.copy(
                background = PureWhiteBackground,
                surface = PureWhiteBackground,
                surfaceVariant = lerp(PureWhiteCard, base.primary, 0.045f)
            )
        } else {
            base
        }
    } else {
        buildLightScheme(palette, preferences.pitchBlackEnabled, preferences.paletteStyle)
    }

    val darkScheme = if (preferences.dynamicMonetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val base = dynamicDarkColorScheme(context)
        if (preferences.pitchBlackEnabled) {
            base.copy(
                background = PitchBlackBackground,
                surface = PitchBlackBackground,
                surfaceVariant = PitchBlackCard
            )
        } else {
            base
        }
    } else {
        buildDarkScheme(palette, preferences.pitchBlackEnabled, preferences.paletteStyle)
    }

    val providers = when (preferences.themeMode) {
        ThemeMode.LIGHT -> ColorProviders(light = lightScheme, dark = lightScheme)
        ThemeMode.DARK -> ColorProviders(light = darkScheme, dark = darkScheme)
        ThemeMode.SYSTEM -> ColorProviders(light = lightScheme, dark = darkScheme)
    }

    GlanceTheme(colors = providers, content = content)
}

data class WidgetClassRow(
    val code: String,
    val name: String,
    val timeRange: String,
    val typeLabel: String,
    val room: String
)

private data class TodaysClassesWidgetState(
    val semesterName: String,
    val dayLabel: String,
    val rows: List<WidgetClassRow>,
    val outsideSemester: Boolean,
    val preferences: UserPreferences
)

class TodaysClassesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetState = withContext(Dispatchers.IO) {
            val dao = UniDeckDatabase.getInstance(context).uniDeckDao()
            val prefs = UserPreferencesRepository(context).getPreferencesSync()
            val sem = dao.getCurrentSemesterSync()
            val today = LocalDate.now()
            val shortDay = today.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
            val shortMonth = today.month.getDisplayName(DateTextStyle.SHORT, Locale.getDefault())
            val dayLabel = "Today • $shortDay, ${today.dayOfMonth} $shortMonth"
            if (sem == null) {
                TodaysClassesWidgetState("UniDeck", dayLabel, emptyList(), outsideSemester = false, preferences = prefs)
            } else {
                val withinSemester = sem.isDateWithinSemester(today.toEpochDay())
                val courses = dao.getCoursesBySemesterSync(sem.id).associateBy { it.id }
                val recurringRows = if (withinSemester) {
                    dao.getSchedulesBySemesterSync(sem.id)
                        .filter { it.dayOfWeek == today.dayOfWeek.value }
                        .mapNotNull { slot ->
                            val course = courses[slot.courseId] ?: return@mapNotNull null
                            val typeStr = when {
                                slot.componentType == ComponentType.LAB || course.isLab -> "Lab"
                                slot.componentType == ComponentType.TUTORIAL -> "Tut"
                                else -> "Theory"
                            }
                            slot.startTime to WidgetClassRow(
                                code = course.code,
                                name = course.name,
                                timeRange = "${AcademicCalculator.formatMinutes(slot.startTime, prefs.use24HourFormat)}–${AcademicCalculator.formatMinutes(slot.endTime, prefs.use24HourFormat)}",
                                typeLabel = typeStr,
                                room = slot.roomNumber.ifBlank { typeStr }
                            )
                        }
                } else {
                    emptyList()
                }
                val oneOffRows = dao.getAllOneOffClassesSync()
                    .filter { it.semesterId == sem.id && it.dateEpochDay == today.toEpochDay() }
                    .mapNotNull { extra ->
                        val course = courses[extra.courseId] ?: return@mapNotNull null
                        val typeStr = when {
                            extra.componentType == ComponentType.LAB || course.isLab -> "Lab"
                            extra.componentType == ComponentType.TUTORIAL -> "Tut"
                            else -> "Theory"
                        }
                        extra.startTime to WidgetClassRow(
                            code = course.code,
                            name = course.name,
                            timeRange = "${AcademicCalculator.formatMinutes(extra.startTime, prefs.use24HourFormat)}–${AcademicCalculator.formatMinutes(extra.endTime, prefs.use24HourFormat)}",
                            typeLabel = typeStr,
                            room = extra.roomNumber.ifBlank { typeStr }
                        )
                    }
                val todaySlots = (recurringRows + oneOffRows)
                    .sortedBy { it.first }
                    .take(4)
                    .map { it.second }
                TodaysClassesWidgetState(
                    semesterName = sem.name,
                    dayLabel = dayLabel,
                    rows = todaySlots,
                    outsideSemester = !withinSemester,
                    preferences = prefs
                )
            }
        }

        provideContent {
            UniDeckGlanceTheme(context = context, preferences = widgetState.preferences) {
                TodaysClassesContent(
                    semesterName = widgetState.semesterName,
                    dayLabel = widgetState.dayLabel,
                    rows = widgetState.rows,
                    outsideSemester = widgetState.outsideSemester
                )
            }
        }
    }
}

@Composable
private fun TodaysClassesContent(
    semesterName: String,
    dayLabel: String,
    rows: List<WidgetClassRow>,
    outsideSemester: Boolean
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = semesterName,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = dayLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (rows.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (outsideSemester) "Outside semester dates" else "No classes scheduled today",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            rows.forEach { item ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .cornerRadius(12.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.timeRange,
                        style = TextStyle(
                            color = GlanceTheme.colors.primary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = item.name,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "${item.typeLabel} • ${item.room}",
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

class TodaysClassesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodaysClassesWidget()
}

private data class AttendanceGpaWidgetState(
    val semesterName: String,
    val weekLabel: String,
    val attendanceText: String,
    val sgpaText: String,
    val preferences: UserPreferences
)

class AttendanceGpaWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) {
            val dao = UniDeckDatabase.getInstance(context).uniDeckDao()
            val prefs = UserPreferencesRepository(context).getPreferencesSync()
            val sem = dao.getCurrentSemesterSync()
            val todayEpoch = LocalDate.now().toEpochDay()
            if (sem == null) {
                AttendanceGpaWidgetState("UniDeck", "Week 1", "--", "--", prefs)
            } else {
                val startEpoch = sem.startDateEpochDay ?: todayEpoch
                val weekNum = (((todayEpoch - startEpoch) / 7L) + 1L).coerceIn(1L, 52L)
                val courses = dao.getCoursesBySemesterSync(sem.id)
                val courseIds = courses.map { it.id }.toSet()
                val logs = dao.getAllAttendanceLogsSync().filter { it.courseId in courseIds }
                val initAttended = courses.sumOf { it.initialAttended.coerceAtLeast(0) }
                val initTotal = courses.sumOf { it.initialTotal.coerceAtLeast(0) }
                val att = AcademicCalculator.computeAttendanceStats(
                    logs = logs,
                    thresholdPercent = prefs.minAttendanceThreshold,
                    initialAttended = initAttended,
                    initialTotal = initTotal
                )
                val sgpa = AcademicCalculator.computeSgpa(courses, prefs.gradeCutoffs)
                val attStr = if (att.totalCounted == 0) "--" else "%.1f%%".format(att.percentage)
                val sgpaStr = if (courses.isEmpty()) "--" else "%.2f".format(sgpa)
                AttendanceGpaWidgetState(sem.name, "Week $weekNum", attStr, sgpaStr, prefs)
            }
        }

        provideContent {
            UniDeckGlanceTheme(context = context, preferences = state.preferences) {
                AttendanceGpaContent(
                    semesterName = state.semesterName,
                    weekLabel = state.weekLabel,
                    attendanceText = state.attendanceText,
                    sgpaText = state.sgpaText
                )
            }
        }
    }
}

@Composable
private fun AttendanceGpaContent(
    semesterName: String,
    weekLabel: String,
    attendanceText: String,
    sgpaText: String
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = semesterName,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = weekLabel,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(14.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text = "ATTENDANCE",
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = attendanceText,
                style = TextStyle(
                    color = GlanceTheme.colors.onPrimaryContainer,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(14.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Text(
                text = "SGPA",
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = sgpaText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

class AttendanceGpaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AttendanceGpaWidget()
}

private data class NextClassWidgetState(
    val semesterName: String,
    val nextClass: NextClassInfo?,
    val dailyAttendance: DailyAttendanceSnapshot,
    val use24HourFormat: Boolean,
    val minAttendanceThreshold: Int,
    val preferences: UserPreferences
)

class NextClassAttendanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 100.dp),
            DpSize(250.dp, 100.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) {
            val dao = UniDeckDatabase.getInstance(context).uniDeckDao()
            val prefs = UserPreferencesRepository(context).getPreferencesSync()
            val sem = dao.getCurrentSemesterSync()
            val today = LocalDate.now()
            val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }

            if (sem == null) {
                NextClassWidgetState(
                    semesterName = "UniDeck",
                    nextClass = null,
                    dailyAttendance = DailyAttendanceSnapshot(0, 0, 0, 0, null, 0),
                    use24HourFormat = prefs.use24HourFormat,
                    minAttendanceThreshold = prefs.minAttendanceThreshold,
                    preferences = prefs
                )
            } else {
                val courses = dao.getCoursesBySemesterSync(sem.id)
                val schedules = dao.getSchedulesBySemesterSync(sem.id)
                val oneOffs = dao.getAllOneOffClassesSync().filter { it.semesterId == sem.id }
                val logs = dao.getAllAttendanceLogsSync()

                val nextClass = AcademicCalculator.computeNextClass(
                    courses = courses,
                    schedules = schedules,
                    oneOffClasses = oneOffs,
                    today = today,
                    nowMinutes = nowMinutes,
                    startDateEpochDay = sem.startDateEpochDay,
                    endDateEpochDay = sem.endDateEpochDay
                )
                val daily = AcademicCalculator.computeDailyAttendance(
                    courses = courses,
                    schedules = schedules,
                    oneOffClasses = oneOffs,
                    logs = logs,
                    today = today,
                    startDateEpochDay = sem.startDateEpochDay,
                    endDateEpochDay = sem.endDateEpochDay
                )
                NextClassWidgetState(
                    semesterName = sem.name,
                    nextClass = nextClass,
                    dailyAttendance = daily,
                    use24HourFormat = prefs.use24HourFormat,
                    minAttendanceThreshold = prefs.minAttendanceThreshold,
                    preferences = prefs
                )
            }
        }

        provideContent {
            UniDeckGlanceTheme(context = context, preferences = state.preferences) {
                NextClassAttendanceContent(state = state)
            }
        }
    }
}

@Composable
private fun NextClassAttendanceContent(state: NextClassWidgetState) {
    val size = LocalSize.current
    val isNarrow = size.width < 225.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(GlanceTheme.colors.surface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp)
    ) {
        // Header Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UniDeck • ${state.semesterName}",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Box(
                modifier = GlanceModifier
                    .size(26.dp)
                    .cornerRadius(13.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .clickable(actionRunCallback<RefreshNextClassAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = "Refresh widget",
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                    modifier = GlanceModifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (isNarrow) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                NextClassCard(
                    nextClass = state.nextClass,
                    use24Hour = state.use24HourFormat,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                DailyAttendanceCompactBar(
                    daily = state.dailyAttendance,
                    threshold = state.minAttendanceThreshold
                )
            }
        } else {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NextClassCard(
                    nextClass = state.nextClass,
                    use24Hour = state.use24HourFormat,
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                DailyAttendanceCard(
                    daily = state.dailyAttendance,
                    threshold = state.minAttendanceThreshold,
                    modifier = GlanceModifier
                        .width(108.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun NextClassCard(
    nextClass: NextClassInfo?,
    use24Hour: Boolean,
    modifier: ModifierAlias = GlanceModifier
) {
    Column(
        modifier = modifier
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (nextClass == null) {
            Text(
                text = "NEXT CLASS",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "No upcoming classes",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Add slots in Timetable",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        } else {
            val statusBadge = when {
                nextClass.isOngoing -> "NOW"
                nextClass.dayOffset == 0 -> "NEXT TODAY"
                else -> nextClass.dayLabel.uppercase()
            }
            val compShort = when (nextClass.componentType) {
                ComponentType.LAB -> "Lab"
                ComponentType.TUTORIAL -> "Tut"
                else -> "Lec"
            }
            val startStr = AcademicCalculator.formatMinutes(nextClass.startTime, use24Hour)
            val endStr = AcademicCalculator.formatMinutes(nextClass.endTime, use24Hour)
            val roomSuffix = if (nextClass.roomNumber.isNotBlank()) " • ${nextClass.roomNumber}" else ""

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .background(
                            if (nextClass.isOngoing) GlanceTheme.colors.primary
                            else GlanceTheme.colors.primaryContainer
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusBadge,
                        maxLines = 1,
                        style = TextStyle(
                            color = if (nextClass.isOngoing) GlanceTheme.colors.onPrimary
                            else GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = compShort,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = "${nextClass.courseCode} • ${nextClass.courseName}",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = "$startStr – $endStr$roomSuffix",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

private typealias ModifierAlias = GlanceModifier

@Composable
private fun DailyAttendanceCard(
    daily: DailyAttendanceSnapshot,
    threshold: Int,
    modifier: GlanceModifier = GlanceModifier
) {
    val pct = daily.percentage
    val bgColor = when {
        pct == null -> GlanceTheme.colors.secondaryContainer
        pct >= threshold -> GlanceTheme.colors.primaryContainer
        else -> GlanceTheme.colors.errorContainer
    }
    val contentColor = when {
        pct == null -> GlanceTheme.colors.onSecondaryContainer
        pct >= threshold -> GlanceTheme.colors.onPrimaryContainer
        else -> GlanceTheme.colors.onErrorContainer
    }
    val pctText = formatDailyPct(pct)
    val subText = formatDailySubtext(daily)

    Column(
        modifier = modifier
            .cornerRadius(16.dp)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TODAY",
            maxLines = 1,
            style = TextStyle(
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = pctText,
            maxLines = 1,
            style = TextStyle(
                color = contentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = subText,
            maxLines = 1,
            style = TextStyle(
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun DailyAttendanceCompactBar(
    daily: DailyAttendanceSnapshot,
    threshold: Int
) {
    val pct = daily.percentage
    val bgColor = when {
        pct == null -> GlanceTheme.colors.secondaryContainer
        pct >= threshold -> GlanceTheme.colors.primaryContainer
        else -> GlanceTheme.colors.errorContainer
    }
    val contentColor = when {
        pct == null -> GlanceTheme.colors.onSecondaryContainer
        pct >= threshold -> GlanceTheme.colors.onPrimaryContainer
        else -> GlanceTheme.colors.onErrorContainer
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(12.dp)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TODAY",
            style = TextStyle(
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        Text(
            text = "${formatDailyPct(pct)} (${formatDailySubtext(daily)})",
            maxLines = 1,
            style = TextStyle(
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun formatDailyPct(pct: Double?): String {
    if (pct == null) return "--"
    val rounded = kotlin.math.round(pct * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) "${rounded.toInt()}%" else "%.1f%%".format(rounded)
}

private fun formatDailySubtext(daily: DailyAttendanceSnapshot): String = when {
    daily.totalCounted > 0 -> "${daily.presentCount}/${daily.totalCounted} present"
    daily.cancelledCount > 0 -> "${daily.cancelledCount} cancelled"
    daily.scheduledTodayCount > 0 -> "${daily.scheduledTodayCount} scheduled"
    else -> "No classes"
}

class RefreshNextClassAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        NextClassAttendanceWidget().update(context, glanceId)
    }
}

class NextClassAttendanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassAttendanceWidget()
}

object WidgetUpdater {
    suspend fun refreshAll(context: Context) {
        runCatching { TodaysClassesWidget().updateAll(context) }
        runCatching { AttendanceGpaWidget().updateAll(context) }
        runCatching { NextClassAttendanceWidget().updateAll(context) }
    }
}
