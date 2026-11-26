package com.unideck.app.data.repository

import android.content.Context
import android.net.Uri
import com.unideck.app.backup.BackupManager
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.OneOffClassEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.local.UniDeckDao
import com.unideck.app.data.local.isDateWithinSemester
import com.unideck.app.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

data class ScheduledLogUndoEntry(
    val courseId: Long,
    val dateEpochDay: Long,
    val componentType: String,
    val previousLog: AttendanceLogEntity?,
    val createdLogId: Long?
)

data class OneOffLogUndoEntry(
    val oneOffId: Long,
    val previousLog: AttendanceLogEntity?,
    val createdLogId: Long?
)

data class BulkAttendanceSnapshot(
    val scheduledEntries: List<ScheduledLogUndoEntry>,
    val oneOffEntries: List<OneOffLogUndoEntry>,
    val affectedCount: Int
)

class UniDeckRepository(
    private val context: Context,
    private val dao: UniDeckDao,
    val preferences: UserPreferencesRepository,
    val backupManager: BackupManager = BackupManager(context, dao, preferences)
) {

    val semesters: Flow<List<SemesterEntity>> = dao.observeSemesters()
    val allCourses: Flow<List<CourseEntity>> = dao.observeAllCourses()
    val allAttendanceLogs: Flow<List<AttendanceLogEntity>> = dao.observeAllAttendanceLogs()
    val distinctProfessors: Flow<List<String>> = dao.observeDistinctProfessors()

    fun observeCoursesBySemester(semesterId: Long): Flow<List<CourseEntity>> =
        dao.observeCoursesBySemester(semesterId)

    fun observeSchedulesBySemester(semesterId: Long): Flow<List<ClassScheduleEntity>> =
        dao.observeSchedulesBySemester(semesterId)

    fun observeOneOffClassesBySemester(semesterId: Long): Flow<List<OneOffClassEntity>> =
        dao.observeOneOffClassesBySemester(semesterId)

    fun observeCourse(courseId: Long): Flow<CourseEntity?> = dao.observeCourse(courseId)

    /**
     * Ensures clean first-launch state with a single empty Semester 1 (zero mock courses).
     */
    suspend fun ensureInitialSemester(): SemesterEntity {
        val existing = dao.getAllSemestersSync()
        if (existing.isEmpty()) {
            val id = dao.insertSemester(SemesterEntity(name = "Sem 1", isCurrent = true))
            return SemesterEntity(id = id, name = "Sem 1", isCurrent = true)
        }
        val current = existing.firstOrNull { it.isCurrent }
        if (current != null) return current
        val fallback = existing.first()
        dao.setActiveSemester(fallback.id)
        return fallback.copy(isCurrent = true)
    }

    suspend fun addSemester(
        name: String,
        makeCurrent: Boolean = true,
        startDateEpochDay: Long? = null,
        endDateEpochDay: Long? = null
    ): Long {
        val trimmed = name.trim().ifEmpty { "Semester" }
        val id = dao.insertSemester(
            SemesterEntity(
                name = trimmed,
                isCurrent = false,
                startDateEpochDay = startDateEpochDay,
                endDateEpochDay = endDateEpochDay
            )
        )
        if (makeCurrent) {
            dao.setActiveSemester(id)
        }
        return id
    }

    suspend fun updateSemester(semester: SemesterEntity) {
        val trimmed = semester.name.trim()
        if (trimmed.isNotEmpty()) {
            dao.updateSemester(semester.copy(name = trimmed))
        }
    }

    suspend fun renameSemester(semester: SemesterEntity, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            dao.updateSemester(semester.copy(name = trimmed))
        }
    }

    suspend fun switchSemester(semesterId: Long) {
        dao.setActiveSemester(semesterId)
    }

    suspend fun deleteSemester(semester: SemesterEntity) {
        val all = dao.getAllSemestersSync()
        if (all.size <= 1) return // Keep at least one semester
        dao.deleteSemester(semester)
        getTimetablePdfFile(semester.id).delete()
        val remaining = dao.getAllSemestersSync()
        if (remaining.none { it.isCurrent } && remaining.isNotEmpty()) {
            dao.setActiveSemester(remaining.first().id)
        }
    }

    suspend fun saveCourse(course: CourseEntity): Long {
        return if (course.id == 0L) {
            dao.insertCourse(course)
        } else {
            dao.updateCourse(course)
            course.id
        }
    }

    suspend fun deleteCourse(course: CourseEntity) {
        dao.deleteCourse(course)
    }

    suspend fun saveSchedule(schedule: ClassScheduleEntity): Long {
        return if (schedule.id == 0L) {
            dao.insertSchedule(schedule)
        } else {
            dao.updateSchedule(schedule)
            schedule.id
        }
    }

    suspend fun deleteSchedule(schedule: ClassScheduleEntity) {
        dao.deleteSchedule(schedule)
    }

    suspend fun markScheduledAttendance(
        courseId: Long,
        dateEpochDay: Long,
        componentType: String,
        status: String
    ) {
        val existing = dao.findScheduledLog(courseId, dateEpochDay, componentType)
        if (existing != null) {
            if (existing.status == status) {
                // Tapping active status toggles it off cleanly
                dao.deleteAttendanceLog(existing)
            } else {
                dao.updateAttendanceLog(existing.copy(status = status))
            }
        } else {
            dao.insertAttendanceLog(
                AttendanceLogEntity(
                    courseId = courseId,
                    dateEpochDay = dateEpochDay,
                    status = status,
                    componentType = componentType,
                    isManualEntry = false
                )
            )
        }
    }

    suspend fun addManualAttendance(
        courseId: Long,
        dateEpochDay: Long,
        status: String,
        componentType: String
    ): Long {
        return dao.insertAttendanceLog(
            AttendanceLogEntity(
                courseId = courseId,
                dateEpochDay = dateEpochDay,
                status = status,
                componentType = componentType,
                isManualEntry = true
            )
        )
    }

    suspend fun updateAttendanceLogStatus(logId: Long, newStatus: String) {
        val existing = dao.getAttendanceLogById(logId) ?: return
        if (existing.status != newStatus) {
            dao.updateAttendanceLog(existing.copy(status = newStatus))
        }
    }

    suspend fun deleteAttendanceLog(logId: Long) {
        dao.deleteAttendanceLogById(logId)
    }

    suspend fun addOneOffClass(item: OneOffClassEntity): Long = dao.insertOneOffClass(item)

    suspend fun deleteOneOffClass(item: OneOffClassEntity) {
        item.attendanceLogId?.let { dao.deleteAttendanceLogById(it) }
        dao.deleteOneOffClass(item)
    }

    suspend fun markOneOffClassAttendance(oneOff: OneOffClassEntity, status: String) {
        val current = dao.getOneOffClassById(oneOff.id) ?: oneOff
        val existingLogId = current.attendanceLogId
        if (existingLogId != null) {
            val existingLog = dao.getAttendanceLogById(existingLogId)
            if (existingLog != null) {
                if (existingLog.status == status) {
                    dao.deleteAttendanceLog(existingLog)
                    dao.updateOneOffClass(current.copy(attendanceLogId = null))
                } else {
                    dao.updateAttendanceLog(existingLog.copy(status = status))
                }
                return
            }
        }
        val newLogId = dao.insertAttendanceLog(
            AttendanceLogEntity(
                courseId = current.courseId,
                dateEpochDay = current.dateEpochDay,
                status = status,
                componentType = current.componentType,
                isManualEntry = true
            )
        )
        dao.updateOneOffClass(current.copy(attendanceLogId = newLogId))
    }

    suspend fun bulkMarkDayAttendance(
        semesterId: Long,
        dateEpochDay: Long,
        targetStatus: String
    ): BulkAttendanceSnapshot {
        val date = LocalDate.ofEpochDay(dateEpochDay)
        val semester = dao.getAllSemestersSync().firstOrNull { it.id == semesterId }
        val withinSemester = semester?.isDateWithinSemester(dateEpochDay) ?: true
        val daySchedules = if (withinSemester) {
            dao.getSchedulesBySemesterSync(semesterId)
                .filter { it.dayOfWeek == date.dayOfWeek.value }
                .distinctBy { Triple(it.courseId, it.componentType, dateEpochDay) }
        } else {
            emptyList()
        }
        val dayOneOffs = dao.getAllOneOffClassesSync()
            .filter { it.semesterId == semesterId && it.dateEpochDay == dateEpochDay }

        val scheduledUndo = mutableListOf<ScheduledLogUndoEntry>()
        for (slot in daySchedules) {
            val existing = dao.findScheduledLog(slot.courseId, dateEpochDay, slot.componentType)
            if (existing != null) {
                scheduledUndo.add(
                    ScheduledLogUndoEntry(
                        courseId = slot.courseId,
                        dateEpochDay = dateEpochDay,
                        componentType = slot.componentType,
                        previousLog = existing,
                        createdLogId = null
                    )
                )
                if (existing.status != targetStatus) {
                    dao.updateAttendanceLog(existing.copy(status = targetStatus))
                }
            } else {
                val createdId = dao.insertAttendanceLog(
                    AttendanceLogEntity(
                        courseId = slot.courseId,
                        dateEpochDay = dateEpochDay,
                        status = targetStatus,
                        componentType = slot.componentType,
                        isManualEntry = false
                    )
                )
                scheduledUndo.add(
                    ScheduledLogUndoEntry(
                        courseId = slot.courseId,
                        dateEpochDay = dateEpochDay,
                        componentType = slot.componentType,
                        previousLog = null,
                        createdLogId = createdId
                    )
                )
            }
        }

        val oneOffUndo = mutableListOf<OneOffLogUndoEntry>()
        for (oneOff in dayOneOffs) {
            val existingLog = oneOff.attendanceLogId?.let { dao.getAttendanceLogById(it) }
            if (existingLog != null) {
                oneOffUndo.add(
                    OneOffLogUndoEntry(
                        oneOffId = oneOff.id,
                        previousLog = existingLog,
                        createdLogId = null
                    )
                )
                if (existingLog.status != targetStatus) {
                    dao.updateAttendanceLog(existingLog.copy(status = targetStatus))
                }
            } else {
                val createdId = dao.insertAttendanceLog(
                    AttendanceLogEntity(
                        courseId = oneOff.courseId,
                        dateEpochDay = oneOff.dateEpochDay,
                        status = targetStatus,
                        componentType = oneOff.componentType,
                        isManualEntry = true
                    )
                )
                dao.updateOneOffClass(oneOff.copy(attendanceLogId = createdId))
                oneOffUndo.add(
                    OneOffLogUndoEntry(
                        oneOffId = oneOff.id,
                        previousLog = null,
                        createdLogId = createdId
                    )
                )
            }
        }

        return BulkAttendanceSnapshot(
            scheduledEntries = scheduledUndo,
            oneOffEntries = oneOffUndo,
            affectedCount = scheduledUndo.size + oneOffUndo.size
        )
    }

    suspend fun restoreBulkAttendanceSnapshot(snapshot: BulkAttendanceSnapshot) {
        for (entry in snapshot.scheduledEntries) {
            if (entry.previousLog != null) {
                dao.insertAttendanceLog(entry.previousLog)
            } else if (entry.createdLogId != null) {
                dao.deleteAttendanceLogById(entry.createdLogId)
            }
        }
        for (entry in snapshot.oneOffEntries) {
            val currentOneOff = dao.getOneOffClassById(entry.oneOffId)
            if (entry.previousLog != null) {
                dao.insertAttendanceLog(entry.previousLog)
                if (currentOneOff != null) {
                    dao.updateOneOffClass(currentOneOff.copy(attendanceLogId = entry.previousLog.id))
                }
            } else if (entry.createdLogId != null) {
                dao.deleteAttendanceLogById(entry.createdLogId)
                if (currentOneOff != null) {
                    dao.updateOneOffClass(currentOneOff.copy(attendanceLogId = null))
                }
            }
        }
    }

    // --- Offline Per-Semester Timetable PDF ---
    fun getTimetablePdfFile(semesterId: Long): File {
        val dir = File(context.filesDir, "timetables")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "sem_$semesterId.pdf")
    }

    suspend fun importTimetablePdf(semesterId: Long, sourceUri: Uri): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val target = getTimetablePdfFile(semesterId)
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    target.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: error("Unable to read selected PDF")
                target
            }
        }

    suspend fun removeTimetablePdf(semesterId: Long) = withContext(Dispatchers.IO) {
        getTimetablePdfFile(semesterId).delete()
    }

    // --- Local JSON Export / Import & Rolling Daily Backup via BackupManager ---
    suspend fun exportBackupToUri(uri: Uri): Result<Unit> =
        backupManager.exportBackupToUri(uri)

    suspend fun importBackupFromUri(uri: Uri): Result<Int> =
        backupManager.importBackupFromUri(uri)

    suspend fun performDailyLocalBackup(treeUri: Uri): Result<String> =
        backupManager.writeDailyRollingBackup(treeUri)
}
