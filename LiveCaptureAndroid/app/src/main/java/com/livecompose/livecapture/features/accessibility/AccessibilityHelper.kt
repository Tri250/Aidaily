package com.livecompose.livecapture.features.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.core.content.getSystemService

/**
 * 无障碍辅助工具类
 *
 * 提供统一的 Compose 无障碍适配接口
 * 封装语义修饰符和 TalkBack 播报功能
 */
object AccessibilityHelper {

    /**
     * 检查是否启用了无障碍服务（如 TalkBack）
     */
    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService<AccessibilityManager>() ?: return false
        return am.isEnabled && am.isTouchExplorationEnabled
    }

    /**
     * 通过 TalkBack 播报消息
     * @param context 上下文
     * @param message 要播报的消息内容
     */
    fun announce(context: Context, message: String) {
        val am = context.getSystemService<AccessibilityManager>() ?: return
        val event = android.view.accessibility.AccessibilityEvent.obtain(
            android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT
        )
        event.text.add(message)
        event.className = AccessibilityHelper::class.java.name
        event.packageName = context.packageName
        am.sendAccessibilityEvent(event)
    }
}

// MARK: - Compose 无障碍修饰符扩展

/**
 * 设置语义无障碍标签（contentDescription）
 * 便捷扩展函数，为 Compose 组件添加语义描述
 *
 * 使用方式：
 * ```kotlin
 * Button(onClick = { ... }) {
 *     Text("拍照")
 * }.semanticsAccessibility("拍照按钮", "双击拍摄照片")
 * ```
 */
fun Modifier.semanticsAccessibility(
    contentDescription: String,
    stateDescription: String? = null,
    testTag: String? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
        stateDescription?.let { this.stateDescription = it }
        testTag?.let { this.testTag = it }
    }
}

/**
 * 为可点击元素设置语义角色为按钮
 * 同时添加无障碍描述
 */
fun Modifier.semanticsButton(
    contentDescription: String,
    stateDescription: String? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
        stateDescription?.let { this.stateDescription = it }
        this.role = Role.Button
    }
}

/**
 * 为图像元素设置无障碍描述
 */
fun Modifier.semanticsImage(contentDescription: String): Modifier {
    return this.semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
        this.role = Role.Image
    }
}

/**
 * 为开关元素设置无障碍描述
 */
fun Modifier.semanticsSwitch(
    contentDescription: String,
    stateDescription: String? = null
): Modifier {
    return this.semantics(mergeDescendants = true) {
        this.contentDescription = contentDescription
        stateDescription?.let { this.stateDescription = it }
        this.role = Role.Switch
    }
}

/**
 * 清除语义信息（用于装饰性元素）
 */
fun Modifier.clearSemantics(): Modifier {
    return this.semantics(mergeDescendants = false) {}
}

// MARK: - Composable 扩展函数

/**
 * 在 Composable 中获取 Context 并播报无障碍消息
 *
 * 使用方式：
 * ```kotlin
 * announceForAccessibility("照片已保存")
 * ```
 */
@Composable
fun announceForAccessibility(message: String) {
    val context = LocalContext.current
    AccessibilityHelper.announce(context, message)
}