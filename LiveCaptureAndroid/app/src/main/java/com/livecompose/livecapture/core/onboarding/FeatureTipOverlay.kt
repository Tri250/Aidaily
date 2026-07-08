package com.livecompose.livecapture.core.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.featureTipDataStore: DataStore<Preferences> by preferencesDataStore(name = "feature_tip_prefs")

private val SHOWN_TIPS_KEY = stringSetPreferencesKey("shown_feature_tips")

/**
 * 功能提示气泡箭头方向
 */
enum class ArrowDirection { TOP, BOTTOM, LEFT, RIGHT }

/**
 * 功能提示数据模型
 */
data class FeatureTip(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val arrowDirection: ArrowDirection = ArrowDirection.TOP
)

/**
 * 预设功能提示（与 iOS FeatureTip.allTips 对齐）
 */
object FeatureTips {
    val flash = FeatureTip("flash", "闪光灯", "点击切换自动/开启/关闭闪光灯模式", Icons.Filled.Bolt, ArrowDirection.TOP)
    val timer = FeatureTip("timer", "定时拍摄", "设置 3/5/10 秒倒计时，轻松自拍", Icons.Filled.Timer, ArrowDirection.TOP)
    val filterStrip = FeatureTip("filter_strip", "滤镜选择", "左右滑动浏览 42+ 款滤镜，点击应用", Icons.Filled.Brush, ArrowDirection.BOTTOM)
    val beautyPanel = FeatureTip("beauty_panel", "美颜调节", "点击打开美颜面板，调节磨皮、美白等参数", Icons.Filled.Face, ArrowDirection.TOP)
    val gridToggle = FeatureTip("grid_toggle", "构图网格", "开启九宫格辅助线，帮助构图", Icons.Filled.GridOn, ArrowDirection.TOP)
    val aspectRatio = FeatureTip("aspect_ratio", "画幅比例", "切换全屏/1:1/3:4/9:16 多种画幅", Icons.Filled.AspectRatio, ArrowDirection.TOP)
    val gestureNav = FeatureTip("gesture_nav", "手势导航", "右滑打开相册，左滑打开设置，上滑打开社区", Icons.Filled.PanTool, ArrowDirection.BOTTOM)

    /** 全部预设提示，按展示顺序排列 */
    val all: List<FeatureTip> = listOf(
        flash, timer, filterStrip, beautyPanel, gridToggle, aspectRatio, gestureNav
    )
}

/**
 * 功能提示管理器
 *
 * 跟踪已展示的提示，按顺序弹出未展示的提示；使用 DataStore 持久化已展示记录。
 * 对应 iOS 端 FeatureTipManager。
 */
class FeatureTipManager(private val context: Context) {

    companion object {
        private const val TAG = "FeatureTipManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _shownTips = MutableStateFlow<Set<String>>(emptySet())
    val shownTips: StateFlow<Set<String>> = _shownTips.asStateFlow()

    private val _currentTip = MutableStateFlow<FeatureTip?>(null)
    val currentTip: StateFlow<FeatureTip?> = _currentTip.asStateFlow()

    private val _showTip = MutableStateFlow(false)
    val showTip: StateFlow<Boolean> = _showTip.asStateFlow()

    init {
        // 从 DataStore 加载已展示记录
        scope.launch {
            val saved = context.featureTipDataStore.data.first()[SHOWN_TIPS_KEY] ?: emptySet()
            _shownTips.value = saved
        }
    }

    /** 释放协程作用域，应在 DI 容器销毁时调用 */
    fun dispose() {
        scope.cancel()
    }

    /** 按顺序返回下一个未展示的提示 */
    fun showNextTip(): FeatureTip? = FeatureTips.all.firstOrNull { it.id !in _shownTips.value }

    /** 显示指定提示（如果未展示过） */
    fun showTipIfNeeded(tip: FeatureTip) {
        if (tip.id in _shownTips.value) return
        _currentTip.value = tip
        _showTip.value = true
    }

    /** 标记提示已展示并关闭气泡 */
    fun markTipAsShown(tip: FeatureTip) {
        val updated = _shownTips.value + tip.id
        _shownTips.value = updated
        _showTip.value = false
        _currentTip.value = null
        scope.launch {
            context.featureTipDataStore.edit { it[SHOWN_TIPS_KEY] = updated }
            AppLogger.i(TAG, "功能提示已展示: ${tip.id}")
        }
    }

    /** 检查提示是否已展示 */
    fun isTipShown(tip: FeatureTip): Boolean = tip.id in _shownTips.value

    /** 重置所有提示（重新展示） */
    fun resetAllTips() {
        _shownTips.value = emptySet()
        _showTip.value = false
        _currentTip.value = null
        scope.launch {
            context.featureTipDataStore.edit { it.remove(SHOWN_TIPS_KEY) }
            AppLogger.i(TAG, "已重置所有功能提示")
        }
    }
}

/**
 * 功能提示气泡视图
 */
@Composable
fun FeatureTipBubble(tip: FeatureTip, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = DesignSystem.Animation.bouncy,
        label = "tip_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = DesignSystem.Animation.easeOut,
        label = "tip_alpha"
    )

