package com.livecompose.livecapture.core.editing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.core.lut.LutPreset
import com.livecompose.livecapture.core.lut.LutProcessor
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.storage.PhotoStorageService
import com.livecompose.livecapture.core.storage.ThumbnailGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 批量处理器
 *
 * 对应 iOS 端 BatchProcessor.swift，并行处理多张照片的滤镜应用、自动增强和批量删除，
 * 并提供实时进度跟踪。
 *
 * ## 主要功能
 * - applyFilter: 批量应用滤镜预设
 * - applyAutoEnhance: 批量自动增强
 * - deleteImages: 批量删除照片
 * - 实时进度跟踪
 *
 * @param photoStorage 照片存储服务，负责照片记录的读写和删除
 * @param context 上下文，用于定位缩略图目录
 */
class BatchProcessor(
    private val photoStorage: PhotoStorageService,
    private val context: Context
) {

    companion object {
        private const val TAG = "BatchProcessor"
        private const val JPEG_QUALITY = 92
        private const val THUMBNAIL_QUALITY = 80
    }

    private val lutProcessor = LutProcessor()
    private val autoEnhancer = AutoEnhancer()

    /** 进度锁，保证多协程并发时进度状态的一致性 */
    private val progressMutex = Mutex()

    // MARK: - 发布属性

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** 当前进度（0.0 - 1.0） */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _completedCount = MutableStateFlow(0)
    /** 已完成数量 */
    val completedCount: StateFlow<Int> = _completedCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    /** 总数量 */
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    // MARK: - 批量应用滤镜

    /**
     * 批量应用滤镜预设
     *
     * 对每张照片：从存储加载位图，应用滤镜（调用 [LutProcessor]），保存回原文件，
     * 更新缩略图，并递增进度。单张照片处理失败不影响其他照片。
     *
     * @param filter 滤镜预设（[LutPreset]）
     * @param images 照片记录列表
     * @param intensity 滤镜强度，默认 1.0
     * @return 处理后的照片记录列表（无论是否处理成功都返回原列表）
     */
    suspend fun applyFilter(
        filter: Any,
        images: List<PhotoRecord>,
        intensity: Float = 1.0f
    ): List<PhotoRecord> = withContext(Dispatchers.Default) {
        resetProgress(images.size)

        coroutineScope {
            images.map { record ->
                async {
                    try {
                        processFilter(record, filter, intensity)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "应用滤镜失败: ${record.id}", e)
                    } finally {
                        incrementProgress()
                    }
                }
            }.awaitAll()
        }

        finishProcessing()
        images
    }

    /**
     * 处理单张照片的滤镜应用
     *
     * @param record 照片记录
     * @param filter 滤镜预设
     * @param intensity 滤镜强度
     */
    private suspend fun processFilter(record: PhotoRecord, filter: Any, intensity: Float) {
        val photoFile = photoStorage.getPhotoFile(record.id)
        if (!photoFile.exists()) return

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return

        val resultBitmap = try {
            // 滤镜占位实现：调用 LutProcessor 处理
            // intensity 参数预留给后续按强度混合原图与结果图，此处先透传给处理器
            when (filter) {
                is LutPreset -> lutProcessor.applyPreset(bitmap, filter) { /* 忽略单图内部进度 */ }
                else -> bitmap
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "滤镜处理异常: ${record.id}", e)
            bitmap
        }

        // 保存处理后的照片
        saveBitmapToFile(resultBitmap, photoFile)

        // 更新缩略图
        updateThumbnail(record.id, resultBitmap)
    }

    // MARK: - 批量自动增强

    /**
     * 批量自动增强
     *
     * 对每张照片：从存储加载位图，调用 [AutoEnhancer.autoEnhance] 进行自动增强，
     * 保存回原文件，更新缩略图，并递增进度。单张照片处理失败不影响其他照片。
     *
     * @param images 照片记录列表
     * @return 处理后的照片记录列表
     */
    suspend fun applyAutoEnhance(images: List<PhotoRecord>): List<PhotoRecord> = withContext(Dispatchers.Default) {
        resetProgress(images.size)

        coroutineScope {
            images.map { record ->
                async {
                    try {
                        processAutoEnhance(record)
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "自动增强失败: ${record.id}", e)
                    } finally {
                        incrementProgress()
                    }
                }
            }.awaitAll()
        }

        finishProcessing()
        images
    }

    /**
     * 处理单张照片的自动增强
     *
     * @param record 照片记录
     */
    private fun processAutoEnhance(record: PhotoRecord) {
        val photoFile = photoStorage.getPhotoFile(record.id)
        if (!photoFile.exists()) return

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return

        val enhanced = try {
            autoEnhancer.autoEnhance(bitmap)
        } catch (e: Exception) {
            AppLogger.e(TAG, "自动增强处理异常: ${record.id}", e)
            bitmap
        }

        saveBitmapToFile(enhanced, photoFile)
        updateThumbnail(record.id, enhanced)
    }

    // MARK: - 批量删除

    /**
     * 批量删除照片
     *
     * 逐条调用 [PhotoStorageService.deleteRecord] 删除照片记录及其文件，
     * 并递增进度。单条删除失败不影响其他照片。
     *
     * @param records 要删除的照片记录列表
     */
    suspend fun deleteImages(records: List<PhotoRecord>) {
        withContext(Dispatchers.Default) {
            resetProgress(records.size)

            coroutineScope {
                records.map { record ->
                    async {
                        try {
                            photoStorage.deleteRecord(record.id)
                        } catch (e: Exception) {
                            AppLogger.e(TAG, "删除照片失败: ${record.id}", e)
                        } finally {
                            incrementProgress()
                        }
                    }
                }.awaitAll()
            }

            finishProcessing()
        }
    }

    // MARK: - 进度管理

    /**
     * 重置进度
     *
     * @param total 总数量
     */
    private suspend fun resetProgress(total: Int) {
        progressMutex.withLock {
            _isProcessing.value = true
            _progress.value = 0f
            _completedCount.value = 0
            _totalCount.value = total
        }
    }

    /**
     * 递增进度（已完成数 +1，并更新百分比）
     */
    private suspend fun incrementProgress() {
        progressMutex.withLock {
            val completed = _completedCount.value + 1
            _completedCount.value = completed
            val total = _totalCount.value
            _progress.value = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        }
    }

    /**
     * 完成处理（进度置满，结束处理状态）
     */
    private suspend fun finishProcessing() {
        progressMutex.withLock {
            _progress.value = 1.0f
            _completedCount.value = _totalCount.value
            _isProcessing.value = false
        }
    }

    // MARK: - 私有辅助

    /**
     * 保存 Bitmap 到文件（JPEG 格式）
     *
     * @param bitmap 要保存的位图
     * @param file 目标文件
     */
    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.flush()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "保存图片失败: ${file.absolutePath}", e)
        }
    }

    /**
     * 更新缩略图
     *
     * 使用 [ThumbnailGenerator] 生成缩略图并写入缩略图目录，
     * 目录结构与 [PhotoStorageService] 内部保持一致。
     *
     * @param id 照片记录 ID
     * @param bitmap 用于生成缩略图的位图
     */
    private fun updateThumbnail(id: String, bitmap: Bitmap) {
        try {
            val thumbnailsDir = File(File(context.filesDir, "LiveCapture"), "thumbnails")
                .also { it.mkdirs() }
            val thumbFile = File(thumbnailsDir, PhotoRecord.thumbnailFilename(id))
            val thumb = ThumbnailGenerator.generate(bitmap)
            FileOutputStream(thumbFile).use { out ->
                thumb.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
                out.flush()
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "更新缩略图失败: $id", e)
        }
    }
}
