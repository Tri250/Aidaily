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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.livecompose.livecapture.features.capture.CaptureScreen
import com.livecompose.livecapture.features.home.GalleryScreen
import com.livecompose.livecapture.features.gallery.PhotoDetailScreen
import com.livecompose.livecapture.features.gallery.CropEditScreen
import com.livecompose.livecapture.features.gallery.PhotoAdjustScreen
import com.livecompose.livecapture.features.livecompose.LiveComposeScreen
import com.livecompose.livecapture.features.settings.SettingsScreen
import com.livecompose.livecapture.ui.design.DesignSystem

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
            // 主 Tab 页面
            when (selectedTab) {
                0 -> LiveComposeScreen(
                    onNavigateToGallery = { selectedTab = 1 }
                )
                1 -> GalleryScreen(
                    onPhotoClick = { photoId ->
                        navController.navigate("photo_detail/$photoId")
                    }
                )
                2 -> CaptureScreen(onBack = { selectedTab = 0 })
                3 -> SettingsScreen()
            }

            // 导航覆盖层（照片详情/编辑）
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            if (navBackStackEntry != null) {
                NavHost(
                    navController = navController,
                    startDestination = "_empty",
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable("_empty") { /* 空占位 */ }

                    composable(
                        route = "photo_detail/{photoId}",
                        arguments = listOf(navArgument("photoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
                        PhotoDetailScreen(
                            photoId = photoId,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate("crop_edit/$id") },
                            onAdjust = { id -> navController.navigate("adjust/$id") }
                        )
                    }

                    composable(
                        route = "crop_edit/{photoId}",
                        arguments = listOf(navArgument("photoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
                        CropEditScreen(
                            photoId = photoId,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "adjust/{photoId}",
                        arguments = listOf(navArgument("photoId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
                        PhotoAdjustScreen(
                            photoId = photoId,
                            onBack = { navController.popBackStack() }
                        )
                    }
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
