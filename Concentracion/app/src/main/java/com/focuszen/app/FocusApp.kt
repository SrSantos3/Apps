package com.focuszen.app

import android.app.Application
import com.focuszen.app.data.local.AppDatabase
import com.focuszen.app.data.local.PreferencesManager
import com.focuszen.app.data.repository.FocusRepository

class FocusApp : Application() {

    companion object {
        lateinit var instance: FocusApp
            private set

        lateinit var database: AppDatabase
            private set

        lateinit var repository: FocusRepository
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        val preferencesManager = PreferencesManager(this)
        repository = FocusRepository(database.focusDao(), preferencesManager)
    }
}
