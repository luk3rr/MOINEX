/*
 * Filename: AiCredentialStore.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.credentials

import org.json.JSONObject
import org.moinex.common.constant.Files
import org.moinex.model.enums.LlmProviderType
import org.springframework.stereotype.Service
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.nio.file.Files as NioFiles

/**
 * Stores the user's BYOK LLM provider API keys, encrypted at rest with a locally-generated
 * AES-GCM key.
 */
@Service
class AiCredentialStore(
    private val baseDir: Path = Paths.get(Files.MOINEX_DATA_DIR, "ai"),
) {
    companion object {
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val POSIX_VIEW = "posix"
        private const val PREF_KEY_SELECTED_PROVIDER = "ai.selectedProvider"
        private const val PREF_KEY_MODEL_PREFIX = "ai.model."

        private val prefs: Preferences = Preferences.userRoot().node("org.moinex.ai")
    }

    private val keyFile: Path get() = baseDir.resolve("keystore.key")
    private val credentialsFile: Path get() = baseDir.resolve("credentials.enc")

    var selectedProvider: LlmProviderType?
        get() =
            prefs[PREF_KEY_SELECTED_PROVIDER, null]?.let {
                runCatching { LlmProviderType.valueOf(it) }.getOrNull()
            }
        set(value) {
            if (value == null) {
                prefs.remove(PREF_KEY_SELECTED_PROVIDER)
            } else {
                prefs.put(PREF_KEY_SELECTED_PROVIDER, value.name)
            }
        }

    fun saveApiKey(
        provider: LlmProviderType,
        apiKey: String,
    ) {
        val current = loadDecrypted().toMutableMap()
        current[provider.name] = apiKey
        persist(current)
    }

    fun getApiKey(provider: LlmProviderType): String? = loadDecrypted()[provider.name]

    fun hasApiKey(provider: LlmProviderType): Boolean = !getApiKey(provider).isNullOrBlank()

    fun clearApiKey(provider: LlmProviderType) {
        val current = loadDecrypted().toMutableMap()
        if (current.remove(provider.name) != null) {
            persist(current)
        }
    }

    /**
     * The model id the user picked for [provider] (e.g. "gpt-4o-mini"), or null to use that
     * provider's own default. Not sensitive - stored as a plain preference, not encrypted.
     */
    fun getModel(provider: LlmProviderType): String? = prefs[PREF_KEY_MODEL_PREFIX + provider.name, null]

    fun saveModel(
        provider: LlmProviderType,
        model: String,
    ) {
        if (model.isBlank()) {
            prefs.remove(PREF_KEY_MODEL_PREFIX + provider.name)
        } else {
            prefs.put(PREF_KEY_MODEL_PREFIX + provider.name, model)
        }
    }

    private fun loadDecrypted(): Map<String, String> {
        if (!NioFiles.exists(credentialsFile)) return emptyMap()

        return runCatching {
            val ivAndCiphertext = NioFiles.readAllBytes(credentialsFile)
            val plaintext = decrypt(ivAndCiphertext, loadOrCreateKey())
            val json = JSONObject(String(plaintext, Charsets.UTF_8))
            json.keySet().associateWith { json.getString(it) }
        }.getOrElse { emptyMap() }
    }

    private fun persist(values: Map<String, String>) {
        NioFiles.createDirectories(baseDir)
        val plaintext = JSONObject(values).toString().toByteArray(Charsets.UTF_8)
        val ivAndCiphertext = encrypt(plaintext, loadOrCreateKey())
        NioFiles.write(credentialsFile, ivAndCiphertext)
        restrictPermissions(credentialsFile)
    }

    private fun loadOrCreateKey(): SecretKey {
        if (NioFiles.exists(keyFile)) {
            return SecretKeySpec(NioFiles.readAllBytes(keyFile), "AES")
        }

        val key = KeyGenerator.getInstance("AES").apply { init(KEY_SIZE_BITS) }.generateKey()
        NioFiles.createDirectories(baseDir)
        NioFiles.write(keyFile, key.encoded)
        restrictPermissions(keyFile)
        return key
    }

    private fun encrypt(
        plaintext: ByteArray,
        key: SecretKey,
    ): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher =
            Cipher.getInstance(AES_ALGORITHM).apply {
                init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
        return iv + cipher.doFinal(plaintext)
    }

    private fun decrypt(
        ivAndCiphertext: ByteArray,
        key: SecretKey,
    ): ByteArray {
        val iv = ivAndCiphertext.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = ivAndCiphertext.copyOfRange(GCM_IV_LENGTH_BYTES, ivAndCiphertext.size)
        val cipher =
            Cipher.getInstance(AES_ALGORITHM).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            }
        return cipher.doFinal(ciphertext)
    }

    private fun restrictPermissions(path: Path) {
        runCatching {
            if (FileSystems.getDefault().supportedFileAttributeViews().contains(POSIX_VIEW)) {
                NioFiles.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
            }
        }
    }
}
