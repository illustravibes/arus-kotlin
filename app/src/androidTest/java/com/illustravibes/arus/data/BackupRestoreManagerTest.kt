package com.illustravibes.arus.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.illustravibes.arus.domain.TransactionType
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreManagerTest {
    private val databaseName = "backup-restore-test.db"
    private val password = "secure backup passphrase"

    @Test
    fun failedPasswordKeepsLocalDataAndCorrectRestoreReplacesIt() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val backup = File(context.cacheDir, "backup-restore-test.arusbackup")
        val futureBackup = File(context.cacheDir, "future-schema.arusbackup")
        val futurePayload = File(context.cacheDir, "future-schema.payload")
        val testFiles = File(context.cacheDir, "backup-restore-files")
        context.deleteDatabase(databaseName)
        backup.delete()
        futureBackup.delete()
        futurePayload.delete()
        testFiles.deleteRecursively()
        val receipt = File(testFiles, "receipts/sample.jpg")
        check(receipt.parentFile?.mkdirs() == true)
        receipt.writeBytes(byteArrayOf(1, 2, 3, 4))

        var database = DatabaseFactory.open(context, databaseName)
        try {
            android.util.Log.i("BackupRestoreTest", "opened source database")
            val walletId = database.walletDao().insert(WalletEntity("Tunai", "CASH", 10_000, true, false, null))
            val categoryId = database.categoryDao().insert(CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0))
            val transactionId = database.transactionDao().insertOrdinary(
                TransactionEntity(TransactionType.EXPENSE, walletId, 500, null, categoryId, 10, "Makan", null, null),
                true,
            )
            database.attachmentDao().insert(AttachmentEntity("TRANSACTION", transactionId,
                "receipts/sample.jpg", "TOTAL 500", 11))
            BackupRestoreManager.createBackup(context, database, password.toCharArray(), backup,
                databaseName, testFiles)
            android.util.Log.i("BackupRestoreTest", "backup created")
            database.walletDao().insert(WalletEntity("Tambahan", "CASH", 20_000, true, false, null))

            try {
                BackupRestoreManager.restore(context, database, backup, "wrong password".toCharArray(),
                    databaseName, testFiles)
                throw AssertionError("Incorrect backup password must be rejected.")
            } catch (expected: IllegalArgumentException) { }
            android.util.Log.i("BackupRestoreTest", "wrong password rejected")
            assertEquals(2, database.walletDao().activeWalletCount())

            futurePayload.writeText("future database payload")
            EncryptedBackupContainer.encrypt(futurePayload, futureBackup, password.toCharArray(),
                ArusDatabase.CURRENT_VERSION + 1)
            try {
                BackupRestoreManager.restore(context, database, futureBackup, password.toCharArray(),
                    databaseName, testFiles)
                throw AssertionError("A backup from a newer schema must be rejected.")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("Perbarui aplikasi"))
            }
            android.util.Log.i("BackupRestoreTest", "newer schema rejected")
            assertEquals(2, database.walletDao().activeWalletCount())
            assertTrue(receipt.delete())

            database = BackupRestoreManager.restore(context, database, backup, password.toCharArray(),
                databaseName, testFiles)
            android.util.Log.i("BackupRestoreTest", "backup restored")
            assertEquals(1, database.walletDao().activeWalletCount())
            assertEquals("Tunai", database.walletDao().activeWallets().single().name)
            assertEquals(1, database.transactionDao().activeTransactions().size)
            assertEquals(1, database.attachmentDao().forOwner("TRANSACTION", transactionId).size)
            assertTrue(File(testFiles, "receipts/sample.jpg").isFile)
            database.close()
            database = DatabaseFactory.open(context, databaseName)
            assertEquals(1, database.walletDao().activeWalletCount())
            assertEquals(1, database.transactionDao().activeTransactions().size)
            assertEquals("TOTAL 500", database.attachmentDao().forOwner("TRANSACTION", transactionId).single().ocrRawText)
        } finally {
            runCatching { database.close() }
            context.deleteDatabase(databaseName)
            backup.delete()
            futureBackup.delete()
            futurePayload.delete()
            testFiles.deleteRecursively()
        }
    }
}
