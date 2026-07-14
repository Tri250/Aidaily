package com.livecompose.livecapture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livecompose.livecapture.core.design.LiveCaptureTheme
import com.livecompose.livecapture.core.settings.FirstRunRepository
import com.livecompose.livecapture.presentation.MainTabView
import com.livecompose.livecapture.presentation.onboarding.OnboardingView
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: com.livecompose.livecapture.presentation.settings.SettingsViewModel = hiltViewModel()
            val isDarkTheme by settingsViewModel.darkTheme.collectAsStateWithLifecycle()
            
            val firstRunRepository = (application as LiveCaptureApp).firstRunRepository
            val hasCompletedOnboarding by firstRunRepository.hasCompletedOnboarding.collectAsStateWithLifecycle(false)
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }

            // 初始化引导页显示状态
            LaunchedEffect(hasCompletedOnboarding) {
                showOnboarding = !hasCompletedOnboarding
            }

            LiveCaptureTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (showOnboarding) {
                        null -> {
                            // 加载中，不显示任何内容
                        }
                        true -> {
                            // 显示引导页
                            OnboardingView(
                                onComplete = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        firstRunRepository.setOnboardingCompleted(true)
                                    }
                                    showOnboarding = false
                                },
                                onSkip = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        firstRunRepository.setOnboardingCompleted(true)
                                    }
                                    showOnboarding = false
                                }
                            )
                        }
                        false -> {
                            // 显示主界面
                            MainTabView()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // #69: onTerminate() 在真机上不可靠，onDestroy() 中释放资源
        if (isFinishing) {
            (application as LiveCaptureApp).releaseResources()
        }
    }
}