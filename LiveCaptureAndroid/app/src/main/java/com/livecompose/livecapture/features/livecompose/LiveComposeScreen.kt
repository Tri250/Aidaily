package com.livecompose.livecapture.features.livecompose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 构妙品牌页
 * 对应 iOS 的 LiveComposeView
 */
@Composable
fun LiveComposeScreen() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo 区域
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DesignSystem.Colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Camera, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "构妙 LiveCompose",
            style = DesignSystem.Typography.largeTitle,
            color = DesignSystem.Colors.textPrimary(),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "让每一次快门，都定格最美的瞬间",
            style = DesignSystem.Typography.body,
            color = DesignSystem.Colors.textSecondary(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 关于我们
        SectionCard(title = "关于我们", icon = Icons.Default.Star) {
            Text(
                "构妙 LiveCompose 致力于让每一位普通用户都能轻松拍出专业级构图照片。不同于传统相机的静态九宫格辅助线，我们通过 AI 实时分析取景画面，结合设备陀螺仪实现物理级追踪引导，主动「告诉」用户如何移动手机以获得最佳构图，并在对齐完美构图时自动拍摄。",
                style = DesignSystem.Typography.body,
                color = DesignSystem.Colors.textTertiary()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 项目仓库
        SectionCard(title = "项目仓库", icon = Icons.Default.Folder) {
            ProjectRow(
                icon = Icons.Default.Apps,
                name = "LiveCapture",
                desc = "Android 客户端 App — 基于 Jetpack Compose 构建，集成 Adacrop 美学裁切模型、陀螺仪运动追踪与实时构图引导。",
                url = "https://github.com/LiveCompose/LiveCapture"
            )
            Spacer(modifier = Modifier.height(8.dp))
            ProjectRow(
                icon = Icons.Default.Memory,
                name = "LiveCompose",
                desc = "核心模型仓库 — 包含 Adacrop 强化学习训练框架、模型定义与实验配置。",
                url = "https://github.com/LiveCompose/LiveCompose"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 资源链接
        SectionCard(title = "资源链接", icon = Icons.Default.Link) {
            LinkRow("GitHub 组织", "github.com/LiveCompose", "https://github.com/LiveCompose")
            LinkRow("HuggingFace 模型库", "huggingface.co/LiveCompose", "https://huggingface.co/LiveCompose")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 核心技术
        SectionCard(title = "核心技术", icon = Icons.Default.Settings) {
            TechRow(Icons.Default.Memory, "Adacrop 强化学习模型", "基于 Actor-Critic 架构的自适应美学裁切。")
            Divider()
            TechRow(Icons.Default.Sensors, "陀螺仪运动追踪", "实时采集设备角速度与加速度。")
            Divider()
            TechRow(Icons.Default.Visibility, "Vision 原生检测", "集成 ML Kit 人脸/人体检测。")
            Divider()
            TechRow(Icons.Default.Camera, "多镜头智能变焦", "支持超广角、广角、长焦等多种镜头。")
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        shape = DesignSystem.mediumRoundedShape,
        colors = CardDefaults.cardColors(containerColor = DesignSystem.Colors.backgroundSecondary())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary())
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ProjectRow(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, desc: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(DesignSystem.Colors.backgroundTertiary())
        .padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, style = DesignSystem.Typography.title3, color = DesignSystem.Colors.textPrimary(), modifier = Modifier.weight(1f))
            IconButton(onClick = { uriHandler.openUri(url) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, style = DesignSystem.Typography.subheadline, color = DesignSystem.Colors.textTertiary())
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DesignSystem.Colors.backgroundTertiary())
            .clickable { uriHandler.openUri(url) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(subtitle, style = DesignSystem.Typography.caption2, color = DesignSystem.Colors.textTertiary())
        }
        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = DesignSystem.Colors.textTertiary(), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TechRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = DesignSystem.Colors.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = DesignSystem.Typography.headline, color = DesignSystem.Colors.textPrimary())
            Text(desc, style = DesignSystem.Typography.caption1, color = DesignSystem.Colors.textTertiary())
        }
    }
}