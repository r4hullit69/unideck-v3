package com.unideck.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UniDeckDao {

    // --- Semesters ---
    @Query("SELECT * FROM semesters ORDER BY id ASC")
    fun observeSemesters(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY id ASC")
    suspend fun getAllSemestersSync(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentSemesterSync(): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Delete
    suspend fun deleteSemester(semester: SemesterEntity)

    @Query("UPDATE semesters SET isCurrent = 0")
    suspend fun clearCurrentSemester()

    @Query("UPDATE semesters SET isCurrent = 1 WHERE id = :semesterId")
    suspend fun markCurrentSemester(semesterId: Long)

    @Transaction
    suspend fun setActiveSemester(semesterId: Long) {
        clearCurrentSemester()
        markCurrentSemester(semesterId)
    }

    // --- Courses ---
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY code ASC")
    fun observeCoursesBySemester(semesterId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY semesterId ASC, code ASC")
    fun observeAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY id ASC")
    suspend fun getAllCoursesSync(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY code ASC")
    suspend fun getCoursesBySemesterSync(semesterId: Long): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun observeCourse(courseId: Long): Flow<CourseEntity?>

    @Query("SELECT DISTINCT professorName FROM courses WHERE TRIM(professorName) != '' ORDER BY professorName ASC")
    fun observeDistinctProfessors(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    // --- Class Schedules ---
    @Query("SELECT * FROM class_schedules WHERE semesterId = :semesterId ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeSchedulesBySemester(semesterId: Long): Flow<List<ClassScheduleEntity>>

    @Query("SELECT * FROM class_schedules WHERE semesterId = :semesterId ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getSchedulesBySemesterSync(semesterId: Long): List<ClassScheduleEntity>

    @Query("SELECT * FROM class_schedules ORDER BY id ASC")
    suspend fun getAllSchedulesSync(): List<ClassScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ClassScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ClassScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ClassScheduleEntity)

    // --- Attendance Logs ---
    @Query("SELECT * FROM attendance_logs ORDER BY dateEpochDay ASC, id ASC")
    fun observeAllAttendanceLogs(): Flow<List<AttendanceLogEntity>>

    @Query("SELECT * FROM attendance_logs ORDER BY id ASC")
    suspend fun getAllAttendanceLogsSync(): List<AttendanceLogEntity>

    @Query(
        """
        SELECT * FROM attendance_logs
        WHERE courseId = :courseId AND dateEpochDay = :dateEpochDay
          AND componentType = :componentType AND isManualEntry = 0
        LIMIT 1
        """
    )
    suspend fun findScheduledLog(
        courseId: Long,
        dateEpochDay: Long,
        componentType: String
    ): AttendanceLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceLog(log: AttendanceLogEntity): Long

    @Update
    suspend fun updateAttendanceLog(log: AttendanceLogEntity)

    @Delete
    suspend fun deleteAttendanceLog(log: AttendanceLogEntity)

    @Query("SELECT * FROM attendance_logs WHERE id = :logId LIMIT 1")
    suspend fun getAttendanceLogById(logId: Long): AttendanceLogEntity?

    @Query("DELETE FROM attendance_logs WHERE id = :logId")
    suspend fun deleteAttendanceLogById(logId: Long)

    // --- One-Off / Extra Date-Specific Classes ---
    @Query("SELECT * FROM one_off_classes WHERE semesterId = :semesterId ORDER BY dateEpochDay ASC, startTime ASC")
    fun observeOneOffClassesBySemester(semesterId: Long): Flow<List<OneOffClassEntity>>

    @Query("SELECT * FROM one_off_classes ORDER BY id ASC")
    suspend fun getAllOneOffClassesSync(): List<OneOffClassEntity>

    @Query("SELECT * FROM one_off_classes WHERE id = :id LIMIT 1")
    suspend fun getOneOffClassById(id: Long): OneOffClassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOneOffClass(item: OneOffClassEntity): Long

    @Update
    suspend fun updateOneOffClass(item: OneOffClassEntity)

    @Delete
    suspend fun deleteOneOffClass(item: OneOffClassEntity)

    @Query("DELETE FROM one_off_classes")
    suspend fun deleteAllOneOffClasses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOneOffClasses(items: List<OneOffClassEntity>)

    // --- Bulk Backup Replace ---
    @Query("DELETE FROM attendance_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM class_schedules")
    suspend fun deleteAllSchedules()

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Query("DELETE FROM semesters")
    suspend fun deleteAllSemesters()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesters(items: List<SemesterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(items: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(items: List<ClassScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceLogs(items: List<AttendanceLogEntity>)

    @Transaction
    suspend fun replaceAllData(
        semesters: List<SemesterEntity>,
        courses: List<CourseEntity>,
        schedules: List<ClassScheduleEntity>,
        logs: List<AttendanceLogEntity>,
        oneOffClasses: List<OneOffClassEntity> = emptyList()
    ) {
        deleteAllOneOffClasses()
        deleteAllLogs()
        deleteAllSchedules()
        deleteAllCourses()
        deleteAllSemesters()
        insertSemesters(semesters)
        insertCourses(courses)
        insertSchedules(schedules)
        insertAttendanceLogs(logs)
        insertOneOffClasses(oneOffClasses)
    }
}
