package com.livecompose.livecapture.core.phantom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.livecompose.livecapture.core.lut.BuiltInPresets
import com.livecompose.livecapture.core.lut.LutProcessor

/**
 * 幻影模式服务
 * 监听系统相册变化，拦截系统相机输出的照片并应用 LUT 色彩处理
 * 参考 PhotonCamera PhantomService 实现
 */
class PhantomService : Service() {

    companion object {
        private const val TAG = "PhantomService"
        private const val CHANNEL_ID = "phantom_mode_channel"
        private const val NOTIFICATION_ID = 1001
        private const val MIN_IMPORT_SIZE = 1_000_000L // 1MB
        private const val MIN_PHANTOM_SHORT_SIDE = 1080
        private const val PHOTO_FRESHNESS_MS = 300_000L // 5分钟

        private const val PREFS_NAME = "phantom_prefs"
        private const val KEY_ENABLED = "phantom_enabled"
        private const val KEY_LUT_ID = "phantom_lut_id"
        private const val KEY_BASELINE_LUT_ID = "phantom_baseline_lut_id"
        private const val KEY_SAVE_AS_NEW = "phantom_save_as_new"
        private const val KEY_INTENSITY = "phantom_intensity"

        fun isEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply()
        }

        fun getSelectedLutId(context: Context): String {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LUT_ID, "") ?: ""
        }

        fun setSelectedLutId(context: Context, lutId: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LUT_ID, lutId).apply()
        }

        fun getIntensity(context: Context): Float {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getFloat(KEY_INTENSITY, 0.8f)
        }

        fun setIntensity(context: Context, intensity: Float) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putFloat(KEY_INTENSITY, intensity).apply()
        }

        fun isSaveAsNew(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SAVE_AS_NEW, true)
        }

        fun setSaveAsNew(context: Context, saveAsNew: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SAVE_AS_NEW, saveAsNew).apply()
        }
    }

    private var contentObserver: ContentObserver? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, createNotification())
            registerContentObserver()
            Log.d(TAG, "幻影模式已启动")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterContentObserver()
        isRunning = false
        Log.d(TAG, "幻影模式已停止")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 注册 ContentObserver 监听系统相册
     */
    private fun registerContentObserver() {
        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let { processNewPhoto(it) }
            }
        }

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
    }

    private fun unregisterContentObserver() {
        contentObserver?.let { contentResolver.unregisterContentObserver(it) }
        contentObserver = null
    }

    /**
     * 处理新照片
     */
    private fun processNewPhoto(uri: Uri) {
        try {
            val resolver = contentResolver

            // 查询照片信息
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.IS_PENDING,
                MediaStore.Images.Media.IS_TRASHED
            )

            resolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return

                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val relativePathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val isPendingIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_PENDING)
                val isTrashedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_TRASHED)

                val name = cursor.getString(nameIndex) ?: return
                val size = cursor.getLong(sizeIndex)
                val dateTaken = cursor.getLong(dateTakenIndex)
                val width = cursor.getInt(widthIndex)
                val height = cursor.getInt(heightIndex)
                val relativePath = cursor.getString(relativePathIndex) ?: ""
                val isPending = cursor.getInt(isPendingIndex)
                val isTrashed = cursor.getInt(isTrashedIndex)

                // 过滤条件
                if (isPending != 0) return // 文件还在写入
                if (isTrashed != 0) return // 已删除
                if (size <= MIN_IMPORT_SIZE) return // 太小
                if (System.currentTimeMillis() - dateTaken > PHOTO_FRESHNESS_MS) return // 不是刚拍的
                if (!relativePath.contains("DCIM/Camera") && !relativePath.contains("DCIM/100IMAGE")) return // 不是系统相机目录
                if (name.startsWith("PhotonCamera") || name.startsWith("LiveCapture")) return // 避免处理自己导出的

                val shortSide = minOf(width, height)
                if (shortSide <= MIN_PHANTOM_SHORT_SIDE) return // 分辨率太低

                // 通过过滤，处理照片
                applyPhantomProcessing(uri, name, width, height)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理新照片失败", e)
        }
    }

    /**
     * 应用幻影模式色彩处理
     */
    private fun applyPhantomProcessing(uri: Uri, name: String, width: Int, height: Int) {
        try {
            // 读取原始照片
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.w(TAG, "无法解码照片: $name")
                return
            }

            // 获取选中的 LUT 预设
            val lutId = getSelectedLutId(this)
            val intensity = getIntensity(this)

            if (lutId.isEmpty()) {
                originalBitmap.recycle()
                return
            }

            val preset = BuiltInPresets.presets.find { it.id == lutId }
            if (preset == null) {
                originalBitmap.recycle()
                return
            }

            val lutProcessor = LutProcessor()
            GlobalScope.launch {
                try {
                    val processedBitmap = lutProcessor.applyPreset(originalBitmap, preset) {}

                    // 保存处理后的照片
                    val saveAsNew = isSaveAsNew(this@PhantomService)
                    if (saveAsNew) {
                        saveAsNewPhoto(processedBitmap, name, lutId)
                    } else {
                        overwriteOriginal(processedBitmap, uri)
                    }

                    originalBitmap.recycle()
                    processedBitmap.recycle()

                    Log.d(TAG, "幻影模式处理完成: $name")
                } catch (e: Exception) {
                    Log.e(TAG, "色彩处理失败: $name", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理新照片失败: $name", e)
        }
    }

    /**
     * 另存为新照片
     */
    private fun saveAsNewPhoto(bitmap: Bitmap, originalName: String, lutId: String) {
        val displayName = "LiveCapture_${originalName.removeSuffix(".jpg")}_${lutId}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uri, contentValues, null, null)
        }
    }

    /**
     * 覆盖原始照片
     */
    private fun overwriteOriginal(bitmap: Bitmap, uri: Uri) {
        contentResolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "幻影模式",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "幻影模式正在运行"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("幻影模式")
            .setContentText("正在监听系统相机输出...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }
}
