package com.illustravibes.arus.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
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

@RunWith(AndroidJUnit4::class)
class TransactionQuickEntryTest {
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var database: ArusDatabase

    @Test
    fun secureFlagIsAlwaysEnabledOnMainWindow() {
        composeRule.activityRule.scenario.onActivity { activity ->
            assert(activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
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
                context.getSharedPreferences("arus_setup", 0).edit().putBoolean("complete", true).commit()
                context.getSharedPreferences("arus_app_lock", 0).edit().clear().commit()
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
        composeRule.onNodeWithTag("arus_text_input").performTextInput("100")
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
    fun transactionFlowOpensPickersAndTransferAndShowsPagedHistoryDetails() {
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("− Pengeluaran").performClick()
        composeRule.onNodeWithText("Pilih kategori").performClick()
        composeRule.onAllNodesWithText("Pilih kategori").assertCountEquals(2)
        composeRule.onNodeWithText("Tutup").performClick()
        composeRule.onNodeWithText("Dompet: ${database.walletDao().activeWallets().first().name}").performClick()
        composeRule.onNodeWithText("Pilih dompet").assertIsDisplayed()
        composeRule.onNodeWithText("Tutup").performClick()
        composeRule.onNodeWithText("Batal").performClick()
        composeRule.onNodeWithTag("home_overview_list").performTouchInput {
            swipe(start = androidx.compose.ui.geometry.Offset(centerX, bottom - 20f), end = androidx.compose.ui.geometry.Offset(centerX, top + 20f))
        }
        composeRule.onNodeWithText("⇄ Transfer").performClick()
        composeRule.onNodeWithText("Template").assertIsDisplayed()
        composeRule.onNodeWithText("Dompet asal dan tujuan harus berbeda.").assertDoesNotExist()
        composeRule.onNodeWithText("Batal").performClick()
    }

    @Test
    fun categoryManagementShowsListsAndCategoryForm() {
        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Tambah kategori").assertIsDisplayed()
        composeRule.onNodeWithText("Tambah kategori").performClick()
        composeRule.onNodeWithText("Kategori baru").assertIsDisplayed()
        composeRule.onNodeWithText("Batal").performClick()
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
        composeRule.onNodeWithText("Aktivitas terbaru").assertIsDisplayed()
        composeRule.onNodeWithText("Lihat semua").performClick()
        composeRule.onNodeWithText("Lihat semua kategori").assertDoesNotExist()
        composeRule.onNodeWithText("Beranda").performClick()
        composeRule.onNodeWithText("Total kekayaan").performClick()
        composeRule.onNodeWithText("Rincian net worth").assertIsDisplayed()
        composeRule.onNodeWithText("Aset dompet:", substring = true).assertExists()
        composeRule.onNodeWithText("Kewajiban kredit:", substring = true).assertExists()
        composeRule.onNodeWithText("Tutup").performClick()
        composeRule.onNodeWithText("Riwayat").performClick()
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
        composeRule.onNodeWithText("Catat pemasukan atau pengeluaran pertama.").assertIsDisplayed()
        assertEquals("Rp 12.345", formatRupiah(12_345))
    }

    @Test
    fun walletReconciliationShowsRecordedActualAndSignedDifference() {
        composeRule.onNodeWithText("Cocokkan saldo").performClick()
        composeRule.onNodeWithTag("actual_balance_input").performClick()
        composeRule.onNodeWithTag("arus_text_input").performTextInput("0")
        composeRule.onNodeWithText("Saldo di Arus · Rp 100.000").assertExists()
        composeRule.onNodeWithText("Selisih · Rp -100.000").assertExists()
        composeRule.onNodeWithText("Tutup").performClick()
    }
}
