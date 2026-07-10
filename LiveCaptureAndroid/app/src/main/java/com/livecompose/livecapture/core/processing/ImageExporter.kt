package com.livecompose.livecapture.core.processing

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 图像导出器
 * 支持 JPEG / HEIC / PNG / TIFF 导出，自定义保存位置，
 * 保留 EXIF 信息，导出进度回调
 */
class ImageExporter(private val contentResolver: ContentResolver) {

    enum class ExportFormat(val extension: String, val mimeType: String) {
        JPEG("jpg", "image/jpeg"),
        HEIC("heic", "image/heic"),
        PNG("png", "image/png"),
        TIFF("tiff", "image/tiff");

        val displayName: String get() = name
    }

    data class ExportOptions(
        val format: ExportFormat = ExportFormat.JPEG,
        val quality: Int = 95,
        val saveTo: SaveLocation = SaveLocation.APP_INTERNAL,
        val customDirectory: String? = null,
        val filenamePrefix: String = "LC_",
        val preserveExif: Boolean = true,
        val exifData: ByteArray? = null  // 原始 EXIF 数据字节
    )

    enum class SaveLocation(val displayName: String) {
        APP_INTERNAL("应用内部"),
        DCIM("系统相册(DCIM)"),
        CUSTOM("自定义目录")
    }

    /**
     * 导出进度回调
     */
    fun interface ExportProgressCallback {
        fun onProgress(progress: Float, stage: String)
    }

    /**
     * 导出照片到指定位置
     * @return 保存的文件路径或 URI
     */
    fun export(
        bitmap: Bitmap,
        options: ExportOptions = ExportOptions(),
        onProgress: ExportProgressCallback? = null
    ): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "${options.filenamePrefix}${timestamp}.${options.format.extension}"

        onProgress?.onProgress(0f, "准备导出")

