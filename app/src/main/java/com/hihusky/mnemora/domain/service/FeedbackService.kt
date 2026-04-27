package com.hihusky.mnemora.domain.service

import android.content.Context
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hihusky.mnemora.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()
    private var streak = 0

    var soundEnabled = true
    var hapticEnabled = true
    var continuousFeedback = true

    init {
        loadSounds()
    }

    private fun loadSounds() {
        val pool = SoundPool.Builder().setMaxStreams(3).build()
        soundPool = pool
        soundMap[1] = pool.load(context, R.raw.streak_1, 1)
        soundMap[2] = pool.load(context, R.raw.streak_2, 1)
        soundMap[3] = pool.load(context, R.raw.streak_3, 1)
        soundMap[4] = pool.load(context, R.raw.streak_4, 1)
        soundMap[5] = pool.load(context, R.raw.streak_5, 1)
        soundMap[6] = pool.load(context, R.raw.streak_ace, 1)
        soundMap[-1] = pool.load(context, R.raw.wrong, 1)
    }

    fun incrementStreak() {
        streak++
    }

    fun resetStreak() {
        streak = 0
    }

    fun playCorrect() {
        if (!soundEnabled) return
        val level = if (continuousFeedback) {
            when {
                streak >= 6 -> 6
                streak >= 5 -> 5
                streak >= 4 -> 4
                streak >= 3 -> 3
                streak >= 2 -> 2
                else -> 1
            }
        } else 1
        soundPool?.play(soundMap[level] ?: 0, 1f, 1f, 1, 0, 1f)
        if (hapticEnabled) vibrate()
    }

    fun playWrong() {
        if (!soundEnabled) return
        soundPool?.play(soundMap[-1] ?: 0, 1f, 1f, 1, 0, 1f)
        if (hapticEnabled) vibrateLong()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    private fun vibrateLong() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(100)
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
