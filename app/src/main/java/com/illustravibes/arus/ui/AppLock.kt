package com.illustravibes.arus.ui

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object AppLock {
    private const val PREFS = "arus_app_lock"
    private const val HASH = "pin_hash"
    private const val SALT = "pin_salt"
    private const val BIOMETRIC = "biometric_enabled"

    fun isEnabled(context: Context): Boolean = prefs(context).contains(HASH)

    fun verify(context: Context, pin: String): Boolean {
        val salt = prefs(context).getString(SALT, null) ?: return false
        val expected = prefs(context).getString(HASH, null) ?: return false
        return MessageDigest.isEqual(Base64.decode(expected, Base64.NO_WRAP), digest(pin, Base64.decode(salt, Base64.NO_WRAP)))
    }

    fun setPin(context: Context, pin: String) {
        require(pin.matches(Regex("\\d{6}")))
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        prefs(context).edit()
            .putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(HASH, Base64.encodeToString(digest(pin, salt), Base64.NO_WRAP))
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(HASH).remove(SALT).remove(BIOMETRIC).apply()
    }

    fun biometricEnabled(context: Context): Boolean = prefs(context).getBoolean(BIOMETRIC, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(BIOMETRIC, enabled).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun digest(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
