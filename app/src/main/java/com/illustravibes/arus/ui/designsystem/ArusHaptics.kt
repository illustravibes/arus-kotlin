package com.illustravibes.arus.ui.designsystem

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Satu-satunya pembungkus interaksi haptic Arus.
 * Menghormati saklar haptic aplikasi dan setelan sistem Android.
 */
class ArusHapticFeedback(
    private val view: View,
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("arus_preferences", Context.MODE_PRIVATE)

    private fun isHapticEnabled(): Boolean {
        return prefs.getBoolean("haptics_enabled", true)
    }

    private inline fun safelyHaptic(action: () -> Unit) {
        try {
            action()
        } catch (_: SecurityException) {
            // Haptics are optional; restricted test and system contexts may deny vibration.
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Klik ringan pada tombol utama / aksi primer
     */
    fun click() {
        if (!isHapticEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Tick halus untuk pemilihan chip, tab segmented, geser carousel, dan keypad
     */
    fun tick() {
        if (!isHapticEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Pola haptic ketika saklar dihidupkan (pulsa naik)
     */
    fun switchOn() {
        if (!isHapticEnabled()) return
        val vibrator = getVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safelyHaptic { vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)) }
        } else {
            @Suppress("DEPRECATION")
            safelyHaptic { vibrator.vibrate(20) }
        }
    }

    /**
     * Pola haptic ketika saklar dimatikan (pulsa turun pendek)
     */
    fun switchOff() {
        if (!isHapticEnabled()) return
        val vibrator = getVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            safelyHaptic { vibrator.vibrate(VibrationEffect.createOneShot(10, 80)) }
        } else {
            @Suppress("DEPRECATION")
            safelyHaptic { vibrator.vibrate(10) }
        }
    }

    /**
     * Konfirmasi keberhasilan: Simpan berhasil, saldo cocok, pelunasan tercatat (double pulse)
     */
    fun confirm() {
        if (!isHapticEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            val vibrator = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safelyHaptic {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 20, 40, 25), intArrayOf(0, 150, 0, 255), -1))
            }
            } else {
                @Suppress("DEPRECATION")
                safelyHaptic { vibrator.vibrate(longArrayOf(0, 20, 40, 25), -1) }
            }
        }
    }

    /**
     * Penolakan: Validasi gagal, PIN salah, limit terlampaui (triple quick buzz)
     */
    fun reject() {
        if (!isHapticEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            val vibrator = getVibrator() ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safelyHaptic {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 30, 25, 30, 25), intArrayOf(0, 255, 0, 255, 0, 255), -1))
            }
            } else {
                @Suppress("DEPRECATION")
                safelyHaptic { vibrator.vibrate(longArrayOf(0, 25, 30, 25, 30, 25), -1) }
            }
        }
    }

    /**
     * Peringatan: Sebelum aksi destruktif (hapus) / long press
     */
    fun warning() {
        if (!isHapticEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

object ArusHaptics {
    var activeFeedback: ArusHapticFeedback? = null

    fun tick() = activeFeedback?.tick()
    fun lightClick() = activeFeedback?.click()
    fun click() = activeFeedback?.click()
    fun switchOn() = activeFeedback?.switchOn()
    fun switchOff() = activeFeedback?.switchOff()
    fun confirm() = activeFeedback?.confirm()
    fun reject() = activeFeedback?.reject()
    fun warning() = activeFeedback?.warning()
}

@Composable
fun rememberArusHaptics(): ArusHapticFeedback {
    val view = LocalView.current
    val context = LocalContext.current
    return remember(view, context) {
        ArusHapticFeedback(view, context).also {
            ArusHaptics.activeFeedback = it
        }
    }
}

