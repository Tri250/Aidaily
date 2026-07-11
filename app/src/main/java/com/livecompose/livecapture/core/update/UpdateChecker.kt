package com.livecompose.livecapture.core.update

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val changelog: String,
    val isRequired: Boolean
)

class UpdateChecker {

    fun checkForUpdate(currentVersion: String): UpdateInfo? {
        // Placeholder: 离线优先应用，暂不连接服务器检查更新
        // 生产环境中可对接自有更新服务器或 In-App Update API
        return null
    }
}
