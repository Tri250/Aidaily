package com.livecompose.livecapture.features.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.livecompose.livecapture.ui.design.DesignSystem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.privacyDataStore by preferencesDataStore(name = "privacy_prefs")

private val PRIVACY_AGREED_KEY = booleanPreferencesKey("privacy_agreed")
private val PRIVACY_AGREED_VERSION_KEY = intPreferencesKey("privacy_agreed_version")

/** 当前隐私协议版本号，更新协议时需递增 */
private const val CURRENT_PRIVACY_VERSION = 1

/**
 * 检查用户是否已同意当前版本隐私协议
 */
suspend fun isPrivacyAgreed(context: android.content.Context): Boolean {
    return context.privacyDataStore.data.map { preferences ->
        preferences[PRIVACY_AGREED_KEY] == true &&
        (preferences[PRIVACY_AGREED_VERSION_KEY] ?: 0) >= CURRENT_PRIVACY_VERSION
    }.first()
}

/**
 * 标记用户已同意隐私协议
 */
private suspend fun setPrivacyAgreed(context: android.content.Context) {
    context.privacyDataStore.edit { preferences ->
        preferences[PRIVACY_AGREED_KEY] = true
        preferences[PRIVACY_AGREED_VERSION_KEY] = CURRENT_PRIVACY_VERSION
    }
}

/**
 * 隐私协议弹窗
 * 符合《个人信息保护法》要求，首次启动必须展示
 */
@Composable
fun PrivacyAgreementDialog(
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFullPolicy by remember { mutableStateOf(false) }

    if (showFullPolicy) {
        FullPrivacyPolicyDialog(
            onBack = { showFullPolicy = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = { /* 不允许点击外部关闭 */ },
        shape = RoundedCornerShape(16.dp),
        containerColor = DesignSystem.Colors.gray1(),
        title = {
            Text(
                text = "构妙 LiveCapture 隐私保护指引",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("欢迎使用构妙！在您使用我们的服务之前，请您仔细阅读")
                        withStyle(SpanStyle(
                            color = DesignSystem.Colors.primary,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("《构妙隐私政策》")
                        }
                        append("和")
                        withStyle(SpanStyle(
                            color = DesignSystem.Colors.primary,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("《用户服务协议》")
                        }
                        append("。")
                    },
                    color = DesignSystem.Colors.minimalLabel,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 权限说明
                Text(
                    text = "我们将会使用以下权限：",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val permissions = listOf(
                    "📷 相机权限 — 用于拍摄照片和视频",
                    "💾 存储权限 — 用于保存和读取您的照片",
                    "🔔 通知权限 — 用于幻影模式后台处理提醒",
                    "📳 传感器权限 — 用于拍摄稳定性检测"
                )

                permissions.forEach { perm ->
                    Text(
                        text = perm,
                        color = DesignSystem.Colors.minimalSecondaryLabel,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "我们承诺：仅在您授权后使用上述权限；您的照片数据仅存储在本地设备，不会上传至任何服务器；不会收集、分享或出售您的个人信息。",
                    color = DesignSystem.Colors.minimalSecondaryLabel,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showFullPolicy = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "查看完整隐私政策 >",
                        color = DesignSystem.Colors.primary,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            setPrivacyAgreed(context)
                            onAgree()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignSystem.Colors.primary
                    )
                ) {
                    Text(
                        text = "同意并继续",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onDisagree
                ) {
                    Text(
                        text = "不同意，退出应用",
                        color = DesignSystem.Colors.minimalSecondaryLabel,
                        fontSize = 13.sp
                    )
                }
            }
        }
    )
}

/**
 * 完整隐私政策页面
 */
@Composable
private fun FullPrivacyPolicyDialog(onBack: () -> Unit) {
    AlertDialog(
        onDismissRequest = onBack,
        shape = RoundedCornerShape(16.dp),
        containerColor = DesignSystem.Colors.gray1(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "构妙隐私政策",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(onClick = onBack) {
                    Text("关闭", color = DesignSystem.Colors.primary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                val policyText = """
更新日期：2026年7月1日
生效日期：2026年7月8日

一、我们如何收集和使用您的个人信息

构妙（以下简称"我们"）深知个人信息对您的重要性，我们将按照法律法规的规定，保护您的个人信息及隐私安全。

1.1 我们收集的信息

我们仅收集为您提供服务所必需的信息：

• 设备信息：为适配不同设备，我们可能收集设备型号、操作系统版本、屏幕分辨率等基础设备信息。
• 相机数据：仅在您主动使用拍摄功能时，通过相机获取图像数据，所有图像数据仅存储在您的设备本地，不会上传至任何服务器。
• 传感器数据：用于拍摄稳定性检测和水平仪功能，数据不会被存储或传输。
• 存储数据：用于保存和读取您的照片和缩略图，数据仅存储在设备本地。

1.2 我们不会收集的信息

我们明确承诺不会收集以下信息：
• 不会上传您的照片至任何服务器
• 不会收集您的通讯录、短信等敏感信息
• 不会收集您的精确定位信息（GPS数据仅保存在照片EXIF中，不上传）
• 不会追踪您的行为数据进行用户画像

二、我们如何存储和保护您的个人信息

2.1 数据存储
• 所有照片和元数据仅存储在您的设备本地
• 不使用任何云端存储服务
• 缩略图和编辑记录保存在应用私有目录中

2.2 数据安全
• 我们采取合理的数据安全措施保护您的信息
• 应用不包含任何第三方数据统计SDK
• 不使用任何远程服务器通信

2.3 数据保留
• 您的照片数据在您主动删除前持续保留
• 卸载应用将自动清除所有应用数据

三、您的权利

根据相关法律法规，您享有以下权利：
• 访问权：您可随时在应用内查看您的照片数据
• 删除权：您可随时删除任何照片或全部数据
• 撤回同意权：您可在系统设置中撤回已授予的权限

四、权限使用说明

• 相机权限（android.permission.CAMERA）：用于拍摄照片，是核心功能必需权限
• 存储权限（android.permission.READ_MEDIA_IMAGES）：用于幻影模式读取系统相册新照片
• 通知权限（android.permission.POST_NOTIFICATIONS）：用于幻影模式后台处理提醒
• 振动权限（android.permission.VIBRATE）：用于触觉反馈，增强拍摄体验

五、未成年人保护

我们高度重视对未成年人个人信息的保护。如果您是未满14周岁的未成年人，请在监护人的陪同和指导下使用本应用。

六、隐私政策的更新

我们可能会适时对本政策进行修订。当政策条款发生变更时，我们会在应用内通过弹窗方式通知您，并在您同意后生效。

七、联系我们

如您对本隐私政策有任何疑问，可通过以下方式联系我们：
• 邮箱：privacy@livecapture.cn
• 客服电话：400-000-0000
""".trimIndent()

                Text(
                    text = policyText,
                    color = DesignSystem.Colors.minimalLabel,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {}
    )
}
