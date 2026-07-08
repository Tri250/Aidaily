package com.livecompose.livecapture.core.security

import org.junit.Assert.*
import org.junit.Test

/**
 * 加密工具单元测试
 */
class CryptoHelperTest {

    @Test
    fun `encrypt and decrypt round trip`() {
        val plaintext = "这是一条测试消息HelloWorld123!@#"
        val encrypted = CryptoHelper.encrypt(plaintext)
        assertNotNull(encrypted)
        assertNotEquals(plaintext, encrypted)

        val decrypted = CryptoHelper.decrypt(encrypted!!)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt produces different ciphertext each time`() {
        val plaintext = "相同内容的加密应产生不同密文"
        val encrypted1 = CryptoHelper.encrypt(plaintext)
        val encrypted2 = CryptoHelper.encrypt(plaintext)

        assertNotNull(encrypted1)
        assertNotNull(encrypted2)
        // 由于随机IV，相同明文的两次加密应产生不同密文
        assertNotEquals(encrypted1, encrypted2)

        // 但都能正确解密
        assertEquals(plaintext, CryptoHelper.decrypt(encrypted1!!))
        assertEquals(plaintext, CryptoHelper.decrypt(encrypted2!!))
    }

    @Test
    fun `encrypt with custom password`() {
        val plaintext = "带密码的加密测试"
        val password = "MySecretPassword123"

        val encrypted = CryptoHelper.encrypt(plaintext, password)
        assertNotNull(encrypted)

        val decrypted = CryptoHelper.decrypt(encrypted!!, password)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `wrong password fails to decrypt`() {
        val plaintext = "密码错误测试"
        val encrypted = CryptoHelper.encrypt(plaintext, "correct_password")

        val decrypted = CryptoHelper.decrypt(encrypted!!, "wrong_password")
        // 错误密码应导致解密失败
        assertNull(decrypted)
    }

    @Test
    fun `empty string encryption`() {
        val plaintext = ""
        val encrypted = CryptoHelper.encrypt(plaintext)
        assertNotNull(encrypted)

        val decrypted = CryptoHelper.decrypt(encrypted!!)
        assertEquals(plaintext, decrypted)
    }

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
    fun `encrypt long text`() {
        val plaintext = "这是一段很长的文本".repeat(100)
        val encrypted = CryptoHelper.encrypt(plaintext)
        assertNotNull(encrypted)

        val decrypted = CryptoHelper.decrypt(encrypted!!)
        assertEquals(plaintext, decrypted)
    }
}
