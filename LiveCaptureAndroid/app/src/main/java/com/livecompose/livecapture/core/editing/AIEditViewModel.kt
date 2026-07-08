package com.livecompose.livecapture.core.editing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import com.livecompose.livecapture.core.logger.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * AI 编辑视图模型
 *
 * 对应 iOS 端 AIEditViewModel.swift，管理生成式 AI 编辑工具的状态和交互，
 * 协调 [ObjectRemover]、[SkyReplacer]、[ImageExpander]、[StyleTransfer] 的执行，
 * 提供编辑前后的图像对比和进度跟踪。
 *
 * ## 编辑工具
 * - REMOVE: 物体移除 - 涂抹区域后自动填充
 * - SKY_REPLACE: 天空替换 - 选择天空类型替换天空
 * - EXPAND: 图像扩展 - 内容感知扩展画布
 * - STYLE_TRANSFER: 风格迁移 - 应用艺术风格滤镜
 *
 * ## 使用方式
 * 1. 设置 [sourceImage]
 * 2. 选择 [selectedTool]
 * 3. 配置工具参数
 * 4. 调用 [applyEdit] 执行编辑
 * 5. 观察 [editedImage] 获取结果
 *
 * @param context 上下文
 */
class AIEditViewModel(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "AIEditViewModel"
    }

    private val appContext = context.applicationContext

    // MARK: - 编辑工具枚举

    /**
     * AI 编辑工具类型
     */
    enum class AIEditTool {
        REMOVE,
        SKY_REPLACE,
        EXPAND,
        STYLE_TRANSFER;

        /** 显示名称 */
        val displayName: String
            get() = when (this) {
                REMOVE -> "物体移除"
                SKY_REPLACE -> "天空替换"
                EXPAND -> "图像扩展"
                STYLE_TRANSFER -> "风格迁移"
            }
    }

    // MARK: - 私有工具实例

    private val objectRemover = ObjectRemover()
    private val skyReplacer = SkyReplacer()
    private val imageExpander = ImageExpander()
    private val styleTransfer = StyleTransfer()

    // MARK: - 图像状态

    private val _sourceImage = MutableStateFlow<Bitmap?>(null)
    /** 源图像（编辑前） */
    val sourceImage: StateFlow<Bitmap?> = _sourceImage.asStateFlow()

    private val _editedImage = MutableStateFlow<Bitmap?>(null)
    /** 编辑后图像 */
    val editedImage: StateFlow<Bitmap?> = _editedImage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    /** 是否正在处理 */
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _selectedTool = MutableStateFlow(AIEditTool.REMOVE)
    /** 当前选中的编辑工具 */
    val selectedTool: StateFlow<AIEditTool> = _selectedTool.asStateFlow()

    // MARK: - 物体移除参数

    private val _maskRect = MutableStateFlow<RectF?>(null)
    /** 移除区域掩码矩形（归一化坐标 0..1） */
    val maskRect: StateFlow<RectF?> = _maskRect.asStateFlow()

    // MARK: - 天空替换参数

    private val _selectedSkyType = MutableStateFlow(SkyReplacer.SkyType.SUNSET)
    /** 选择的天空类型 */
    val selectedSkyType: StateFlow<SkyReplacer.SkyType> = _selectedSkyType.asStateFlow()

    // MARK: - 风格迁移参数

    private val _selectedStyle = MutableStateFlow(StyleTransfer.ArtStyle.WATERCOLOR)
    /** 选择的艺术风格 */
    val selectedStyle: StateFlow<StyleTransfer.ArtStyle> = _selectedStyle.asStateFlow()

    private val _styleIntensity = MutableStateFlow(0.7f)
    /** 风格强度（0.0 = 原图，1.0 = 完全风格化） */
    val styleIntensity: StateFlow<Float> = _styleIntensity.asStateFlow()

    // MARK: - 图像扩展参数

    private val _expandAmount = MutableStateFlow(100)
    /** 每边扩展像素数 */
    val expandAmount: StateFlow<Int> = _expandAmount.asStateFlow()

    private val _expandDirection = MutableStateFlow(ImageExpander.ExpansionDirection.ALL)
    /** 扩展方向 */
    val expandDirection: StateFlow<ImageExpander.ExpansionDirection> = _expandDirection.asStateFlow()

    // MARK: - 进度与错误

    private val _progress = MutableStateFlow(0f)
    /** 当前进度（0.0 - 1.0） */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** 错误信息 */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // MARK: - 设置方法

    /**
     * 设置源图像
     *
     * @param bitmap 源图像位图，传入 null 清空
     */
    fun setSourceImage(bitmap: Bitmap?) {
        _sourceImage.value = bitmap
    }

    /**
     * 选择编辑工具
     *
     * @param tool 编辑工具
     */
    fun selectTool(tool: AIEditTool) {
        _selectedTool.value = tool
    }

    /**
     * 设置移除区域掩码
     *
     * @param rect 归一化矩形（0..1），传入 null 清空
     */
    fun setMaskRect(rect: RectF?) {
        _maskRect.value = rect
    }

    /**
     * 设置天空类型
     *
     * @param type 天空类型
     */
    fun setSkyType(type: SkyReplacer.SkyType) {
        _selectedSkyType.value = type
    }

    /**
     * 设置艺术风格
     *
     * @param style 艺术风格
     */
    fun setStyle(style: StyleTransfer.ArtStyle) {
        _selectedStyle.value = style
    }

    /**
     * 设置风格强度
     *
     * @param intensity 风格强度（会被限制在 0..1）
     */
    fun setStyleIntensity(intensity: Float) {
        _styleIntensity.value = intensity.coerceIn(0f, 1f)
    }

    /**
     * 设置扩展像素数
     *
     * @param pixels 每边扩展像素数
     */
    fun setExpandAmount(pixels: Int) {
        _expandAmount.value = pixels
    }

    /**
     * 设置扩展方向
     *
     * @param direction 扩展方向
     */
    fun setExpandDirection(direction: ImageExpander.ExpansionDirection) {
        _expandDirection.value = direction
    }

    // MARK: - 编辑执行

    /**
     * 应用当前选中的编辑
     *
     * 流程：
     * 1. 校验源图像存在，否则设置错误信息
     * 2. 进入处理状态，重置进度与错误
     * 3. 根据 [selectedTool] 调用对应工具
     * 4. 成功则更新 [editedImage] 并置满进度；失败则设置错误信息
     */
    suspend fun applyEdit() {
        val image = _sourceImage.value
        if (image == null) {
            _errorMessage.value = "请先选择一张图片"
            return
        }

        _isProcessing.value = true
        _progress.value = 0f
        _errorMessage.value = null

        val result: Bitmap? = withContext(Dispatchers.Default) {
            try {
                when (_selectedTool.value) {
                    AIEditTool.REMOVE -> applyRemoveTool(image)
                    AIEditTool.SKY_REPLACE -> applySkyReplaceTool(image)
                    AIEditTool.EXPAND -> applyExpandTool(image)
                    AIEditTool.STYLE_TRANSFER -> applyStyleTransferTool(image)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "编辑处理异常", e)
                null
            }
        }

        if (result != null) {
            _editedImage.value = result
            _isProcessing.value = false
            _progress.value = 1.0f
        } else {
            _isProcessing.value = false
            _progress.value = 0f
            _errorMessage.value = "编辑处理失败，请重试"
        }
    }

    /**
     * 物体移除
     *
     * 需要 [maskRect] 宽高均大于 0，否则视为失败。
     *
     * @param image 源图像
     * @return 移除物体后的图像，失败返回 null
     */
    private suspend fun applyRemoveTool(image: Bitmap): Bitmap? {
        val rect = _maskRect.value ?: return null
        if (rect.width() <= 0f || rect.height() <= 0f) return null
        return objectRemover.removeObject(image, rect)
    }

    /**
     * 天空替换
     *
     * @param image 源图像
     * @return 替换天空后的图像，失败返回 null
     */
    private suspend fun applySkyReplaceTool(image: Bitmap): Bitmap? {
        return skyReplacer.replaceSky(image, _selectedSkyType.value)
    }

    /**
     * 图像扩展
     *
     * 需要 [expandAmount] 大于 0，否则视为失败。
     *
     * @param image 源图像
     * @return 扩展后的图像，失败返回 null
     */
    private suspend fun applyExpandTool(image: Bitmap): Bitmap? {
        if (_expandAmount.value <= 0) return null
        return imageExpander.expandImage(image, _expandAmount.value, _expandDirection.value)
    }

    /**
     * 风格迁移
     *
     * @param image 源图像
     * @return 风格化后的图像，失败返回 null
     */
    private suspend fun applyStyleTransferTool(image: Bitmap): Bitmap? {
        return styleTransfer.applyStyle(image, _selectedStyle.value, _styleIntensity.value)
    }

    // MARK: - 快捷操作方法

    /**
     * 快速应用物体移除
     *
     * 设置掩码矩形与工具后立即执行编辑。
     *
     * @param rect 移除区域矩形（归一化坐标 0..1）
     */
    suspend fun quickRemove(rect: RectF) {
        _maskRect.value = rect
        _selectedTool.value = AIEditTool.REMOVE
        applyEdit()
    }

    /**
     * 快速应用天空替换
     *
     * 设置天空类型与工具后立即执行编辑。
     *
     * @param type 天空类型
     */
    suspend fun quickSkyReplace(type: SkyReplacer.SkyType) {
        _selectedSkyType.value = type
        _selectedTool.value = AIEditTool.SKY_REPLACE
        applyEdit()
    }

    /**
     * 快速应用图像扩展
     *
     * 设置扩展像素数、方向与工具后立即执行编辑。
     *
     * @param pixels 每边扩展像素数
     * @param direction 扩展方向
     */
    suspend fun quickExpand(pixels: Int, direction: ImageExpander.ExpansionDirection) {
        _expandAmount.value = pixels
        _expandDirection.value = direction
        _selectedTool.value = AIEditTool.EXPAND
        applyEdit()
    }

    /**
     * 快速应用风格迁移
     *
     * 设置艺术风格、强度与工具后立即执行编辑。
     *
     * @param style 艺术风格
     * @param intensity 风格强度
     */
    suspend fun quickStyleTransfer(style: StyleTransfer.ArtStyle, intensity: Float) {
        _selectedStyle.value = style
        _styleIntensity.value = intensity
        _selectedTool.value = AIEditTool.STYLE_TRANSFER
        applyEdit()
    }

    // MARK: - 重置与撤销

    /**
     * 重置所有编辑状态（清空编辑结果、掩码、错误信息，结束处理状态）
     */
    fun reset() {
        _editedImage.value = null
        _maskRect.value = null
        _errorMessage.value = null
        _isProcessing.value = false
        _progress.value = 0f
    }

    /**
     * 将编辑结果设为源图像（基于上一次结果继续编辑）
     *
     * 清空编辑结果与掩码矩形，使下一次编辑基于当前结果进行。
     */
    fun applyAndContinue() {
        val edited = _editedImage.value
        if (edited != null) {
            _sourceImage.value = edited
            _editedImage.value = null
            _maskRect.value = null
        }
    }

    /**
     * 撤销编辑（丢弃编辑结果，恢复源图像状态）
     */
    fun undo() {
        _editedImage.value = null
        _maskRect.value = null
        _errorMessage.value = null
    }

    // MARK: - 状态查询

    /** 是否有编辑结果 */
    val hasEditResult: Boolean
        get() = _editedImage.value != null

    /** 是否可以进行物体移除（已设置有效移除区域） */
    val canRemove: Boolean
        get() {
            val rect = _maskRect.value ?: return false
            return rect.width() > 0f && rect.height() > 0f
        }

    /** 当前工具的描述 */
    val currentToolDescription: String
        get() = when (_selectedTool.value) {
            AIEditTool.REMOVE -> "在图片上涂抹想要移除的物体区域"
            AIEditTool.SKY_REPLACE -> "选择一种天空类型替换当前天空"
            AIEditTool.EXPAND -> "设置扩展像素数，向外扩展画布"
            AIEditTool.STYLE_TRANSFER -> "选择一种艺术风格应用到图片"
        }

    // MARK: - 资源清理

    override fun onCleared() {
        super.onCleared()
        try {
            // 释放持有的位图引用，避免内存泄漏
            _sourceImage.value = null
            _editedImage.value = null
        } catch (e: Exception) {
            AppLogger.w(TAG, "AIEditViewModel 资源清理异常", e)
        }
    }
}
