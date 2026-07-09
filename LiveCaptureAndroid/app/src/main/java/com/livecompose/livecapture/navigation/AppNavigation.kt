package com.livecompose.livecapture.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.CaptureScreen
import com.livecompose.livecapture.features.home.GalleryScreen
import com.livecompose.livecapture.features.gallery.PhotoDetailScreen
import com.livecompose.livecapture.features.gallery.CropEditScreen
import com.livecompose.livecapture.features.gallery.PhotoAdjustScreen
import com.livecompose.livecapture.features.gallery.CurveEditorScreen
import com.livecompose.livecapture.features.gallery.HslAdjustScreen
import com.livecompose.livecapture.features.gallery.VignetteEditorScreen
import com.livecompose.livecapture.features.community.CommunityScreen
import com.livecompose.livecapture.features.compliance.YouthModeScreen
import com.livecompose.livecapture.features.compliance.IcpFilingScreen
import com.livecompose.livecapture.features.settings.SettingsScreen
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 应用主导航 - 以 AI 相机为核心的三 Tab 极简结构
 * 对标 2026 年高端手机摄影应用的交互设计：
 *   - Tab 0: 拍摄（主入口/AI相机，默认首页）
 *   - Tab 1: 图库（作品浏览）
 *   - Tab 2: 设置（个性配置）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DesignSystem.Colors.backgroundSecondary(),
                contentColor = DesignSystem.Colors.primary,
                tonalElevation = 0.dp
            ) {
                TabItem.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) tab.selectedIcon else tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            androidx.compose.material3.Text(
                                text = tab.label,
                                style = DesignSystem.Typography.caption2
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DesignSystem.Colors.primary,
                            selectedTextColor = DesignSystem.Colors.primary,
                            unselectedIconColor = DesignSystem.Colors.textTertiary(),
                            unselectedTextColor = DesignSystem.Colors.textTertiary(),
                            indicatorColor = DesignSystem.Colors.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        // 主 Tab 页面
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> CaptureScreen(
                    onBack = {}, // 主页不提供返回
                    onNavigateToGallery = { selectedTab = 1 }
                )
                1 -> GalleryScreen(
                    onPhotoClick = { photoId ->
                        navController.navigate("photo_detail/$photoId")
                    }
                )
                2 -> SettingsScreen()
            }
        }

        // 导航覆盖层（照片详情/编辑页面）
        NavHost(
            navController = navController,
            startDestination = "_empty"
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
                    onBack = { navController.popBackStack() },
                    onNavigateToCurve = { id -> navController.navigate("curve_edit/$id") },
                    onNavigateToHsl = { id -> navController.navigate("hsl_edit/$id") },
                    onNavigateToVignette = { id -> navController.navigate("vignette_edit/$id") }
                )
            }

            composable(
                route = "curve_edit/{photoId}",
                arguments = listOf(navArgument("photoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
                CurveEditorScreen(
                    photoId = photoId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "hsl_edit/{photoId}",
                arguments = listOf(navArgument("photoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
                HslAdjustScreen(
                    photoId = photoId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "vignette_edit/{photoId}",
                arguments = listOf(navArgument("photoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val photoId = backStackEntry.arguments?.getString("photoId") ?: return@composable
                VignetteEditorScreen(
                    photoId = photoId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("community") {
                CommunityScreen(
                    communityManager = AppContainer.getInstance(LocalContext.current).communityManager,
                    locationRecommender = AppContainer.getInstance(LocalContext.current).locationRecommender
                )
            }

            composable("youth_mode") {
                YouthModeScreen(
                    manager = AppContainer.getInstance(LocalContext.current).youthModeManager,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("icp_filing") {
                IcpFilingScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * 底部导航 Tab 定义 - 精简为 3 项，以 AI 相机为中心
 */
enum class TabItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    CAPTURE("拍摄", Icons.Default.Camera, Icons.Default.Camera),
    GALLERY("图库", Icons.Default.PhotoLibrary, Icons.Default.PhotoLibrary),
    SETTINGS("设置", Icons.Default.Tune, Icons.Default.Tune)
}