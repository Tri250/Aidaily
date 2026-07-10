package com.livecompose.livecapture.features.profile

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.core.permission.PermissionManager

/**
 * 我的页面 - 专业级 UX 设计
 *
 * 对标 iOS 设置页面的专业设计语言
 * 包含：用户信息、拍摄数据、设置、帮助、关于
 */
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToShootingGuide: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAgreement: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToIcp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContainer = remember { AppContainer.getInstance(context) }
    val permManager = remember { PermissionManager.getInstance(context) }
    val permSummary by remember { derivedStateOf { permManager.getPermissionSummary() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.backgroundPrimary())
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部区域：用户头像 + 信息卡片
        ProfileHeaderSection()

        Spacer(Modifier.height(DesignSystem.Spacing.medium))

        // 拍摄数据统计
        PhotographyStatsSection()

        Spacer(Modifier.height(DesignSystem.Spacing.medium))

        // 权限状态
        PermissionStatusSection(permSummary)

        Spacer(Modifier.height(DesignSystem.Spacing.medium))

        // 功能入口
        SettingsGroup(title = "功能") {
            ProfileRow(
                icon = Icons.Outlined.Settings,
                title = "相机设置",
                subtitle = "拍摄参数、构图、性能",
                onClick = onNavigateToSettings
            )
            ProfileRow(
                icon = Icons.Outlined.CameraAlt,
                title = "拍摄教程",
                subtitle = "构图技巧、光线运用、人像摄影",
                onClick = onNavigateToShootingGuide
            )
            ProfileRow(
                icon = Icons.Outlined.PhotoFilter,
                title = "滤镜社区",
                subtitle = "探索和下载社区滤镜",
                onClick = onNavigateToCommunity
            )
        }

        Spacer(Modifier.height(DesignSystem.Spacing.medium))

        // 合规入口
        SettingsGroup(title = "隐私与合规") {
            ProfileRow(
                icon = Icons.Outlined.Shield,
                title = "隐私政策",
                subtitle = "了解我们如何保护你的数据",
                onClick = onNavigateToPrivacy
            )
            ProfileRow(
                icon = Icons.Outlined.Description,
                title = "用户协议",
                subtitle = "使用条款和服务协议",
                onClick = onNavigateToAgreement
            )
            ProfileRow(
                icon = Icons.Outlined.Gavel,
                title = "ICP 备案",
                subtitle = "ICP 备案信息公示",
                onClick = onNavigateToIcp
            )
        }

        Spacer(Modifier.height(DesignSystem.Spacing.medium))

        // 关于
        SettingsGroup(title = "关于") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small, vertical = DesignSystem.Spacing.xSmall),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "版本",
                    style = DesignSystem.Typography.callout,
                    color = DesignSystem.Colors.textPrimary()
                )
                Text(
                    "v1.1.7",
                    style = DesignSystem.Typography.callout,
                    color = DesignSystem.Colors.textTertiary()
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignSystem.Spacing.small, vertical = DesignSystem.Spacing.xSmall),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "构建",
                    style = DesignSystem.Typography.callout,
                    color = DesignSystem.Colors.textPrimary()
                )
                Text(
                    "2026 正式版",
                    style = DesignSystem.Typography.callout,
                    color = DesignSystem.Colors.textTertiary()
                )
            }
        }

        Spacer(Modifier.height(DesignSystem.Spacing.xxLarge))
    }
}

// MARK: - 头部区域

@Composable
private fun ProfileHeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DesignSystem.Spacing.medium)
    ) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = DesignSystem.Spacing.medium)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.xxLarge))
                .background(
                    Brush.linearGradient(
                        listOf(
                            DesignSystem.Colors.primary.copy(alpha = 0.15f),
                            DesignSystem.Colors.secondary.copy(alpha = 0.08f),
                            DesignSystem.Colors.backgroundPrimary()
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    DesignSystem.Colors.primary,
                                    DesignSystem.Colors.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = DesignSystem.Colors.minimalLabel,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(DesignSystem.Spacing.xSmall))

                Text(
                    "构妙",
                    style = DesignSystem.Typography.title2,
                    color = DesignSystem.Colors.textPrimary()
                )

                Spacer(Modifier.height(DesignSystem.Spacing.xxxSmall))

                Text(
                    "记录每一刻精彩",
                    style = DesignSystem.Typography.subheadline,
                    color = DesignSystem.Colors.textTertiary()
                )
            }
        }
    }
}

