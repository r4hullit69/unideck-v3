package com.unideck.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.domain.GradeLetter
import com.unideck.app.ui.UniDeckViewModel
import com.unideck.app.ui.components.CircularAttendanceRing
import com.unideck.app.ui.components.Cookie9SidedShape
import com.unideck.app.ui.components.ExpressiveGroupedCard
import com.unideck.app.ui.components.GradeDistributionBar
import com.unideck.app.ui.components.HeroTintCard
import com.unideck.app.ui.components.ScallopedGradeBadge
import com.unideck.app.ui.components.SgpaCgpaTrendChart
import com.unideck.app.ui.components.rememberUniDeckHaptics
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue

@Composable
fun GradesScreen(
    semesters: List<SemesterEntity>,
    activeSemester: SemesterEntity?,
    activeCourses: List<CourseEntity>,
    allCourses: List<CourseEntity>,
    preferences: UserPreferences,
    whatIfGrades: Map<Long, GradeLetter>,
    onSetWhatIfGrade: (Long, GradeLetter?) -> Unit,
    onResetWhatIf: () -> Unit,
    onSwitchSemester: (Long) -> Unit,
    onOpenAddCourse: () -> Unit,
    allLogs: List<AttendanceLogEntity> = emptyList(),
    isReadOnly: Boolean = false,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ext = LocalUniDeckColors.current
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = rememberUniDeckHaptics()
    val activeSemId = activeSemester?.id ?: -1L

    val trajectory = remember(semesters, allCourses, preferences.gradeCutoffs, activeSemId, whatIfGrades) {
        AcademicCalculator.computeSemesterTrajectory(
            semesters = semesters,
            allCourses = allCourses,
            cutoffs = preferences.gradeCutoffs,
            activeSemesterId = activeSemId,
            whatIfOverrides = whatIfGrades
        )
    }

    val coursesBySemester = remember(allCourses) {
        allCourses.groupBy { it.semesterId }
    }

    val semesterAttendanceMap = remember(semesters, coursesBySemester, allLogs, preferences.minAttendanceThreshold) {
        semesters.associate { sem ->
            val semCourses = coursesBySemester[sem.id].orEmpty()
            sem.id to AcademicCalculator.computeSemesterAttendanceStats(
                courses = semCourses,
                allLogs = allLogs,
                thresholdPercent = preferences.minAttendanceThreshold
            )
        }
    }

    val currentSgpa = remember(activeCourses, preferences.gradeCutoffs, whatIfGrades) {
        AcademicCalculator.computeSgpa(activeCourses, preferences.gradeCutoffs, whatIfGrades)
    }
    val baseSgpa = remember(activeCourses, preferences.gradeCutoffs) {
        AcademicCalculator.computeSgpa(activeCourses, preferences.gradeCutoffs)
    }
    val cumulativeCgpa = remember(trajectory) {
        trajectory.lastOrNull()?.cumulativeCgpa ?: 0.0
    }
    val equivalentPct = cumulativeCgpa * 10.0

    val gradeCounts = remember(activeCourses, preferences.gradeCutoffs, whatIfGrades) {
        activeCourses.groupingBy { course ->
            whatIfGrades[course.id]
                ?: AcademicCalculator.summarizeCourseScore(course, preferences.gradeCutoffs).resolvedGrade
        }.eachCount()
    }

    val highCreditCourses = remember(activeCourses) {
        activeCourses.filter { it.credits >= 4.0 }.sortedByDescending { it.credits }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (onBack != null) {
            item(key = "grades_nav_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("grades_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Grades & SGPA Calculator",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // 0. Top Semester Tab Strip (Sem 1, Sem 2...) + Share Summary Button
        item(key = "grades_semester_tab_strip") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(semesters, key = { it.id }) { sem ->
                        val selected = sem.id == activeSemId
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptics.tick()
                                    onSwitchSemester(sem.id)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("grades_sem_tab_${sem.id}")
                        ) {
                            Text(
                                text = sem.name,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                ),
                                color = if (selected) cs.primary else cs.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) cs.primary else cs.surface.copy(alpha = 0f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalIconButton(
                    onClick = {
                        haptics.confirm()
                        val semName = activeSemester?.name ?: "Semester"
                        val activeAtt = semesterAttendanceMap[activeSemId]
                        val attLine = if (activeAtt != null && activeAtt.totalCounted > 0) {
                            "${"%.1f".format(activeAtt.percentage)}% (${activeAtt.present}/${activeAtt.totalCounted})"
                        } else {
                            "--"
                        }
                        val summaryText = buildString {
                            appendLine("UniDeck • $semName Academic Summary")
                            appendLine("SGPA: ${if (activeCourses.isEmpty()) "--" else "%.2f".format(currentSgpa)} | CGPA: ${if (allCourses.isEmpty()) "--" else "%.2f".format(cumulativeCgpa)}")
                            appendLine("Attendance: $attLine")
                            if (activeCourses.isNotEmpty()) {
                                appendLine()
                                activeCourses.forEach { c ->
                                    val g = whatIfGrades[c.id]
                                        ?: AcademicCalculator.summarizeCourseScore(c, preferences.gradeCutoffs).resolvedGrade
                                    appendLine("• ${c.code} (${AcademicCalculator.formatCredits(c.credits)} cr): ${g.label}")
                                }
                            }
                        }.trim()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "UniDeck $semName Summary")
                            putExtra(Intent.EXTRA_TEXT, summaryText)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(sendIntent, "Share Summary"))
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("grades_share_summary_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share Academic Summary",
                        tint = cs.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 1. Hero 3-Column Stats Card (SGPA / CGPA / Equivalent %)
        item(key = "grades_hero") {
            HeroTintCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (whatIfGrades.isNotEmpty()) "WHAT-IF PREVIEW ACTIVE" else "ACADEMIC STANDING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (whatIfGrades.isNotEmpty()) {
                        val delta = currentSgpa - baseSgpa
                        val sign = if (delta >= 0) "+" else ""
                        Text(
                            text = "$sign${"%.2f".format(delta)} SGPA",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = if (delta >= 0) ext.safe else ext.danger
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GpaHeroStat(
                        label = "SGPA",
                        value = if (activeCourses.isEmpty()) "--" else "%.2f".format(currentSgpa),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    GpaHeroStat(
                        label = "CGPA",
                        value = if (allCourses.isEmpty()) "--" else "%.2f".format(cumulativeCgpa),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Grade Distribution Bar + High-Credit Impact Indicator
        if (activeCourses.isNotEmpty()) {
            item(key = "grade_dist_card") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Grade Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        GradeDistributionBar(gradeCounts = gradeCounts)

                        if (highCreditCourses.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bolt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "High-Credit Impact (${highCreditCourses.size})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = highCreditCourses.joinToString(", ") {
                                                "${it.code} (${AcademicCalculator.formatCredits(it.credits)} cr)"
                                            } + " carry the highest weight on this semester's SGPA.",
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

        // 3. Compact Semester History Card with Circular Attendance Progress Ring
        if (trajectory.isNotEmpty()) {
            item(key = "semester_history_card") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ext.cardSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("semester_history_card")
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
                            Text(
                                text = "Semester History",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ATT % • SGPA / CGPA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        trajectory.forEach { point ->
                            val isCurrent = point.semester.id == activeSemId
                            val isArchived = UniDeckViewModel.isSemesterArchived(
                                point.semester,
                                preferences.completedSemesterIds
                            )
                            val semAtt = semesterAttendanceMap[point.semester.id]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        haptics.tick()
                                        onSwitchSemester(point.semester.id)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                                    .testTag("semester_history_row_${point.semester.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = point.semester.name,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCurrent) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        } else if (isArchived) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                                            ) {
                                                Text(
                                                    text = "Archived",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    val attSummaryText = if (semAtt != null && semAtt.totalCounted > 0) {
                                        "${AcademicCalculator.formatCredits(point.credits)} cr • ${semAtt.present}/${semAtt.totalCounted} classes"
                                    } else {
                                        "${AcademicCalculator.formatCredits(point.credits)} cr"
                                    }
                                    Text(
                                        text = attSummaryText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Compact Circular Attendance Progress Bar for quick visual reference
                                    CircularAttendanceRing(
                                        percentage = semAtt?.percentage ?: 0.0,
                                        health = semAtt?.health ?: com.unideck.app.domain.AttendanceHealth.NONE,
                                        hasData = (semAtt?.totalCounted ?: 0) > 0,
                                        size = 42.dp,
                                        strokeWidth = 4.2.dp,
                                        modifier = Modifier.testTag("semester_history_attendance_ring_${point.semester.id}")
                                    )

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (point.courseCount == 0) "--" else "%.2f".format(point.sgpa),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFeatureSettings = TABULAR_NUMS
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "CGPA ${"%.2f".format(point.cumulativeCgpa)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_NUMS),
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

        // 4. What-If Simulator
        item(key = "what_if_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "What-If Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap organic grade badges to simulate SGPA/CGPA live",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (whatIfGrades.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            haptics.confirm()
                            onResetWhatIf()
                        },
                        modifier = Modifier.testTag("reset_what_if_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            }
        }

        if (activeCourses.isEmpty()) {
            item(key = "grades_empty") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add courses to calculate SGPA & CGPA",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "UniDeck uses your customizable 10-point scale from Settings and computes credit-weighted SGPA automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenAddCourse,
                            enabled = !isReadOnly
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Course")
                        }
                    }
                }
            }
        } else {
            item(key = "what_if_list") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activeCourses.forEachIndexed { idx, course ->
                        val actualGrade = AcademicCalculator.summarizeCourseScore(course, preferences.gradeCutoffs).resolvedGrade
                        val activeGrade = whatIfGrades[course.id] ?: actualGrade
                        val isSimulated = whatIfGrades.containsKey(course.id)

                        ExpressiveGroupedCard(index = idx, count = activeCourses.size) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
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
                                        val formattedCr = AcademicCalculator.formatCredits(course.credits)
                                        val crText = when {
                                            course.credits == 0.0 -> "0 credits • Audit"
                                            course.credits == 1.0 -> "1 credit"
                                            else -> "$formattedCr credits"
                                        }
                                        Text(
                                            text = crText,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                            color = if (course.credits >= 4.0) ext.margin else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSimulated) {
                                        Text(
                                            text = "Sim",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = cs.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    ScallopedGradeBadge(
                                        gradeLabel = activeGrade.label,
                                        size = 38.dp,
                                        useTertiary = isSimulated
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                GradeLetter.selectablePills.forEach { pill ->
                                    val selected = activeGrade == pill
                                    val pillRadius by animateDpAsState(
                                        targetValue = if (selected) 20.dp else 12.dp,
                                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                                        label = "whatIfPillRadius"
                                    )
                                    val pillShape = RoundedCornerShape(pillRadius)
                                    Surface(
                                        shape = pillShape,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 46.dp)
                                            .clip(pillShape)
                                            .clickable {
                                                haptics.tick()
                                                if (pill == actualGrade && !isSimulated) {
                                                    onSetWhatIfGrade(course.id, null)
                                                } else if (whatIfGrades[course.id] == pill) {
                                                    onSetWhatIfGrade(course.id, null)
                                                } else {
                                                    onSetWhatIfGrade(course.id, pill)
                                                }
                                            }
                                            .testTag("whatif_${course.id}_${pill.name}")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = pill.label,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                                    fontFeatureSettings = TABULAR_NUMS
                                                ),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                textAlign = TextAlign.Center,
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
    }
}

@Composable
private fun GpaHeroStat(
    label: String,
    value: String,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TABULAR_NUMS),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
