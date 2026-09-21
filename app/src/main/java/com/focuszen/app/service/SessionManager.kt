package com.focuszen.app.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Gestiona las sesiones temporales concedidas a aplicaciones tras completar la fricción consciente.
 */
object SessionManager {

    // Mapa de packageName -> Timestamp en milisegundos en que expira la sesión concedida
    private val activeSessions = ConcurrentHashMap<String, Long>()

    /**
     * Comprueba si el usuario tiene una sesión válida y activa para la app indicada.
     */
    fun hasActiveSession(packageName: String): Boolean {
        val expiryTime = activeSessions[packageName] ?: return false
        val now = System.currentTimeMillis()
        if (now < expiryTime) {
            return true
        } else {
            // La sesión ha expirado: la eliminamos y requerirá nueva intervención
            activeSessions.remove(packageName)
            return false
        }
    }

    /**
     * Concede una sesión temporal fija (en minutos) para usar la app sin interrupciones.
     */
    fun grantSession(packageName: String, durationMinutes: Int) {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        activeSessions[packageName] = expiry
    }

    /**
     * Concede una prórroga extra tras haber esperado la penalización larga.
     */
    fun grantExtension(packageName: String, extraMinutes: Int = 5) {
        val currentExpiry = activeSessions[packageName] ?: System.currentTimeMillis()
        val base = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
        activeSessions[packageName] = base + (extraMinutes * 60 * 1000L)
    }

    /**
     * Revoca inmediatamente el acceso a una aplicación.
     */
    fun revokeSession(packageName: String) {
        activeSessions.remove(packageName)
    }

    /**
     * Retorna los segundos restantes de la sesión actual o 0 si expiró.
     */
    fun getRemainingSeconds(packageName: String): Long {
        val expiry = activeSessions[packageName] ?: return 0L
        val diff = expiry - System.currentTimeMillis()
        return if (diff > 0) diff / 1000L else 0L
    }
}
