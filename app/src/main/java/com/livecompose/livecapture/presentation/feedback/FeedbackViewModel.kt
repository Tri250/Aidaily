package com.livecompose.livecapture.presentation.feedback

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class FeedbackState(
    val category: String = "bug",
    val content: String = "",
    val email: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackState())
    val state: StateFlow<FeedbackState> = _state.asStateFlow()

    fun setCategory(category: String) {
        _state.value = _state.value.copy(category = category)
    }

    fun setContent(content: String) {
        _state.value = _state.value.copy(content = content)
    }

    fun setEmail(email: String) {
        _state.value = _state.value.copy(email = email)
    }

    fun submitFeedback() {
        val currentState = _state.value
        if (currentState.content.isBlank()) {
            // TODO(i18n): R.string.feedback_content_required — 需引入 StringResourceResolver
            _state.value = currentState.copy(errorMessage = "请输入反馈内容")
            return
        }

        viewModelScope.launch {
            _state.value = currentState.copy(isSubmitting = true, errorMessage = null)

            try {
                saveFeedbackLocally(currentState)
                _state.value = FeedbackState(isSubmitted = true)
            } catch (e: Exception) {
                // TODO(i18n): 提交失败提示需使用 StringResourceResolver 格式化
                _state.value = currentState.copy(
                    isSubmitting = false,
                    errorMessage = "提交失败: ${e.message}"
                )
            }
        }
    }

    // 注意: 此方法中的中文字符串写入本地日志文件，非 UI 展示文案，无需国际化。
    private fun saveFeedbackLocally(state: FeedbackState) {
        val feedbackDir = File(context.filesDir, "feedback")
        if (!feedbackDir.exists()) feedbackDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(timestamp))
        val file = File(feedbackDir, "feedback_$dateStr.txt")

        val content = buildString {
            appendLine("=== 用户反馈 ===")
            appendLine("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))}")
            appendLine("类别: ${state.category}")
            appendLine("邮箱: ${state.email.ifBlank { "未提供" }}")
            appendLine()
            appendLine("内容:")
            appendLine(state.content)
        }

        file.writeText(content)
    }

    fun reset() {
        _state.value = FeedbackState()
    }
}
