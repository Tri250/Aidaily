package com.livecompose.livecapture.features.capture.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.DesignSystemCultural
import java.util.Calendar
import java.util.Date

/**
 * 拍摄上下文信息数据
 *
 * @param weather 天气描述，如 "晴 24°"
 * @param lunarDate 农历日期，如 "五月廿三"
 * @param yi 今日宜，如 "出行·摄影·会友"
 * @param ji 今日忌，如 "动土·开仓"
 * @param healthTip 健康提示，如 "宜晨间散步，拍摄光线柔和"
 */
data class CaptureContextInfo(
    val weather: String = "--",
    val lunarDate: String = "",
    val yi: String = "",
    val ji: String = "",
    val healthTip: String = ""
)

/**
 * 拍摄上下文面板
 *
 * 位于取景框左上角，展示与当下拍摄相关的环境与文化信息：
 * - 天气与温度
 * - 农历日期
 * - 今日宜忌
 * - 健康/出行提示
 *
 * 设计风格：液态玻璃质感 + 国潮排版，信息密度低，不干扰取景。
 */
@Composable
fun ContextInfoPanel(
    info: CaptureContextInfo,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && (info.weather.isNotEmpty() || info.lunarDate.isNotEmpty()),
        enter = fadeIn(animationSpec = tween(250, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .width(DesignSystemCultural.Dimensions.contextPanelWidth)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.medium))
                .background(DesignSystemCultural.Colors.contextPanelBackground)
                .border(
                    width = DesignSystem.Stroke.widthThin,
                    color = DesignSystemCultural.Colors.contextPanelBorder,
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 天气
            if (info.weather.isNotEmpty()) {
                ContextItem(
                    icon = Icons.Default.WbSunny,
                    iconTint = DesignSystemCultural.Colors.warmGold,
                    label = info.weather,
                    style = DesignSystemCultural.Typography.contextPrimary
                )
            }

            // 农历
            if (info.lunarDate.isNotEmpty()) {
                ContextItem(
                    icon = Icons.Default.CalendarMonth,
                    iconTint = DesignSystemCultural.Colors.jiQingLight,
                    label = info.lunarDate,
                    style = DesignSystemCultural.Typography.contextPrimary
                )
            }

            // 宜忌
            if (info.yi.isNotEmpty() || info.ji.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (info.yi.isNotEmpty()) {
                        ContextCulturalLine(
                            tag = "宜",
                            tagColor = DesignSystem.Colors.success,
                            text = info.yi
                        )
                    }
                    if (info.ji.isNotEmpty()) {
                        ContextCulturalLine(
                            tag = "忌",
                            tagColor = DesignSystemCultural.Colors.zhuSha,
                            text = info.ji
                        )
                    }
                }
            }

            // 健康提示
            if (info.healthTip.isNotEmpty()) {
                ContextItem(
                    icon = Icons.Default.Favorite,
                    iconTint = DesignSystem.Colors.error.copy(alpha = 0.85f),
                    label = info.healthTip,
                    style = DesignSystemCultural.Typography.contextSecondary
                )
            }
        }
    }
}

@Composable
private fun ContextItem(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(DesignSystemCultural.Dimensions.contextPanelItemHeight)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = style,
            color = DesignSystem.Colors.minimalLabel,
            maxLines = 1
        )
    }
}

@Composable
private fun ContextCulturalLine(
    tag: String,
    tagColor: Color,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = tag,
            style = DesignSystemCultural.Typography.contextCultural,
            color = tagColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = DesignSystemCultural.Typography.contextCultural,
            color = DesignSystem.Colors.minimalLabelSecondary,
            maxLines = 1
        )
    }
}

// MARK: - 农历与宜忌工具

private val lunarInfo = longArrayOf(
    0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
    0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
    0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
    0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
    0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
    0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
    0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
    0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
    0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
    0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
    0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
    0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
    0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
    0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
    0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0
)

private val lunarMonths = arrayOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
private val lunarDays = arrayOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
)

/**
 * 将公历日期转换为农历描述
 *
 * 覆盖 1900-2099 年，基于传统农历数据表。
 */
fun Date.toLunarDate(): String {
    val cal = Calendar.getInstance().apply { time = this@toLunarDate }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)

    if (year < 1900 || year > 2099) return "农历"

    val baseDate = Calendar.getInstance().apply { set(1900, 0, 31) }
    val targetDate = Calendar.getInstance().apply { set(year, month - 1, day) }
    var offset = ((targetDate.timeInMillis - baseDate.timeInMillis) / 86400000L).toInt()

    var lunarYear = 1900
    var daysInYear: Int
    while (offset > 0) {
        daysInYear = lunarYearDays(lunarYear)
        if (offset < daysInYear) break
        offset -= daysInYear
        lunarYear++
    }

    val leap = leapMonth(lunarYear)
    var lunarMonth = 1
    var isLeap = false
    while (true) {
        val daysInMonth = when {
            isLeap -> leapDays(lunarYear)
            (lunarInfo[lunarYear - 1900] and (0x10000 shr lunarMonth).toLong()) != 0L -> 30
            else -> 29
        }
        if (offset < daysInMonth) break
        offset -= daysInMonth

        if (leap != 0 && lunarMonth == leap && !isLeap) {
            isLeap = true
        } else {
            lunarMonth++
            isLeap = false
        }
    }
    val lunarDay = offset + 1

    return "${lunarMonths[lunarMonth - 1]}月${lunarDays[lunarDay - 1]}"
}

/**
 * 获取今日拍摄上下文信息（默认实现）
 *
 * 天气为占位符，实际应由定位/天气服务提供；
 * 农历、宜忌、健康提示基于当前日期生成。
 */
fun defaultCaptureContext(weather: String = "晴 24°"): CaptureContextInfo {
    val today = Date()
    val calendar = Calendar.getInstance().apply { time = today }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

    val yiList = listOf("摄影", "出行", "会友", "采风", "观景")
    val jiList = listOf("动土", "开仓", "安床", "嫁娶")
    // 根据星期简单轮转，实际应由黄历算法计算
    val yiIndex = (calendar.get(Calendar.DAY_OF_YEAR) % yiList.size)
    val jiIndex = ((calendar.get(Calendar.DAY_OF_YEAR) + 2) % jiList.size)

    val healthTip = when (dayOfWeek) {
        Calendar.SATURDAY, Calendar.SUNDAY -> "周末宜户外拍摄，注意防晒"
        Calendar.MONDAY -> "新周开始，宜记录晨光"
        Calendar.FRIDAY -> "周五傍晚光线柔和，适合街拍"
        else -> "今日光线平稳，宜城市漫游"
    }

    return CaptureContextInfo(
        weather = weather,
        lunarDate = today.toLunarDate(),
        yi = "${yiList[yiIndex]}·${yiList[(yiIndex + 1) % yiList.size]}",
        ji = jiList[jiIndex],
        healthTip = healthTip
    )
}

private fun lunarYearDays(year: Int): Int {
    var sum = 348
    for (i in 0x8000 downTo 0x8) {
        if ((lunarInfo[year - 1900] and i.toLong()) != 0L) sum++
    }
    return sum + leapDays(year)
}

private fun leapDays(year: Int): Int {
    if (leapMonth(year) == 0) return 0
    return if ((lunarInfo[year - 1899] and 0x0fL) == 0x0fL) 30 else 29
}

private fun leapMonth(year: Int): Int {
    return (lunarInfo[year - 1900] and 0xf).toInt()
}
