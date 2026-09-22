package com.focuszen.app.ui.friction

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuszen.app.FocusApp
import com.focuszen.app.service.FloatingGoalOverlayService
import com.focuszen.app.service.FocusAccessibilityService
import com.focuszen.app.service.SessionManager
import com.focuszen.app.ui.theme.AlertRed
import com.focuszen.app.ui.theme.DarkBorder
import com.focuszen.app.ui.theme.DarkSurface
import com.focuszen.app.ui.theme.DarkSurfaceElevated
import com.focuszen.app.ui.theme.FocusEmerald
import com.focuszen.app.ui.theme.FocusZenTheme
import com.focuszen.app.ui.theme.TextMuted
import com.focuszen.app.ui.theme.TextPrimary
import com.focuszen.app.ui.theme.TextSecondary
import com.focuszen.app.ui.theme.ZenIndigo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FrictionActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_FRICTION_SECONDS = "extra_friction_seconds"
        const val EXTRA_DAILY_LIMIT = "extra_daily_limit"
        const val EXTRA_MINUTES_USED = "extra_minutes_used"
        const val EXTRA_MODE = "extra_mode"

        const val MODE_NORMAL_FRICTION = "normal_friction"
        const val MODE_LIMIT_EXCEEDED = "limit_exceeded"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Esta Aplicación"
        val frictionSeconds = intent.getIntExtra(EXTRA_FRICTION_SECONDS, 10)
        val dailyLimit = intent.getIntExtra(EXTRA_DAILY_LIMIT, 30)
        val minutesUsed = intent.getIntExtra(EXTRA_MINUTES_USED, 0)
        val initialMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_NORMAL_FRICTION

        setContent {
            FocusZenTheme {
                FrictionContent(
                    packageName = targetPackage,
                    appName = appName,
                    initialFrictionSeconds = frictionSeconds,
                    dailyLimit = dailyLimit,
                    minutesUsed = minutesUsed,
                    initialMode = initialMode,
                    onCancelAndGoHome = {
                        vibrateOnce(50)
                        FocusAccessibilityService.goToHomeScreen()
                        finish()
                    },
                    onSessionWithPurposeStarted = { durationMinutes, purpose ->
                        vibrateOnce(80)
                        SessionManager.grantSession(targetPackage, durationMinutes, purpose)
                        // Iniciar la píldora flotante con el propósito definido
                        startFloatingOverlay(targetPackage, purpose)
                        finish()
                    },
                    onExtensionGranted = { extraMinutes ->
                        vibrateOnce(120)
                        SessionManager.grantExtension(targetPackage, extraMinutes)
                        val purpose = SessionManager.getPurpose(targetPackage)
                        startFloatingOverlay(targetPackage, purpose)
                        finish()
                    }
                )
            }
        }
    }

    private fun startFloatingOverlay(packageName: String, purpose: String) {
        val overlayIntent = Intent(this, FloatingGoalOverlayService::class.java).apply {
            action = FloatingGoalOverlayService.ACTION_START_OVERLAY
            putExtra(FloatingGoalOverlayService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(FloatingGoalOverlayService.EXTRA_PURPOSE_TEXT, purpose)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(overlayIntent)
        } else {
            startService(overlayIntent)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateOnce(durationMillis: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(durationMillis)
            }
        } catch (_: Exception) {}
    }
}

enum class FrictionStep {
    COUNTDOWN_BREATHING,
    DEFINE_PURPOSE,
    LIMIT_WARNING,
    EXTENDED_WAIT
}

