package com.livecompose.livecapture.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.crash.CrashHandler
import com.livecompose.livecapture.core.crash.CrashHandler.CrashLogEntry
import com.livecompose.livecapture.core.perf.MemoryPressure
import com.livecompose.livecapture.core.perf.PerformanceMonitor
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
    private val performanceMonitor: PerformanceMonitor,
    private val crashHandler: CrashHandler
) : ViewModel() {

    val detectionMode: StateFlow<String> = repository.detectionMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "FAST")

    val autoCapture: StateFlow<Boolean> = repository.autoCapture
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val captureDelay: StateFlow<Int> = repository.captureDelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val darkTheme: StateFlow<Boolean> = repository.darkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val torchEnabled: StateFlow<Boolean> = repository.torchEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val privacyConsented: StateFlow<Boolean> = repository.privacyConsented
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val onboardingCompleted: StateFlow<Boolean> = repository.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Performance monitoring
    val fps: StateFlow<Float> = performanceMonitor.fps
    val jankCount: StateFlow<Long> = performanceMonitor.jankCount
    val isPerformant: StateFlow<Boolean> = performanceMonitor.isPerformant
    val memoryUsageMb: StateFlow<Float> = performanceMonitor.memoryUsageMb
    val memoryPressure: StateFlow<MemoryPressure> = performanceMonitor.memoryPressure
    val isMonitoring: StateFlow<Boolean> = performanceMonitor.isMonitoring

    // Crash logs
    private val _crashLogs = kotlinx.coroutines.flow.MutableStateFlow<List<CrashLogEntry>>(emptyList())
    val crashLogs: StateFlow<List<CrashLogEntry>> = _crashLogs

    fun setDetectionMode(mode: String) = viewModelScope.launch {
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

    fun setPrivacyConsented(consented: Boolean) = viewModelScope.launch {
        repository.setPrivacyConsented(consented)
    }

    fun setOnboardingCompleted(completed: Boolean) = viewModelScope.launch {
        repository.setOnboardingCompleted(completed)
    }

    fun togglePerformanceMonitoring(enabled: Boolean) {
        if (enabled) {
            performanceMonitor.startMonitoring()
        } else {
            performanceMonitor.stopMonitoring()
        }
    }

    fun loadCrashLogs() {
        _crashLogs.value = crashHandler.getCrashLogs()
    }

    fun clearCrashLogs() {
        crashHandler.clearCrashLogs()
        _crashLogs.value = emptyList()
    }
}
