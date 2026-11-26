package com.unideck.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.unideck.app.ui.theme.UniDeckPalettes
import com.unideck.app.ui.theme.resolveUniDeckPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "unideck_prefs")

private val hubJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

@Serializable
data class GradeCutoffs(
    val aPlusMin: Int = 90,
    val aMin: Int = 80,
    val bMin: Int = 70,
    val cMin: Int = 60,
    val dMin: Int = 50,
    val eMin: Int = 40
)

@Serializable
data class AcademicNoteItem(
    val id: Long,
    val semesterId: Long = 0L,
    val title: String,
    val body: String = "",
    val category: String = "Faculty", // "Faculty", "Assignments", "Pinned", "Exam"
    val courseCode: String = "",
    val pinned: Boolean = false,
    val dueEpochDay: Long? = null
)

@Serializable
data class AcademicTaskItem(
    val id: Long,
    val semesterId: Long = 0L,
    val title: String,
    val completed: Boolean = false,
    val dueEpochDay: Long? = null
)

@Serializable
data class AcademicMilestoneItem(
    val id: Long,
    val year: Int,
    val title: String,
    val subtitle: String = "",
    val completed: Boolean = false
)

@Serializable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val pitchBlackEnabled: Boolean = false,
    val dynamicMonetEnabled: Boolean = true,
    val paletteIndex: Int = 0, // Legacy index kept in sync with accentKey
    val accentKey: String = "blue",
    val cutCorners: Boolean = false,
    val paletteStyle: String = "TonalSpot", // "TonalSpot", "Vibrant", "Expressive", "Neutral"
    val useSystemFont: Boolean = true,
    val defaultAttendanceTab: Int = 0, // 0 = Today, 1 = Courses, 2 = Events
    val minAttendanceThreshold: Int = 75,
    val use24HourFormat: Boolean = false,
    val reminderLeadMinutes: Int = 0, // 0 = Off, 5, 10, 15, 30
    val liveClassNotificationEnabled: Boolean = true,
    val activeIconKey: String = "dark",
    val gradeCutoffs: GradeCutoffs = GradeCutoffs(),
    val completedSemesterIds: Set<Long> = emptySet(),
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val autoBackupFolderName: String? = null,
    val lastAutoBackupTimestamp: Long? = null,
    val academicNotes: List<AcademicNoteItem> = emptyList(),
    val academicTasks: List<AcademicTaskItem> = emptyList(),
    val academicMilestones: List<AcademicMilestoneItem> = emptyList()
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PITCH_BLACK = booleanPreferencesKey("pitch_black")
        val DYNAMIC_MONET = booleanPreferencesKey("dynamic_monet")
        val PALETTE_INDEX = intPreferencesKey("palette_index")
        val ACCENT_KEY = stringPreferencesKey("accent_key")
        val CUT_CORNERS = booleanPreferencesKey("cut_corners")
        val PALETTE_STYLE = stringPreferencesKey("palette_style")
        val USE_SYSTEM_FONT = booleanPreferencesKey("use_system_font")
        val DEFAULT_ATTENDANCE_TAB = intPreferencesKey("default_attendance_tab")
        val MIN_ATTENDANCE = intPreferencesKey("min_attendance_threshold")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour_format")
        val REMINDER_LEAD = intPreferencesKey("reminder_lead_minutes")
        val LIVE_CLASS_NOTIFICATION = booleanPreferencesKey("live_class_notification")
        val ACTIVE_ICON = stringPreferencesKey("active_icon_key")
        val CUTOFF_A_PLUS = intPreferencesKey("scale2_cutoff_a_plus")
        val CUTOFF_A = intPreferencesKey("scale2_cutoff_a")
        val CUTOFF_B = intPreferencesKey("scale2_cutoff_b")
        val CUTOFF_C = intPreferencesKey("scale2_cutoff_c")
        val CUTOFF_D = intPreferencesKey("scale2_cutoff_d")
        val CUTOFF_E = intPreferencesKey("scale2_cutoff_e")
        val COMPLETED_SEMESTER_IDS = stringSetPreferencesKey("completed_semester_ids")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        val AUTO_BACKUP_FOLDER_NAME = stringPreferencesKey("auto_backup_folder_name")
        val LAST_AUTO_BACKUP_TS = longPreferencesKey("last_auto_backup_timestamp")
        val ACADEMIC_NOTES_JSON = stringPreferencesKey("academic_notes_json")
        val ACADEMIC_TASKS_JSON = stringPreferencesKey("academic_tasks_json")
        val ACADEMIC_MILESTONES_JSON = stringPreferencesKey("academic_milestones_json")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)
        val resolvedPalette = resolveUniDeckPalette(prefs[Keys.ACCENT_KEY])
        val completedIds = prefs[Keys.COMPLETED_SEMESTER_IDS]
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

        val notes = prefs[Keys.ACADEMIC_NOTES_JSON]?.let { raw ->
            runCatching { hubJson.decodeFromString<List<AcademicNoteItem>>(raw) }.getOrNull()
        } ?: emptyList()

        val tasks = prefs[Keys.ACADEMIC_TASKS_JSON]?.let { raw ->
            runCatching { hubJson.decodeFromString<List<AcademicTaskItem>>(raw) }.getOrNull()
        } ?: emptyList()

        val milestones = prefs[Keys.ACADEMIC_MILESTONES_JSON]?.let { raw ->
            runCatching { hubJson.decodeFromString<List<AcademicMilestoneItem>>(raw) }.getOrNull()
        } ?: emptyList()

        UserPreferences(
            themeMode = themeMode,
            pitchBlackEnabled = prefs[Keys.PITCH_BLACK] ?: false,
            dynamicMonetEnabled = prefs[Keys.DYNAMIC_MONET] ?: true,
            paletteIndex = resolvedPalette.index,
            accentKey = resolvedPalette.key,
            cutCorners = prefs[Keys.CUT_CORNERS] ?: false,
            paletteStyle = prefs[Keys.PALETTE_STYLE] ?: "TonalSpot",
            useSystemFont = prefs[Keys.USE_SYSTEM_FONT] ?: true,
            defaultAttendanceTab = (prefs[Keys.DEFAULT_ATTENDANCE_TAB] ?: 0).coerceIn(0, 2),
            minAttendanceThreshold = (prefs[Keys.MIN_ATTENDANCE] ?: 75).coerceIn(50, 99),
            use24HourFormat = prefs[Keys.USE_24_HOUR] ?: false,
            reminderLeadMinutes = prefs[Keys.REMINDER_LEAD] ?: 0,
            liveClassNotificationEnabled = prefs[Keys.LIVE_CLASS_NOTIFICATION] ?: true,
            activeIconKey = prefs[Keys.ACTIVE_ICON] ?: "dark",
            gradeCutoffs = GradeCutoffs(
                aPlusMin = prefs[Keys.CUTOFF_A_PLUS] ?: 90,
                aMin = prefs[Keys.CUTOFF_A] ?: 80,
                bMin = prefs[Keys.CUTOFF_B] ?: 70,
                cMin = prefs[Keys.CUTOFF_C] ?: 60,
                dMin = prefs[Keys.CUTOFF_D] ?: 50,
                eMin = prefs[Keys.CUTOFF_E] ?: 40
            ),
            completedSemesterIds = completedIds,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: false,
            autoBackupFolderUri = prefs[Keys.AUTO_BACKUP_FOLDER_URI],
            autoBackupFolderName = prefs[Keys.AUTO_BACKUP_FOLDER_NAME],
            lastAutoBackupTimestamp = prefs[Keys.LAST_AUTO_BACKUP_TS],
            academicNotes = notes,
            academicTasks = tasks,
            academicMilestones = milestones
        )
    }

    suspend fun getPreferencesSync(): UserPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPitchBlack(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PITCH_BLACK] = enabled }
    }

    suspend fun setDynamicMonet(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_MONET] = enabled }
    }

    suspend fun setAccentKey(key: String, disableDynamicMonet: Boolean = true) {
        val palette = resolveUniDeckPalette(key)
        context.dataStore.edit {
            if (disableDynamicMonet) {
                it[Keys.DYNAMIC_MONET] = false
            }
            it[Keys.ACCENT_KEY] = palette.key
            it[Keys.PALETTE_INDEX] = palette.index
        }
    }

    suspend fun setPaletteIndex(index: Int) {
        val palette = UniDeckPalettes.getOrElse(index) { UniDeckPalettes.first() }
        setAccentKey(palette.key)
    }

    suspend fun setCutCorners(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CUT_CORNERS] = enabled }
    }

    suspend fun setPaletteStyle(style: String) {
        context.dataStore.edit { it[Keys.PALETTE_STYLE] = style }
    }

    suspend fun setUseSystemFont(useSystem: Boolean) {
        context.dataStore.edit { it[Keys.USE_SYSTEM_FONT] = useSystem }
    }

    suspend fun setDefaultAttendanceTab(tabIndex: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_ATTENDANCE_TAB] = tabIndex.coerceIn(0, 2) }
    }

    suspend fun setMinAttendanceThreshold(threshold: Int) {
        context.dataStore.edit { it[Keys.MIN_ATTENDANCE] = threshold.coerceIn(50, 99) }
    }

    suspend fun setUse24HourFormat(use24Hour: Boolean) {
        context.dataStore.edit { it[Keys.USE_24_HOUR] = use24Hour }
    }

    suspend fun setReminderLeadMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.REMINDER_LEAD] = minutes }
    }

    suspend fun setLiveClassNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LIVE_CLASS_NOTIFICATION] = enabled }
    }

    suspend fun setActiveIconKey(iconKey: String) {
        context.dataStore.edit { it[Keys.ACTIVE_ICON] = iconKey }
    }

    suspend fun setSemesterCompleted(semesterId: Long, completed: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_SEMESTER_IDS].orEmpty().toMutableSet()
            if (completed) {
                current.add(semesterId.toString())
            } else {
                current.remove(semesterId.toString())
            }
            prefs[Keys.COMPLETED_SEMESTER_IDS] = current
        }
    }

    suspend fun setGradeCutoffs(cutoffs: GradeCutoffs) {
        context.dataStore.edit { prefs ->
            val safeAPlus = cutoffs.aPlusMin.coerceIn(50, 100)
            val safeA = cutoffs.aMin.coerceIn(40, safeAPlus - 1)
            val safeB = cutoffs.bMin.coerceIn(30, safeA - 1)
            val safeC = cutoffs.cMin.coerceIn(20, safeB - 1)
            val safeD = cutoffs.dMin.coerceIn(10, safeC - 1)
            val safeE = cutoffs.eMin.coerceIn(1, safeD - 1)
            prefs[Keys.CUTOFF_A_PLUS] = safeAPlus
            prefs[Keys.CUTOFF_A] = safeA
            prefs[Keys.CUTOFF_B] = safeB
            prefs[Keys.CUTOFF_C] = safeC
            prefs[Keys.CUTOFF_D] = safeD
            prefs[Keys.CUTOFF_E] = safeE
        }
    }

    suspend fun setAcademicNotes(notes: List<AcademicNoteItem>) {
        context.dataStore.edit {
            it[Keys.ACADEMIC_NOTES_JSON] = hubJson.encodeToString(notes)
        }
    }

    suspend fun setAcademicTasks(tasks: List<AcademicTaskItem>) {
        context.dataStore.edit {
            it[Keys.ACADEMIC_TASKS_JSON] = hubJson.encodeToString(tasks)
        }
    }

    suspend fun setAcademicMilestones(milestones: List<AcademicMilestoneItem>) {
        context.dataStore.edit {
            it[Keys.ACADEMIC_MILESTONES_JSON] = hubJson.encodeToString(milestones)
        }
    }

    suspend fun applyBackupPreferences(prefs: UserPreferences) {
        val resolvedPalette = resolveUniDeckPalette(prefs.accentKey)
        context.dataStore.edit {
            it[Keys.THEME_MODE] = prefs.themeMode.name
            it[Keys.PITCH_BLACK] = prefs.pitchBlackEnabled
            it[Keys.DYNAMIC_MONET] = prefs.dynamicMonetEnabled
            it[Keys.ACCENT_KEY] = resolvedPalette.key
            it[Keys.PALETTE_INDEX] = resolvedPalette.index
            it[Keys.CUT_CORNERS] = prefs.cutCorners
            it[Keys.PALETTE_STYLE] = prefs.paletteStyle
            it[Keys.USE_SYSTEM_FONT] = prefs.useSystemFont
            it[Keys.DEFAULT_ATTENDANCE_TAB] = prefs.defaultAttendanceTab.coerceIn(0, 2)
            it[Keys.MIN_ATTENDANCE] = prefs.minAttendanceThreshold.coerceIn(50, 99)
            it[Keys.USE_24_HOUR] = prefs.use24HourFormat
            it[Keys.REMINDER_LEAD] = prefs.reminderLeadMinutes
            it[Keys.LIVE_CLASS_NOTIFICATION] = prefs.liveClassNotificationEnabled
            it[Keys.COMPLETED_SEMESTER_IDS] = prefs.completedSemesterIds.map { id -> id.toString() }.toSet()
            it[Keys.CUTOFF_A_PLUS] = prefs.gradeCutoffs.aPlusMin
            it[Keys.CUTOFF_A] = prefs.gradeCutoffs.aMin
            it[Keys.CUTOFF_B] = prefs.gradeCutoffs.bMin
            it[Keys.CUTOFF_C] = prefs.gradeCutoffs.cMin
            it[Keys.CUTOFF_D] = prefs.gradeCutoffs.dMin
            it[Keys.CUTOFF_E] = prefs.gradeCutoffs.eMin
            it[Keys.ACADEMIC_NOTES_JSON] = hubJson.encodeToString(prefs.academicNotes)
            it[Keys.ACADEMIC_TASKS_JSON] = hubJson.encodeToString(prefs.academicTasks)
            it[Keys.ACADEMIC_MILESTONES_JSON] = hubJson.encodeToString(prefs.academicMilestones)
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setAutoBackupFolder(uriString: String?, displayName: String?, enable: Boolean) {
        context.dataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(Keys.AUTO_BACKUP_FOLDER_URI)
                prefs.remove(Keys.AUTO_BACKUP_FOLDER_NAME)
                prefs[Keys.AUTO_BACKUP_ENABLED] = false
            } else {
                prefs[Keys.AUTO_BACKUP_FOLDER_URI] = uriString
                prefs[Keys.AUTO_BACKUP_FOLDER_NAME] = displayName ?: "Selected Folder"
                prefs[Keys.AUTO_BACKUP_ENABLED] = enable
            }
        }
    }

    suspend fun setLastAutoBackupTimestamp(epochMillis: Long) {
        context.dataStore.edit { it[Keys.LAST_AUTO_BACKUP_TS] = epochMillis }
    }
}
