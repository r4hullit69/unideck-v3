package com.unideck.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.data.local.AttendanceStatus
import com.unideck.app.domain.AttendanceHealth
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Uniform 20.dp rounded card shape used across standalone cards in UniDeck.
 */
val UniformCardShape = RoundedCornerShape(20.dp)

/**
 * 9-Sided Scalloped Organic Shape (Material 3 Expressive Cookie9Sided equivalent).
 */
val Cookie9SidedShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy)
        val baseR = radius * 0.90f
        val ampR = radius * 0.10f
        val steps = 108
        for (i in 0..steps) {
            val theta = (i.toFloat() / steps) * (2.0 * PI) - (PI / 2.0)
            val r = baseR + ampR * cos(9.0 * theta).toFloat()
            val x = cx + r * cos(theta).toFloat()
            val y = cy + r * sin(theta).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Connected grouped card shape: 20.dp outer corners at top/bottom of a stack and 6.dp inner corners.
 */
@Suppress("UNUSED_PARAMETER")
fun expressiveConnectedCardShape(
    index: Int,
    count: Int,
    cutCorners: Boolean = false
): CornerBasedShape {
    val outer = 20.dp
    val inner = 6.dp
    val top = if (index == 0) outer else inner
    val bottom = if (index >= count - 1) outer else inner
    return RoundedCornerShape(topStart = top, topEnd = top, bottomEnd = bottom, bottomStart = bottom)
}

@Suppress("UNUSED_PARAMETER")
fun expressiveGroupShape(index: Int, count: Int): RoundedCornerShape = UniformCardShape

/**
 * Clean Material You Soft Squircle Grade Badge (`A+` through `F`).
 */
@Composable
fun ScallopedGradeBadge(
    gradeLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    useTertiary: Boolean = false
) {
    val isFail = gradeLabel == "F"
    val containerColor = when {
        isFail -> MaterialTheme.colorScheme.errorContainer
        useTertiary -> MaterialTheme.colorScheme.tertiaryContainer
        gradeLabel == "A+" || gradeLabel == "A" -> MaterialTheme.colorScheme.primaryContainer
        gradeLabel == "B" || gradeLabel == "C" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        isFail -> MaterialTheme.colorScheme.onErrorContainer
        useTertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        gradeLabel == "A+" || gradeLabel == "A" -> MaterialTheme.colorScheme.onPrimaryContainer
        gradeLabel == "B" || gradeLabel == "C" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val badgeShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(badgeShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = gradeLabel,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (size >= 40.dp) 13.5.sp else 12.sp,
                fontFeatureSettings = TABULAR_NUMS
            ),
            color = contentColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Material 3 Expressive Connected Pill Button Group (`height = 38.dp`, `3.dp` gap).
 * Active tab morphs into a full pill (`CircleShape`) with primary/tertiary fill;
 * inactive tabs use `surfaceContainerHigh` with `12.dp` corners.
 */
@Composable
fun ExpressiveConnectedButtonGroup(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTags: List<String> = emptyList()
) {
    val haptics = rememberUniDeckHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            val cornerRadius by animateDpAsState(
                targetValue = if (selected) 19.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                label = "pillCorner"
            )
            val containerColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(200),
                label = "pillBg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(200),
                label = "pillFg"
            )
            val shape = RoundedCornerShape(cornerRadius)
            val tag = testTags.getOrNull(idx) ?: "connected_tab_$idx"

            Surface(
                shape = shape,
                color = containerColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .clickable {
                        haptics.tick()
                        onSelect(idx)
                    }
                    .testTag(tag)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                        ),
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun UniDeckWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    weight: FontWeight = FontWeight.Bold
) {
    val ext = LocalUniDeckColors.current
    val primary = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = ext.wordmarkLeading, fontWeight = weight)) {
                append("Uni")
            }
            withStyle(SpanStyle(color = primary, fontWeight = weight)) {
                append("Deck")
            }
        },
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = fontSize,
            fontWeight = weight,
            letterSpacing = (-0.5).sp
        ),
        modifier = modifier
    )
}

/**
 * Explicit high-contrast SwitchColors so unchecked switches in Dark & Pure Black modes
 * remain clearly separated from dark card surfaces.
 */
