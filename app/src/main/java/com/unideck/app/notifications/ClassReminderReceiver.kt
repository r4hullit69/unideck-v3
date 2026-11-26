package com.unideck.app.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unideck.app.MainActivity
import com.unideck.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.createNotificationChannels(context)

        val subject = intent.getStringExtra(ReminderScheduler.EXTRA_SUBJECT) ?: "Upcoming Class"
        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME) ?: ""
        val room = intent.getStringExtra(ReminderScheduler.EXTRA_ROOM) ?: "Room TBA"
        val professor = intent.getStringExtra(ReminderScheduler.EXTRA_PROFESSOR) ?: ""
        val component = intent.getStringExtra(ReminderScheduler.EXTRA_COMPONENT) ?: "LECTURE"

        if (ReminderScheduler.hasNotificationPermission(context)) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                subject.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val detailLine = buildString {
                if (time.isNotBlank()) append(time)
                if (room.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(room)
                }
                if (professor.isNotBlank() && professor != "Instructor TBA") {
                    if (isNotEmpty()) append(" • ")
                    append(professor)
                }
            }

            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_CLASS_REMINDERS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("$subject ($component)")
                .setContentText(detailLine)
                .setStyle(NotificationCompat.BigTextStyle().bigText(detailLine))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            runCatching {
                NotificationManagerCompat.from(context).notify(
                    (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    notification
                )
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.scheduleNextReminder(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
