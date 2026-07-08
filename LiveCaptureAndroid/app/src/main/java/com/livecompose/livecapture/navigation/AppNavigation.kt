package com.livecompose.livecapture.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.livecompose.livecapture.features.capture.CaptureScreen
import com.livecompose.livecapture.features.home.GalleryScreen
import com.livecompose.livecapture.features.livecompose.LiveComposeScreen
import com.livecompose.livecapture.features.settings.SettingsScreen
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 应用导航
 * 对应 iOS 的 MainTabView
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val isDark = isSystemInDarkTheme()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCapture by remember { mutableStateOf(false) }

    if (showCapture) {
        CaptureScreen(onBack = { showCapture = false })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                    contentColor = DesignSystem.Colors.primary
                ) {
                    TabItem.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                if (tab == TabItem.CAPTURE) {
                                    showCapture = true
                                } else {
                                    selectedTab = index
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == index) tab.selectedIcon else tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> LiveComposeScreen()
                    1 -> GalleryScreen()
                    3 -> SettingsScreen()
                }
            }
        }
    }
}

enum class TabItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    LIVECOMPOSE("构妙", Icons.Default.Home, Icons.Default.Home),
    GALLERY("图库", Icons.Default.PhotoLibrary, Icons.Default.PhotoLibrary),
    CAPTURE("拍摄", Icons.Default.CameraAlt, Icons.Default.CameraAlt),
    SETTINGS("设置", Icons.Default.Settings, Icons.Default.Settings)
}