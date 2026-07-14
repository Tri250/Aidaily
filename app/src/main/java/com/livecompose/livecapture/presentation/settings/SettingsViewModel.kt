package com.livecompose.livecapture.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.diagnostics.SelfChecker
import com.livecompose.livecapture.core.settings.DetectionMode
import com.livecompose.livecapture.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val selfChecker: SelfChecker
) : ViewModel() {

    val detectionMode: StateFlow<DetectionMode> = repository.detectionMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetectionMode.FAST)

    val autoCapture: StateFlow<Boolean> = repository.autoCapture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val captureDelay: StateFlow<Int> = repository.captureDelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val darkTheme: StateFlow<Boolean> = repository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val torchEnabled: StateFlow<Boolean> = repository.torchEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val watermarkEnabled: StateFlow<Boolean> = repository.watermarkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val gridEnabled: StateFlow<Boolean> = repository.gridEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val voiceCaptureDefault: StateFlow<Boolean> = repository.voiceCaptureDefault
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hapticEnabled: StateFlow<Boolean> = repository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val sceneRecognitionEnabled: StateFlow<Boolean> = repository.sceneRecognitionEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val aspectRatio: StateFlow<String> = repository.aspectRatio
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "3:4")

    val selfCheckResults: StateFlow<List<SelfChecker.CheckItem>> = selfChecker.checkResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDetectionMode(mode: DetectionMode) = viewModelScope.launch {
        repository.setDetectionMode(mode)
    }

    fun setAutoCapture(enabled: Boolean) = viewModelScope.launch {
        repository.setAutoCapture(enabled)
    }

    fun setCaptureDelay(delay: Int) = viewModelScope.launch {
        repository.setCaptureDelay(delay)
    }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        repository.setDarkTheme(enabled)
    }

    fun setTorchEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setTorchEnabled(enabled)
    }

    fun setWatermarkEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setWatermarkEnabled(enabled)
    }

    fun setGridEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setGridEnabled(enabled)
    }

    fun setVoiceCaptureDefault(enabled: Boolean) = viewModelScope.launch {
        repository.setVoiceCaptureDefault(enabled)
    }

    fun setHapticEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setHapticEnabled(enabled)
    }

    fun setSceneRecognitionEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.setSceneRecognitionEnabled(enabled)
    }

    fun setAspectRatio(ratio: String) = viewModelScope.launch {
        repository.setAspectRatio(ratio)
    }

    fun runSelfCheck() = viewModelScope.launch {
        selfChecker.runFullCheck()
    }
}
