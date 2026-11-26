package com.unideck.app.domain

import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.AttendanceStatus
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.OneOffClassEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.local.isDateWithinSemester
import com.unideck.app.data.prefs.GradeCutoffs
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

enum class AttendanceHealth {
    SAFE, MARGIN, DANGER, NONE
}

enum class GradeLetter(val label: String, val points: Int) {
    A_PLUS("A+", 10),
    A("A", 9),
    B("B", 8),
    C("C", 7),
    D("D", 6),
    E("E", 5),
    F("F", 0);

    companion object {
        val selectablePills = listOf(A_PLUS, A, B, C, D, E)

        fun fromLabel(label: String?): GradeLetter? {
            val trimmed = label?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            return when (trimmed.uppercase()) {
                "O" -> A_PLUS
                "B+", "B_PLUS" -> C
                else -> entries.firstOrNull {
                    it.label.equals(trimmed, ignoreCase = true) ||
                        it.name.equals(trimmed, ignoreCase = true)
                }
            }
        }
    }
}

data class AttendanceStats(
    val present: Int,
    val absent: Int,
    val cancelled: Int,
    val totalCounted: Int,
    val percentage: Double,
    val health: AttendanceHealth,
    val safeBunks: Int,
    val classesNeeded: Int,
    val statusHint: String
)

data class ComponentBreakdown(
    val componentType: String,
    val timetablePresent: Int,
    val timetableTotal: Int,
    val manualPresent: Int,
    val manualTotal: Int,
    val slotWeight: Int
) {
    val totalPresent: Int get() = timetablePresent + manualPresent
    val totalCounted: Int get() = timetableTotal + manualTotal
    val percentage: Double get() = if (totalCounted == 0) 0.0 else (totalPresent * 100.0) / totalCounted
}

data class TrendPoint(
    val epochDay: Long,
    val date: LocalDate,
    val cumulativePercentage: Double,
    val presentCount: Int,
    val totalCount: Int
)

data class WeekdayAttendanceStat(
    val dayOfWeek: DayOfWeek,
    val shortLabel: String,
    val present: Int,
    val total: Int,
    val percentage: Double
)

data class CourseScoreSummary(
    val course: CourseEntity,
    val internalEarned: Float,
    val internalMax: Float,
    val endSemEarned: Float,
    val endSemMax: Float,
    val totalPercentage: Float,
    val resolvedGrade: GradeLetter,
    val endSemNeededForAPlus: String,
    val targetMatrix: Map<GradeLetter, String>
)

data class SemesterGpaPoint(
    val semester: SemesterEntity,
    val credits: Double,
    val courseCount: Int = 0,
    val sgpa: Double,
    val cumulativeCgpa: Double
)

data class NextClassInfo(
    val courseCode: String,
    val courseName: String,
    val professorName: String,
    val componentType: String,
    val startTime: Int,
    val endTime: Int,
    val roomNumber: String,
    val dayOffset: Int,
    val dayLabel: String,
    val isOngoing: Boolean
)

data class DailyAttendanceSnapshot(
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val totalCounted: Int,
    val percentage: Double?,
    val scheduledTodayCount: Int
)

object AcademicCalculator {

    private data class CandidateSlot(
        val course: CourseEntity,
        val componentType: String,
        val startTime: Int,
        val endTime: Int,
        val roomNumber: String
    )

