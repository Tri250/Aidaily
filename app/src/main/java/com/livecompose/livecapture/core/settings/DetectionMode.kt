package com.livecompose.livecapture.core.settings

enum class DetectionMode(val value: String) {
    FAST("FAST"),
    PRO("PRO");

    companion object {
        fun fromValue(value: String?): DetectionMode =
            values().find { it.value == value } ?: FAST
    }
}
