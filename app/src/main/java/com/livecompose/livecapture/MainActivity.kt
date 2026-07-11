package com.livecompose.livecapture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livecompose.livecapture.core.design.LiveCaptureTheme
import com.livecompose.livecapture.presentation.MainTabView
import com.livecompose.livecapture.presentation.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkTheme by settingsViewModel.darkTheme.collectAsStateWithLifecycle()

            LiveCaptureTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainTabView()
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