    fun computeNextClass(
        courses: List<CourseEntity>,
        schedules: List<ClassScheduleEntity>,
        oneOffClasses: List<OneOffClassEntity>,
        today: LocalDate = LocalDate.now(),
        nowMinutes: Int = LocalTime.now().let { it.hour * 60 + it.minute },
        startDateEpochDay: Long? = null,
        endDateEpochDay: Long? = null
    ): NextClassInfo? {
        val courseMap = courses.associateBy { it.id }
        if (courseMap.isEmpty()) return null

        for (dayOffset in 0..6) {
            val targetDate = today.plusDays(dayOffset.toLong())
            val targetDow = targetDate.dayOfWeek.value
            val targetEpoch = targetDate.toEpochDay()
            val withinSemester = (startDateEpochDay == null || targetEpoch >= startDateEpochDay) &&
                (endDateEpochDay == null || targetEpoch <= endDateEpochDay)

            val recurring = if (withinSemester) {
                schedules
                    .filter { it.dayOfWeek == targetDow }
                    .mapNotNull { slot ->
                        val course = courseMap[slot.courseId] ?: return@mapNotNull null
                        CandidateSlot(
                            course = course,
                            componentType = slot.componentType,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            roomNumber = slot.roomNumber
                        )
                    }
            } else {
                emptyList()
            }

            val oneOffs = oneOffClasses
                .filter { it.dateEpochDay == targetEpoch }
                .mapNotNull { extra ->
                    val course = courseMap[extra.courseId] ?: return@mapNotNull null
                    CandidateSlot(
                        course = course,
                        componentType = extra.componentType,
                        startTime = extra.startTime,
                        endTime = extra.endTime,
                        roomNumber = extra.roomNumber
                    )
                }

            val candidates = (recurring + oneOffs)
                .filter { dayOffset > 0 || it.endTime > nowMinutes }
                .sortedWith(compareBy<CandidateSlot> { it.startTime }.thenBy { it.endTime })

            val next = candidates.firstOrNull()
            if (next != null) {
                val label = when (dayOffset) {
                    0 -> "Today"
                    1 -> "Tomorrow"
                    else -> targetDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                }
                return NextClassInfo(
                    courseCode = next.course.code,
                    courseName = next.course.name,
                    professorName = next.course.professorName,
                    componentType = next.componentType,
                    startTime = next.startTime,
                    endTime = next.endTime,
                    roomNumber = next.roomNumber,
                    dayOffset = dayOffset,
                    dayLabel = label,
                    isOngoing = dayOffset == 0 && next.startTime <= nowMinutes
                )
            }
        }
        return null
    }

    fun computeDailyAttendance(
        courses: List<CourseEntity>,
        schedules: List<ClassScheduleEntity>,
        oneOffClasses: List<OneOffClassEntity>,
        logs: List<AttendanceLogEntity>,
        today: LocalDate = LocalDate.now(),
        startDateEpochDay: Long? = null,
        endDateEpochDay: Long? = null
    ): DailyAttendanceSnapshot {
        val todayEpoch = today.toEpochDay()
        val courseIds = courses.map { it.id }.toSet()
        val todayLogs = logs.filter { it.dateEpochDay == todayEpoch && it.courseId in courseIds }
        val present = todayLogs.count { it.status == AttendanceStatus.PRESENT }
        val absent = todayLogs.count { it.status == AttendanceStatus.ABSENT }
        val cancelled = todayLogs.count { it.status == AttendanceStatus.CANCELLED }
        val total = present + absent
        val pct = if (total == 0) null else (present * 100.0) / total
        val withinSemester = (startDateEpochDay == null || todayEpoch >= startDateEpochDay) &&
            (endDateEpochDay == null || todayEpoch <= endDateEpochDay)
        val recurringToday = if (withinSemester) {
            schedules.count { it.dayOfWeek == today.dayOfWeek.value && it.courseId in courseIds }
        } else 0
        val scheduledToday = recurringToday +
            oneOffClasses.count { it.dateEpochDay == todayEpoch && it.courseId in courseIds }

        return DailyAttendanceSnapshot(
            presentCount = present,
            absentCount = absent,
            cancelledCount = cancelled,
            totalCounted = total,
            percentage = pct,
            scheduledTodayCount = scheduledToday
        )
    }

    /**
     * Equal-weight attendance math (confirmed): every logged PRESENT or ABSENT session counts as 1.
     * CANCELLED classes are excluded from the denominator.
     * Optional mid-semester starting offsets (initialAttended / initialTotal) are folded seamlessly into totals.
     */
    fun computeAttendanceStats(
        logs: List<AttendanceLogEntity>,
        thresholdPercent: Int,
        initialAttended: Int = 0,
        initialTotal: Int = 0
    ): AttendanceStats {
        val safeInitTotal = initialTotal.coerceAtLeast(0)
        val safeInitAttended = initialAttended.coerceIn(0, safeInitTotal)
        val safeInitAbsent = safeInitTotal - safeInitAttended

        val present = safeInitAttended + logs.count { it.status == AttendanceStatus.PRESENT }
        val absent = safeInitAbsent + logs.count { it.status == AttendanceStatus.ABSENT }
        val cancelled = logs.count { it.status == AttendanceStatus.CANCELLED }
        val total = present + absent
        if (total == 0) {
            return AttendanceStats(
                present = 0,
                absent = 0,
                cancelled = cancelled,
                totalCounted = 0,
                percentage = 0.0,
                health = AttendanceHealth.NONE,
                safeBunks = 0,
                classesNeeded = 0,
                statusHint = "No classes logged yet"
            )
        }
        val pct = (present * 100.0) / total
        val target = thresholdPercent / 100.0

        val health = when {
            pct < thresholdPercent -> AttendanceHealth.DANGER
            pct < thresholdPercent + 5.0 -> AttendanceHealth.MARGIN
            else -> AttendanceHealth.SAFE
        }

        val safeBunks = if (pct >= thresholdPercent && target > 0.0) {
            floor((present / target) - total).toInt().coerceAtLeast(0)
        } else 0

        val classesNeeded = if (pct < thresholdPercent && target < 1.0) {
            ceil((target * total - present) / (1.0 - target)).toInt().coerceAtLeast(1)
        } else 0

        val hint = when {
            pct < thresholdPercent -> "Attend next $classesNeeded ${if (classesNeeded == 1) "class" else "classes"} for $thresholdPercent%"
            safeBunks == 0 -> "On the edge — don't miss the next class"
            else -> "Can safely miss $safeBunks ${if (safeBunks == 1) "class" else "classes"}"
        }

        return AttendanceStats(
            present = present,
            absent = absent,
            cancelled = cancelled,
            totalCounted = total,
            percentage = pct,
            health = health,
            safeBunks = safeBunks,
            classesNeeded = classesNeeded,
            statusHint = hint
        )
    }

