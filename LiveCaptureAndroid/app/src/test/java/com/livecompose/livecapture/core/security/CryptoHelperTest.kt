package com.livecompose.livecapture.core.security

import org.junit.Assert.*
import org.junit.Test

/**
 * 加密工具单元测试
 *
 * 注意：CryptoHelper 使用 Android Keystore 存储密钥，
 * 因此加解密测试需要在 Android 设备或模拟器上运行（androidTest）。
 * 以下测试覆盖非 Keystore 依赖的功能。
 */
class CryptoHelperTest {

    @Test
    fun `generateRandomToken produces unique tokens`() {
        val token1 = CryptoHelper.generateRandomToken()
        val token2 = CryptoHelper.generateRandomToken()

        assertNotNull(token1)
        assertNotNull(token2)
        assertNotEquals(token1, token2)
        assertTrue(token1.length >= 32)
    }

    @Test
    fun `generateRandomToken with custom length`() {
        val token = CryptoHelper.generateRandomToken(16)
        assertNotNull(token)
        assertEquals(22, token.length) // Base64 URL-safe without padding: ceil(16 * 4/3) = 22
    }

    @Test
    fun `generateRandomToken produces URL-safe characters`() {
        val token = CryptoHelper.generateRandomToken(64)
        // URL-safe Base64 字符集：A-Z, a-z, 0-9, -, _
        val urlSafePattern = Regex("^[A-Za-z0-9_-]+$")
        assertTrue(urlSafePattern.matches(token))
    }
}