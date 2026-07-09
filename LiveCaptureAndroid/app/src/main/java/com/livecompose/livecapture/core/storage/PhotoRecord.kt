package com.livecompose.livecapture.core.storage

import java.util.UUID

/**
 * 照片记录数据模型
 */
data class PhotoRecord(
    val id: String = UUID.randomUUID().toString(),
    val creationDate: Long = System.currentTimeMillis(),
    val localIdentifier: String? = null,
    val detectionMethod: String? = null,
    val iso: Float? = null,
    val shutterSpeed: Double? = null,
    val aperture: Double? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val rating: Int = 0,
    val flag: Boolean = false,
    val filePath: String = ""
) {
    companion object {
        fun photoFilename(id: String) = "${id}.jpg"
        fun thumbnailFilename(id: String) = "${id}_thumb.jpg"
    }
}