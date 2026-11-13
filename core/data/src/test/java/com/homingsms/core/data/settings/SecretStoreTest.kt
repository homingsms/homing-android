package com.homingsms.core.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.homingsms.core.data.secrets.PreferencesSecretStore
import com.homingsms.core.data.secrets.SecretCipher
import com.homingsms.core.data.secrets.SecretPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecretStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cipher = object : SecretCipher {
        override fun encrypt(plain: ByteArray) = plain.map { (it.toInt() xor KEY).toByte() }.toByteArray()

        override fun decrypt(data: ByteArray) = encrypt(data)
    }

    @Test
    fun storesOnlyEncryptedValues() {
        val preferences = context.getSharedPreferences("secrets-test", Context.MODE_PRIVATE)
        val store = PreferencesSecretStore(preferences, cipher)
        store.put("cloud.deviceToken", "hd_super_secret")
        assertEquals("hd_super_secret", store.get("cloud.deviceToken"))
        val raw = preferences.getString("cloud.deviceToken", null).orEmpty()
        assertFalse(raw.contains("super"))
        assertEquals("hd_super_secret", PreferencesSecretStore(preferences, cipher).get("cloud.deviceToken"))
        store.remove("cloud.deviceToken")
        assertNull(PreferencesSecretStore(preferences, cipher).get("cloud.deviceToken"))
    }

    @Test
    fun previewsNeverShowTheWholeSecret() {
        assertEquals("hk_live_abcd…", SecretPreview.of("hk_live_abcdefghijklmnop"))
        assertEquals("whsec_abcd…", SecretPreview.of("whsec_abcdefghijklmnop"))
        assertNull(SecretPreview.of(null))
    }

    private companion object {
        const val KEY = 0x5A
    }
}
