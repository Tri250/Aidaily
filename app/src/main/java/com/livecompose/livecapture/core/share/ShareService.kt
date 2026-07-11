package com.livecompose.livecapture.core.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.livecompose.livecapture.R
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareService @Inject constructor(
    @androidx.hilt.android.qualifiers.ApplicationContext private val appContext: Context
) {

    companion object {
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        private const val AUTHORITY_SUFFIX = ".fileprovider"
    }

    /**
     * 分享单张照片到任意应用
     */
    fun sharePhoto(photoPath: String, context: Context) {
        val uri = resolveUri(photoPath, context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_photo_title)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * 分享单张照片到微信，若未安装则回退到通用分享
     */
    fun sharePhotoToWechat(photoPath: String, context: Context) {
        val uri = resolveUri(photoPath, context)
        val wechatIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage(WECHAT_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (wechatIntent.resolveActivity(context.packageManager) != null) {
            wechatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(wechatIntent)
        } else {
            Toast.makeText(context, context.getString(R.string.share_wechat_not_installed), Toast.LENGTH_SHORT).show()
            sharePhoto(photoPath, context)
        }
    }

    /**
     * 分享多张照片
     */
    fun shareMultiplePhotos(photoPaths: List<String>, context: Context) {
        if (photoPaths.isEmpty()) return
        if (photoPaths.size == 1) {
            sharePhoto(photoPaths.first(), context)
            return
        }

        val uris = photoPaths.map { resolveUri(it, context) }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_photo_title)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * 检测微信是否已安装
     */
    fun isWechatInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WECHAT_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 将文件路径解析为 content:// URI
     * - content:// 路径直接使用并授予读权限
     * - 文件路径使用 FileProvider 转换
     */
    private fun resolveUri(photoPath: String, context: Context): Uri {
        return if (photoPath.startsWith("content://")) {
            val uri = Uri.parse(photoPath)
            // 对 content:// URI 授予临时读权限
            context.grantUriPermission(
                context.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            uri
        } else {
            val file = File(photoPath)
            val authority = "${context.packageName}$AUTHORITY_SUFFIX"
            FileProvider.getUriForFile(context, authority, file)
        }
    }
}
