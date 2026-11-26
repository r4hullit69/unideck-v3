package com.unideck.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.ui.components.HeroTintCard
import com.unideck.app.ui.components.InteractiveAttendanceTrendChart
import com.unideck.app.ui.components.UniformCardShape
import com.unideck.app.ui.components.WeekdayBreakdownChart
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceAnalyticsScreen(
    course: CourseEntity?,
    logs: List<AttendanceLogEntity>,
    preferences: UserPreferences,
    onBack: () -> Unit
) {
    val ext = LocalUniDeckColors.current
    val filteredLogs = remember(logs, course) {
        if (course != null) logs.filter { it.courseId == course.id } else logs
    }
    val stats = remember(course, filteredLogs, preferences.minAttendanceThreshold) {
        AcademicCalculator.computeAttendanceStats(
            logs = filteredLogs,
            thresholdPercent = preferences.minAttendanceThreshold,
            initialAttended = course?.initialAttended ?: 0,
            initialTotal = course?.initialTotal ?: 0
        )
    }
    val monthlyPct = remember(filteredLogs) {
        AcademicCalculator.computeMonthlyPercentage(filteredLogs)
    }
    val trendPoints = remember(course, filteredLogs) {
        AcademicCalculator.computeTrendPoints(
            logs = filteredLogs,
            initialAttended = course?.initialAttended ?: 0,
            initialTotal = course?.initialTotal ?: 0
        )
    }
    val weekdayStats = remember(filteredLogs) {
        AcademicCalculator.computeWeekdayBreakdown(filteredLogs)
    }
    val streak = remember(filteredLogs) {
        AcademicCalculator.computeCurrentStreak(filteredLogs)
    }

    var sliderDelta by remember { mutableFloatStateOf(0f) }
    val deltaInt = sliderDelta.roundToInt()
    val projectedPct = remember(stats, deltaInt) {
        AcademicCalculator.projectAttendance(stats.present, stats.totalCounted, deltaInt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (course != null) "${course.code} Analytics" else "Attendance Analytics",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (course != null) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("analytics_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // 1. Full Interactive Trend Chart (Touch-to-Scrub)
            item(key = "full_trend_chart") {
                Surface(
                    shape = UniformCardShape,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Cumulative Attendance Trend",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        InteractiveAttendanceTrendChart(
                            points = trendPoints,
                            thresholdPercent = preferences.minAttendanceThreshold
                        )
                    }
                }
            }

            // 2. Monthly vs Overall Delta
            item(key = "monthly_vs_overall") {
                val delta = if (monthlyPct != null && stats.totalCounted > 0) {
                    monthlyPct - stats.percentage
                } else null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = UniformCardShape,
                        color = ext.cardSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "This Month",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = monthlyPct?.let { "%.1f%%".format(it) } ?: "--",
                                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (delta != null) {
                                val sign = if (delta >= 0) "+" else ""
                                Text(
                                    text = "$sign${"%.1f".format(delta)}% vs overall",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = TABULAR_NUMS
                                    ),
                                    color = if (delta >= 0) ext.safe else ext.danger
                                )
                            }
                        }
                    }

                    Surface(
                        shape = UniformCardShape,
                        color = ext.cardSurface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Overall",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (stats.totalCounted == 0) "--" else "%.1f%%".format(stats.percentage),
                                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${stats.present}/${stats.totalCounted} attended",
                                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TABULAR_NUMS),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Smart Bunk / Recovery Calculator + Streak & Buffer
            item(key = "bunk_recovery_calculator") {
                HeroTintCard {
                    Text(
                        text = "Smart Bunk & Recovery Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val scenarioLabel = when {
                        deltaInt == 0 -> "Current standing (slide left to miss, right to attend)"
                        deltaInt > 0 -> "If I attend the next $deltaInt ${if (deltaInt == 1) "class" else "classes"}"
                        else -> "If I miss the next ${abs(deltaInt)} ${if (abs(deltaInt) == 1) "class" else "classes"}"
                    }
                    Text(
                        text = scenarioLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (stats.totalCounted == 0 && deltaInt == 0) "--" else "%.2f%%".format(projectedPct),
                            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = TABULAR_NUMS),
                            color = if (projectedPct >= preferences.minAttendanceThreshold) ext.safe else ext.danger
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (projectedPct >= preferences.minAttendanceThreshold) ext.presentFill else ext.absentFill
                        ) {
                            Text(
                                text = if (projectedPct >= preferences.minAttendanceThreshold) "Above ${preferences.minAttendanceThreshold}%" else "Below ${preferences.minAttendanceThreshold}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (projectedPct >= preferences.minAttendanceThreshold) ext.safe else ext.danger,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Slider(
                        value = sliderDelta,
                        onValueChange = { sliderDelta = it },
                        valueRange = -15f..15f,
                        steps = 29,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bunk_recovery_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Miss 15", style = MaterialTheme.typography.labelSmall, color = ext.danger)
                        Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Attend 15", style = MaterialTheme.typography.labelSmall, color = ext.safe)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = ext.margin
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$streak",
                                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR_NUMS),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Current Streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (stats.safeBunks > 0) ext.safe else ext.danger
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (stats.percentage >= preferences.minAttendanceThreshold) "${stats.safeBunks}" else "-${stats.classesNeeded}",
                                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR_NUMS),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (stats.percentage >= preferences.minAttendanceThreshold) "Safe Bunk Buffer" else "Needed to Recover",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Day-of-Week Breakdown
            item(key = "weekday_breakdown") {
                Surface(
                    shape = UniformCardShape,
                    color = ext.cardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Day-of-Week Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Spot which weekdays get missed most often",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        WeekdayBreakdownChart(
                            stats = weekdayStats,
                            thresholdPercent = preferences.minAttendanceThreshold
                        )
                    }
                }
            }
        }
    }
}
