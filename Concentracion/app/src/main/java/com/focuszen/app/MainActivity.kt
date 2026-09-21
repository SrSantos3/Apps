package com.focuszen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.focuszen.app.ui.apppicker.AppPickerScreen
import com.focuszen.app.ui.dashboard.DashboardScreen
import com.focuszen.app.ui.permissions.PermissionGuideScreen
import com.focuszen.app.ui.settings.SettingsScreen
import com.focuszen.app.ui.theme.DarkBackground
import com.focuszen.app.ui.theme.FocusZenTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FocusZenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    FocusZenAppNavigation()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AppPicker : Screen("app_picker")
    object Settings : Screen("settings")
    object Permissions : Screen("permissions")
}

@Composable
fun FocusZenAppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAppPicker = { navController.navigate(Screen.AppPicker.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) }
            )
        }

        composable(Screen.AppPicker.route) {
            AppPickerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Permissions.route) {
            PermissionGuideScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
