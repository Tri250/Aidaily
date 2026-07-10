package com.livecompose.livecapture.core.storage

data class PhotoRecord(
    val id: String,
    val filePath: String,
    val thumbPath: String,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val cropRegion: CropRegion? = null,
    val aestheticScore: Float? = null
)

data class CropRegion(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float
)

data class ExifData(
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val make: String = "LiveCapture",
    val model: String = "Android"
)