@Composable
fun FrictionContent(
    packageName: String,
    appName: String,
    initialFrictionSeconds: Int,
    dailyLimit: Int,
    minutesUsed: Int,
    initialMode: String,
    onCancelAndGoHome: () -> Unit,
    onSessionWithPurposeStarted: (Int, String) -> Unit,
    onExtensionGranted: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember {
        mutableStateOf(
            if (initialMode == FrictionActivity.MODE_LIMIT_EXCEEDED) {
                FrictionStep.LIMIT_WARNING
            } else {
                FrictionStep.COUNTDOWN_BREATHING
            }
        )
    }

    var secondsLeft by remember { mutableIntStateOf(initialFrictionSeconds) }
    var extendedSecondsLeft by remember { mutableIntStateOf(60) }

    // Cuenta atrás para los 10 segundos
    LaunchedEffect(currentStep) {
        if (currentStep == FrictionStep.COUNTDOWN_BREATHING) {
            secondsLeft = initialFrictionSeconds
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            currentStep = FrictionStep.DEFINE_PURPOSE
        } else if (currentStep == FrictionStep.EXTENDED_WAIT) {
            extendedSecondsLeft = 60
            while (extendedSecondsLeft > 0) {
                delay(1000L)
                extendedSecondsLeft--
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0B0E))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "FrictionStepTransition"
                ) { step ->
                    when (step) {
                        FrictionStep.COUNTDOWN_BREATHING -> {
                            BreathingStepView(
                                appName = appName,
                                secondsLeft = secondsLeft,
                                onCancel = {
                                    coroutineScope.launch {
                                        FocusApp.repository.recordAttemptAvoided(packageName)
                                        onCancelAndGoHome()
                                    }
                                }
                            )
                        }
                        FrictionStep.DEFINE_PURPOSE -> {
                            DefinePurposeStepView(
                                appName = appName,
                                onCancel = {
                                    coroutineScope.launch {
                                        FocusApp.repository.recordAttemptAvoided(packageName)
                                        onCancelAndGoHome()
                                    }
                                },
                                onConfirm = { selectedMinutes, purposeText ->
                                    coroutineScope.launch {
                                        FocusApp.repository.recordSessionStarted(packageName)
                                        onSessionWithPurposeStarted(selectedMinutes, purposeText)
                                    }
                                }
                            )
                        }
                        FrictionStep.LIMIT_WARNING -> {
                            LimitWarningStepView(
                                appName = appName,
                                dailyLimit = dailyLimit,
                                minutesUsed = minutesUsed,
                                onGoHome = onCancelAndGoHome,
                                onRequestExtension = {
                                    currentStep = FrictionStep.EXTENDED_WAIT
                                }
                            )
                        }
                        FrictionStep.EXTENDED_WAIT -> {
                            ExtendedWaitStepView(
                                appName = appName,
                                secondsRemaining = extendedSecondsLeft,
                                onCancel = onCancelAndGoHome,
                                onGrantExtension = {
                                    coroutineScope.launch {
                                        FocusApp.repository.recordExtensionRequested()
                                        onExtensionGranted(5) // +5 minutos
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Paso 1: Cuenta atrás de 10 segundos con aro de respiración guiada
 */
@Composable
fun BreathingStepView(
    appName: String,
    secondsLeft: Int,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BreathingPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingScale"
    )

    val isExhaling = scale < 1.0f
    val breathText = if (isExhaling) "Exhala despacio..." else "Inhala profundo..."

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¿Realmente quieres entrar?",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Pausa consciente para $appName",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Círculo de respiración con animación pulsante
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(FocusEmerald.copy(alpha = 0.25f), FocusEmerald.copy(alpha = 0.05f))
                    )
                )
                .border(2.dp, FocusEmerald, CircleShape)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$secondsLeft",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "segundos",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = breathText,
            style = MaterialTheme.typography.bodyMedium,
            color = FocusEmerald,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = ZenIndigo),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = TextPrimary)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = "He cambiado de idea (Cerrar)", color = TextPrimary)
        }
    }
}

