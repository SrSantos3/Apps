package com.focuszen.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuszen.app.FocusApp
import com.focuszen.app.data.model.DailyStat
import com.focuszen.app.data.model.MonitoredApp
import com.focuszen.app.service.FocusAccessibilityService
import com.focuszen.app.service.UsageStatsHelper
import com.focuszen.app.ui.theme.AlertAmber
import com.focuszen.app.ui.theme.DarkBackground
import com.focuszen.app.ui.theme.DarkBorder
import com.focuszen.app.ui.theme.DarkSurface
import com.focuszen.app.ui.theme.DarkSurfaceElevated
import com.focuszen.app.ui.theme.FocusEmerald
import com.focuszen.app.ui.theme.TextMuted
import com.focuszen.app.ui.theme.TextPrimary
import com.focuszen.app.ui.theme.TextSecondary
import com.focuszen.app.ui.theme.ZenIndigo
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    onNavigateToAppPicker: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val monitoredApps by FocusApp.repository.allMonitoredApps.collectAsState(initial = emptyList())
    val todayStat by FocusApp.repository.getTodayStatFlow().collectAsState(initial = null)
    val isStrict by FocusApp.repository.preferencesManager.isStrictModeEnabled.collectAsState(initial = true)

    val isServiceActive = FocusAccessibilityService.isServiceRunning()
    val hasUsagePermission = UsageStatsHelper.hasUsageStatsPermission(context)

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAppPicker,
                containerColor = FocusEmerald,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir apps")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                DashboardHeader(
                    isStrict = isStrict,
                    onSettingsClick = onNavigateToSettings
                )
            }

            // Alerta si falta algún permiso esencial
            if (!isServiceActive || !hasUsagePermission) {
                item {
                    PermissionAlertBanner(
                        isServiceActive = isServiceActive,
                        hasUsagePermission = hasUsagePermission,
                        onFixClick = onNavigateToPermissions
                    )
                }
            }

            // Tarjetas de Métricas Principales (Stats)
            item {
                MetricsSection(todayStat = todayStat)
            }

            // Título de la sección de Apps Supervisadas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apps Supervisadas (${monitoredApps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "+ Añadir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FocusEmerald,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToAppPicker() }
                    )
                }
            }

            // Lista de Apps Supervisadas
            if (monitoredApps.isEmpty()) {
                item {
                    EmptyAppsPlaceholder(onAddApps = onNavigateToAppPicker)
                }
            } else {
                items(monitoredApps, key = { it.packageName }) { app ->
                    val minutesUsed = UsageStatsHelper.getMinutesUsedToday(context, app.packageName)
                    MonitoredAppCard(
                        app = app,
                        minutesUsedToday = minutesUsed,
                        onToggle = { isChecked ->
                            coroutineScope.launch {
                                FocusApp.repository.toggleAppEnabled(app.packageName, isChecked)
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun DashboardHeader(
    isStrict: Boolean,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "FocusZen",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isStrict) FocusEmerald else AlertAmber)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isStrict) "Modo Estricto Activo" else "Modo Flexible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Ajustes",
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun PermissionAlertBanner(
    isServiceActive: Boolean,
    hasUsagePermission: Boolean,
    onFixClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AlertAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A11)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = AlertAmber,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permisos requeridos",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Se necesita activar el Servicio de Accesibilidad para interceptar las apps.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onFixClick,
                colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Activar", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MetricsSection(todayStat: DailyStat?) {
    val avoided = todayStat?.attemptsAvoided ?: 0
    val minutesSaved = todayStat?.estimatedMinutesSaved ?: 0
    val hoursSaved = minutesSaved / 60
    val remainingMinutes = minutesSaved % 60
    val savedString = if (hoursSaved > 0) "${hoursSaved}h ${remainingMinutes}m" else "${minutesSaved}m"

    val totalDecisions = (todayStat?.attemptsAvoided ?: 0) + (todayStat?.sessionsStarted ?: 0)
    val focusScore = if (totalDecisions > 0) {
        ((avoided.toFloat() / totalDecisions) * 100).toInt()
    } else {
        100
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Impulsos Frenados",
            value = "$avoided",
            subtitle = "Intentos evitados hoy",
            icon = Icons.Default.Shield,
            iconTint = FocusEmerald,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Tiempo Ahorrado",
            value = savedString,
            subtitle = "Estimado de scroll",
            icon = Icons.Default.Timer,
            iconTint = ZenIndigo,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Puntuación de Enfoque Hoy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "$focusScore%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (focusScore >= 75) FocusEmerald else ZenIndigo
                )
            }
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = if (focusScore >= 75) FocusEmerald else ZenIndigo,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, DarkBorder, RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 13.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun MonitoredAppCard(
    app: MonitoredApp,
    minutesUsedToday: Int,
    onToggle: (Boolean) -> Unit
) {
    val progress = if (app.dailyLimitMinutes > 0) {
        (minutesUsedToday.toFloat() / app.dailyLimitMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val isExceeded = app.dailyLimitMinutes > 0 && minutesUsedToday >= app.dailyLimitMinutes

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isExceeded) AlertAmber.copy(alpha = 0.6f) else DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${app.frictionSeconds}s de pausa | ${app.opensToday} aperturas hoy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = app.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = FocusEmerald,
                        checkedTrackColor = FocusEmerald.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurface
                    )
                )
            }

            if (app.dailyLimitMinutes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Uso hoy: ${minutesUsedToday}m / ${app.dailyLimitMinutes}m",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = if (isExceeded) AlertAmber else TextSecondary
                    )
                    Text(
                        text = if (isExceeded) "¡Límite alcanzado!" else "${app.dailyLimitMinutes - minutesUsedToday}m restantes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = if (isExceeded) AlertAmber else FocusEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isExceeded) AlertAmber else FocusEmerald,
                    trackColor = Color(0xFF222633),
                )
            }
        }
    }
}

@Composable
fun EmptyAppsPlaceholder(onAddApps: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No tienes apps supervisadas",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selecciona las apps que más te distraen (Instagram, TikTok, YouTube) para aplicar la pausa de 10 segundos.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddApps,
                colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Seleccionar Apps", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}
