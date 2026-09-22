package com.focuszen.app.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuszen.app.FocusApp
import com.focuszen.app.ui.theme.DarkBackground
import com.focuszen.app.ui.theme.DarkBorder
import com.focuszen.app.ui.theme.DarkSurfaceElevated
import com.focuszen.app.ui.theme.FocusEmerald
import com.focuszen.app.ui.theme.TextMuted
import com.focuszen.app.ui.theme.TextPrimary
import com.focuszen.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val prefs = FocusApp.repository.preferencesManager

    val isStrict by prefs.isStrictModeEnabled.collectAsState(initial = true)
    val frictionSeconds by prefs.defaultFrictionSeconds.collectAsState(initial = 10)
    val extensionDelaySeconds by prefs.extensionDelaySeconds.collectAsState(initial = 60)
    val isVibrationEnabled by prefs.isVibrationEnabled.collectAsState(initial = true)

    val frictionOptions = listOf(5, 10, 15)
    val extensionOptions = listOf(60, 90, 120)

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes & Disciplina", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección Modo Estricto (Anti-Trampas)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = FocusEmerald)
                                Spacer(modifier = Modifier.padding(start = 10.dp))
                                Column {
                                    Text(
                                        text = "Modo Estricto Anti-Trampas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Protege contra la desactivación impulsiva en los Ajustes del teléfono.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Switch(
                                checked = isStrict,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch { prefs.setStrictMode(enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = FocusEmerald,
                                    checkedTrackColor = FocusEmerald.copy(alpha = 0.3f),
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkBackground
                                )
                            )
                        }
                    }
                }
            }

            // Duración de la Pausa Consciente (Fricción)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = FocusEmerald)
                            Spacer(modifier = Modifier.padding(start = 10.dp))
                            Text(
                                text = "Pausa Consciente antes de Entrar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tiempo obligatorio de respiración y reflexión al pulsar una app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            frictionOptions.forEachIndexed { index, sec ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = frictionOptions.size),
                                    onClick = {
                                        coroutineScope.launch { prefs.setDefaultFrictionSeconds(sec) }
                                    },
                                    selected = frictionSeconds == sec,
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = FocusEmerald,
                                        activeContentColor = DarkBackground,
                                        inactiveContainerColor = DarkBackground,
                                        inactiveContentColor = TextSecondary
                                    ),
                                    label = { Text("${sec}s") }
                                )
                            }
                        }
                    }
                }
            }

            // Tiempo de espera para solicitar Prórrogas (Espera larga)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Espera Penalizada para Prórrogas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Segundos de espera obligatoria sin tocar la pantalla para pedir 5 min extra cuando se agotó el cupo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            extensionOptions.forEachIndexed { index, sec ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = extensionOptions.size),
                                    onClick = {
                                        coroutineScope.launch { prefs.setExtensionDelaySeconds(sec) }
                                    },
                                    selected = extensionDelaySeconds == sec,
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = FocusEmerald,
                                        activeContentColor = DarkBackground,
                                        inactiveContainerColor = DarkBackground,
                                        inactiveContentColor = TextSecondary
                                    ),
                                    label = { Text("${sec}s") }
                                )
                            }
                        }
                    }
                }
            }

            // Vibración y Respuesta Háptica
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = FocusEmerald)
                            Spacer(modifier = Modifier.padding(start = 10.dp))
                            Column {
                                Text(
                                    text = "Respuesta Háptica",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Vibración suave durante la inhalación y exhalación de los 10s.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = isVibrationEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch { prefs.setVibrationEnabled(enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FocusEmerald,
                                checkedTrackColor = FocusEmerald.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBackground
                            )
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
