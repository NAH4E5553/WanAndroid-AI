package com.personal.wanandroid.core.data.datasource

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.personal.wanandroid.core.network.session.SessionStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Opaque session payload encrypted with a non-exportable Android Keystore key. No passwords. */
@Singleton
internal class EncryptedSessionStorage @Inject constructor(@ApplicationContext context: Context) :
    SessionStorage {
    private val file = AtomicFile(File(context.noBackupFilesDir, "auth/session.v1"))
    private val keyStore get() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized
    override fun read(): String? {
        // openRead restores AtomicFile's backup after an interrupted write.
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        val bytes = file.openRead().use { stream ->
            val data = stream.readBytesLimited(MAX_BYTES)
            if (data.size < 30 || data[0] != 1.toByte()) throw IOException("Invalid session file")
            data
        }
        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IOException("Session key unavailable")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
        return cipher.doFinal(bytes.copyOfRange(13, bytes.size)).toString(Charsets.UTF_8)
    }

    @Synchronized
    override fun write(payload: String?) {
        if (payload == null) {
            // Destroy the key first so even a failed file deletion cannot restore old credentials.
            keyStore.deleteEntry(KEY_ALIAS)
            file.delete()
            if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                throw IOException("Session cleanup failed")
            }
            return
        }
        val bytes = payload.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES - 64)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        check(cipher.iv.size == 12)
        val encrypted = byteArrayOf(1) + cipher.iv + cipher.doFinal(bytes)
        val parent = requireNotNull(file.baseFile.parentFile)
        if (!parent.isDirectory &&
            !parent.mkdirs()
        ) {
            throw IOException("Session directory unavailable")
        }
        val output = file.startWrite()
        try {
            output.write(encrypted)
            file.finishWrite(output)
        } catch (failure: Exception) {
            file.failWrite(output)
            throw failure
        }
    }

    private fun encryptionKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build()
            )
        }.generateKey()
    }

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            if (output.size() + count > limit) throw IOException("Session file too large")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private companion object {
        const val KEY_ALIAS = "wanandroid.api.session.v1"
        const val MAX_BYTES = 262144
    }
}
