package com.focuszen.app.ui.friction

import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.focuszen.app.service.FocusAccessibilityService
import com.focuszen.app.service.SessionManager
import com.focuszen.app.ui.theme.AlertRed
import com.focuszen.app.ui.theme.DarkBackground
import com.focuszen.app.ui.theme.DarkBorder
import com.focuszen.app.ui.theme.DarkSurface
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
                    onSessionSelected = { durationMinutes ->
                        vibrateOnce(80)
                        SessionManager.grantSession(targetPackage, durationMinutes)
                        finish()
                    },
                    onExtensionGranted = { extraMinutes ->
                        vibrateOnce(120)
                        SessionManager.grantExtension(targetPackage, extraMinutes)
                        finish()
                    }
                )
            }
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
    CHOOSE_SESSION,
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
    onSessionSelected: (Int) -> Unit,
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
            currentStep = FrictionStep.CHOOSE_SESSION
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
            .padding(24.dp),
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
                    .padding(28.dp),
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
                                totalSeconds = initialFrictionSeconds,
                                onCancel = {
                                    coroutineScope.launch {
                                        FocusApp.repository.recordAttemptAvoided(packageName)
                                        onCancelAndGoHome()
                                    }
                                }
                            )
                        }
                        FrictionStep.CHOOSE_SESSION -> {
                            ChooseSessionStepView(
                                appName = appName,
                                onCancel = {
                                    coroutineScope.launch {
                                        FocusApp.repository.recordAttemptAvoided(packageName)
                                        onCancelAndGoHome()
                                    }
                                },
                                onConfirmSession = { minutes ->
                                    coroutineScope.launch {
                                        FocusApp.repository.recordSessionStarted(packageName)
                                        onSessionSelected(minutes)
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
 * Vista de los 10 segundos de cuenta atrás con animación de respiración guiada
 */
@Composable
fun BreathingStepView(
    appName: String,
    secondsLeft: Int,
    totalSeconds: Int,
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
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Pausa consciente para $appName",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Círculo de respiración con animación pulsante
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(150.dp)
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
                    fontSize = 44.sp,
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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = breathText,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = FocusEmerald,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(28.dp))

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
 * Vista mostrada tras los 10 segundos para fijar el tiempo de sesión antes de reintervenir
 */
@Composable
fun ChooseSessionStepView(
    appName: String,
    onCancel: () -> Unit,
    onConfirmSession: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Timer,
            contentDescription = null,
            tint = FocusEmerald,
            modifier = Modifier.size(40.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Tiempo de sesión",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "¿Cuánto tiempo vas a usar $appName antes de que volvamos a intervenir?",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SessionTimeButton(minutes = 5, modifier = Modifier.weight(1f)) { onConfirmSession(5) }
            SessionTimeButton(minutes = 10, modifier = Modifier.weight(1f)) { onConfirmSession(10) }
            SessionTimeButton(minutes = 15, modifier = Modifier.weight(1f)) { onConfirmSession(15) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SessionTimeButton(minutes = 20, modifier = Modifier.weight(1f)) { onConfirmSession(20) }
            SessionTimeButton(minutes = 30, modifier = Modifier.weight(1f)) { onConfirmSession(30) }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Mejor no entrar (Volver)", color = TextSecondary)
        }
    }
}

@Composable
fun SessionTimeButton(
    minutes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2330)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = "${minutes}m",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Límite Diario Agotado",
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = AlertRed,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Has alcanzado tu cuota de $dailyLimit min hoy en $appName ($minutesUsed min consumidos).",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onGoHome,
            colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cerrar y seguir enfocado", color = DarkBackground, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Para evitar el acceso impulsivo, reflexiona durante este minuto si realmente necesitas $appName.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Color(0xFF181B26))
                .border(2.dp, if (canProceed) FocusEmerald else ZenIndigo, CircleShape)
        ) {
            Text(
                text = if (canProceed) "✓" else "${secondsRemaining}s",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (canProceed) FocusEmerald else TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                color = if (canProceed) DarkBackground else TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cancelar y salir", color = TextSecondary)
        }
    }
}
