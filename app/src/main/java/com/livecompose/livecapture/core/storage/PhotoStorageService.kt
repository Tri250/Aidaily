package com.livecompose.livecapture.core.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoStorageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoRecordDao: PhotoRecordDao
) {

    companion object {
        private const val TAG = "PhotoStorageService"
        private const val THUMBS_DIR = "thumbs"
        private const val MAX_THUMB_SIZE = 300
    }

    private val storageDir: File by lazy {
        File(context.getExternalFilesDir(null), "LiveCapture/photos").apply {
            if (!exists() && !mkdirs()) {
                Log.w(TAG, "Failed to create storage dir: $absolutePath")
            }
        }
    }

    private val thumbsDir: File by lazy {
        File(storageDir, THUMBS_DIR).apply {
            if (!exists() && !mkdirs()) {
                Log.w(TAG, "Failed to create thumbs dir: $absolutePath")
            }
        }
    }

    fun getAllRecordsFlow(): Flow<List<PhotoRecord>> =
        photoRecordDao.getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun savePhoto(
        imageProxy: ImageProxy,
        cropRegion: CropRegion? = null,
        exifData: ExifData = ExifData(),
        aestheticScore: Float? = null
    ): PhotoRecord = withContext(Dispatchers.IO) {
        try {
            val bitmap = imageProxyToBitmap(imageProxy)
                ?: throw IllegalStateException("Failed to decode JPEG from ImageProxy")

            // 3:4 裁切
            val targetAspect = 3f / 4f
            val croppedBitmap = cropToAspectRatio(bitmap, targetAspect)

            // 旋转校正
            val rotatedBitmap = rotateBitmap(croppedBitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
            val rotatedIsNew = rotatedBitmap !== croppedBitmap

            val fileName = "${UUID.randomUUID()}.jpg"

            // 压缩为 JPEG 字节，写入临时文件，再写 EXIF
            val tempFile = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
            try {
                FileOutputStream(tempFile).use { out ->
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                writeExif(tempFile, exifData)

                // 保存主图到 MediaStore 或文件系统
                val savedPath = saveJpegToStorage(tempFile, fileName)

                // 生成缩略图
                val thumbBitmap = ThumbnailUtils.extractThumbnail(rotatedBitmap, MAX_THUMB_SIZE, MAX_THUMB_SIZE)
                val thumbFile = File(thumbsDir, "thumb_$fileName")
                FileOutputStream(thumbFile).use { out ->
                    thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbBitmap.recycle()

                val record = PhotoRecord(
                    id = UUID.randomUUID().toString(),
                    filePath = savedPath,
                    thumbPath = thumbFile.absolutePath,
                    width = rotatedBitmap.width,
                    height = rotatedBitmap.height,
                    timestamp = System.currentTimeMillis(),
                    iso = exifData.iso,
                    shutterSpeed = exifData.shutterSpeed,
                    aperture = exifData.aperture,
                    focalLength = exifData.focalLength,
                    cropRegion = cropRegion,
                    aestheticScore = aestheticScore
                )

                photoRecordDao.insert(PhotoRecordEntity.fromDomain(record))

                // 安全回收
                if (rotatedIsNew) rotatedBitmap.recycle()
                croppedBitmap.recycle()
                bitmap.recycle()

                record
            } finally {
                tempFile.delete()
            }
        } finally {
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun cropToAspectRatio(bitmap: Bitmap, aspectRatio: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return bitmap
        val currentAspect = width.toFloat() / height

        return if (currentAspect > aspectRatio) {
            val newWidth = maxOf(1, (height * aspectRatio).toInt()).coerceAtMost(width)
            val xOffset = (width - newWidth) / 2
            Bitmap.createBitmap(bitmap, xOffset, 0, newWidth, height)
        } else {
            val newHeight = maxOf(1, (width / aspectRatio).toInt()).coerceAtMost(height)
            val yOffset = (height - newHeight) / 2
            Bitmap.createBitmap(bitmap, 0, yOffset, width, newHeight)
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun saveJpegToStorage(jpegFile: File, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LiveCapture")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = try {
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore insert failed, fallback to private dir", e)
                null
            }

            if (uri != null) {
                val streamOpened = context.contentResolver.openOutputStream(uri)?.use { out ->
                    jpegFile.inputStream().use { input -> input.copyTo(out) }
                    true
                } ?: false

                if (streamOpened) {
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, updateValues, null, null)
                    return uri.toString()
                } else {
                    context.contentResolver.delete(uri, null, null)
                    Log.w(TAG, "MediaStore openOutputStream failed, fallback to private dir")
                }
            }
        }
        val file = File(storageDir, fileName)
        jpegFile.inputStream().use { input ->
            FileOutputStream(file).use { out -> input.copyTo(out) }
        }
        return file.absolutePath
    }

    private fun writeExif(file: File, exifData: ExifData) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exifData.iso?.let { exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, it) }
            exifData.aperture?.let { exif.setAttribute(ExifInterface.TAG_F_NUMBER, it) }
            exifData.focalLength?.let { exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, it) }
            exifData.shutterSpeed?.let {
                exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, it)
            }
            exif.setAttribute(ExifInterface.TAG_MAKE, exifData.make)
            exif.setAttribute(ExifInterface.TAG_MODEL, exifData.model)
            exif.saveAttributes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write EXIF", e)
        }
    }

    suspend fun deleteRecordAsync(record: PhotoRecord) {
        try {
            if (record.filePath.startsWith("content://")) {
                context.contentResolver.delete(Uri.parse(record.filePath), null, null)
            } else {
                File(record.filePath).delete()
            }
            File(record.thumbPath).delete()
            photoRecordDao.deleteById(record.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete record", e)
        }
    }

    suspend fun saveEditedPhoto(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val fileName = "edited_${UUID.randomUUID()}.jpg"
        val tempFile = File(context.cacheDir, "edit_${System.currentTimeMillis()}.jpg")
        try {
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val savedPath = saveJpegToStorage(tempFile, fileName)

            // Generate thumbnail and save record
            val thumbBitmap = ThumbnailUtils.extractThumbnail(bitmap, MAX_THUMB_SIZE, MAX_THUMB_SIZE)
            val thumbFile = File(thumbsDir, "thumb_$fileName")
            FileOutputStream(thumbFile).use { out ->
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            thumbBitmap.recycle()

            val record = PhotoRecord(
                id = UUID.randomUUID().toString(),
                filePath = savedPath,
                thumbPath = thumbFile.absolutePath,
                width = bitmap.width,
                height = bitmap.height,
                timestamp = System.currentTimeMillis()
            )
            photoRecordDao.insert(PhotoRecordEntity.fromDomain(record))

            savedPath
        } finally {
            tempFile.delete()
        }
    }

    fun loadBitmapFromPath(path: String): Bitmap? {
        return try {
            if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            } else {
                BitmapFactory.decodeFile(path)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from path: $path", e)
            null
        }
    }
}