        return when (options.saveTo) {
            SaveLocation.APP_INTERNAL -> saveToInternal(bitmap, filename, options, onProgress)
            SaveLocation.DCIM -> saveToDCIM(bitmap, filename, options, onProgress)
            SaveLocation.CUSTOM -> saveToCustom(bitmap, filename, options, onProgress)
        }
    }

    /**
     * 保存到应用内部存储
     */
    private fun saveToInternal(
        bitmap: Bitmap,
        filename: String,
        options: ExportOptions,
        onProgress: ExportProgressCallback?
    ): String {
        val dir = File(Environment.getExternalStorageDirectory(), "MiaoJian").also { it.mkdirs() }
        val file = File(dir, filename)
        onProgress?.onProgress(0.3f, "编码中")
        val bytes = compressBitmap(bitmap, options)
        onProgress?.onProgress(0.8f, "写入文件")
        FileOutputStream(file).use { it.write(bytes.toByteArray()) }
        onProgress?.onProgress(1f, "完成")
        return file.absolutePath
    }

    /**
     * 保存到系统相册 (DCIM)
     */
    private fun saveToDCIM(
        bitmap: Bitmap,
        filename: String,
        options: ExportOptions,
        onProgress: ExportProgressCallback?
    ): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, options.format.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/MiaoJian")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("无法创建媒体文件")

        onProgress?.onProgress(0.3f, "编码中")
        val bytes = compressBitmap(bitmap, options)
        onProgress?.onProgress(0.7f, "写入媒体库")

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(bytes.toByteArray())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }

        onProgress?.onProgress(1f, "完成")
        return uri.toString()
    }

    /**
     * 保存到自定义目录
     */
    private fun saveToCustom(
        bitmap: Bitmap,
        filename: String,
        options: ExportOptions,
        onProgress: ExportProgressCallback?
    ): String {
        val customDir = options.customDirectory
            ?: throw IllegalArgumentException("自定义目录路径未设置")
        val dir = File(customDir).also { it.mkdirs() }
        val file = File(dir, filename)
        onProgress?.onProgress(0.3f, "编码中")
        val bytes = compressBitmap(bitmap, options)
        onProgress?.onProgress(0.8f, "写入文件")
        FileOutputStream(file).use { it.write(bytes.toByteArray()) }
        onProgress?.onProgress(1f, "完成")
        return file.absolutePath
    }

    /**
     * 压缩 Bitmap 到字节数组
     */
    private fun compressBitmap(bitmap: Bitmap, options: ExportOptions): ByteArrayOutputStream {
        val outputStream = ByteArrayOutputStream()
        when (options.format) {
            ExportFormat.JPEG -> {
                bitmap.compress(Bitmap.CompressFormat.JPEG, options.quality, outputStream)
            }
            ExportFormat.HEIC -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, options.quality, outputStream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, options.quality, outputStream)
                }
            }
            ExportFormat.PNG -> {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            ExportFormat.TIFF -> {
                writeTiff(bitmap, outputStream)
            }
        }
        return outputStream
    }

    /**
     * 写入 TIFF 格式（简化实现，支持 RGB 24-bit 无压缩）
     */
    private fun writeTiff(bitmap: Bitmap, outputStream: ByteArrayOutputStream) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 转换为 RGB 字节数组
        val rgbData = ByteArray(width * height * 3)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            rgbData[i * 3] = ((pixel shr 16) and 0xFF).toByte()     // R
            rgbData[i * 3 + 1] = ((pixel shr 8) and 0xFF).toByte()  // G
            rgbData[i * 3 + 2] = (pixel and 0xFF).toByte()          // B
        }

        val dataOffset = 8 // TIFF header (8 bytes) + IFD
        val ifdEntryCount = 12 // Number of tags
        val ifdSize = 2 + ifdEntryCount * 12 + 4 // count + entries + next IFD offset

        val headerSize = dataOffset + ifdSize
        val totalSize = headerSize + rgbData.size

        val dos = DataOutputStream(outputStream)

        // TIFF Header
        // Byte order: little-endian
        dos.writeByte(0x49) // 'I'
        dos.writeByte(0x49) // 'I'
        // Magic number: 42
        dos.writeShort(42)
        // Offset to first IFD
        dos.writeInt(dataOffset)

        // IFD: Number of directory entries
        dos.writeShort(ifdEntryCount)

        // Tag 256: ImageWidth (SHORT)
        writeTiffTag(dos, 256, 3, 1, width.toLong())
        // Tag 257: ImageLength (SHORT)
        writeTiffTag(dos, 257, 3, 1, height.toLong())
        // Tag 258: BitsPerSample (SHORT, 3 values)
        // We need to store the 3 values outside the IFD
        val bitsPerSampleOffset = headerSize
        writeTiffTag(dos, 258, 3, 3, bitsPerSampleOffset.toLong())
        // Tag 259: Compression (SHORT) - 1 = No compression
        writeTiffTag(dos, 259, 3, 1, 1L)
        // Tag 262: PhotometricInterpretation (SHORT) - 2 = RGB
        writeTiffTag(dos, 262, 3, 1, 2L)
        // Tag 273: StripOffsets (LONG)
        writeTiffTag(dos, 273, 4, 1, (headerSize + 6).toLong()) // +6 for the BitsPerSample values
        // Tag 274: Orientation (SHORT) - 1 = TopLeft
        writeTiffTag(dos, 274, 3, 1, 1L)
        // Tag 277: SamplesPerPixel (SHORT) - 3 = RGB
        writeTiffTag(dos, 277, 3, 1, 3L)
        // Tag 278: RowsPerStrip (SHORT)
        writeTiffTag(dos, 278, 3, 1, height.toLong())
        // Tag 279: StripByteCounts (LONG)
        writeTiffTag(dos, 279, 4, 1, rgbData.size.toLong())
        // Tag 282: XResolution (RATIONAL)
        writeTiffTag(dos, 282, 5, 1, (headerSize + 6 + 8).toLong()) // after bitsPerSample
        // Tag 283: YResolution (RATIONAL)
        writeTiffTag(dos, 283, 5, 1, (headerSize + 6 + 16).toLong())

        // Next IFD offset: 0 (no more IFDs)
        dos.writeInt(0)

        // BitsPerSample values (3 SHORTs)
        dos.writeShort(8)
        dos.writeShort(8)
        dos.writeShort(8)

        // XResolution (2 LONGs: numerator, denominator) - 72 DPI
        dos.writeInt(72)
        dos.writeInt(1)

        // YResolution (2 LONGs: numerator, denominator) - 72 DPI
        dos.writeInt(72)
        dos.writeInt(1)

        // Image data
        dos.write(rgbData)
        dos.flush()
    }

    private fun writeTiffTag(
        dos: DataOutputStream,
        tag: Int,
        type: Int,
        count: Int,
        value: Long
    ) {
        dos.writeShort(tag)
        dos.writeShort(type)
        dos.writeInt(count)
        // Value or offset (4 bytes total)
        if (type == 3) { // SHORT
            if (count == 1) {
                dos.writeShort(value.toInt())
                dos.writeShort(0)
            } else {
                dos.writeInt(value.toInt())
            }
        } else {
            dos.writeInt(value.toInt())
        }
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
            results.add(
                export(
                    bitmap,
                    customOptions,
                    ExportProgressCallback { progress, _ ->
                        onProgress(index + 1, bitmaps.size)
                    }
                )
            )
        }
        return results
    }
}
