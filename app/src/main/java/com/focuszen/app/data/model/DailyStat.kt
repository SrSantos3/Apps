package com.focuszen.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registra las métricas agregadas por día para mostrar en el Dashboard de estadísticas.
 */
@Entity(tableName = "daily_stats")
data class DailyStat(
    @PrimaryKey
    val date: String,                  // Formato: "yyyy-MM-dd"
    val attemptsAvoided: Int = 0,      // Cuántas veces el usuario canceló la entrada tras los 10 segundos
    val sessionsStarted: Int = 0,      // Cuántas veces el usuario decidió continuar
    val estimatedMinutesSaved: Int = 0, // Minutos estimados ahorrados (cada intento evitado ~ 15 min de scroll)
    val extensionsRequested: Int = 0   // Cuántas prórrogas con espera larga solicitó
)
