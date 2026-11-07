package com.illustravibes.arus.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.DatabaseFactory
import com.illustravibes.arus.data.PresetSeeder
import com.illustravibes.arus.data.WalletEntity
import net.sqlcipher.database.SQLiteDatabase
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class TransactionQuickEntryTest {
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var database: ArusDatabase

    @Test
    fun secureFlagIsDisabledForDebugScreenshots() {
        composeRule.activityRule.scenario.onActivity { activity ->
            assert(activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE == 0)
        }
    }

    @Test
    fun receiptScanRouteOffersManualTransactionFallback() {
        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()
        composeRule.onNodeWithText("Scan struk").performClick()
        composeRule.onNodeWithText("Pilih foto struk").assertIsDisplayed()

        composeRule.onNodeWithText("Input manual").performClick()

        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Tambah transaksi").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun exportRouteOffersDateRangeAndAllSupportedFormats() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Ekspor data").performScrollTo().performClick()

        composeRule.onNodeWithText("Rentang tanggal · akhir inklusif").assertIsDisplayed()
        composeRule.onNodeWithText("CSV").assertExists()
        composeRule.onNodeWithText("Excel (.xlsx)").assertExists()
        composeRule.onNodeWithText("PDF").assertExists()
        composeRule.onNodeWithText("Simpan file ekspor").assertIsEnabled()
    }

    @Test
    fun recurringReminderSettingsShowPermissionStatusAndCanDisableReminder() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Transaksi rutin").performScrollTo().performClick()

        composeRule.onNodeWithText("Pengaturan reminder").assertIsDisplayed()
        composeRule.onNodeWithText("Occurrence rutin tertunda · aktif").assertIsDisplayed()
        val notificationsDisabled = composeRule.onAllNodesWithText("Izin notifikasi · nonaktif")
            .fetchSemanticsNodes().isNotEmpty()
        if (notificationsDisabled) {
            composeRule.onNodeWithText("Izin notifikasi · nonaktif").assertIsDisplayed()
            composeRule.onNodeWithText("Aktifkan izin notifikasi").assertExists()
        } else {
            composeRule.onNodeWithText("Izin notifikasi · aktif").assertIsDisplayed()
        }
        composeRule.onNodeWithText("Status dan bantuan izin notifikasi").performScrollTo().assertExists()
        composeRule.onNodeWithText("Buka pengaturan baterai").performScrollTo().assertExists()
        composeRule.onNodeWithText("Matikan reminder").performClick()
        composeRule.onNodeWithText("Occurrence rutin tertunda · nonaktif").assertIsDisplayed()
    }

    @Test
    fun generalSettingsPersistThemePeriodAndLocalFormatsAfterRestart() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Pengaturan").performScrollTo().performClick()
        composeRule.onNodeWithText("Gelap").performClick()
        composeRule.onNodeWithText("Ubah tanggal awal periode").performClick()
        composeRule.onNodeWithText("Tanggal 25").performClick()
        composeRule.onNodeWithText("ISO").performClick()
        composeRule.onNodeWithText("1,234").performClick()

        assertEquals("DARK", context.getSharedPreferences("arus_preferences", 0).getString("theme_mode", null))
        assertEquals(25, context.getSharedPreferences("arus_preferences", 0).getInt("period_start_day", 0))
        assertEquals("ISO", context.getSharedPreferences("arus_preferences", 0).getString("date_format", null))
        assertEquals("EN", context.getSharedPreferences("arus_preferences", 0).getString("number_format", null))

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Awal periode bulanan · tanggal 25").assertIsDisplayed()
        composeRule.onNodeWithText("Contoh · 2026-10-22").assertIsDisplayed()
        composeRule.onNodeWithText("Contoh · Rp 1,234").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun archivedWalletListOpensBalanceAndTransactionHistory() {
        val walletId = database.walletDao().insert(WalletEntity("Dompet Arsip Test", "CASH", 5_000, true, false, null))
        val categoryId = database.categoryDao().activeCategories("EXPENSE").first().id
        database.transactionDao().insertOrdinary(
            TransactionEntity(com.illustravibes.arus.domain.TransactionType.EXPENSE, walletId,
                500, null, categoryId, System.currentTimeMillis(), "Belanja arsip", null, null), true,
        )
        database.walletDao().archive(walletId)
        val archivedTransactions = database.transactionDao().activeTransactionsThrough(walletId, System.currentTimeMillis())
        assertEquals(1, archivedTransactions.size)
        assertEquals("Belanja arsip", archivedTransactions.single().note)

        composeRule.onNodeWithText("Lihat semua ›").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Terarsip (1)").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Terarsip (1)").performClick()
        composeRule.onNodeWithText("Dompet Arsip Test").performClick()

        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("RIWAYAT TRANSAKSI (1)").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Saldo saat ini", substring = true).assertExists()
        composeRule.onNodeWithText("Rp 4.500").assertExists()
        composeRule.onNodeWithText("Belanja arsip", substring = true).assertExists()
        composeRule.onNodeWithContentDescription("Kembali").performClick()
        composeRule.onNodeWithText("Aktif (", substring = true).performClick()
        composeRule.onNodeWithText("Dompet Arsip Test").assertDoesNotExist()
    }

    @Test
    fun reportCategoryOpensOnlyItsTransactionsForSelectedPeriod() {
        val wallet = database.walletDao().activeWallets().first()
        val category = database.categoryDao().activeCategories("EXPENSE").first()
        val now = System.currentTimeMillis()
        database.transactionDao().insertOrdinary(
            TransactionEntity(com.illustravibes.arus.domain.TransactionType.EXPENSE, wallet.id,
                1_500, null, category.id, now, "Detail kategori laporan", null, null), true,
        )

        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onAllNodesWithText("Laporan")[0].performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("${category.name} · 100%").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("${category.name} · 100%").performClick()

        composeRule.onNodeWithText(category.name).assertExists()
        composeRule.onNodeWithText("Detail kategori laporan", substring = true).assertExists()
        composeRule.onAllNodesWithText("Rp 1.500").assertCountEquals(2)
    }

    @Test
    fun cameraCaptureCanBeCroppedAndReadOnDevice() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant com.illustravibes.arus android.permission.CAMERA")
            .use { descriptor -> android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).close() }
        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()
        composeRule.onNodeWithText("Scan struk").performClick()
        composeRule.onNodeWithText("Kamera").performClick()
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText("Ambil foto").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Ambil foto").performClick()
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText("Geser sudut untuk memangkas struk.").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithContentDescription("Tambah potongan kanan").performClick()
        composeRule.onNodeWithText("Potongan kanan · 5%").assertExists()
        composeRule.onNodeWithText("Putar kanan").performClick()
        composeRule.onNodeWithText("Pangkas dan baca struk").performScrollTo().performClick()
        composeRule.waitUntil(30_000) { composeRule.onAllNodesWithText("Hasil OCR").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Foto disimpan sebagai lampiran tertunda. Periksa hasil sebelum menautkan ke transaksi.").assertExists()
        assertEquals(1, database.attachmentDao().pending().size)
        val pendingAttachment = database.attachmentDao().pending().single()
        assertTrue(database.transactionDao().activeTransactions().isEmpty())

        composeRule.onNodeWithText("Jadikan transaksi").performClick()
        composeRule.onNodeWithText("Konfirmasi hasil scan").assertIsDisplayed()
        composeRule.onNodeWithText("Lihat teks OCR").assertExists()
        assertTrue("OCR must not create a transaction before confirmation.",
            database.transactionDao().activeTransactions().isEmpty())
        composeRule.onNodeWithTag("transaction_amount_input").performTextClearance()
        composeRule.onNodeWithTag("transaction_amount_input").performTextInput("5000")
        composeRule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { database.transactionDao().activeTransactions().size == 1 }
        val savedTransaction = database.transactionDao().activeTransactions().single()
        assertEquals(0, database.attachmentDao().pending().size)
        assertEquals(1, database.attachmentDao().forOwner("TRANSACTION", savedTransaction.id).size)
        val confirmedMerchant = com.illustravibes.arus.domain.ReceiptOcrParser
            .parse(pendingAttachment.ocrRawText.orEmpty()).merchant
        if (confirmedMerchant != null) {
            assertEquals(savedTransaction.categoryId,
                database.merchantCategoryHintDao().suggestedCategoryId(
                    com.illustravibes.arus.domain.ReceiptOcrParser.normalizeMerchant(confirmedMerchant),
                ))
        }
    }

    @Test
    fun OCRCanStartGroupOrderWithUnassignedItemsAndPhoto() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant com.illustravibes.arus android.permission.CAMERA")
            .use { descriptor -> android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).close() }
        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()
        composeRule.onNodeWithText("Scan struk").performClick()
        composeRule.onNodeWithText("Kamera").performClick()
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText("Ambil foto").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Ambil foto").performClick()
        composeRule.waitUntil(15_000) { composeRule.onAllNodesWithText("Geser sudut untuk memangkas struk.").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Pangkas dan baca struk").performScrollTo().performClick()
        composeRule.waitUntil(30_000) { composeRule.onAllNodesWithText("Hasil OCR").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Jadikan Group Order").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Tambah item").fetchSemanticsNodes().isNotEmpty() }

        val draft = database.groupOrderDao().drafts().single()
        val items = database.groupOrderDao().items(draft.id)
        assertTrue(items.all { it.source == "OCR" && it.status == "UNASSIGNED" })
        assertEquals(1, database.attachmentDao().forOwner("GROUP_ORDER", draft.id).size)
        assertTrue(database.transactionDao().activeTransactions().isEmpty())
    }

    @get:Rule
    val testEnvironmentRule: TestRule = RuleChain.outerRule(object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = object : Statement() {
    override fun evaluate() {
                val context = ApplicationProvider.getApplicationContext<android.content.Context>()
                SQLiteDatabase.loadLibs(context)
                database = DatabaseFactory.open(context, "arus-ui-test.db")
                database.openHelper.writableDatabase.execSQL("DELETE FROM transactions")
                PresetSeeder.seed(database, context)
                if (database.categoryDao().activeCategories().none { it.kind == "EXPENSE" }) {
                    database.categoryDao().insert(CategoryEntity("Tes Pengeluaran", "EXPENSE", "receipt", 0xFF607D8B.toInt(), false, false, 0))
                }
                if (database.categoryDao().activeCategories().none { it.kind == "INCOME" }) {
                    database.categoryDao().insert(CategoryEntity("Tes Pemasukan", "INCOME", "payments", 0xFF607D8B.toInt(), false, false, 0))
                }
                if (database.walletDao().activeWalletCount() == 0) {
                    database.walletDao().insert(WalletEntity("Tes Tunai", "CASH", 100_000, true, false, null))
                }
                MainActivity.transactionTestDatabase = database
                val isOnboardingSmoke = description.methodName == "onboardingCreatesWalletAndCompletesSetup"
                context.getSharedPreferences("arus_setup", 0).edit().putBoolean("complete", !isOnboardingSmoke).commit()
                context.getSharedPreferences("arus_app_lock", 0).edit().clear().commit()
                context.getSharedPreferences("arus_reminders", 0).edit().clear().commit()
                context.getSharedPreferences("arus_preferences", 0).edit().clear().commit()
                try {
                    base.evaluate()
                } finally {
                    MainActivity.transactionTestDatabase = null
                    if (::database.isInitialized) database.close()
                    context.deleteDatabase("arus-ui-test.db")
                }
            }
        }
    }).around(composeRule)

    @Test
    fun switchesTypeSavesAndUndoRestoresBalanceAndHistory() {
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("− Pengeluaran").performClick()
        composeRule.onNodeWithText("Pengeluaran").assertIsDisplayed()
        composeRule.onNodeWithText("Pemasukan").performClick()
        composeRule.onNodeWithText("Tambah transaksi").assertIsDisplayed()
        composeRule.onNodeWithText("Pengeluaran").performClick()
        composeRule.onNodeWithTag("transaction_amount_input").performTextInput("100")
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Transaksi tersimpan").fetchSemanticsNodes().isNotEmpty() }
        val walletId = database.walletDao().activeWallets().first().id
        val initialBalance = database.walletDao().findById(walletId).initialBalanceRupiah
        val balanceAfterInsert = initialBalance + database.transactionDao().transactionEffect(walletId, System.currentTimeMillis())
        assert(balanceAfterInsert < initialBalance)
        composeRule.onNodeWithText("Urungkan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Pulihkan").fetchSemanticsNodes().isNotEmpty() }
        assert(database.transactionDao().activeTransactions().isEmpty())
        composeRule.onNodeWithText("Pulihkan").performClick()
        composeRule.waitUntil(10_000) { database.transactionDao().activeTransactions().isNotEmpty() }
        assert(database.walletDao().initialBalance(walletId)!! + database.transactionDao().transactionEffect(walletId, System.currentTimeMillis()) < initialBalance)
    }

    @Test
    fun transactionSplitEditorDialogOpensAndCanBeCancelled() {
        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()
        composeRule.onNodeWithText("Pengeluaran").performClick()
        composeRule.onNodeWithTag("transaction_amount_input").performTextInput("10000")
        composeRule.onNodeWithText("Pecah kategori").performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Bagi nominal transaksi ke beberapa kategori.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Bagi nominal transaksi ke beberapa kategori.").assertIsDisplayed()
        composeRule.onNodeWithText("Simpan split").assertIsDisplayed()
        composeRule.onAllNodesWithText("Batal")[1].performClick()
        composeRule.onNodeWithText("Pilih kategori ›").assertIsDisplayed()
        assertTrue(database.transactionDao().activeTransactions().isEmpty())
    }

    @Test
    fun transactionFlowOpensPickersAndTransferAndShowsPagedHistoryDetails() {
        database.walletDao().insert(WalletEntity("Tes Tujuan", "CASH", 10_000, true, false, null))
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("− Pengeluaran").performClick()
        composeRule.onNodeWithText("Pilih kategori ›").performClick()
        composeRule.onNodeWithText("Pilih kategori").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup").performClick()
        composeRule.onNodeWithText("Tes Tujuan").assertExists()
        composeRule.onNodeWithText("Batal").performClick()
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("⇄ Transfer").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Template").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Template").assertIsDisplayed()
        composeRule.onNodeWithText("Dompet asal dan tujuan harus berbeda.").assertDoesNotExist()
        composeRule.onNodeWithTag("transfer_amount_input").performTextInput("1000")
        composeRule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) {
            database.transactionDao().activeTransactions().count {
                it.type == com.illustravibes.arus.domain.TransactionType.TRANSFER_OUT ||
                    it.type == com.illustravibes.arus.domain.TransactionType.TRANSFER_IN
            } == 2 ||
                composeRule.onAllNodesWithText("Saldo asal tidak cukup, transfer akan membuat saldo minus. Tekan Simpan lagi untuk melanjutkan.").fetchSemanticsNodes().isNotEmpty()
        }
        val transferCount = database.transactionDao().activeTransactions().count {
            it.type == com.illustravibes.arus.domain.TransactionType.TRANSFER_OUT ||
                it.type == com.illustravibes.arus.domain.TransactionType.TRANSFER_IN
        }
        if (transferCount < 2) composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) {
            database.transactionDao().activeTransactions().count {
                it.type == com.illustravibes.arus.domain.TransactionType.TRANSFER_OUT ||
                    it.type == com.illustravibes.arus.domain.TransactionType.TRANSFER_IN
            } == 2
        }
        val transferRows = database.transactionDao().activeTransactions()
        assertEquals(setOf(com.illustravibes.arus.domain.TransactionType.TRANSFER_OUT, com.illustravibes.arus.domain.TransactionType.TRANSFER_IN), transferRows.map { it.type }.toSet())
        assert(transferRows.all { it.amountRupiah == 1_000L && it.transferGroupId == transferRows.first().transferGroupId })
    }

    @Test
    fun categoryManagementShowsListsAndCategoryForm() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Kategori").performScrollTo().performClick()
        composeRule.onNodeWithText("+ Tambah kategori").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Kategori baru").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Kembali").performClick()
        composeRule.onNodeWithText("+ Tambah kategori").assertIsDisplayed()
    }

    @Test
    fun categorySaveReturnsToListWithNewCategory() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Kategori").performScrollTo().performClick()
        composeRule.onNodeWithText("+ Tambah kategori").performClick()
        composeRule.onNodeWithTag("arus_text_input").performTextInput("Kategori Routing Test")
        composeRule.activityRule.scenario.onActivity { activity ->
            val manager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            manager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag("arus_search_input").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("arus_search_input").performTextInput("Kategori Routing Test")
        val categoryLabel = androidx.compose.ui.test.hasText("Kategori Routing Test") and !hasSetTextAction()
        composeRule.waitUntil(10_000) { composeRule.onAllNodes(categoryLabel).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNode(categoryLabel).assertIsDisplayed()
        assertEquals(1, database.categoryDao().allCategories().count { it.name == "Kategori Routing Test" })
    }

    @Test
    fun categoryDeleteMigratesTransactionsAndRefreshesList() {
        val sourceId = database.categoryDao().insert(CategoryEntity("Kategori Hapus Test", "EXPENSE", "tag", 0, false, false, 100))
        val targetId = database.categoryDao().insert(CategoryEntity("Kategori Tujuan Test", "EXPENSE", "tag", 0, false, false, -100))
        val wallet = database.walletDao().activeWallets().first()
        val transactionId = database.transactionDao().insertOrdinary(
            TransactionEntity(com.illustravibes.arus.domain.TransactionType.EXPENSE, wallet.id,
                500, null, sourceId, System.currentTimeMillis(), "Migrasi kategori", null, null), true,
        )
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Kategori").performScrollTo().performClick()
        composeRule.onNodeWithTag("arus_search_input").performTextInput("Kategori Hapus Test")
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Hapus").fetchSemanticsNodes().size == 1 }
        composeRule.activityRule.scenario.onActivity { activity ->
            val manager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            manager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Hapus").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Konfirmasi hapus").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Kategori Tujuan Test").performClick()
        composeRule.onNodeWithText("Konfirmasi hapus").performClick()
        composeRule.waitUntil(10_000) { database.categoryDao().findById(sourceId) == null }
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Kategori tidak ditemukan").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(targetId, database.transactionDao().activeTransactions().first { it.id == transactionId }.categoryId)
        assertEquals(targetId, database.transactionSplitDao().splitsForTransaction(transactionId).single().categoryId)
        assertEquals(500L, database.transactionSplitDao().splitTotal(transactionId))
    }

    @Test
    fun walletEditKeepsTargetAfterActivityRecreation() {
        val original = database.walletDao().activeWallets().first()
        val count = database.walletDao().activeWalletCount()
        composeRule.onNodeWithText("Lihat semua ›").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Edit").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText("Edit")[0].performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Edit dompet").fetchSemanticsNodes().isNotEmpty() }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Edit dompet").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("arus_text_input").performTextClearance()
        composeRule.onNodeWithTag("arus_text_input").performTextInput("Dompet Setelah Rotasi")
        composeRule.activityRule.scenario.onActivity { activity ->
            val manager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            manager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { database.walletDao().findById(original.id).name == "Dompet Setelah Rotasi" }
        assertEquals(count, database.walletDao().activeWalletCount())
    }

    @Test
    fun categoryEditKeepsTargetAfterActivityRecreation() {
        val id = database.categoryDao().insert(CategoryEntity("Kategori Sebelum Rotasi", "EXPENSE", "tag", 0, false, false, 100))
        val count = database.categoryDao().allCategories().size
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Kategori").performScrollTo().performClick()
        composeRule.onNodeWithTag("arus_search_input").performTextInput("Kategori Sebelum Rotasi")
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Edit").fetchSemanticsNodes().size == 1 }
        composeRule.onNodeWithText("Edit").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Edit kategori").fetchSemanticsNodes().isNotEmpty() }
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Edit kategori").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag("arus_text_input").performTextClearance()
        composeRule.onNodeWithTag("arus_text_input").performTextInput("Kategori Setelah Rotasi")
        composeRule.activityRule.scenario.onActivity { activity ->
            val manager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            manager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { database.categoryDao().findById(id).name == "Kategori Setelah Rotasi" }
        assertEquals(count, database.categoryDao().allCategories().size)
    }

    @Test
    fun budgetSaveRefreshesListAndShowsPersistedProgress() {
        composeRule.onNodeWithText("Anggaran").performClick()
        composeRule.onNodeWithText("+ Buat anggaran").performClick()
        composeRule.onNodeWithTag("arus_amount_input").performScrollTo().performTextInput("200000")
        composeRule.activityRule.scenario.onActivity { activity ->
            val manager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            manager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan anggaran").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Anggaran Total").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Anggaran Total").assertIsDisplayed()
        val saved = database.budgetDao().activeBudgets().single()
        assertEquals(200_000L, saved.amountRupiah)
        composeRule.onNodeWithText("Belum ada anggaran").assertDoesNotExist()
    }

    @Test
    fun appLockSettingsCanSetAndDisablePin() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Pengaturan keamanan").performClick()
        composeRule.onNodeWithText("Atur PIN").performClick()
        composeRule.onNodeWithTag("lock_pin_setup").performTextInput("123456")
        composeRule.onNodeWithText("Lanjut").performClick()
        composeRule.onNodeWithTag("lock_pin_setup").performTextInput("123456")
        composeRule.onNodeWithText("Aktifkan").performClick()
        assert(com.illustravibes.arus.ui.AppLock.isEnabled(ApplicationProvider.getApplicationContext()))
        assert(com.illustravibes.arus.ui.AppLock.verify(ApplicationProvider.getApplicationContext(), "123456"))

        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeRule.onNodeWithText("Arus terkunci").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_pin_entry").performTextInput("654321")
        composeRule.onNodeWithText("Buka").performClick()
        composeRule.onNodeWithText("PIN salah.").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_pin_entry").performTextInput("123456")
        composeRule.onNodeWithText("Buka").performClick()
        composeRule.onNodeWithText("Total kekayaan").assertIsDisplayed()

        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Pengaturan keamanan").performClick()
        composeRule.onNodeWithText("Matikan kunci").performClick()
        assert(!com.illustravibes.arus.ui.AppLock.isEnabled(ApplicationProvider.getApplicationContext()))
    }

    @Test
    fun biometricUnavailableFallsBackToPin() {
        AppLock.setPin(ApplicationProvider.getApplicationContext(), "123456")
        AppLock.setBiometricEnabled(ApplicationProvider.getApplicationContext(), true)
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeRule.onNodeWithText("Arus terkunci").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_pin_entry").performTextInput("123456")
        composeRule.onNodeWithText("Buka").performClick()
        composeRule.onNodeWithText("Total kekayaan").assertIsDisplayed()
        AppLock.clear(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun biometricPromptFailureKeepsPinFallbackAvailable() {
        AppLock.setPin(ApplicationProvider.getApplicationContext(), "123456")
        AppLock.setBiometricEnabled(ApplicationProvider.getApplicationContext(), true)
        org.junit.Assume.assumeFalse(
            "This test simulates a device without enrolled biometrics.",
            androidx.biometric.BiometricManager.from(ApplicationProvider.getApplicationContext())
                .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS,
        )
        val biometricFailure = java.util.concurrent.atomic.AtomicBoolean(false)
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.requestBiometric(onSuccess = { throw AssertionError("Biometric prompt must not succeed without enrollment.") }, onFailure = { biometricFailure.set(true) })
        }
        assert(biometricFailure.get())
        composeRule.waitForIdle()
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeRule.onNodeWithText("Arus terkunci").assertIsDisplayed()
        composeRule.onNodeWithTag("lock_pin_entry").performTextInput("123456")
        composeRule.onNodeWithText("Buka").performClick()
        composeRule.onNodeWithText("Total kekayaan").assertIsDisplayed()
        AppLock.clear(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun homeShowsNetWorthWalletsAndLatestTransactionsAndOpensDestinations() {
        composeRule.onNodeWithText("Total kekayaan").assertIsDisplayed()
        composeRule.onNodeWithText("AKTIVITAS TERBARU").assertIsDisplayed()
        composeRule.onNodeWithText("Lihat semua").assertDoesNotExist()
        composeRule.onNodeWithText("Beranda").performClick()
        composeRule.onNodeWithText("Total kekayaan").performClick()
        composeRule.onNodeWithText("Rincian").assertIsDisplayed()
        composeRule.onNodeWithText("Rincian Kekayaan Bersih").assertIsDisplayed()
        composeRule.onNodeWithText("Aset:", substring = true).assertExists()
        composeRule.onNodeWithText("Kewajiban Kartu Kredit:", substring = true).assertExists()
        composeRule.onNodeWithText("Tutup").performClick()
        composeRule.onNodeWithText("Transaksi").performClick()
        composeRule.onNodeWithText("Cari transaksi, catatan, merchant").assertIsDisplayed()
        composeRule.onNodeWithText("Semua kategori").assertExists()
        composeRule.onNodeWithText("Semua").assertExists()
        composeRule.onNodeWithText("Semua dompet").assertExists()
        composeRule.onNodeWithText("Cari transaksi, catatan, merchant").performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Bulan ini").assertExists()
        composeRule.onNodeWithText("Rekonsiliasi").performScrollTo().performClick()
        composeRule.onNodeWithText("Riwayat rekonsiliasi").assertIsDisplayed()
        composeRule.onNodeWithText("Belum ada checkpoint untuk dompet ini.").assertExists()
        composeRule.onNodeWithText("Kembali ke transaksi").performClick()
        composeRule.onNodeWithText("Cari transaksi, catatan, merchant").assertIsDisplayed()
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Transaksi rutin").assertExists()
        composeRule.onNodeWithText("Ekspor data").assertExists()
        composeRule.onNodeWithText("Utang-Piutang").assertExists()
        composeRule.onNodeWithText("Pengaturan").assertExists()
        composeRule.onAllNodesWithText("Anggaran").assertCountEquals(2)
        composeRule.onAllNodesWithText("Laporan").assertCountEquals(2)
        composeRule.onAllNodesWithText("Anggaran")[0].performClick()
        composeRule.onNodeWithText("+ Buat anggaran").assertIsDisplayed()
        composeRule.onNodeWithText("Nama Anggaran (opsional)").assertDoesNotExist()
        composeRule.onNodeWithText("Belum ada anggaran").assertExists()
        composeRule.onNodeWithText("+ Buat anggaran").performClick()
        composeRule.onNodeWithText("Cakupan Anggaran").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Kembali").performClick()
        composeRule.onNodeWithText("+ Buat anggaran").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Kembali").performClick()
        composeRule.onAllNodesWithText("Laporan")[0].performClick()
        composeRule.onNodeWithText("Selisih:", substring = true).assertExists()
        composeRule.onNodeWithText("‹ Kembali").performClick()
        composeRule.onNodeWithText("Beranda").performClick()
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("− Pengeluaran").performClick()
        composeRule.onNodeWithText("Batal").performClick()
    }

    @Test
    fun homeShowsEmptyStateAndIndonesianCurrencyAndDateFormats() {
        composeRule.onNodeWithText("Belum ada transaksi").assertIsDisplayed()
        composeRule.onNodeWithText("Catat transaksi pertamamu dengan tombol (+) di bawah.").assertIsDisplayed()
        assertEquals("Rp 12.345", formatRupiah(12_345))
    }

    @Test
    fun onboardingCreatesWalletAndCompletesSetup() {
        val initialWalletCount = database.walletDao().activeWalletCount()
        composeRule.onNodeWithText("Catat arus uangmu").assertIsDisplayed()
        composeRule.onNodeWithText("Pulihkan backup").assertIsDisplayed()
        composeRule.onNodeWithText("Pulihkan backup").performClick()
        composeRule.onNodeWithText("Buat backup").assertIsDisplayed()
        composeRule.onNodeWithText("‹ Kembali").performClick()
        composeRule.onNodeWithText("Catat arus uangmu").assertIsDisplayed()
        composeRule.onNodeWithText("Mulai").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Siapkan dompet pertama").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Tambah dompet lain?").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Tunai").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Dompet Smoke")
        composeRule.onNodeWithText(if (initialWalletCount == 0) "Lanjut" else "Lanjut tanpa tambah").performClick()
        composeRule.onNodeWithText("Pilih kategori bawaan").assertIsDisplayed()
        composeRule.onNodeWithText("Lewati keamanan untuk nanti").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Total kekayaan").fetchSemanticsNodes().isNotEmpty() }
        assert(database.walletDao().activeWallets().any { it.name == "Dompet Smoke" })
    }

    @Test
    fun walletReconciliationShowsRecordedActualAndSignedDifference() {
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("− Pengeluaran").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()
        composeRule.onNodeWithText("Cocokkan saldo").performClick()
        composeRule.onNodeWithTag("actual_balance_input").performTextInput("0")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Selisih Saldo").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Saldo di Arus · Rp 100.000").assertCountEquals(2)
        composeRule.onNodeWithText("Saldo Asli Aktual · Rp 0").assertExists()
        composeRule.onNodeWithText("Selisih Saldo").assertExists()
        composeRule.onNodeWithText("-Rp 100.000").assertExists()
        composeRule.onNodeWithText("Tak terlacak").performScrollTo().performClick()
        composeRule.onNodeWithText("Selesaikan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Saldo cocok: Rp 0").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun cashDenominationCountSetsActualBalanceAndSavesWithCheckpointWithoutLedgerTransaction() {
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("− Pengeluaran").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()
        composeRule.onNodeWithText("Cocokkan saldo").performClick()
        composeRule.onNodeWithText("Hitung pecahan uang tunai").performClick()
        composeRule.onNodeWithTag("cash_count_100000").performClick().performTextInput("1")
        composeRule.onNodeWithTag("cash_count_20000").performClick().performTextInput("1")
        composeRule.onNodeWithText("Total pecahan · Rp 120.000").assertExists()
        composeRule.activityRule.scenario.onActivity { activity ->
            val manager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            manager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reconciliation_dialog_content").performTouchInput { swipeUp() }
        composeRule.onNodeWithText("Gunakan total pecahan").performClick()
        composeRule.onNodeWithText("Selesaikan").performClick()

        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Saldo cocok: Rp 120.000").fetchSemanticsNodes().isNotEmpty() }
        val checkpoint = database.reconciliationCheckpointDao().checkpointsForWallet(
            database.walletDao().activeWallets().first().id,
        ).first()
        val counts = database.cashDenominationCountDao().forCheckpoint(checkpoint.id)
        assertEquals(2, counts.size)
        assertEquals(120_000L, counts.sumOf { it.denominationRupiah * it.pieceCount })
        val adjustments = database.transactionDao().activeTransactions()
        assertEquals(1, adjustments.size)
        assertEquals(com.illustravibes.arus.domain.TransactionType.ADJUSTMENT, adjustments.single().type)
        assertEquals(100_000L, database.walletDao().activeWallets().first().initialBalanceRupiah)
        assertEquals(120_000L, com.illustravibes.arus.data.BalanceRepository(database)
            .balance(database.walletDao().activeWallets().first().id, System.currentTimeMillis()))
    }

    @Test
    fun matchingMutationSavesCheckpointItemWithoutChangingLedger() {
        val wallet = database.walletDao().activeWallets().first()
        val categoryId = database.categoryDao().activeCategories("EXPENSE").first().id
        val transactionId = database.transactionDao().insertOrdinary(
            TransactionEntity(com.illustravibes.arus.domain.TransactionType.EXPENSE, wallet.id,
                1_000, null, categoryId, System.currentTimeMillis(), "Mutasi listrik", null, null), true,
        )
        val balance = com.illustravibes.arus.data.BalanceRepository(database)
            .balance(wallet.id, System.currentTimeMillis())!!

        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("− Pengeluaran").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tambah transaksi").performClick()
        composeRule.onNodeWithText("Cocokkan saldo").performClick()
        composeRule.onNodeWithTag("actual_balance_input").performTextInput(balance.toString())
        composeRule.onNodeWithText("Cek mutasi").performClick()
        composeRule.onNodeWithTag("mutation_match_$transactionId").performClick()
        composeRule.onNodeWithText("Simpan pemeriksaan").performClick()

        composeRule.waitUntil(10_000) {
            database.reconciliationCheckpointDao().checkpointsForWallet(wallet.id).isNotEmpty()
        }
        val checkpoint = database.reconciliationCheckpointDao().checkpointsForWallet(wallet.id).first()
        val item = database.reconciliationItemDao().itemsForCheckpoint(checkpoint.id).single()
        assertEquals(transactionId, item.transactionId)
        assertTrue(item.matched)
        assertEquals(1, database.transactionDao().activeTransactions().size)
        assertEquals(balance, com.illustravibes.arus.data.BalanceRepository(database)
            .balance(wallet.id, System.currentTimeMillis()))
    }

    @Test
    fun debtScreenRecordsInstallmentAndNetsOppositeDebts() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Utang-Piutang").performClick()
        composeRule.onNodeWithText("Tambah catatan utang").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Kontak Utang Test")
        composeRule.onAllNodesWithTag("arus_text_input")[1].performTextInput("1000")
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Piutang · Rp 1.000 · OPEN").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithText("Catat pelunasan").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("250")
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Piutang · Rp 750 · PARTIAL").fetchSemanticsNodes().isNotEmpty() }

        composeRule.onNodeWithText("Tambah catatan utang").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Kontak Utang Test")
        composeRule.onNodeWithText("Utang").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[1].performTextInput("100")
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Saling hapus").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Saling hapus").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Utang · Rp 0 · PAID").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun contactDetailsShowDirectionTotalsAndDebtHistory() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Utang-Piutang").performClick()
        composeRule.onNodeWithText("Tambah catatan utang").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Kontak Rincian")
        composeRule.onAllNodesWithTag("arus_text_input")[1].performTextInput("2500")
        composeRule.onAllNodesWithTag("arus_text_input")[2].performTextInput("Catatan rincian")
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Detail kontak").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Detail kontak").performClick()

        composeRule.onNodeWithText("Total piutang · Rp 2.500").assertExists()
        composeRule.onNodeWithText("Total utang · Rp 0").assertExists()
        composeRule.onNodeWithText("Catatan rincian").assertExists()
    }

    @Test
    fun groupOrderDraftFinalizesBalancedPaymentAndReceivable() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Group Order").performClick()
        composeRule.onNodeWithText("Buat draft").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextClearance()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Makan tim")
        composeRule.onAllNodesWithTag("arus_text_input")[1].performTextInput("Warung")
        composeRule.onNodeWithText("Makan tim").assertExists()
        composeRule.onNodeWithText("Warung").assertExists()
        composeRule.onNodeWithText(database.walletDao().activeWallets().first().name).performClick()
        composeRule.onNodeWithText(database.categoryDao().activeCategories().first { it.kind == "EXPENSE" }.name).performClick()
        composeRule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Lanjut · Peserta").performScrollTo().performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Tambah peserta").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Isi judul, dompet pembayar, dan kategori.").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("Draft tidak dapat diperbarui.").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue("Wizard did not advance to participant step.",
            composeRule.onAllNodesWithText("Tambah peserta").fetchSemanticsNodes().isNotEmpty())
        composeRule.waitUntil(10_000) { database.groupOrderDao().drafts().any { it.title == "Makan tim" } }

        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Peserta Group Test")
        composeRule.onNodeWithText("Tambah peserta").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Peserta Group Test").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Lanjut · Item").performScrollTo().performClick()

        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Makan")
        composeRule.onAllNodesWithTag("arus_text_input")[1].performTextInput("300")
        composeRule.onNodeWithText("Aku").performClick()
        composeRule.onNodeWithText("Peserta Group Test").performClick()
        composeRule.onNodeWithText("Tambah item").performClick()
        composeRule.onNodeWithText("Lanjut · Biaya").performScrollTo().performClick()

        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("Ongkir")
        composeRule.onAllNodesWithTag("arus_text_input")[1].performTextInput("100")
        composeRule.onNodeWithText("Tambah biaya").performClick()
        composeRule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Lanjut · Tinjau").performScrollTo().performClick()
        composeRule.onNodeWithText("Total · Rp 400").assertExists()
        composeRule.onNodeWithText("Finalisasi").assertIsEnabled()
        composeRule.onNodeWithText("Finalisasi").performScrollTo().performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("FINALIZED · Rp 400").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onAllNodesWithText("Makan tim", substring = false).assertCountEquals(1)
        composeRule.onNodeWithText("Peserta Group Test · Rp 200 · OPEN · sisa Rp 200").assertExists()
        composeRule.onNodeWithText("Bagikan tagihan").assertExists()

        composeRule.onNodeWithText("Catat pelunasan").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("100")
        composeRule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("Peserta Group Test · Rp 200 · PARTIAL · sisa Rp 100").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Catat pelunasan").performClick()
        composeRule.onAllNodesWithTag("arus_text_input")[0].performTextInput("100")
        composeRule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan").performClick()
        composeRule.waitUntil(10_000) { composeRule.onAllNodesWithText("SETTLED · Rp 400").fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText("Peserta Group Test · Rp 200 · PAID · sisa Rp 0").assertExists()

        val finalizedPayment = database.transactionDao().activeTransactions().single { it.type == com.illustravibes.arus.domain.TransactionType.EXPENSE }
        assertEquals(400L, finalizedPayment.amountRupiah)
        assertEquals(400L, database.transactionSplitDao().splitTotal(finalizedPayment.id))
        assertEquals(1, database.debtDao().activeDebts().count { it.source == "GROUP_ORDER" && it.principalRupiah == 200L })
    }

}
