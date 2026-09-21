package com.focuszen.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.focuszen.app.data.model.DailyStat
import com.focuszen.app.data.model.MonitoredApp
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

    // --- Monitored Apps ---
    @Query("SELECT * FROM monitored_apps ORDER BY appName ASC")
    fun getAllMonitoredAppsFlow(): Flow<List<MonitoredApp>>

    @Query("SELECT * FROM monitored_apps WHERE isEnabled = 1")
    suspend fun getActiveMonitoredApps(): List<MonitoredApp>

    @Query("SELECT * FROM monitored_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getMonitoredApp(packageName: String): MonitoredApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: MonitoredApp)

    @Update
    suspend fun updateApp(app: MonitoredApp)

    @Delete
    suspend fun deleteApp(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET opensToday = opensToday + 1, lastOpenedTimestamp = :timestamp WHERE packageName = :packageName")
    suspend fun incrementOpens(packageName: String, timestamp: Long)

    @Query("UPDATE monitored_apps SET opensToday = 0")
    suspend fun resetDailyOpens()

    // --- Daily Stats ---
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    fun getDailyStatFlow(date: String): Flow<DailyStat?>

    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    suspend fun getDailyStat(date: String): DailyStat?

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :limit")
    fun getRecentStatsFlow(limit: Int = 7): Flow<List<DailyStat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyStat(stat: DailyStat)

    @Query("UPDATE daily_stats SET attemptsAvoided = attemptsAvoided + 1, estimatedMinutesSaved = estimatedMinutesSaved + 15 WHERE date = :date")
    suspend fun incrementAttemptsAvoided(date: String)

    @Query("UPDATE daily_stats SET sessionsStarted = sessionsStarted + 1 WHERE date = :date")
    suspend fun incrementSessionsStarted(date: String)

    @Query("UPDATE daily_stats SET extensionsRequested = extensionsRequested + 1 WHERE date = :date")
    suspend fun incrementExtensions(date: String)
}
