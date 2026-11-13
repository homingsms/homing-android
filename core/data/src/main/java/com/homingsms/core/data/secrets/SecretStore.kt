package com.homingsms.core.data.secrets

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecretStore {
    fun get(key: String): String?

    fun put(key: String, value: String)

    fun remove(key: String)

    companion object {
        const val DEVICE_TOKEN = "cloud.deviceToken"
        const val API_KEY_PEPPER = "apikeys.pepper"
        const val PASSPHRASE = "encryption.passphrase"

        fun webhookSecret(endpointId: String): String = "webhook.secret.$endpointId"
    }
}

interface SecretCipher {
    fun encrypt(plain: ByteArray): ByteArray

    fun decrypt(data: ByteArray): ByteArray
}

class PreferencesSecretStore(private val preferences: SharedPreferences, private val cipher: SecretCipher) : SecretStore {
    private val cache = ConcurrentHashMap<String, String>()

    override fun get(key: String): String? {
        cache[key]?.let { return it }
        val stored = preferences.getString(key, null) ?: return null
        return decrypt(stored)?.also { cache[key] = it }
    }

    private fun decrypt(stored: String): String? = try {
        cipher.decrypt(Base64.decode(stored, Base64.NO_WRAP)).decodeToString()
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    override fun put(key: String, value: String) {
        val encrypted = Base64.encodeToString(cipher.encrypt(value.encodeToByteArray()), Base64.NO_WRAP)
        preferences.edit(commit = true) { putString(key, encrypted) }
        cache[key] = value
    }

    override fun remove(key: String) {
        preferences.edit(commit = true) { remove(key) }
        cache.remove(key)
    }

    companion object {
        const val FILE_NAME = "homing_secrets"

        fun create(context: Context): PreferencesSecretStore = PreferencesSecretStore(
            context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
            AndroidKeystoreCipher()
        )
    }
}

class AndroidKeystoreCipher(private val alias: String = DEFAULT_ALIAS) : SecretCipher {
    override fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return cipher.iv + cipher.doFinal(plain)
    }

    override fun decrypt(data: ByteArray): ByteArray {
        if (data.size <= IV_LENGTH) throw GeneralSecurityException("Encrypted secret is too short")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, data, 0, IV_LENGTH))
        return cipher.doFinal(data, IV_LENGTH, data.size - IV_LENGTH)
    }

    @Synchronized
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val DEFAULT_ALIAS = "homing_secrets_v1"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_BITS = 128
        private const val KEY_BITS = 256
    }
}

object SecretPreview {
    private const val VISIBLE = 4

    fun of(secret: String?): String? {
        if (secret.isNullOrEmpty()) return null
        val prefixEnd = secret.indexOf('_', startIndex = secret.indexOf('_') + 1).takeIf { it > 0 }?.plus(1)
            ?: secret.indexOf('_').takeIf { it > 0 }?.plus(1)
            ?: 0
        return secret.take(prefixEnd) + secret.drop(prefixEnd).take(VISIBLE) + "…"
    }
}
