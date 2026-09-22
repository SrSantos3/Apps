package com.focuszen.app.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Gestiona las sesiones temporales concedidas a aplicaciones tras completar la fricción consciente
 * y almacena el propósito deliberado definido por el usuario para la Versión 2.
 */
object SessionManager {

    // Mapa de packageName -> Timestamp en milisegundos en que expira la sesión concedida
    private val activeSessions = ConcurrentHashMap<String, Long>()

    // Mapa de packageName -> Propósito fijado por el usuario (ej. "Responder a Carlos")
    private val sessionPurposes = ConcurrentHashMap<String, String>()

    // Paquete actualmente activo con propósito flotante
    @Volatile
    var currentActivePackage: String? = null
        private set

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
            revokeSession(packageName)
            return false
        }
    }

    /**
     * Concede una sesión temporal fija (en minutos) con un propósito deliberado.
     */
    fun grantSession(packageName: String, durationMinutes: Int, purpose: String = "Mantener enfoque") {
        val expiry = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        activeSessions[packageName] = expiry
        sessionPurposes[packageName] = purpose
        currentActivePackage = packageName
    }

    /**
     * Retorna el propósito fijado para la aplicación indicada.
     */
    fun getPurpose(packageName: String): String {
        return sessionPurposes[packageName] ?: "Mi Objetivo"
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
     * Revoca inmediatamente el acceso y borra el propósito.
     */
    fun revokeSession(packageName: String) {
        activeSessions.remove(packageName)
        sessionPurposes.remove(packageName)
        if (currentActivePackage == packageName) {
            currentActivePackage = null
        }
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