/**
 * Paso 2 (Versión 2): Definir el propósito consciente antes de entrar.
 * Permite escribir el objetivo libremente o elegir una etiqueta rápida.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DefinePurposeStepView(
    appName: String,
    onCancel: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var purposeInput by remember { mutableStateOf("") }
    var selectedMinutes by remember { mutableIntStateOf(10) }

    val quickTags = listOf(
        "💬 Responder mensaje",
        "🔍 Buscar tutorial",
        "💼 Asunto de trabajo",
        "📺 Vídeo concreto",
        "📅 Revisar evento"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎯 ¿Cuál es tu objetivo?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Mantendremos tu objetivo como una píldora flotante para que no te distraigas en $appName.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo para escribir libremente
        OutlinedTextField(
            value = purposeInput,
            onValueChange = { purposeInput = it },
            placeholder = { Text("Escribe tu objetivo aquí...", color = TextMuted, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FocusEmerald,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Etiquetas rápidas en un solo toque
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTags.forEach { tag ->
                val isSelected = purposeInput == tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) FocusEmerald.copy(alpha = 0.25f) else Color(0xFF1A1D27))
                        .border(
                            1.dp,
                            if (isSelected) FocusEmerald else DarkBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { purposeInput = tag }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 11.sp,
                        color = if (isSelected) FocusEmerald else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de duración
        Text(
            text = "Tiempo de sesión:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5, 10, 15, 20).forEach { mins ->
                val isSelected = selectedMinutes == mins
                Button(
                    onClick = { selectedMinutes = mins },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) FocusEmerald else Color(0xFF1A1D27)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${mins}m",
                        color = if (isSelected) Color(0xFF0A0B0E) else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Botón principal de entrada con propósito
        Button(
            onClick = {
                val finalPurpose = if (purposeInput.isBlank()) "Mantener enfoque" else purposeInput.trim()
                onConfirm(selectedMinutes, finalPurpose)
            },
            colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Entrar con este objetivo 🎯",
                color = Color(0xFF0A0B0E),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cancelar y no entrar", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

/**
 * Vista mostrada cuando se ha superado el límite diario por aplicación
 */
@Composable
fun LimitWarningStepView(
    appName: String,
    dailyLimit: Int,
    minutesUsed: Int,
    onGoHome: () -> Unit,
    onRequestExtension: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Block,
            contentDescription = null,
            tint = AlertRed,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Límite Diario Agotado",
            style = MaterialTheme.typography.titleLarge,
            color = AlertRed,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Has alcanzado tu cuota de $dailyLimit min hoy en $appName ($minutesUsed min consumidos).",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onGoHome,
            colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cerrar y seguir enfocado", color = Color(0xFF0A0B0E), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onRequestExtension,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Solicitar prórroga (+5 min con espera de 60s)", color = TextMuted, fontSize = 13.sp)
        }
    }
}

/**
 * Vista de prórroga con espera larga (60s) para erradicar el impulso inmediato
 */
@Composable
fun ExtendedWaitStepView(
    appName: String,
    secondsRemaining: Int,
    onCancel: () -> Unit,
    onGrantExtension: () -> Unit
) {
    val canProceed = secondsRemaining <= 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.HourglassTop,
            contentDescription = null,
            tint = ZenIndigo,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Espera de Reflexión",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Para evitar el acceso impulsivo, reflexiona durante este minuto si realmente necesitas $appName.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF181B26))
                .border(2.dp, if (canProceed) FocusEmerald else ZenIndigo, CircleShape)
        ) {
            Text(
                text = if (canProceed) "✓" else "${secondsRemaining}s",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = if (canProceed) FocusEmerald else TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onGrantExtension,
            enabled = canProceed,
            colors = ButtonDefaults.buttonColors(
                containerColor = FocusEmerald,
                disabledContainerColor = Color(0xFF1E2330)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = if (canProceed) "Acceder por 5 minutos" else "Espera ${secondsRemaining}s...",
                color = if (canProceed) Color(0xFF0A0B0E) else TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cancelar y salir", color = TextSecondary)
        }
    }
}
