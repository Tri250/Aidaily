package com.livecompose.livecapture.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.CaptureScreen
import com.livecompose.livecapture.features.gallery.PhotoDetailScreen
import com.livecompose.livecapture.features.gallery.CropEditScreen
import com.livecompose.livecapture.features.gallery.PhotoAdjustScreen
import com.livecompose.livecapture.features.gallery.CurveEditorScreen
import com.livecompose.livecapture.features.gallery.HslAdjustScreen
import com.livecompose.livecapture.features.gallery.VignetteEditorScreen
import com.livecompose.livecapture.features.community.CommunityScreen
import com.livecompose.livecapture.features.compliance.YouthModeScreen
import com.livecompose.livecapture.features.compliance.IcpFilingScreen
import com.livecompose.livecapture.features.compliance.PrivacyPolicyScreen
import com.livecompose.livecapture.features.compliance.UserAgreementScreen
import com.livecompose.livecapture.features.onboarding.ShootingGuideScreen

/**
 * 应用主导航 - 单屏拍摄为核心，设置/图库集成到拍摄页面
 * 对标 2026 年国内旗舰手机摄影应用：
 *   - 拍摄为主界面，无底部 Tab
 *   - 图库：拍摄页底部缩略条 + 全屏浮层
 *   - 设置：拍摄页顶部图标 → 底部浮层
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        // 主拍摄页面
        CaptureScreen(
            onBack = {},
            onNavigateToPhotoDetail = { photoId ->
                navController.navigate("photo_detail/$photoId")
            },
            onNavigateToShootingGuide = { navController.navigate("shooting_guide") },
            onNavigateToPrivacy = { navController.navigate("privacy") },
            onNavigateToAgreement = { navController.navigate("agreement") },
            onNavigateToCommunity = { navController.navigate("community") },
            onNavigateToIcp = { navController.navigate("icp_filing") }
        )

        // 导航覆盖层（照片详情/编辑/社区/合规等页面）
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

            composable("shooting_guide") {
                ShootingGuideScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("privacy") {
                PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable("agreement") {
                UserAgreementScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}