package com.livecompose.livecapture.features.compliance

import org.junit.Assert.*
import org.junit.Test

/**
 * ComplianceScreens 合规页面文本内容单元测试
 *
 * 测试隐私政策、用户协议、SDK 清单的文本内容不为空，
 * 确保合规页面内容完整。
 */
class ComplianceScreensTest {

    // ====== 隐私政策文本测试 ======

    @Test
    fun `privacyPolicyText is not empty`() {
        // 通过反射调用 buildPrivacyPolicyText
        val text = callPrivateString("buildPrivacyPolicyText")
        assertNotNull("隐私政策文本不应为 null", text)
        assertTrue("隐私政策文本不应为空", text.isNotEmpty())
    }

    @Test
    fun `privacyPolicyText contains key sections`() {
        // 测试隐私政策文本包含关键章节
        val text = callPrivateString("buildPrivacyPolicyText")
        assertTrue("应包含"个人信息法"", text.contains("个人信息"))
        assertTrue("应包含"收集"", text.contains("收集"))
        assertTrue("应包含"存储"", text.contains("存储"))
        assertTrue("应包含"权利"", text.contains("权利"))
    }

    @Test
    fun `privacyPolicyText contains app name`() {
        // 测试隐私政策包含应用名称
        val text = callPrivateString("buildPrivacyPolicyText")
        assertTrue("应包含"秒简"", text.contains("秒简"))
    }

    @Test
    fun `privacyPolicyText contains contact info`() {
        // 测试隐私政策包含联系方式
        val text = callPrivateString("buildPrivacyPolicyText")
        assertTrue("应包含联系邮箱", text.contains("privacy@livecapture.cn"))
    }

    // ====== 用户协议文本测试 ======

    @Test
    fun `userAgreementText is not empty`() {
        // 测试用户协议文本不为空
        val text = callPrivateString("buildAgreementText")
        assertNotNull("用户协议文本不应为 null", text)
        assertTrue("用户协议文本不应为空", text.isNotEmpty())
    }

    @Test
    fun `userAgreementText contains key sections`() {
        // 测试用户协议文本包含关键章节
        val text = callPrivateString("buildAgreementText")
        assertTrue("应包含"服务说明"", text.contains("服务"))
        assertTrue("应包含"知识产权"", text.contains("知识产权"))
        assertTrue("应包含"免责声明"", text.contains("免责"))
        assertTrue("应包含"适用法律"", text.contains("法律"))
    }

    @Test
    fun `userAgreementText contains app name`() {
        // 测试用户协议包含应用名称
        val text = callPrivateString("buildAgreementText")
        assertTrue("应包含"秒简"", text.contains("秒简"))
    }

    @Test
    fun `userAgreementText contains effective date`() {
        // 测试用户协议包含生效日期
        val text = callPrivateString("buildAgreementText")
        assertTrue("应包含生效日期", text.contains("2026"))
    }

    // ====== SDK 清单文本测试 ======

    @Test
    fun `sdkListText is not empty`() {
        // 测试 SDK 清单文本不为空
        val text = callPrivateString("buildSDKListText")
        assertNotNull("SDK清单文本不应为 null", text)
        assertTrue("SDK清单文本不应为空", text.isNotEmpty())
    }

    @Test
    fun `sdkListText contains bugly`() {
        // 测试 SDK 清单包含腾讯 Bugly
        val text = callPrivateString("buildSDKListText")
        assertTrue("应包含"Bugly"", text.contains("Bugly"))
    }

    @Test
    fun `sdkListText contains wechat`() {
        // 测试 SDK 清单包含微信 SDK
        val text = callPrivateString("buildSDKListText")
        assertTrue("应包含"微信"", text.contains("微信"))
    }

    @Test
    fun `sdkListText contains tensorflow`() {
        // 测试 SDK 清单包含 TensorFlow Lite
        val text = callPrivateString("buildSDKListText")
        assertTrue("应包含"TensorFlow"", text.contains("TensorFlow"))
    }

    @Test
    fun `sdkListText contains mlkit`() {
        // 测试 SDK 清单包含 Google ML Kit
        val text = callPrivateString("buildSDKListText")
        assertTrue("应包含"ML Kit"", text.contains("ML Kit"))
    }

    @Test
    fun `sdkListText contains privacy statement`() {
        // 测试 SDK 清单包含隐私说明
        val text = callPrivateString("buildSDKListText")
        assertTrue("应包含隐私说明", text.contains("不收集任何个人信息"))
    }

    // ====== 辅助方法 ======

    /**
     * 通过反射调用 ComplianceScreensKt 中的私有函数来获取文本内容。
     */
    private fun callPrivateString(methodName: String): String {
        // 使用反射获取 ComplianceScreensKt 类的私有方法
        val clazz = Class.forName("com.livecompose.livecapture.features.compliance.ComplianceScreensKt")
        val method = clazz.getDeclaredMethod(methodName)
        method.isAccessible = true
        return method.invoke(null) as String
    }
}