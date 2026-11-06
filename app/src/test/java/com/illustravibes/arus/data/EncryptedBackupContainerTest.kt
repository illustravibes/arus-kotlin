package com.illustravibes.arus.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedBackupContainerTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun backupContainerRoundTripsPayloadAndAuthenticatedSchema() {
        val source = temporaryFolder.newFile("payload.zip")
        val encrypted = temporaryFolder.newFile("backup.arusbackup")
        val restored = temporaryFolder.newFile("restored.zip")
        val payload = ByteArray(1024) { (it % 251).toByte() }
        source.writeBytes(payload)

        EncryptedBackupContainer.encrypt(source, encrypted, "correct horse battery".toCharArray(), 19)
        val header = EncryptedBackupContainer.decrypt(encrypted, restored,
            "correct horse battery".toCharArray())

        assertEquals(1, header.formatVersion)
        assertEquals(19, header.schemaVersion)
        assertTrue(payload.contentEquals(restored.readBytes()))
    }

    @Test
    fun incorrectPasswordDoesNotAcceptBackupContents() {
        val source = temporaryFolder.newFile("payload.zip")
        val encrypted = temporaryFolder.newFile("backup.arusbackup")
        val restored = temporaryFolder.newFile("restored.zip")
        source.writeText("database bytes")

        EncryptedBackupContainer.encrypt(source, encrypted, "correct horse battery".toCharArray(), 19)
        val failure = runCatching {
            EncryptedBackupContainer.decrypt(encrypted, restored, "wrong password".toCharArray())
        }.exceptionOrNull()

        assertTrue("Wrong password must fail authenticated decryption.", failure != null)
    }
}
