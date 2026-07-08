package com.livecompose.livecapture.utilities

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 触觉反馈管理器
 */
object HapticManager {
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate(effect: VibrationEffect) {
        vibrator?.vibrate(effect)
    }

    fun light() {
        vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun medium() {
        vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun heavy() {
        vibrate(VibrationEffect.createOneShot(30, 200))
    }

    fun soft() {
        vibrate(VibrationEffect.createOneShot(8, 50))
    }

    fun rigid() {
        vibrate(VibrationEffect.createOneShot(15, 150))
    }

    fun selection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            light()
        }
    }

    fun success() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            medium()
        }
    }

    fun warning() {
        val timings = longArrayOf(0, 30, 50, 30)
        val amplitudes = intArrayOf(0, 100, 0, 100)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    fun error() {
        val timings = longArrayOf(0, 50, 50, 50, 50, 50)
        val amplitudes = intArrayOf(0, 200, 0, 200, 0, 200)
        vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    fun capture() {
        medium()
        // 延迟后轻触
        vibrator?.let { v ->
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                v.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            }, 50)
        }
    }

    fun focusLock() {
        soft()
        vibrator?.let { v ->
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                v.vibrate(VibrationEffect.createOneShot(8, 50))
            }, 80)
        }
    }

    fun zoomSnap() {
        rigid()
    }
}