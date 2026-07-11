package com.livecompose.livecapture.core.storage

import androidx.room.Embedded

data class CropRegionEmbed(
    @Embedded(prefix = "crop_")
    val cropRegion: CropRegion? = null
)
