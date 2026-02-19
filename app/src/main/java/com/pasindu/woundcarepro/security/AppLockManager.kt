package com.pasindu.woundcarepro.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class LockGateState {
    NEEDS_PIN_SETUP,
    LOCKED,
    UNLOCKED
}

class AppLockManager(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context)
    private val _gateState = MutableStateFlow(resolveInitialState())
    val gateState: StateFlow<LockGateState> = _gateState

    private var backgroundAtElapsed: Long? = null

    fun onAppForegrounded() {
        val nowElapsed = SystemClock.elapsedRealtime()
        val bgAt = backgroundAtElapsed
        val lastInteraction = prefs.getLong(KEY_LAST_INTERACTION_ELAPSED, nowElapsed)
        val timedOutInBackground = bgAt != null && nowElapsed - bgAt >= SESSION_TIMEOUT_MS
        val timedOutInForeground = nowElapsed - lastInteraction >= SESSION_TIMEOUT_MS
        if (isPinConfigured() && (timedOutInBackground || timedOutInForeground)) {
            _gateState.value = LockGateState.LOCKED
        }
        markInteraction()
        backgroundAtElapsed = null
    }

    fun onAppBackgrounded() {
        backgroundAtElapsed = SystemClock.elapsedRealtime()
    }

    fun markInteraction() {
        prefs.edit().putLong(KEY_LAST_INTERACTION_ELAPSED, SystemClock.elapsedRealtime()).apply()
    }

    fun isPinConfigured(): Boolean {
        return prefs.contains(KEY_PIN_HASH)
    }

    fun setupPin(pin: String): Boolean {
        if (!pin.matches(Regex("^\\d{4,6}$"))) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = deriveHash(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_COOLDOWN_UNTIL, 0L)
            .apply()
        _gateState.value = LockGateState.UNLOCKED
        markInteraction()
        return true
    }

    fun unlock(pin: String): UnlockResult {
        val now = System.currentTimeMillis()
        val cooldownUntil = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L)
        if (cooldownUntil > now) {
            return UnlockResult.Cooldown((cooldownUntil - now) / 1000)
        }

        val ok = verifyPin(pin)
        if (ok) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_COOLDOWN_UNTIL, 0L).apply()
            _gateState.value = LockGateState.UNLOCKED
            markInteraction()
            return UnlockResult.Success
        }

        val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        return if (attempts >= MAX_ATTEMPTS) {
            val until = now + COOLDOWN_MS
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_COOLDOWN_UNTIL, until).apply()
            UnlockResult.Cooldown(COOLDOWN_MS / 1000)
        } else {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
            UnlockResult.Invalid(MAX_ATTEMPTS - attempts)
        }
    }

    private fun verifyPin(pin: String): Boolean {
        val hashString = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltString = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = Base64.decode(hashString, Base64.NO_WRAP)
        val salt = Base64.decode(saltString, Base64.NO_WRAP)
        val incomingHash = deriveHash(pin, salt)
        return MessageDigest.isEqual(storedHash, incomingHash)
    }

    private fun resolveInitialState(): LockGateState {
        return if (isPinConfigured()) LockGateState.LOCKED else LockGateState.NEEDS_PIN_SETUP
    }

    private fun createPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            context.getSharedPreferences(FALLBACK_PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun deriveHash(pin: String, salt: ByteArray): ByteArray {
        val keySpec = PBEKeySpec(pin.toCharArray(), salt, 12000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(keySpec).encoded
    }

    companion object {
        private const val SECURE_PREF_NAME = "secure_lock_prefs"
        private const val FALLBACK_PREF_NAME = "fallback_lock_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_COOLDOWN_UNTIL = "cooldown_until"
        private const val KEY_LAST_INTERACTION_ELAPSED = "last_interaction_elapsed"
        private const val MAX_ATTEMPTS = 5
        private const val COOLDOWN_MS = 30_000L
        private const val SESSION_TIMEOUT_MS = 120_000L
    }
}

sealed interface UnlockResult {
    data object Success : UnlockResult
    data class Invalid(val attemptsRemaining: Int) : UnlockResult
    data class Cooldown(val secondsRemaining: Long) : UnlockResult
}
