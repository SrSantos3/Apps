package com.focuszen.app.data.repository

import com.focuszen.app.data.local.FocusDao
import com.focuszen.app.data.local.PreferencesManager
import com.focuszen.app.data.model.DailyStat
import com.focuszen.app.data.model.MonitoredApp
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FocusRepository(
    private val focusDao: FocusDao,
    val preferencesManager: PreferencesManager
) {

    val allMonitoredApps: Flow<List<MonitoredApp>> = focusDao.getAllMonitoredAppsFlow()
    val recentStats: Flow<List<DailyStat>> = focusDao.getRecentStatsFlow(7)

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getTodayStatFlow(): Flow<DailyStat?> {
        return focusDao.getDailyStatFlow(getTodayDateString())
    }

    suspend fun getActiveMonitoredApps(): List<MonitoredApp> {
        return focusDao.getActiveMonitoredApps()
    }

    suspend fun getMonitoredApp(packageName: String): MonitoredApp? {
        return focusDao.getMonitoredApp(packageName)
    }

    suspend fun saveMonitoredApp(app: MonitoredApp) {
        focusDao.insertOrUpdateApp(app)
    }

    suspend fun toggleAppEnabled(packageName: String, isEnabled: Boolean) {
        val app = focusDao.getMonitoredApp(packageName)
        if (app != null) {
            focusDao.updateApp(app.copy(isEnabled = isEnabled))
        }
    }

    suspend fun updateDailyLimit(packageName: String, limitMinutes: Int) {
        val app = focusDao.getMonitoredApp(packageName)
        if (app != null) {
            focusDao.updateApp(app.copy(dailyLimitMinutes = limitMinutes))
        }
    }

    suspend fun deleteApp(app: MonitoredApp) {
        focusDao.deleteApp(app)
    }

    suspend fun recordAttemptAvoided(packageName: String) {
        val today = getTodayDateString()
        ensureTodayStatExists(today)
        focusDao.incrementAttemptsAvoided(today)
    }

    suspend fun recordSessionStarted(packageName: String) {
        val today = getTodayDateString()
        ensureTodayStatExists(today)
        focusDao.incrementSessionsStarted(today)
        focusDao.incrementOpens(packageName, System.currentTimeMillis())
    }

    suspend fun recordExtensionRequested() {
        val today = getTodayDateString()
        ensureTodayStatExists(today)
        focusDao.incrementExtensions(today)
    }

    private suspend fun ensureTodayStatExists(today: String) {
        val stat = focusDao.getDailyStat(today)
        if (stat == null) {
            focusDao.insertOrUpdateDailyStat(
                DailyStat(date = today, attemptsAvoided = 0, sessionsStarted = 0, estimatedMinutesSaved = 0)
            )
        }
    }
}
