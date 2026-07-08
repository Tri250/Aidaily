package com.livecompose.livecapture.core.metadata

import android.content.ContentResolver
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * EXIF 数据模型
 */
data class ExifData(
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val gpsLatitude: String? = null,
    val gpsLongitude: String? = null,
    val gpsAltitude: String? = null,
    val dateTime: String? = null,
    val cameraModel: String? = null,
    val flash: String? = null,
    val imageWidth: String? = null,
    val imageHeight: String? = null,
    val hasGps: Boolean = false
)

/**
 * EXIF 信息读取器
 * 使用 AndroidX ExifInterface 读取照片 EXIF 元数据
 */
object ExifReader {

    /**
     * 从 URI 读取 EXIF 信息
     */
    fun readExif(uri: Uri, contentResolver: ContentResolver): ExifData {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()
            parseExif(exif)
        } catch (e: Exception) {
            ExifData()
        }
    }

    /**
     * 从文件路径读取 EXIF 信息
     */
    fun readExif(path: String): ExifData {
        return try {
            val file = File(path)
            if (!file.exists()) return ExifData()
            val exif = ExifInterface(file)
            parseExif(exif)
        } catch (e: Exception) {
            ExifData()
        }
    }

    /**
     * 解析 ExifInterface 为结构化数据
     */
    private fun parseExif(exif: ExifInterface?): ExifData {
        if (exif == null) return ExifData()

        return ExifData(
            iso = extractIso(exif),
            shutterSpeed = extractShutterSpeed(exif),
            aperture = extractAperture(exif),
            focalLength = extractFocalLength(exif),
            gpsLatitude = extractGpsLatitude(exif),
            gpsLongitude = extractGpsLongitude(exif),
            gpsAltitude = extractGpsAltitude(exif),
            dateTime = extractDateTime(exif),
            cameraModel = extractCameraModel(exif),
            flash = extractFlash(exif),
            imageWidth = extractImageWidth(exif),
            imageHeight = extractImageHeight(exif),
            hasGps = hasGpsData(exif)
        )
    }

    private fun extractIso(exif: ExifInterface): String? {
        val iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED)
            ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
        return iso?.let { "ISO $it" }
    }

    private fun extractShutterSpeed(exif: ExifInterface): String? {
        val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        if (exposureTime != null) {
            return try {
                val value = exposureTime.toDouble()
                if (value >= 1.0) {
                    "${value}s"
                } else {
                    "1/${(1.0 / value).toInt()}s"
                }
            } catch (e: Exception) {
                null
            }
        }
        // 也尝试从快门速度值读取
        val shutterSpeedValue = exif.getAttribute(ExifInterface.TAG_SHUTTER_SPEED_VALUE)
        return shutterSpeedValue?.let { "${it}s" }
    }

    private fun extractAperture(exif: ExifInterface): String? {
        val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
        if (aperture != null) {
            return try {
                "f/${aperture.toDouble().let { String.format("%.1f", it) }}"
            } catch (e: Exception) {
                null
            }
        }
        // 也尝试从光圈值读取
        val apertureValue = exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE)
        return apertureValue?.let { "f/$it" }
    }

    private fun extractFocalLength(exif: ExifInterface): String? {
        val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        if (focalLength != null) {
            return try {
                "${focalLength.toDouble().let { String.format("%.0f", it) }}mm"
            } catch (e: Exception) {
                null
            }
        }
        // 35mm 等效焦距
        val focalLength35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)
        return focalLength35mm?.let { "${it}mm (35mm等效)" }
    }

    private fun extractGpsLatitude(exif: ExifInterface): String? {
        val latLong = exif.latLong
        return latLong?.let { formatGpsCoordinate(it[0], "N", "S") }
    }

    private fun extractGpsLongitude(exif: ExifInterface): String? {
        val latLong = exif.latLong
        return latLong?.let { formatGpsCoordinate(it[1], "E", "W") }
    }

    private fun formatGpsCoordinate(value: Double, positiveSuffix: String, negativeSuffix: String): String {
        val degrees = value.toInt()
        val minutes = ((value - degrees) * 60).toInt()
        val seconds = ((value - degrees - minutes / 60.0) * 3600)
        val direction = if (value >= 0) positiveSuffix else negativeSuffix
        return "${Math.abs(degrees)}°${minutes}'${String.format("%.1f", Math.abs(seconds))}\"$direction"
    }

    private fun extractGpsAltitude(exif: ExifInterface): String? {
        val altitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)
        val altitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF)
        if (altitude != null) {
            return try {
                val alt = altitude.toDouble()
                val ref = if (altitudeRef == "1") "-" else ""
                "${ref}${String.format("%.1f", alt)}m"
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun extractDateTime(exif: ExifInterface): String? {
        return exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
    }

    private fun extractCameraModel(exif: ExifInterface): String? {
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
        return when {
            make != null && model != null -> "$make $model"
            model != null -> model
            make != null -> make
            else -> null
        }
    }

    private fun extractFlash(exif: ExifInterface): String? {
        val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
        if (flash != null) {
            return try {
                val flashValue = flash.toInt()
                when {
                    flashValue and 0x1 != 0 -> "闪光灯已触发"
                    flashValue == 0 -> "未使用闪光灯"
                    flashValue and 0x18 != 0 -> "强制闪光"
                    flashValue and 0x20 != 0 -> "未检测到闪光灯功能"
                    flashValue and 0x40 != 0 -> "防红眼模式"
                    else -> "闪光灯状态: $flashValue"
                }
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun extractImageWidth(exif: ExifInterface): String? {
        return exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            ?: exif.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION)
    }

    private fun extractImageHeight(exif: ExifInterface): String? {
        return exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            ?: exif.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION)
    }

    private fun hasGpsData(exif: ExifInterface): Boolean {
        return exif.latLong != null
    }
}