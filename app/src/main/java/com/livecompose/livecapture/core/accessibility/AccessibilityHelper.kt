package com.livecompose.livecapture.core.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.Role
import com.livecompose.livecapture.core.storage.PhotoRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Accessibility helper utilities for consistent semantics across the app.
 * All user-facing strings should use string resources, but this object
 * provides the composable/Modifier helpers that build on those resources.
 */
object AccessibilityHelper {

    /**
     * Announces a state change for accessibility by applying a live region modifier.
     * Use this for dynamic content that should be announced when it changes
     * (e.g., guidance text, alignment status).
     */
    fun Modifier.announceLiveRegion(mode: LiveRegionMode = LiveRegionMode.Polite): Modifier =
        this.semantics { liveRegion = mode }

    /**
     * Modifier extension for toggle semantics (Switch, Checkbox, etc.).
     * Sets role, stateDescription, and contentDescription.
     */
    fun Modifier.semanticsForToggle(
        label: String,
        isOn: Boolean
    ): Modifier = this.semantics {
        role = Role.Switch
        stateDescription = if (isOn) "$label，已开启" else "$label，已关闭"
        contentDescription = label
    }

    /**
     * Modifier extension for slider semantics.
     * Sets role, stateDescription, and contentDescription with current value.
     */
    fun Modifier.semanticsForSlider(
        label: String,
        value: Float,
        range: ClosedFloatingPointRange<Float>
    ): Modifier = this.semantics {
        role = Role.Slider
        stateDescription = "$label，当前值 ${value.toInt()}"
        contentDescription = "$label，范围 ${range.start.toInt()} 到 ${range.endInclusive.toInt()}"
    }

    /**
     * Generates an accessible content description for a photo record.
     * Format: "照片，拍摄于 <date>"
     */
    fun contentDescriptionForPhoto(record: PhotoRecord): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(record.timestamp))
        return "照片，拍摄于 $dateStr"
    }
}
