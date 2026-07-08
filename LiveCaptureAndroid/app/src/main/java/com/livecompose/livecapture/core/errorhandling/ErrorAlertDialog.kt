package com.livecompose.livecapture.core.errorhandling

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 错误弹窗
 *
 * 对应 iOS 端 ErrorAlertView。当 [error] 非空时展示 Material3 [AlertDialog]，
 * 包含分类标题、图标、错误描述与恢复建议，并提供「关闭」与可选「重试」按钮。
 *
 * @param error 当前错误，为 null 时不展示任何内容
 * @param onDismiss 关闭回调
 * @param onRetry 可选的重试回调，提供时展示「重试」按钮
 * @param modifier 修饰符
 */
@Composable
fun ErrorAlertDialog(
    error: AppError?,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (error != null) {
        val icon = remember(error.category) { iconForCategory(error.category) }

        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            },
            title = {
                Text(text = error.category.displayName)
            },
            text = {
                Column {
                    Text(
                        text = error.localizedDescription,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error.recoverySuggestion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Text("重试")
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            },
            dismissButton = {
                if (onRetry != null) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        )
    }
}

/**
 * 根据错误分类返回对应图标
 *
 * 权限与未知错误使用警告图标，其余使用错误图标。
 */
private fun iconForCategory(category: ErrorCategory): ImageVector {
    return when (category) {
        ErrorCategory.PERMISSION, ErrorCategory.UNKNOWN -> Icons.Default.Warning
        else -> Icons.Default.Error
    }
}
