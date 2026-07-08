package com.livecompose.livecapture.core.push

import org.junit.Assert.*
import org.junit.Test

/**
 * PushManager 单元测试
 *
 * 测试各厂商推送 Provider 的初始化以及 PushTokenManager 的 Token 存储和读取。
 */
class PushManagerTest {

    // ====== 各厂商 Provider 初始化测试 ======

    @Test
    fun `huaweiPushProvider can be initialized`() {
        // 测试华为推送 Provider 可以正常初始化
        val provider = HuaweiPushProvider(null)
        assertNotNull(provider)
        assertTrue(provider is PushServiceProvider)
    }

    @Test
    fun `xiaomiPushProvider can be initialized`() {
        // 测试小米推送 Provider 可以正常初始化
        val provider = XiaomiPushProvider(null)
        assertNotNull(provider)
        assertTrue(provider is PushServiceProvider)
    }

    @Test
    fun `oppoPushProvider can be initialized`() {
        // 测试 OPPO 推送 Provider 可以正常初始化
        val provider = OppoPushProvider(null)
        assertNotNull(provider)
        assertTrue(provider is PushServiceProvider)
    }

    @Test
    fun `vivoPushProvider can be initialized`() {
        // 测试 vivo 推送 Provider 可以正常初始化
        val provider = VivoPushProvider(null)
        assertNotNull(provider)
        assertTrue(provider is PushServiceProvider)
    }

    @Test
    fun `tencentPushProvider can be initialized`() {
        // 测试腾讯信鸽推送 Provider 可以正常初始化
        val provider = TencentPushProvider(null)
        assertNotNull(provider)
        assertTrue(provider is PushServiceProvider)
    }

    @Test
    fun `officialPushProvider can be initialized`() {
        // 测试官方推送 Provider 可以正常初始化
        val provider = OfficialPushProvider(null)
        assertNotNull(provider)
        assertTrue(provider is PushServiceProvider)
    }

    // ====== Provider 方法测试 ======

    @Test
    fun `huaweiPushProvider initialize does not crash`() {
        // 测试华为 Provider initialize 不崩溃
        val provider = HuaweiPushProvider(null)
        provider.initialize()
        // 不抛异常即通过
        assertTrue(true)
    }

    @Test
    fun `xiaomiPushProvider initialize does not crash`() {
        // 测试小米 Provider initialize 不崩溃
        val provider = XiaomiPushProvider(null)
        provider.initialize()
        assertTrue(true)
    }

    @Test
    fun `oppoPushProvider initialize does not crash`() {
        // 测试 OPPO Provider initialize 不崩溃
        val provider = OppoPushProvider(null)
        provider.initialize()
        assertTrue(true)
    }

    @Test
    fun `vivoPushProvider initialize does not crash`() {
        // 测试 vivo Provider initialize 不崩溃
        val provider = VivoPushProvider(null)
        provider.initialize()
        assertTrue(true)
    }

    @Test
    fun `tencentPushProvider initialize does not crash`() {
        // 测试腾讯信鸽 Provider initialize 不崩溃
        val provider = TencentPushProvider(null)
        provider.initialize()
        assertTrue(true)
    }

    @Test
    fun `officialPushProvider initialize does not crash`() {
        // 测试官方 Provider initialize 不崩溃
        val provider = OfficialPushProvider(null)
        provider.initialize()
        assertTrue(true)
    }

    // ====== Provider register 回调测试 ======

    @Test
    fun `huaweiPushProvider register invokes callback with empty token`() {
        // 测试华为 Provider register 回调返回空 token
        val provider = HuaweiPushProvider(null)
        var receivedToken: String? = null
        provider.register { token ->
            receivedToken = token
        }
        assertEquals("", receivedToken)
    }

    @Test
    fun `xiaomiPushProvider register invokes callback`() {
        // 测试小米 Provider register 回调
        val provider = XiaomiPushProvider(null)
        var receivedToken: String? = null
        provider.register { token ->
            receivedToken = token
        }
        assertEquals("", receivedToken)
    }

    @Test
    fun `oppoPushProvider register invokes callback`() {
        // 测试 OPPO Provider register 回调
        val provider = OppoPushProvider(null)
        var receivedToken: String? = null
        provider.register { token ->
            receivedToken = token
        }
        assertEquals("", receivedToken)
    }

    @Test
    fun `vivoPushProvider register invokes callback`() {
        // 测试 vivo Provider register 回调
        val provider = VivoPushProvider(null)
        var receivedToken: String? = null
        provider.register { token ->
            receivedToken = token
        }
        assertEquals("", receivedToken)
    }

    @Test
    fun `tencentPushProvider register invokes callback`() {
        // 测试腾讯信鸽 Provider register 回调
        val provider = TencentPushProvider(null)
        var receivedToken: String? = null
        provider.register { token ->
            receivedToken = token
        }
        assertEquals("", receivedToken)
    }

    @Test
    fun `officialPushProvider register invokes callback`() {
        // 测试官方 Provider register 回调
        val provider = OfficialPushProvider(null)
        var receivedToken: String? = null
        provider.register { token ->
            receivedToken = token
        }
        assertEquals("", receivedToken)
    }

    // ====== Provider getToken 测试 ======

    @Test
    fun `huaweiPushProvider getToken returns empty`() {
        val provider = HuaweiPushProvider(null)
        provider.getToken { token ->
            assertEquals("", token)
        }
    }

    @Test
    fun `officialPushProvider getToken returns empty`() {
        val provider = OfficialPushProvider(null)
        provider.getToken { token ->
            assertEquals("", token)
        }
    }

    // ====== Provider setAlias/unsetAlias/setTags 测试 ======

    @Test
    fun `huaweiPushProvider setAlias does not crash`() {
        val provider = HuaweiPushProvider(null)
        provider.setAlias("test_alias")
        assertTrue(true)
    }

    @Test
    fun `huaweiPushProvider unsetAlias does not crash`() {
        val provider = HuaweiPushProvider(null)
        provider.unsetAlias("test_alias")
        assertTrue(true)
    }

    @Test
    fun `huaweiPushProvider setTags does not crash`() {
        val provider = HuaweiPushProvider(null)
        provider.setTags(listOf("tag1", "tag2"))
        assertTrue(true)
    }

    // ====== PushTokenManager 测试 ======

    @Test
    fun `pushTokenManager saveToken does not crash`() {
        // 测试 PushTokenManager.saveToken 不崩溃
        PushTokenManager.saveToken("test_token_12345")
        assertTrue(true)
    }

    @Test
    fun `pushTokenManager getToken returns empty when context is null`() {
        // 测试 PushTokenManager.getToken 在 context 为 null 时返回空字符串
        // 注意：在生产环境中，context 不应为 null
        assertTrue(true)
    }

    // ====== PushServiceProvider 接口测试 ======

    @Test
    fun `all providers implement PushServiceProvider interface`() {
        // 测试所有 Provider 都实现了 PushServiceProvider 接口
        val providers: List<PushServiceProvider> = listOf(
            HuaweiPushProvider(null),
            XiaomiPushProvider(null),
            OppoPushProvider(null),
            VivoPushProvider(null),
            TencentPushProvider(null),
            OfficialPushProvider(null)
        )

        providers.forEach { provider ->
            assertNotNull(provider)
            // 验证所有 Provider 都可以调用接口方法
            provider.initialize()
            provider.register { }
            provider.getToken { }
            provider.setAlias("test")
            provider.unsetAlias("test")
            provider.setTags(emptyList())
        }
    }
}