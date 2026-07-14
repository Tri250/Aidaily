package com.livecompose.livecapture.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 声控拍照服务
 * 通过语音识别触发拍摄
 *
 * 支持的触发词:
 * - "拍照"
 * - "茄子" (中国用户习惯)
 * - "cheese"
 * - "开始"
 * - "拍"
 */
@Singleton
class VoiceCaptureService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceCaptureService"

        // 触发关键词列表
        private val TRIGGER_WORDS = listOf(
            "拍照", "茄子", "cheese", "开始", "拍", "shot",
            "咔嚓", "拍照吗", "cheese", "shoot"
        )
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _lastHeardText = MutableStateFlow("")
    val lastHeardText: StateFlow<String> = _lastHeardText

    private val _captureTriggered = MutableStateFlow(false)
    val captureTriggered: StateFlow<Boolean> = _captureTriggered

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "准备接收语音")
            _isReady.value = true
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "开始说话")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // 音量变化
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            Log.d(TAG, "说话结束")
            _isReady.value = false
            // 自动重启监听
            if (isListening) {
                restartListening()
            }
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙碌"
                SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音超时"
                else -> "未知错误: $error"
            }
            Log.w(TAG, "语音识别错误: $errorMessage")
            _isReady.value = false

            // 错误后自动重启
            if (isListening && error != SpeechRecognizer.ERROR_CLIENT) {
                restartListening()
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null && matches.isNotEmpty()) {
                val text = matches[0]
                Log.d(TAG, "识别结果: $text")
                _lastHeardText.value = text

                // 检查是否包含触发词
                val lowerText = text.lowercase()
                val shouldTrigger = TRIGGER_WORDS.any { trigger ->
                    lowerText.contains(trigger.lowercase())
                }

                if (shouldTrigger) {
                    Log.i(TAG, "检测到触发词，触发拍摄")
                    _captureTriggered.value = true
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // 部分结果，可用于实时显示
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * 启动语音监听
     */
    fun startListening() {
        if (isListening) return

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            Log.i(TAG, "语音监听已启动")
        } catch (e: Exception) {
            Log.e(TAG, "启动语音监听失败", e)
        }
    }

    /**
     * 停止语音监听
     */
    fun stopListening() {
        if (!isListening) return

        try {
            speechRecognizer?.stopListening()
            isListening = false
            _isReady.value = false
            Log.i(TAG, "语音监听已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止语音监听失败", e)
        }
    }

    /**
     * 重启语音监听
     */
    private fun restartListening() {
        try {
            speechRecognizer?.cancel()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "重启语音监听失败", e)
        }
    }

    /**
     * 重置触发状态
     */
    fun resetTrigger() {
        _captureTriggered.value = false
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "释放语音资源失败", e)
        }
    }
}