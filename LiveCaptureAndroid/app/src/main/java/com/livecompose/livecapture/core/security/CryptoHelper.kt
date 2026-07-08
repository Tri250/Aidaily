package com.livecompose.livecapture.core.security

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 本地数据加密工具
 * 用于加密敏感数据（如用户偏好、编辑会话状态等）
 * 使用 AES-256-GCM 认证加密
 */
object CryptoHelper {

    private const val TAG = "CryptoHelper"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PBKDF2_ITERATIONS = 10000
    private const val SALT_LENGTH = 16

    // 应用级密钥派生盐（每个应用唯一）
    private const val APP_SALT = "LiveCapture2026SecuritySalt"

    /**
     * 加密数据
     * @param plaintext 明文字符串
     * @param password 用户级密码（可选，默认使用应用级密钥）
     * @return Base64 编码的密文（格式：salt + iv + ciphertext）
     */
    fun encrypt(plaintext: String, password: String? = null): String? {
        return try {
            val key = deriveKey(password ?: APP_SALT)
            val cipher = Cipher.getInstance(ALGORITHM)

            // 生成随机 IV
            val iv = ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

            // 组合：IV + 密文
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "加密失败", e)
            null
        }
    }

    /**
     * 解密数据
     * @param cipherText Base64 编码的密文
     * @param password 用户级密码（可选，需与加密时一致）
     * @return 明文字符串
     */
    fun decrypt(cipherText: String, password: String? = null): String? {
        return try {
            val key = deriveKey(password ?: APP_SALT)
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
            Log.e(TAG, "解密失败", e)
            null
        }
    }

    /**
     * 从密码派生 AES 密钥
     * 使用 PBKDF2WithHmacSHA256 进行密钥派生
     */
    private fun deriveKey(password: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            password.toCharArray(),
            APP_SALT.toByteArray(Charsets.UTF_8),
            PBKDF2_ITERATIONS,
            KEY_SIZE
        )
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * 生成安全随机字符串（用于令牌等）
     */
    fun generateRandomToken(length: Int = 32): String {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