// MARK: - 拍摄数据统计

@Composable
private fun PhotographyStatsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.medium)
    ) {
        Text(
            "拍摄数据",
            style = DesignSystem.Typography.title3,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.padding(horizontal = DesignSystem.Spacing.xxxSmall, vertical = DesignSystem.Spacing.xxSmall)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignSystem.Spacing.small)
        ) {
            StatCard(
                icon = Icons.Filled.PhotoCamera,
                value = "0",
                label = "照片",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Videocam,
                value = "0",
                label = "视频",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Filled.Collections,
                value = "0",
                label = "相册",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
            .background(DesignSystem.Colors.backgroundSecondary())
            .padding(DesignSystem.Spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = DesignSystem.Colors.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(DesignSystem.Spacing.xxxSmall))
        Text(
            value,
            style = DesignSystem.Typography.title2,
            color = DesignSystem.Colors.textPrimary(),
            textAlign = TextAlign.Center
        )
        Text(
            label,
            style = DesignSystem.Typography.caption1,
            color = DesignSystem.Colors.textTertiary(),
            textAlign = TextAlign.Center
        )
    }
}

// MARK: - 权限状态

@Composable
private fun PermissionStatusSection(summary: PermissionManager.PermissionSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "权限状态",
                style = DesignSystem.Typography.title3,
                color = DesignSystem.Colors.textPrimary(),
                modifier = Modifier.padding(horizontal = DesignSystem.Spacing.xxxSmall, vertical = DesignSystem.Spacing.xxSmall)
            )
            Surface(
                shape = CircleShape,
                color = if (summary.coreGranted) DesignSystem.Colors.successBg
                else DesignSystem.Colors.warningBg
            ) {
                Text(
                    if (summary.coreGranted) "核心就绪" else "待授权",
                    style = DesignSystem.Typography.caption2,
                    color = if (summary.coreGranted) DesignSystem.Colors.success
                    else DesignSystem.Colors.warning,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                .background(DesignSystem.Colors.backgroundSecondary())
                .padding(DesignSystem.Spacing.small),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PermissionDot(label = "相机", granted = summary.camera)
            PermissionDot(label = "存储", granted = summary.storage)
            PermissionDot(label = "麦克风", granted = summary.microphone)
            PermissionDot(label = "位置", granted = summary.location)
            PermissionDot(label = "通知", granted = summary.notification)
        }
    }
}

@Composable
private fun PermissionDot(label: String, granted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (granted) DesignSystem.Colors.success else DesignSystem.Colors.gray3())
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = DesignSystem.Typography.caption2,
            color = if (granted) DesignSystem.Colors.textSecondary() else DesignSystem.Colors.textTertiary()
        )
    }
}

// MARK: - 设置分组

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSystem.Spacing.medium)
    ) {
        Text(
            title,
            style = DesignSystem.Typography.title3,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.padding(horizontal = DesignSystem.Spacing.xxxSmall, vertical = DesignSystem.Spacing.xxSmall)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                .background(DesignSystem.Colors.backgroundSecondary()),
            content = content
        )
    }
}

// MARK: - 设置行

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(DesignSystem.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(DesignSystem.CornerRadius.small))
                .background(DesignSystem.Colors.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = DesignSystem.Colors.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(DesignSystem.Spacing.xSmall))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = DesignSystem.Typography.callout,
                color = DesignSystem.Colors.textPrimary()
            )
            Text(
                subtitle,
                style = DesignSystem.Typography.caption1,
                color = DesignSystem.Colors.textTertiary(),
                maxLines = 1
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = DesignSystem.Colors.textQuaternary(),
            modifier = Modifier.size(18.dp)
        )
    }
    if (title != "ICP 备案") {
        HorizontalDivider(
            modifier = Modifier.padding(start = 52.dp),
            thickness = DesignSystem.Stroke.widthThin,
            color = DesignSystem.Stroke.subtle()
        )
    }
}