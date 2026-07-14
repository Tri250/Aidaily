package com.livecompose.livecapture.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.livecompose.livecapture.presentation.capture.CaptureView
import com.livecompose.livecapture.presentation.compose.LiveComposeView
import com.livecompose.livecapture.presentation.home.HomeView
import com.livecompose.livecapture.presentation.settings.SettingsView

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object LiveCompose : Screen("compose", "构妙", Icons.Default.Palette)
    data object Home : Screen("home", "图库", Icons.Default.Home)
    data object Capture : Screen("capture", "拍摄", Icons.Default.CameraAlt)
}

@Composable
fun MainTabView() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.LiveCompose,
        Screen.Home,
        Screen.Capture
    )

    var showSettingsDialog by remember { mutableStateOf(false) }

    // 拍摄页隐藏导航栏 — 沉浸式拍摄体验
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isCapturePage = currentDestination?.hierarchy?.any { it.route == Screen.Capture.route } == true

    Scaffold(
        bottomBar = {
            // 拍摄页自动隐藏导航栏，其他页面显示带动画
            AnimatedVisibility(
                visible = !isCapturePage,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Capture.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.LiveCompose.route) {
                LiveComposeView(onSettingsClick = { showSettingsDialog = true })
            }
            composable(Screen.Home.route) {
                HomeView(navController = navController, onSettingsClick = { showSettingsDialog = true })
            }
            composable(Screen.Capture.route) {
                CaptureView(navController = navController, onSettingsClick = { showSettingsDialog = true })
            }
        }
    }

    if (showSettingsDialog) {
        Dialog(
            onDismissRequest = { showSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                SettingsView(onDismiss = { showSettingsDialog = false })
            }
        }
    }
}
