package com.unideck.app.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unideck.app.UniDeckApplication

/**
 * Offline background worker running every 24 hours via WorkManager to write a
 * rolling daily local JSON backup into the user's chosen SAF folder.
 */
class DailyBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? UniDeckApplication ?: return Result.success()
        val prefs = app.preferencesRepository.getPreferencesSync()

        if (!prefs.autoBackupEnabled) {
            return Result.success()
        }

        val uriStr = prefs.autoBackupFolderUri
        if (uriStr.isNullOrBlank()) {
            return Result.success()
        }

        val treeUri = runCatching { Uri.parse(uriStr) }.getOrNull() ?: return Result.success()
        if (!app.backupManager.hasPersistedWritePermission(treeUri)) {
            // Permission was revoked by user/system; disable auto-backup cleanly without failing
            app.preferencesRepository.setAutoBackupEnabled(false)
            AutoBackupScheduler.cancelDailyBackup(applicationContext)
            return Result.success()
        }

        val result = app.backupManager.writeDailyRollingBackup(treeUri)
        return if (result.isSuccess) {
            Result.success()
        } else if (runAttemptCount < 2) {
            Result.retry()
        } else {
            Result.failure()
        }
    }
}
