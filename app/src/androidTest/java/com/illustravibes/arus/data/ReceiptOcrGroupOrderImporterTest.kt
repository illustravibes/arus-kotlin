package com.illustravibes.arus.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.illustravibes.arus.domain.ReceiptOcrParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReceiptOcrGroupOrderImporterTest {
    private lateinit var database: ArusDatabase
    private var walletId = 0L
    private var categoryId = 0L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ArusDatabase::class.java,
        ).build()
        walletId = database.walletDao().insert(WalletEntity("Tunai", "CASH", 100_000, true, false, null))
        categoryId = database.categoryDao().insert(
            CategoryEntity("Makan", "EXPENSE", "restaurant", 0, false, false, 0),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun parsedReceiptItemsStartUnassignedAndPhotoLinksToDraft() {
        val attachmentId = database.attachmentDao().insert(
            AttachmentEntity("PENDING", null, "receipts/order.jpg", "WARUNG BUDI", 10),
        )
        val review = ReceiptOcrParser.parse(
            """WARUNG BUDI
                |Nasi ayam 32.000
                |Es teh 8.000
                |TOTAL 40.000
            """.trimMargin(),
        )

        val orderId = ReceiptOcrGroupOrderImporter.createDraft(database, attachmentId, review,
            walletId, categoryId, 20)
        val orderItems = database.groupOrderDao().items(orderId)

        assertEquals("WARUNG BUDI", database.groupOrderDao().find(orderId).merchant)
        assertEquals(2, orderItems.size)
        assertTrue(orderItems.all { it.source == "OCR" && it.status == "UNASSIGNED" })
        assertEquals(0, database.transactionDao().activeTransactions().size)
        assertEquals(1, database.attachmentDao().forOwner("GROUP_ORDER", orderId).size)
    }
}
