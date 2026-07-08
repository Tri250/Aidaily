package com.livecompose.livecapture.features.compliance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 用户服务协议页面
 * 符合《个人信息保护法》要求，应用内可查看完整协议
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAgreementScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户服务协议", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C1C1E))
            )
        },
        containerColor = Color(0xFF1C1C1E)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = buildAgreementText(),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 隐私政策完整页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("构妙隐私政策", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C1C1E))
            )
        },
        containerColor = Color(0xFF1C1C1E)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = buildPrivacyPolicyText(),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * 第三方SDK清单页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdPartySDKScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("第三方SDK清单", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C1C1E))
            )
        },
        containerColor = Color(0xFF1C1C1E)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = buildSDKListText(),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

private fun buildAgreementText(): String = """
用户服务协议

更新日期：2026年7月1日
生效日期：2026年7月8日

欢迎使用"构妙 LiveCapture"（以下简称"本应用"或"构妙"）。

一、服务说明

1.1 构妙是一款基于端侧AI的智能构图辅助拍摄应用，提供实时构图指导、智能拍摄、照片编辑、LUT色彩预设等功能。

1.2 本应用所有图像处理功能均在本地设备上运行，不会将您的照片上传至任何远程服务器。

1.3 本应用可在无网络连接的环境下正常使用核心拍摄功能。

二、用户权利和义务

2.1 您承诺在使用本应用时遵守中华人民共和国相关法律法规，不得利用本应用从事任何违法违规活动。

2.2 您应妥善保管您的设备和账户信息，因设备丢失或账户泄露导致的损失由您自行承担。

2.3 您拥有对您拍摄的所有照片的完整权利，本应用不会对您的照片主张任何知识产权。

三、知识产权

3.1 本应用的所有软件代码、UI设计、AI模型、算法等知识产权归开发者所有。

3.2 本应用内置的LUT色彩预设、滤镜效果等创意内容的知识产权归开发者所有。

3.3 您通过本应用创作的照片作品，其知识产权归您所有。

四、免责声明

4.1 本应用按"现状"提供，不提供任何明示或暗示的保证。

4.2 本应用不保证AI构图建议在所有场景下都能达到您期望的效果。

4.3 因不可抗力、设备故障、系统维护等原因导致的服务中断，本应用不承担责任。

五、协议修改

5.1 我们有权根据需要修改本协议，修改后的协议将在应用内通知您。

5.2 如您不同意修改后的协议，您有权停止使用本应用。

六、适用法律

6.1 本协议受中华人民共和国法律管辖。

6.2 因本协议产生的争议，双方应协商解决；协商不成的，任何一方可向开发者所在地有管辖权的人民法院提起诉讼。

七、联系方式

如有任何疑问，请通过以下方式联系我们：
- 邮箱：privacy@livecapture.cn
- 客服电话：400-000-0000
""".trimIndent()

