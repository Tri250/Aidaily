package com.livecompose.livecapture.features.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 分享管理器
 * 支持系统分享、微信分享、微博分享、多图分享
 */
class ShareManager(private val context: Context) {

    companion object {
        private const val WEIXIN_PACKAGE = "com.tencent.mm"
        private const val WEIBO_PACKAGE = "com.sina.weibo"
    }

    /**
     * 通过系统分享图片
     *
     * @param uri 图片 URI
     */
    fun shareImage(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享图片")
        context.startActivity(chooser)
    }

    /**
     * 通过系统分享图片（文件路径）
     *
     * @param filePath 图片文件路径
     */
    fun shareImage(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = fileToUri(file)
        shareImage(uri)
    }

    /**
     * 分享到微信
     *
     * @param uri 图片 URI
     */
    fun shareToWeChat(uri: Uri) {
        if (!isWeChatInstalled()) {
            shareImage(uri) // 降级到系统分享
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            component = android.content.ComponentName(
                WEIXIN_PACKAGE,
                "$WEIXIN_PACKAGE.ui.tools.ShareImgUI"
            )
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 降级到系统分享
            shareImage(uri)
        }
    }

    /**
     * 分享到微信（文件路径）
     */
    fun shareToWeChat(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = fileToUri(file)
        shareToWeChat(uri)
    }

    /**
     * 分享到微博
     *
     * @param uri 图片 URI
     */
    fun shareToWeibo(uri: Uri) {
        if (!isWeiboInstalled()) {
            shareImage(uri) // 降级到系统分享
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            component = android.content.ComponentName(
                WEIBO_PACKAGE,
                "$WEIBO_PACKAGE.composerinde.ComposerDispatchActivity"
            )
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // 降级到系统分享
            shareImage(uri)
        }
    }

    /**
     * 分享到微博（文件路径）
     */
    fun shareToWeibo(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = fileToUri(file)
        shareToWeibo(uri)
    }

    /**
     * 多图分享
     *
     * @param uris 图片 URI 列表
     */
    fun shareMultipleImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            shareImage(uris.first())
            return
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享图片")
        context.startActivity(chooser)
    }

    /**
     * 多图分享（文件路径）
     */
    fun shareMultipleImages(filePaths: List<String>) {
        val uris = filePaths
            .map { File(it) }
            .filter { it.exists() }
            .map { fileToUri(it) }
        shareMultipleImages(uris)
    }

    /**
     * 分享文本
     */
    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "分享")
        context.startActivity(chooser)
    }

    /**
     * 分享图片和文本
     */
    fun shareImageWithText(uri: Uri, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享")
        context.startActivity(chooser)
    }

    // --- 私有辅助方法 ---

    /**
     * 将文件路径转换为 FileProvider URI
     */
    private fun fileToUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
    }

    /**
     * 检查微信是否安装
     */
    fun isWeChatInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(WEIXIN_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 检查微博是否安装
     */
    fun isWeiboInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(WEIBO_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}