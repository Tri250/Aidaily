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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionManagerTest {

    private lateinit var context: android.content.Context
    private lateinit var activity: Activity
    private lateinit var launcher: ActivityResultLauncher<String>
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(Activity::class.java).get()
        launcher = mock()
        permissionManager = PermissionManager(context)
    }

    @After
    fun tearDown() {
        shadowOf(context).grantPermissions()
    }

    @Test
    fun `hasCameraPermission 授予相机权限时返回 true`() {
        shadowOf(context).grantPermissions(Manifest.permission.CAMERA)

        val result = permissionManager.hasCameraPermission()

        assertTrue(result)
    }

    @Test
    fun `hasCameraPermission 未授予相机权限时返回 false`() {
        shadowOf(context).denyPermissions(Manifest.permission.CAMERA)

        val result = permissionManager.hasCameraPermission()

        assertFalse(result)
    }

    @Test
    fun `hasMediaPermission Android 13+ 授予 READ_MEDIA_IMAGES 时返回 true`() {
        shadowOf(context).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val result = permissionManager.hasMediaPermission()

        assertTrue(result)
    }

    @Test
    fun `hasMediaPermission Android 13+ 未授予 READ_MEDIA_IMAGES 时返回 false`() {
        shadowOf(context).denyPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val result = permissionManager.hasMediaPermission()

        assertFalse(result)
    }

    @Test
    @Config(sdk = [30])
    fun `hasMediaPermission 旧版 Android 授予 READ_EXTERNAL_STORAGE 时返回 true`() {
        shadowOf(context).grantPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        val result = permissionManager.hasMediaPermission()

        assertTrue(result)
    }

    @Test
    @Config(sdk = [30])
    fun `hasMediaPermission 旧版 Android 未授予 READ_EXTERNAL_STORAGE 时返回 false`() {
        shadowOf(context).denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        val result = permissionManager.hasMediaPermission()

        assertFalse(result)
    }

    @Test
    fun `shouldShowRationale 当系统要求显示 Rationale 时返回 true`() {
        shadowOf(activity).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, true)

        val result = permissionManager.shouldShowRationale(activity, Manifest.permission.CAMERA)

        assertTrue(result)
    }

    @Test
    fun `shouldShowRationale 当系统不要求显示 Rationale 时返回 false`() {
        shadowOf(activity).setShouldShowRequestPermissionRationale(Manifest.permission.CAMERA, false)

        val result = permissionManager.shouldShowRationale(activity, Manifest.permission.CAMERA)

        assertFalse(result)
    }

    @Test
    fun `requestCameraPermission 使用 CAMERA 权限启动 launcher`() {
        permissionManager.requestCameraPermission(launcher)

        val captor = argumentCaptor<String>()
        verify(launcher).launch(captor.capture())
        assertEquals(Manifest.permission.CAMERA, captor.firstValue)
    }

    @Test
    fun `requestMediaPermission Android 13+ 使用 READ_MEDIA_IMAGES 启动 launcher`() {
        permissionManager.requestMediaPermission(launcher)

        val captor = argumentCaptor<String>()
        verify(launcher).launch(captor.capture())
        assertEquals(Manifest.permission.READ_MEDIA_IMAGES, captor.firstValue)
    }

    @Test
    @Config(sdk = [30])
    fun `requestMediaPermission 旧版 Android 使用 READ_EXTERNAL_STORAGE 启动 launcher`() {
        permissionManager.requestMediaPermission(launcher)

        val captor = argumentCaptor<String>()
        verify(launcher).launch(captor.capture())
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, captor.firstValue)
    }

    @Test
    fun `openAppSettings 启动应用详情设置页面`() {
        val shadowActivity = shadowOf(context as android.app.Application)
        shadowActivity.clearNextStartedActivity()

        permissionManager.openAppSettings()

        val intent = shadowActivity.nextStartedActivity
        assertTrue(intent != null)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        val expectedUri = Uri.fromParts("package", context.packageName, null)
        assertEquals(expectedUri, intent.data)
    }

    @Test
    fun `checkRequiredPermissions 所有权限都已授予时返回空列表`() {
        shadowOf(context).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        val missing = permissionManager.checkRequiredPermissions()

        assertTrue(missing.isEmpty())
    }

    @Test
    fun `checkRequiredPermissions 缺少相机权限时返回包含 CAMERA 的列表`() {
        shadowOf(context).denyPermissions(Manifest.permission.CAMERA)
        shadowOf(context).grantPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val missing = permissionManager.checkRequiredPermissions()

        assertEquals(1, missing.size)
        assertEquals(Manifest.permission.CAMERA, missing[0])
    }

    @Test
    fun `checkRequiredPermissions Android 13+ 缺少媒体权限时返回包含 READ_MEDIA_IMAGES 的列表`() {
        shadowOf(context).grantPermissions(Manifest.permission.CAMERA)
        shadowOf(context).denyPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val missing = permissionManager.checkRequiredPermissions()

        assertEquals(1, missing.size)
        assertEquals(Manifest.permission.READ_MEDIA_IMAGES, missing[0])
    }

    @Test
    @Config(sdk = [30])
    fun `checkRequiredPermissions 旧版 Android 缺少媒体权限时返回包含 READ_EXTERNAL_STORAGE 的列表`() {
        shadowOf(context).grantPermissions(Manifest.permission.CAMERA)
        shadowOf(context).denyPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)

        val missing = permissionManager.checkRequiredPermissions()

        assertEquals(1, missing.size)
        assertEquals(Manifest.permission.READ_EXTERNAL_STORAGE, missing[0])
    }

    @Test
    fun `checkRequiredPermissions 两个权限都缺少时返回包含两个权限的列表`() {
        shadowOf(context).denyPermissions(Manifest.permission.CAMERA)
        shadowOf(context).denyPermissions(Manifest.permission.READ_MEDIA_IMAGES)

        val missing = permissionManager.checkRequiredPermissions()

        assertEquals(2, missing.size)
        assertTrue(missing.contains(Manifest.permission.CAMERA))
        assertTrue(missing.contains(Manifest.permission.READ_MEDIA_IMAGES))
    }
}
