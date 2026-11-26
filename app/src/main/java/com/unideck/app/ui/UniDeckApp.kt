package com.unideck.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.ui.components.AddOneOffClassBottomSheet
import com.unideck.app.ui.components.ArchivedSemesterBanner
import com.unideck.app.ui.components.LaunchLoaderOverlay
import com.unideck.app.ui.components.SemesterEditorDialog
import com.unideck.app.ui.components.UniDeckWordmark
import com.unideck.app.ui.screens.AcademicSettingsScreen
import com.unideck.app.ui.screens.AppearanceScreen
import com.unideck.app.ui.screens.AttendanceAnalyticsScreen
import com.unideck.app.ui.screens.AttendanceScreen
import com.unideck.app.ui.screens.BackupSettingsScreen
import com.unideck.app.ui.screens.CourseEditorDialog
import com.unideck.app.ui.screens.GradesScreen
import com.unideck.app.ui.screens.RemindersSettingsScreen
import com.unideck.app.ui.screens.ScoresScreen
import com.unideck.app.ui.screens.SettingsScreen
import com.unideck.app.ui.screens.SubjectDetailScreen
import com.unideck.app.ui.screens.TimetableScreen
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
private data object HomeRoute

@Serializable
private data class SubjectDetailRoute(val courseId: Long)

@Serializable
private data class AttendanceAnalyticsRoute(val courseId: Long)

@Serializable
private data object TimetableRoute

@Serializable
private data object GradesRoute

@Serializable
private data object SettingsRoute

@Serializable
private data object AppearanceRoute

@Serializable
private data object AcademicSettingsRoute

@Serializable
private data object RemindersSettingsRoute

@Serializable
private data object BackupSettingsRoute