    LaunchedEffect(tip.id) { visible = true }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 上箭头（气泡位于目标下方时显示）
        if (tip.arrowDirection == ArrowDirection.BOTTOM) {
            ArrowTriangle(pointingUp = true)
        }

        // 气泡卡片
        Surface(
            color = DesignSystem.Colors.backgroundSecondary(),
            shape = RoundedCornerShape(DesignSystem.CornerRadius.large),
            border = BorderStroke(DesignSystem.Stroke.widthStandard, DesignSystem.Colors.primary.copy(alpha = 0.2f)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .scale(scale)
                .alpha(alpha)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(DesignSystem.Colors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tip.icon,
                            contentDescription = null,
                            tint = DesignSystem.Colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tip.title,
                        style = DesignSystem.Typography.headline,
                        color = DesignSystem.Colors.textPrimary()
                    )
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = DesignSystem.Colors.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                        onClick = onDismiss
                    ) {
                        Text(
                            text = "知道了",
                            style = DesignSystem.Typography.caption1,
                            color = DesignSystem.Colors.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tip.description,
                    style = DesignSystem.Typography.subheadline,
                    color = DesignSystem.Colors.textSecondary(),
                    lineHeight = 20.sp
                )
            }
        }

        // 下箭头（气泡位于目标上方时显示）
        if (tip.arrowDirection == ArrowDirection.TOP) {
            ArrowTriangle(pointingUp = false)
        }
    }
}

/** 气泡箭头（三角形） */
@Composable
private fun ArrowTriangle(pointingUp: Boolean) {
    val color = DesignSystem.Colors.backgroundSecondary()
    Canvas(modifier = Modifier.size(width = 14.dp, height = 10.dp)) {
        val path = Path()
        if (pointingUp) {
            path.moveTo(size.width / 2f, 0f)
            path.lineTo(0f, size.height)
            path.lineTo(size.width, size.height)
        } else {
            path.moveTo(size.width / 2f, size.height)
            path.lineTo(0f, 0f)
            path.lineTo(size.width, 0f)
        }
        path.close()
        drawPath(path, color)
    }
}

/**
 * 功能提示覆盖层
 *
 * 在指定对齐位置展示当前提示；点击背景或"知道了"按钮关闭并标记为已展示。
 *
 * @param manager 功能提示管理器
 * @param alignment 气泡在屏幕中的对齐位置
 */
@Composable
fun FeatureTipOverlay(
    manager: FeatureTipManager,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center
) {
    val currentTip by manager.currentTip.collectAsState()
    val showTip by manager.showTip.collectAsState()

    AnimatedVisibility(
        visible = showTip && currentTip != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val tip = currentTip
        if (tip != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { manager.markTipAsShown(tip) },
                contentAlignment = alignment
            ) {
                // 气泡容器：消费点击事件，避免点击气泡本身时误触发背景关闭
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { /* 消费点击，不关闭 */ }
                ) {
                    FeatureTipBubble(tip = tip) { manager.markTipAsShown(tip) }
                }
            }
        }
    }
}