    /**
     * Computes aggregated equal-weight attendance across all courses belonging to a single semester,
     * folding in each course's starting mid-semester attendance offsets.
     */
    fun computeSemesterAttendanceStats(
        courses: List<CourseEntity>,
        allLogs: List<AttendanceLogEntity>,
        thresholdPercent: Int
    ): AttendanceStats {
        if (courses.isEmpty()) {
            return computeAttendanceStats(emptyList(), thresholdPercent)
        }
        val courseIds = courses.map { it.id }.toSet()
        val semesterLogs = allLogs.filter { it.courseId in courseIds }
        val totalInitAttended = courses.sumOf { it.initialAttended.coerceAtLeast(0) }
        val totalInitClasses = courses.sumOf { it.initialTotal.coerceAtLeast(0) }
        return computeAttendanceStats(
            logs = semesterLogs,
            thresholdPercent = thresholdPercent,
            initialAttended = totalInitAttended,
            initialTotal = totalInitClasses
        )
    }

    fun computeMonthlyPercentage(
        logs: List<AttendanceLogEntity>,
        month: YearMonth = YearMonth.now()
    ): Double? {
        val monthLogs = logs.filter {
            val d = LocalDate.ofEpochDay(it.dateEpochDay)
            d.year == month.year && d.month == month.month && it.status != AttendanceStatus.CANCELLED
        }
        if (monthLogs.isEmpty()) return null
        val present = monthLogs.count { it.status == AttendanceStatus.PRESENT }
        return (present * 100.0) / monthLogs.size
    }

    fun computeTrendPoints(
        logs: List<AttendanceLogEntity>,
        initialAttended: Int = 0,
        initialTotal: Int = 0
    ): List<TrendPoint> {
        val safeInitTotal = initialTotal.coerceAtLeast(0)
        val safeInitAttended = initialAttended.coerceIn(0, safeInitTotal)
        val counted = logs.filter { it.status != AttendanceStatus.CANCELLED }
            .sortedWith(compareBy<AttendanceLogEntity> { it.dateEpochDay }.thenBy { it.id })
        if (counted.isEmpty()) return emptyList()

        val groupedByDay = counted.groupBy { it.dateEpochDay }.toSortedMap()
        var runningPresent = safeInitAttended
        var runningTotal = safeInitTotal
        return groupedByDay.map { (epochDay, dayLogs) ->
            runningPresent += dayLogs.count { it.status == AttendanceStatus.PRESENT }
            runningTotal += dayLogs.size
            TrendPoint(
                epochDay = epochDay,
                date = LocalDate.ofEpochDay(epochDay),
                cumulativePercentage = (runningPresent * 100.0) / runningTotal,
                presentCount = runningPresent,
                totalCount = runningTotal
            )
        }
    }

    fun computeWeekdayBreakdown(logs: List<AttendanceLogEntity>): List<WeekdayAttendanceStat> {
        val counted = logs.filter { it.status != AttendanceStatus.CANCELLED }
        val days = listOf(
            DayOfWeek.MONDAY to "Mon",
            DayOfWeek.TUESDAY to "Tue",
            DayOfWeek.WEDNESDAY to "Wed",
            DayOfWeek.THURSDAY to "Thu",
            DayOfWeek.FRIDAY to "Fri",
            DayOfWeek.SATURDAY to "Sat"
        )
        return days.map { (day, label) ->
            val dayLogs = counted.filter { LocalDate.ofEpochDay(it.dateEpochDay).dayOfWeek == day }
            val present = dayLogs.count { it.status == AttendanceStatus.PRESENT }
            val total = dayLogs.size
            WeekdayAttendanceStat(
                dayOfWeek = day,
                shortLabel = label,
                present = present,
                total = total,
                percentage = if (total == 0) 0.0 else (present * 100.0) / total
            )
        }
    }

