package com.unideck.app.ui.screens

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unideck.app.data.prefs.ThemeMode
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.system.AppIconManager
import com.unideck.app.system.AppIconOption
import com.unideck.app.ui.components.expressiveConnectedCardShape
import com.unideck.app.ui.components.rememberUniDeckHaptics
import com.unideck.app.ui.components.uniDeckSwitchColors
import com.unideck.app.ui.theme.LocalUniDeckColors
import com.unideck.app.ui.theme.TABULAR_NUMS
import com.unideck.app.ui.theme.UniDeckPalette
import com.unideck.app.ui.theme.UniDeckPalettes
import com.unideck.app.ui.theme.buildDarkScheme
import com.unideck.app.ui.theme.buildLightScheme
import com.unideck.app.ui.theme.resolveUniDeckPalette

@Composable
internal fun FlatSettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
    )
}

@Composable
internal fun FlatSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val haptics = rememberUniDeckHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(enabled = enabled) {
                haptics.tick()
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
internal fun FlatSettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    switchTestTag: String,
    modifier: Modifier = Modifier
) {
    val haptics = rememberUniDeckHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable {
                haptics.tick()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.tick()
                onCheckedChange(it)
            },
            colors = uniDeckSwitchColors(),
            modifier = Modifier.testTag(switchTestTag)
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    preferences: UserPreferences,
    onBack: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetPitchBlack: (Boolean) -> Unit,
    onSetDynamicMonet: (Boolean) -> Unit,
    onSetAccentKey: (String) -> Unit,
    onSelectAppIcon: (String) -> Unit,
    onSetUseSystemFont: (Boolean) -> Unit,
    onSetDefaultAttendanceTab: (Int) -> Unit,
    onSetCutCorners: (Boolean) -> Unit = {},
    onSetPaletteStyle: (String) -> Unit = {}
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentsSheet by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showDefaultViewDialog by remember { mutableStateOf(false) }
    var showPaletteStyleDialog by remember { mutableStateOf(false) }

    val haptics = rememberUniDeckHaptics()
    val ext = LocalUniDeckColors.current
    val systemDark = isSystemInDarkTheme()
    val isDarkPreview = when (preferences.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val activePalette = remember(preferences.accentKey) {
        resolveUniDeckPalette(preferences.accentKey)
    }
    val activeIcon = remember(preferences.activeIconKey) {
        AppIconManager.allIcons.firstOrNull { it.key == preferences.activeIconKey }
            ?: AppIconManager.allIcons.first()
    }

    if (showThemeDialog) {
        val options = listOf(
            ThemeMode.SYSTEM to "System",
            ThemeMode.DARK to "Dark",
            ThemeMode.LIGHT to "Light"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    options.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tick()
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                                .testTag("theme_mode_${mode.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.themeMode == mode,
                                onClick = {
                                    haptics.tick()
                                    onSetThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPaletteStyleDialog) {
        val styles = listOf(
            "TonalSpot" to "Tonal Spot (Balanced Google Material You)",
            "Vibrant" to "Vibrant (High-chroma accents)",
            "Expressive" to "Expressive (Dynamic hue shift)"
        )
        AlertDialog(
            onDismissRequest = { showPaletteStyleDialog = false },
            title = { Text("Palette style", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    styles.forEach { (styleKey, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tick()
                                    onSetPaletteStyle(styleKey)
                                    showPaletteStyleDialog = false
                                }
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                                .testTag("palette_style_${styleKey.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.paletteStyle == styleKey,
                                onClick = {
                                    haptics.tick()
                                    onSetPaletteStyle(styleKey)
                                    showPaletteStyleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaletteStyleDialog = false }) { Text("Close") }
            }
        )
    }

    if (showFontDialog) {
        val options = listOf(
            true to "System",
            false to "Plus Jakarta Sans"
        )
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Font", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    options.forEach { (useSystem, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tick()
                                    onSetUseSystemFont(useSystem)
                                    showFontDialog = false
                                }
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                                .testTag(if (useSystem) "font_style_system" else "font_style_app"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.useSystemFont == useSystem,
                                onClick = {
                                    haptics.tick()
                                    onSetUseSystemFont(useSystem)
                                    showFontDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text("Close") }
            }
        )
    }

    if (showDefaultViewDialog) {
        val options = listOf(
            0 to "Today",
            1 to "Courses"
        )
        AlertDialog(
            onDismissRequest = { showDefaultViewDialog = false },
            title = { Text("Attendance default view", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    options.forEach { (idx, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tick()
                                    onSetDefaultAttendanceTab(idx)
                                    showDefaultViewDialog = false
                                }
                                .padding(horizontal = 4.dp, vertical = 10.dp)
                                .testTag("attendance_default_view_$idx"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.defaultAttendanceTab.coerceIn(0, 1) == idx,
                                onClick = {
                                    haptics.tick()
                                    onSetDefaultAttendanceTab(idx)
                                    showDefaultViewDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultViewDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAccentsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAccentsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AccentsPickerSheet(
                activeKey = activePalette.key,
                dynamicMonetEnabled = preferences.dynamicMonetEnabled,
                onSelect = { key ->
                    haptics.tick()
                    onSetAccentKey(key)
                    showAccentsSheet = false
                }
            )
        }
    }

    if (showIconSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showIconSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AppIconPickerSheet(
                activeKey = preferences.activeIconKey,
                onSelect = { key ->
                    haptics.tick()
                    onSelectAppIcon(key)
                    showIconSheet = false
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("appearance_back_button")) {
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
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Top "Wallpaper colors" Banner Card
            item(key = "wallpaper_colors_banner") {
                val monetSelected = preferences.dynamicMonetEnabled
                val bannerShape = RoundedCornerShape(28.dp)
                Surface(
                    shape = bannerShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = if (monetSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(58.dp)
                        .clip(bannerShape)
                        .clickable {
                            haptics.tick()
                            onSetDynamicMonet(!preferences.dynamicMonetEnabled)
                        }
                        .testTag("wallpaper_colors_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Wallpaper colors",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        "Dynamic Material You palette"
                                    } else {
                                        "Default color palette"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        if (monetSelected) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Wallpaper colors active",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. 4-Quadrant Tonal Swatch Carousel (16 Seed Themes, selected swatch morphs into CircleShape)
            item(key = "quadrant_swatch_carousel") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.testTag("tonal_swatch_carousel")
                    ) {
                        items(UniDeckPalettes, key = { it.key }) { pal ->
                            val selected = !preferences.dynamicMonetEnabled && activePalette.key == pal.key
                            FourQuadrantTonalSwatch(
                                palette = pal,
                                isDark = isDarkPreview,
                                pureTheme = preferences.pitchBlackEnabled,
                                paletteStyle = preferences.paletteStyle,
                                selected = selected,
                                onClick = {
                                    haptics.tick()
                                    onSetAccentKey(pal.key)
                                }
                            )
                        }
                    }
                }
            }

            item(key = "header_appearance") {
                FlatSettingsSectionHeader("Appearance")
            }

            // 3. Grouped M3 Settings Cards (4.dp inner gap, 20.dp outer corners)
            item(key = "grouped_appearance_cards") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Card 1: Theme Mode Segmented Row (System | Light | Dark)
                    Surface(
                        shape = expressiveConnectedCardShape(0, 4),
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThemeDialog = true }
                                .padding(14.dp)
                                .testTag("appearance_theme_row")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.DarkMode,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Theme",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = when (preferences.themeMode) {
                                        ThemeMode.SYSTEM -> "System"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            val modes = listOf(
                                ThemeMode.SYSTEM to "System",
                                ThemeMode.LIGHT to "Light",
                                ThemeMode.DARK to "Dark"
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                modes.forEachIndexed { idx, (mode, label) ->
                                    SegmentedButton(
                                        selected = preferences.themeMode == mode,
                                        onClick = {
                                            haptics.tick()
                                            onSetThemeMode(mode)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(idx, modes.size),
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = if (preferences.themeMode == mode) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: Pitch-Black AMOLED Switch
                    Surface(
                        shape = expressiveConnectedCardShape(1, 4),
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlatSettingsSwitchRow(
                            icon = Icons.Filled.Contrast,
                            title = "Pure theme",
                            subtitle = "Pure #000000 AMOLED black / #FFFFFF white surface",
                            checked = preferences.pitchBlackEnabled,
                            onCheckedChange = onSetPitchBlack,
                            switchTestTag = "pitch_black_switch"
                        )
                    }

                    // Card 3: Dynamic colors (Monet) Switch
                    Surface(
                        shape = expressiveConnectedCardShape(2, 4),
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlatSettingsSwitchRow(
                            icon = Icons.Filled.ColorLens,
                            title = "Dynamic colors (Monet)",
                            subtitle = "Follow Android 12+ wallpaper palette",
                            checked = preferences.dynamicMonetEnabled,
                            onCheckedChange = onSetDynamicMonet,
                            switchTestTag = "dynamic_monet_switch"
                        )
                    }

                    // Card 4: Palette Style & Accents Grid Opener
                    Surface(
                        shape = expressiveConnectedCardShape(3, 4),
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            val activeStyleLabel = if (preferences.paletteStyle == "Neutral") "TonalSpot" else preferences.paletteStyle
                            FlatSettingsRow(
                                icon = Icons.Filled.Tune,
                                title = "Palette style",
                                subtitle = "Tonal harmony for custom seed colors",
                                onClick = { showPaletteStyleDialog = true },
                                modifier = Modifier.testTag("open_palette_style_dialog"),
                                trailing = {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = activeStyleLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            )
                            FlatSettingsRow(
                                icon = Icons.Filled.Palette,
                                title = "Accents",
                                subtitle = if (preferences.dynamicMonetEnabled) "Monet active • Tap for 4×4 grid" else activePalette.name,
                                onClick = { showAccentsSheet = true },
                                modifier = Modifier.testTag("open_accents_picker"),
                                trailing = {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (preferences.dynamicMonetEnabled) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    activePalette.swatchColor
                                                }
                                            )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item(key = "header_layout_typography") {
                FlatSettingsSectionHeader("Layout & Typography")
            }

            item(key = "grouped_layout_cards") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = expressiveConnectedCardShape(0, 3),
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlatSettingsRow(
                            icon = Icons.Filled.Apps,
                            title = "Icon",
                            subtitle = "${activeIcon.label} • ${AppIconManager.allIcons.size} adaptive icons",
                            onClick = { showIconSheet = true },
                            modifier = Modifier.testTag("open_icon_switcher"),
                            trailing = {
                                AdaptiveIconPreview(
                                    option = activeIcon,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        )
                    }

                    Surface(
                        shape = expressiveConnectedCardShape(1, 3),
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlatSettingsRow(
                            icon = Icons.Filled.FontDownload,
                            title = "Font",
                            subtitle = if (preferences.useSystemFont) "System" else "Plus Jakarta Sans",
                            onClick = { showFontDialog = true },
                            modifier = Modifier.testTag("appearance_font_row")
                        )
                    }

                    Surface(
                        shape = expressiveConnectedCardShape(2, 3),
                        color = ext.cardSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val defaultTabLabel = if (preferences.defaultAttendanceTab == 1) "Courses" else "Today"
                        FlatSettingsRow(
                            icon = Icons.Filled.ViewAgenda,
                            title = "Attendance default view",
                            subtitle = defaultTabLabel,
                            onClick = { showDefaultViewDialog = true },
                            modifier = Modifier.testTag("appearance_default_view_row")
                        )
                    }
                }
            }

            // 4. Live Home Screen Widget Previews Section
            item(key = "header_widget_previews") {
                FlatSettingsSectionHeader("Home Screen Widgets")
            }

            item(key = "widget_previews_section") {
                WidgetPreviewsSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("widget_previews_section")
                )
            }
        }
    }
}

/**
 * 58.dp × 58.dp 4-Quadrant Tonal Swatch previewing `secondaryContainer`, `surfaceContainerHighest`,
 * `primaryContainer`, and `primary` for each of the 16 seed palettes.
 * Morphs smoothly from a rounded square (16.dp) into a full CircleShape (29.dp) when selected.
 */
@Composable
private fun FourQuadrantTonalSwatch(
    palette: UniDeckPalette,
    isDark: Boolean,
    pureTheme: Boolean,
    paletteStyle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val previewScheme = remember(palette, isDark, pureTheme, paletteStyle) {
        if (isDark) buildDarkScheme(palette, pureTheme, paletteStyle)
        else buildLightScheme(palette, pureTheme, paletteStyle)
    }
    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 29.dp else 16.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "swatchCornerRadius"
    )
    val innerPadding by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "swatchInnerPadding"
    )
    val safeCornerRadius = cornerRadius.coerceAtLeast(0.dp)
    val safeInnerPadding = innerPadding.coerceAtLeast(0.dp)
    val outerShape = RoundedCornerShape(safeCornerRadius)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(2.dp)
            .testTag("quadrant_swatch_${palette.key}")
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(outerShape)
                .then(
                    if (selected) {
                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, outerShape)
                    } else {
                        Modifier
                    }
                )
                .padding(safeInnerPadding)
                .clip(outerShape),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(previewScheme.secondaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(previewScheme.surfaceContainerHighest)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(previewScheme.primaryContainer)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(previewScheme.primary)
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(previewScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = palette.name,
                        tint = previewScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = palette.name,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * Live visual showcase of UniDeck's Material You Home Screen Glance Widgets
 * rendered with the active ColorScheme.
 */
@Composable
private fun WidgetPreviewsSection(
    modifier: Modifier = Modifier
) {
    val ext = LocalUniDeckColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row of 2 compact widgets: Attendance & GPA + Next Class & Today
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Widget 1: Attendance & GPA Snapshot (2x2)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ext.cardSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attendance & GPA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "W10",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "ATTENDANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "88%",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = TABULAR_NUMS
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SGPA",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "9.20",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFeatureSettings = TABULAR_NUMS
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Widget 2: Next Class & Today (4x2 miniature)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ext.cardSurface,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "88% Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = TABULAR_NUMS
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "11:00–12:00",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFeatureSettings = TABULAR_NUMS
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "Operating Systems",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "LH-204 • Theory",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Widget 3: Wide Today's Classes Widget Preview (4x2)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ext.cardSurface,
            modifier = Modifier.fillMaxWidth()
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
                        text = "Today's Classes",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Sem 4",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "09:00–10:00 • LH-204",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Data Structures & Algorithms",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Pre",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "11:00–12:00 • CS-Lab 2",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = TABULAR_NUMS
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Operating Systems Lab",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Lab",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentsPickerSheet(
    activeKey: String,
    dynamicMonetEnabled: Boolean,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
    ) {
        Text(
            text = "Accents",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (dynamicMonetEnabled) "Picking an accent switches off Dynamic colors" else "Choose an accent color",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(340.dp)
        ) {
            items(UniDeckPalettes, key = { it.key }) { pal ->
                val selected = !dynamicMonetEnabled && activeKey == pal.key
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(pal.key) }
                        .padding(vertical = 4.dp)
                        .testTag("accent_swatch_${pal.key}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(pal.swatchColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = pal.name,
                                tint = pal.onDarkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pal.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun AdaptiveIconPreview(
    option: AppIconOption,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = option.bgDrawable),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            painter = painterResource(id = option.fgDrawable),
            contentDescription = option.label,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AppIconPickerSheet(
    activeKey: String,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
    ) {
        Text(
            text = "Choose App Icon",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${AppIconManager.allIcons.size} colorful flat-outlined animal & deck adaptive icons",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(440.dp)
        ) {
            items(AppIconManager.allIcons, key = { it.key }) { option ->
                val isSelected = option.key == activeKey
                val iconCorner by animateDpAsState(
                    targetValue = if (isSelected) 26.dp else 14.dp,
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    label = "iconOptionCorner"
                )
                val safeIconShape = RoundedCornerShape(iconCorner.coerceAtLeast(0.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(option.key) }
                        .padding(2.dp)
                        .testTag("icon_option_${option.key}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(safeIconShape)
                            .then(
                                if (isSelected) {
                                    Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, safeIconShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AdaptiveIconPreview(option = option, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
