package com.livecompose.livecapture.features.capture.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.core.camera.CameraErrorType
import com.livecompose.livecapture.core.intelligence.SceneType
import com.livecompose.livecapture.features.capture.CaptureViewModel

/**
 * 拍摄智能信息覆盖层
 *
 * 整合 AI 场景识别、胶片模拟标签与拍摄上下文面板，
 * 统一摆放在预览区合适位置，避免 CaptureScreen 过度膨胀。
 */
@Composable
fun BoxScope.CaptureIntelligenceOverlay(
    viewModel: CaptureViewModel,
    aiSceneName: String,
    aiSceneType: SceneType,
    hyperfocalEnabled: Boolean,
    selectedMode: CaptureMode,
    cameraError: CameraErrorType? = null
) {
    // AI 场景识别标签
    if (aiSceneName.isNotEmpty() && aiSceneType != SceneType.UNKNOWN && cameraError == null) {
        AiSceneLabel(
            sceneName = aiSceneName,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        )
    }

    // 胶片模拟标签 - 场景标签下方
    if (cameraError == null) {
        val selectedPresetName = viewModel.selectedPreset.collectAsState().value?.name
        val filmBadge = selectedPresetName?.let { filmBadgeFromPresetName(it) }
        if (filmBadge != null) {
            FilmBadge(
                film = filmBadge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = if (aiSceneName.isNotEmpty() && aiSceneType != SceneType.UNKNOWN) 40.dp else 8.dp
                    )
            )
        }
    }

    // 拍摄上下文面板 - 天气/农历/宜忌/健康
    if (cameraError == null && selectedMode != CaptureMode.VIDEO && selectedMode != CaptureMode.TIMELAPSE) {
        val contextInfo = remember { defaultCaptureContext() }
        ContextInfoPanel(
            info = contextInfo,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = if (hyperfocalEnabled) 56.dp else 16.dp,
                    start = 16.dp
                )
        )
    }
}
