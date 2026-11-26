package com.unideck.app.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.unideck.app.data.local.AttendanceLogEntity
import com.unideck.app.data.local.ClassScheduleEntity
import com.unideck.app.data.local.CourseEntity
import com.unideck.app.data.local.OneOffClassEntity
import com.unideck.app.data.local.SemesterEntity
import com.unideck.app.data.local.UniDeckDao
import com.unideck.app.data.prefs.UserPreferences
import com.unideck.app.data.prefs.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class UniDeckBackupPayload(
    val version: Int = 4,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val semesters: List<SemesterEntity>,
    val courses: List<CourseEntity>,
    val schedules: List<ClassScheduleEntity>,
    val attendanceLogs: List<AttendanceLogEntity>,
    val oneOffClasses: List<OneOffClassEntity> = emptyList(),
    val preferences: UserPreferences
)

/**
 * 100% offline JSON Backup & Restore engine supporting both one-shot SAF files
 * and rolling 3-day automatic local folder backups via DocumentFile.
 */
class BackupManager(
    private val context: Context,
    private val dao: UniDeckDao,
    private val preferences: UserPreferencesRepository
) {
    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val payload = UniDeckBackupPayload(
            version = 4,
            exportedAtEpochMs = System.currentTimeMillis(),
            semesters = dao.getAllSemestersSync(),
            courses = dao.getAllCoursesSync(),
            schedules = dao.getAllSchedulesSync(),
            attendanceLogs = dao.getAllAttendanceLogsSync(),
            oneOffClasses = dao.getAllOneOffClassesSync(),
            preferences = preferences.getPreferencesSync()
        )
        json.encodeToString(payload)
    }

    suspend fun exportBackupToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val serialized = generateBackupJson()
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(serialized.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(serialized.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Could not open destination file")
        }
    }

    suspend fun importBackupFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: error("Could not read backup file")
            val payload = decodeBackupPayload(raw)
            require(payload.semesters.isNotEmpty()) { "Backup contains no semesters" }
            dao.replaceAllData(
                semesters = payload.semesters,
                courses = payload.courses,
                schedules = payload.schedules,
                logs = payload.attendanceLogs,
                oneOffClasses = payload.oneOffClasses
            )
            preferences.applyBackupPreferences(payload.preferences)
            payload.courses.size
        }
    }

    fun hasPersistedWritePermission(treeUri: Uri): Boolean {
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri == treeUri && perm.isWritePermission
        }
    }

    suspend fun writeDailyRollingBackup(
        treeUri: Uri,
        today: LocalDate = LocalDate.now()
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!hasPersistedWritePermission(treeUri)) {
                error("Folder permission revoked. Please re-select your backup folder.")
            }
            val rootDir = DocumentFile.fromTreeUri(context, treeUri)
                ?: error("Unable to access backup folder")
            if (!rootDir.exists() || !rootDir.canWrite()) {
                error("Backup folder is not writable")
            }

            val serialized = generateBackupJson()
            val fileName = dailyBackupFileName(today)

            val existing = rootDir.findFile(fileName)
            val targetFile = existing ?: rootDir.createFile("application/json", fileName)
                ?: error("Could not create $fileName in selected folder")

            context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { out ->
                out.write(serialized.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: context.contentResolver.openOutputStream(targetFile.uri)?.use { out ->
                out.write(serialized.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Could not write to $fileName")

            // Enforce rolling retention: keep latest 3 daily files, delete older ones
            val allFiles = rootDir.listFiles().filter { it.isFile && it.name != null }
            val toDeleteNames = selectFilesToDelete(
                fileNames = allFiles.mapNotNull { it.name },
                maxKeep = MAX_ROLLING_DAILY_BACKUPS
            ).toSet()
            allFiles.filter { it.name in toDeleteNames }.forEach { oldFile ->
                runCatching { oldFile.delete() }
            }

            preferences.setLastAutoBackupTimestamp(System.currentTimeMillis())
            fileName
        }
    }

    companion object {
        const val MAX_ROLLING_DAILY_BACKUPS = 3
        private val DAILY_BACKUP_REGEX = Regex("""^unideck_backup_\d{4}-\d{2}-\d{2}\.json$""")

        val json: Json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

        fun decodeBackupPayload(rawJson: String): UniDeckBackupPayload {
            return json.decodeFromString<UniDeckBackupPayload>(rawJson)
        }

        fun dailyBackupFileName(date: LocalDate): String {
            return "unideck_backup_${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"
        }

        fun selectFilesToDelete(fileNames: List<String>, maxKeep: Int = MAX_ROLLING_DAILY_BACKUPS): List<String> {
            val matched = fileNames
                .filter { DAILY_BACKUP_REGEX.matches(it) }
                .sortedDescending()
            return if (matched.size > maxKeep) matched.drop(maxKeep) else emptyList()
        }

        fun resolveFolderDisplayName(context: Context, treeUri: Uri): String {
            val docName = runCatching { DocumentFile.fromTreeUri(context, treeUri)?.name }.getOrNull()
            if (!docName.isNullOrBlank()) return docName
            val lastSeg = treeUri.lastPathSegment.orEmpty()
            return lastSeg.substringAfterLast(':').substringAfterLast('/').ifBlank { "Selected Folder" }
        }
    }
}
