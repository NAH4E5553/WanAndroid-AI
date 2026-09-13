package com.personal.wanandroid.core.data.datasource

import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.network.session.SessionNotice
import com.personal.wanandroid.core.network.session.SessionStore
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class EncryptedSessionStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = EncryptedSessionStorage(context)
    private val file = File(context.noBackupFilesDir, "auth/session.v1")

    @Before fun before() {
        storage.write(null)
    }

    @After fun after() {
        storage.write(null)
    }

    @Test fun encryptedPayloadSurvivesNewStorageInstanceWithoutPlaintextOnDisk() {
        val fixture = "fixture-session-credential"
        storage.write(fixture)
        assertEquals(fixture, EncryptedSessionStorage(context).read())
        assertFalse(file.readBytes().toString(Charsets.UTF_8).contains(fixture))
        storage.write(null)
        assertNull(EncryptedSessionStorage(context).read())
        assertFalse(file.exists())
    }

    @Test fun tamperedCiphertextFailsClosedAndIsRemoved() {
        storage.write("fixture-session-credential")
        val bytes = file.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        file.writeBytes(bytes)
        val store = SessionStore(EncryptedSessionStorage(context))
        assertEquals(SessionNotice.STORAGE_ERROR, store.initialize().notice)
        assertNull(store.state.value.user)
        assertNull(storage.read())
    }
}
