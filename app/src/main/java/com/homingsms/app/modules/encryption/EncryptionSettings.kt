package com.homingsms.app.modules.encryption

class EncryptionSettings(private val passphraseProvider: () -> String?) {
    val passphrase: String?
        get() = passphraseProvider()?.takeIf { it.isNotEmpty() }
}
