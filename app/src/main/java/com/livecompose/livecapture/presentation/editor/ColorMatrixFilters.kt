package com.livecompose.livecapture.presentation.editor

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ColorMatrix
import com.livecompose.livecapture.R

enum class PhotoFilter(
    @StringRes val nameResId: Int,
    val colorMatrix: FloatArray
) {
    ORIGINAL(
        nameResId = R.string.editor_filter_original,
        colorMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    BLACK_WHITE(
        nameResId = R.string.editor_filter_black_white,
        colorMatrix = floatArrayOf(
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0.299f, 0.587f, 0.114f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    WARM(
        nameResId = R.string.editor_filter_warm,
        colorMatrix = floatArrayOf(
            1.1f, 0f, 0f, 0f, 10f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 0.9f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    COOL(
        nameResId = R.string.editor_filter_cool,
        colorMatrix = floatArrayOf(
            0.9f, 0f, 0f, 0f, 0f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 1.1f, 0f, 10f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VINTAGE(
        nameResId = R.string.editor_filter_vintage,
        colorMatrix = floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    ),
    VIVID(
        nameResId = R.string.editor_filter_vivid,
        colorMatrix = floatArrayOf(
            1.2f, 0f, 0f, 0f, 0f,
            0f, 1.2f, 0f, 0f, 0f,
            0f, 0f, 1.2f, 0f, 0f,
            0f, 0f, 0f, 1.1f, 0f
        )
    );

    fun toColorMatrix(): ColorMatrix = ColorMatrix(colorMatrix)
}

fun combineMatrices(vararg matrices: ColorMatrix): ColorMatrix {
    val result = ColorMatrix()
    for (matrix in matrices) {
        result.preConcat(matrix)
    }
    return result
}

fun buildBrightnessMatrix(value: Float): ColorMatrix {
    // value: -100 to 100, map to -128 to 128 offset
    val offset = value * 1.28f
    return ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, offset,
            0f, 1f, 0f, 0f, offset,
            0f, 0f, 1f, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

fun buildContrastMatrix(value: Float): ColorMatrix {
    // value: -100 to 100, map to 0.5 to 2.0 scale
    val scale = 1f + value / 100f
    val offset = 128f * (1f - scale)
    return ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
}

fun buildSaturationMatrix(value: Float): ColorMatrix {
    // value: -100 to 100, map to 0.0 to 2.0 saturation
    val saturation = 1f + value / 100f
    val desaturatedR = 0.299f
    val desaturatedG = 0.587f
    val desaturatedB = 0.114f
    val sr = (1f - saturation) * desaturatedR + saturation
    val sg = (1f - saturation) * desaturatedG
    val sb = (1f - saturation) * desaturatedB
    val dr = (1f - saturation) * desaturatedR
    val dg = (1f - saturation) * desaturatedG
    val db = (1f - saturation) * desaturatedB
    return ColorMatrix(
        floatArrayOf(
            sr, dg, db, 0f, 0f,
            dr, sg, db, 0f, 0f,
            dr, dg, sb, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
}
