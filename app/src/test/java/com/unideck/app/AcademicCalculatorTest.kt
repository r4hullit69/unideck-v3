package com.unideck.app

import com.unideck.app.backup.BackupManager
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.AttendanceStatus
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.OneOffClassEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.local.formatDateRange
import com.unideck.app.data.local.isDateWithinSemester
import com.unideck.app.data.prefs.GradeCutoffs
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.domain.AttendanceHealth
import com.unideck.app.domain.GradeLetter
import com.unideck.app.ui.UniDeckViewModel
import com.unideck.app.ui.components.filterProfessorSuggestions
import com.unideck.app.ui.theme.UniDeckPalettes
import com.unideck.app.ui.theme.resolveUniDeckPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AcademicCalculatorTest {

    @Test
    fun equalWeightAttendanceExcludesCancelledAndCalculatesBunks() {
        val logs = listOf(
            AttendanceLogEntity(id = 1, courseId = 1, dateEpochDay = 20000, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 2, courseId = 1, dateEpochDay = 20001, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 3, courseId = 1, dateEpochDay = 20002, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 4, courseId = 1, dateEpochDay = 20003, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 5, courseId = 1, dateEpochDay = 20004, status = AttendanceStatus.CANCELLED)
        )
        val stats = AcademicCalculator.computeAttendanceStats(logs, thresholdPercent = 75)
        assertEquals(4, stats.totalCounted)
        assertEquals(100.0, stats.percentage, 0.01)
        assertEquals(AttendanceHealth.SAFE, stats.health)
        assertEquals(1, stats.safeBunks) // 4 / 5 = 80% >= 75%, 4 / 6 = 66.6% < 75%
    }

    @Test
    fun recoveryClassesNeededWhenBelowThreshold() {
        val logs = listOf(
            AttendanceLogEntity(id = 1, courseId = 1, dateEpochDay = 20000, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 2, courseId = 1, dateEpochDay = 20001, status = AttendanceStatus.ABSENT),
            AttendanceLogEntity(id = 3, courseId = 1, dateEpochDay = 20002, status = AttendanceStatus.ABSENT)
        )
        val stats = AcademicCalculator.computeAttendanceStats(logs, thresholdPercent = 75)
        assertEquals(AttendanceHealth.DANGER, stats.health)
        assertEquals(5, stats.classesNeeded) // (1 + 5) / (3 + 5) = 6 / 8 = 75%
    }

    @Test
    fun defaultTenPointGradeScaleAndLegacyFallback() {
        val defaults = GradeCutoffs()
        assertEquals(GradeLetter.A_PLUS, AcademicCalculator.gradeForScore(90f, defaults))
        assertEquals(10, AcademicCalculator.gradeForScore(90f, defaults).points)
        assertEquals(GradeLetter.A, AcademicCalculator.gradeForScore(85f, defaults))
        assertEquals(9, AcademicCalculator.gradeForScore(85f, defaults).points)
        assertEquals(GradeLetter.B, AcademicCalculator.gradeForScore(72f, defaults))
        assertEquals(8, AcademicCalculator.gradeForScore(72f, defaults).points)
        assertEquals(GradeLetter.C, AcademicCalculator.gradeForScore(64f, defaults))
        assertEquals(7, AcademicCalculator.gradeForScore(64f, defaults).points)
        assertEquals(GradeLetter.D, AcademicCalculator.gradeForScore(50f, defaults))
        assertEquals(6, AcademicCalculator.gradeForScore(50f, defaults).points)
        assertEquals(GradeLetter.E, AcademicCalculator.gradeForScore(40f, defaults))
        assertEquals(5, AcademicCalculator.gradeForScore(40f, defaults).points)
        assertEquals(GradeLetter.F, AcademicCalculator.gradeForScore(39.5f, defaults))
        assertEquals(0, AcademicCalculator.gradeForScore(39.5f, defaults).points)

        // Legacy grade label compatibility
        assertEquals(GradeLetter.A_PLUS, GradeLetter.fromLabel("O"))
        assertEquals(GradeLetter.C, GradeLetter.fromLabel("B+"))
        assertEquals(GradeLetter.C, GradeLetter.fromLabel("B_PLUS"))
        assertEquals(GradeLetter.A_PLUS, GradeLetter.fromLabel("A+"))
    }

    @Test
    fun customizableGradeCutoffsAndCreditWeightedSgpa() {
        val strictCutoffs = GradeCutoffs(aPlusMin = 95, aMin = 85, bMin = 75, cMin = 65, dMin = 55, eMin = 45)
        val c1 = CourseEntity(id = 1, semesterId = 1, code = "CS101", name = "Algorithms", credits = 4.0, midSem = 22f, quiz1 = 9f, quiz2 = 9f, assignment = 5f, endSem = 45f) // 90 -> A (9 pts) under strict
        val c2 = CourseEntity(id = 2, semesterId = 1, code = "CS102L", name = "Algo Lab", credits = 2.0, isLab = true, midSem = 58f, endSem = 38f) // 96 -> A+ (10 pts) under strict

        val sgpa = AcademicCalculator.computeSgpa(listOf(c1, c2), strictCutoffs)
        // (4 * 9 + 2 * 10) / 6 = 56 / 6 = 9.3333
        assertEquals(9.333, sgpa, 0.01)

        val whatIf = AcademicCalculator.computeSgpa(listOf(c1, c2), strictCutoffs, mapOf(1L to GradeLetter.A_PLUS))
        assertEquals(10.0, whatIf, 0.001)
    }

    @Test
    fun startingAttendanceOffsetsFoldIntoStatsAndTrend() {
        val logs = listOf(
            AttendanceLogEntity(id = 1, courseId = 1, dateEpochDay = 20010, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 2, courseId = 1, dateEpochDay = 20011, status = AttendanceStatus.ABSENT)
        )
        // Starting mid-semester with 18/20 + 1 present + 1 absent = 19/22 = 86.36%
        val stats = AcademicCalculator.computeAttendanceStats(
            logs = logs,
            thresholdPercent = 75,
            initialAttended = 18,
            initialTotal = 20
        )
        assertEquals(19, stats.present)
        assertEquals(3, stats.absent)
        assertEquals(22, stats.totalCounted)
        assertEquals(86.36, stats.percentage, 0.05)
        assertEquals(AttendanceHealth.SAFE, stats.health)
        assertEquals(3, stats.safeBunks) // floor(19 / 0.75 - 22) = floor(25.33 - 22) = 3

        val trend = AcademicCalculator.computeTrendPoints(
            logs = logs,
            initialAttended = 18,
            initialTotal = 20
        )
        assertEquals(2, trend.size)
        assertEquals(19, trend[0].presentCount)
        assertEquals(21, trend[0].totalCount)
        assertEquals(19, trend[1].presentCount)
        assertEquals(22, trend[1].totalCount)
    }

    @Test
    fun nextClassResolvesOngoingUpcomingAndFutureDays() {
        val monday = LocalDate.of(2026, 11, 23) // Monday = dayOfWeek 1
        val courses = listOf(
            CourseEntity(id = 1, semesterId = 1, code = "CS101", name = "Algorithms"),
            CourseEntity(id = 2, semesterId = 1, code = "MA101", name = "Calculus")
        )
        val schedules = listOf(
            ClassScheduleEntity(
                id = 1, courseId = 1, semesterId = 1,
                dayOfWeek = 1, startTime = 540, endTime = 600, roomNumber = "AB-101", componentType = ComponentType.LECTURE
            ),
            ClassScheduleEntity(
                id = 2, courseId = 2, semesterId = 1,
                dayOfWeek = 2, startTime = 600, endTime = 660, roomNumber = "LH-2", componentType = ComponentType.TUTORIAL
            )
        )
        val oneOffToday = listOf(
            OneOffClassEntity(
                id = 1, courseId = 2, semesterId = 1,
                dateEpochDay = monday.toEpochDay(), startTime = 660, endTime = 720, roomNumber = "Lab 3", componentType = ComponentType.LAB
            )
        )

        // Before 9:00 AM -> 9:00 AM CS101 upcoming today
        val upcoming = AcademicCalculator.computeNextClass(courses, schedules, oneOffToday, monday, nowMinutes = 510)
        assertNotNull(upcoming)
        assertEquals("CS101", upcoming!!.courseCode)
        assertEquals(0, upcoming.dayOffset)
        assertEquals(false, upcoming.isOngoing)

        // At 9:15 AM -> 9:00-10:00 AM CS101 is ongoing
        val ongoing = AcademicCalculator.computeNextClass(courses, schedules, oneOffToday, monday, nowMinutes = 555)
        assertNotNull(ongoing)
        assertEquals("CS101", ongoing!!.courseCode)
        assertTrue(ongoing.isOngoing)

        // At 10:30 AM -> 11:00 AM one-off lab today
        val laterOneOff = AcademicCalculator.computeNextClass(courses, schedules, oneOffToday, monday, nowMinutes = 630)
        assertNotNull(laterOneOff)
        assertEquals("MA101", laterOneOff!!.courseCode)
        assertEquals(ComponentType.LAB, laterOneOff.componentType)
        assertEquals(0, laterOneOff.dayOffset)

        // At 1:00 PM -> rolls over to Tomorrow's MA101 tutorial
        val tomorrow = AcademicCalculator.computeNextClass(courses, schedules, oneOffToday, monday, nowMinutes = 780)
        assertNotNull(tomorrow)
        assertEquals("MA101", tomorrow!!.courseCode)
        assertEquals(1, tomorrow.dayOffset)
        assertEquals("Tomorrow", tomorrow.dayLabel)
    }

    @Test
    fun dailyAttendanceSnapshotCalculatesPercentageAndIgnoresCancelled() {
        val today = LocalDate.of(2026, 11, 23)
        val courses = listOf(
            CourseEntity(id = 1, semesterId = 1, code = "CS101", name = "Algorithms"),
            CourseEntity(id = 2, semesterId = 1, code = "MA101", name = "Calculus"),
            CourseEntity(id = 99, semesterId = 2, code = "OLD100", name = "Other Sem")
        )
        val logs = listOf(
            AttendanceLogEntity(id = 1, courseId = 1, dateEpochDay = today.toEpochDay(), status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 2, courseId = 2, dateEpochDay = today.toEpochDay(), status = AttendanceStatus.ABSENT),
            AttendanceLogEntity(id = 3, courseId = 1, dateEpochDay = today.toEpochDay(), status = AttendanceStatus.CANCELLED),
            // Yesterday & another semester should be excluded
            AttendanceLogEntity(id = 4, courseId = 1, dateEpochDay = today.minusDays(1).toEpochDay(), status = AttendanceStatus.ABSENT),
            AttendanceLogEntity(id = 5, courseId = 99, dateEpochDay = today.toEpochDay(), status = AttendanceStatus.PRESENT)
        )

        val snap = AcademicCalculator.computeDailyAttendance(
            courses = courses.filter { it.semesterId == 1L },
            schedules = emptyList(),
            oneOffClasses = emptyList(),
            logs = logs,
            today = today
        )
        assertEquals(1, snap.presentCount)
        assertEquals(1, snap.absentCount)
        assertEquals(1, snap.cancelledCount)
        assertEquals(2, snap.totalCounted)
        assertEquals(50.0, snap.percentage!!, 0.01)

        val emptySnap = AcademicCalculator.computeDailyAttendance(
            courses = courses.filter { it.semesterId == 1L },
            schedules = emptyList(),
            oneOffClasses = emptyList(),
            logs = emptyList(),
            today = today
        )
        assertNull(emptySnap.percentage)
    }

    @Test
    fun semesterDateWindowHelpersAndRangeFormatting() {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 12, 15)

        val unbounded = SemesterEntity(id = 1, name = "Sem 1")
        assertTrue(unbounded.isDateWithinSemester(start.minusMonths(2).toEpochDay()))
        assertTrue(unbounded.isDateWithinSemester(end.plusMonths(2).toEpochDay()))
        assertEquals("No dates set", unbounded.formatDateRange())

        val bounded = SemesterEntity(
            id = 2,
            name = "Fall 2026",
            startDateEpochDay = start.toEpochDay(),
            endDateEpochDay = end.toEpochDay()
        )
        assertFalse(bounded.isDateWithinSemester(start.minusDays(1).toEpochDay()))
        assertTrue(bounded.isDateWithinSemester(start.toEpochDay()))
        assertTrue(bounded.isDateWithinSemester(LocalDate.of(2026, 10, 10).toEpochDay()))
        assertTrue(bounded.isDateWithinSemester(end.toEpochDay()))
        assertFalse(bounded.isDateWithinSemester(end.plusDays(1).toEpochDay()))
        assertEquals("Aug 1 – Dec 15, 2026", bounded.formatDateRange())
    }

    @Test
    fun nextClassAndDailyAttendanceRespectSemesterBoundsWhileKeepingOneOffs() {
        val monday = LocalDate.of(2026, 11, 23) // Monday = 1
        val semEnd = monday.minusDays(1) // Semester ended yesterday
        val courses = listOf(CourseEntity(id = 1, semesterId = 1, code = "CS101", name = "Algorithms"))
        val schedules = listOf(
            ClassScheduleEntity(
                id = 1, courseId = 1, semesterId = 1,
                dayOfWeek = 1, startTime = 540, endTime = 600, roomNumber = "AB-101"
            )
        )

        // Outside semester with no one-offs -> no recurring next class, scheduledTotal = 0
        val noRecurringNext = AcademicCalculator.computeNextClass(
            courses = courses,
            schedules = schedules,
            oneOffClasses = emptyList(),
            today = monday,
            nowMinutes = 500,
            startDateEpochDay = null,
            endDateEpochDay = semEnd.toEpochDay()
        )
        assertNull(noRecurringNext)

        val snapOutside = AcademicCalculator.computeDailyAttendance(
            courses = courses,
            schedules = schedules,
            oneOffClasses = emptyList(),
            logs = emptyList(),
            today = monday,
            startDateEpochDay = null,
            endDateEpochDay = semEnd.toEpochDay()
        )
        assertEquals(0, snapOutside.scheduledTodayCount)

        // One-off class on a date outside semester still shows up
        val oneOff = listOf(
            OneOffClassEntity(
                id = 10, courseId = 1, semesterId = 1,
                dateEpochDay = monday.toEpochDay(), startTime = 600, endTime = 660, roomNumber = "Hall A"
            )
        )
        val oneOffNext = AcademicCalculator.computeNextClass(
            courses = courses,
            schedules = schedules,
            oneOffClasses = oneOff,
            today = monday,
            nowMinutes = 500,
            startDateEpochDay = null,
            endDateEpochDay = semEnd.toEpochDay()
        )
        assertNotNull(oneOffNext)
        assertEquals("Hall A", oneOffNext!!.roomNumber)

        val snapWithOneOff = AcademicCalculator.computeDailyAttendance(
            courses = courses,
            schedules = schedules,
            oneOffClasses = oneOff,
            logs = emptyList(),
            today = monday,
            startDateEpochDay = null,
            endDateEpochDay = semEnd.toEpochDay()
        )
        assertEquals(1, snapWithOneOff.scheduledTodayCount)
    }

    @Test
    fun decimalAndZeroCreditCoursesComputeSgpaCgpaWithoutDivisionByZero() {
        val cutoffs = GradeCutoffs() // A+ >= 90 (10), A >= 80 (9), B >= 70 (8)
        val sem1 = SemesterEntity(id = 1, name = "Sem 1", isCurrent = false)
        val sem2 = SemesterEntity(id = 2, name = "Sem 2", isCurrent = true)

        val lab15 = CourseEntity(
            id = 1, semesterId = 1, code = "CS101L", name = "Programming Lab",
            credits = 1.5, isLab = true, midSem = 55f, endSem = 38f // 93 -> A+ (10)
        )
        val theory25 = CourseEntity(
            id = 2, semesterId = 1, code = "CS102", name = "Discrete Math",
            credits = 2.5, midSem = 20f, quiz1 = 8f, quiz2 = 8f, assignment = 4f, endSem = 42f // 82 -> A (9)
        )
        val audit0 = CourseEntity(
            id = 3, semesterId = 1, code = "EVS100", name = "Environmental Audit",
            credits = 0.0, midSem = 5f, endSem = 10f // 15 -> F (0), must NOT drag SGPA down
        )
        val onlyAuditSem2 = CourseEntity(
            id = 4, semesterId = 2, code = "NCC100", name = "NCC Mandatory",
            credits = 0.0, midSem = 25f, endSem = 50f // 0-credit only semester
        )

        // (1.5 * 10 + 2.5 * 9 + 0.0 * 0) / (1.5 + 2.5) = (15 + 22.5) / 4.0 = 37.5 / 4.0 = 9.375
        val sgpaSem1 = AcademicCalculator.computeSgpa(listOf(lab15, theory25, audit0), cutoffs)
        assertEquals(9.375, sgpaSem1, 0.0001)

        // All-0-credit semester returns 0.0 safely (no NaN / Infinity)
        val sgpaSem2 = AcademicCalculator.computeSgpa(listOf(onlyAuditSem2), cutoffs)
        assertEquals(0.0, sgpaSem2, 0.0001)
        assertFalse(sgpaSem2.isNaN())

        val trajectory = AcademicCalculator.computeSemesterTrajectory(
            semesters = listOf(sem1, sem2),
            allCourses = listOf(lab15, theory25, audit0, onlyAuditSem2),
            cutoffs = cutoffs,
            activeSemesterId = 2L
        )
        assertEquals(2, trajectory.size)
        assertEquals(4.0, trajectory[0].credits, 0.0001)
        assertEquals(9.375, trajectory[0].sgpa, 0.0001)
        assertEquals(9.375, trajectory[0].cumulativeCgpa, 0.0001)
        assertEquals(0.0, trajectory[1].credits, 0.0001)
        assertEquals(0.0, trajectory[1].sgpa, 0.0001)
        assertEquals(9.375, trajectory[1].cumulativeCgpa, 0.0001)

        assertEquals("0", AcademicCalculator.formatCredits(0.0))
        assertEquals("1.5", AcademicCalculator.formatCredits(1.5))
        assertEquals("3", AcademicCalculator.formatCredits(3.0))
        assertEquals("2.25", AcademicCalculator.formatCredits(2.25))
    }

    @Test
    fun backupManagerDecodesLegacyIntegerCreditsAndEnforcesRolling3DayRetention() {
        val legacyJson = """
            {
              "version": 3,
              "exportedAtEpochMs": 1700000000000,
              "semesters": [
                { "id": 1, "name": "Sem 1", "isCurrent": true }
              ],
              "courses": [
                { "id": 10, "semesterId": 1, "code": "CS101", "name": "DSA", "credits": 4 },
                { "id": 11, "semesterId": 1, "code": "CS102L", "name": "Lab", "credits": 1.5 },
                { "id": 12, "semesterId": 1, "code": "AUD01", "name": "Audit", "credits": 0 }
              ],
              "schedules": [],
              "attendanceLogs": [],
              "preferences": {
                "themeMode": "DARK"
              }
            }
        """.trimIndent()

        val payload = BackupManager.decodeBackupPayload(legacyJson)
        assertEquals(3, payload.courses.size)
        assertEquals(4.0, payload.courses[0].credits, 0.0001)
        assertEquals(1.5, payload.courses[1].credits, 0.0001)
        assertEquals(0.0, payload.courses[2].credits, 0.0001)
        assertNull(payload.semesters[0].startDateEpochDay)
        assertNull(payload.semesters[0].endDateEpochDay)

        val files = listOf(
            "unideck_backup_2026-11-18.json",
            "unideck_backup_2026-11-19.json",
            "unideck_backup_2026-11-20.json",
            "unideck_backup_2026-11-21.json",
            "unideck_backup_2026-11-22.json",
            "my_personal_notes.txt",
            "unideck_backup_manual.json"
        )
        val toDelete = BackupManager.selectFilesToDelete(files, maxKeep = 3)
        assertEquals(
            listOf("unideck_backup_2026-11-19.json", "unideck_backup_2026-11-18.json"),
            toDelete
        )
        assertEquals("unideck_backup_2026-11-23.json", BackupManager.dailyBackupFileName(LocalDate.of(2026, 11, 23)))
    }

    @Test
    fun accentPalettesContainSixteenOneWordSwatchesWithBlueFallback() {
        assertEquals(16, UniDeckPalettes.size)
        val expectedKeys = listOf(
            "blue", "indigo", "sky", "cyan",
            "teal", "emerald", "green", "lime",
            "violet", "purple", "magenta", "pink",
            "rose", "coral", "orange", "amber"
        )
        assertEquals(expectedKeys, UniDeckPalettes.map { it.key })
        UniDeckPalettes.forEach { pal ->
            assertFalse("Accent name '${pal.name}' should be a single word", pal.name.contains(" "))
            assertEquals(pal.key, resolveUniDeckPalette(pal.key).key)
            assertEquals(pal.key, resolveUniDeckPalette(pal.key.uppercase()).key)
        }
        // Unmatched or legacy keys fall back gracefully to Blue
        assertEquals("blue", resolveUniDeckPalette("vibrant_solar").key)
        assertEquals("blue", resolveUniDeckPalette(null).key)
        assertEquals("blue", resolveUniDeckPalette("").key)
    }

    @Test
    fun semesterArchiveDetectionChecksPastEndDateAndManualCompletedFlag() {
        val today = LocalDate.of(2026, 11, 23).toEpochDay()
        val pastSem = SemesterEntity(id = 1, name = "Fall 2025", endDateEpochDay = today - 1)
        val endingTodaySem = SemesterEntity(id = 2, name = "Fall 2026", endDateEpochDay = today)
        val openSem = SemesterEntity(id = 3, name = "Spring 2027", endDateEpochDay = null)

        assertTrue(UniDeckViewModel.isSemesterArchived(pastSem, emptySet(), today))
        assertFalse(UniDeckViewModel.isSemesterArchived(endingTodaySem, emptySet(), today))
        assertFalse(UniDeckViewModel.isSemesterArchived(openSem, emptySet(), today))
        // Manually completed semester is archived regardless of dates
        assertTrue(UniDeckViewModel.isSemesterArchived(openSem, setOf(3L), today))
    }

    @Test
    fun professorAutocompleteDeduplicatesRanksPrefixFirstAndHidesOnExactMatch() {
        val saved = listOf(
            "Dr. Sharma",
            "dr. sharma ",
            "Prof. R. Sharma",
            "Dr. S. Nair",
            "Dr. Anita Verma",
            "   "
        )

        // Blank query returns deduplicated alphabetically sorted list
        val all = filterProfessorSuggestions("", saved)
        assertEquals(listOf("Dr. Anita Verma", "Dr. S. Nair", "Dr. Sharma", "Prof. R. Sharma"), all)

        // Prefix matches ranked before substring matches
        val shMatches = filterProfessorSuggestions("sh", listOf("Prof. R. Sharma", "Sharma", "Dr. Sharma"))
        assertEquals(listOf("Sharma", "Dr. Sharma", "Prof. R. Sharma"), shMatches)

        val drMatches = filterProfessorSuggestions("dr.", saved)
        assertEquals(listOf("Dr. Anita Verma", "Dr. S. Nair", "Dr. Sharma"), drMatches)

        // Exact match (case-insensitive) hides dropdown
        assertTrue(filterProfessorSuggestions("dr. sharma", saved).isEmpty())
    }

    @Test
    fun computeSemesterAttendanceStatsAggregatesAcrossCoursesAndDefaultsMaterialYouDynamicColor() {
        val c1 = CourseEntity(
            id = 1, semesterId = 1, code = "CS101", name = "DSA",
            initialAttended = 8, initialTotal = 10
        )
        val c2 = CourseEntity(
            id = 2, semesterId = 1, code = "CS102", name = "OS",
            initialAttended = 0, initialTotal = 0
        )
        val logs = listOf(
            AttendanceLogEntity(id = 1, courseId = 1, dateEpochDay = 20000, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 2, courseId = 2, dateEpochDay = 20000, status = AttendanceStatus.PRESENT),
            AttendanceLogEntity(id = 3, courseId = 2, dateEpochDay = 20001, status = AttendanceStatus.ABSENT),
            AttendanceLogEntity(id = 4, courseId = 2, dateEpochDay = 20002, status = AttendanceStatus.CANCELLED)
        )
        // c1: 8+1 = 9 / 11; c2: 1 / 2 -> total: 10 / 13 = 76.92%
        val semStats = AcademicCalculator.computeSemesterAttendanceStats(
            courses = listOf(c1, c2),
            allLogs = logs,
            thresholdPercent = 75
        )
        assertEquals(10, semStats.present)
        assertEquals(3, semStats.absent)
        assertEquals(1, semStats.cancelled)
        assertEquals(13, semStats.totalCounted)
        assertEquals(76.92, semStats.percentage, 0.05)

        val defaultPrefs = com.unideck.app.data.prefs.UserPreferences()
        assertTrue(defaultPrefs.dynamicMonetEnabled)
        assertFalse(defaultPrefs.pitchBlackEnabled)
    }
}