@Composable
fun uniDeckSwitchColors(): SwitchColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = Color(0xFFBDC4D6),
            uncheckedTrackColor = Color(0xFF2A2E3B),
            uncheckedBorderColor = Color(0xFF4A5060)
        )
    } else {
        SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Slim single-line banner shown when viewing a past or manually completed semester.
 */
@Composable
fun ArchivedSemesterBanner(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .testTag("archived_semester_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Read-only",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Viewing archived semester • Read-only",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val haptics = rememberUniDeckHaptics()
            TextButton(
                onClick = {
                    haptics.confirm()
                    onUnlock()
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("unlock_archived_semester_button")
            ) {
                Text(
                    text = "Unlock",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Hero card with subtle 10% opacity top-left radial tint of active primary color,
 * uniform 20.dp rounded corners (or cut corners when enabled), and borderless tonal container.
 */
@Composable
fun HeroTintCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalUniDeckColors.current
    val primary = MaterialTheme.colorScheme.primary
    val shape = MaterialTheme.shapes.large

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
        shape = shape,
        color = ext.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(0f, 0f),
                            radius = size.maxDimension * 0.85f
                        )
                    )
                }
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * Uniform borderless tonal card with 20.dp rounded corners (or cut corners when enabled) and 16.dp internal padding.
 */
@Composable
fun ExpressiveGroupedCard(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalUniDeckColors.current
    val shape = MaterialTheme.shapes.large

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = ext.cardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun healthColor(health: AttendanceHealth): Color {
    val ext = LocalUniDeckColors.current
    return when (health) {
        AttendanceHealth.SAFE -> ext.safe
        AttendanceHealth.MARGIN -> ext.margin
        AttendanceHealth.DANGER -> ext.danger
        AttendanceHealth.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun CircularAttendanceRing(
    percentage: Double,
    health: AttendanceHealth,
    hasData: Boolean = true,
    size: Dp = 56.dp,
    strokeWidth: Dp = 5.5.dp,
    modifier: Modifier = Modifier
) {
    val ringColor = if (hasData) healthColor(health) else MaterialTheme.colorScheme.outline
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (hasData) (percentage / 100.0).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )

    val ringDescription = if (hasData) "${percentage.toInt()}% attendance" else "No attendance data"
    val labelFontSize = when {
        size >= 64.dp -> 15.sp
        size <= 44.dp -> 10.5.sp
        else -> 12.sp
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = ringDescription },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = if (hasData) "${percentage.toInt()}%" else "--",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = labelFontSize,
                fontFeatureSettings = TABULAR_NUMS
            ),
            color = if (hasData) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun AttendanceActionRow(
    activeStatus: String?,
    onSelectStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTagPrefix: String = "att"
) {
    val ext = LocalUniDeckColors.current
    val haptics = rememberUniDeckHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TonalStatusButton(
            label = "Present",
            selected = activeStatus == AttendanceStatus.PRESENT,
            fill = ext.presentFill,
            contentColor = ext.presentContent,
            enabled = enabled,
            onClick = {
                haptics.forAttendanceStatus(AttendanceStatus.PRESENT)
                onSelectStatus(AttendanceStatus.PRESENT)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_present")
        )
        TonalStatusButton(
            label = "Absent",
            selected = activeStatus == AttendanceStatus.ABSENT,
            fill = ext.absentFill,
            contentColor = ext.absentContent,
            enabled = enabled,
            onClick = {
                haptics.forAttendanceStatus(AttendanceStatus.ABSENT)
                onSelectStatus(AttendanceStatus.ABSENT)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_absent")
        )
        TonalStatusButton(
            label = "Cancelled",
            selected = activeStatus == AttendanceStatus.CANCELLED,
            fill = ext.cancelledFill,
            contentColor = ext.cancelledContent,
            enabled = enabled,
            onClick = {
                haptics.forAttendanceStatus(AttendanceStatus.CANCELLED)
                onSelectStatus(AttendanceStatus.CANCELLED)
            },
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_cancelled")
        )
    }
}

@Composable
private fun TonalStatusButton(
    label: String,
    selected: Boolean,
    fill: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    val bg = if (selected) fill else fill.copy(alpha = 0.65f)

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = bg
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LaunchLoaderOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = 250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UniDeckWordmark(
                    fontSize = 30.sp,
                    weight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(20.dp))
                ContainedLoadingIndicator(
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