    fun computeCurrentStreak(logs: List<AttendanceLogEntity>): Int {
        val counted = logs.filter { it.status != AttendanceStatus.CANCELLED }
            .sortedWith(compareByDescending<AttendanceLogEntity> { it.dateEpochDay }.thenByDescending { it.id })
        var streak = 0
        for (log in counted) {
            if (log.status == AttendanceStatus.PRESENT) streak++ else break
        }
        return streak
    }

    fun projectAttendance(present: Int, total: Int, deltaClasses: Int): Double {
        if (deltaClasses == 0) return if (total == 0) 0.0 else (present * 100.0) / total
        val newTotal = total + kotlin.math.abs(deltaClasses)
        val newPresent = if (deltaClasses > 0) present + deltaClasses else present
        return if (newTotal == 0) 0.0 else (newPresent * 100.0) / newTotal
    }

    fun computeComponentBreakdowns(
        logs: List<AttendanceLogEntity>,
        schedules: List<ClassScheduleEntity>
    ): List<ComponentBreakdown> {
        return ComponentType.all.map { type ->
            val typeLogs = logs.filter { it.componentType == type && it.status != AttendanceStatus.CANCELLED }
            val ttLogs = typeLogs.filter { !it.isManualEntry }
            val manLogs = typeLogs.filter { it.isManualEntry }
            val weight = schedules.firstOrNull { it.componentType == type }?.weight ?: 1
            ComponentBreakdown(
                componentType = type,
                timetablePresent = ttLogs.count { it.status == AttendanceStatus.PRESENT },
                timetableTotal = ttLogs.size,
                manualPresent = manLogs.count { it.status == AttendanceStatus.PRESENT },
                manualTotal = manLogs.size,
                slotWeight = weight
            )
        }
    }

    // --- Scores & 10-Point Grade Engine ---
    fun gradeForScore(totalScore: Float, cutoffs: GradeCutoffs): GradeLetter = when {
        totalScore >= cutoffs.aPlusMin -> GradeLetter.A_PLUS
        totalScore >= cutoffs.aMin -> GradeLetter.A
        totalScore >= cutoffs.bMin -> GradeLetter.B
        totalScore >= cutoffs.cMin -> GradeLetter.C
        totalScore >= cutoffs.dMin -> GradeLetter.D
        totalScore >= cutoffs.eMin -> GradeLetter.E
        else -> GradeLetter.F
    }

    fun cutoffForGrade(grade: GradeLetter, cutoffs: GradeCutoffs): Int = when (grade) {
        GradeLetter.A_PLUS -> cutoffs.aPlusMin
        GradeLetter.A -> cutoffs.aMin
        GradeLetter.B -> cutoffs.bMin
        GradeLetter.C -> cutoffs.cMin
        GradeLetter.D -> cutoffs.dMin
        GradeLetter.E -> cutoffs.eMin
        GradeLetter.F -> 0
    }