private fun buildPrivacyPolicyText(): String = """
构妙隐私政策

更新日期：2026年7月1日
生效日期：2026年7月8日

构妙（以下简称"我们"）深知个人信息对您的重要性，我们将按照法律法规的规定，保护您的个人信息及隐私安全。

一、我们如何收集和使用您的个人信息

1.1 我们收集的信息

我们仅收集为您提供服务所必需的信息：

- 设备信息：为适配不同设备，我们可能收集设备型号、操作系统版本、屏幕分辨率等基础设备信息。
- 相机数据：仅在您主动使用拍摄功能时，通过相机获取图像数据，所有图像数据仅存储在您的设备本地，不会上传至任何服务器。
- 传感器数据：用于拍摄稳定性检测和水平仪功能，数据不会被存储或传输。
- 存储数据：用于保存和读取您的照片和缩略图，数据仅存储在设备本地。

1.2 我们不会收集的信息

我们明确承诺不会收集以下信息：
- 不会上传您的照片至任何服务器
- 不会收集您的通讯录、短信等敏感信息
- 不会收集您的精确定位信息（GPS数据仅保存在照片EXIF中，不上传）
- 不会追踪您的行为数据进行用户画像

二、我们如何存储和保护您的个人信息

2.1 数据存储
- 所有照片和元数据仅存储在您的设备本地
- 不使用任何云端存储服务
- 缩略图和编辑记录保存在应用私有目录中

2.2 数据安全
- 我们采取合理的数据安全措施保护您的信息
- 应用不包含任何第三方数据统计SDK（除Bugly崩溃上报外）
- 不使用任何远程服务器通信

2.3 数据保留
- 您的照片数据在您主动删除前持续保留
- 卸载应用将自动清除所有应用数据

三、您的权利

根据《个人信息保护法》，您享有以下权利：
- 访问权：您可随时在应用内查看您的照片数据
- 删除权：您可随时删除任何照片或全部数据
- 撤回同意权：您可在系统设置中撤回已授予的权限
- 数据携带权：您可将照片导出至其他设备
- 投诉举报权：如您认为我们处理个人信息存在违规，可向监管部门投诉

四、权限使用说明

- 相机权限：用于拍摄照片，是核心功能必需权限
- 存储权限：用于保存和读取您的照片
- 通知权限：用于幻影模式后台处理提醒
- 振动权限：用于触觉反馈，增强拍摄体验
- 传感器权限：用于拍摄稳定性检测

五、未成年人保护

我们高度重视对未成年人个人信息的保护。如果您是未满14周岁的未成年人，请在监护人的陪同和指导下使用本应用。

六、隐私政策的更新

我们可能会适时对本政策进行修订。当政策条款发生变更时，我们会在应用内通过弹窗方式通知您，并在您同意后生效。

七、联系我们

如您对本隐私政策有任何疑问，可通过以下方式联系我们：
- 邮箱：privacy@livecapture.cn
- 客服电话：400-000-0000

八、ICP备案信息

应用名称：构妙 LiveCapture
ICP备案号：待备案（请前往工信部ICP备案系统完成备案）
""".trimIndent()

private fun buildSDKListText(): String = """
第三方SDK清单

根据《个人信息保护法》及相关法规要求，现将本应用集成的第三方SDK信息公示如下：

一、腾讯Bugly

功能：崩溃监控与异常上报
收集信息类型：设备标识信息（Android ID）、应用运行日志、崩溃堆栈信息
收集方式：自动采集
隐私政策：https://privacy.qq.com/document/preview/fc748b3d96224fdb825ea79e132c1a56
使用场景：应用崩溃时自动上报异常信息，用于问题排查和稳定性提升

二、微信OpenSDK

功能：微信分享
收集信息类型：设备标识信息、网络状态
收集方式：用户主动触发分享时采集
隐私政策：https://privacy.qq.com/
使用场景：用户分享照片至微信好友或朋友圈

三、TensorFlow Lite

功能：端侧AI模型推理
收集信息类型：不收集任何个人信息
收集方式：纯本地运行
隐私政策：不适用
使用场景：AI构图检测、场景分类等端侧AI推理

四、Google ML Kit

功能：人脸检测
收集信息类型：不收集任何个人信息
收集方式：纯本地运行
隐私政策：https://policies.google.com/privacy
使用场景：人像拍摄模式下的面部检测

五、AndroidX / Jetpack

功能：应用基础框架
收集信息类型：不收集任何个人信息
收集方式：纯本地运行
使用场景：应用UI框架、数据存储、生命周期管理

六、Kotlin Coroutines

功能：异步编程框架
收集信息类型：不收集任何个人信息
收集方式：纯本地运行
使用场景：图像处理、数据存储等异步操作

七、Coil

功能：图像加载
收集信息类型：不收集任何个人信息
收集方式：纯本地运行
使用场景：图库中照片缩略图加载

说明：
1. 除Bugly和微信SDK外，其他SDK均为纯本地运行，不收集任何个人信息。
2. Bugly仅在应用崩溃时上报必要信息，可在设置中查看。
3. 微信SDK仅在用户主动分享时使用。
4. 本应用不集成任何广告SDK、用户画像SDK、数据统计SDK。
""".trimIndent()