package com.livecompose.livecapture.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.livecompose.livecapture.core.logger.AppLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 本地数据加密工具
 * 用于加密敏感数据（如用户偏好、编辑会话状态等）
 * 使用 Android Keystore 保护的 AES-256-GCM 认证加密
 *
 * 安全特性：
 * - 密钥由 Android Keystore 硬件级保护，不可导出
 * - 每次加密使用随机 IV
 * - GCM 模式提供认证加密（防篡改）
 */
object CryptoHelper {

    private const val TAG = "CryptoHelper"
    private const val KEYSTORE_ALIAS = "livecapture_master_key"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /**
     * 加密数据
     * @param plaintext 明文字符串
     * @return Base64 编码的密文（格式：iv + ciphertext）
     */
    fun encrypt(plaintext: String): String? {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // 组合：IV + 密文
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            AppLogger.e(TAG, "加密失败", e)
            null
        }
    }

    /**
     * 解密数据
     * @param cipherText Base64 编码的密文
     * @return 明文字符串
     */
    fun decrypt(cipherText: String): String? {
        return try {
            val key = getOrCreateKey()
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)

            // 提取 IV
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            AppLogger.e(TAG, "解密失败", e)
            null
        }
    }

    /**
     * 生成安全随机字符串（用于令牌等）
     */
    fun generateRandomToken(length: Int = 32): String {
        val bytes = ByteArray(length)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * 获取或创建 Android Keystore 密钥
     * 密钥由硬件级安全存储保护，不可导出
     * 如果密钥因系统更新等原因失效，自动删除旧密钥并重新生成
     */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // 如果密钥已存在，直接返回
        try {
            keyStore.getEntry(KEYSTORE_ALIAS, null)?.let { entry ->
                return (entry as KeyStore.SecretKeyEntry).secretKey
            }
        } catch (e: Exception) {
            // 密钥可能已损坏，删除后重新生成
            AppLogger.w(TAG, "密钥读取失败，重新生成", e)
            try {
                keyStore.deleteEntry(KEYSTORE_ALIAS)
            } catch (_: Exception) {}
        }

        // 创建新密钥
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}