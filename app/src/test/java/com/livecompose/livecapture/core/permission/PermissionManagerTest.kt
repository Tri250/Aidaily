package com.livecompose.livecapture.core.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

/**
 * PermissionManager 单元测试
 * 覆盖所有公开方法，包括权限检查、权限请求、Rationale 判断、设置跳转和自检逻辑。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionManagerTest {

    private lateinit var context: android.content.Context
    private lateinit var activity: Activity
    private lateinit var launcher: ActivityResultLauncher<String>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(Activity::class.java).get()
        launcher = mock()
    }

    @After
    fun tearDown() {
        // 重置所有权限状态，避免测试间互相干扰
        shadowOf(context).grantPermissions()
    }

    // ======================== hasCameraPermission ========================

    @Test
    fun `hasCameraPermission 授予相机权限时返回 true`() {
        shadowOf(context).grantPermissions(Manifest.permission.CAMERA)

        val result = PermissionManager.hasCameraPermission(context)

        assertTrue("相机权限已授予，应返回 true", result)
    }

    @Test
    fun `hasCameraPermission 未授予相机权限时返回 false`() {
        shadowOf(context).denyPermissions(Manifest.permission.CAMERA)

        val result = PermissionManager.hasCameraPermission(context)

        assertFalse("相机权限未授予，应返回 false", result)
    }

    // ======================== hasMediaPermission ========================

    @Test
    fun `hasMediaPermission Android 13+ 授予 READ_MEDIA_IMAGES 时返回 true`() {
        // SDK 34 (Android 14) 属于 TIRAMISU+ 范围
        shadowOf(context).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val result = PermissionManager.hasMediaPermission(context)

        assertTrue("Android 13+ 媒体权限已授予，应返回 true", result)
    }

    @Test
    fun `hasMediaPermission Android 13+ 未授予 READ_MEDIA_IMAGES 时返回 false`() {
        shadowOf(context).denyPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val result = PermissionManager.hasMediaPermission(context)

        assertFalse("Android 13+ 媒体权限未授予，应返回 false", result)
    }

    @Test
    @Config(sdk = [30]) // 低于 TIRAMISU (33) 的 SDK
    fun `hasMediaPermission 旧版 Android 授予 READ_EXTERNAL_STORAGE 时返回 true`() {
        shadowOf(context).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        val result = PermissionManager.hasMediaPermission(context)

        assertTrue("旧版 Android 媒体权限已授予，应返回 true", result)
    }

    @Test
    @Config(sdk = [30])
    fun `hasMediaPermission 旧版 Android 未授予 READ_EXTERNAL_STORAGE 时返回 false`() {
        shadowOf(context).denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        val result = PermissionManager.hasMediaPermission(context)

        assertFalse("旧版 Android 媒体权限未授予，应返回 false", result)
    }

    // ======================== shouldShowRationale ========================

    @Test
    fun `shouldShowRationale 当系统要求显示 Rationale 时返回 true`() {
        // 通过 Robolectric shadow 设置 shouldShowRequestPermissionRationale 返回 true
        shadowOf(activity).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, true)

        val result = PermissionManager.shouldShowRationale(activity, Manifest.permission.CAMERA)

        assertTrue("系统要求显示权限解释，应返回 true", result)
    }

    @Test
    fun `shouldShowRationale 当系统不要求显示 Rationale 时返回 false`() {
        shadowOf(activity).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, false)

        val result = PermissionManager.shouldShowRationale(activity, Manifest.permission.CAMERA)

        assertFalse("系统不要求显示权限解释，应返回 false", result)
    }

    // ======================== requestCameraPermission ========================

    @Test
    fun `requestCameraPermission 使用 CAMERA 权限启动 launcher`() {
        PermissionManager.requestCameraPermission(launcher)

        val captor = argumentCaptor<String>()
        verify(launcher).launch(captor.capture())
        assertEquals(
            "请求相机权限应传入 CAMERA 权限字符串",
            Manifest.permission.CAMERA,
            captor.firstValue
        )
    }

    // ======================== requestMediaPermission ========================

    @Test
    fun `requestMediaPermission Android 13+ 使用 READ_MEDIA_IMAGES 启动 launcher`() {
        // SDK 34 属于 TIRAMISU+，应使用 READ_MEDIA_IMAGES
        PermissionManager.requestMediaPermission(launcher)

        val captor = argumentCaptor<String>()
        verify(launcher).launch(captor.capture())
        assertEquals(
            "Android 13+ 请求媒体权限应传入 READ_MEDIA_IMAGES",
            Manifest.permission.READ_MEDIA_IMAGES,
            captor.firstValue
        )
    }

    @Test
    @Config(sdk = [30])
    fun `requestMediaPermission 旧版 Android 使用 READ_EXTERNAL_STORAGE 启动 launcher`() {
        PermissionManager.requestMediaPermission(launcher)

        val captor = argumentCaptor<String>()
        verify(launcher).launch(captor.capture())
        assertEquals(
            "旧版 Android 请求媒体权限应传入 READ_EXTERNAL_STORAGE",
            Manifest.permission.READ_EXTERNAL_STORAGE,
            captor.firstValue
        )
    }

    // ======================== openAppSettings ========================

    @Test
    fun `openAppSettings 启动应用详情设置页面`() {
        val shadowActivity = shadowOf(context as android.app.Application)
        // 清空之前可能遗留的 intent
        shadowActivity.clearNextStartedActivity()

        PermissionManager.openAppSettings(context)

        val intent = shadowActivity.nextStartedActivity
        assertTrue("应启动设置页面", intent != null)
        assertEquals(
            "Action 应为应用详情设置",
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            intent.action
        )
        val expectedUri = Uri.fromParts("package", context.packageName, null)
        assertEquals(
            "Intent data 应指向本应用包名",
            expectedUri,
            intent.data
        )
    }

    // ======================== checkRequiredPermissions ========================

    @Test
    fun `checkRequiredPermissions 所有权限都已授予时返回空列表`() {
        shadowOf(context).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        val missing = PermissionManager.checkRequiredPermissions(context)

        assertTrue("所有权限已授予，缺失列表应为空", missing.isEmpty())
    }

    @Test
    fun `checkRequiredPermissions 缺少相机权限时返回包含 CAMERA 的列表`() {
        shadowOf(context).denyPermissions(Manifest.permission.CAMERA)
        shadowOf(context).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val missing = PermissionManager.checkRequiredPermissions(context)

        assertEquals("应缺少 1 个权限", 1, missing.size)
        assertEquals(
            "缺少的权限应为 CAMERA",
            Manifest.permission.CAMERA,
            missing[0]
        )
    }

    @Test
    fun `checkRequiredPermissions Android 13+ 缺少媒体权限时返回包含 READ_MEDIA_IMAGES 的列表`() {
        shadowOf(context).grantPermissions(Manifest.permission.CAMERA)
        shadowOf(context).denyPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val missing = PermissionManager.checkRequiredPermissions(context)

        assertEquals("应缺少 1 个权限", 1, missing.size)
        assertEquals(
            "缺少的权限应为 READ_MEDIA_IMAGES",
            Manifest.permission.READ_MEDIA_IMAGES,
            missing[0]
        )
    }

    @Test
    @Config(sdk = [30])
    fun `checkRequiredPermissions 旧版 Android 缺少媒体权限时返回包含 READ_EXTERNAL_STORAGE 的列表`() {
        shadowOf(context).grantPermissions(Manifest.permission.CAMERA)
        shadowOf(context).denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        val missing = PermissionManager.checkRequiredPermissions(context)

        assertEquals("应缺少 1 个权限", 1, missing.size)
        assertEquals(
            "缺少的权限应为 READ_EXTERNAL_STORAGE",
            Manifest.permission.READ_EXTERNAL_STORAGE,
            missing[0]
        )
    }

    @Test
    fun `checkRequiredPermissions 两个权限都缺少时返回包含两个权限的列表`() {
        shadowOf(context).denyPermissions(Manifest.permission.CAMERA)
        shadowOf(context).denyPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val missing = PermissionManager.checkRequiredPermissions(context)

        assertEquals("应缺少 2 个权限", 2, missing.size)
        assertTrue(
            "缺失列表应包含 CAMERA",
            missing.contains(Manifest.permission.CAMERA)
        )
        assertTrue(
            "缺失列表应包含 READ_MEDIA_IMAGES",
            missing.contains(Manifest.permission.READ_MEDIA_IMAGES)
        )
    }
}
