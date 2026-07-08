package com.livecompose.livecapture.features.compliance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.livecompose.livecapture.ui.design.LiveCaptureTheme

/**
 * 合规页面宿主 Activity
 * 用于承载隐私政策、用户协议、SDK清单等全屏页面
 */
class ComplianceHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val page = intent.getStringExtra("compliance_page") ?: "privacy"

        setContent {
            LiveCaptureTheme {
                when (page) {
                    "privacy" -> PrivacyPolicyScreen(onBack = { finish() })
                    "agreement" -> UserAgreementScreen(onBack = { finish() })
                    "sdk_list" -> ThirdPartySDKScreen(onBack = { finish() })
                    else -> PrivacyPolicyScreen(onBack = { finish() })
                }
            }
        }
    }
}