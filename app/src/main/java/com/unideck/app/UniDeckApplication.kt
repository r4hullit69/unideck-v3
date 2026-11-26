package com.unideck.app

import android.app.Application
import com.unideck.app.backup.BackupManager
import com.unideck.app.data.local.UniDeckDatabase
import com.unideck.app.data.prefs.UserPreferencesRepository
import com.unideck.app.data.repository.UniDeckRepository
import com.unideck.app.notifications.ReminderScheduler

class UniDeckApplication : Application() {
    lateinit var database: UniDeckDatabase
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var backupManager: BackupManager
        private set
    lateinit var repository: UniDeckRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = UniDeckDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        backupManager = BackupManager(this, database.uniDeckDao(), preferencesRepository)
        repository = UniDeckRepository(this, database.uniDeckDao(), preferencesRepository, backupManager)
        ReminderScheduler.createNotificationChannels(this)
    }
}
