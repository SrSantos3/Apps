package com.focuszen.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa una aplicación instalada que el usuario ha seleccionado para supervisar.
 */
@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 30,      // Límite diario individual en minutos (0 = sin límite numérico, solo fricción)
    val frictionSeconds: Int = 10,         // Segundos de pausa consciente requeridos antes de entrar
    val isEnabled: Boolean = true,         // Si la supervisión está activa para esta app
    val isStrict: Boolean = true,          // Si aplica modo estricto para esta app
    val opensToday: Int = 0,               // Contador de aperturas en el día de hoy
    val lastOpenedTimestamp: Long = 0L     // Última vez que se accedió
)
