package com.illustravibes.arus.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflinePersistenceTest {
    @Test
    fun localLedgerPersistsAcrossDatabaseReopen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "offline-persistence-test.db"
        context.deleteDatabase(databaseName)
        var database = DatabaseFactory.open(context, databaseName)
        try {
            database.walletDao().insert(WalletEntity("Tunai offline", "CASH", 12_000, true, false, null))
            database.close()

            database = DatabaseFactory.open(context, databaseName)
            assertEquals("Tunai offline", database.walletDao().activeWallets().single().name)
            assertEquals(12_000L, database.walletDao().activeWallets().single().initialBalanceRupiah)
        } finally {
            runCatching { database.close() }
            context.deleteDatabase(databaseName)
        }
    }
}
