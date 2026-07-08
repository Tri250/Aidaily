package com.livecompose.livecapture.core.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.livecompose.livecapture.core.intelligence.SceneType
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/**
 * 智能相册分类器
 *
 * 对应 iOS 端 SmartAlbumClassifier.swift，使用 ML Kit（替代 Vision）实现照片场景分类、
 * 日期分组、位置分组和人脸聚类。
 *
 * ## 主要功能
 * - [classifyPhoto] 使用 ML Kit Image Labeling 对单张照片进行场景分类
 * - [groupByScene] 按场景类型分组照片
 * - [groupByDate] 按日期分组照片（今天、昨天、本周、本月、更早）
 * - [groupByLocation] 按位置分组照片（基于 EXIF GPS）
 * - [groupByFaces] 按人脸聚类分组照片（使用 ML Kit Face Detection）
 *
 * @param context 上下文，用于定位照片文件
 * @param photoStorage 照片存储服务，负责照片文件读写
 */
class SmartAlbumClassifier(
    private val photoStorage: PhotoStorageService
) {

    companion object {
        private const val TAG = "SmartAlbumClassifier"
        private const val MIN_LABEL_CONFIDENCE = 0.6f
    }

    /** ML Kit 图像标签器（替代 iOS VNClassifyImageRequest） */
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(MIN_LABEL_CONFIDENCE)
            .build()
    )

    /** ML Kit 人脸检测器（替代 iOS VNDetectFaceRectanglesRequest） */
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()
    )

    /** Vision 场景标签到 SceneType 的映射表（英文标签 → 场景类型） */
    private val sceneLabelMapping: Map<String, SceneType> = mapOf(
        "portrait" to SceneType.PORTRAIT,
        "human" to SceneType.PORTRAIT,
        "people" to SceneType.PORTRAIT,
        "person" to SceneType.PORTRAIT,
        "selfie" to SceneType.SELFIE,
        "food" to SceneType.FOOD,
        "cuisine" to SceneType.FOOD,
        "meal" to SceneType.FOOD,
        "dish" to SceneType.FOOD,
        "landscape" to SceneType.LANDSCAPE,
        "nature" to SceneType.NATURE,
        "mountain" to SceneType.LANDSCAPE,
        "forest" to SceneType.NATURE,
        "sky" to SceneType.LANDSCAPE,
        "animal" to SceneType.PET,
        "dog" to SceneType.PET,
        "cat" to SceneType.PET,
        "pet" to SceneType.PET,
        "building" to SceneType.ARCHITECTURE,
        "architecture" to SceneType.ARCHITECTURE,
        "city" to SceneType.URBAN,
        "night" to SceneType.NIGHT,
        "nightlife" to SceneType.NIGHT,
        "sunrise" to SceneType.SUNSET,
        "sunset" to SceneType.SUNSET,
        "dusk" to SceneType.SUNSET,
        "dawn" to SceneType.SUNSET,
        "street" to SceneType.STREET,
        "road" to SceneType.STREET,
        "indoor" to SceneType.INDOOR,
        "interior" to SceneType.INDOOR,
        "room" to SceneType.INDOOR,
        "flower" to SceneType.NATURE,
        "blossom" to SceneType.NATURE,
        "beach" to SceneType.NATURE,
        "ocean" to SceneType.NATURE,
        "seaside" to SceneType.NATURE,
        "snow" to SceneType.NATURE,
        "stage" to SceneType.EVENT,
        "performance" to SceneType.EVENT,
        "concert" to SceneType.EVENT,
        "wedding" to SceneType.WEDDING,
        "macro" to SceneType.MACRO,
        "product" to SceneType.PRODUCT,
        "fashion" to SceneType.FASHION
    )

    // MARK: - 场景分类

    /**
     * 对单张照片进行场景分类
     *
     * 使用 ML Kit Image Labeling 识别照片内容，并通过 [sceneLabelMapping] 映射到场景类型。
     * 如果标签为空，则回退到基于像素亮度的规则化分类。
     *
     * @param bitmap 输入位图
     * @return 场景类型与置信度的 Pair，失败返回 (UNKNOWN, 0)
     */
    suspend fun classifyPhoto(bitmap: Bitmap): Pair<SceneType, Float> =
        withContext(Dispatchers.Default) {
            return@withContext try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val labels = Tasks.await(imageLabeler.process(image))

                if (labels.isEmpty()) {
                    return@withContext fallbackClassification(bitmap)
                }

                var bestScene = SceneType.UNKNOWN
                var bestConfidence = 0f

                for (label in labels) {
                    val text = label.text.lowercase(Locale.US)
                    val confidence = label.confidence

                    // 直接匹配
                    val mapped = sceneLabelMapping[text]
                    if (mapped != null && confidence > bestConfidence) {
                        bestScene = mapped
                        bestConfidence = confidence
                        continue
                    }

                    // 子串匹配（标签包含 key 或 key 包含标签）
                    if (bestScene == SceneType.UNKNOWN) {
                        for ((key, sceneType) in sceneLabelMapping) {
                            if (text.contains(key) || key.contains(text)) {
                                bestScene = sceneType
                                bestConfidence = confidence
                                break
                            }
                        }
                    }
                }

                Pair(bestScene, bestConfidence)
            } catch (e: Exception) {
                AppLogger.e(TAG, "场景分类失败", e)
                fallbackClassification(bitmap)
            }
        }

    /**
     * 基于像素亮度的兜底分类（替代 iOS performFallbackClassification）
     *
     * @param bitmap 输入位图
     * @return 场景类型与置信度的 Pair
     */
    private fun fallbackClassification(bitmap: Bitmap): Pair<SceneType, Float> {
        val brightness = computeAverageBrightness(bitmap)
        val scene = when {
            brightness < 0.15f -> SceneType.NIGHT
            brightness > 0.75f -> SceneType.LANDSCAPE
            else -> SceneType.UNKNOWN
        }
        return Pair(scene, 0.5f)
    }

    /**
     * 计算图像平均亮度（替代 iOS CIAreaHistogram + CIPhotoEffectMono）
     *
     * 通过降采样并对像素 RGB 加权求平均亮度。
     *
     * @param bitmap 输入位图
     * @return 归一化亮度（0.0 - 1.0）
     */
    private fun computeAverageBrightness(bitmap: Bitmap): Float {
        val targetSize = 64
        val scaled = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        var sum = 0L
        var count = 0
        for (y in 0 until targetSize) {
            for (x in 0 until targetSize) {
                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // ITU-R BT.601 亮度公式
                sum += (0.299 * r + 0.587 * g + 0.114 * b).toLong()
                count++
            }
        }
        if (scaled !== bitmap) scaled.recycle()
        return if (count > 0) sum.toFloat() / (count * 255f) else 0.5f
    }

    // MARK: - 按场景分组

    /**
     * 按场景类型分组照片
     *
     * 对每张照片基于 EXIF 元数据进行快速规则化场景分类（不调用 ML Kit，避免逐张推理开销）。
     *
     * @param records 照片记录列表
     * @return 场景类型到照片记录列表的映射
     */
    fun groupByScene(records: List<PhotoRecord>): Map<SceneType, List<PhotoRecord>> {
        val groups = mutableMapOf<SceneType, MutableList<PhotoRecord>>()
        for (record in records) {
            val scene = classifyRecordByMetadata(record)
            groups.getOrPut(scene) { mutableListOf() }.add(record)
        }
        return groups
    }

    /**
     * 基于 EXIF 元数据快速分类（替代 iOS classifyRecordByMetadata）
     *
     * 根据 ISO 和快门速度推断场景类型。
     *
     * @param record 照片记录
     * @return 推断的场景类型
     */
    private fun classifyRecordByMetadata(record: PhotoRecord): SceneType {
        record.iso?.let { iso ->
            if (iso >= 1600) return SceneType.NIGHT // 高 ISO 通常意味着暗光环境
        }
        record.shutterSpeed?.let { shutter ->
            if (shutter > 0.5) return SceneType.NIGHT // 长曝光 → 夜景
            if (shutter < 1.0 / 2000.0) return SceneType.LANDSCAPE // 高速快门 → 户外亮光
        }
        return SceneType.UNKNOWN
    }

    // MARK: - 按日期分组

    /**
     * 按日期分组照片（中文标签）
     *
     * 分组规则：今天、昨天、本周（带星期）、本月（M月d日）、更早（yyyy年M月）。
     *
     * @param records 照片记录列表
     * @return 日期标签到照片记录列表的映射
     */
    fun groupByDate(records: List<PhotoRecord>): Map<String, List<PhotoRecord>> {
        val calendar = Calendar.getInstance(Locale.CHINA)
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startOfYesterday = calendar.timeInMillis

        // 本周起点（按周一为一周第一天）
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        // 本月起点
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        val weekdayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val groups = mutableMapOf<String, MutableList<PhotoRecord>>()

        for (record in records) {
            val date = record.creationDate
            val label: String = when {
                date >= startOfToday -> "今天"
                date >= startOfYesterday -> "昨天"
                date >= startOfWeek -> {
                    calendar.timeInMillis = date
                    val weekday = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sunday ... 7=Saturday
                    val idx = (weekday - 1).coerceIn(0, 6)
                    "本周" + weekdayNames[idx]
                }
                date >= startOfMonth -> {
                    calendar.timeInMillis = date
                    "${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日"
                }
                else -> {
                    calendar.timeInMillis = date
                    "${calendar.get(Calendar.YEAR)}年${calendar.get(Calendar.MONTH) + 1}月"
                }
            }
            groups.getOrPut(label) { mutableListOf() }.add(record)
        }

        return groups
    }

    // MARK: - 按位置分组

    /**
     * 按位置分组照片（基于 EXIF GPS 元数据）
     *
     * 对每张照片读取 EXIF GPS 坐标，并通过 [coordinateToRegionLabel] 映射到中文城市标签。
     * 无 GPS 数据的照片归入"未知位置"。
     *
     * @param records 照片记录列表
     * @return 位置标签到照片记录列表的映射
     */
    suspend fun groupByLocation(records: List<PhotoRecord>): Map<String, List<PhotoRecord>> =
        withContext(Dispatchers.IO) {
            val groups = mutableMapOf<String, MutableList<PhotoRecord>>()
            for (record in records) {
                val label = extractLocationLabel(record)
                groups.getOrPut(label) { mutableListOf() }.add(record)
            }
            groups
        }

    /**
     * 从照片 EXIF 提取位置标签
     *
     * @param record 照片记录
     * @return 位置标签，无 GPS 数据返回"未知位置"
     */
    private fun extractLocationLabel(record: PhotoRecord): String {
        val file = photoStorage.getPhotoFile(record.id)
        if (!file.exists()) return "未知位置"
        return try {
            val exif = ExifInterface(file.absolutePath)
            val latLong = exif.latLong // floatArrayOf(lat, lon) 或 null
            if (latLong != null) {
                coordinateToRegionLabel(latLong[0].toDouble(), latLong[1].toDouble())
            } else {
                "未知位置"
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "读取 EXIF GPS 失败: ${record.id}", e)
            "未知位置"
        }
    }

    /**
     * 将坐标映射到中国城市区域标签（替代 iOS coordinateToRegionLabel）
     *
     * 包含 26 个主要城市的经纬度范围，匹配失败时按粗略中国区域返回。
     *
     * @param lat 纬度
     * @param lon 经度
     * @return 城市名称或区域标签
     */
    private fun coordinateToRegionLabel(lat: Double, lon: Double): String {
        // 简化的中国城市坐标映射（名称, 纬度范围, 经度范围）
        val cities = listOf(
            Triple("北京", 39.4..41.0, 115.4..117.5),
            Triple("上海", 30.7..31.5, 120.8..122.0),
            Triple("广州", 22.5..23.5, 112.9..114.0),
            Triple("深圳", 22.4..22.8, 113.7..114.6),
            Triple("杭州", 29.8..30.5, 119.7..120.8),
            Triple("成都", 30.0..31.0, 103.5..104.5),
            Triple("南京", 31.5..32.5, 118.3..119.2),
            Triple("武汉", 30.0..31.0, 113.7..115.0),
            Triple("西安", 33.8..34.6, 108.5..109.5),
            Triple("重庆", 29.0..30.0, 106.0..107.0),
            Triple("苏州", 30.8..31.5, 120.3..121.0),
            Triple("厦门", 24.2..24.6, 117.8..118.3),
            Triple("青岛", 35.8..36.5, 120.0..120.8),
            Triple("大连", 38.5..39.2, 121.0..122.0),
            Triple("三亚", 18.0..18.5, 109.0..109.8),
            Triple("香港", 22.1..22.5, 114.0..114.4),
            Triple("澳门", 22.0..22.2, 113.4..113.6),
            Triple("台北", 24.9..25.2, 121.4..121.6),
            Triple("哈尔滨", 45.3..46.0, 126.0..127.0),
            Triple("昆明", 24.5..25.5, 102.0..103.0),
            Triple("拉萨", 29.3..30.0, 90.5..91.5),
            Triple("乌鲁木齐", 43.3..44.2, 87.0..88.0),
            Triple("海口", 19.5..20.2, 110.0..110.5),
            Triple("桂林", 24.5..25.5, 110.0..110.8),
            Triple("丽江", 26.5..27.5, 100.0..100.5),
            Triple("张家界", 28.5..29.5, 110.0..111.0)
        )

        for ((name, latRange, lonRange) in cities) {
            if (lat in latRange && lon in lonRange) return name
        }

        // 粗略的中国区域
        if (lat > 35 && lon > 100) return "中国北方"
        if (lat <= 35 && lon > 100) return "中国南方"
        if (lon <= 100) return "中国西部"

        return String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
    }

    // MARK: - 按人脸分组

    /**
     * 按人脸聚类分组照片
     *
     * 使用 ML Kit Face Detection 检测每张照片中的人脸，按人脸数量进行聚类分组。
     * 简化实现：与人脸数量相同的照片归为一组。
     *
     * @param records 照片记录列表
     * @return 人脸聚类分组（每个内层列表是一组照片）
     */
    suspend fun groupByFaces(records: List<PhotoRecord>): List<List<PhotoRecord>> =
        withContext(Dispatchers.Default) {
            // record → 人脸数量
            val faceCounts = mutableMapOf<String, Int>()

            for (record in records) {
                val file = photoStorage.getPhotoFile(record.id)
                if (!file.exists()) continue
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                try {
                    val count = detectFaceCount(bitmap)
                    if (count > 0) {
                        faceCounts[record.id] = count
                    }
                } finally {
                    bitmap.recycle()
                }
            }

            // 按人脸数量聚类
            val byFaceCount = mutableMapOf<Int, MutableList<PhotoRecord>>()
            for (record in records) {
                val count = faceCounts[record.id] ?: continue
                byFaceCount.getOrPut(count) { mutableListOf() }.add(record)
            }

            byFaceCount.values.toList()
        }

    /**
     * 检测图片中的人脸数量
     *
     * @param bitmap 输入位图
     * @return 人脸数量，失败返回 0
     */
    private fun detectFaceCount(bitmap: Bitmap): Int {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = Tasks.await(faceDetector.process(image))
            faces.size
        } catch (e: Exception) {
            AppLogger.w(TAG, "人脸检测失败", e)
            0
        }
    }
}
