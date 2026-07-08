package com.livecompose.livecapture.features.onboarding

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.flow.first

private val Context.whatsNewDataStore: DataStore<Preferences> by preferencesDataStore(name = "whats_new_prefs")

private val LAST_SEEN_VERSION_KEY = stringPreferencesKey("last_seen_version")

/**
 * 新功能条目
 */
data class WhatsNewItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

/**
 * 新功能内容（与 iOS FirstLaunchManager.getWhatsNewItems 对齐）
 */
object WhatsNewContent {
    val items: List<WhatsNewItem> = listOf(
        WhatsNewItem(Icons.Filled.CenterFocusStrong, "AI 智能构图增强", "全新端侧模型，构图识别更精准"),
        WhatsNewItem(Icons.Filled.FilterVintage, "新增 12 款滤镜", "42+ 款经典胶片滤镜，更多风格选择"),
        WhatsNewItem(Icons.Filled.Face, "智能美颜升级", "自然美颜算法优化，保留更多肌肤质感"),
        WhatsNewItem(Icons.Filled.MenuBook, "拍摄教程", "新增拍摄技巧指南，助你快速提升摄影水平")
    )
}

/**
 * 读取上次已查看"新功能"的版本号
 */
suspend fun getLastSeenWhatsNewVersion(context: Context): String {
    return context.whatsNewDataStore.data.first()[LAST_SEEN_VERSION_KEY] ?: ""
}

/**
 * 标记指定版本的"新功能"已查看
 */
suspend fun markWhatsNewSeen(context: Context, version: String) {
    context.whatsNewDataStore.edit { it[LAST_SEEN_VERSION_KEY] = version }
}

/**
 * 版本对比：是否需要展示"新功能"对话框
 *
 * 仅在版本升级（lastSeenVersion 非空且与当前版本不同）时展示；
 * 首次安装（lastSeenVersion 为空）由引导页处理，不展示新功能。
 */
fun shouldShowWhatsNew(currentVersion: String, lastSeenVersion: String): Boolean {
    return lastSeenVersion.isNotEmpty() && lastSeenVersion != currentVersion
}

/**
 * 新功能对话框
 *
 * @param currentVersion 当前应用版本号
 * @param onDismiss 关闭回调（调用方应在此时标记为已查看）
 */
@Composable
fun WhatsNewDialog(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(),
        icon = {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = DesignSystem.Colors.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "新功能",
                    style = DesignSystem.Typography.title1,
                    color = DesignSystem.Colors.textPrimary(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "版本 $currentVersion",
                    style = DesignSystem.Typography.subheadline,
                    color = DesignSystem.Colors.textTertiary(),
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.xSmall)
            ) {
                WhatsNewContent.items.forEach { item -> WhatsNewRow(item) }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(DesignSystem.CornerRadius.large),
                colors = ButtonDefaults.buttonColors(containerColor = DesignSystem.Colors.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "开始使用",
                    fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        shape = RoundedCornerShape(DesignSystem.CornerRadius.xLarge),
        containerColor = DesignSystem.Colors.backgroundPrimary()
    )
}

@Composable
private fun WhatsNewRow(item: WhatsNewItem) {
    Surface(
        color = DesignSystem.Colors.backgroundSecondary(),
        shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(DesignSystem.Spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = DesignSystem.Colors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(DesignSystem.CornerRadius.medium),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = DesignSystem.Colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(DesignSystem.Spacing.xSmall))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = DesignSystem.Typography.headline,
                    color = DesignSystem.Colors.textPrimary()
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = DesignSystem.Typography.subheadline,
                    color = DesignSystem.Colors.textSecondary(),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
