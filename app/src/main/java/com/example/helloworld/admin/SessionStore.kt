package com.example.helloworld.admin

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the admin session cookie encrypted with an Android Keystore key. */
class SessionStore(context: Context) {
    companion object {
        private const val PREFS = "kfcc_admin_session"
        private const val COOKIE_KEY = "session_cookie"
        private const val KEY_ALIAS = "kfcc_admin_session_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .build())
        return generator.generateKey()
    }

    fun get(): String? {
        val encoded = prefs.getString(COOKIE_KEY, null) ?: return null
        return try {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            if (packed.size <= IV_LENGTH) return null
            val iv = packed.copyOfRange(0, IV_LENGTH)
            val ciphertext = packed.copyOfRange(IV_LENGTH, packed.size)
            Cipher.getInstance(TRANSFORMATION).run {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
                String(doFinal(ciphertext), StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun save(cookie: String) {
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, secretKey())
            }
            val encrypted = cipher.doFinal(cookie.toByteArray(StandardCharsets.UTF_8))
            val packed = cipher.iv + encrypted
            prefs.edit().putString(COOKIE_KEY, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
        } catch (_: Exception) {
            clear()
        }
    }

    fun clear() {
        prefs.edit().remove(COOKIE_KEY).apply()
    }
}
