package com.focuszen.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focuszen.app.FocusApp
import com.focuszen.app.ui.friction.FrictionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class FocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastInterceptedPackage: String? = null
    private var lastInterceptedTimestamp: Long = 0L

    companion object {
        private var instanceRef: WeakReference<FocusAccessibilityService>? = null

        fun isServiceRunning(): Boolean = instanceRef?.get() != null

        fun goToHomeScreen(): Boolean {
            val service = instanceRef?.get()
            return service?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        instanceRef = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val rawPackageName = event.packageName?.toString() ?: return

        // Sincronizar visibilidad de la píldora flotante:
        // Solo debe estar visible si la app en primer plano es la app vigilada activa
        FloatingGoalOverlayService.updateVisibilityForPackage(this, rawPackageName)

        // Ignorar nuestra propia aplicación para evitar bucles
        if (rawPackageName == packageName) return

        // Anti-Trampas (Strict Mode): Si intenta deshabilitar accesibilidad en Ajustes
        if (rawPackageName == "com.android.settings") {
            handleSettingsTampering(event)
            return
        }

        // Consultar de forma asíncrona si la app está en la lista de monitoreo
        serviceScope.launch {
            checkAndInterceptApp(rawPackageName)
        }
    }

    private suspend fun checkAndInterceptApp(targetPackage: String) {
        val app = FocusApp.repository.getMonitoredApp(targetPackage) ?: return

        if (!app.isEnabled) return

        // Si el usuario ya tiene un pase temporal activo y no ha expirado, permitir uso sin fricción
        if (SessionManager.hasActiveSession(targetPackage)) {
            return
        }

        // Si la sesión expiró o no existe, nos aseguramos de ocultar la ventana flotante anterior si la hubiera
        FloatingGoalOverlayService.stop(this@FocusAccessibilityService)

        // Debounce rápido para evitar disparos dobles en el mismo segundo
        val now = System.currentTimeMillis()
        if (lastInterceptedPackage == targetPackage && (now - lastInterceptedTimestamp) < 1500L) {
            return
        }
        lastInterceptedPackage = targetPackage
        lastInterceptedTimestamp = now

        // Comprobar si ha superado la cuota de tiempo diario
        val minutesUsed = UsageStatsHelper.getMinutesUsedToday(this@FocusAccessibilityService, targetPackage)
        val limitReached = app.dailyLimitMinutes > 0 && minutesUsed >= app.dailyLimitMinutes

        // Lanzar la pantalla de fricción o de cuota agotada
        val intent = Intent(this@FocusAccessibilityService, FrictionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(FrictionActivity.EXTRA_PACKAGE_NAME, targetPackage)
            putExtra(FrictionActivity.EXTRA_APP_NAME, app.appName)
            putExtra(FrictionActivity.EXTRA_FRICTION_SECONDS, app.frictionSeconds)
            putExtra(FrictionActivity.EXTRA_DAILY_LIMIT, app.dailyLimitMinutes)
            putExtra(FrictionActivity.EXTRA_MINUTES_USED, minutesUsed)
            putExtra(
                FrictionActivity.EXTRA_MODE,
                if (limitReached) FrictionActivity.MODE_LIMIT_EXCEEDED else FrictionActivity.MODE_NORMAL_FRICTION
            )
        }

        startActivity(intent)
    }

    private fun handleSettingsTampering(event: AccessibilityEvent) {
        serviceScope.launch {
            val isStrict = FocusApp.repository.preferencesManager.isStrictModeEnabled.first()
            if (!isStrict) return@launch

            // Si el texto de la pantalla de ajustes menciona Accesibilidad o FocusZen, advertir y proteger
            val text = event.text.joinToString(" ")
            if (text.contains("FocusZen", ignoreCase = true) || text.contains("Accesibilidad", ignoreCase = true)) {
                // Modo estricto activo: volvemos al inicio para evitar que se desactive por impulso
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    override fun onInterrupt() {
        // Manejo de interrupciones del servicio por el sistema
    }
}
