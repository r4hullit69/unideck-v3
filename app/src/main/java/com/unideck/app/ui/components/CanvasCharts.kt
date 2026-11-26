package com.unideck.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.domain.CourseScoreSummary
import com.unideck.app.domain.GradeLetter
import com.unideck.app.domain.SemesterGpaPoint
import com.unideck.app.domain.TrendPoint
import com.unideck.app.domain.WeekdayAttendanceStat
import com.unideck.app.ui.theme.GradeAColor
import com.unideck.app.ui.theme.GradeAPlusColor
import com.unideck.app.ui.theme.GradeBColor
import com.unideck.app.ui.theme.GradeCColor
import com.unideck.app.ui.theme.GradeDColor
import com.unideck.app.ui.theme.GradeEColor
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.ScoreAssignColor
import com.unideck.app.ui.theme.ScoreEndSemColor
import com.unideck.app.ui.theme.ScoreMidColor
import com.unideck.app.ui.theme.ScoreQuiz1Color
import com.unideck.app.ui.theme.ScoreQuiz2Color
import com.unideck.app.ui.theme.TABULAR_NUMS
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Compact sparkline preview for Smart Subject Detail with dotted target threshold line.
 */
@Composable
fun AttendanceTrendSparkline(
    points: List<TrendPoint>,
    thresholdPercent: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val danger = LocalUniDeckColors.current.danger
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "sparklineAnim"
    )

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(104.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log classes to plot your attendance trend",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
    ) {
        val padH = 8.dp.toPx()
        val padV = 10.dp.toPx()
        val w = (size.width - padH * 2).coerceAtLeast(1f)
        val h = (size.height - padV * 2).coerceAtLeast(1f)

        val thresholdY = padV + h * (1f - (thresholdPercent.coerceIn(0, 100) / 100f))
        drawLine(
            color = danger.copy(alpha = 0.85f),
            start = Offset(padH, thresholdY),
            end = Offset(size.width - padH, thresholdY),
            strokeWidth = 1.75.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )

        val coords = points.mapIndexed { idx, pt ->
            val x = if (points.size == 1) padH + w / 2f else padH + (idx.toFloat() / (points.size - 1)) * w
            val y = padV + h * (1f - (pt.cumulativePercentage.toFloat().coerceIn(0f, 100f) / 100f) * progress)
            Offset(x, y)
        }

        if (coords.size >= 2) {
            val linePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) lineTo(coords[i].x, coords[i].y)
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, padV + h)
                lineTo(coords.first().x, padV + h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primary.copy(alpha = 0.25f), Color.Transparent),
                    startY = padV,
                    endY = padV + h
                )
            )
            drawPath(
                path = linePath,
                color = primary,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        coords.lastOrNull()?.let { last ->
            drawCircle(color = primary, radius = 4.5.dp.toPx(), center = last)
        }
    }
}

/**
 * Full-screen interactive Compose Canvas line chart with touch-to-scrub tooltips.
 */