private data class HomeTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniDeckApp(viewModel: UniDeckViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val safeNavigateBack: () -> Unit = {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        }
    }

    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val activeSemester by viewModel.activeSemester.collectAsStateWithLifecycle()
    val activeCourses by viewModel.activeCourses.collectAsStateWithLifecycle()
    val allCourses by viewModel.allCourses.collectAsStateWithLifecycle()
    val activeSchedules by viewModel.activeSchedules.collectAsStateWithLifecycle()
    val activeOneOffClasses by viewModel.activeOneOffClasses.collectAsStateWithLifecycle()
    val allLogs by viewModel.allAttendanceLogs.collectAsStateWithLifecycle()
    val savedProfessors by viewModel.savedProfessors.collectAsStateWithLifecycle()
    val isActiveSemesterLocked by viewModel.isActiveSemesterLocked.collectAsStateWithLifecycle()
    val selectedEpochDay by viewModel.selectedEpochDay.collectAsStateWithLifecycle()
    val whatIfGrades by viewModel.whatIfGrades.collectAsStateWithLifecycle()
    val pdfVersion by viewModel.pdfVersion.collectAsStateWithLifecycle()
    val isBackingUpNow by viewModel.isBackingUpNow.collectAsStateWithLifecycle()

    // 0.9s crisp M3 Expressive Launch Loader overlay
    var showLaunchLoader by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900L)
        showLaunchLoader = false
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                withDismissAction = event.actionLabel == null,
                duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && event.isUndoBulkMark) {
                viewModel.undoLastBulkMark()
            }
        }
    }

    // SAF Launchers for Offline JSON Export & Import
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var attendanceSubTab by rememberSaveable { mutableIntStateOf(preferences.defaultAttendanceTab) }
    LaunchedEffect(preferences.defaultAttendanceTab) {
        attendanceSubTab = preferences.defaultAttendanceTab
    }

    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddOneOffSheet by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var showNewSemesterDialog by remember { mutableStateOf(false) }

    if (showAddOneOffSheet && !isActiveSemesterLocked) {
        AddOneOffClassBottomSheet(
            selectedDate = LocalDate.ofEpochDay(selectedEpochDay),
            courses = activeCourses,
            savedProfessors = savedProfessors,
            use24Hour = preferences.use24HourFormat,
            onDismiss = { showAddOneOffSheet = false },
            onCreateCourseInstead = { showAddCourseDialog = true },
            onUpdateCourseProfessor = viewModel::updateCourseProfessorIfChanged,
            onAddClass = { courseId, startTime, endTime, room, componentType ->
                viewModel.addOneOffClass(
                    courseId = courseId,
                    dateEpochDay = selectedEpochDay,
                    startTime = startTime,
                    endTime = endTime,
                    roomNumber = room,
                    componentType = componentType
                )
            }
        )
    }

    if (!isActiveSemesterLocked && (showAddCourseDialog || editingCourse != null)) {
        CourseEditorDialog(
            initialCourse = editingCourse,
            semesterId = activeSemester?.id ?: 0L,
            savedProfessors = savedProfessors,
            onDismiss = {
                showAddCourseDialog = false
                editingCourse = null
            },
            onSave = { course -> viewModel.saveCourse(course) },
            onDelete = editingCourse?.let { existing ->
                { viewModel.deleteCourse(existing) }
            }
        )
    }

    if (showNewSemesterDialog) {
        SemesterEditorDialog(
            initial = null,
            defaultName = "Sem ${semesters.size + 1}",
            onDismiss = { showNewSemesterDialog = false },
            onSave = { name, start, end ->
                viewModel.addSemester(name, start, end)
                showNewSemesterDialog = false
            }
        )
    }

    val tabs = remember {
        listOf(
            HomeTabItem("Attendance", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "tab_attendance"),
            HomeTabItem("Scores", Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_scores"),
            HomeTabItem("Timetable", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_timetable")
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = HomeRoute
        ) {
            composable<HomeRoute> {
                val haptics = com.unideck.app.ui.components.rememberUniDeckHaptics()
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWideScreen = maxWidth >= 600.dp

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                title = { UniDeckWordmark() },
                                actions = {
                                    // Right-aligned borderless tonal Semester Switcher Pill
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    haptics.tick()
                                                    menuExpanded = true
                                                }
                                                .testTag("semester_switcher_pill")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = activeSemester?.name ?: "Sem 1",
                                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.ExpandMore,
                                                    contentDescription = "Switch Semester",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            semesters.forEach { sem ->
                                                val isArchived = UniDeckViewModel.isSemesterArchived(
                                                    sem,
                                                    preferences.completedSemesterIds
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = if (isArchived) "${sem.name} (Archived)" else sem.name,
                                                            fontWeight = if (sem.isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (sem.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        haptics.tick()
                                                        menuExpanded = false
                                                        viewModel.switchSemester(sem.id)
                                                    }
                                                )
                                            }
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = { Text("+ New Semester", color = MaterialTheme.colorScheme.primary) },
                                                onClick = {
                                                    haptics.tick()
                                                    menuExpanded = false
                                                    showNewSemesterDialog = true
                                                }
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            haptics.tick()
                                            navController.navigate(GradesRoute) {
                                                launchSingleTop = true
                                            }
                                        },
                                        modifier = Modifier.testTag("top_bar_grades_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.School,
                                            contentDescription = "Grades & SGPA"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            haptics.tick()
                                            navController.navigate(SettingsRoute) {
                                                launchSingleTop = true
                                            }
                                        },
                                        modifier = Modifier.testTag("top_bar_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        },
                        bottomBar = {
                            if (!isWideScreen) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    tabs.forEachIndexed { idx, item ->
                                        val selected = selectedTabIndex == idx
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                haptics.tick()
                                                selectedTabIndex = idx
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = item.label,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                            },
                                            alwaysShowLabel = false,
                                            colors = NavigationBarItemDefaults.colors(
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag(item.testTag)
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (!isActiveSemesterLocked && selectedTabIndex == 0 && attendanceSubTab == 0) {
                                FloatingActionButton(
                                    onClick = {
                                        haptics.tick()
                                        showAddOneOffSheet = true
                                    },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("add_one_off_class_fab")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add class"
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (isWideScreen) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    tabs.forEachIndexed { idx, item ->
                                        val selected = selectedTabIndex == idx
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = {
                                                haptics.tick()
                                                selectedTabIndex = idx
                                            },
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                    contentDescription = item.label
                                                )
                                            },
                                            label = { Text(item.label) },
                                            alwaysShowLabel = false,
                                            modifier = Modifier.testTag(item.testTag)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .widthIn(max = 720.dp)
                                ) {
                                    if (isActiveSemesterLocked) {
                                        ArchivedSemesterBanner(
                                            onUnlock = viewModel::unlockActiveArchivedSemester,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        when (selectedTabIndex) {
                                            0 -> AttendanceScreen(
                                                activeSemester = activeSemester,
                                                courses = activeCourses,
                                                schedules = activeSchedules,
                                                oneOffClasses = activeOneOffClasses,
                                                allLogs = allLogs,
                                                selectedEpochDay = selectedEpochDay,
                                                selectedSubTab = attendanceSubTab,
                                                preferences = preferences,
                                                isReadOnly = isActiveSemesterLocked,
                                                onSelectSubTab = { attendanceSubTab = it },
                                                onSelectDate = viewModel::selectDate,
                                                onJumpToToday = viewModel::jumpToToday,
                                                onMarkScheduledClass = viewModel::markScheduledClass,
                                                onMarkOneOffClass = viewModel::markOneOffClass,
                                                onDeleteOneOffClass = viewModel::deleteOneOffClass,
                                                onBulkMarkDay = viewModel::bulkMarkSelectedDay,
                                                onLogManualAttendance = { course, status, comp ->
                                                    viewModel.logManualAttendance(course, status, comp)
                                                },
                                                onOpenCourseDetail = { courseId ->
                                                    navController.navigate(SubjectDetailRoute(courseId)) {
                                                        launchSingleTop = true
                                                    }
                                                },
                                                onOpenAddCourse = { showAddCourseDialog = true },
                                                onEditCourse = { editingCourse = it },
                                                onOpenTimetable = { selectedTabIndex = 2 },
                                                onImportBackup = {
                                                    importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                                },
                                                onUpdateNotes = viewModel::setAcademicNotes,
                                                onUpdateTasks = viewModel::setAcademicTasks,
                                                onUpdateMilestones = viewModel::setAcademicMilestones
                                            )

                                            1 -> ScoresScreen(
                                                courses = activeCourses,
                                                preferences = preferences,
                                                isReadOnly = isActiveSemesterLocked,
                                                onUpdateCourse = viewModel::saveCourse,
                                                onOpenAddCourse = { showAddCourseDialog = true },
                                                onOpenGrades = {
                                                    navController.navigate(GradesRoute) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )

                                            2 -> {
                                                val pdfFile = remember(activeSemester, pdfVersion) {
                                                    viewModel.getActiveTimetablePdfFile()
                                                }
                                                TimetableScreen(
                                                    semesterId = activeSemester?.id ?: 0L,
                                                    semesterName = activeSemester?.name ?: "Sem 1",
                                                    courses = activeCourses,
                                                    schedules = activeSchedules,
                                                    savedProfessors = savedProfessors,
                                                    preferences = preferences,
                                                    pdfFile = pdfFile,
                                                    pdfVersion = pdfVersion,
                                                    isReadOnly = isActiveSemesterLocked,
                                                    isEmbeddedInHome = true,
                                                    onUnlockArchived = viewModel::unlockActiveArchivedSemester,
                                                    onBack = { selectedTabIndex = 0 },
                                                    onSaveSchedule = viewModel::saveSchedule,
                                                    onDeleteSchedule = viewModel::deleteSchedule,
                                                    onUpdateCourseProfessor = viewModel::updateCourseProfessorIfChanged,
                                                    onImportPdf = viewModel::importTimetablePdf,
                                                    onRemovePdf = viewModel::removeTimetablePdf
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

            composable<GradesRoute> {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        GradesScreen(
                            semesters = semesters,
                            activeSemester = activeSemester,
                            activeCourses = activeCourses,
                            allCourses = allCourses,
                            allLogs = allLogs,
                            preferences = preferences,
                            whatIfGrades = whatIfGrades,
                            isReadOnly = isActiveSemesterLocked,
                            onSetWhatIfGrade = viewModel::setWhatIfGrade,
                            onResetWhatIf = viewModel::resetWhatIf,
                            onSwitchSemester = viewModel::switchSemester,
                            onOpenAddCourse = { showAddCourseDialog = true },
                            onBack = safeNavigateBack,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp)
                        )
                    }
                }
            }

            composable<SubjectDetailRoute> { entry ->
                val route = entry.toRoute<SubjectDetailRoute>()
                val course = allCourses.firstOrNull { it.id == route.courseId }
                SubjectDetailScreen(
                    course = course,
                    schedules = activeSchedules,
                    logs = allLogs,
                    preferences = preferences,
                    isReadOnly = isActiveSemesterLocked,
                    onUnlockArchived = viewModel::unlockActiveArchivedSemester,
                    onBack = safeNavigateBack,
                    onEditCourse = { editingCourse = it },
                    onOpenAnalytics = { cid ->
                        navController.navigate(AttendanceAnalyticsRoute(cid)) {
                            launchSingleTop = true
                        }
                    },
                    onLogManualAttendance = { c, status, comp ->
                        viewModel.logManualAttendance(c, status, comp)
                    },
                    onUpdateLogStatus = viewModel::updateAttendanceLogStatus,
                    onDeleteLog = viewModel::deleteAttendanceLog
                )
            }

            composable<AttendanceAnalyticsRoute> { entry ->
                val route = entry.toRoute<AttendanceAnalyticsRoute>()
                val course = allCourses.firstOrNull { it.id == route.courseId }
                AttendanceAnalyticsScreen(
                    course = course,
                    logs = allLogs,
                    preferences = preferences,
                    onBack = safeNavigateBack
                )
            }

            composable<TimetableRoute> {
                val pdfFile = remember(activeSemester, pdfVersion) {
                    viewModel.getActiveTimetablePdfFile()
                }
                TimetableScreen(
                    semesterId = activeSemester?.id ?: 0L,
                    semesterName = activeSemester?.name ?: "Sem 1",
                    courses = activeCourses,
                    schedules = activeSchedules,
                    savedProfessors = savedProfessors,
                    preferences = preferences,
                    pdfFile = pdfFile,
                    pdfVersion = pdfVersion,
                    isReadOnly = isActiveSemesterLocked,
                    onUnlockArchived = viewModel::unlockActiveArchivedSemester,
                    onBack = safeNavigateBack,
                    onSaveSchedule = viewModel::saveSchedule,
                    onDeleteSchedule = viewModel::deleteSchedule,
                    onUpdateCourseProfessor = viewModel::updateCourseProfessorIfChanged,
                    onImportPdf = viewModel::importTimetablePdf,
                    onRemovePdf = viewModel::removeTimetablePdf
                )
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = safeNavigateBack,
                    onOpenAppearance = {
                        navController.navigate(AppearanceRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenAcademic = {
                        navController.navigate(AcademicSettingsRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenReminders = {
                        navController.navigate(RemindersSettingsRoute) {
                            launchSingleTop = true
                        }
                    },
                    onOpenBackup = {
                        navController.navigate(BackupSettingsRoute) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable<AppearanceRoute> {
                AppearanceScreen(
                    preferences = preferences,
                    onBack = safeNavigateBack,
                    onSetThemeMode = viewModel::setThemeMode,
                    onSetPitchBlack = viewModel::setPitchBlack,
                    onSetDynamicMonet = viewModel::setDynamicMonet,
                    onSetAccentKey = viewModel::setAccentKey,
                    onSetCutCorners = viewModel::setCutCorners,
                    onSetPaletteStyle = viewModel::setPaletteStyle,
                    onSelectAppIcon = viewModel::selectAppIcon,
                    onSetUseSystemFont = viewModel::setUseSystemFont,
                    onSetDefaultAttendanceTab = viewModel::setDefaultAttendanceTab
                )
            }

            composable<AcademicSettingsRoute> {
                AcademicSettingsScreen(
                    preferences = preferences,
                    semesters = semesters,
                    onBack = safeNavigateBack,
                    onSetMinAttendance = viewModel::setMinAttendanceThreshold,
                    onSetGradeCutoffs = viewModel::setGradeCutoffs,
                    onAddSemester = viewModel::addSemester,
                    onUpdateSemester = viewModel::updateSemester,
                    onSwitchSemester = viewModel::switchSemester,
                    onToggleSemesterCompleted = viewModel::toggleSemesterCompleted,
                    onDeleteSemester = viewModel::deleteSemester
                )
            }

            composable<RemindersSettingsRoute> {
                RemindersSettingsScreen(
                    preferences = preferences,
                    onBack = safeNavigateBack,
                    onSetReminderLeadMinutes = viewModel::setReminderLeadMinutes,
                    onSetUse24Hour = viewModel::setUse24HourFormat,
                    onSetLiveClassNotificationEnabled = viewModel::setLiveClassNotificationEnabled
                )
            }

            composable<BackupSettingsRoute> {
                BackupSettingsScreen(
                    preferences = preferences,
                    isBackingUpNow = isBackingUpNow,
                    onBack = safeNavigateBack,
                    onSetAutoBackupEnabled = viewModel::setAutoBackupEnabled,
                    onAutoBackupFolderSelected = viewModel::onAutoBackupFolderSelected,
                    onRunAutoBackupNow = viewModel::runAutoBackupNow,
                    onExportJson = { exportJsonLauncher.launch("unideck_backup.json") },
                    onImportJson = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }
        }

        // 0.9s Material 3 Expressive Launch Loader Overlay
        LaunchLoaderOverlay(visible = showLaunchLoader)
    }
}
