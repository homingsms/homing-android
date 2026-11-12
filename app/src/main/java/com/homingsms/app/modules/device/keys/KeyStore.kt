package com.homingsms.app.modules.device.keys

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey

data class KeyPairResult(
    val keyPair: KeyPair,
    val publicKeyBase64: String,
)

/**
 * Keystore-only E2E key management. Keys are generated with
 * [KeyGenParameterSpec]. Private keys never leave AndroidKeyStore and no
 * software keys exist anywhere.
 */
class KeyStore {

    fun generateKeyPair(alias: String): KeyPairResult {
        val keyPair = generateKeyPairInKeystore(alias)
        return KeyPairResult(
            keyPair,
            encodePublicKey(keyPair),
        )
    }

    fun getPrivateKey(alias: String): PrivateKey? {
        return getKeyFromKeystore(alias)
    }

    fun delete(alias: String) {
        deleteKeyFromKeystore(alias)
    }

    private fun encodePublicKey(keyPair: KeyPair): String {
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    private fun generateKeyPairInKeystore(alias: String): KeyPair {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT,
        )
            .setKeySize(2048)
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    private fun getKeyFromKeystore(alias: String): PrivateKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)
        return ks.getKey(alias, null) as? PrivateKey
    }

    private fun deleteKeyFromKeystore(alias: String) {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
        ks.load(null)
        ks.deleteEntry(alias)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
