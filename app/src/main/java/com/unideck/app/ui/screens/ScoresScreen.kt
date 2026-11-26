package com.unideck.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.prefs.GradeCutoffs
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.domain.CourseScoreSummary
import com.unideck.app.domain.GradeLetter
import com.unideck.app.ui.components.ExpressiveGroupedCard
import com.unideck.app.ui.components.HeroTintCard
import com.unideck.app.ui.components.LegendDot
import com.unideck.app.ui.components.ScallopedGradeBadge
import com.unideck.app.ui.components.SubjectComparisonBarChart
import com.unideck.app.ui.components.rememberUniDeckHaptics
import com.unideck.app.ui.components.uniDeckSwitchColors
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.filled.Calculate
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.ScoreAssignColor
import com.unideck.app.ui.theme.ScoreEndSemColor
import com.unideck.app.ui.theme.ScoreMidColor
import com.unideck.app.ui.theme.ScoreQuiz1Color
import com.unideck.app.ui.theme.TABULAR_NUMS

private enum class ScoreFilter(val label: String) {
    ALL("All"),
    THEORY("Theory (50/50)"),
    LAB("Lab (60/40)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoresScreen(
    courses: List<CourseEntity>,
    preferences: UserPreferences,
    onUpdateCourse: (CourseEntity) -> Unit,
    onOpenAddCourse: () -> Unit,
    onOpenGrades: () -> Unit = {},
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(ScoreFilter.ALL) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    val expandedIds = remember { mutableStateMapOf<Long, Boolean>() }
    val haptics = rememberUniDeckHaptics()
    val cs = MaterialTheme.colorScheme

    val allSummaries = remember(courses, preferences.gradeCutoffs) {
        courses.map { AcademicCalculator.summarizeCourseScore(it, preferences.gradeCutoffs) }
    }

    val filteredSummaries = remember(allSummaries, activeFilter) {
        when (activeFilter) {
            ScoreFilter.ALL -> allSummaries
            ScoreFilter.THEORY -> allSummaries.filter { !it.course.isLab }
            ScoreFilter.LAB -> allSummaries.filter { it.course.isLab }
        }
    }

    if (showInsightsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showInsightsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ScoresInsightsSheetContent(summaries = allSummaries)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Filter Chips + Insights Button
        item(key = "scores_filter_bar") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exam Scores",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                haptics.tick()
                                onOpenGrades()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_grades_button")
                        ) {
                            Icon(Icons.Filled.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grades")
                        }
                        if (courses.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    haptics.tick()
                                    showInsightsSheet = true
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("scores_insights_button")
                            ) {
                                Icon(Icons.Filled.Insights, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Insights")
                            }
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ScoreFilter.entries) { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = {
                                haptics.tick()
                                activeFilter = filter
                            },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("score_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (courses.isEmpty()) {
            item(key = "scores_empty") {
                HeroTintCard {
                    Text(
                        text = "No courses to record scores yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add your Theory (50/50) and Lab (60/40) courses to track Mid-Sem, Quizzes, Assignments, and live End-Sem grade targets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
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
        } else if (filteredSummaries.isEmpty()) {
            item(key = "scores_filter_empty") {
                val ext = LocalUniDeckColors.current
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No ${activeFilter.label} courses in this semester.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            // Vibrant High-Contrast Legend for Segmented Score Bar
            item(key = "scores_legend") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendDot(ScoreMidColor, "Mid/Int")
                    LegendDot(ScoreQuiz1Color, "Quizzes")
                    LegendDot(ScoreAssignColor, "Asgn/Rec")
                    LegendDot(ScoreEndSemColor, "End-Sem")
                }
            }

            item(key = "scores_course_group") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredSummaries.forEachIndexed { idx, summary ->
                        val course = summary.course
                        val expanded = expandedIds[course.id] ?: false
                        ExpressiveGroupedCard(
                            index = idx,
                            count = filteredSummaries.size,
                            onClick = {
                                haptics.tick()
                                expandedIds[course.id] = !expanded
                            },
                            modifier = Modifier.testTag("score_card_${course.id}")
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
                                            Text(
                                                text = if (course.isLab) "Lab 60/40" else "Theory 50/50",
                                                style = MaterialTheme.typography.labelSmall,
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
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${"%.1f".format(summary.totalPercentage)}%",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFeatureSettings = TABULAR_NUMS
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // 9-Sided Scalloped Organic Grade Badge
                                    ScallopedGradeBadge(
                                        gradeLabel = summary.resolvedGrade.label,
                                        size = 38.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (expanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Full-Width Material You Segmented Score Bar with Target Tick
                            SegmentedScoreProgressBar(
                                course = course,
                                totalPercentage = summary.totalPercentage,
                                cutoffs = preferences.gradeCutoffs
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            val guidanceIsHard = summary.endSemNeededForAPlus.contains("unreachable", ignoreCase = true) ||
                                summary.endSemNeededForAPlus.contains("Maxed", ignoreCase = true)
                            Text(
                                text = summary.endSemNeededForAPlus,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFeatureSettings = TABULAR_NUMS
                                ),
                                color = if (guidanceIsHard) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )

                            AnimatedVisibility(visible = expanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (course.isLab) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            ScoreInputBox(
                                                label = "Internal /60",
                                                value = course.midSem,
                                                maxValue = 60f,
                                                enabled = !isReadOnly,
                                                onCommit = {
                                                    haptics.tick()
                                                    onUpdateCourse(course.copy(midSem = it))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ScoreInputBox(
                                                label = "End-Sem /40",
                                                value = course.endSem,
                                                maxValue = 40f,
                                                enabled = !isReadOnly,
                                                onCommit = {
                                                    haptics.tick()
                                                    onUpdateCourse(course.copy(endSem = it))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ScoreInputBox(
                                                label = "Mid-Sem /25",
                                                value = course.midSem,
                                                maxValue = 25f,
                                                enabled = !isReadOnly,
                                                onCommit = {
                                                    haptics.tick()
                                                    onUpdateCourse(course.copy(midSem = it))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ScoreInputBox(
                                                label = "Quiz 1 /10",
                                                value = course.quiz1,
                                                maxValue = 10f,
                                                enabled = !isReadOnly,
                                                onCommit = {
                                                    haptics.tick()
                                                    onUpdateCourse(course.copy(quiz1 = it))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ScoreInputBox(
                                                label = "Quiz 2 /10",
                                                value = course.quiz2,
                                                maxValue = 10f,
                                                enabled = !isReadOnly,
                                                onCommit = {
                                                    haptics.tick()
                                                    onUpdateCourse(course.copy(quiz2 = it))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ScoreInputBox(
                                                label = "Assignment /5",
                                                value = course.assignment,
                                                maxValue = 5f,
                                                enabled = !isReadOnly,
                                                onCommit = {
                                                    haptics.tick()
                                                    onUpdateCourse(course.copy(assignment = it))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ScoreInputBox(
                                                label = "End-Sem /50",
                                                value = course.endSem,
                                                maxValue = 50f,
                                                enabled = !isReadOnly,
                                                onCommit = {
                                                    haptics.tick()
                                                    onUpdateCourse(course.copy(endSem = it))
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // Per-course Relative Grading Switch + Scalloped Override Pills
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Relative Grading",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Override grade manually for bell-curve courses",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = course.relativeGradingEnabled,
                                            enabled = !isReadOnly,
                                            onCheckedChange = { enabled ->
                                                haptics.tick()
                                                onUpdateCourse(
                                                    course.copy(
                                                        relativeGradingEnabled = enabled,
                                                        customGradeOverride = if (enabled) {
                                                            course.customGradeOverride ?: summary.resolvedGrade.label
                                                        } else null
                                                    )
                                                )
                                            },
                                            colors = uniDeckSwitchColors()
                                        )
                                    }

                                    AnimatedVisibility(visible = course.relativeGradingEnabled) {
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(GradeLetter.entries) { grade ->
                                                val selected = summary.resolvedGrade == grade
                                                FilterChip(
                                                    selected = selected,
                                                    enabled = !isReadOnly,
                                                    onClick = {
                                                        haptics.confirm()
                                                        onUpdateCourse(course.copy(customGradeOverride = grade.label))
                                                    },
                                                    label = { Text(grade.label) }
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

/**
 * Full-width Material You Segmented Progress Bar (height = 10.dp):
 * - Track: CircleShape in surfaceContainerHighest
 * - Segments: RoundedCornerShape(4.dp) separated by 3.dp transparent gaps
 * - Colors: strictly MaterialTheme.colorScheme (primary, tertiary, secondary, inversePrimary)
 * - Target tick: subtle 2.dp vertical marker on the track indicating next reachable grade cutoff
 */
@Composable
private fun SegmentedScoreProgressBar(
    course: CourseEntity,
    totalPercentage: Float,
    cutoffs: GradeCutoffs
) {
    val cs = MaterialTheme.colorScheme
    val trackColor = cs.surfaceContainerHighest
    val segments: List<Pair<Float, Color>> = if (course.isLab) {
        listOf(
            course.midSem.coerceIn(0f, 60f) to ScoreMidColor,
            course.endSem.coerceIn(0f, 40f) to ScoreEndSemColor
        )
    } else {
        val quizzesCombined = (course.quiz1.coerceIn(0f, 10f) + course.quiz2.coerceIn(0f, 10f))
        listOf(
            course.midSem.coerceIn(0f, 25f) to ScoreMidColor,
            quizzesCombined to ScoreQuiz1Color,
            course.assignment.coerceIn(0f, 5f) to ScoreAssignColor,
            course.endSem.coerceIn(0f, 50f) to ScoreEndSemColor
        )
    }
    val totalEarned = segments.sumOf { it.first.toDouble() }.toFloat().coerceIn(0f, 100f)
    val remaining = (100f - totalEarned).coerceAtLeast(0f)

    // Next reachable grade cutoff tick (or A+ cutoff if already high)
    val nextCutoffFraction = remember(totalPercentage, cutoffs) {
        val ordered = listOf(
            cutoffs.eMin,
            cutoffs.dMin,
            cutoffs.cMin,
            cutoffs.bMin,
            cutoffs.aMin,
            cutoffs.aPlusMin
        )
        val target = ordered.firstOrNull { it > totalPercentage } ?: cutoffs.aPlusMin
        (target.coerceIn(1, 100) / 100f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            segments.forEachIndexed { idx, (value, color) ->
                if (value > 0f) {
                    val animatedVal by animateFloatAsState(
                        targetValue = value,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                        label = "seg_$idx"
                    )
                    Box(
                        modifier = Modifier
                            .weight(animatedVal.coerceAtLeast(0.01f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
            if (remaining > 0f) {
                val animatedRem by animateFloatAsState(
                    targetValue = remaining,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    label = "seg_rem"
                )
                Spacer(modifier = Modifier.weight(animatedRem.coerceAtLeast(0.01f)))
            }
        }

        // Subtle 2.dp vertical target-grade cutoff tick marker
        val tickColor = cs.primary.copy(alpha = 0.85f)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        ) {
            val tickX = (size.width * nextCutoffFraction).coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
            drawLine(
                color = tickColor,
                start = Offset(tickX, 0f),
                end = Offset(tickX, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ScoreInputBox(
    label: String,
    value: Float,
    maxValue: Float,
    onCommit: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var text by remember(value) {
        mutableStateOf(if (value == 0f) "" else AcademicCalculator.formatMark(value))
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val cleaned = raw.filter { it.isDigit() || it == '.' }
            text = cleaned
            val parsed = cleaned.toFloatOrNull()
            if (parsed != null) {
                onCommit(parsed.coerceIn(0f, maxValue))
            } else if (cleaned.isEmpty()) {
                onCommit(0f)
            }
        },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR_NUMS),
        modifier = modifier
    )
}

@Composable
private fun ScoresInsightsSheetContent(summaries: List<CourseScoreSummary>) {
    val ext = LocalUniDeckColors.current
    val cs = MaterialTheme.colorScheme
    val avgInternalPct = remember(summaries) {
        if (summaries.isEmpty()) 0.0
        else summaries.map { (it.internalEarned / it.internalMax) * 100.0 }.average()
    }
    val avgEndSemPct = remember(summaries) {
        if (summaries.isEmpty()) 0.0
        else summaries.map { (it.endSemEarned / it.endSemMax) * 100.0 }.average()
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Analytics, contentDescription = null, tint = cs.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Exam Insights",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = cs.onSurface
                )
            }
        }

        // Compact 2-Column Internal vs End-Sem Split
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ext.cardSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "INTERNAL AVG",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${"%.1f".format(avgInternalPct)}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = ScoreMidColor
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = ext.cardSurface,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "END-SEM AVG",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = cs.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${"%.1f".format(avgEndSemPct)}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = ScoreEndSemColor
                        )
                    }
                }
            }
        }

        // Subject Performance Comparison Bar Chart
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ext.cardSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Subject Comparison",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = cs.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SubjectComparisonBarChart(summaries = summaries)
                }
            }
        }

        // Clean Grade Target Matrix
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ext.cardSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "End-Sem Target Matrix",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = cs.onSurface
                    )
                    val matrixGrades = listOf(
                        GradeLetter.A_PLUS,
                        GradeLetter.A,
                        GradeLetter.B,
                        GradeLetter.C,
                        GradeLetter.D
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Course",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1.25f)
                        )
                        matrixGrades.forEach { g ->
                            Text(
                                text = g.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = cs.primary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider(color = cs.outline.copy(alpha = 0.35f))
                    summaries.forEach { s ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.course.code,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.25f)
                            )
                            matrixGrades.forEach { g ->
                                val txt = s.targetMatrix[g] ?: "--"
                                val col = when (txt) {
                                    "Secured" -> ext.safe
                                    "Maxed" -> ext.danger
                                    else -> cs.onSurface
                                }
                                Text(
                                    text = txt,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontFeatureSettings = TABULAR_NUMS
                                    ),
                                    color = col,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
