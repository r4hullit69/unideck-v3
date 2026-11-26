package com.unideck.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.unideck.app.MainActivity
import com.unideck.app.R
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.ComponentType
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.local.UniDeckDatabase
import com.unideck.app.data.local.isDateWithinSemester
import com.unideck.app.data.prefs.UserPreferencesRepository
import com.unideck.app.domain.AcademicCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    const val CHANNEL_CLASS_REMINDERS = "class_reminders"
    const val CHANNEL_ATTENDANCE_ALERTS = "attendance_alerts"
    const val CHANNEL_LIVE_CLASS = "live_ongoing_class"

    const val EXTRA_SUBJECT = "extra_subject"
    const val EXTRA_TIME = "extra_time"
    const val EXTRA_ROOM = "extra_room"
    const val EXTRA_PROFESSOR = "extra_professor"
    const val EXTRA_COMPONENT = "extra_component"

    private const val ALARM_REQUEST_CODE = 4041
    private const val LIVE_CLASS_NOTIFICATION_ID = 4099

    fun createNotificationChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val classChannel = NotificationChannel(
            CHANNEL_CLASS_REMINDERS,
            context.getString(R.string.channel_class_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_class_reminders_desc)
            enableVibration(true)
        }

        val attendanceChannel = NotificationChannel(
            CHANNEL_ATTENDANCE_ALERTS,
            context.getString(R.string.channel_attendance_alerts_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_attendance_alerts_desc)
        }

        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_CLASS,
            "Live Ongoing Class",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows an ongoing progress bar while a scheduled class is in session"
            setShowBadge(false)
        }

        manager.createNotificationChannel(classChannel)
        manager.createNotificationChannel(attendanceChannel)
        manager.createNotificationChannel(liveChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    suspend fun scheduleNextReminder(context: Context) = withContext(Dispatchers.IO) {
        val prefs = UserPreferencesRepository(context).getPreferencesSync()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        updateLiveOngoingClassNotification(context)
        cancelExistingReminder(context, alarmManager)

        val leadMinutes = prefs.reminderLeadMinutes
        if (leadMinutes <= 0) return@withContext

        val dao = UniDeckDatabase.getInstance(context).uniDeckDao()
        val currentSem = dao.getCurrentSemesterSync() ?: return@withContext
        if (!currentSem.isDateWithinSemester(LocalDate.now().toEpochDay())) return@withContext
        val schedules = dao.getSchedulesBySemesterSync(currentSem.id)
        if (schedules.isEmpty()) return@withContext
        val coursesById = dao.getCoursesBySemesterSync(currentSem.id).associateBy { it.id }

        val next = findNextOccurrence(currentSem, schedules, coursesById, leadMinutes) ?: return@withContext
        val (triggerMillis, schedule, course) = next

        val timeText = "${AcademicCalculator.formatMinutes(schedule.startTime, prefs.use24HourFormat)} – " +
            AcademicCalculator.formatMinutes(schedule.endTime, prefs.use24HourFormat)

        val intent = Intent(context, ClassReminderReceiver::class.java).apply {
            putExtra(EXTRA_SUBJECT, "${course.code} • ${course.name}")
            putExtra(EXTRA_TIME, timeText)
            putExtra(EXTRA_ROOM, schedule.roomNumber.ifBlank { "Room TBA" })
            putExtra(EXTRA_PROFESSOR, course.professorName.ifBlank { "Instructor TBA" })
            putExtra(EXTRA_COMPONENT, schedule.componentType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    /**
     * Posts or updates a live ongoing-class notification with a determinate progress bar
     * whenever a scheduled class is currently in session.
     */
    suspend fun updateLiveOngoingClassNotification(context: Context) = withContext(Dispatchers.IO) {
        val notifManager = NotificationManagerCompat.from(context)
        if (!hasNotificationPermission(context)) {
            runCatching { notifManager.cancel(LIVE_CLASS_NOTIFICATION_ID) }
            return@withContext
        }

        val prefs = UserPreferencesRepository(context).getPreferencesSync()
        if (!prefs.liveClassNotificationEnabled) {
            runCatching { notifManager.cancel(LIVE_CLASS_NOTIFICATION_ID) }
            return@withContext
        }

        val dao = UniDeckDatabase.getInstance(context).uniDeckDao()
        val sem = dao.getCurrentSemesterSync()
        val today = LocalDate.now()
        if (sem == null || !sem.isDateWithinSemester(today.toEpochDay())) {
            runCatching { notifManager.cancel(LIVE_CLASS_NOTIFICATION_ID) }
            return@withContext
        }

        val nowMinutes = LocalTime.now().let { it.hour * 60 + it.minute }
        val courses = dao.getCoursesBySemesterSync(sem.id)
        val schedules = dao.getSchedulesBySemesterSync(sem.id)
        val oneOffs = dao.getAllOneOffClassesSync().filter { it.semesterId == sem.id }

        val nextOrLive = AcademicCalculator.computeNextClass(
            courses = courses,
            schedules = schedules,
            oneOffClasses = oneOffs,
            today = today,
            nowMinutes = nowMinutes,
            startDateEpochDay = sem.startDateEpochDay,
            endDateEpochDay = sem.endDateEpochDay
        )

        if (nextOrLive == null || !nextOrLive.isOngoing) {
            runCatching { notifManager.cancel(LIVE_CLASS_NOTIFICATION_ID) }
            return@withContext
        }

        createNotificationChannels(context)
        val duration = (nextOrLive.endTime - nextOrLive.startTime).coerceAtLeast(1)
        val elapsed = (nowMinutes - nextOrLive.startTime).coerceIn(0, duration)
        val startStr = AcademicCalculator.formatMinutes(nextOrLive.startTime, prefs.use24HourFormat)
        val endStr = AcademicCalculator.formatMinutes(nextOrLive.endTime, prefs.use24HourFormat)
        val typeLabel = when (nextOrLive.componentType) {
            ComponentType.LAB -> "Lab"
            ComponentType.TUTORIAL -> "Tutorial"
            else -> "Theory"
        }
        val roomLabel = nextOrLive.roomNumber.ifBlank { typeLabel }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            LIVE_CLASS_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_LIVE_CLASS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${nextOrLive.courseName} • In progress now")
            .setContentText("$startStr – $endStr • $typeLabel • $roomLabel")
            .setSubText(nextOrLive.courseCode)
            .setProgress(duration, elapsed, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            notifManager.notify(LIVE_CLASS_NOTIFICATION_ID, notification)
        }
    }

    private fun cancelExistingReminder(context: Context, alarmManager: AlarmManager) {
        val intent = Intent(context, ClassReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun findNextOccurrence(
        semester: SemesterEntity,
        schedules: List<ClassScheduleEntity>,
        coursesById: Map<Long, CourseEntity>,
        leadMinutes: Int
    ): Triple<Long, ClassScheduleEntity, CourseEntity>? {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        var best: Triple<Long, ClassScheduleEntity, CourseEntity>? = null

        for (dayOffset in 0..7) {
            val date = today.plusDays(dayOffset.toLong())
            if (!semester.isDateWithinSemester(date.toEpochDay())) continue
            val isoDay = date.dayOfWeek.value
            for (slot in schedules) {
                if (slot.dayOfWeek != isoDay) continue
                val course = coursesById[slot.courseId] ?: continue
                val startTime = LocalTime.of(slot.startTime / 60, slot.startTime % 60)
                val triggerDateTime = LocalDateTime.of(date, startTime).minusMinutes(leadMinutes.toLong())
                if (triggerDateTime.isAfter(now.plusSeconds(15))) {
                    val millis = triggerDateTime.atZone(zone).toInstant().toEpochMilli()
                    if (best == null || millis < best.first) {
                        best = Triple(millis, slot, course)
                    }
                }
            }
        }
        return best
    }

    fun postLowAttendanceAlert(
        context: Context,
        courseCode: String,
        courseName: String,
        currentPercent: Double,
        threshold: Int,
        classesNeeded: Int
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            courseCode.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatted = "%.1f%%".format(currentPercent)
        val notification = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$courseCode dropped to $formatted")
            .setContentText("$courseName is below $threshold%. Attend next $classesNeeded to recover.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(courseCode.hashCode(), notification)
        }
    }
}
