package com.livecompose.livecapture.core.diagnostics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.hardware.Sensor
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.livecompose.livecapture.core.permission.PermissionManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCameraManager
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowSensor

/**
 * SelfChecker 综合单元测试
 *
 * 覆盖范围：
 * - runFullCheck()：全量自检返回非空列表，包含7大类别
 * - checkEngine()：Camera2支持、后置摄像头、硬件级别、NNAPI可用性
 * - checkPerformance()：CPU核心数、堆内存、Android版本
 * - checkStability()：模拟器检测、崩溃处理器注册
 * - checkCompatibility()：OpenGL ES版本、CPU架构(ABI)
 * - checkPermissions()：相机权限、媒体权限、缺失权限
 * - checkSecurity()：网络安全配置、调试模式、代码混淆
 * - checkSensors()：陀螺仪、加速度计、环境光传感器
 * - CheckItem数据类：status枚举值、category/name/detail字段
 * - checkResults StateFlow 在 runFullCheck() 后更新
 * - logResults 输出到 logcat
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SelfCheckerTest {

    private lateinit var context: Context
    private lateinit var shadowCameraManager: ShadowCameraManager
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        shadowCameraManager = Shadows.shadowOf(cameraManager)
        // 清空之前可能遗留的相机配置
        shadowCameraManager.clearCameras()
        // PermissionManager 直接构造，通过 Context 检查权限
        permissionManager = PermissionManager(context)
    }

    @After
    fun tearDown() {
        // 重置权限状态，避免测试间互相干扰
        Shadows.shadowOf(context).grantPermissions()
        // 清空 logcat 日志
        ShadowLog.clear()
    }

    // =====================================================
    // 辅助方法
    // =====================================================

    /**
     * 创建 SelfChecker 实例。
     * SelfChecker 是 @Singleton，但我们可以直接构造用于测试。
     */
    private fun createSelfChecker(): SelfChecker {
        return SelfChecker(context, permissionManager)
    }

    /**
     * 添加一个后置摄像头到 ShadowCameraManager。
     * 模拟正常设备上至少有一个后置摄像头的情况。
     */
    private fun addBackCamera(
        cameraId: String = "0",
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
    ) {
        val characteristics = CameraCharacteristics(mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_BACK,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel
        ))
        shadowCameraManager.addCamera(cameraId, characteristics)
    }

    /**
     * 添加一个前置摄像头到 ShadowCameraManager。
     * 用于测试只有前置摄像头（无后置摄像头）的场景。
     */
    private fun addFrontCamera(
        cameraId: String = "1",
        hardwareLevel: Int = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEVEL_3
    ) {
        val characteristics = CameraCharacteristics(mapOf(
            CameraCharacteristics.LENS_FACING to CameraCharacteristics.LENS_FACING_FRONT,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL to hardwareLevel
        ))
        shadowCameraManager.addCamera(cameraId, characteristics)
    }

    /**
     * 添加一个 LEGACY 级别的后置摄像头。
     * 模拟低端设备的 Camera2 支持情况。
     */
    private fun addLegacyBackCamera(cameraId: String = "0") {
        addBackCamera(cameraId, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
    }

    /**
     * 授予相机和媒体权限，模拟所有权限均已授予的场景。
     */
    private fun grantAllPermissions() {
        Shadows.shadowOf(context).grantPermissions(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    }

    /**
     * 拒绝相机和媒体权限，模拟所有权限均未授予的场景。
     */
    private fun denyAllPermissions() {
        Shadows.shadowOf(context).denyPermissions(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    }

    // =====================================================
    // CheckItem 数据类测试
    // =====================================================

    @Test
    fun `CheckItem的status可以是PASS`() {
        val item = SelfChecker.CheckItem("测试", "测试项", SelfChecker.CheckStatus.PASS, "详情")
        assertEquals(SelfChecker.CheckStatus.PASS, item.status)
    }

    @Test
    fun `CheckItem的status可以是WARN`() {
        val item = SelfChecker.CheckItem("测试", "测试项", SelfChecker.CheckStatus.WARN, "详情")
        assertEquals(SelfChecker.CheckStatus.WARN, item.status)
    }

    @Test
    fun `CheckItem的status可以是FAIL`() {
        val item = SelfChecker.CheckItem("测试", "测试项", SelfChecker.CheckStatus.FAIL, "详情")
        assertEquals(SelfChecker.CheckStatus.FAIL, item.status)
    }

    @Test
    fun `CheckItem的status可以是INFO`() {
        val item = SelfChecker.CheckItem("测试", "测试项", SelfChecker.CheckStatus.INFO, "详情")
        assertEquals(SelfChecker.CheckStatus.INFO, item.status)
    }

    @Test
    fun `CheckItem包含正确的category字段`() {
        val item = SelfChecker.CheckItem("引擎", "测试项", SelfChecker.CheckStatus.PASS)
        assertEquals("引擎", item.category)
    }

    @Test
    fun `CheckItem包含正确的name字段`() {
        val item = SelfChecker.CheckItem("测试", "Camera2 支持", SelfChecker.CheckStatus.PASS)
        assertEquals("Camera2 支持", item.name)
    }

    @Test
    fun `CheckItem包含正确的detail字段`() {
        val item = SelfChecker.CheckItem("测试", "测试项", SelfChecker.CheckStatus.PASS, "已就绪")
        assertEquals("已就绪", item.detail)
    }

    @Test
    fun `CheckItem的detail默认为空字符串`() {
        val item = SelfChecker.CheckItem("测试", "测试项", SelfChecker.CheckStatus.PASS)
        assertEquals("", item.detail)
    }

    @Test
    fun `CheckItem的equals和hashCode基于内容`() {
        val item1 = SelfChecker.CheckItem("引擎", "后置摄像头", SelfChecker.CheckStatus.PASS, "已就绪")
        val item2 = SelfChecker.CheckItem("引擎", "后置摄像头", SelfChecker.CheckStatus.PASS, "已就绪")
        val item3 = SelfChecker.CheckItem("引擎", "后置摄像头", SelfChecker.CheckStatus.WARN, "已就绪")

        // 内容相同的对象相等
        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())

        // 内容不同的对象不相等
        assertNotEquals(item1, item3)
    }

    @Test
    fun `CheckItem的copy函数正确工作`() {
        val original = SelfChecker.CheckItem("引擎", "后置摄像头", SelfChecker.CheckStatus.PASS, "已就绪")
        val copied = original.copy(status = SelfChecker.CheckStatus.WARN, detail = "仅前置摄像头可用")

        // copy 后修改的字段
        assertEquals(SelfChecker.CheckStatus.WARN, copied.status)
        assertEquals("仅前置摄像头可用", copied.detail)

        // copy 后未修改的字段保持原值
        assertEquals("引擎", copied.category)
        assertEquals("后置摄像头", copied.name)

        // 原对象不受影响
        assertEquals(SelfChecker.CheckStatus.PASS, original.status)
        assertEquals("已就绪", original.detail)
    }

    @Test
    fun `CheckStatus枚举包含PASS-WARN-FAIL-INFO四个值`() {
        val values = SelfChecker.CheckStatus.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(SelfChecker.CheckStatus.PASS))
        assertTrue(values.contains(SelfChecker.CheckStatus.WARN))
        assertTrue(values.contains(SelfChecker.CheckStatus.FAIL))
        assertTrue(values.contains(SelfChecker.CheckStatus.INFO))
    }

    // =====================================================
    // runFullCheck() 综合测试
    // =====================================================

    @Test
    fun `runFullCheck返回非空列表`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()

        assertNotNull("runFullCheck 返回结果不应为 null", results)
        assertTrue("runFullCheck 应返回非空列表", results.isNotEmpty())
    }

    @Test
    fun `runFullCheck结果包含7大类别`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val categories = results.map { it.category }.distinct()

        // 验证7大类别全部出现
        assertTrue("应包含「引擎」类别", categories.contains("引擎"))
        assertTrue("应包含「性能」类别", categories.contains("性能"))
        assertTrue("应包含「稳定性」类别", categories.contains("稳定性"))
        assertTrue("应包含「兼容性」类别", categories.contains("兼容性"))
        assertTrue("应包含「权限」类别", categories.contains("权限"))
        assertTrue("应包含「安全」类别", categories.contains("安全"))
        assertTrue("应包含「传感器」类别", categories.contains("传感器"))
        assertEquals("应恰好包含7个类别", 7, categories.size)
    }

    @Test
    fun `runFullCheck返回的结果按类别顺序排列`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val categories = results.map { it.category }.distinct()

        // 验证7大类别按代码中定义的顺序出现
        val expectedOrder = listOf("引擎", "性能", "稳定性", "兼容性", "权限", "安全", "传感器")
        assertEquals("类别应按定义顺序排列", expectedOrder, categories)
    }

    // =====================================================
    // checkEngine() 测试 — Camera2 支持
    // =====================================================

    @Test
    fun `checkEngine-有后置摄像头时Camera2支持检测通过`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val engineItems = results.filter { it.category == "引擎" }

        // 有后置摄像头时，不应出现"未检测到摄像头"的FAIL项
        val camera2Fail = engineItems.find { it.name == "Camera2 支持" && it.status == SelfChecker.CheckStatus.FAIL }
        assertNull("有摄像头时不应出现 Camera2 支持 FAIL", camera2Fail)
    }

    @Test
    fun `checkEngine-无摄像头时Camera2支持检测失败`() {
        // 不添加任何摄像头，模拟无摄像头设备
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val engineItems = results.filter { it.category == "引擎" }

        val camera2Item = engineItems.find { it.name == "Camera2 支持" }
        assertNotNull("应包含 Camera2 支持检查项", camera2Item)
        assertEquals("无摄像头时 Camera2 支持应为 FAIL", SelfChecker.CheckStatus.FAIL, camera2Item!!.status)
        assertEquals("FAIL 详情应为'未检测到摄像头'", "未检测到摄像头", camera2Item.detail)
    }

    // =====================================================
    // checkEngine() 测试 — 后置摄像头
    // =====================================================

    @Test
    fun `checkEngine-有后置摄像头时后置摄像头检查PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val backCameraItem = results.find { it.category == "引擎" && it.name == "后置摄像头" }

        assertNotNull("应包含后置摄像头检查项", backCameraItem)
        assertEquals("有后置摄像头时应为 PASS", SelfChecker.CheckStatus.PASS, backCameraItem!!.status)
        assertEquals("PASS 详情应为'已就绪'", "已就绪", backCameraItem.detail)
    }

    @Test
    fun `checkEngine-仅有前置摄像头时后置摄像头检查WARN`() {
        addFrontCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val backCameraItem = results.find { it.category == "引擎" && it.name == "后置摄像头" }

        assertNotNull("应包含后置摄像头检查项", backCameraItem)
        assertEquals("仅有前置摄像头时应为 WARN", SelfChecker.CheckStatus.WARN, backCameraItem!!.status)
        assertEquals("WARN 详情应为'仅前置摄像头可用'", "仅前置摄像头可用", backCameraItem.detail)
    }

    // =====================================================
    // checkEngine() 测试 — Camera2 硬件级别
    // =====================================================

    @Test
    fun `checkEngine-LEGACY硬件级别时Camera2硬件级别为WARN`() {
        addLegacyBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val levelItem = results.find { it.category == "引擎" && it.name == "Camera2 硬件级别" }

        assertNotNull("应包含 Camera2 硬件级别检查项", levelItem)
        assertEquals("LEGACY 级别应为 WARN", SelfChecker.CheckStatus.WARN, levelItem!!.status)
        assertEquals("LEGACY 详情应为'LEGACY'", "LEGACY", levelItem.detail)
    }

    @Test
    fun `checkEngine-FULL硬件级别时Camera2硬件级别为PASS`() {
        addBackCamera("0", CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val levelItem = results.find { it.category == "引擎" && it.name == "Camera2 硬件级别" }

        assertNotNull("应包含 Camera2 硬件级别检查项", levelItem)
        assertEquals("FULL 级别应为 PASS", SelfChecker.CheckStatus.PASS, levelItem!!.status)
        assertEquals("FULL 详情应为'FULL'", "FULL", levelItem.detail)
    }

    @Test
    fun `checkEngine-LEVEL_3硬件级别时Camera2硬件级别为PASS`() {
        addBackCamera("0", CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3)
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val levelItem = results.find { it.category == "引擎" && it.name == "Camera2 硬件级别" }

        assertNotNull("应包含 Camera2 硬件级别检查项", levelItem)
        assertEquals("LEVEL_3 级别应为 PASS", SelfChecker.CheckStatus.PASS, levelItem!!.status)
        assertEquals("LEVEL_3 详情应为'LEVEL_3'", "LEVEL_3", levelItem.detail)
    }

    @Test
    fun `checkEngine-LIMITED硬件级别时Camera2硬件级别为PASS`() {
        addBackCamera("0", CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED)
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val levelItem = results.find { it.category == "引擎" && it.name == "Camera2 硬件级别" }

        assertNotNull("应包含 Camera2 硬件级别检查项", levelItem)
        assertEquals("LIMITED 级别应为 PASS", SelfChecker.CheckStatus.PASS, levelItem!!.status)
        assertEquals("LIMITED 详情应为'LIMITED'", "LIMITED", levelItem.detail)
    }

    // =====================================================
    // checkEngine() 测试 — NNAPI 可用性
    // =====================================================

    @Test
    fun `checkEngine-Android 28以上NNAPI检查项存在且为PASS`() {
        // SDK 34 >= 28 (P)，NNAPI 至少可能可用
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val nnapiItem = results.find { it.category == "引擎" && it.name == "NNAPI 硬件加速" }

        assertNotNull("应包含 NNAPI 硬件加速检查项", nnapiItem)
        // SDK 34 >= P，根据源码逻辑，应判定为 PASS
        assertEquals("SDK 34 上 NNAPI 应为 PASS", SelfChecker.CheckStatus.PASS, nnapiItem!!.status)
    }

    @Test
    @Config(sdk = [26]) // Android 8.0，低于 P (28)
    fun `checkEngine-Android 26以下NNAPI检查为WARN`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val nnapiItem = results.find { it.category == "引擎" && it.name == "NNAPI 硬件加速" }

        assertNotNull("应包含 NNAPI 硬件加速检查项", nnapiItem)
        // SDK 26 < P，NNAPI 不可用
        assertEquals("SDK 26 上 NNAPI 应为 WARN", SelfChecker.CheckStatus.WARN, nnapiItem!!.status)
        assertTrue("WARN 详情应提示 CPU 回退", nnapiItem.detail.contains("CPU"))
    }

    // =====================================================
    // checkPerformance() 测试 — CPU 核心数
    // =====================================================

    @Test
    fun `checkPerformance-包含CPU核心数检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val cpuItem = results.find { it.category == "性能" && it.name == "CPU 核心数" }

        assertNotNull("应包含 CPU 核心数检查项", cpuItem)
        // 测试环境 CPU 核心数通常 >= 4，应为 PASS
        // 但不硬断言 PASS/WARN，仅验证字段不为空
        assertTrue("CPU 核心数详情不应为空", cpuItem!!.detail.isNotEmpty())
        assertTrue("CPU 核心数详情应包含'核'", cpuItem.detail.contains("核"))
    }

    @Test
    fun `checkPerformance-CPU核心数大于等于4时为PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val cpuItem = results.find { it.category == "性能" && it.name == "CPU 核心数" }

        assertNotNull("应包含 CPU 核心数检查项", cpuItem)
        val cores = Runtime.getRuntime().availableProcessors()
        if (cores >= 4) {
            assertEquals("CPU 核心数 >= 4 时应为 PASS", SelfChecker.CheckStatus.PASS, cpuItem!!.status)
        } else {
            assertEquals("CPU 核心数 < 4 时应为 WARN", SelfChecker.CheckStatus.WARN, cpuItem!!.status)
        }
    }

    // =====================================================
    // checkPerformance() 测试 — 堆内存
    // =====================================================

    @Test
    fun `checkPerformance-包含堆内存检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val heapItem = results.find { it.category == "性能" && it.name == "堆内存" }

        assertNotNull("应包含堆内存检查项", heapItem)
        assertTrue("堆内存详情应包含'MB'", heapItem!!.detail.contains("MB"))
        assertTrue("堆内存详情应包含'最大'", heapItem.detail.contains("最大"))
        assertTrue("堆内存详情应包含'空闲'", heapItem.detail.contains("空闲"))
    }

    @Test
    fun `checkPerformance-堆内存最大值大于等于128MB时为PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val heapItem = results.find { it.category == "性能" && it.name == "堆内存" }

        assertNotNull("应包含堆内存检查项", heapItem)
        val maxMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        if (maxMemoryMB >= 128) {
            assertEquals("堆内存 >= 128MB 时应为 PASS", SelfChecker.CheckStatus.PASS, heapItem!!.status)
        } else {
            assertEquals("堆内存 < 128MB 时应为 WARN", SelfChecker.CheckStatus.WARN, heapItem!!.status)
        }
    }

    // =====================================================
    // checkPerformance() 测试 — Android 版本
    // =====================================================

    @Test
    fun `checkPerformance-SDK 34时Android版本检查为PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val versionItem = results.find { it.category == "性能" && it.name == "Android 版本" }

        assertNotNull("应包含 Android 版本检查项", versionItem)
        // SDK 34 >= 26，应为 PASS
        assertEquals("SDK 34 >= 26 时应为 PASS", SelfChecker.CheckStatus.PASS, versionItem!!.status)
        assertTrue("版本详情应包含'API'", versionItem.detail.contains("API"))
    }

    @Test
    @Config(sdk = [25]) // Android 7.1.1，低于 26
    fun `checkPerformance-SDK 25时Android版本检查为FAIL`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val versionItem = results.find { it.category == "性能" && it.name == "Android 版本" }

        assertNotNull("应包含 Android 版本检查项", versionItem)
        // SDK 25 < 26，应为 FAIL
        assertEquals("SDK 25 < 26 时应为 FAIL", SelfChecker.CheckStatus.FAIL, versionItem!!.status)
    }

    // =====================================================
    // checkStability() 测试 — 模拟器检测
    // =====================================================

    @Test
    fun `checkStability-包含运行环境检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val envItem = results.find { it.category == "稳定性" && it.name == "运行环境" }

        assertNotNull("应包含运行环境检查项", envItem)
    }

    @Test
    fun `checkStability-Robolectric环境中检测为模拟器`() {
        // Robolectric 的 Build.FINGERPRINT 包含 "generic"，应被识别为模拟器
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val envItem = results.find { it.category == "稳定性" && it.name == "运行环境" }

        assertNotNull("应包含运行环境检查项", envItem)
        // Robolectric 环境下 FINGERPRINT 包含 "generic"，应判为模拟器
        if (Build.FINGERPRINT.contains("generic")) {
            assertEquals("模拟器环境下应为 INFO", SelfChecker.CheckStatus.INFO, envItem!!.status)
            assertTrue("模拟器详情应包含'模拟器'", envItem.detail.contains("模拟器"))
        }
    }

    // =====================================================
    // checkStability() 测试 — 崩溃处理器
    // =====================================================

    @Test
    fun `checkStability-包含崩溃处理器检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val crashItem = results.find { it.category == "稳定性" && it.name == "崩溃处理器" }

        assertNotNull("应包含崩溃处理器检查项", crashItem)
    }

    @Test
    fun `checkStability-存在默认异常处理器时崩溃处理器为PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val crashItem = results.find { it.category == "稳定性" && it.name == "崩溃处理器" }

        assertNotNull("应包含崩溃处理器检查项", crashItem)
        val hasHandler = Thread.getDefaultUncaughtExceptionHandler() != null
        if (hasHandler) {
            assertEquals("有默认异常处理器时应为 PASS", SelfChecker.CheckStatus.PASS, crashItem!!.status)
            assertEquals("PASS 详情应为'已注册'", "已注册", crashItem.detail)
        }
    }

    // =====================================================
    // checkCompatibility() 测试 — OpenGL ES 版本
    // =====================================================

    @Test
    fun `checkCompatibility-包含OpenGL ES检查项且为PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val glItem = results.find { it.category == "兼容性" && it.name == "OpenGL ES" }

        assertNotNull("应包含 OpenGL ES 检查项", glItem)
        // OpenGL ES 版本检测当前始终返回 PASS
        assertEquals("OpenGL ES 检查应为 PASS", SelfChecker.CheckStatus.PASS, glItem!!.status)
        assertTrue("OpenGL ES 详情不应为空", glItem.detail.isNotEmpty())
    }

    // =====================================================
    // checkCompatibility() 测试 — CPU 架构 (ABI)
    // =====================================================

    @Test
    fun `checkCompatibility-包含CPU架构检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val abiItem = results.find { it.category == "兼容性" && it.name == "CPU 架构" }

        assertNotNull("应包含 CPU 架构检查项", abiItem)
        assertTrue("CPU 架构详情不应为空", abiItem!!.detail.isNotEmpty())
    }

    @Test
    fun `checkCompatibility-支持arm64或x86_64架构时为PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val abiItem = results.find { it.category == "兼容性" && it.name == "CPU 架构" }

        assertNotNull("应包含 CPU 架构检查项", abiItem)
        val has64Bit = Build.SUPPORTED_ABIS?.any { it.contains("arm64") || it.contains("x86_64") } == true
        if (has64Bit) {
            assertEquals("支持 64 位架构时应为 PASS", SelfChecker.CheckStatus.PASS, abiItem!!.status)
        }
    }

    // =====================================================
    // checkPermissions() 测试 — 相机权限
    // =====================================================

    @Test
    fun `checkPermissions-授予相机权限时相机权限检查为PASS`() {
        addBackCamera()
        Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.CAMERA)
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val cameraPermItem = results.find { it.category == "权限" && it.name == "相机权限" }

        assertNotNull("应包含相机权限检查项", cameraPermItem)
        assertEquals("授予相机权限时应为 PASS", SelfChecker.CheckStatus.PASS, cameraPermItem!!.status)
        assertEquals("PASS 详情应为'已授予'", "已授予", cameraPermItem.detail)
    }

    @Test
    fun `checkPermissions-未授予相机权限时相机权限检查为FAIL`() {
        addBackCamera()
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.CAMERA)
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val cameraPermItem = results.find { it.category == "权限" && it.name == "相机权限" }

        assertNotNull("应包含相机权限检查项", cameraPermItem)
        assertEquals("未授予相机权限时应为 FAIL", SelfChecker.CheckStatus.FAIL, cameraPermItem!!.status)
        assertEquals("FAIL 详情应为'未授予'", "未授予", cameraPermItem.detail)
    }

    // =====================================================
    // checkPermissions() 测试 — 媒体权限
    // =====================================================

    @Test
    fun `checkPermissions-授予媒体权限时媒体权限检查为PASS`() {
        addBackCamera()
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.CAMERA)
        Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val mediaPermItem = results.find { it.category == "权限" && it.name == "媒体读取权限" }

        assertNotNull("应包含媒体读取权限检查项", mediaPermItem)
        assertEquals("授予媒体权限时应为 PASS", SelfChecker.CheckStatus.PASS, mediaPermItem!!.status)
        assertEquals("PASS 详情应为'已授予'", "已授予", mediaPermItem.detail)
    }

    @Test
    fun `checkPermissions-未授予媒体权限时媒体权限检查为INFO`() {
        addBackCamera()
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.CAMERA)
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val mediaPermItem = results.find { it.category == "权限" && it.name == "媒体读取权限" }

        assertNotNull("应包含媒体读取权限检查项", mediaPermItem)
        // 媒体权限未授予不是 FAIL，而是 INFO（不影响核心功能）
        assertEquals("未授予媒体权限时应为 INFO", SelfChecker.CheckStatus.INFO, mediaPermItem!!.status)
        assertTrue("INFO 详情应说明不影响核心功能", mediaPermItem.detail.contains("不影响核心功能"))
    }

    // =====================================================
    // checkPermissions() 测试 — 缺失权限
    // =====================================================

    @Test
    fun `checkPermissions-无缺失权限时不出现缺失权限项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val missingItem = results.find { it.category == "权限" && it.name == "缺失权限" }

        assertNull("所有权限已授予时不应出现缺失权限项", missingItem)
    }

    @Test
    fun `checkPermissions-有缺失权限时出现缺失权限项且为FAIL`() {
        addBackCamera()
        denyAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val missingItem = results.find { it.category == "权限" && it.name == "缺失权限" }

        assertNotNull("有缺失权限时应出现缺失权限项", missingItem)
        assertEquals("缺失权限应为 FAIL", SelfChecker.CheckStatus.FAIL, missingItem!!.status)
        assertTrue("缺失权限详情应列出缺失的权限", missingItem.detail.isNotEmpty())
    }

    @Test
    fun `checkPermissions-仅缺少相机权限时缺失权限项包含CAMERA`() {
        addBackCamera()
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.CAMERA)
        Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val missingItem = results.find { it.category == "权限" && it.name == "缺失权限" }

        assertNotNull("缺少相机权限时应出现缺失权限项", missingItem)
        assertTrue("缺失权限详情应包含 CAMERA", missingItem!!.detail.contains("CAMERA"))
    }

    @Test
    fun `checkPermissions-仅缺少媒体权限时缺失权限项包含READ_MEDIA_IMAGES`() {
        addBackCamera()
        Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.CAMERA)
        Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.READ_MEDIA_IMAGES)
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val missingItem = results.find { it.category == "权限" && it.name == "缺失权限" }

        assertNotNull("缺少媒体权限时应出现缺失权限项", missingItem)
        assertTrue("缺失权限详情应包含 READ_MEDIA_IMAGES", missingItem!!.detail.contains("READ_MEDIA_IMAGES"))
    }

    // =====================================================
    // checkSecurity() 测试 — 网络安全配置
    // =====================================================

    @Test
    fun `checkSecurity-包含网络安全配置检查项且为PASS`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val netItem = results.find { it.category == "安全" && it.name == "网络安全配置" }

        assertNotNull("应包含网络安全配置检查项", netItem)
        // 网络安全配置当前始终返回 PASS
        assertEquals("网络安全配置检查应为 PASS", SelfChecker.CheckStatus.PASS, netItem!!.status)
    }

    // =====================================================
    // checkSecurity() 测试 — 调试模式
    // =====================================================

    @Test
    fun `checkSecurity-包含调试模式检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val debugItem = results.find { it.category == "安全" && it.name == "调试模式" }

        assertNotNull("应包含调试模式检查项", debugItem)
    }

    @Test
    fun `checkSecurity-调试模式下调试模式检查为WARN`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val debugItem = results.find { it.category == "安全" && it.name == "调试模式" }

        assertNotNull("应包含调试模式检查项", debugItem)
        // Robolectric 默认 applicationInfo.flags 包含 FLAG_DEBUGGABLE
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable) {
            assertEquals("调试模式下应为 WARN", SelfChecker.CheckStatus.WARN, debugItem!!.status)
            assertTrue("WARN 详情应包含'开发版本'", debugItem.detail.contains("开发版本"))
        } else {
            assertEquals("非调试模式下应为 PASS", SelfChecker.CheckStatus.PASS, debugItem!!.status)
            assertTrue("PASS 详情应包含'Release'", debugItem!!.detail.contains("Release"))
        }
    }

    // =====================================================
    // checkSecurity() 测试 — 代码混淆
    // =====================================================

    @Test
    fun `checkSecurity-包含代码混淆检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val proguardItem = results.find { it.category == "安全" && it.name == "代码混淆" }

        assertNotNull("应包含代码混淆检查项", proguardItem)
        // Robolectric 默认是 Debug 构建，代码混淆检查应为 INFO
        // 同时验证详情包含 ProGuard
        assertTrue("代码混淆详情应包含'ProGuard'", proguardItem!!.detail.contains("ProGuard"))
    }

    // =====================================================
    // checkSensors() 测试 — 陀螺仪
    // =====================================================

    @Test
    fun `checkSensors-无陀螺仪时陀螺仪检查为WARN`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val gyroItem = results.find { it.category == "传感器" && it.name == "陀螺仪" }

        assertNotNull("应包含陀螺仪检查项", gyroItem)
        // Robolectric 默认没有传感器
        assertEquals("无陀螺仪时应为 WARN", SelfChecker.CheckStatus.WARN, gyroItem!!.status)
        assertTrue("WARN 详情应包含'降级模式'", gyroItem.detail.contains("降级模式"))
    }

    @Test
    fun `checkSensors-有陀螺仪时陀螺仪检查为PASS`() {
        addBackCamera()
        grantAllPermissions()

        // 注册陀螺仪传感器到 ShadowSensorManager
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val gyroSensor = ShadowSensor.newInstance(Sensor.TYPE_GYROSCOPE)
        Shadows.shadowOf(sensorManager).addSensor(Sensor.TYPE_GYROSCOPE, gyroSensor)

        val checker = createSelfChecker()
        val results = checker.runFullCheck()
        val gyroItem = results.find { it.category == "传感器" && it.name == "陀螺仪" }

        assertNotNull("应包含陀螺仪检查项", gyroItem)
        assertEquals("有陀螺仪时应为 PASS", SelfChecker.CheckStatus.PASS, gyroItem!!.status)
        assertTrue("PASS 详情应包含'已就绪'", gyroItem.detail.contains("已就绪"))
    }

    // =====================================================
    // checkSensors() 测试 — 加速度计
    // =====================================================

    @Test
    fun `checkSensors-无加速度计时加速度计检查为WARN`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val accelItem = results.find { it.category == "传感器" && it.name == "加速度计" }

        assertNotNull("应包含加速度计检查项", accelItem)
        // Robolectric 默认没有传感器
        assertEquals("无加速度计时应为 WARN", SelfChecker.CheckStatus.WARN, accelItem!!.status)
        assertTrue("WARN 详情应包含'降级模式'", accelItem.detail.contains("降级模式"))
    }

    @Test
    fun `checkSensors-有加速度计时加速度计检查为PASS`() {
        addBackCamera()
        grantAllPermissions()

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val accelSensor = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        Shadows.shadowOf(sensorManager).addSensor(Sensor.TYPE_ACCELEROMETER, accelSensor)

        val checker = createSelfChecker()
        val results = checker.runFullCheck()
        val accelItem = results.find { it.category == "传感器" && it.name == "加速度计" }

        assertNotNull("应包含加速度计检查项", accelItem)
        assertEquals("有加速度计时应为 PASS", SelfChecker.CheckStatus.PASS, accelItem!!.status)
        assertTrue("PASS 详情应包含'已就绪'", accelItem.detail.contains("已就绪"))
    }

    // =====================================================
    // checkSensors() 测试 — 环境光传感器
    // =====================================================

    @Test
    fun `checkSensors-无环境光传感器时检查为INFO`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val lightItem = results.find { it.category == "传感器" && it.name == "环境光传感器" }

        assertNotNull("应包含环境光传感器检查项", lightItem)
        // 无环境光传感器不影响核心功能，应为 INFO
        assertEquals("无环境光传感器时应为 INFO", SelfChecker.CheckStatus.INFO, lightItem!!.status)
        assertTrue("INFO 详情应包含'不影响核心功能'", lightItem.detail.contains("不影响核心功能"))
    }

    @Test
    fun `checkSensors-有环境光传感器时检查为PASS`() {
        addBackCamera()
        grantAllPermissions()

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        Shadows.shadowOf(sensorManager).addSensor(Sensor.TYPE_LIGHT, lightSensor)

        val checker = createSelfChecker()
        val results = checker.runFullCheck()
        val lightItem = results.find { it.category == "传感器" && it.name == "环境光传感器" }

        assertNotNull("应包含环境光传感器检查项", lightItem)
        assertEquals("有环境光传感器时应为 PASS", SelfChecker.CheckStatus.PASS, lightItem!!.status)
        assertTrue("PASS 详情应包含'已就绪'", lightItem.detail.contains("已就绪"))
    }

    // =====================================================
    // checkResults StateFlow 测试
    // =====================================================

    @Test
    fun `checkResults初始值为空列表`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        // 未执行 runFullCheck 前，checkResults 应为空列表
        val initialValue = checker.checkResults.value
        assertTrue("checkResults 初始值应为空列表", initialValue.isEmpty())
    }

    @Test
    fun `checkResults在runFullCheck后更新为非空列表`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        // 执行前为空
        assertTrue("执行前 checkResults 应为空", checker.checkResults.value.isEmpty())

        // 执行 runFullCheck
        val results = checker.runFullCheck()

        // 执行后 checkResults 应与 runFullCheck 返回值一致
        val flowValue = checker.checkResults.value
        assertEquals("checkResults 应与 runFullCheck 返回值一致", results, flowValue)
        assertTrue("执行后 checkResults 应为非空", flowValue.isNotEmpty())
    }

    @Test
    fun `checkResults在runFullCheck后包含所有7大类别`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        checker.runFullCheck()

        val flowCategories = checker.checkResults.value.map { it.category }.distinct()
        assertEquals("checkResults 应包含7大类别", 7, flowCategories.size)
    }

    @Test
    fun `多次runFullCheck调用后checkResults更新为最新结果`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        // 第一次调用
        val firstResults = checker.runFullCheck()
        assertEquals(firstResults, checker.checkResults.value)

        // 第二次调用，结果应更新
        val secondResults = checker.runFullCheck()
        assertEquals(secondResults, checker.checkResults.value)
        // 两次结果内容相同但都是新的列表实例
        assertEquals("两次结果应包含相同数量的检查项", firstResults.size, secondResults.size)
    }

    // =====================================================
    // logResults 输出到 logcat 测试
    // =====================================================

    @Test
    fun `logResults输出自检报告到logcat`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        checker.runFullCheck()

        // 通过 ShadowLog 捕获日志输出
        val logs = ShadowLog.getLogsForTag("SelfChecker")
        assertNotNull("应输出到 SelfChecker tag", logs)

        // 查找自检报告的标题行
        val headerLog = logs.find { it.msg.contains("自检报告") }
        assertNotNull("应输出自检报告标题", headerLog)

        // 查找自检完成行
        val footerLog = logs.find { it.msg.contains("自检完成") }
        assertNotNull("应输出自检完成标记", footerLog)
    }

    @Test
    fun `logResults输出统计摘要包含通过和失败计数`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        checker.runFullCheck()

        val logs = ShadowLog.getLogsForTag("SelfChecker")
        val summaryLog = logs.find { it.msg.contains("通过") && it.msg.contains("警告") }
        assertNotNull("应输出包含通过/警告/失败/信息的统计摘要", summaryLog)
    }

    @Test
    fun `logResults输出每个检查项带图标和类别`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        checker.runFullCheck()

        val logs = ShadowLog.getLogsForTag("SelfChecker")
        // 验证有带 [✓] 或 [⚠] 或 [✗] 或 [ℹ] 图标的日志行
        val itemLogs = logs.filter { it.msg.contains("[") && it.msg.contains("]") }
        assertTrue("应输出每个检查项的详情日志", itemLogs.isNotEmpty())
    }

    // =====================================================
    // 综合场景测试
    // =====================================================

    @Test
    fun `完整场景-正常设备全量自检所有7大类别都有检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()

        // 每个类别至少有1个检查项
        val grouped = results.groupBy { it.category }
        for (category in listOf("引擎", "性能", "稳定性", "兼容性", "权限", "安全", "传感器")) {
            assertTrue("类别'$category'应至少有1个检查项", (grouped[category]?.size ?: 0) >= 1)
        }
    }

    @Test
    fun `完整场景-全权限授予时权限类别不应有FAIL状态`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val permissionFails = results.filter {
            it.category == "权限" && it.status == SelfChecker.CheckStatus.FAIL
        }

        assertTrue("全权限授予时权限类别不应有 FAIL 项", permissionFails.isEmpty())
    }

    @Test
    fun `完整场景-每个检查项都有有效的status值`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()

        for (item in results) {
            assertTrue(
                "检查项 [${item.category}] ${item.name} 的 status 应为有效枚举值",
                item.status in listOf(
                    SelfChecker.CheckStatus.PASS,
                    SelfChecker.CheckStatus.WARN,
                    SelfChecker.CheckStatus.FAIL,
                    SelfChecker.CheckStatus.INFO
                )
            )
        }
    }

    @Test
    fun `完整场景-每个检查项的category和name不为空`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()

        for (item in results) {
            assertTrue("category 不应为空", item.category.isNotEmpty())
            assertTrue("name 不应为空", item.name.isNotEmpty())
        }
    }

    @Test
    fun `完整场景-运行结果与StateFlow一致`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val flowResults = checker.checkResults.value

        // 逐项比对，确保返回值和 StateFlow 完全一致
        assertEquals("返回值和 StateFlow 的项目数应一致", results.size, flowResults.size)
        for (i in results.indices) {
            assertEquals("第 $i 项 category 应一致", results[i].category, flowResults[i].category)
            assertEquals("第 $i 项 name 应一致", results[i].name, flowResults[i].name)
            assertEquals("第 $i 项 status 应一致", results[i].status, flowResults[i].status)
            assertEquals("第 $i 项 detail 应一致", results[i].detail, flowResults[i].detail)
        }
    }

    // =====================================================
    // 2026 正式版新增检查项测试
    // =====================================================

    @Test
    fun `checkEngine-包含TFLite模型文件检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val studentModel = results.find { it.category == "引擎" && it.name.contains("adacrop_student") }
        val teacherModel = results.find { it.category == "引擎" && it.name.contains("adacrop_teacher") }

        assertNotNull("应包含 Student 模型文件检查项", studentModel)
        assertNotNull("应包含 Teacher 模型文件检查项", teacherModel)
    }

    @Test
    fun `checkEngine-TFLite模型文件缺失时检查为FAIL`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val modelItems = results.filter { it.category == "引擎" && it.name.startsWith("模型文件") }

        // Robolectric 测试环境没有 assets，模型文件检查应为 FAIL
        for (item in modelItems) {
            assertEquals("模型文件缺失时应为 FAIL", SelfChecker.CheckStatus.FAIL, item.status)
        }
        assertTrue("应至少有2个模型文件检查项", modelItems.size >= 2)
    }

    @Test
    fun `checkPerformance-包含存储空间检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val storageItem = results.find { it.category == "性能" && it.name == "存储空间" }

        assertNotNull("应包含存储空间检查项", storageItem)
        assertTrue("存储空间详情应包含'MB'或'GB'",
            storageItem!!.detail.contains("MB") || storageItem.detail.contains("GB"))
    }

    @Test
    fun `checkPerformance-包含GPU渲染检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val gpuItem = results.find { it.category == "性能" && it.name == "GPU 渲染" }

        assertNotNull("应包含 GPU 渲染检查项", gpuItem)
        assertTrue("GPU 详情应包含'OpenGL ES'", gpuItem!!.detail.contains("OpenGL ES"))
    }

    @Test
    fun `checkStability-包含电池优化检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val batteryItem = results.find { it.category == "稳定性" && it.name == "电池优化" }

        assertNotNull("应包含电池优化检查项", batteryItem)
    }

    @Test
    fun `checkCompatibility-包含最低API要求检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val apiItem = results.find { it.category == "兼容性" && it.name == "最低 API 要求" }

        assertNotNull("应包含最低 API 要求检查项", apiItem)
        // SDK 34 >= 26，应为 PASS
        assertEquals("SDK 34 应满足 API 26+ 要求", SelfChecker.CheckStatus.PASS, apiItem!!.status)
    }

    @Test
    fun `checkPermissions-全权限授予时包含权限状态PASS项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val statusItem = results.find { it.category == "权限" && it.name == "权限状态" }

        assertNotNull("全权限授予时应包含权限状态检查项", statusItem)
        assertEquals("全权限授予时应为 PASS", SelfChecker.CheckStatus.PASS, statusItem!!.status)
    }

    @Test
    fun `checkSecurity-包含明文流量检查项`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val cleartextItem = results.find { it.category == "安全" && it.name == "明文流量" }

        assertNotNull("应包含明文流量检查项", cleartextItem)
    }

    @Test
    @Config(sdk = [25])
    fun `checkCompatibility-SDK 25时最低API要求检查为FAIL`() {
        addBackCamera()
        grantAllPermissions()
        val checker = createSelfChecker()

        val results = checker.runFullCheck()
        val apiItem = results.find { it.category == "兼容性" && it.name == "最低 API 要求" }

        assertNotNull("应包含最低 API 要求检查项", apiItem)
        // SDK 25 < 26，应为 FAIL
        assertEquals("SDK 25 < 26 时应为 FAIL", SelfChecker.CheckStatus.FAIL, apiItem!!.status)
    }
}
