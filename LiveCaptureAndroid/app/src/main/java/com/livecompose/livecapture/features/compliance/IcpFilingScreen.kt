package com.livecompose.livecapture.features.compliance

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livecompose.livecapture.core.compliance.IcpFilingInfo
import com.livecompose.livecapture.core.logger.AppLogger
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * ICP 备案信息展示界面
 *
 * 对应 iOS 端 ICPFilingDetailView，展示主办单位、ICP 备案号、网安备案号等信息，
 * 并提供前往工信部备案系统查询的入口。
 *
 * 备案信息从 AndroidManifest <meta-data> 读取，详见 [IcpFilingInfo.fromManifest]。
 *
 * @param onBack 返回回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IcpFilingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val info = remember { IcpFilingInfo.fromManifest(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ICP 备案", color = DesignSystem.Colors.textPrimary(), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = DesignSystem.Colors.textPrimary())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DesignSystem.Colors.gray1())
            )
        },
        containerColor = DesignSystem.Colors.gray1()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 头部信息
            HeaderSection(isFiled = info.isFiled)

            // 备案信息卡片
            FilingInfoCard(info = info)

            // 前往工信部查询
            QueryLinkCard(
                queryUrl = info.queryUrl,
                onClick = { openMiitQuery(context, info.queryUrl) }
            )

            // 说明
            ExplanationSection()

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// MARK: - 头部

@Composable
private fun HeaderSection(isFiled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = DesignSystem.Colors.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "ICP 备案信息",
                color = DesignSystem.Colors.textPrimary(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "根据《中华人民共和国电信条例》和《互联网信息服务管理办法》规定，" +
                if (isFiled) "本应用已完成 ICP 备案。"
                else "本应用尚未完成 ICP 备案，请前往工信部备案系统完成备案。",
            color = DesignSystem.Colors.textTertiary(),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

// MARK: - 备案信息卡片

@Composable
private fun FilingInfoCard(info: IcpFilingInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DesignSystem.Colors.gray2())
    ) {
        InfoRow(
            icon = Icons.Default.Apartment,
            title = "主办单位",
            value = info.companyName.ifBlank { "待备案" }
        )
        HorizontalDivider(color = DesignSystem.Colors.gray3(), modifier = Modifier.padding(start = 44.dp))
        InfoRow(
            icon = Icons.Default.Badge,
            title = "ICP 备案号",
            value = info.icpNumber.ifBlank { "待备案" }
        )
        if (!info.networkSecurityNumber.isNullOrBlank()) {
            HorizontalDivider(color = DesignSystem.Colors.gray3(), modifier = Modifier.padding(start = 44.dp))
            InfoRow(
                icon = Icons.Default.Lock,
                title = "网安备案号",
                value = info.networkSecurityNumber
            )
        }
        if (info.auditDate.isNotBlank()) {
            HorizontalDivider(color = DesignSystem.Colors.gray3(), modifier = Modifier.padding(start = 44.dp))
            InfoRow(
                icon = Icons.Default.Public,
                title = "审核日期",
                value = info.auditDate
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DesignSystem.Colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            title,
            color = DesignSystem.Colors.textPrimary(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            value,
            color = DesignSystem.Colors.textTertiary(),
            fontSize = 15.sp
        )
    }
}

// MARK: - 工信部查询入口

@Composable
private fun QueryLinkCard(queryUrl: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DesignSystem.Colors.gray2())
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = null,
            tint = DesignSystem.Colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            "前往工信部备案系统查询",
            color = DesignSystem.Colors.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ArrowOutward,
            contentDescription = "打开链接",
            tint = DesignSystem.Colors.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * 通过系统浏览器打开工信部备案查询页面。
 */
private fun openMiitQuery(context: android.content.Context, url: String) {
    val target = url.ifBlank { IcpFilingInfo.DEFAULT_QUERY_URL }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        AppLogger.w("IcpFiling", "无法打开工信部查询链接: ${e.message}")
    }
}

// MARK: - 说明

@Composable
private fun ExplanationSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "什么是 ICP 备案？",
            color = DesignSystem.Colors.textPrimary(),
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "ICP 备案（Internet Content Provider 备案）是中国大陆境内提供互联网信息服务的网站和应用的法定要求。" +
                "所有在中国大陆运营的网站和应用都必须完成 ICP 备案。",
            color = DesignSystem.Colors.textTertiary(),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}
