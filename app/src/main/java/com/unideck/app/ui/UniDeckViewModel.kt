package com.unideck.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unideck.app.UniDeckApplication
import com.unideck.app.backup.AutoBackupScheduler
import com.unideck.app.backup.BackupManager
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.AttendanceStatus
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.OneOffClassEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.prefs.GradeCutoffs
import com.unideck.app.data.prefs.ThemeMode
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.data.repository.BulkAttendanceSnapshot
import com.unideck.app.domain.AcademicCalculator
import com.unideck.app.domain.GradeLetter
import com.unideck.app.notifications.ReminderScheduler
import com.unideck.app.system.AppIconManager
import com.unideck.app.widget.WidgetUpdater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

data class UiSnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val isUndoBulkMark: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class UniDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as UniDeckApplication
    private val repository = app.repository
    private val prefsRepo = app.preferencesRepository

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences()
    )

    val semesters: StateFlow<List<SemesterEntity>> = repository.semesters.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeSemester: StateFlow<SemesterEntity?> = semesters.map { list ->
        list.firstOrNull { it.isCurrent } ?: list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val activeCourses: StateFlow<List<CourseEntity>> = activeSemester.flatMapLatest { sem ->
        if (sem == null) flowOf(emptyList()) else repository.observeCoursesBySemester(sem.id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allCourses: StateFlow<List<CourseEntity>> = repository.allCourses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeSchedules: StateFlow<List<ClassScheduleEntity>> = activeSemester.flatMapLatest { sem ->
        if (sem == null) flowOf(emptyList()) else repository.observeSchedulesBySemester(sem.id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeOneOffClasses: StateFlow<List<OneOffClassEntity>> = activeSemester.flatMapLatest { sem ->
        if (sem == null) flowOf(emptyList()) else repository.observeOneOffClassesBySemester(sem.id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val allAttendanceLogs: StateFlow<List<AttendanceLogEntity>> = repository.allAttendanceLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val savedProfessors: StateFlow<List<String>> = repository.distinctProfessors
        .map { rawList ->
            val seen = LinkedHashMap<String, String>()
            rawList.forEach { entry ->
                val cleaned = entry.trim()
                if (cleaned.isNotEmpty()) {
                    val key = cleaned.lowercase()
                    if (!seen.containsKey(key)) {
                        seen[key] = cleaned
                    }
                }
            }
            seen.values.sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _unlockedArchivedSemesterIds = MutableStateFlow<Set<Long>>(emptySet())
    val unlockedArchivedSemesterIds: StateFlow<Set<Long>> = _unlockedArchivedSemesterIds.asStateFlow()

    val isActiveSemesterArchived: StateFlow<Boolean> = combine(
        activeSemester,
        preferences
    ) { sem, prefs ->
        if (sem == null) false
        else isSemesterArchived(sem, prefs.completedSemesterIds)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val isActiveSemesterLocked: StateFlow<Boolean> = combine(
        activeSemester,
        preferences,
        unlockedArchivedSemesterIds
    ) { sem, prefs, unlocked ->
        if (sem == null) false
        else isSemesterReadOnlyLocked(sem, prefs.completedSemesterIds) && sem.id !in unlocked
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    private val _selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    val selectedEpochDay: StateFlow<Long> = _selectedEpochDay.asStateFlow()

    private val _whatIfGrades = MutableStateFlow<Map<Long, GradeLetter>>(emptyMap())
    val whatIfGrades: StateFlow<Map<Long, GradeLetter>> = _whatIfGrades.asStateFlow()

    private val _pdfVersion = MutableStateFlow(0)
    val pdfVersion: StateFlow<Int> = _pdfVersion.asStateFlow()

    private val _isBackingUpNow = MutableStateFlow(false)
    val isBackingUpNow: StateFlow<Boolean> = _isBackingUpNow.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<UiSnackbarEvent>(extraBufferCapacity = 6)
    val snackbarEvents: SharedFlow<UiSnackbarEvent> = _snackbarEvents.asSharedFlow()

    private var lastBulkSnapshot: BulkAttendanceSnapshot? = null

    private fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(UiSnackbarEvent(message = message))
    }

    init {
        viewModelScope.launch {
            repository.ensureInitialSemester()
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
            val currentPrefs = prefsRepo.getPreferencesSync()
            if (currentPrefs.autoBackupEnabled && !currentPrefs.autoBackupFolderUri.isNullOrBlank()) {
                AutoBackupScheduler.scheduleDailyBackup(app)
            }
        }
    }

    fun selectDate(epochDay: Long) {
        _selectedEpochDay.value = epochDay
    }

    fun jumpToToday() {
        _selectedEpochDay.value = LocalDate.now().toEpochDay()
    }

    // --- Semester Operations ---
    fun switchSemester(semesterId: Long) {
        viewModelScope.launch {
            repository.switchSemester(semesterId)
            _whatIfGrades.value = emptyMap()
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
        }
    }

    fun addSemester(
        name: String,
        startDateEpochDay: Long? = null,
        endDateEpochDay: Long? = null
    ) {
        viewModelScope.launch {
            repository.addSemester(
                name = name,
                makeCurrent = true,
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay
            )
            _whatIfGrades.value = emptyMap()
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
            emitMessage("Added $name")
        }
    }

    fun updateSemester(semester: SemesterEntity) {
        viewModelScope.launch {
            repository.updateSemester(semester)
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
            emitMessage("Updated ${semester.name}")
        }
    }

    fun renameSemester(semester: SemesterEntity, newName: String) {
        viewModelScope.launch {
            repository.renameSemester(semester, newName)
            WidgetUpdater.refreshAll(app)
        }
    }

    fun deleteSemester(semester: SemesterEntity) {
        viewModelScope.launch {
            if (semesters.value.size <= 1) {
                emitMessage("At least one semester is required")
                return@launch
            }
            repository.deleteSemester(semester)
            _whatIfGrades.value = emptyMap()
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
            emitMessage("Deleted ${semester.name}")
        }
    }

    fun toggleSemesterCompleted(semesterId: Long) {
        viewModelScope.launch {
            val currentlyCompleted = semesterId in preferences.value.completedSemesterIds
            prefsRepo.setSemesterCompleted(semesterId, !currentlyCompleted)
            if (!currentlyCompleted) {
                _unlockedArchivedSemesterIds.value = _unlockedArchivedSemesterIds.value - semesterId
                emitMessage("Semester marked completed (read-only)")
            } else {
                emitMessage("Semester reopened for editing")
            }
        }
    }

    fun unlockActiveArchivedSemester() {
        val semId = activeSemester.value?.id ?: return
        _unlockedArchivedSemesterIds.value = _unlockedArchivedSemesterIds.value + semId
        emitMessage("Archived semester unlocked for this session")
    }

    // --- Course Operations ---
    fun saveCourse(course: CourseEntity) {
        viewModelScope.launch {
            val semId = if (course.semesterId == 0L) {
                activeSemester.value?.id ?: repository.ensureInitialSemester().id
            } else {
                course.semesterId
            }
            repository.saveCourse(course.copy(semesterId = semId))
            WidgetUpdater.refreshAll(app)
        }
    }

    fun updateCourseProfessorIfChanged(courseId: Long, professorName: String) {
        val trimmed = professorName.trim()
        val course = allCourses.value.firstOrNull { it.id == courseId } ?: return
        if (course.professorName.trim() != trimmed) {
            saveCourse(course.copy(professorName = trimmed))
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
            emitMessage("Removed ${course.code}")
        }
    }

    // --- Timetable Schedule Operations ---
    fun saveSchedule(schedule: ClassScheduleEntity) {
        viewModelScope.launch {
            val semId = if (schedule.semesterId == 0L) {
                activeSemester.value?.id ?: repository.ensureInitialSemester().id
            } else {
                schedule.semesterId
            }
            repository.saveSchedule(schedule.copy(semesterId = semId))
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
        }
    }

    fun deleteSchedule(schedule: ClassScheduleEntity) {
        viewModelScope.launch {
            repository.deleteSchedule(schedule)
            ReminderScheduler.scheduleNextReminder(app)
            WidgetUpdater.refreshAll(app)
        }
    }

    // --- Attendance Logging ---
    fun markScheduledClass(
        course: CourseEntity,
        dateEpochDay: Long,
        componentType: String,
        status: String
    ) {
        viewModelScope.launch {
            val threshold = preferences.value.minAttendanceThreshold
            val beforeLogs = allAttendanceLogs.value.filter { it.courseId == course.id }
            val beforeStats = AcademicCalculator.computeAttendanceStats(
                logs = beforeLogs,
                thresholdPercent = threshold,
                initialAttended = course.initialAttended,
                initialTotal = course.initialTotal
            )

            repository.markScheduledAttendance(course.id, dateEpochDay, componentType, status)
            WidgetUpdater.refreshAll(app)

            if (status == AttendanceStatus.ABSENT) {
                checkAndAlertLowAttendance(course, beforeStats.percentage, beforeStats.totalCounted, threshold)
            }
        }
    }

    fun logManualAttendance(
        course: CourseEntity,
        status: String,
        componentType: String,
        dateEpochDay: Long = _selectedEpochDay.value
    ) {
        viewModelScope.launch {
            val threshold = preferences.value.minAttendanceThreshold
            val beforeLogs = allAttendanceLogs.value.filter { it.courseId == course.id }
            val beforeStats = AcademicCalculator.computeAttendanceStats(
                logs = beforeLogs,
                thresholdPercent = threshold,
                initialAttended = course.initialAttended,
                initialTotal = course.initialTotal
            )

            repository.addManualAttendance(course.id, dateEpochDay, status, componentType)
            WidgetUpdater.refreshAll(app)
            emitMessage("${course.code} marked ${status.lowercase().replaceFirstChar { it.uppercase() }}")

            if (status == AttendanceStatus.ABSENT) {
                checkAndAlertLowAttendance(course, beforeStats.percentage, beforeStats.totalCounted, threshold)
            }
        }
    }

    fun updateAttendanceLogStatus(logId: Long, newStatus: String) {
        viewModelScope.launch {
            repository.updateAttendanceLogStatus(logId, newStatus)
            WidgetUpdater.refreshAll(app)
            emitMessage("Log updated to ${newStatus.lowercase().replaceFirstChar { it.uppercase() }}")
        }
    }

    fun deleteAttendanceLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteAttendanceLog(logId)
            WidgetUpdater.refreshAll(app)
            emitMessage("Log entry deleted")
        }
    }

    fun bulkMarkSelectedDay(targetStatus: String) {
        viewModelScope.launch {
            val semId = activeSemester.value?.id ?: return@launch
            val epochDay = _selectedEpochDay.value
            val snapshot = repository.bulkMarkDayAttendance(
                semesterId = semId,
                dateEpochDay = epochDay,
                targetStatus = targetStatus
            )
            if (snapshot.affectedCount > 0) {
                lastBulkSnapshot = snapshot
                WidgetUpdater.refreshAll(app)
                val msg = if (targetStatus == AttendanceStatus.PRESENT) {
                    "Marked ${snapshot.affectedCount} ${if (snapshot.affectedCount == 1) "class" else "classes"} Present"
                } else {
                    "Marked ${snapshot.affectedCount} ${if (snapshot.affectedCount == 1) "class" else "classes"} as Holiday"
                }
                _snackbarEvents.tryEmit(
                    UiSnackbarEvent(
                        message = msg,
                        actionLabel = "Undo",
                        isUndoBulkMark = true
                    )
                )
            }
        }
    }

    fun undoLastBulkMark() {
        val snapshot = lastBulkSnapshot ?: return
        lastBulkSnapshot = null
        viewModelScope.launch {
            repository.restoreBulkAttendanceSnapshot(snapshot)
            WidgetUpdater.refreshAll(app)
            emitMessage("Restored previous attendance")
        }
    }

    fun addOneOffClass(
        courseId: Long,
        dateEpochDay: Long,
        startTime: Int,
        endTime: Int,
        roomNumber: String,
        componentType: String
    ) {
        viewModelScope.launch {
            val semId = activeSemester.value?.id ?: repository.ensureInitialSemester().id
            repository.addOneOffClass(
                OneOffClassEntity(
                    courseId = courseId,
                    semesterId = semId,
                    dateEpochDay = dateEpochDay,
                    startTime = startTime,
                    endTime = endTime.coerceAtLeast(startTime + 15),
                    roomNumber = roomNumber.trim(),
                    componentType = componentType
                )
            )
            WidgetUpdater.refreshAll(app)
            emitMessage("Extra class added for this date only")
        }
    }

    fun deleteOneOffClass(item: OneOffClassEntity) {
        viewModelScope.launch {
            repository.deleteOneOffClass(item)
            WidgetUpdater.refreshAll(app)
            emitMessage("Extra class removed")
        }
    }

    fun markOneOffClass(
        course: CourseEntity,
        oneOff: OneOffClassEntity,
        status: String
    ) {
        viewModelScope.launch {
            val threshold = preferences.value.minAttendanceThreshold
            val beforeLogs = allAttendanceLogs.value.filter { it.courseId == course.id }
            val beforeStats = AcademicCalculator.computeAttendanceStats(
                logs = beforeLogs,
                thresholdPercent = threshold,
                initialAttended = course.initialAttended,
                initialTotal = course.initialTotal
            )

            repository.markOneOffClassAttendance(oneOff, status)
            WidgetUpdater.refreshAll(app)

            if (status == AttendanceStatus.ABSENT) {
                checkAndAlertLowAttendance(course, beforeStats.percentage, beforeStats.totalCounted, threshold)
            }
        }
    }

    private suspend fun checkAndAlertLowAttendance(
        course: CourseEntity,
        previousPercent: Double,
        previousTotal: Int,
        threshold: Int
    ) {
        val updatedLogs = app.database.uniDeckDao().getAllAttendanceLogsSync().filter { it.courseId == course.id }
        val afterStats = AcademicCalculator.computeAttendanceStats(
            logs = updatedLogs,
            thresholdPercent = threshold,
            initialAttended = course.initialAttended,
            initialTotal = course.initialTotal
        )
        val crossedDown = (previousTotal == 0 || previousPercent >= threshold) &&
            afterStats.totalCounted > 0 && afterStats.percentage < threshold
        if (crossedDown) {
            ReminderScheduler.postLowAttendanceAlert(
                context = app,
                courseCode = course.code,
                courseName = course.name,
                currentPercent = afterStats.percentage,
                threshold = threshold,
                classesNeeded = afterStats.classesNeeded
            )
        }
    }

    // --- What-If GPA Simulator ---
    fun setWhatIfGrade(courseId: Long, grade: GradeLetter?) {
        val current = _whatIfGrades.value.toMutableMap()
        if (grade == null) current.remove(courseId) else current[courseId] = grade
        _whatIfGrades.value = current
    }

    fun resetWhatIf() {
        _whatIfGrades.value = emptyMap()
    }

    // --- Timetable PDF ---
    fun getActiveTimetablePdfFile(): File? {
        val semId = activeSemester.value?.id ?: return null
        val file = repository.getTimetablePdfFile(semId)
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun importTimetablePdf(uri: Uri) {
        viewModelScope.launch {
            val semId = activeSemester.value?.id ?: repository.ensureInitialSemester().id
            repository.importTimetablePdf(semId, uri)
                .onSuccess {
                    _pdfVersion.value += 1
                    emitMessage("Timetable PDF saved offline")
                }
                .onFailure {
                    emitMessage("Failed to import PDF: ${it.localizedMessage ?: "Unknown error"}")
                }
        }
    }

    fun removeTimetablePdf() {
        viewModelScope.launch {
            val semId = activeSemester.value?.id ?: return@launch
            repository.removeTimetablePdf(semId)
            _pdfVersion.value += 1
            emitMessage("Timetable PDF removed")
        }
    }

    // --- Preferences ---
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        runCatching {
            prefsRepo.setThemeMode(mode)
            WidgetUpdater.refreshAll(app)
        }
    }
    fun setPitchBlack(enabled: Boolean) = viewModelScope.launch {
        runCatching {
            prefsRepo.setPitchBlack(enabled)
            WidgetUpdater.refreshAll(app)
        }
    }
    fun setDynamicMonet(enabled: Boolean) = viewModelScope.launch {
        runCatching {
            prefsRepo.setDynamicMonet(enabled)
            WidgetUpdater.refreshAll(app)
        }
    }
    fun setAccentKey(key: String) = viewModelScope.launch {
        runCatching {
            prefsRepo.setAccentKey(key, disableDynamicMonet = true)
            WidgetUpdater.refreshAll(app)
        }
    }
    fun setPaletteIndex(index: Int) = viewModelScope.launch {
        runCatching {
            prefsRepo.setPaletteIndex(index)
            WidgetUpdater.refreshAll(app)
        }
    }
    fun setCutCorners(enabled: Boolean) = viewModelScope.launch {
        runCatching { prefsRepo.setCutCorners(enabled) }
    }
    fun setPaletteStyle(style: String) = viewModelScope.launch {
        runCatching {
            prefsRepo.setPaletteStyle(style)
            WidgetUpdater.refreshAll(app)
        }
    }
    fun setUseSystemFont(useSystem: Boolean) = viewModelScope.launch {
        runCatching { prefsRepo.setUseSystemFont(useSystem) }
    }
    fun setDefaultAttendanceTab(tabIndex: Int) = viewModelScope.launch {
        runCatching { prefsRepo.setDefaultAttendanceTab(tabIndex) }
    }
    fun setAcademicNotes(notes: List<com.unideck.app.data.prefs.AcademicNoteItem>) = viewModelScope.launch {
        prefsRepo.setAcademicNotes(notes)
    }
    fun setAcademicTasks(tasks: List<com.unideck.app.data.prefs.AcademicTaskItem>) = viewModelScope.launch {
        prefsRepo.setAcademicTasks(tasks)
    }
    fun setAcademicMilestones(milestones: List<com.unideck.app.data.prefs.AcademicMilestoneItem>) = viewModelScope.launch {
        prefsRepo.setAcademicMilestones(milestones)
    }
    fun setMinAttendanceThreshold(threshold: Int) = viewModelScope.launch {
        prefsRepo.setMinAttendanceThreshold(threshold)
        WidgetUpdater.refreshAll(app)
    }
    fun setUse24HourFormat(use24Hour: Boolean) = viewModelScope.launch {
        prefsRepo.setUse24HourFormat(use24Hour)
        ReminderScheduler.scheduleNextReminder(app)
        WidgetUpdater.refreshAll(app)
    }
    fun setGradeCutoffs(cutoffs: GradeCutoffs) = viewModelScope.launch {
        prefsRepo.setGradeCutoffs(cutoffs)
        WidgetUpdater.refreshAll(app)
        emitMessage("Grading scale updated")
    }
    fun setReminderLeadMinutes(minutes: Int) = viewModelScope.launch {
        prefsRepo.setReminderLeadMinutes(minutes)
        ReminderScheduler.scheduleNextReminder(app)
        val label = if (minutes == 0) "Class reminders turned off" else "Reminders set to $minutes min before class"
        emitMessage(label)
    }
    fun setLiveClassNotificationEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setLiveClassNotificationEnabled(enabled)
        ReminderScheduler.updateLiveOngoingClassNotification(app)
    }
    fun selectAppIcon(iconKey: String) = viewModelScope.launch {
        AppIconManager.switchIcon(app, iconKey)
        prefsRepo.setActiveIconKey(iconKey)
        emitMessage("App icon updated (launcher may refresh shortly)")
    }

    // --- JSON Backup Export / Import ---
    fun exportBackup(uri: Uri) = viewModelScope.launch {
        repository.exportBackupToUri(uri)
            .onSuccess { emitMessage("Offline backup exported successfully") }
            .onFailure { emitMessage("Export failed: ${it.localizedMessage ?: "Error"}") }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        repository.importBackupFromUri(uri)
            .onSuccess { courseCount ->
                _whatIfGrades.value = emptyMap()
                ReminderScheduler.scheduleNextReminder(app)
                WidgetUpdater.refreshAll(app)
                emitMessage("Restored backup ($courseCount courses)")
            }
            .onFailure { emitMessage("Invalid backup file: ${it.localizedMessage ?: "Error"}") }
    }

    // --- Automatic Daily Local Backup (SAF + WorkManager) ---
    fun onAutoBackupFolderSelected(treeUri: Uri) = viewModelScope.launch {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val persisted = runCatching {
            app.contentResolver.takePersistableUriPermission(treeUri, flags)
        }.isSuccess
        if (!persisted) {
            emitMessage("Could not persist access to selected folder")
            return@launch
        }
        val displayName = BackupManager.resolveFolderDisplayName(app, treeUri)
        prefsRepo.setAutoBackupFolder(
            uriString = treeUri.toString(),
            displayName = displayName,
            enable = true
        )
        AutoBackupScheduler.scheduleDailyBackup(app)
        emitMessage("Auto daily backup enabled in \"$displayName\"")
    }

    fun setAutoBackupEnabled(enabled: Boolean) = viewModelScope.launch {
        if (!enabled) {
            prefsRepo.setAutoBackupEnabled(false)
            AutoBackupScheduler.cancelDailyBackup(app)
            emitMessage("Auto daily backup turned off")
            return@launch
        }
        val uriStr = preferences.value.autoBackupFolderUri
        val treeUri = uriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (treeUri == null || !app.backupManager.hasPersistedWritePermission(treeUri)) {
            prefsRepo.setAutoBackupEnabled(false)
            AutoBackupScheduler.cancelDailyBackup(app)
            emitMessage("Please choose a local backup folder first")
            return@launch
        }
        prefsRepo.setAutoBackupEnabled(true)
        AutoBackupScheduler.scheduleDailyBackup(app)
        emitMessage("Auto daily backup scheduled every 24h")
    }

    fun runAutoBackupNow() = viewModelScope.launch {
        if (_isBackingUpNow.value) return@launch
        val uriStr = preferences.value.autoBackupFolderUri
        val treeUri = uriStr?.let { runCatching { Uri.parse(it) }.getOrNull() }
        if (treeUri == null) {
            emitMessage("Choose a local backup folder first")
            return@launch
        }
        _isBackingUpNow.value = true
        try {
            repository.performDailyLocalBackup(treeUri)
                .onSuccess { fileName ->
                    emitMessage("Saved $fileName")
                }
                .onFailure { err ->
                    emitMessage("Backup failed: ${err.localizedMessage ?: "Error"}")
                }
        } finally {
            _isBackingUpNow.value = false
        }
    }

    companion object {
        fun isSemesterArchived(
            semester: SemesterEntity,
            completedSemesterIds: Set<Long>,
            todayEpochDay: Long = LocalDate.now().toEpochDay()
        ): Boolean {
            val endedInPast = semester.endDateEpochDay != null && semester.endDateEpochDay < todayEpochDay
            return endedInPast || semester.id in completedSemesterIds
        }

        /**
         * Semesters within the current academic/calendar year always remain directly editable.
         * Read-only archival locking only applies to older, previous-year semesters.
         */
        fun isSemesterReadOnlyLocked(
            semester: SemesterEntity,
            completedSemesterIds: Set<Long>,
            todayEpochDay: Long = LocalDate.now().toEpochDay()
        ): Boolean {
            val today = LocalDate.ofEpochDay(todayEpochDay)
            val currentAcademicYearStart = LocalDate.of(
                if (today.monthValue >= 7) today.year else today.year - 1,
                7,
                1
            )
            val endDate = semester.endDateEpochDay?.let { LocalDate.ofEpochDay(it) }
            val startDate = semester.startDateEpochDay?.let { LocalDate.ofEpochDay(it) }

            val isPreviousYearSemester = when {
                endDate != null -> endDate.year < today.year && endDate.isBefore(currentAcademicYearStart)
                startDate != null -> startDate.year < today.year && startDate.isBefore(currentAcademicYearStart)
                else -> false
            }

            if (!isPreviousYearSemester) return false
            return isSemesterArchived(semester, completedSemesterIds, todayEpochDay)
        }
    }
}
