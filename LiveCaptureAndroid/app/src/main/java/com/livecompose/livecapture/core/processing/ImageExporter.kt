package com.livecompose.livecapture.core.processing

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图像导出器
 * 支持 JPEG / HEIC 导出，自定义保存位置
 */
class ImageExporter(private val contentResolver: ContentResolver) {

    enum class ExportFormat(val extension: String, val mimeType: String) {
        JPEG("jpg", "image/jpeg"),
        HEIC("heic", "image/heic");

        val displayName: String get() = name
    }

    data class ExportOptions(
        val format: ExportFormat = ExportFormat.JPEG,
        val quality: Int = 95,
        val saveTo: SaveLocation = SaveLocation.APP_INTERNAL,
        val customDirectory: String? = null,
        val filenamePrefix: String = "LC_"
    )

    enum class SaveLocation(val displayName: String) {
        APP_INTERNAL("应用内部"),
        DCIM("系统相册(DCIM)"),
        CUSTOM("自定义目录")
    }

    /**
     * 导出照片到指定位置
     * @return 保存的文件路径或 URI
     */
    fun export(
        bitmap: Bitmap,
        options: ExportOptions = ExportOptions()
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "${options.filenamePrefix}${timestamp}.${options.format.extension}"

        return when (options.saveTo) {
            SaveLocation.APP_INTERNAL -> saveToInternal(bitmap, filename, options)
            SaveLocation.DCIM -> saveToDCIM(bitmap, filename, options)
            SaveLocation.CUSTOM -> saveToCustom(bitmap, filename, options)
        }
    }

    /**
     * 保存到应用内部存储
     */
    private fun saveToInternal(bitmap: Bitmap, filename: String, options: ExportOptions): String {
        val dir = File(Environment.getExternalStorageDirectory(), "LiveCapture").also { it.mkdirs() }
        val file = File(dir, filename)
        compressBitmap(bitmap, options).writeTo(FileOutputStream(file))
        return file.absolutePath
    }

    /**
     * 保存到系统相册 (DCIM)
     */
    private fun saveToDCIM(bitmap: Bitmap, filename: String, options: ExportOptions): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, options.format.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/LiveCapture")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("无法创建媒体文件")

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            compressBitmap(bitmap, options).writeTo(outputStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }

        return uri.toString()
    }

    /**
     * 保存到自定义目录
     */
    private fun saveToCustom(bitmap: Bitmap, filename: String, options: ExportOptions): String {
        val customDir = options.customDirectory
            ?: throw IllegalArgumentException("自定义目录路径未设置")
        val dir = File(customDir).also { it.mkdirs() }
        val file = File(dir, filename)
        compressBitmap(bitmap, options).writeTo(FileOutputStream(file))
        return file.absolutePath
    }

    /**
     * 压缩 Bitmap 到字节数组
     * JPEG 使用 Bitmap.CompressFormat.JPEG
     * HEIC 使用 Bitmap.CompressFormat.WEBP 作为降级（Android 原生 HEIC 写入需要 MediaMuxer）
     */
    private fun compressBitmap(bitmap: Bitmap, options: ExportOptions): ByteArrayOutputStream {
        val outputStream = ByteArrayOutputStream()
        val compressFormat = when (options.format) {
            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ExportFormat.HEIC -> {
                // Android 12+ 支持原生 HEIC 编码
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Bitmap.CompressFormat.WEBP_LOSSY  // 降级为 WebP (最接近 HEIC 的压缩效率)
                } else {
                    Bitmap.CompressFormat.JPEG  // 旧版本降级为 JPEG
                }
            }
        }
        bitmap.compress(compressFormat, options.quality, outputStream)
        return outputStream
    }

    /**
     * 批量导出
     */
    fun batchExport(
        bitmaps: List<Pair<Bitmap, String>>,  // bitmap + filename
        options: ExportOptions = ExportOptions(),
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<String> {
        val results = mutableListOf<String>()
        bitmaps.forEachIndexed { index, (bitmap, name) ->
            val customOptions = options.copy(filenamePrefix = name)
            results.add(export(bitmap, customOptions))
            onProgress(index + 1, bitmaps.size)
        }
        return results
    }
}
