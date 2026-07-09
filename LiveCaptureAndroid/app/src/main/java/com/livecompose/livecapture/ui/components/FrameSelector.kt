package com.livecompose.livecapture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.livecompose.livecapture.core.frame.FrameInfo
import com.livecompose.livecapture.ui.design.DesignSystem

/**
 * 相框选择器组件
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun FrameSelector(
    frames: List<FrameInfo> = FrameInfo.ALL_BUILT_IN,
    selectedFrame: FrameInfo?,
    onFrameSelected: (FrameInfo?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("相框", color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 无边框选项
            FrameChip(
                name = "无",
                color = Color.Transparent,
                isSelected = selectedFrame == null || selectedFrame.id == "no_frame",
                onClick = { onFrameSelected(null) }
            )

            frames.filter { it.id != "no_frame" }.forEach { frame ->
                FrameChip(
                    name = frame.name,
                    color = Color(frame.borderColor),
                    isSelected = selectedFrame?.id == frame.id,
                    onClick = { onFrameSelected(frame) }
                )
            }
        }
    }
}

@Composable
private fun FrameChip(name: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) DesignSystem.Colors.minimalLabel.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) DesignSystem.Colors.minimalBorder else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 缩略色块
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DesignSystem.Colors.gray2())
                .then(
                    if (color != Color.Transparent)
                        Modifier.border(2.dp, color, RoundedCornerShape(4.dp))
                    else Modifier
                )
        )
        Spacer(Modifier.height(4.dp))
        Text(name, color = DesignSystem.Colors.minimalSecondaryLabel, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}
