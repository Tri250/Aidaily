package com.livecompose.livecapture.features.compliance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.community.CommunityScreen

/**
 * 合规页面宿主 Activity
 * 用于承载隐私政策、用户协议、SDK清单、青少年模式、ICP备案、社区等全屏页面
 */
class ComplianceHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val page = intent.getStringExtra("compliance_page") ?: "privacy"
        val appContainer = AppContainer.getInstance(this)

        setContent {
            MaterialTheme {
                when (page) {
                    "privacy" -> PrivacyPolicyScreen(onBack = { finish() })
                    "agreement" -> UserAgreementScreen(onBack = { finish() })
                    "sdk_list" -> ThirdPartySDKScreen(onBack = { finish() })
                    "youth_mode" -> YouthModeScreen(
                        manager = appContainer.youthModeManager,
                        onBack = { finish() }
                    )
                    "icp_filing" -> IcpFilingScreen(onBack = { finish() })
                    "community" -> CommunityScreen(
                        communityManager = appContainer.communityManager,
                        locationRecommender = appContainer.locationRecommender
                    )
                    else -> PrivacyPolicyScreen(onBack = { finish() })
                }
            }
        }
    }
}