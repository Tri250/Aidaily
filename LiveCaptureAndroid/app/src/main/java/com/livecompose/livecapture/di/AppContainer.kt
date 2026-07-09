package com.livecompose.livecapture.di

import android.content.Context
import com.livecompose.livecapture.core.camera.CameraManager
import com.livecompose.livecapture.core.camera.ProCameraManager
import com.livecompose.livecapture.core.composition.LevelMonitor
import com.livecompose.livecapture.core.editing.AIEditViewModel
import com.livecompose.livecapture.core.editing.AutoEnhancer
import com.livecompose.livecapture.core.editing.BatchProcessor
import com.livecompose.livecapture.core.community.CommunityManager
import com.livecompose.livecapture.core.community.LocationRecommender
import com.livecompose.livecapture.core.compliance.YouthModeManager
import com.livecompose.livecapture.core.errorhandling.AppErrorHandler
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.SkinProtectionFilter
import com.livecompose.livecapture.core.intelligence.ImageQualityAssessor
import com.livecompose.livecapture.core.intelligence.PoseRecommendationEngine
import com.livecompose.livecapture.core.intelligence.SceneIntelligenceEngine
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.lut.AiColorMatcher
import com.livecompose.livecapture.core.lut.LutImporter
import com.livecompose.livecapture.core.motion.MotionStabilityMonitor
import com.livecompose.livecapture.core.performance.MemoryMonitor
import com.livecompose.livecapture.core.storage.PhotoSearchEngine
import com.livecompose.livecapture.core.storage.PhotoStorageService
import com.livecompose.livecapture.core.storage.SmartAlbumClassifier
import com.livecompose.livecapture.core.video.VideoViewModel

/**
 * 轻量级 DI 容器
 * 管理应用级单例对象，替代 Hilt 依赖注入
 */
class AppContainer(context: Context) {

    companion object {
        private const val TAG = "AppContainer"

        @Volatile
        private var instance: AppContainer? = null

        /**
         * 获取应用级单例，如果不存在则创建
         */
        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
        }
    }

    private val applicationContext = context.applicationContext
        ?: throw IllegalArgumentException("Application context is required")

    // MARK: - 相机与视频

    /** 相机管理器 */
    val cameraManager by lazy {
        CameraManager(applicationContext)
    }

    /** 专业相机模式管理器 */
    val proCameraManager by lazy {
        ProCameraManager(applicationContext)
    }

    /** 视频录制视图模型 */
    val videoViewModel by lazy {
        VideoViewModel(applicationContext)
    }

    // MARK: - 运动与构图

    /** 运动稳定性监控器 */
    val motionMonitor by lazy {
        MotionStabilityMonitor(applicationContext)
    }

    /** 水平仪监控器（构图辅助） */
    val levelMonitor by lazy {
        LevelMonitor(applicationContext)
    }

    // MARK: - 智能与编辑

    /** 场景智能引擎 */
    val sceneIntelligenceEngine by lazy {
        SceneIntelligenceEngine(applicationContext)
    }

    /** 自动增强器（AI 编辑） */
    val autoEnhancer by lazy {
        AutoEnhancer()
    }

    /** 批量处理器 */
    val batchProcessor by lazy {
        BatchProcessor(photoStorageService, applicationContext)
    }

    /** AI 编辑视图模型 */
    val aiEditViewModel by lazy {
        AIEditViewModel(applicationContext)
    }

    /** AI 仿色匹配器 */
    val aiColorMatcher by lazy {
        AiColorMatcher
    }

    /** 姿势推荐引擎 */
    val poseRecommendationEngine by lazy {
        PoseRecommendationEngine.create()
    }

    /** 图像质量评估器 */
    val imageQualityAssessor by lazy {
        ImageQualityAssessor()
    }

    // MARK: - 存储与错误处理

    /** 照片存储服务 */
    val photoStorageService by lazy {
        PhotoStorageService(applicationContext)
    }

    /** 智能相册分类器 */
    val smartAlbumClassifier by lazy {
        SmartAlbumClassifier(photoStorageService)
    }

    /** 照片搜索引擎 */
    val photoSearchEngine by lazy {
        PhotoSearchEngine(photoStorageService)
    }

    /** AI 滤镜推荐器 */
    val aiFilterRecommender by lazy {
        AiFilterRecommender()
    }

    /** 皮肤保护滤镜 */
    val skinProtectionFilter by lazy {
        SkinProtectionFilter()
    }

    /** LUT 导入器 */
    val lutImporter by lazy {
        LutImporter(applicationContext)
    }

    /** 内存监控器 */
    val memoryMonitor by lazy {
        MemoryMonitor(applicationContext)
    }

    // MARK: - 社区与合规

    /** 社区管理器（照片挑战 + 滤镜社区） */
    val communityManager by lazy {
        CommunityManager(applicationContext)
    }

    /** 拍照地点推荐器 */
    val locationRecommender by lazy {
        LocationRecommender()
    }

    /** 青少年模式管理器 */
    val youthModeManager by lazy {
        YouthModeManager(applicationContext)
    }

    /** 全局错误处理器 */
    val errorHandler by lazy {
        AppErrorHandler()
    }

    /**
     * 清理所有资源
     */
    fun destroy() {
        try {
            levelMonitor.stopMonitoring()
            motionMonitor.stop()
            memoryMonitor.dispose()
            // VideoViewModel 的 onCleared 会在 ViewModel 销毁时调用，此处确保音频/录制资源释放
            videoViewModel.stopRecording()
            aiEditViewModel.onCleared()
            cameraManager.destroy()
            lutImporter.dispose()
            communityManager.dispose()
            youthModeManager.dispose()
        } catch (e: Exception) {
            AppLogger.w(TAG, "清理资源时发生异常", e)
        }
    }
}