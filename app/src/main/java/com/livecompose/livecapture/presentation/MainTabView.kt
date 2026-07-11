package com.livecompose.livecapture.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.livecompose.livecapture.R
import com.livecompose.livecapture.presentation.capture.CaptureView
import com.livecompose.livecapture.presentation.compose.LiveComposeView
import com.livecompose.livecapture.presentation.compliance.PersonalInfoDeclaration
import com.livecompose.livecapture.presentation.compliance.PrivacyPolicyView
import com.livecompose.livecapture.presentation.compliance.UserAgreementView
import com.livecompose.livecapture.presentation.editor.PhotoEditorView
import com.livecompose.livecapture.presentation.feedback.FeedbackView
import com.livecompose.livecapture.presentation.home.HomeView
import com.livecompose.livecapture.presentation.settings.SettingsView
import java.net.URLDecoder

sealed class Screen(val route: String, @StringRes val labelResId: Int, val icon: ImageVector) {
    data object LiveCompose : Screen("compose", R.string.tab_compose, Icons.Default.Palette)
    data object Home : Screen("home", R.string.tab_home, Icons.Default.Home)
    data object Capture : Screen("capture", R.string.tab_capture, Icons.Default.CameraAlt)
    data object Settings : Screen("settings", R.string.tab_settings, Icons.Default.Settings)
}

@Composable
fun MainTabView() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.LiveCompose,
        Screen.Home,
        Screen.Capture,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelResId)) },
                        label = { Text(stringResource(screen.labelResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Capture.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.LiveCompose.route) { LiveComposeView(navController = navController) }
            composable(Screen.Home.route) { HomeView(navController = navController) }
            composable(Screen.Capture.route) { CaptureView(navController = navController) }
            composable(Screen.Settings.route) { SettingsView(navController = navController) }
            composable("privacy_policy") { PrivacyPolicyView(onBack = { navController.popBackStack() }) }
            composable("user_agreement") { UserAgreementView(onBack = { navController.popBackStack() }) }
            composable("personal_info_declaration") { PersonalInfoDeclaration(onBack = { navController.popBackStack() }) }
            composable("feedback") { FeedbackView(onNavigateBack = { navController.popBackStack() }) }
            composable(
                route = "photo_editor/{photoPath}",
                arguments = listOf(navArgument("photoPath") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("photoPath") ?: ""
                val photoPath = URLDecoder.decode(encodedPath, "UTF-8")
                PhotoEditorView(
                    photoPath = photoPath,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
