package jp.co.soracom.qlm29hrtk.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class NtripCredentials(val username: String = "", val password: String = "")

interface SecureCredentialStore {
    fun load(): NtripCredentials
    fun save(credentials: NtripCredentials)
}

class AndroidSecureCredentialStore(context: Context) : SecureCredentialStore {
    private val preferences = context.applicationContext.getSharedPreferences("secure_credentials", Context.MODE_PRIVATE)

    override fun load(): NtripCredentials {
        val encoded = preferences.getString(CIPHERTEXT, null) ?: return NtripCredentials()
        val iv = preferences.getString(IV, null) ?: return NtripCredentials()
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.getDecoder().decode(iv)))
            val plaintext = cipher.doFinal(Base64.getDecoder().decode(encoded)).toString(Charsets.UTF_8)
            val separator = plaintext.indexOf('\u0000')
            if (separator < 0) NtripCredentials() else NtripCredentials(plaintext.substring(0, separator), plaintext.substring(separator + 1))
        }.getOrDefault(NtripCredentials())
    }

    override fun save(credentials: NtripCredentials) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val plaintext = "${credentials.username}\u0000${credentials.password}".toByteArray(Charsets.UTF_8)
        val encrypted = cipher.doFinal(plaintext)
        preferences.edit()
            .putString(CIPHERTEXT, Base64.getEncoder().encodeToString(encrypted))
            .putString(IV, Base64.getEncoder().encodeToString(cipher.iv))
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val KEY_ALIAS = "qlm29h_ntrip_credentials"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val CIPHERTEXT = "ciphertext"
        const val IV = "iv"
    }
}
