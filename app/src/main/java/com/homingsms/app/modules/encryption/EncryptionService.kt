package com.homingsms.app.modules.encryption

import com.homingsms.app.modules.encryption.providers.EncryptionProviderFactory

class EncryptionService() {
    suspend fun decrypt(encryptedText: String): String {
        val chunks = encryptedText.split('$')
        if (chunks.size < 3) {
            throw RuntimeException("Invalid encrypted data format")
        }

        val algorithm = chunks[1]
        val data = chunks.drop(2).joinToString("$")

        return EncryptionProviderFactory.create(algorithm).decrypt(data)
    }

    suspend fun encrypt(plainText: String): String {
        val algorithm = EncryptionProviderFactory.DEFAULT_ALGORITHM
        val data = EncryptionProviderFactory.create(algorithm).encrypt(plainText)
        return "\$$algorithm\$$data"
    }
}
