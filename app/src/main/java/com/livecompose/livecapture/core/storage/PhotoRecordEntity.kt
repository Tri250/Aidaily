package com.livecompose.livecapture.core.storage

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoRecordEntity(
    @PrimaryKey
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
    @Embedded(prefix = "crop_")
    val cropRegion: CropRegion? = null,
    val aestheticScore: Float? = null
) {
    fun toDomain(): PhotoRecord = PhotoRecord(
        id = id,
        filePath = filePath,
        thumbPath = thumbPath,
        width = width,
        height = height,
        timestamp = timestamp,
        iso = iso,
        shutterSpeed = shutterSpeed,
        aperture = aperture,
        focalLength = focalLength,
        cropRegion = cropRegion,
        aestheticScore = aestheticScore
    )

    companion object {
        fun fromDomain(record: PhotoRecord): PhotoRecordEntity = PhotoRecordEntity(
            id = record.id,
            filePath = record.filePath,
            thumbPath = record.thumbPath,
            width = record.width,
            height = record.height,
            timestamp = record.timestamp,
            iso = record.iso,
            shutterSpeed = record.shutterSpeed,
            aperture = record.aperture,
            focalLength = record.focalLength,
            cropRegion = record.cropRegion,
            aestheticScore = record.aestheticScore
        )
    }
}
