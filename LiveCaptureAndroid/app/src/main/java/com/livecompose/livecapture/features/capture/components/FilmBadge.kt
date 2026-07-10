package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.DesignSystemCultural

/**
 * 胶片模拟标签数据
 *
 * @param name 胶片名称，如 "Kodak Gold 200"
 * @param iso 胶片 ISO，如 200
 * @param grain 颗粒等级，如 "细"
 * @param tone 色调描述，如 "暖金"
 */
data class FilmBadgeInfo(
    val name: String,
    val iso: Int? = null,
    val grain: String = "",
    val tone: String = ""
)

/**
 * 胶片模拟标签
 *
 * 显示当前所选胶片预设的型号与参数，强化胶片摄影仪式感。
 * 位于取景框左下角或场景标签附近，与 ContextInfoPanel 形成信息呼应。
 */
@Composable
fun FilmBadge(
    film: FilmBadgeInfo?,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && film != null && film.name.isNotEmpty(),
        enter = fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        val badge = film ?: return@AnimatedVisibility

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(DesignSystemCultural.Dimensions.filmBadgeHeight)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DesignSystemCultural.Colors.ricePaper.copy(alpha = 0.18f),
                            DesignSystemCultural.Colors.silverGrain.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(
                    width = DesignSystem.Stroke.widthThin,
                    color = DesignSystemCultural.Colors.filmAccent.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.small)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // 胶片孔装饰
            FilmPerforation()
            Spacer(modifier = Modifier.width(8.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = badge.name.uppercase(),
                    style = DesignSystemCultural.Typography.filmLabelPrimary,
                    color = DesignSystemCultural.Colors.filmAccent,
                    maxLines = 1
                )

                val params = buildString {
                    badge.iso?.let { append("ISO $it") }
                    if (badge.grain.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(badge.grain)
                    }
                    if (badge.tone.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(badge.tone)
                    }
                }
                if (params.isNotEmpty()) {
                    Text(
                        text = params,
                        style = DesignSystemCultural.Typography.filmLabelSecondary,
                        color = DesignSystem.Colors.minimalLabelTertiary,
                        maxLines = 1
                    )
                }
            }

            if (badge.grain.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Grain,
                    contentDescription = null,
                    tint = DesignSystemCultural.Colors.silverGrain.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun FilmPerforation(
    modifier: Modifier = Modifier,
    holeCount: Int = 3
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.height(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(holeCount) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 4.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(DesignSystemCultural.Colors.filmAccent.copy(alpha = 0.6f))
            )
        }
    }
}

/**
 * 从大师预设名称推断胶片标签信息
 */
fun filmBadgeFromPresetName(presetName: String): FilmBadgeInfo {
    val lower = presetName.lowercase()
    return when {
        lower.contains("kodak") && lower.contains("gold") -> FilmBadgeInfo(
            name = "Kodak Gold 200",
            iso = 200,
            grain = "细腻",
            tone = "暖金"
        )
        lower.contains("portra") && lower.contains("400") -> FilmBadgeInfo(
            name = "Portra 400",
            iso = 400,
            grain = "柔和",
            tone = "自然"
        )
        lower.contains("fuji") && lower.contains("c200") -> FilmBadgeInfo(
            name = "Fuji C200",
            iso = 200,
            grain = "清新",
            tone = "冷绿"
        )
        lower.contains("ilford") -> FilmBadgeInfo(
            name = "Ilford HP5",
            iso = 400,
            grain = "粗粝",
            tone = "黑白"
        )
        lower.contains("cinematic") -> FilmBadgeInfo(
            name = "Cinematic",
            iso = null,
            grain = "轻微",
            tone = "电影"
        )
        lower.contains("teal") || lower.contains("orange") -> FilmBadgeInfo(
            name = "Teal & Orange",
            iso = null,
            grain = "轻微",
            tone = "青橙"
        )
        lower.contains("ricoh") -> FilmBadgeInfo(
            name = "Ricoh GR",
            iso = null,
            grain = "都市",
            tone = "高对比"
        )
        lower.contains("velvia") -> FilmBadgeInfo(
            name = "Fuji Velvia 50",
            iso = 50,
            grain = "极细",
            tone = "鲜艳"
        )
        lower.contains("vintage") || lower.contains("sepia") -> FilmBadgeInfo(
            name = "Vintage Sepia",
            iso = null,
            grain = "复古",
            tone = "褐色"
        )
        else -> FilmBadgeInfo(
            name = presetName,
            iso = null,
            grain = "",
            tone = ""
        )
    }
}
