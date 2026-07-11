package com.livecompose.livecapture.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livecompose.livecapture.core.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    val pageCount = 3

    fun setCurrentPage(page: Int) {
        _currentPage.value = page.coerceIn(0, pageCount - 1)
    }

    fun nextPage() {
        val next = _currentPage.value + 1
        if (next < pageCount) {
            _currentPage.value = next
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
        }
    }
}
