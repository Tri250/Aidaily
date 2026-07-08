package com.livecompose.livecapture.core.compliance

import android.content.Context
import android.content.pm.PackageManager

/**
 * ICP 备案信息数据模型
 *
 * 对应 iOS 端 ICPFilingInfo，存储工信部 ICP 备案所需信息。
 *
 * @param icpNumber ICP 备案号
 * @param companyName 主办单位（公司全称）
 * @param auditDate 审核日期（格式 yyyy-MM-dd）
 * @param queryUrl 工信部备案查询 URL
 * @param networkSecurityNumber 公安网安备案号（可选）
 */
data class IcpFilingInfo(
    val icpNumber: String,
    val companyName: String,
    val auditDate: String,
    val queryUrl: String,
    val networkSecurityNumber: String? = null
) {

    companion object {
        /** 工信部备案系统默认查询地址 */
        const val DEFAULT_QUERY_URL = "https://beian.miit.gov.cn"

        /** 占位默认值（未备案） */
        fun default(): IcpFilingInfo = IcpFilingInfo(
            icpNumber = "",
            companyName = "",
            auditDate = "",
            queryUrl = DEFAULT_QUERY_URL,
            networkSecurityNumber = null
        )

        /**
         * 从 AndroidManifest <meta-data> 读取备案信息。
         *
         * 支持的 meta-data key：
         * - ICP_FILING_NUMBER: ICP 备案号
         * - COMPANY_NAME: 主办单位全称
         * - ICP_AUDIT_DATE: 审核日期
         * - ICP_QUERY_URL: 工信部查询链接（缺省时使用默认）
         * - NETWORK_SECURITY_NUMBER: 公安网安备案号（可选）
         *
         * 读取失败时返回 [default]。
         */
        fun fromManifest(context: Context): IcpFilingInfo = try {
            val ai = context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val meta = ai.metaData
            IcpFilingInfo(
                icpNumber = meta?.getString("ICP_FILING_NUMBER") ?: "",
                companyName = meta?.getString("COMPANY_NAME") ?: "",
                auditDate = meta?.getString("ICP_AUDIT_DATE") ?: "",
                queryUrl = meta?.getString("ICP_QUERY_URL")?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_QUERY_URL,
                networkSecurityNumber = meta?.getString("NETWORK_SECURITY_NUMBER")
                    ?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            default()
        }
    }

    /** 是否已完成备案 */
    val isFiled: Boolean
        get() = icpNumber.isNotBlank() && companyName.isNotBlank()

    /** 备案号展示文本（未备案时显示占位） */
    val icpDisplayNumber: String
        get() = icpNumber.ifBlank { "ICP备案号：待备案" }
}