    fun summarizeCourseScore(
        course: CourseEntity,
        cutoffs: GradeCutoffs
    ): CourseScoreSummary {
        val internalMax = if (course.isLab) 60f else 50f
        val endSemMax = if (course.isLab) 40f else 50f
        val internalEarned = if (course.isLab) {
            course.midSem.coerceIn(0f, 60f)
        } else {
            (course.midSem.coerceIn(0f, 25f) +
                course.quiz1.coerceIn(0f, 10f) +
                course.quiz2.coerceIn(0f, 10f) +
                course.assignment.coerceIn(0f, 5f)).coerceIn(0f, 50f)
        }
        val endSemEarned = course.endSem.coerceIn(0f, endSemMax)
        val total = (internalEarned + endSemEarned).coerceIn(0f, 100f)

        val resolvedGrade = if (course.relativeGradingEnabled && !course.customGradeOverride.isNullOrBlank()) {
            GradeLetter.fromLabel(course.customGradeOverride) ?: gradeForScore(total, cutoffs)
        } else {
            gradeForScore(total, cutoffs)
        }

        val targetGrades = listOf(GradeLetter.A_PLUS, GradeLetter.A, GradeLetter.B, GradeLetter.C, GradeLetter.D)
        val matrix = targetGrades.associateWith { grade ->
            val cutoff = cutoffForGrade(grade, cutoffs).toFloat()
            val needed = cutoff - internalEarned
            when {
                needed <= 0f -> "Secured"
                needed > endSemMax -> "Maxed"
                else -> "${formatMark(needed)}/${endSemMax.toInt()}"
            }
        }

        val aPlusNeededVal = cutoffForGrade(GradeLetter.A_PLUS, cutoffs) - internalEarned
        val aPlusHelper = when {
            course.relativeGradingEnabled -> "Relative grading active (${resolvedGrade.label})"
            aPlusNeededVal <= 0f -> "A+ already secured from internals!"
            aPlusNeededVal > endSemMax -> {
                val aNeededVal = cutoffForGrade(GradeLetter.A, cutoffs) - internalEarned
                if (aNeededVal <= endSemMax) {
                    "Need ${formatMark(aNeededVal.coerceAtLeast(0f))}/${endSemMax.toInt()} in End-Sem for A"
                } else {
                    "A+ unreachable (max possible: ${formatMark(internalEarned + endSemMax)}%)"
                }
            }
            else -> "Need ${formatMark(aPlusNeededVal)}/${endSemMax.toInt()} in End-Sem for A+"
        }

        return CourseScoreSummary(
            course = course,
            internalEarned = internalEarned,
            internalMax = internalMax,
            endSemEarned = endSemEarned,
            endSemMax = endSemMax,
            totalPercentage = total,
            resolvedGrade = resolvedGrade,
            endSemNeededForAPlus = aPlusHelper,
            targetMatrix = matrix
        )
    }

    fun computeSgpa(
        courses: List<CourseEntity>,
        cutoffs: GradeCutoffs,
        whatIfOverrides: Map<Long, GradeLetter> = emptyMap()
    ): Double {
        val totalCredits = courses.sumOf { it.credits.coerceAtLeast(0.0) }
        if (totalCredits <= 0.0) return 0.0
        val weightedSum = courses.sumOf { course ->
            val safeCredits = course.credits.coerceAtLeast(0.0)
            val grade = whatIfOverrides[course.id] ?: summarizeCourseScore(course, cutoffs).resolvedGrade
            grade.points.toDouble() * safeCredits
        }
        return weightedSum / totalCredits
    }

    fun computeSemesterTrajectory(
        semesters: List<SemesterEntity>,
        allCourses: List<CourseEntity>,
        cutoffs: GradeCutoffs,
        activeSemesterId: Long,
        whatIfOverrides: Map<Long, GradeLetter> = emptyMap()
    ): List<SemesterGpaPoint> {
        var cumulativeWeighted = 0.0
        var cumulativeCredits = 0.0
        return semesters.map { semester ->
            val semCourses = allCourses.filter { it.semesterId == semester.id }
            val overrides = if (semester.id == activeSemesterId) whatIfOverrides else emptyMap()
            val semCredits = semCourses.sumOf { it.credits.coerceAtLeast(0.0) }
            val semWeighted = semCourses.sumOf { course ->
                val safeCredits = course.credits.coerceAtLeast(0.0)
                val grade = overrides[course.id] ?: summarizeCourseScore(course, cutoffs).resolvedGrade
                grade.points.toDouble() * safeCredits
            }
            val sgpa = if (semCredits <= 0.0) 0.0 else semWeighted / semCredits
            cumulativeWeighted += semWeighted
            cumulativeCredits += semCredits
            val cgpa = if (cumulativeCredits <= 0.0) 0.0 else cumulativeWeighted / cumulativeCredits
            SemesterGpaPoint(
                semester = semester,
                credits = semCredits,
                courseCount = semCourses.size,
                sgpa = sgpa,
                cumulativeCgpa = cgpa
            )
        }
    }

    fun formatCredits(credits: Double): String {
        val safe = credits.coerceAtLeast(0.0)
        val rounded = kotlin.math.round(safe * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString()
        }
    }

    fun formatMark(value: Float): String {
        val rounded = kotlin.math.round(value * 10f) / 10f
        return if (rounded % 1f == 0f) rounded.toInt().toString() else "%.1f".format(rounded)
    }

    fun formatMinutes(minutesFromMidnight: Int, use24Hour: Boolean): String {
        val safe = minutesFromMidnight.coerceIn(0, 1439)
        val hour24 = safe / 60
        val minute = safe % 60
        return if (use24Hour) {
            "%02d:%02d".format(hour24, minute)
        } else {
            val amPm = if (hour24 < 12) "AM" else "PM"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            "%d:%02d %s".format(hour12, minute, amPm)
        }
    }
}
