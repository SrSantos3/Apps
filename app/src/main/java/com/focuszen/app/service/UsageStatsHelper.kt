package com.focuszen.app.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

object UsageStatsHelper {

    /**
     * Verifica si la app tiene concedido el permiso de Acceso a Estadísticas de Uso.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Devuelve los minutos totales que la aplicación ha estado en primer plano durante el día de hoy (desde las 00:00).
     */
    fun getMinutesUsedToday(context: Context, targetPackageName: String): Int {
        if (!hasUsageStatsPermission(context)) return 0

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val stat = usageStatsList.firstOrNull { it.packageName == targetPackageName }
        val totalTimeVisibleMillis = stat?.totalTimeInForeground ?: 0L

        return (totalTimeVisibleMillis / (1000 * 60)).toInt()
    }
}
