package com.livecompose.livecapture.presentation.compose

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class CompositionTip(
    val titleResId: Int,
    val descResId: Int
)

@HiltViewModel
class LiveComposeViewModel @Inject constructor() : ViewModel() {

    val compositionTips = listOf(
        CompositionTip(
            com.livecompose.livecapture.R.string.compose_tip_rule_of_thirds_title,
            com.livecompose.livecapture.R.string.compose_tip_rule_of_thirds_desc
        ),
        CompositionTip(
            com.livecompose.livecapture.R.string.compose_tip_center_title,
            com.livecompose.livecapture.R.string.compose_tip_center_desc
        ),
        CompositionTip(
            com.livecompose.livecapture.R.string.compose_tip_leading_lines_title,
            com.livecompose.livecapture.R.string.compose_tip_leading_lines_desc
        ),
        CompositionTip(
            com.livecompose.livecapture.R.string.compose_tip_symmetry_title,
            com.livecompose.livecapture.R.string.compose_tip_symmetry_desc
        )
    )

    private val _currentTipIndex = MutableStateFlow(0)
    val currentTipIndex: StateFlow<Int> = _currentTipIndex.asStateFlow()

    private val _expandedFeatureCard = MutableStateFlow(-1)
    val expandedFeatureCard: StateFlow<Int> = _expandedFeatureCard.asStateFlow()

    private val _isDemoAnimating = MutableStateFlow(false)
    val isDemoAnimating: StateFlow<Boolean> = _isDemoAnimating.asStateFlow()

    private val _demoExampleIndex = MutableStateFlow(0)
    val demoExampleIndex: StateFlow<Int> = _demoExampleIndex.asStateFlow()

    fun cycleTip() {
        _currentTipIndex.value = (_currentTipIndex.value + 1) % compositionTips.size
    }

    fun setExpandedFeatureCard(index: Int) {
        _expandedFeatureCard.value = if (_expandedFeatureCard.value == index) -1 else index
    }

    fun setDemoAnimating(animating: Boolean) {
        _isDemoAnimating.value = animating
    }

    fun cycleDemoExample() {
        _demoExampleIndex.value = (_demoExampleIndex.value + 1) % 4
    }
}
