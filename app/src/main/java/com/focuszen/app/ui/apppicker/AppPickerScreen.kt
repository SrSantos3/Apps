package com.focuszen.app.ui.apppicker

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.focuszen.app.FocusApp
import com.focuszen.app.data.model.MonitoredApp
import com.focuszen.app.ui.theme.DarkBackground
import com.focuszen.app.ui.theme.DarkBorder
import com.focuszen.app.ui.theme.DarkSurface
import com.focuszen.app.ui.theme.DarkSurfaceElevated
import com.focuszen.app.ui.theme.FocusEmerald
import com.focuszen.app.ui.theme.TextMuted
import com.focuszen.app.ui.theme.TextPrimary
import com.focuszen.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val monitoredApps by FocusApp.repository.allMonitoredApps.collectAsState(initial = emptyList())
    val monitoredMap = remember(monitoredApps) {
        monitoredApps.associateBy { it.packageName }
    }

    var selectedAppToEdit by remember { mutableStateOf<InstalledAppItem?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // Cargar aplicaciones del dispositivo de forma asíncrona
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val apps = resolveInfos
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    InstalledAppItem(
                        packageName = info.activityInfo.packageName,
                        appName = info.loadLabel(pm).toString(),
                        icon = info.loadIcon(pm)
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }

            installedApps = apps
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Elegir Aplicaciones", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            // Buscador
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar aplicación...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FocusEmerald,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando aplicaciones instaladas...", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { appItem ->
                        val existing = monitoredMap[appItem.packageName]
                        val isMonitored = existing?.isEnabled == true

                        AppListItem(
                            app = appItem,
                            isMonitored = isMonitored,
                            dailyLimit = existing?.dailyLimitMinutes ?: 30,
                            onClick = {
                                selectedAppToEdit = appItem
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet para configurar los límites de la app seleccionada
    selectedAppToEdit?.let { target ->
        val existing = monitoredMap[target.packageName]
        var limitSlider by remember { mutableFloatStateOf((existing?.dailyLimitMinutes ?: 30).toFloat()) }

        ModalBottomSheet(
            onDismissRequest = { selectedAppToEdit = null },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Configurar ${target.appName}",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ajusta la cuota máxima diaria y la protección.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Límite diario permitido:", color = TextSecondary)
                    Text(
                        text = if (limitSlider.toInt() == 0) "Solo fricción (sin límite)" else "${limitSlider.toInt()} minutos",
                        color = FocusEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = limitSlider,
                    onValueChange = { limitSlider = it },
                    valueRange = 0f..120f,
                    steps = 23, // Saltos de 5 minutos
                    colors = SliderDefaults.colors(
                        thumbColor = FocusEmerald,
                        activeTrackColor = FocusEmerald,
                        inactiveTrackColor = DarkBorder
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val newApp = MonitoredApp(
                                packageName = target.packageName,
                                appName = target.appName,
                                dailyLimitMinutes = limitSlider.toInt(),
                                frictionSeconds = 10,
                                isEnabled = true
                            )
                            FocusApp.repository.saveMonitoredApp(newApp)
                            selectedAppToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Supervisar esta App", color = DarkBackground, fontWeight = FontWeight.Bold)
                }

                if (existing != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                FocusApp.repository.deleteApp(existing)
                                selectedAppToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1A1A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dejar de supervisar", color = Color(0xFFEF4444))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AppListItem(
    app: InstalledAppItem,
    isMonitored: Boolean,
    dailyLimit: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, if (isMonitored) FocusEmerald.copy(alpha = 0.4f) else DarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la app
            if (app.icon != null) {
                Image(
                    bitmap = app.icon.toBitmap(width = 96, height = 96).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF222634)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (isMonitored) "Supervisada • ${dailyLimit}m límite" else "Toca para supervisar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMonitored) FocusEmerald else TextMuted,
                    fontSize = 12.sp
                )
            }

            if (isMonitored) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(FocusEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