@Composable
fun InteractiveAttendanceTrendChart(
    points: List<TrendPoint>,
    thresholdPercent: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val ext = LocalUniDeckColors.current
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    var selectedIndex by remember(points) { mutableIntStateOf((points.size - 1).coerceAtLeast(0)) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d") }

    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No counted sessions yet to chart",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val activePoint = points[safeIndex]

    Column(modifier = modifier.fillMaxWidth()) {
        // Live Scrub Tooltip Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activePoint.date.format(dateFmt),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${activePoint.presentCount}/${activePoint.totalCount} classes attended • Drag chart to scrub",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "%.2f%%".format(activePoint.cumulativePercentage),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = TABULAR_NUMS
                    ),
                    color = if (activePoint.cumulativePercentage >= thresholdPercent) ext.safe else ext.danger
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.size > 1) {
                                val frac = ((offset.x - 16f) / (size.width - 32f)).coerceIn(0f, 1f)
                                selectedIndex = (frac * (points.size - 1)).roundToInt()
                            }
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures { change, _ ->
                            if (points.size > 1) {
                                val frac = ((change.position.x - 16f) / (size.width - 32f)).coerceIn(0f, 1f)
                                selectedIndex = (frac * (points.size - 1)).roundToInt()
                            }
                        }
                    }
            ) {
                val padH = 14.dp.toPx()
                val padV = 14.dp.toPx()
                val w = (size.width - padH * 2).coerceAtLeast(1f)
                val h = (size.height - padV * 2).coerceAtLeast(1f)

                // Horizontal reference grid lines (100%, 50%, 0%)
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = padV + h * frac
                    drawLine(
                        color = gridColor,
                        start = Offset(padH, y),
                        end = Offset(size.width - padH, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Target threshold dotted coral-red line
                val thresholdY = padV + h * (1f - thresholdPercent / 100f)
                drawLine(
                    color = ext.danger,
                    start = Offset(padH, thresholdY),
                    end = Offset(size.width - padH, thresholdY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                val coords = points.mapIndexed { idx, pt ->
                    val x = if (points.size == 1) padH + w / 2f else padH + (idx.toFloat() / (points.size - 1)) * w
                    val y = padV + h * (1f - (pt.cumulativePercentage.toFloat().coerceIn(0f, 100f) / 100f))
                    Offset(x, y)
                }

                if (coords.size >= 2) {
                    val path = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) lineTo(coords[i].x, coords[i].y)
                    }
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(coords.last().x, padV + h)
                        lineTo(coords.first().x, padV + h)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primary.copy(alpha = 0.28f), Color.Transparent),
                            startY = padV,
                            endY = padV + h
                        )
                    )
                    drawPath(
                        path = path,
                        color = primary,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Scrub guide line + glowing node
                val activeCoord = coords[safeIndex]
                drawLine(
                    color = primary.copy(alpha = 0.45f),
                    start = Offset(activeCoord.x, padV),
                    end = Offset(activeCoord.x, padV + h),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                drawCircle(color = primary.copy(alpha = 0.25f), radius = 11.dp.toPx(), center = activeCoord)
                drawCircle(color = primary, radius = 5.5.dp.toPx(), center = activeCoord)
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = activeCoord)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = points.first().date.format(dateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Target: $thresholdPercent%",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ext.danger
            )
            Text(
                text = points.last().date.format(dateFmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Horizontal bar chart showing attendance percentage by weekday (Mon–Sat).
 */
@Composable
fun WeekdayBreakdownChart(
    stats: List<WeekdayAttendanceStat>,
    thresholdPercent: Int,
    modifier: Modifier = Modifier
) {
    val ext = LocalUniDeckColors.current
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.forEach { day ->
            val barColor = when {
                day.total == 0 -> trackColor
                day.percentage < thresholdPercent -> ext.danger
                day.percentage < thresholdPercent + 5 -> ext.margin
                else -> ext.safe
            }
            val frac by animateFloatAsState(
                targetValue = if (day.total == 0) 0f else (day.percentage / 100.0).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(480),
                label = "dayBar"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.shortLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(36.dp)
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                ) {
                    val radius = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
                    if (frac > 0f) {
                        drawRoundRect(
                            color = barColor,
                            size = Size(size.width * frac, size.height),
                            cornerRadius = radius
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (day.total == 0) "--" else "${day.percentage.roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = TABULAR_NUMS
                    ),
                    color = if (day.total == 0) MaterialTheme.colorScheme.onSurfaceVariant else barColor,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

/**
 * Horizontal bar chart comparing course totals in Scores Insights (Theory vs Lab color-coded).
 */
@Composable
fun SubjectComparisonBarChart(
    summaries: List<CourseScoreSummary>,
    modifier: Modifier = Modifier
) {
    val theoryColor = MaterialTheme.colorScheme.primary
    val labColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(color = theoryColor, label = "Theory (50/50)")
            LegendDot(color = labColor, label = "Lab (60/40)")
        }
        summaries.forEach { summary ->
            val barColor = if (summary.course.isLab) labColor else theoryColor
            val frac by animateFloatAsState(
                targetValue = (summary.totalPercentage / 100f).coerceIn(0f, 1f),
                animationSpec = tween(500),
                label = "subjBar"
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${summary.course.code} • ${summary.course.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${"%.1f".format(summary.totalPercentage)}% (${summary.resolvedGrade.label})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = TABULAR_NUMS
                        ),
                        color = barColor
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    val r = CornerRadius(size.height / 2f, size.height / 2f)
                    drawRoundRect(color = trackColor, size = size, cornerRadius = r)
                    if (frac > 0f) {
                        drawRoundRect(
                            color = barColor,
                            size = Size(size.width * frac, size.height),
                            cornerRadius = r
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dual-line Compose Canvas chart plotting semester-by-semester SGPA vs Cumulative CGPA.
 */
@Composable
fun SgpaCgpaTrendChart(
    points: List<SemesterGpaPoint>,
    modifier: Modifier = Modifier
) {
    val sgpaColor = MaterialTheme.colorScheme.primary
    val cgpaColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    if (points.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(color = sgpaColor, label = "SGPA")
            LegendDot(color = cgpaColor, label = "Cumulative CGPA")
        }
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
        ) {
            val padH = 14.dp.toPx()
            val padV = 12.dp.toPx()
            val w = (size.width - padH * 2).coerceAtLeast(1f)
            val h = (size.height - padV * 2).coerceAtLeast(1f)

            // Reference lines at 10, 7.5, 5, 2.5, 0
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = padV + h * frac
                drawLine(
                    color = gridColor,
                    start = Offset(padH, y),
                    end = Offset(size.width - padH, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun mapY(gpa: Double): Float = padV + h * (1f - (gpa.toFloat().coerceIn(0f, 10f) / 10f))
            fun mapX(idx: Int): Float =
                if (points.size == 1) padH + w / 2f else padH + (idx.toFloat() / (points.size - 1)) * w

            val sgpaCoords = points.mapIndexed { idx, p -> Offset(mapX(idx), mapY(p.sgpa)) }
            val cgpaCoords = points.mapIndexed { idx, p -> Offset(mapX(idx), mapY(p.cumulativeCgpa)) }

            if (points.size >= 2) {
                val sgpaPath = Path().apply {
                    moveTo(sgpaCoords.first().x, sgpaCoords.first().y)
                    for (i in 1 until sgpaCoords.size) lineTo(sgpaCoords[i].x, sgpaCoords[i].y)
                }
                val cgpaPath = Path().apply {
                    moveTo(cgpaCoords.first().x, cgpaCoords.first().y)
                    for (i in 1 until cgpaCoords.size) lineTo(cgpaCoords[i].x, cgpaCoords[i].y)
                }
                drawPath(
                    path = sgpaPath,
                    color = sgpaColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = cgpaPath,
                    color = cgpaColor,
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                    )
                )
            }

            sgpaCoords.forEach { pt ->
                drawCircle(color = sgpaColor, radius = 4.5.dp.toPx(), center = pt)
            }
            cgpaCoords.forEach { pt ->
                drawCircle(color = cgpaColor, radius = 4.dp.toPx(), center = pt)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.semester.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Material You Segmented Grade Distribution bar (A+, A, B, C, D, E counts)
 * with 10.dp CircleShape track, 4.dp rounded pill segments, and 3.dp transparent gaps.
 */
@Composable
fun GradeDistributionBar(
    gradeCounts: Map<GradeLetter, Int>,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val buckets = listOf(
        GradeLetter.A_PLUS to GradeAPlusColor,
        GradeLetter.A to GradeAColor,
        GradeBColor.let { GradeLetter.B to it },
        GradeLetter.C to GradeCColor,
        GradeLetter.D to GradeDColor,
        GradeLetter.E to GradeEColor
    )
    val total = buckets.sumOf { gradeCounts[it.first] ?: 0 }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (total == 0) {
            Text(
                text = "Add courses to see grade distribution",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(cs.surfaceContainerHighest)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                buckets.forEach { (grade, color) ->
                    val count = gradeCounts[grade] ?: 0
                    if (count > 0) {
                        val animatedWeight by animateFloatAsState(
                            targetValue = count.toFloat(),
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = 0.8f,
                                stiffness = 380f
                            ),
                            label = "gradeDistWeight"
                        )
                        Box(
                            modifier = Modifier
                                .weight(animatedWeight.coerceAtLeast(0.01f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            buckets.forEach { (grade, color) ->
                val count = gradeCounts[grade] ?: 0
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${grade.label}:$count",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFeatureSettings = TABULAR_NUMS
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
