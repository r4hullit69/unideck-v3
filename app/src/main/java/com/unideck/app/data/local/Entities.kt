package com.unideck.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ComponentType {
    const val LECTURE = "LECTURE"
    const val LAB = "LAB"
    const val TUTORIAL = "TUTORIAL"

    val all = listOf(LECTURE, LAB, TUTORIAL)
}

object AttendanceStatus {
    const val PRESENT = "PRESENT"
    const val ABSENT = "ABSENT"
    const val CANCELLED = "CANCELLED"
}

@Serializable
@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCurrent: Boolean = false,
    val startDateEpochDay: Long? = null,
    val endDateEpochDay: Long? = null
)

fun SemesterEntity.isDateWithinSemester(epochDay: Long): Boolean {
    val afterStart = startDateEpochDay == null || epochDay >= startDateEpochDay
    val beforeEnd = endDateEpochDay == null || epochDay <= endDateEpochDay
    return afterStart && beforeEnd
}

fun SemesterEntity.formatDateRange(): String {
    val s = startDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    val e = endDateEpochDay?.let { LocalDate.ofEpochDay(it) }
    val monthDayFmt = DateTimeFormatter.ofPattern("MMM d")
    val fullFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    return when {
        s != null && e != null -> {
            if (s.year == e.year) "${s.format(monthDayFmt)} – ${e.format(fullFmt)}"
            else "${s.format(fullFmt)} – ${e.format(fullFmt)}"
        }
        s != null -> "From ${s.format(fullFmt)}"
        e != null -> "Until ${e.format(fullFmt)}"
        else -> "No dates set"
    }
}

@Serializable
@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("semesterId")]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val code: String,
    val name: String,
    val professorName: String = "",
    val credits: Double = 3.0,
    val isLab: Boolean = false,
    // For Theory (50/50): midSem (/25), quiz1 (/10), quiz2 (/10), assignment (/5), endSem (/50)
    // For Lab (60/40): midSem stores Internal (/60), endSem stores End-Sem (/40)
    val midSem: Float = 0f,
    val quiz1: Float = 0f,
    val quiz2: Float = 0f,
    val assignment: Float = 0f,
    val endSem: Float = 0f,
    val relativeGradingEnabled: Boolean = false,
    val customGradeOverride: String? = null,
    val initialAttended: Int = 0,
    val initialTotal: Int = 0
)

@Serializable
@Entity(
    tableName = "class_schedules",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("semesterId"), Index("dayOfWeek")]
)
data class ClassScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val semesterId: Long,
    val dayOfWeek: Int, // ISO-8601: 1 = Monday .. 7 = Sunday
    val startTime: Int, // Minutes from midnight, e.g., 9:00 AM = 540
    val endTime: Int,   // Minutes from midnight, e.g., 10:00 AM = 600
    val roomNumber: String = "",
    val componentType: String = ComponentType.LECTURE,
    val weight: Int = 1
)

@Serializable
@Entity(
    tableName = "attendance_logs",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("dateEpochDay")]
)
data class AttendanceLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val dateEpochDay: Long,
    val status: String, // PRESENT / ABSENT / CANCELLED
    val componentType: String = ComponentType.LECTURE,
    val isManualEntry: Boolean = false
)

@Serializable
@Entity(
    tableName = "one_off_classes",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId"), Index("semesterId"), Index("dateEpochDay")]
)
data class OneOffClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,
    val semesterId: Long,
    val dateEpochDay: Long,
    val startTime: Int, // Minutes from midnight
    val endTime: Int,   // Minutes from midnight
    val roomNumber: String = "",
    val componentType: String = ComponentType.LECTURE,
    val attendanceLogId: Long? = null
)
