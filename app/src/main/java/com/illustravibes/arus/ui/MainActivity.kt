package com.illustravibes.arus.ui

import com.illustravibes.arus.BuildConfig

import android.os.Bundle
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.illustravibes.arus.ui.designsystem.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.map
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import android.app.DatePickerDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.RecurringReminderScheduler
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.DatabaseFactory
import com.illustravibes.arus.data.DatabaseKeyStore
import com.illustravibes.arus.data.PresetSeeder
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.ReceiptOcrGroupOrderImporter
import com.illustravibes.arus.data.BalanceRepository
import com.illustravibes.arus.data.TransactionDao
import com.illustravibes.arus.data.TransactionSplitDraft
import com.illustravibes.arus.data.CashDenominationCountInput
import com.illustravibes.arus.data.ReconciliationItemInput
import com.illustravibes.arus.domain.TransactionDraft
import com.illustravibes.arus.domain.TransactionSplitCalculator
import com.illustravibes.arus.domain.TransactionType
import com.illustravibes.arus.domain.BalanceReconciliation
import com.illustravibes.arus.domain.ReceiptOcrParser
import com.illustravibes.arus.domain.ReceiptOcrReview
import com.illustravibes.arus.data.TransactionDao.InsufficientBalanceException
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusCheckbox
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusSegmentedButton
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import com.illustravibes.arus.ui.designsystem.ArusNavigationBar
import com.illustravibes.arus.ui.designsystem.ArusNavigationItem
import com.illustravibes.arus.ui.designsystem.ArusDropdownMenu
import com.illustravibes.arus.ui.designsystem.ArusDropdownMenuItem
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconCode
import com.illustravibes.arus.ui.designsystem.categoryIcon
import com.illustravibes.arus.ui.designsystem.ArusRadius
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusBottomNavigation
import com.illustravibes.arus.ui.screens.HomeScreen
import com.illustravibes.arus.ui.screens.QuickAddActionSheet
import com.illustravibes.arus.ui.screens.ReconciliationScreen
import com.illustravibes.arus.ui.screens.TransactionDetailScreen
import com.illustravibes.arus.ui.screens.TransactionFormScreen
import com.illustravibes.arus.ui.screens.TransactionHistoryScreen
import com.illustravibes.arus.ui.screens.TransferScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class MainActivity : FragmentActivity() {
    companion object {
        var transactionTestDatabase: ArusDatabase? = null
    }

    private var backgroundedAt = 0L
    private var unlockRequest: (() -> Unit)? = null
    private var hasStarted = false
    private var lockEnabledWhenStopped = false
    private var authenticationPromptActive = false

    override fun onStart() {
        super.onStart()
        if (hasStarted && backgroundedAt > 0 && lockEnabledWhenStopped) unlockRequest?.invoke()
        hasStarted = true
        backgroundedAt = 0L
    }

    override fun onStop() {
        lockEnabledWhenStopped = AppLock.isEnabled(this) && !authenticationPromptActive
        if (lockEnabledWhenStopped) backgroundedAt = SystemClock.elapsedRealtime()
        super.onStop()
    }

    internal fun requestBiometric(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val manager = BiometricManager.from(this)
        if (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
            onFailure()
            return
        }
        authenticationPromptActive = true
        val prompt = BiometricPrompt(this@MainActivity, ContextCompat.getMainExecutor(this@MainActivity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { authenticationPromptActive = false; onSuccess() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { authenticationPromptActive = false; onFailure() }
            override fun onAuthenticationFailed() { authenticationPromptActive = false; onFailure() }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Arus")
            .setSubtitle("Verifikasi biometrik atau gunakan PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Gunakan PIN")
            .build())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RecurringReminderScheduler.schedule(applicationContext)
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        setContent {
            val context = LocalContext.current
            val settingsPreferences = remember(context) {
                context.getSharedPreferences("arus_preferences", android.content.Context.MODE_PRIVATE)
            }
            var themeMode by remember { mutableStateOf(settingsPreferences.getString("theme_mode", "SYSTEM") ?: "SYSTEM") }
            var periodStartDay by remember { mutableStateOf(settingsPreferences.getInt("period_start_day", 1)) }
            var dateFormat by remember { mutableStateOf(settingsPreferences.getString("date_format", "LOCAL") ?: "LOCAL") }
            var numberFormat by remember { mutableStateOf(settingsPreferences.getString("number_format", "ID") ?: "ID") }
            ArusFormatting.update(numberFormat, dateFormat)
            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }
            ArusTheme(darkTheme = darkTheme) {
                val scope = rememberCoroutineScope()
                var locked by rememberSaveable { mutableStateOf(AppLock.isEnabled(context)) }
                var lockPin by rememberSaveable { mutableStateOf("") }
                var lockError by rememberSaveable { mutableStateOf<String?>(null) }
                var lockSettings by rememberSaveable { mutableStateOf(false) }
                var pinSetup by rememberSaveable { mutableStateOf(false) }
                var pinConfirm by rememberSaveable { mutableStateOf(false) }
                var pinFirst by rememberSaveable { mutableStateOf("") }
                var pinEntry by rememberSaveable { mutableStateOf("") }
                var biometricAvailable by remember { mutableStateOf(false) }
                val appContext = LocalContext.current
                LaunchedEffect(Unit) {
                    biometricAvailable = BiometricManager.from(appContext).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                    unlockRequest = {
                        if (AppLock.biometricEnabled(appContext)) {
                            locked = true
                            requestBiometric(onSuccess = { locked = false; lockPin = ""; lockError = null }, onFailure = { locked = true })
                        } else locked = true
                    }
                    if (AppLock.isEnabled(appContext) && backgroundedAt > 0) locked = true
                }
                var database by remember { mutableStateOf<ArusDatabase?>(null) }
                var onboardingDone by rememberSaveable { mutableStateOf(false) }
                var onboardingRestoreOpen by rememberSaveable { mutableStateOf(false) }
                var onboardingLoaded by remember { mutableStateOf(false) }
                var hasWallets by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    val prefs = context.getSharedPreferences("arus_setup", android.content.Context.MODE_PRIVATE)
                    try {
                         database = withContext(Dispatchers.IO) {
                             transactionTestDatabase ?: DatabaseFactory.open(context).also { PresetSeeder.seed(it, context) }
                        }
                        onboardingDone = prefs.getBoolean("complete", false)
                        hasWallets = withContext(Dispatchers.IO) { database?.walletDao()?.activeWalletCount() ?: 0 } > 0
                    } catch (error: Exception) {
                        android.util.Log.e("ArusStartup", "Secure database startup failed", error)
                    }
                    onboardingLoaded = true
                }
                if (!onboardingLoaded) {
                    Box(Modifier.fillMaxSize().background(ArusTheme.colors.background)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Memuat Arus…") } }
                    return@ArusTheme
                }
                if (locked) {
                    AppLockScreen(
                        pin = lockPin,
                        error = lockError,
                        onPinChange = { lockPin = it.filter(Char::isDigit).take(6); lockError = null },
                        biometricAvailable = biometricAvailable && AppLock.biometricEnabled(appContext),
                        onBiometric = { requestBiometric(onSuccess = { locked = false; lockPin = ""; lockError = null }, onFailure = { lockError = "Biometrik gagal. Masukkan PIN." }) },
                        onSubmit = {
                            if (AppLock.verify(appContext, lockPin)) { locked = false; lockPin = ""; lockError = null }
                            else { lockError = "PIN salah."; lockPin = "" }
                        },
                    )
                    return@ArusTheme
                }
                val activeDatabase = database
                if (activeDatabase != null && onboardingRestoreOpen) {
                    BackupRestoreManagement(
                        database = activeDatabase,
                        onDismiss = { onboardingRestoreOpen = false },
                        onDatabaseReplaced = { restored ->
                            database = restored
                            scope.launch {
                                val restoredHasWallets = withContext(Dispatchers.IO) {
                                    restored.walletDao().activeWalletCount() > 0
                                }
                                hasWallets = restoredHasWallets
                                if (restoredHasWallets) {
                                    context.getSharedPreferences("arus_setup", android.content.Context.MODE_PRIVATE)
                                        .edit().putBoolean("complete", true).apply()
                                    onboardingDone = true
                                    onboardingRestoreOpen = false
                                }
                            }
                        },
                    )
                    return@ArusTheme
                }
                if (!onboardingDone && activeDatabase == null) {
    Box(Modifier.fillMaxSize().background(ArusTheme.colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text("Arus belum bisa membuka penyimpanan aman.", style = ArusTheme.typography.title)
                            Text("Data tidak ditampilkan atau dibuat tanpa enkripsi.")
                            ArusButton(onClick = { onboardingLoaded = false }, kind = ArusButtonKind.Primary, modifier = Modifier.fillMaxWidth()) { Text("Coba lagi") }
                        }
                    }
                    return@ArusTheme
                }
                if (activeDatabase != null && (!onboardingDone || !hasWallets)) {
                    Onboarding(activeDatabase, context, onComplete = {
                        context.getSharedPreferences("arus_setup", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("complete", true).apply()
                        onboardingDone = true
                    }, onRestore = { onboardingRestoreOpen = true })
                    return@ArusTheme
                }
                if (activeDatabase == null) {
                    Box(Modifier.fillMaxSize().background(ArusTheme.colors.background).windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.Center) {
                        Text("Penyimpanan Arus belum tersedia.", style = ArusTheme.typography.title)
                    }
                    return@ArusTheme
                }
                var selectedDestination by rememberSaveable { mutableStateOf("Beranda") }
                var transactionType by rememberSaveable { mutableStateOf<String?>(null) }
                var transferOpen by rememberSaveable { mutableStateOf(false) }
                var quickAddOpen by rememberSaveable { mutableStateOf(false) }
                var reconciliationWallet by remember { mutableStateOf<WalletEntity?>(null) }
                var receiptReview by remember { mutableStateOf<ReceiptOcrReview?>(null) }
                var receiptAttachmentId by rememberSaveable { mutableStateOf<Long?>(null) }
                var groupOrderDraftToOpen by rememberSaveable { mutableStateOf<Long?>(null) }
                var undoTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
                var undoDeletedAt by rememberSaveable { mutableStateOf<Long?>(null) }
                var undoDeadline by rememberSaveable { mutableStateOf(0L) }
                var showUndo by rememberSaveable { mutableStateOf(false) }
                var historySelected by rememberSaveable { mutableStateOf<Long?>(null) }
                var checkpointWalletId by rememberSaveable { mutableStateOf<Long?>(null) }
                LaunchedEffect(undoTransactionId, undoDeadline) {
                    if (undoTransactionId != null) {
                        showUndo = true
                        delay((undoDeadline - System.currentTimeMillis()).coerceAtLeast(0L))
                        showUndo = false
                        undoTransactionId = null
                        undoDeletedAt = null
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(ArusTheme.colors.background)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (selectedDestination == "Backup & Restore") BackupRestoreManagement(
                                    database = activeDatabase,
                                    onDismiss = { selectedDestination = "Lainnya" },
                                    onDatabaseReplaced = { restored ->
                                        database = restored
                                        scope.launch {
                                            hasWallets = withContext(Dispatchers.IO) {
                                                restored.walletDao().activeWalletCount() > 0
                                            }
                                        }
                                    },
                                )
                                else if (selectedDestination == "Ekspor data") ExportDataManagement(
                                    database = activeDatabase,
                                    onDismiss = { selectedDestination = "Lainnya" },
                                )
                                else if (selectedDestination == "Scan Struk") ReceiptScanner(
                                    database = activeDatabase,
                                    onClose = { selectedDestination = "Beranda" },
                                    onManualEntry = { attachmentId, review ->
                                        selectedDestination = "Beranda"
                                        receiptAttachmentId = attachmentId
                                        receiptReview = review
                                        transactionType = "EXPENSE"
                                    },
                                    onConfirmTransaction = { attachmentId, review ->
                                        selectedDestination = "Beranda"
                                        receiptAttachmentId = attachmentId
                                        receiptReview = review
                                        transactionType = "EXPENSE"
                                    },
                                    onConfirmGroupOrder = { attachmentId, review ->
                                        scope.launch {
                                            runCatching { withContext(Dispatchers.IO) {
                                                val wallet = activeDatabase.walletDao().activeWallets().firstOrNull()
                                                    ?: error("Tambahkan dompet sebelum membuat group order.")
                                                val categories = activeDatabase.categoryDao().activeCategories("EXPENSE")
                                                val normalizedMerchant = review.merchant?.let(ReceiptOcrParser::normalizeMerchant)
                                                val hintedCategoryId = normalizedMerchant?.let {
                                                    activeDatabase.merchantCategoryHintDao().suggestedCategoryId(it)
                                                }
                                                val category = categories.firstOrNull { it.id == hintedCategoryId }
                                                    ?: categories.firstOrNull()
                                                    ?: error("Tambahkan kategori pengeluaran sebelum membuat group order.")
                                                val now = System.currentTimeMillis()
                                                ReceiptOcrGroupOrderImporter.createDraft(activeDatabase, attachmentId,
                                                    review, wallet.id, category.id, now)
                                            } }.onSuccess { orderId ->
                                                receiptReview = null
                                                receiptAttachmentId = null
                                                groupOrderDraftToOpen = orderId
                                                selectedDestination = "Group Order"
                                            }.onFailure { failure ->
                                                android.widget.Toast.makeText(context,
                                                    failure.message ?: "Group order dari struk tidak dapat dibuat.",
                                                    android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                )
                                else if (selectedDestination == "Group Order") GroupOrderManagement(
                                    activeDatabase,
                                    initialDraftId = groupOrderDraftToOpen,
                                    onInitialDraftConsumed = { groupOrderDraftToOpen = null },
                                )
                                else if (selectedDestination == "Utang-Piutang") DebtManagement(activeDatabase)
                                else if (selectedDestination == "Rutin") RecurringManagement(activeDatabase)
                                else if (selectedDestination == "Riwayat") TransactionHistoryScreen(
                                    database = activeDatabase,
                                    onSelect = { historySelected = it },
                                    onTransfer = { transferOpen = true },
                                    onCheckpoints = { selectedDestination = "Checkpoint" },
                                )
                                else if (selectedDestination == "Checkpoint") CheckpointHistory(activeDatabase, selectedWalletId = checkpointWalletId,
                                    onWalletSelected = { checkpointWalletId = it }, onBack = { selectedDestination = "Riwayat" })
                                else if (selectedDestination == "Laporan") ReportScreen(database = activeDatabase, periodStartDay = periodStartDay)
                                else if (selectedDestination == "Pengaturan") AppSettingsManagement(
                                    themeMode = themeMode,
                                    periodStartDay = periodStartDay,
                                    dateFormat = dateFormat,
                                    numberFormat = numberFormat,
                                    onThemeModeChange = { value ->
                                        themeMode = value
                                        settingsPreferences.edit().putString("theme_mode", value).apply()
                                    },
                                    onPeriodStartDayChange = { value ->
                                        periodStartDay = value
                                        settingsPreferences.edit().putInt("period_start_day", value).apply()
                                    },
                                    onDateFormatChange = { value ->
                                        dateFormat = value
                                        settingsPreferences.edit().putString("date_format", value).apply()
                                    },
                                    onNumberFormatChange = { value ->
                                        numberFormat = value
                                        settingsPreferences.edit().putString("number_format", value).apply()
                                    },
                                    onDismiss = { selectedDestination = "Lainnya" },
                                )
                                else if (selectedDestination == "Anggaran") BudgetManagement(database = activeDatabase)
                                else if (selectedDestination == "Dompet") WalletManagement(database = activeDatabase)
                                else if (selectedDestination == "Lainnya") {
                                     Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                                        Text("Lainnya", style = ArusTheme.typography.headline)
                                        ArusButton(onClick = { lockSettings = true }, kind = ArusButtonKind.Outline) { Text("Pengaturan keamanan") }
                                         ArusButton(onClick = { selectedDestination = "Anggaran" }, kind = ArusButtonKind.Outline) { Text("Anggaran") }
                                         ArusButton(onClick = { selectedDestination = "Laporan" }, kind = ArusButtonKind.Outline) { Text("Laporan") }
                                          ArusButton(onClick = { selectedDestination = "Rutin" }, kind = ArusButtonKind.Outline) { Text("Transaksi rutin") }
                                          ArusButton(onClick = { selectedDestination = "Utang-Piutang" }, kind = ArusButtonKind.Outline) { Text("Utang-Piutang") }
                                           ArusButton(onClick = { selectedDestination = "Group Order" }, kind = ArusButtonKind.Outline) { Text("Group Order") }
                                          Text("Kategori", style = ArusTheme.typography.headline)
                                           CategoryManagement(database = activeDatabase)
                                           ArusButton(onClick = { selectedDestination = "Pengaturan" }, kind = ArusButtonKind.Outline) { Text("Pengaturan") }
                                           ArusButton(onClick = { selectedDestination = "Ekspor data" }, kind = ArusButtonKind.Outline) { Text("Ekspor data") }
                                           ArusButton(onClick = { selectedDestination = "Backup & Restore" }, kind = ArusButtonKind.Outline) { Text("Backup & Restore") }
                                     }
                                }
                                else HomeScreen(
                                    database = activeDatabase,
                                    onNetWorthDetail = { },
                                    onWallets = { selectedDestination = "Dompet" },
                                    onWallet = { _ -> selectedDestination = "Dompet" },
                                    onTransaction = { historySelected = it },
                                    onExpense = { transactionType = "EXPENSE" },
                                    onIncome = { transactionType = "INCOME" },
                                    onTransfer = { transferOpen = true },
                                    onScanReceipt = { selectedDestination = "Scan Struk" },
                                    onReconciliation = { reconciliationWallet = it },
                                )
                            if (showUndo && undoTransactionId != null) {
                                androidx.compose.foundation.layout.Row(
                                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(ArusTheme.colors.surface).padding(ArusSpacing.Medium),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Transaksi tersimpan", modifier = Modifier.weight(1f))
                                    ArusButton(onClick = {
                                        val id = undoTransactionId ?: return@ArusButton
                                        scope.launch {
                                            val deletedAt = System.currentTimeMillis()
                                            val deleted = withContext(Dispatchers.IO) { activeDatabase.transactionDao().softDeleteOrdinaryTrackingCheckpoint(id, deletedAt) }
                                            if (deleted == 1) { undoDeletedAt = deletedAt; undoDeadline = System.currentTimeMillis() + 5_000 }
                                        }
                                    }, kind = ArusButtonKind.Ghost) { Text("Urungkan") }
                                    if (undoDeletedAt != null) ArusButton(onClick = {
                                        val id = undoTransactionId ?: return@ArusButton
                                        val deletedAt = undoDeletedAt ?: return@ArusButton
                                        scope.launch {
                                            val restored = withContext(Dispatchers.IO) { activeDatabase.transactionDao().restoreOrdinary(id, deletedAt) }
                                            if (restored == 1) { showUndo = false; undoTransactionId = null; undoDeletedAt = null }
                                        }
                                    }, kind = ArusButtonKind.Ghost) { Text("Pulihkan") }
                                }
                            }
                        }
                        ArusBottomNavigation(
                            selectedTab = if (selectedDestination == "Riwayat") "Transaksi" else selectedDestination,
                            onTabSelected = { destination ->
                                selectedDestination = if (destination == "Transaksi") "Riwayat" else destination
                            },
                            onFabClick = {
                                quickAddOpen = true
                            },
                        )
                    }
                }
                if (quickAddOpen) {
                    QuickAddActionSheet(
                        onDismiss = { quickAddOpen = false },
                        onExpense = {
                            quickAddOpen = false
                            transactionType = "EXPENSE"
                        },
                        onIncome = {
                            quickAddOpen = false
                            transactionType = "INCOME"
                        },
                        onTransfer = {
                            quickAddOpen = false
                            transferOpen = true
                        },
                        onScanReceipt = {
                            quickAddOpen = false
                            selectedDestination = "Scan Struk"
                        },
                        onGroupOrder = {
                            quickAddOpen = false
                            selectedDestination = "Group Order"
                        },
                        onReconciliation = {
                            quickAddOpen = false
                            scope.launch {
                                val wallet = withContext(Dispatchers.IO) {
                                    activeDatabase.walletDao().activeWallets().firstOrNull()
                                }
                                if (wallet != null) reconciliationWallet = wallet
                                else selectedDestination = "Dompet"
                            }
                        },
                    )
                }
                transactionType?.let { type ->
                    TransactionFormScreen(
                        database = activeDatabase,
                        initialType = TransactionType.valueOf(type),
                        onDismiss = { transactionType = null; receiptReview = null; receiptAttachmentId = null },
                        onTypeChange = { transactionType = it.name },
                        initialReceipt = receiptReview,
                        receiptAttachmentId = receiptAttachmentId,
                        onSaved = { id: Long, dismiss: Boolean ->
                            undoTransactionId = id
                            undoDeletedAt = null
                            undoDeadline = System.currentTimeMillis() + 5_000
                            receiptReview = null
                            receiptAttachmentId = null
                            if (dismiss) transactionType = null
                        },
                    )
                }
                if (transferOpen) TransferScreen(activeDatabase, onDismiss = { transferOpen = false })
                if (selectedDestination == "Anggaran" || selectedDestination == "Laporan") {
                    ArusButton(onClick = { selectedDestination = "Lainnya" }, kind = ArusButtonKind.Outline) { Text("‹ Kembali") }
                }
                historySelected?.let { id -> TransactionDetailScreen(activeDatabase, id, onDismiss = { historySelected = null }) }
                reconciliationWallet?.let { wallet ->
                    var recordedBalance by remember(wallet.id) { mutableStateOf(wallet.initialBalanceRupiah) }
                    LaunchedEffect(wallet.id) {
                        recordedBalance = withContext(Dispatchers.IO) {
                            BalanceRepository(activeDatabase).balance(wallet.id, System.currentTimeMillis()) ?: wallet.initialBalanceRupiah
                        }
                    }
                    ReconciliationScreen(
                        database = activeDatabase,
                        wallet = wallet,
                        recordedBalance = recordedBalance,
                        onDismiss = { reconciliationWallet = null },
                        onSuccess = { _ -> },
                    )
                }
                if (lockSettings) AppLockSettings(
                    enabled = AppLock.isEnabled(appContext),
                    biometricEnabled = AppLock.biometricEnabled(appContext),
                    biometricAvailable = biometricAvailable,
                    onDismiss = { lockSettings = false; pinSetup = false; pinConfirm = false; pinEntry = ""; pinFirst = "" },
                    onSetup = { pinSetup = true; pinConfirm = false; pinEntry = ""; pinFirst = "" },
                    onDisable = { AppLock.clear(appContext); lockSettings = false },
                    onBiometric = { enabled ->
                        if (enabled) requestBiometric(
                            onSuccess = { AppLock.setBiometricEnabled(appContext, true); lockError = null },
                            onFailure = { AppLock.setBiometricEnabled(appContext, false); lockError = "Biometrik gagal. PIN tetap aktif." },
                        ) else AppLock.setBiometricEnabled(appContext, false)
                    },
                )
                if (pinSetup) PinSetupDialog(
                    confirm = pinConfirm,
                    pin = pinEntry,
                    error = lockError,
                    onPinChange = { pinEntry = it.filter(Char::isDigit).take(6); lockError = null },
                    onCancel = { pinSetup = false; pinConfirm = false; lockError = null },
                    onSubmit = {
                        if (pinEntry.length != 6) lockError = "PIN harus 6 digit."
                        else if (!pinConfirm) { pinFirst = pinEntry; pinEntry = ""; pinConfirm = true }
                        else if (pinEntry != pinFirst) { lockError = "PIN tidak sama. Ulangi."; pinEntry = ""; pinFirst = ""; pinConfirm = false }
                        else { AppLock.setPin(appContext, pinEntry); pinSetup = false; pinConfirm = false; lockSettings = false; lockError = null }
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeOverview(
    database: ArusDatabase,
    onNetWorth: () -> Unit,
    onWallets: () -> Unit,
    onWallet: (Long) -> Unit,
    onTransaction: (Long) -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
    onScanReceipt: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val wallets = remember { mutableStateListOf<WalletEntity>() }
    val walletBalances = remember { androidx.compose.runtime.mutableStateMapOf<Long, Long>() }
    val latest = remember { mutableStateListOf<TransactionEntity>() }
    var summary by remember { mutableStateOf<BalanceRepository.FinancialSummary?>(null) }
    var detailOpen by rememberSaveable { mutableStateOf(false) }
    var reconciliationWallet by remember { mutableStateOf<WalletEntity?>(null) }
    var actualBalance by rememberSaveable { mutableStateOf("") }
    var reconciliationError by remember { mutableStateOf<String?>(null) }
    var resolutionKind by rememberSaveable { mutableStateOf("UNTRACKED") }
    var resolutionParts by remember { mutableStateOf(listOf<TransactionDao.ReconciliationPart>()) }
    var cashCountOpen by rememberSaveable { mutableStateOf(false) }
    var denominationCounts by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var mutationCheckOpen by rememberSaveable { mutableStateOf(false) }
    var mutationTransactions by remember { mutableStateOf(emptyList<TransactionEntity>()) }
    var matchedTransactionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val cashDenominations = remember { listOf(100_000L, 50_000L, 20_000L, 10_000L, 5_000L, 2_000L, 1_000L, 500L, 200L, 100L) }
    LaunchedEffect(database) {
        val snapshot = withContext(Dispatchers.IO) {
            val activeWallets = database.walletDao().activeWallets()
            val balances = activeWallets.associate { wallet -> wallet.id to (BalanceRepository(database).balance(wallet.id, System.currentTimeMillis()) ?: 0L) }
            Triple(activeWallets, database.transactionDao().latestTransactions(), BalanceRepository(database).financialSummary(System.currentTimeMillis())) to balances
        }
        wallets.clear(); wallets.addAll(snapshot.first.first)
        latest.clear(); latest.addAll(snapshot.first.second)
        summary = snapshot.first.third
        walletBalances.clear(); walletBalances.putAll(snapshot.second)
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("home_overview_list"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Arus", style = ArusTheme.typography.headline)
                ArusCard(modifier = Modifier.fillMaxWidth(), onClick = { detailOpen = true }, contentPadding = 20.dp) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Total kekayaan", style = ArusTheme.typography.body.copy(color = ArusTheme.colors.mutedForeground))
                        Text(formatRupiah(summary?.netWorthRupiah ?: 0L), style = ArusTheme.typography.display)
                        Text("Aset ${formatRupiah(summary?.assetsRupiah ?: 0L)} · Kewajiban ${formatRupiah((summary?.creditLiabilitiesRupiah ?: 0L) + (summary?.debtLiabilitiesRupiah ?: 0L))}", style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground))
                    }
                }
            }
        }
        if (wallets.isEmpty()) item {
            ArusCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(ArusSpacing.Large), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("Belum ada dompet", style = ArusTheme.typography.title)
                    Text("Tambahkan dompet untuk mulai melihat saldo dan ringkasan.")
                    ArusButton(onClick = onWallets) { Text("Tambah dompet") }
                }
            }
        }
        item {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dompet", style = ArusTheme.typography.title, modifier = Modifier.weight(1f))
                ArusButton(onClick = onWallets, kind = ArusButtonKind.Ghost) { Text("Lihat semua") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                items(wallets, key = { it.id }) { wallet ->
                    ArusCard(modifier = Modifier.width(188.dp), onClick = { onWallet(wallet.id) }, contentPadding = 14.dp) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(wallet.name, style = ArusTheme.typography.body)
                            Text(wallet.type, style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground))
                            Text(formatRupiah(walletBalances[wallet.id] ?: 0L), style = ArusTheme.typography.title)
                            ArusButton(onClick = {
                                reconciliationWallet = wallet
                                actualBalance = ""
                                reconciliationError = null
                                resolutionParts = emptyList()
                                resolutionKind = "UNTRACKED"
                                cashCountOpen = false
                                denominationCounts = emptyMap()
                                mutationCheckOpen = false
                                mutationTransactions = emptyList()
                                matchedTransactionIds = emptySet()
                            }, kind = ArusButtonKind.Ghost) { Text("Cocokkan saldo") }
                        }
                    }
                }
            }
        }
        item {
            Text("Aktivitas terbaru", style = ArusTheme.typography.title)
        }
        if (latest.isEmpty()) item {
            ArusCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(ArusSpacing.Large), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("Belum ada transaksi", style = ArusTheme.typography.title)
                    Text("Catat pemasukan atau pengeluaran pertama.")
                }
            }
        }
        items(latest, key = { it.id }) { transaction ->
            ArusCard(modifier = Modifier.fillMaxWidth(), onClick = { onTransaction(transaction.id) }, contentPadding = 14.dp) {
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(transaction.merchant?.takeIf(String::isNotBlank) ?: transaction.note?.takeIf(String::isNotBlank) ?: transaction.type.name)
                        Text("${transaction.type} · ${ArusFormatting.formatDate(java.time.Instant.ofEpochMilli(transaction.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate())}", style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground))
                    }
                    Text(formatRupiah(transaction.amountRupiah), style = ArusTheme.typography.title)
                }
            }
        }
        item {
            Text("Aksi cepat", style = ArusTheme.typography.title)
        }
        item {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArusButton(onClick = onExpense, modifier = Modifier.weight(1f)) { Text("− Pengeluaran") }
                ArusButton(onClick = onIncome, modifier = Modifier.weight(1f)) { Text("+ Pemasukan") }
                ArusButton(onClick = onTransfer, modifier = Modifier.weight(1f)) { Text("⇄ Transfer") }
            }
        }
        item {
            OutlinedArusButton(onClick = onScanReceipt, modifier = Modifier.fillMaxWidth()) { Text("Scan struk") }
        }
    }
    if (detailOpen) ArusDialog(onDismissRequest = { detailOpen = false }, title = { Text("Rincian net worth") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aset dompet: ${formatRupiah(summary?.assetsRupiah ?: 0L)}")
            Text("Kewajiban kredit: ${formatRupiah(summary?.creditLiabilitiesRupiah ?: 0L)}")
            Text("Utang berjalan: ${formatRupiah(summary?.debtLiabilitiesRupiah ?: 0L)}")
            Text("Net worth: ${formatRupiah(summary?.netWorthRupiah ?: 0L)}")
        }
    }, confirmButton = { OutlinedArusButton(onClick = { detailOpen = false }) { Text("Tutup") } })
    reconciliationWallet?.let { wallet ->
        val recorded = walletBalances[wallet.id] ?: 0L
        ReconciliationScreen(
            database = database,
            wallet = wallet,
            recordedBalance = recorded,
            onDismiss = { reconciliationWallet = null },
            onSuccess = { actual ->
                walletBalances[wallet.id] = actual
                scope.launch {
                    latest.clear()
                    latest.addAll(withContext(Dispatchers.IO) { database.transactionDao().latestTransactions() })
                }
            },
        )
    }
}

internal fun formatRupiah(amount: Long): String = ArusFormatting.formatRupiah(amount)

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TransactionHistory(database: ArusDatabase, onSelect: (Long) -> Unit, onTransfer: () -> Unit, onCheckpoints: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var walletId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transactionType by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var minimumAmount by rememberSaveable { mutableStateOf("") }
    var maximumAmount by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var quickRange by rememberSaveable { mutableStateOf("") }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }
    var walletPickerOpen by rememberSaveable { mutableStateOf(false) }
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }
    var transactionTypeOpen by rememberSaveable { mutableStateOf(false) }
    var wallets by remember { mutableStateOf(emptyList<WalletEntity>()) }
    var categories by remember { mutableStateOf(emptyList<CategoryEntity>()) }
    LaunchedEffect(database) {
        val filters = withContext(Dispatchers.IO) {
            database.walletDao().allWallets() to database.categoryDao().activeCategories()
        }
        wallets = filters.first
        categories = filters.second
    }
    if (walletPickerOpen) {
        ArusDialog(
            onDismissRequest = { walletPickerOpen = false },
            title = { Text("Pilih dompet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    wallets.forEach { wallet ->
                        ArusButton(onClick = { walletId = wallet.id; walletPickerOpen = false }, selected = walletId == wallet.id) {
                            Text(wallet.name)
                        }
                    }
                }
            },
            confirmButton = { ArusButton(onClick = { walletPickerOpen = false }) { Text("Tutup") } },
        )
    }
    if (categoryPickerOpen) {
        ArusDialog(
            onDismissRequest = { categoryPickerOpen = false },
            title = { Text("Pilih kategori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArusButton(onClick = { categoryId = null; categoryPickerOpen = false }, selected = categoryId == null) {
                        Text("Semua kategori")
                    }
                    categories.filterNot { it.isSystem }.forEach { category ->
                        ArusButton(onClick = { categoryId = category.id; categoryPickerOpen = false }, selected = categoryId == category.id) {
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = { ArusButton(onClick = { categoryPickerOpen = false }) { Text("Tutup") } },
        )
    }
    if (transactionTypeOpen) {
        ArusDialog(
            onDismissRequest = { transactionTypeOpen = false },
            title = { Text("Pilih jenis") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to "Semua", "EXPENSE" to "Pengeluaran", "INCOME" to "Pemasukan", "TRANSFER_OUT" to "Transfer").forEach { (value, label) ->
                        ArusButton(onClick = { transactionType = value; transactionTypeOpen = false }, selected = transactionType == value) {
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { ArusButton(onClick = { transactionTypeOpen = false }) { Text("Tutup") } },
        )
    }
    val today = LocalDate.now()
    val rangeStartDate = when (quickRange) {
        "Bulan ini" -> today.withDayOfMonth(1)
        "Tahun ini" -> today.withDayOfYear(1)
        else -> startDate.toLocalDateOrNull()
    }
    val rangeEndDate = when (quickRange) {
        "Bulan ini", "Tahun ini" -> today.plusDays(1)
        else -> endDate.toLocalDateOrNull()?.plusDays(1)
    }
    val start = rangeStartDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val end = rangeEndDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val minimum = minimumAmount.toLongOrNull()
    val maximum = maximumAmount.toLongOrNull()
    LaunchedEffect(startDate, endDate, quickRange, minimumAmount, maximumAmount, start, end, minimum, maximum) {
        dateError = when {
            quickRange.isBlank() && startDate.isNotBlank() && startDate.toLocalDateOrNull() == null -> "Tanggal awal tidak valid. Gunakan YYYY-MM-DD."
            quickRange.isBlank() && endDate.isNotBlank() && endDate.toLocalDateOrNull() == null -> "Tanggal akhir tidak valid. Gunakan YYYY-MM-DD."
            start != null && end != null && start >= end -> "Tanggal awal harus sebelum tanggal akhir."
            minimumAmount.isNotBlank() && minimum == null -> "Nominal minimum tidak valid."
            maximumAmount.isNotBlank() && maximum == null -> "Nominal maksimum tidak valid."
            minimum != null && maximum != null && minimum > maximum -> "Nominal minimum harus di bawah nominal maksimum."
            else -> null
        }
    }
    val filterIsValid = (quickRange.isNotBlank() || startDate.isBlank() || start != null) &&
        (quickRange.isNotBlank() || endDate.isBlank() || end != null) &&
        !(start != null && end != null && start >= end) && (minimumAmount.isBlank() || minimum != null) &&
        (maximumAmount.isBlank() || maximum != null) && !(minimum != null && maximum != null && minimum > maximum)
    val pager = remember(database, query, walletId, transactionType, categoryId, minimum, maximum, start, end, filterIsValid) {
        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
            database.transactionDao().searchTransactionHistory(query.trim(), walletId, transactionType.ifBlank { null },
                categoryId, minimum, maximum, if (filterIsValid) start else Long.MAX_VALUE,
                if (filterIsValid) end else Long.MIN_VALUE)
        }
    }
    val transactions = pager.flow.collectAsLazyPagingItems()
    val resultTotal = remember(transactions.itemSnapshotList.items) {
        transactions.itemSnapshotList.items.sumOf { it.amountRupiah }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Transaksi", style = ArusTheme.typography.headline)
                ArusTextField(query, { query = it }, placeholder = "Cari catatan atau merchant", modifier = Modifier.fillMaxWidth(), singleLine = true)
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArusButton(onClick = { transactionTypeOpen = true }, kind = ArusButtonKind.Outline) {
                        Text(transactionTypeLabel(transactionType))
                    }
                    ArusButton(onClick = { categoryPickerOpen = true }, kind = ArusButtonKind.Outline) {
                        Text(categories.firstOrNull { it.id == categoryId }?.name ?: "Semua kategori")
                    }
                }
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ArusButton(onClick = { walletId = null }, selected = walletId == null, kind = ArusButtonKind.Outline) { Text("Semua dompet") }
                    ArusButton(onClick = { walletPickerOpen = true }, kind = ArusButtonKind.Outline) {
                        Text(wallets.firstOrNull { it.id == walletId }?.name ?: "Pilih dompet")
                    }
                }
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("" to "Semua tanggal", "Bulan ini" to "Bulan ini", "Tahun ini" to "Tahun ini").forEach { (value, label) ->
                        ArusButton(onClick = { quickRange = value; if (value.isNotBlank()) { startDate = ""; endDate = "" } },
                            selected = quickRange == value, kind = ArusButtonKind.Outline) { Text(label) }
                    }
                }
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    ArusButton(onClick = { quickRange = ""; startDate = ""; endDate = ""; minimumAmount = ""; maximumAmount = ""; categoryId = null; walletId = null; transactionType = "" },
                        kind = ArusButtonKind.Ghost) { Text("Hapus filter") }
                }
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArusTextField(startDate, { startDate = it.filter { c -> c.isDigit() || c == '-' }.take(10) }, placeholder = "Dari YYYY-MM-DD", modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = true)
                        ArusTextField(endDate, { endDate = it.filter { c -> c.isDigit() || c == '-' }.take(10) }, placeholder = "Sampai YYYY-MM-DD", modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = true)
                    }
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArusTextField(minimumAmount, { minimumAmount = it.filter(Char::isDigit) }, placeholder = "Nominal minimum", modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = true)
                        ArusTextField(maximumAmount, { maximumAmount = it.filter(Char::isDigit) }, placeholder = "Nominal maksimum", modifier = Modifier.weight(1f).fillMaxWidth(), singleLine = true)
                    }
                }
                dateError?.let { Text(it, color = ArusTheme.colors.error) }
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArusButton(onClick = onCheckpoints, kind = ArusButtonKind.Outline) { Text("Riwayat rekonsiliasi") }
                    ArusButton(onClick = onTransfer, kind = ArusButtonKind.Outline) { Text("Transfer") }
                }
                ArusCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(ArusSpacing.Small), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                        Column {
                            Text("${transactions.itemCount} hasil")
                            Text(formatRupiah(resultTotal))
                        }
                    }
                }
            }
        }
        items(count = transactions.itemCount, key = transactions.itemKey { it.id }) { index ->
            val item = transactions[index] ?: return@items
            val day = java.time.Instant.ofEpochMilli(item.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            if (index == 0 || transactions.peek(index - 1)?.let {
                    java.time.Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() != day
                } != false) {
                Text(ArusFormatting.formatDate(day), style = ArusTheme.typography.title, modifier = Modifier.padding(top = ArusSpacing.Medium))
            }
            ArusCard(modifier = Modifier.fillMaxWidth(), onClick = { onSelect(item.id) }, contentPadding = ArusSpacing.Medium) {
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.merchant?.takeIf(String::isNotBlank) ?: item.note?.takeIf(String::isNotBlank) ?: item.type.name, style = ArusTheme.typography.body)
                        Text(item.type.name, style = ArusTheme.typography.label, color = ArusTheme.colors.mutedForeground)
                    }
                    Text(formatRupiah(item.amountRupiah), style = ArusTheme.typography.title)
                }
            }
        }
        if (transactions.loadState.refresh is androidx.paging.LoadState.Loading) item { Text("Memuat riwayat…", color = ArusTheme.colors.mutedForeground) }
        if (transactions.loadState.refresh is androidx.paging.LoadState.Error) item { ArusCard(Modifier.fillMaxWidth()) { Text("Riwayat gagal dimuat.", color = ArusTheme.colors.error) } }
    }
}

private fun transactionTypeLabel(type: String): String = when (type) {
    "EXPENSE" -> "Pengeluaran"
    "INCOME" -> "Pemasukan"
    "TRANSFER_OUT" -> "Transfer"
    else -> "Semua jenis"
}

private fun String.toLocalEpochMillisOrNull(endOfDay: Boolean): Long? {
    if (isBlank()) return null
    return runCatching {
        val date = java.time.LocalDate.parse(this)
        val boundary = if (endOfDay) date.plusDays(1) else date
        boundary.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}

private fun String.toLocalDateOrNull(): LocalDate? = if (isBlank()) null else runCatching { LocalDate.parse(this) }.getOrNull()

@Composable
private fun TransactionDetails(database: ArusDatabase, transactionId: Long, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var transaction by remember(transactionId) { mutableStateOf<TransactionEntity?>(null) }
    var walletName by remember(transactionId) { mutableStateOf("") }
    var categoryName by remember(transactionId) { mutableStateOf("") }
    var editorOpen by rememberSaveable(transactionId) { mutableStateOf(false) }
    var confirmationOpen by rememberSaveable(transactionId) { mutableStateOf(false) }
    var warning by rememberSaveable(transactionId) { mutableStateOf<String?>(null) }
    var amount by rememberSaveable(transactionId) { mutableStateOf("") }
    var note by rememberSaveable(transactionId) { mutableStateOf("") }
    var merchant by rememberSaveable(transactionId) { mutableStateOf("") }
    var date by rememberSaveable(transactionId) { mutableStateOf("") }
    var wallets by remember { mutableStateOf(emptyList<WalletEntity>()) }
    var categories by remember { mutableStateOf(emptyList<CategoryEntity>()) }
    var walletId by rememberSaveable(transactionId) { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable(transactionId) { mutableStateOf<Long?>(null) }
    var error by rememberSaveable(transactionId) { mutableStateOf<String?>(null) }
    suspend fun reload() {
        val data = withContext(Dispatchers.IO) {
            val dao = database.transactionDao()
            val row = dao.transactionById(transactionId)
            val wallet = row?.walletId?.let { database.walletDao().findById(it) }
            val category = row?.categoryId?.let { database.categoryDao().findById(it) }
            TransactionDetailsData(row, wallet, category, database.walletDao().activeWallets(), database.categoryDao().activeCategories())
        }
        transaction = data.transaction
        walletName = data.wallet?.name.orEmpty()
        categoryName = data.category?.name.orEmpty()
        wallets = data.wallets
        categories = data.categories
    }
    LaunchedEffect(transactionId) { reload() }
    val row = transaction
    if (row == null) return
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 760.dp).padding(ArusSpacing.Medium).verticalScroll(rememberScrollState()).clip(RoundedCornerShape(ArusRadius.ExtraLarge)).background(ArusTheme.colors.surface).border(1.dp, ArusTheme.colors.border, RoundedCornerShape(ArusRadius.ExtraLarge)).padding(ArusSpacing.Large), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
            Text("Rincian transaksi", style = ArusTheme.typography.headline)
            Text(formatRupiah(row.amountRupiah), style = ArusTheme.typography.display)
            ArusSegmentedButton(row.type.name, true, {}, Modifier.fillMaxWidth())
            Text(ArusFormatting.formatDateTime(java.time.Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault())))
            ArusCard(Modifier.fillMaxWidth(), contentPadding = ArusSpacing.Medium) {
                Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("Dompet", style = ArusTheme.typography.label, color = ArusTheme.colors.mutedForeground)
                    Text(walletName, style = ArusTheme.typography.title)
                    if (categoryName.isNotBlank()) Text("Kategori · $categoryName")
                    if (!row.note.isNullOrBlank()) Text("Catatan · ${row.note}")
                    if (!row.merchant.isNullOrBlank()) Text("Merchant · ${row.merchant}")
                    Text(if (row.occurredAtEpochMillis > System.currentTimeMillis()) "Terjadwal" else "Efektif", color = ArusTheme.colors.mutedForeground)
                }
            }
            if (row.transferGroupId != null) Text("Bagian transfer terkait.", color = ArusTheme.colors.mutedForeground)
            if (row.verifiedCheckpointId != null) Text("Terverifikasi checkpoint.", color = ArusTheme.colors.mutedForeground)
            warning?.let { Text(it, color = ArusTheme.colors.error) }
            error?.let { Text(it, color = ArusTheme.colors.error) }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                OutlinedArusButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Tutup") }
                if (row.type == TransactionType.INCOME || row.type == TransactionType.EXPENSE) {
                    ArusButton(onClick = {
                        scope.launch {
                            val special = withContext(Dispatchers.IO) { database.transactionDao().isSpecial(row.id) > 0 }
                            if (special) warning = "Transaksi bagian operasi khusus tidak dapat diedit terpisah."
                            else {
                                if (database.transactionDao().isVerified(row.id) > 0) warning = "Transaksi terverifikasi. Perubahan menandai checkpoint untuk rekonsiliasi ulang. Lanjutkan?"
                                editorOpen = true
                            }
                        }
                    }, kind = ArusButtonKind.Outline, modifier = Modifier.weight(1f)) { Text("Edit") }
                    ArusButton(onClick = {
                        scope.launch {
                            val protected = withContext(Dispatchers.IO) { database.transactionDao().isVerified(row.id) > 0 || database.transactionDao().isSpecial(row.id) > 0 }
                            if (protected) warning = "Transaksi terverifikasi atau bagian operasi khusus. Penghapusan akan ditolak atau perlu pemeriksaan ulang."
                            else confirmationOpen = true
                        }
                    }, kind = ArusButtonKind.Destructive, modifier = Modifier.weight(1f)) { Text("Hapus") }
                } else if (row.transferGroupId != null) {
                    ArusButton(onClick = { warning = "Transfer dan biaya terkait harus dikelola sebagai satu grup." }, kind = ArusButtonKind.Outline) { Text("Info transfer") }
                }
            }
        }
    }
    if (confirmationOpen) ArusDialog(onDismissRequest = { confirmationOpen = false }, title = { Text("Hapus transaksi?") }, text = { Text("Saldo akan dihitung ulang. Penghapusan dapat diurungkan selama lima detik.") }, confirmButton = {
        ArusButton(onClick = {
            scope.launch {
                val deletedAt = System.currentTimeMillis()
                val result = withContext(Dispatchers.IO) { database.transactionDao().softDeleteOrdinaryTrackingCheckpoint(row.id, deletedAt) }
                if (result == 1) { confirmationOpen = false; onDismiss() } else error = "Transaksi tidak dapat dihapus."
            }
        }) { Text("Hapus") }
    }, dismissButton = { OutlinedArusButton(onClick = { confirmationOpen = false }) { Text("Batal") } })
    if (editorOpen) {
        LaunchedEffect(editorOpen, row.id) {
            amount = row.amountRupiah.toString(); note = row.note.orEmpty(); merchant = row.merchant.orEmpty()
            date = java.time.Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            walletId = row.walletId; categoryId = row.categoryId
        }
        ArusDialog(onDismissRequest = { editorOpen = false }, title = { Text("Edit transaksi") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ArusTextField(amount, { amount = it.filter(Char::isDigit) }, placeholder = "Nominal", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                ArusTextField(date, { date = it }, placeholder = "Tanggal YYYY-MM-DD", singleLine = true)
                wallets.forEach { wallet -> ArusButton(onClick = { walletId = wallet.id }, kind = if (walletId == wallet.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(wallet.name) } }
                categories.filter { it.kind == row.type.name }.forEach { category -> ArusButton(onClick = { categoryId = category.id }, kind = if (categoryId == category.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(category.name) } }
                ArusTextField(note, { note = it }, placeholder = "Catatan", singleLine = true)
                ArusTextField(merchant, { merchant = it }, placeholder = "Merchant", singleLine = true)
            }
        }, confirmButton = {
            ArusButton(onClick = {
                val value = amount.toLongOrNull()
                val wallet = walletId
                val category = categoryId
                val effective = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
                if (value == null || value <= 0 || wallet == null || category == null || effective == null) { error = "Periksa nominal, dompet, kategori, dan tanggal."; return@ArusButton }
                scope.launch {
                    val verified = withContext(Dispatchers.IO) { database.transactionDao().isVerified(row.id) > 0 }
                    if (verified && !warning.orEmpty().contains("Lanjutkan perubahan")) { warning = "Transaksi terverifikasi. Perubahan akan memerlukan rekonsiliasi ulang. Lanjutkan perubahan dengan menekan Simpan lagi."; return@launch }
                    val result = withContext(Dispatchers.IO) { database.transactionDao().updateOrdinaryTrackingCheckpoint(row.id, value, wallet, category, effective, note.ifBlank { null }, merchant.ifBlank { null }) }
                    if (result == 1) { editorOpen = false; warning = null; reload() } else error = "Perubahan ditolak. Transaksi mungkin terkait operasi khusus."
                }
            }) { Text("Simpan") }
        }, dismissButton = { OutlinedArusButton(onClick = { editorOpen = false }) { Text("Batal") } })
    }
}

private data class TransactionDetailsData(
    val transaction: TransactionEntity?,
    val wallet: WalletEntity?,
    val category: CategoryEntity?,
    val wallets: List<WalletEntity>,
    val categories: List<CategoryEntity>,
)

@Composable
private fun WalletManagement(database: ArusDatabase) {
    val wallets = remember { mutableStateListOf<com.illustravibes.arus.data.WalletEntity>() }
    val archivedWallets = remember { mutableStateListOf<com.illustravibes.arus.data.WalletEntity>() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<com.illustravibes.arus.data.WalletEntity?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailBalance by remember { mutableStateOf<Long?>(null) }
    var detailTransactions by remember { mutableStateOf(emptyList<TransactionEntity>()) }
    var archiveTarget by remember { mutableStateOf<com.illustravibes.arus.data.WalletEntity?>(null) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    fun refresh() {
        scope.launch {
            val values = withContext(Dispatchers.IO) {
                database.walletDao().activeWallets() to database.walletDao().archivedWallets()
            }
            wallets.clear(); wallets.addAll(values.first)
            archivedWallets.clear(); archivedWallets.addAll(values.second)
        }
    }
    LaunchedEffect(database) { refresh() }
    LaunchedEffect(detailId, database) {
        val id = detailId ?: return@LaunchedEffect
        val details = withContext(Dispatchers.IO) {
            (BalanceRepository(database).balance(id, System.currentTimeMillis()) ?: 0L) to
                database.transactionDao().activeTransactionsThrough(id, System.currentTimeMillis())
        }
        detailBalance = details.first
        detailTransactions = details.second
    }
    val displayedWallets = if (showArchived) archivedWallets else wallets
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Text("Dompet", style = ArusTheme.typography.headline)
        OutlinedArusButton(onClick = { showArchived = !showArchived }, modifier = Modifier.fillMaxWidth()) {
            Text(if (showArchived) "‹ Dompet aktif" else "Dompet terarsip (${archivedWallets.size})")
        }
        if (!showArchived) ArusButton(onClick = { editing = null; formError = null; showForm = true }, kind = ArusButtonKind.Primary, modifier = Modifier.fillMaxWidth()) {
            ArusIcon(ArusIconCode.PLUS, fontSize = 14.sp)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Tambah dompet")
        }
        if (displayedWallets.isEmpty()) ArusCard(Modifier.fillMaxWidth()) {
            Text(if (showArchived) "Belum ada dompet terarsip." else "Belum ada dompet. Tambahkan dompet untuk mencatat saldo.", color = ArusTheme.colors.mutedForeground)
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            items(displayedWallets, key = { it.id }) { wallet ->
                ArusCard(onClick = { detailId = wallet.id }) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.background(ArusTheme.colors.surfaceVariant, RoundedCornerShape(50)).border(1.dp, ArusTheme.colors.border, RoundedCornerShape(50)).padding(8.dp)) {
                                ArusIcon(if (wallet.type == "CREDIT") ArusIconCode.CREDIT_CARD else if (wallet.type == "CASH") ArusIconCode.MONEY_BILL else if (wallet.type == "BANK") ArusIconCode.BANK else ArusIconCode.WALLET)
                            }
                            Text(wallet.name, style = ArusTheme.typography.title)
                        }
                        Text("${wallet.type} · Saldo awal ${formatRupiah(wallet.initialBalanceRupiah)}", color = ArusTheme.colors.mutedForeground)
                        if (wallet.type == "CREDIT") Text("Limit ${formatRupiah(wallet.creditLimitRupiah ?: 0)}" + (wallet.billingDay?.let { " · Tagihan tanggal $it" } ?: ""), color = ArusTheme.colors.mutedForeground)
                        if (showArchived) {
                            Text("Terarsip · tidak tersedia untuk transaksi baru", color = ArusTheme.colors.mutedForeground)
                        } else {
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedArusButton(onClick = { editing = wallet; formError = null; showForm = true }) { Text("Edit") }
                                OutlinedArusButton(onClick = { archiveTarget = wallet }) { Text("Arsip") }
                                OutlinedArusButton(onClick = { moveWallet(database, wallets, wallet, -1, ::refresh, scope) }, enabled = wallets.indexOf(wallet) > 0) { Text("Naik") }
                                OutlinedArusButton(onClick = { moveWallet(database, wallets, wallet, 1, ::refresh, scope) }, enabled = wallets.indexOf(wallet) < wallets.lastIndex) { Text("Turun") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showForm) WalletFormDialog(database, editing, formError, { formError = it }, {
        showForm = false; editing = null; refresh()
    }, { showForm = false })
    archiveTarget?.let { target ->
        ArusDialog(onDismissRequest = { archiveTarget = null },
            title = { Text("Arsipkan ${target.name}?") },
            text = { Text("Dompet tidak tersedia untuk transaksi baru. Riwayat tetap tersimpan.") },
            confirmButton = { ArusButton(onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) { database.walletDao().archive(target.id) }
                    archiveTarget = null; refresh()
                }
            }) { Text("Arsipkan") } },
            dismissButton = { OutlinedArusButton(onClick = { archiveTarget = null }) { Text("Batal") } })
    }
    detailId?.let { id ->
        val wallet = (wallets + archivedWallets).find { it.id == id }
        if (wallet != null) ArusDialog(onDismissRequest = { detailId = null },
            title = { Text(wallet.name) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("${wallet.type}")
                    Text("Saldo saat ini")
                    Text(formatRupiah(detailBalance ?: wallet.initialBalanceRupiah), style = ArusTheme.typography.title)
                    Text("Saldo awal · ${formatRupiah(wallet.initialBalanceRupiah)}")
                    Text("Riwayat · ${detailTransactions.size} transaksi", style = ArusTheme.typography.label)
                    if (detailTransactions.isEmpty()) Text("Belum ada transaksi di dompet ini.", color = ArusTheme.colors.mutedForeground)
                        detailTransactions.forEach { transaction ->
                        val transactionDate = java.time.Instant.ofEpochMilli(transaction.occurredAtEpochMillis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        Text("${transaction.merchant ?: transaction.note ?: transactionTypeLabel(transaction.type.name)} · ${ArusFormatting.formatDate(transactionDate)} · ${formatRupiah(transaction.amountRupiah)}")
                    }
                }
            },
            confirmButton = { ArusButton(onClick = { detailId = null }) { Text("Tutup") } })
    }
}

private fun saveOrdinaryTransaction(
    database: ArusDatabase,
    draft: TransactionDraft,
    allowNegative: Boolean,
    splits: List<TransactionSplitDraft>?,
): Long {
    val entity = TransactionEntity(
        draft.type, draft.walletId, draft.amountRupiah, null, draft.categoryId,
        draft.occurredAtEpochMillis, draft.note?.trim()?.ifEmpty { null },
        draft.merchant?.trim()?.ifEmpty { null }, null,
    )
    return if (splits.isNullOrEmpty()) database.transactionDao().insertOrdinary(entity, allowNegative)
    else database.transactionDao().insertOrdinaryWithSplits(entity, allowNegative, splits)
}

@Composable
private fun TransferEntry(database: ArusDatabase, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dialogMaxHeight = if (WindowInsets.ime.getBottom(LocalDensity.current) > 0) 440.dp else 760.dp
    val wallets = remember { mutableStateListOf<WalletEntity>() }
    var sourceId by rememberSaveable { mutableStateOf<Long?>(null) }
    var destinationId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var fee by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var allowNegative by rememberSaveable { mutableStateOf(false) }
    var template by rememberSaveable { mutableStateOf("Tarik tunai") }
    var swapOrder by rememberSaveable { mutableStateOf(false) }
    var sameWallet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(database) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        val values = withContext(Dispatchers.IO) { database.walletDao().activeWallets() }
        wallets.clear(); wallets.addAll(values)
        val lastId = context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).getLong("last_wallet_id", -1L)
        sourceId = wallets.firstOrNull { it.id == lastId }?.id ?: wallets.firstOrNull()?.id
        destinationId = wallets.firstOrNull { it.id != sourceId }?.id
    }
    val occurred = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().imePadding().heightIn(max = dialogMaxHeight).verticalScroll(rememberScrollState()).padding(ArusSpacing.Medium).clip(RoundedCornerShape(ArusRadius.ExtraLarge)).background(ArusTheme.colors.surface).border(1.dp, ArusTheme.colors.border, RoundedCornerShape(ArusRadius.ExtraLarge)).padding(ArusSpacing.Large), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
            Text("Transfer", style = ArusTheme.typography.headline)
            Text("Template")
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                listOf("Tarik tunai", "Bayar kredit", "Top up").forEach { choice ->
                    ArusButton(onClick = { template = choice; if (choice == "Bayar kredit") destinationId = wallets.firstOrNull { it.type == "CREDIT" }?.id }, kind = if (template == choice) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(choice) }
                }
            }
            val sourceLabel = if (swapOrder) "Ke · ${wallets.firstOrNull { it.id == sourceId }?.name.orEmpty()}" else "Dari · ${wallets.firstOrNull { it.id == sourceId }?.name.orEmpty()}"
            val destinationLabel = if (swapOrder) "Dari · ${wallets.firstOrNull { it.id == destinationId }?.name.orEmpty()}" else if (wallets.firstOrNull { it.id == destinationId }?.type == "CREDIT") "Bayar tagihan · Ke kartu kredit" else "Ke · ${wallets.firstOrNull { it.id == destinationId }?.name.orEmpty()}"
            ArusButton(onClick = { swapOrder = !swapOrder; val previousSource = sourceId; sourceId = destinationId; destinationId = previousSource }, kind = ArusButtonKind.Ghost, modifier = Modifier.fillMaxWidth()) { Text("⇅ Tukar dompet") }
            Text(sourceLabel)
            wallets.forEach { wallet -> ArusButton(onClick = { sourceId = wallet.id; sameWallet = wallet.id == destinationId; error = null }, kind = if (sourceId == wallet.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(wallet.name) } }
            Text(destinationLabel)
            wallets.forEach { wallet -> ArusButton(onClick = { destinationId = wallet.id; sameWallet = wallet.id == sourceId; error = null }, kind = if (destinationId == wallet.id) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(wallet.name) } }
            if (sameWallet) Text("Dompet asal dan tujuan harus berbeda.", color = ArusTheme.colors.error)
            ArusTextField(amount, { amount = it.filter(Char::isDigit); error = null }, placeholder = "Nominal transfer (Rp)", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            if (wallets.firstOrNull { it.id == destinationId }?.type == "CREDIT") {
                ArusButton(onClick = {
                    val destination = destinationId ?: return@ArusButton
                    val creditWallet = wallets.firstOrNull { it.id == destination } ?: return@ArusButton
                    scope.launch {
                        val used = withContext(Dispatchers.IO) { com.illustravibes.arus.data.BalanceRepository(database).creditUsed(destination, System.currentTimeMillis()) ?: 0L }
                        amount = used.coerceAtLeast(0L).toString()
                    }
                }, kind = ArusButtonKind.Outline) { Text("Bayar penuh (terpakai saat ini)") }
            }
            ArusTextField(fee, { fee = it.filter(Char::isDigit); error = null }, placeholder = "Biaya admin (Rp)", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            ArusTextField(date, { date = it; error = null }, placeholder = "Tanggal (YYYY-MM-DD)", singleLine = true)
            ArusTextField(note, { note = it }, placeholder = "Catatan (opsional)", singleLine = true)
            error?.let { Text(it, color = ArusTheme.colors.error) }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedArusButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Batal") }
                ArusButton(onClick = {
                    val source = sourceId
                    val destination = destinationId
                    val value = amount.toLongOrNull()
                    val feeValue = fee.toLongOrNull()
                    if (sameWallet || source == null || destination == null || value == null || feeValue == null || occurred == null || value <= 0) {
                        error = "Pilih dua dompet berbeda, nominal transfer positif, dan tanggal valid."
                    } else scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runCatching { database.transactionDao().insertTransfer(source, destination, value, feeValue, occurred, note.trim().ifEmpty { null }, allowNegative) }
                        }
                        result.fold(
                            onSuccess = { context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).edit().putLong("last_wallet_id", source).apply(); onDismiss() },
                            onFailure = { failure ->
                                if (failure is TransactionDao.InsufficientBalanceException && !allowNegative) {
                                    allowNegative = true
                                    error = "Saldo asal tidak cukup, transfer akan membuat saldo minus. Tekan Simpan lagi untuk melanjutkan."
                                } else error = failure.message ?: "Transfer tidak dapat disimpan."
                            },
                        )
                    }
                }, modifier = Modifier.weight(1f)) { Text("Simpan") }
            }
        }
    }
}

private data class EditableTransactionSplit(
    val id: Long,
    val categoryId: Long?,
    val value: String,
)

@Composable
private fun TransactionQuickEntry(
    database: ArusDatabase,
    type: TransactionType,
    onDismiss: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    initialReceipt: ReceiptOcrReview? = null,
    receiptAttachmentId: Long? = null,
    onSaved: (Long, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val wallets = remember { mutableStateListOf<WalletEntity>() }
    val categories = remember { mutableStateListOf<CategoryEntity>() }
    var walletId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amount by rememberSaveable { mutableStateOf(initialReceipt?.totalRupiah?.toString().orEmpty()) }
    var note by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf(initialReceipt?.merchant.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(initialReceipt?.date?.toString() ?: LocalDate.now().toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var negativeWarning by rememberSaveable { mutableStateOf(false) }
    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    var showMetadata by rememberSaveable { mutableStateOf(initialReceipt?.merchant != null) }
    var showOcrText by rememberSaveable { mutableStateOf(false) }
    var splitEnabled by rememberSaveable { mutableStateOf(false) }
    var splitEditorOpen by rememberSaveable { mutableStateOf(false) }
    var splitValueMode by rememberSaveable { mutableStateOf("NOMINAL") }
    var splitEditorError by rememberSaveable { mutableStateOf<String?>(null) }
    var splitCategoryPickerRowId by rememberSaveable { mutableStateOf<Long?>(null) }
    val splitRows = remember { mutableStateListOf<EditableTransactionSplit>() }
    var nextSplitRowId by remember { mutableStateOf(1L) }
    var hintedCategoryId by remember { mutableStateOf<Long?>(null) }
    var walletPickerOpen by rememberSaveable { mutableStateOf(false) }
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    LaunchedEffect(database, type, initialReceipt) {
        val values = withContext(Dispatchers.IO) {
            database.walletDao().activeWallets() to database.categoryDao().activeCategories(
                if (type == TransactionType.INCOME) "INCOME" else "EXPENSE",
            )
        }
        wallets.clear(); wallets.addAll(values.first)
        categories.clear(); categories.addAll(values.second)
        val lastWalletId = context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).getLong("last_wallet_id", -1L)
        walletId = wallets.firstOrNull { it.id == lastWalletId }?.id ?: wallets.firstOrNull()?.id
        val suggestion = withContext(Dispatchers.IO) {
            initialReceipt?.merchant?.let { merchantName ->
                database.merchantCategoryHintDao().suggestedCategoryId(ReceiptOcrParser.normalizeMerchant(merchantName))
            }
        }
        hintedCategoryId = categories.firstOrNull { it.id == suggestion }?.id
        categoryId = hintedCategoryId ?: categories.firstOrNull()?.id
        if (initialReceipt == null) withContext(Dispatchers.Main) {
            withFrameNanos { }
            amountFocus.requestFocus()
            keyboard?.show()
        }
    }

    fun beginSplitEditing() {
        if (!splitEnabled) {
            splitRows.clear()
            splitRows += EditableTransactionSplit(nextSplitRowId++, categoryId, amount.filter(Char::isDigit))
            splitRows += EditableTransactionSplit(nextSplitRowId++, null, "")
            splitValueMode = "NOMINAL"
        }
        splitEditorError = null
        splitEditorOpen = true
    }

    fun changeSplitValueMode(mode: String) {
        if (mode == splitValueMode) return
        val total = amount.toLongOrNull()
        val values = splitRows.map { it.value.toLongOrNull() ?: 0L }
        val converted = if (total == null) null else if (mode == "PERSEN")
            TransactionSplitCalculator.percentagesFromAmounts(total, values)
        else TransactionSplitCalculator.amountsFromPercent(total, values)
        if (converted == null) {
            splitEditorError = if (mode == "PERSEN")
                "Isi nominal split hingga seimbang sebelum mengubah ke persen."
            else "Persentase split harus berjumlah 100% sebelum mengubah ke nominal."
            return
        }
        splitRows.indices.forEach { index -> splitRows[index] = splitRows[index].copy(value = converted[index].toString()) }
        splitValueMode = mode
        splitEditorError = null
    }

    fun transactionSplitDrafts(): List<TransactionSplitDraft>? {
        if (!splitEnabled) return null
        val total = amount.toLongOrNull() ?: return null
        val categoriesInRows = splitRows.map { it.categoryId ?: return null }
        if (categoriesInRows.toSet().size != categoriesInRows.size) return null
        val values = splitRows.map { it.value.toLongOrNull() ?: return null }
        val splitAmounts = if (splitValueMode == "PERSEN")
            TransactionSplitCalculator.amountsFromPercent(total, values)
        else TransactionSplitCalculator.amountsFromNominal(total, values)
        splitAmounts ?: return null
        return splitRows.mapIndexed { index, row ->
            TransactionSplitDraft(splitAmounts[index], requireNotNull(row.categoryId), true, true)
        }
    }

    val selectedWallet = wallets.firstOrNull { it.id == walletId }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val dateEpoch = runCatching {
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
    val now = System.currentTimeMillis()
    val dateIsFuture = dateEpoch != null && dateEpoch > now
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 760.dp).verticalScroll(rememberScrollState()).padding(ArusSpacing.Medium).clip(RoundedCornerShape(ArusRadius.ExtraLarge)).background(ArusTheme.colors.surface).border(1.dp, ArusTheme.colors.border, RoundedCornerShape(ArusRadius.ExtraLarge)).padding(ArusSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            Text(if (initialReceipt == null) "Tambah transaksi" else "Konfirmasi hasil scan",
                style = ArusTheme.typography.headline)
            if (initialReceipt != null) {
                Text("Periksa semua hasil OCR sebelum menyimpan transaksi.")
                OutlinedArusButton(onClick = { showOcrText = !showOcrText }) {
                    Text(if (showOcrText) "Sembunyikan teks OCR" else "Lihat teks OCR")
                }
                if (showOcrText) ArusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(initialReceipt.rawText.ifBlank { "Belum ada teks yang terbaca." },
                        modifier = Modifier.padding(ArusSpacing.Medium))
                }
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small), modifier = Modifier.fillMaxWidth()) {
                ArusSegmentedButton("Pengeluaran", type == TransactionType.EXPENSE, { onTypeChange(TransactionType.EXPENSE) }, Modifier.weight(1f))
                ArusSegmentedButton("Pemasukan", type == TransactionType.INCOME, { onTypeChange(TransactionType.INCOME) }, Modifier.weight(1f))
            }
            ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Column {
                    Text("Nominal", style = ArusTheme.typography.label, color = ArusTheme.colors.mutedForeground)
                    ArusTextField(amount, { amount = it.filter(Char::isDigit); error = null; negativeWarning = false }, placeholder = "Rp 0", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().focusRequester(amountFocus))
                    initialReceipt?.let { ReceiptConfidenceLabel("Total", it.totalConfidencePercent) }
                }
            }
            if (splitEnabled) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Pecah kategori", style = ArusTheme.typography.title)
                        Text("${splitRows.size} bagian · total harus sama dengan nominal")
                    }
                    ArusButton(onClick = ::beginSplitEditing, kind = ArusButtonKind.Outline) { Text("Ubah split") }
                }
                val drafts = transactionSplitDrafts()
                val splitAmount = drafts?.sumOf { it.amountRupiah } ?: 0L
                Text(
                    if (drafts == null) "Split belum seimbang. Ubah pembagian sebelum menyimpan."
                    else "Total split · ${formatRupiah(splitAmount)}",
                    color = if (drafts == null) ArusTheme.colors.error else ArusTheme.colors.mutedForeground,
                )
            } else {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text(if (hintedCategoryId == categoryId && hintedCategoryId != null) "Saran merchant" else "Sering dipakai",
                        style = ArusTheme.typography.title, modifier = Modifier.weight(1f))
                    ArusButton(onClick = { categoryPickerOpen = true }, kind = ArusButtonKind.Outline) { Text("Pilih kategori") }
                }
                if (initialReceipt != null && hintedCategoryId == categoryId && hintedCategoryId != null) {
                    Text("Saran dari merchant ${initialReceipt.merchant.orEmpty()}.")
                }
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    val quickCategories = if (showAllCategories) categories else categories.take(5)
                    quickCategories.forEach { category ->
                        ArusButton(onClick = { categoryId = category.id }, kind = if (category.id == categoryId) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(category.name) }
                    }
                    if (categories.size > 5) ArusButton(onClick = { showAllCategories = !showAllCategories }, kind = ArusButtonKind.Ghost) { Text(if (showAllCategories) "Tutup" else "Lainnya ›") }
                }
                OutlinedArusButton(onClick = ::beginSplitEditing, modifier = Modifier.fillMaxWidth()) {
                    Text("Pecah kategori")
                }
            }
            ArusButton(onClick = { walletPickerOpen = true }, kind = ArusButtonKind.Outline, modifier = Modifier.fillMaxWidth()) {
                Text("Dompet: ${selectedWallet?.name ?: "Pilih dompet"}")
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ArusButton(onClick = {
                    val initial = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                    DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }, kind = ArusButtonKind.Outline) { Text("Tanggal: $date") }
                initialReceipt?.let { ReceiptConfidenceLabel("Tanggal", it.dateConfidencePercent) }
                ArusButton(onClick = { showMetadata = !showMetadata }, kind = ArusButtonKind.Ghost) { Text(if (showMetadata) "− Rincian" else "+ Catatan / merchant") }
            }
            if (showMetadata) {
                ArusTextField(note, { note = it }, placeholder = "Catatan (opsional)", singleLine = true, modifier = Modifier.fillMaxWidth())
                ArusTextField(merchant, { merchant = it }, placeholder = "Merchant (opsional)", singleLine = true, modifier = Modifier.fillMaxWidth())
                initialReceipt?.let { ReceiptConfidenceLabel("Merchant", it.merchantConfidencePercent) }
            }
            if (dateIsFuture) Text("Terjadwal · belum masuk saldo", color = ArusTheme.colors.mutedForeground)
            error?.let { Text(it, color = ArusTheme.colors.error) }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedArusButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Batal") }
                ArusButton(onClick = {
                    val splits = transactionSplitDrafts()
                    if (splitEnabled && splits == null) {
                        error = "Total split harus sama dengan nominal transaksi."
                        return@ArusButton
                    }
                    submitTransaction(scope, type, walletId, categoryId, amount, dateEpoch, note, merchant,
                        negativeWarning, { negativeWarning = it }, { error = it }, database,
                        receiptAttachmentId, initialReceipt != null, splits) { savedId ->
                    context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).edit().putLong("last_wallet_id", walletId ?: -1L).apply()
                    amount = ""; note = ""; merchant = ""; error = null; negativeWarning = false
                    splitEnabled = false; splitRows.clear(); splitValueMode = "NOMINAL"
                    amountFocus.requestFocus(); keyboard?.show()
                    onSaved(savedId, false)
                } }, modifier = Modifier.weight(1f)) { Text("Simpan & tambah lagi") }
                ArusButton(onClick = {
                    val splits = transactionSplitDrafts()
                    if (splitEnabled && splits == null) {
                        error = "Total split harus sama dengan nominal transaksi."
                        return@ArusButton
                    }
                    submitTransaction(scope, type, walletId, categoryId, amount, dateEpoch, note, merchant,
                        negativeWarning, { negativeWarning = it }, { error = it }, database,
                        receiptAttachmentId, initialReceipt != null, splits) { savedId ->
                    context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).edit().putLong("last_wallet_id", walletId ?: -1L).apply()
                    onSaved(savedId, true)
                } }, modifier = Modifier.weight(1f)) { Text("Simpan") }
            }
        }
    }
    if (walletPickerOpen) ArusDialog(onDismissRequest = { walletPickerOpen = false }, title = { Text("Pilih dompet") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            wallets.forEach { wallet ->
                ArusButton(onClick = { walletId = wallet.id; walletPickerOpen = false }, kind = if (wallet.id == walletId) ArusButtonKind.Secondary else ArusButtonKind.Outline, modifier = Modifier.fillMaxWidth()) {
                    Text("${wallet.name} · ${wallet.type}")
                }
            }
        }
    }, confirmButton = { OutlinedArusButton(onClick = { walletPickerOpen = false }) { Text("Tutup") } })
    if (categoryPickerOpen) ArusDialog(onDismissRequest = { categoryPickerOpen = false }, title = { Text("Pilih kategori") }, text = {
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { category ->
                ArusButton(onClick = { categoryId = category.id; categoryPickerOpen = false }, kind = if (category.id == categoryId) ArusButtonKind.Secondary else ArusButtonKind.Outline, modifier = Modifier.fillMaxWidth()) {
                    Text(category.name)
                }
            }
        }
    }, confirmButton = { OutlinedArusButton(onClick = { categoryPickerOpen = false }) { Text("Tutup") } })

    if (splitEditorOpen) ArusDialog(
        onDismissRequest = { splitEditorOpen = false },
        title = { Text("Pecah kategori") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
            ) {
                Text("Total transaksi · ${amount.toLongOrNull()?.let(::formatRupiah) ?: "Rp 0"}")
                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    ArusSegmentedButton(
                        "Nominal", splitValueMode == "NOMINAL", { changeSplitValueMode("NOMINAL") },
                        Modifier.weight(1f).testTag("transaction_split_mode_nominal"),
                    )
                    ArusSegmentedButton(
                        "Persen", splitValueMode == "PERSEN", { changeSplitValueMode("PERSEN") },
                        Modifier.weight(1f).testTag("transaction_split_mode_percent"),
                    )
                }
                splitRows.forEachIndexed { index, row ->
                    ArusCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(ArusSpacing.Small),
                            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                        ) {
                            Text("Bagian ${index + 1}", style = ArusTheme.typography.label)
                            ArusButton(
                                onClick = { splitCategoryPickerRowId = row.id },
                                kind = ArusButtonKind.Outline,
                                modifier = Modifier.fillMaxWidth().testTag("transaction_split_row_category_${row.id}"),
                            ) {
                                Text(categories.firstOrNull { it.id == row.categoryId }?.name ?: "Pilih kategori")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small), verticalAlignment = Alignment.CenterVertically) {
                                ArusTextField(
                                    value = row.value,
                                    onValueChange = { value ->
                                        val rowIndex = splitRows.indexOfFirst { it.id == row.id }
                                        if (rowIndex >= 0) splitRows[rowIndex] = splitRows[rowIndex].copy(value = value.filter(Char::isDigit))
                                        splitEditorError = null
                                    },
                                    placeholder = if (splitValueMode == "PERSEN") "Persen" else "Nominal (Rp)",
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    testTag = "transaction_split_value_${row.id}",
                                )
                                if (splitRows.size > 2) {
                                    OutlinedArusButton(onClick = {
                                        splitRows.removeAll { it.id == row.id }
                                        splitEditorError = null
                                    }) { Text("Hapus") }
                                }
                            }
                        }
                    }
                }
                OutlinedArusButton(onClick = {
                    if (splitRows.size >= 10) {
                        splitEditorError = "Split transaksi dibatasi maksimal 10 bagian."
                    } else {
                        val newValue = if (splitValueMode == "PERSEN") "1" else ""
                        splitRows += EditableTransactionSplit(nextSplitRowId++, null, newValue)
                        if (splitValueMode == "PERSEN" && splitRows.size > 1) {
                            val previous = splitRows[0]
                            val previousValue = previous.value.toIntOrNull()
                            if (previousValue != null && previousValue > 1)
                                splitRows[0] = previous.copy(value = (previousValue - 1).toString())
                        }
                        splitEditorError = null
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Tambah baris") }
                val parsedValues = splitRows.mapNotNull { it.value.toLongOrNull() }
                Text(
                    if (splitValueMode == "PERSEN") "Persentase · ${parsedValues.sum()}% dari 100%"
                    else "Total bagian · ${formatRupiah(parsedValues.sum())}",
                    style = ArusTheme.typography.label,
                )
                splitEditorError?.let { Text(it, color = ArusTheme.colors.error) }
            }
        },
        dismissButton = {
            OutlinedArusButton(onClick = { splitEditorOpen = false }) { Text("Batal") }
        },
        confirmButton = {
            ArusButton(onClick = {
                val total = amount.toLongOrNull()
                val rowCategories = splitRows.mapNotNull { it.categoryId }
                val values = splitRows.map { it.value.toLongOrNull() ?: 0L }
                val validRows = rowCategories.size == splitRows.size && rowCategories.toSet().size == rowCategories.size
                val splitAmounts = if (total == null) null else if (splitValueMode == "PERSEN")
                    TransactionSplitCalculator.amountsFromPercent(total, values)
                else TransactionSplitCalculator.amountsFromNominal(total, values)
                when {
                    total == null || total <= 0 -> splitEditorError = "Isi nominal transaksi positif terlebih dahulu."
                    !validRows -> splitEditorError = "Pilih kategori berbeda untuk setiap bagian."
                    splitAmounts == null -> splitEditorError = if (splitValueMode == "PERSEN")
                        "Persentase split harus berjumlah 100% dan setiap bagian harus positif."
                    else "Total split harus sama dengan nominal transaksi."
                    else -> {
                        categoryId = splitRows.first().categoryId
                        splitEnabled = true
                        splitEditorOpen = false
                        splitEditorError = null
                    }
                }
            }) { Text("Simpan split") }
        },
    )

    splitCategoryPickerRowId?.let { rowId ->
        ArusDialog(
            onDismissRequest = { splitCategoryPickerRowId = null },
            title = { Text("Pilih kategori split") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    categories.forEach { category ->
                        val usedElsewhere = splitRows.any { it.id != rowId && it.categoryId == category.id }
                        ArusButton(
                            onClick = {
                                val index = splitRows.indexOfFirst { it.id == rowId }
                                if (index >= 0) splitRows[index] = splitRows[index].copy(categoryId = category.id)
                                splitCategoryPickerRowId = null
                                splitEditorError = null
                            },
                            enabled = !usedElsewhere,
                            kind = ArusButtonKind.Outline,
                            modifier = Modifier.fillMaxWidth().testTag("transaction_split_category_${rowId}_${category.id}"),
                        ) { Text(category.name) }
                    }
                }
            },
            confirmButton = { OutlinedArusButton(onClick = { splitCategoryPickerRowId = null }) { Text("Tutup") } },
        )
    }
}

private fun submitTransaction(
    scope: kotlinx.coroutines.CoroutineScope,
    type: TransactionType,
    walletId: Long?,
    categoryId: Long?,
    amount: String,
    occurredAt: Long?,
    note: String,
    merchant: String,
    negativeConfirmed: Boolean,
    setNegativeConfirmed: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    database: ArusDatabase,
    receiptAttachmentId: Long?,
    recordMerchantHint: Boolean,
    splits: List<TransactionSplitDraft>?,
    onSaved: (Long) -> Unit,
) {
    val id = walletId
    val category = categoryId
    val value = amount.toLongOrNull()
    val occurred = occurredAt
    if (id == null || category == null || value == null || value <= 0 || occurred == null) {
        setError("Pilih dompet dan kategori, lalu isi nominal serta tanggal yang valid.")
        return
    }
    val now = System.currentTimeMillis()
    scope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val wallet = database.walletDao().findById(id) ?: error("Dompet tidak ditemukan.")
                val balance = com.illustravibes.arus.data.BalanceRepository(database).balance(id, now) ?: 0L
                val draft = TransactionDraft(type, value, wallet.type, balance, occurred, now, category, note, merchant, id)
                if (draft.requiresNegativeBalanceWarning && !negativeConfirmed) throw TransactionDao.InsufficientBalanceException()
                database.runInTransaction(java.util.concurrent.Callable {
                    val transactionId = saveOrdinaryTransaction(database, draft, negativeConfirmed, splits)
                    if (receiptAttachmentId != null) {
                        check(database.attachmentDao().attach(receiptAttachmentId, "TRANSACTION", transactionId) == 1) {
                            "Lampiran struk tidak dapat ditautkan ke transaksi."
                        }
                    }
                    if (recordMerchantHint && !merchant.isNullOrBlank()) {
                        database.merchantCategoryHintDao().recordConfirmedChoice(
                            ReceiptOcrParser.normalizeMerchant(merchant), category,
                        )
                    }
                    transactionId
                })
            }
        }
        result.fold(
            onSuccess = { onSaved(it) },
            onFailure = { failure ->
                if (failure is TransactionDao.InsufficientBalanceException) {
                    setNegativeConfirmed(true)
                    setError("Saldo tidak cukup, saldo akan minus. Tetap simpan?")
                } else setError(failure.message ?: "Transaksi tidak dapat disimpan.")
            },
        )
    }
}

@Composable
private fun WalletFormDialog(
    database: ArusDatabase,
    wallet: com.illustravibes.arus.data.WalletEntity?,
    error: String?,
    onError: (String?) -> Unit,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    var type by remember(wallet?.id) { mutableStateOf(wallet?.type ?: "BANK") }
    var name by remember(wallet?.id) { mutableStateOf(wallet?.name.orEmpty()) }
    var balance by remember(wallet?.id) { mutableStateOf(wallet?.initialBalanceRupiah?.toString() ?: "0") }
    var limit by remember(wallet?.id) { mutableStateOf(wallet?.creditLimitRupiah?.toString().orEmpty()) }
    var billingDay by remember(wallet?.id) { mutableStateOf(wallet?.billingDay?.toString().orEmpty()) }
    val context = LocalContext.current
    val presets = remember(type) { readWalletPresets(context, type).map { it.name } }
    val scope = rememberCoroutineScope()
    ArusDialog(onDismissRequest = onCancel,
        title = { Text(if (wallet == null) "Dompet baru" else "Edit dompet") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("BANK", "EWALLET", "CASH", "CREDIT").forEach { value ->
                            OutlinedArusButton(onClick = { type = value }) { Text(when(value) { "BANK" -> "Bank"; "EWALLET" -> "E-Wallet"; "CASH" -> "Tunai"; else -> "Kredit" }) }
                        }
                    }
                }
                if (presets.isNotEmpty()) item { Text("Preset: ${presets.take(8).joinToString(" · ")}") }
                item { ArusTextField(value = name, onValueChange = { name = it }, placeholder = "Nama", modifier = Modifier.fillMaxWidth()) }
                item { ArusTextField(value = balance, onValueChange = { balance = it.filter(Char::isDigit) }, placeholder = "Saldo awal (Rp)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                if (type == "CREDIT") {
                    item { ArusTextField(value = limit, onValueChange = { limit = it.filter(Char::isDigit) }, placeholder = "Batas kredit (Rp)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                    item { ArusTextField(value = billingDay, onValueChange = { billingDay = it.filter(Char::isDigit).take(2) }, placeholder = "Hari tagihan (opsional, 1–31)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) }
                }
                if (error != null) item { Text(error, color = ArusTheme.colors.error) }
            }
        },
        confirmButton = { ArusButton(onClick = {
            val start = balance.toLongOrNull()
            val creditLimit = if (type == "CREDIT") limit.toLongOrNull() else null
            val day = if (type == "CREDIT" && billingDay.isNotBlank()) billingDay.toIntOrNull() else null
            if (name.isBlank() || start == null || start < 0 || type == "CREDIT" && (creditLimit == null || creditLimit <= 0) || day != null && day !in 1..31) {
                onError("Periksa nama, saldo, batas kredit, dan hari tagihan.")
                return@ArusButton
            }
            scope.launch {
                withContext(Dispatchers.IO) {
                    val target = wallet ?: com.illustravibes.arus.data.WalletEntity(name.trim(), type, "account_balance_wallet", 0xFF607D8B.toInt(), start, type != "CREDIT", false, null)
                    target.name = name.trim(); target.type = type; target.initialBalanceRupiah = start
                    target.creditLimitRupiah = creditLimit; target.billingDay = day
                    target.includeInTotal = type != "CREDIT"
                    if (wallet == null) {
                        target.sortOrder = database.walletDao().nextSortOrder()
                        database.walletDao().insert(target)
                    } else database.walletDao().update(target)
                }
                onSaved()
            }
        }) { Text("Simpan") } },
        dismissButton = { OutlinedArusButton(onClick = onCancel) { Text("Batal") } })
}

private fun moveWallet(database: ArusDatabase, wallets: List<com.illustravibes.arus.data.WalletEntity>, wallet: com.illustravibes.arus.data.WalletEntity, delta: Int, refresh: () -> Unit, scope: kotlinx.coroutines.CoroutineScope) {
    val from = wallets.indexOf(wallet); val to = from + delta
    if (to !in wallets.indices) return
    scope.launch {
        withContext(Dispatchers.IO) {
            database.runInTransaction {
                database.walletDao().updateSortOrder(wallets[to].id, from)
                database.walletDao().updateSortOrder(wallets[from].id, to)
            }
        }
        refresh()
    }
}

@Composable
private fun AppLockSettings(
    enabled: Boolean,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    onDismiss: () -> Unit,
    onSetup: () -> Unit,
    onDisable: () -> Unit,
    onBiometric: (Boolean) -> Unit,
) {
    ArusDialog(onDismissRequest = onDismiss, title = { Text("Pengaturan keamanan") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            Text(if (enabled) "Kunci aplikasi aktif. Arus terkunci saat aplikasi masuk background." else "Kunci aplikasi belum aktif.")
            if (enabled && biometricAvailable) {
                ArusButton(onClick = { onBiometric(!biometricEnabled) }, kind = if (biometricEnabled) ArusButtonKind.Secondary else ArusButtonKind.Outline) {
                    Text(if (biometricEnabled) "Biometrik aktif" else "Aktifkan biometrik")
                }
            }
            if (enabled && !biometricAvailable) Text("Biometrik tidak tersedia. PIN tetap dapat digunakan.")
        }
    }, confirmButton = {
        if (enabled) ArusButton(onClick = onDisable, kind = ArusButtonKind.Destructive) { Text("Matikan kunci") }
        else ArusButton(onClick = onSetup) { Text("Atur PIN") }
        OutlinedArusButton(onClick = onDismiss) { Text("Tutup") }
    })
}

@Composable
private fun PinSetupDialog(
    confirm: Boolean,
    pin: String,
    error: String?,
    onPinChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    ArusDialog(onDismissRequest = onCancel, title = { Text(if (confirm) "Konfirmasi PIN" else "Buat PIN") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            Text(if (confirm) "Masukkan ulang PIN enam digit." else "Pilih PIN enam digit.")
            ArusTextField(pin, onPinChange, placeholder = "PIN 6 digit", singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(), testTag = "lock_pin_setup")
            error?.let { Text(it, color = ArusTheme.colors.error) }
        }
    }, confirmButton = {
        OutlinedArusButton(onClick = onCancel) { Text("Batal") }
        ArusButton(onClick = onSubmit) { Text(if (confirm) "Aktifkan" else "Lanjut") }
    })
}

@Composable
private fun AppLockScreen(
    pin: String,
    error: String?,
    onPinChange: (String) -> Unit,
    biometricAvailable: Boolean,
    onBiometric: () -> Unit,
    onSubmit: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ArusRadius.ExtraLarge))
                .background(ArusTheme.colors.surface).padding(ArusSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            Text("Arus terkunci", style = ArusTheme.typography.headline)
            Text("Masukkan PIN enam digit untuk melanjutkan.")
            ArusTextField(pin, onPinChange, placeholder = "PIN", singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth(), testTag = "lock_pin_entry")
            error?.let { Text(it, color = ArusTheme.colors.error) }
            ArusButton(onClick = onSubmit, enabled = pin.length == 6, modifier = Modifier.fillMaxWidth()) { Text("Buka") }
            if (biometricAvailable) OutlinedArusButton(onClick = onBiometric, modifier = Modifier.fillMaxWidth()) { Text("Gunakan biometrik") }
        }
    }
}

@Composable
private fun CategoryManagement(database: ArusDatabase) {
    val categories = remember { mutableStateListOf<CategoryEntity>() }
    val wallets = remember { mutableStateListOf<com.illustravibes.arus.data.WalletEntity>() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var deleteSource by remember { mutableStateOf<CategoryEntity?>(null) }
    var moveTarget by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteWithoutMove by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var kind by rememberSaveable { mutableStateOf("EXPENSE") }
    var search by rememberSaveable { mutableStateOf("") }
    fun refresh() {
        scope.launch {
            val values = withContext(Dispatchers.IO) { database.categoryDao().allCategories() }
            categories.clear(); categories.addAll(values)
            val walletValues = withContext(Dispatchers.IO) { database.walletDao().activeWallets() }
            wallets.clear(); wallets.addAll(walletValues)
        }
    }
    LaunchedEffect(database) { refresh() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Text("Kategori", style = ArusTheme.typography.headline)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArusSegmentedButton("Pengeluaran", kind == "EXPENSE", { kind = "EXPENSE" }, Modifier.weight(1f))
            ArusSegmentedButton("Pemasukan", kind == "INCOME", { kind = "INCOME" }, Modifier.weight(1f))
        }
        ArusTextField(search, { search = it }, placeholder = "Cari kategori", modifier = Modifier.fillMaxWidth(), singleLine = true)
        ArusButton(onClick = { editing = null; error = null; dialogOpen = true }, kind = ArusButtonKind.Primary, modifier = Modifier.fillMaxWidth()) {
            ArusIcon(ArusIconCode.PLUS, fontSize = 14.sp)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Tambah kategori")
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                items(categories.filter { it.kind == kind && !it.isArchived && it.name.contains(search, ignoreCase = true) }, key = { it.id }) { category ->
                    var expanded by remember(category.id) { mutableStateOf(false) }
                ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = ArusSpacing.Medium) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.background(Color(category.colorArgb), RoundedCornerShape(50)).border(1.dp, ArusTheme.colors.border, RoundedCornerShape(50)).padding(6.dp)) {
                            ArusIcon(categoryIcon(category.icon), fontSize = 14.sp)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(category.name + if (category.isSystem) " · Sistem" else "", style = ArusTheme.typography.title)
                            if (category.isArchived) Text("Diarsipkan", color = ArusTheme.colors.mutedForeground)
                        }
                        androidx.compose.foundation.layout.Box(Modifier.clip(RoundedCornerShape(50)).clickable { expanded = true }.padding(8.dp)) { ArusIcon(ArusIconCode.ELLIPSIS) }
                        ArusDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ArusDropdownMenuItem(text = { Text("Edit") }, enabled = !category.isSystem, onClick = { editing = category; error = null; dialogOpen = true; expanded = false })
                            ArusDropdownMenuItem(text = { Text("Arsip") }, enabled = !category.isSystem, onClick = {
                                scope.launch { withContext(Dispatchers.IO) { database.categoryDao().archive(category.id) }; refresh() }
                                expanded = false
                            })
                            ArusDropdownMenuItem(text = { Text("Naik") }, enabled = !category.isSystem && categories.filter { it.kind == kind && !it.isArchived }.firstOrNull()?.id != category.id, onClick = {
                                val index = categories.filter { it.kind == kind && !it.isArchived }.indexOf(category)
                                val previous = categories.filter { it.kind == kind && !it.isArchived }.getOrNull(index - 1)
                                if (previous != null) scope.launch {
                                    withContext(Dispatchers.IO) { database.runInTransaction {
                                        database.categoryDao().updateSortOrder(category.id, previous.sortOrder)
                                        database.categoryDao().updateSortOrder(previous.id, category.sortOrder)
                                    } }
                                    refresh()
                                }
                                expanded = false
                            })
                            ArusDropdownMenuItem(text = { Text("Hapus") }, enabled = !category.isSystem, onClick = { deleteSource = category; deleteWithoutMove = false; expanded = false })
                        }
                    }
                }
            }
            item {
                Text("Kategori default dompet", style = ArusTheme.typography.title)
                wallets.filter { it.type != "CREDIT" }
                    .forEach { wallet ->
                        var expanded by remember(wallet.id) { mutableStateOf(false) }
                        Box {
                            ArusTextField(
                                value = categories.firstOrNull { it.id == wallet.defaultExpenseCategoryId }?.name ?: "Pilih kategori",
                                onValueChange = {}, readOnly = true, placeholder = wallet.name, modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                            )
                            ArusDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                categories.filter { it.kind == "EXPENSE" && !it.isArchived }.forEach { category ->
                                    ArusDropdownMenuItem(text = { Text(category.name) }, onClick = {
                                        scope.launch { withContext(Dispatchers.IO) { database.walletDao().updateDefaultExpenseCategory(wallet.id, category.id) }; refresh() }
                                        expanded = false
                                    })
                                }
                            }
                        }
                    }
            }
        }
    }
    if (dialogOpen) CategoryFormDialog(editing, kind, error, { error = it }, {
        val current = editing
        if (current != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    val dao = database.categoryDao()
                    if (dao.countName(current.kind, it.first, current.id) > 0) 0
                    else dao.editCustom(current.id, it.first, it.second, it.third)
                }
                if (saved == 0) error = "Kategori gagal disimpan. Nama mungkin sudah dipakai."
                else { dialogOpen = false; refresh() }
            }
        } else {
            scope.launch {
                val inserted = withContext(Dispatchers.IO) {
                    val dao = database.categoryDao()
                    if (dao.countName(kind, it.first) > 0) -1L else dao.insert(CategoryEntity(
                        it.first, kind, it.second, it.third, false, false, dao.allCategories().size))
                }
                if (inserted == -1L) error = "Nama kategori sudah dipakai."
                else { dialogOpen = false; refresh() }
            }
        }
    }, { dialogOpen = false })
    deleteSource?.let { source ->
        val candidates = categories.filter { it.kind == source.kind && it.id != source.id && !it.isArchived }
        LaunchedEffect(source.id) {
            val targets = withContext(Dispatchers.IO) { database.categoryDao().activeTransactionCount(source.id) }
            if (targets == 0 && database.categoryDao().walletReferenceCount(source.id) == 0) deleteWithoutMove = true
        }
        ArusDialog(onDismissRequest = { deleteSource = null },
            title = { Text("Hapus ${source.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pindahkan transaksi ke kategori lain, atau hapus kategori dari transaksi.")
                    candidates.forEach { candidate ->
                        OutlinedArusButton(onClick = { moveTarget = candidate }) {
                            Text((if (moveTarget?.id == candidate.id) "✓ " else "") + candidate.name)
                        }
                    }
                    OutlinedArusButton(onClick = { moveTarget = null; deleteWithoutMove = true }) { Text("Tanpa kategori") }
                    if (error != null) Text(error!!, color = ArusTheme.colors.error)
                }
            },
            confirmButton = { ArusButton(onClick = {
                if (!deleteWithoutMove && moveTarget == null) { error = "Pilih kategori tujuan atau Tanpa kategori."; return@ArusButton }
                scope.launch {
                    val failure = withContext(Dispatchers.IO) {
                        var message: String? = null
                        database.runInTransaction {
                            val dao = database.categoryDao()
                            val sourceNow = dao.findById(source.id)
                            if (sourceNow == null || sourceNow.isSystem) message = "Kategori sistem tidak dapat dihapus."
                            else {
                                moveTarget?.let { dao.moveTransactions(source.id, it.id) }
                                if (deleteWithoutMove) dao.clearTransactionCategories(source.id)
                                dao.clearWalletDefaults(source.id)
                                if (dao.walletReferenceCount(source.id) > 0) message = "Kategori masih dipakai sebagai default."
                                else if (dao.deleteCustom(source.id) != 1) message = "Kategori gagal dihapus."
                            }
                        }
                        message
                    }
                    if (failure != null) error = failure else { deleteSource = null; moveTarget = null; error = null; refresh() }
                }
            }) { Text("Konfirmasi hapus") } },
            dismissButton = { OutlinedArusButton(onClick = { deleteSource = null; moveTarget = null; error = null }) { Text("Batal") } })
    }
}

@Composable
private fun CategoryFormDialog(
    category: CategoryEntity?,
    initialKind: String,
    error: String?,
    onError: (String?) -> Unit,
    onSave: (Triple<String, String, Int>) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var icon by remember(category?.id) { mutableStateOf(category?.icon ?: "more_horiz") }
    var color by remember(category?.id) { mutableStateOf(category?.colorArgb?.toUInt()?.toString(16)?.uppercase() ?: "FF9E9E9E") }
    ArusDialog(onDismissRequest = onCancel,
        title = { Text(if (category == null) "Kategori baru" else "Edit kategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (category?.kind == "INCOME" || category == null && initialKind == "INCOME") "Pemasukan" else "Pengeluaran")
                ArusTextField(name, { name = it }, placeholder = "Nama", singleLine = true)
                ArusTextField(icon, { icon = it }, placeholder = "Ikon", singleLine = true)
                ArusTextField(color, { color = it.filter(Char::isLetterOrDigit).take(8) }, placeholder = "Warna ARGB (hex)", singleLine = true)
                if (error != null) Text(error, color = ArusTheme.colors.error)
            }
        },
        confirmButton = { ArusButton(onClick = {
            val parsed = color.toLongOrNull(16)?.toInt()
            if (name.isBlank() || parsed == null) onError("Isi nama dan warna ARGB hex yang valid.")
            else onSave(Triple(name.trim(), icon.ifBlank { "more_horiz" }, parsed))
        }) { Text("Simpan") } },
        dismissButton = { OutlinedArusButton(onClick = onCancel) { Text("Batal") } })
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun Onboarding(
    database: ArusDatabase,
    context: android.content.Context,
    onComplete: () -> Unit,
    onRestore: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(0) }
    var walletType by rememberSaveable { mutableStateOf("BANK") }
    var selectedPreset by rememberSaveable { mutableStateOf("BCA") }
    var walletName by rememberSaveable { mutableStateOf("BCA") }
    var initialBalance by rememberSaveable { mutableStateOf("0") }
    val categories = remember { mutableStateListOf<CategoryEntity>() }
    val selectedCategories = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var existingWalletCount by remember { mutableStateOf(0) }
    LaunchedEffect(database) {
        existingWalletCount = withContext(Dispatchers.IO) { database.walletDao().activeWalletCount() }
        withContext(Dispatchers.IO) { database.categoryDao().activeCategories() }.forEach {
            categories.add(it); selectedCategories.add(it.kind + ":" + it.name)
        }
    }
            Box(Modifier.fillMaxSize().background(ArusTheme.colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = ArusSpacing.Large), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(3) { index ->
            Box(Modifier.weight(1f).heightIn(min = 8.dp).clip(RoundedCornerShape(4.dp)).background(if (index <= step) ArusTheme.colors.primary else ArusTheme.colors.surfaceVariant))
                }
            }
            Text("Langkah ${minOf(step + 1, 3)} dari 3${if (step == 1) " · Dompet" else if (step == 2) " · Kategori" else ""}", style = ArusTheme.typography.label, color = ArusTheme.colors.mutedForeground)
            when (step) {
                0 -> {
                    ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = ArusSpacing.Large) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 180.dp).clip(RoundedCornerShape(ArusRadius.Large)).background(ArusTheme.colors.surfaceVariant), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                                ArusIcon(ArusIconCode.WALLET, fontSize = 36.sp)
                                Text("Semua arus uang, terlihat jelas", color = ArusTheme.colors.mutedForeground)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Catat arus uangmu", style = ArusTheme.typography.headline)
                        Text("Semua dompet, satu tempat. Data tersimpan di HP, bukan di server.", style = ArusTheme.typography.label, color = ArusTheme.colors.mutedForeground, textAlign = TextAlign.Center)
                    }
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        repeat(3) { index ->
                            Box(Modifier.size(if (index == 0) 10.dp else 8.dp).background(if (index == 0) ArusTheme.colors.primary else ArusTheme.colors.border, RoundedCornerShape(50)))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    ArusButton(onClick = { step = 1 }, kind = ArusButtonKind.Primary, modifier = Modifier.fillMaxWidth()) { Text("Mulai", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    OutlinedArusButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Lewati", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    OutlinedArusButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("Pulihkan backup", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
                1 -> {
                    Text(if (existingWalletCount == 0) "Siapkan dompet pertama" else "Tambah dompet lain?", style = ArusTheme.typography.headline)
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("BANK", "EWALLET", "CASH", "CREDIT").forEach { type ->
                            OutlinedArusButton(onClick = { walletType = type; selectedPreset = ""; walletName = "" }) {
                                Text(when(type) { "BANK" -> "Bank"; "EWALLET" -> "E-Wallet"; "CASH" -> "Tunai"; else -> "Kredit" })
                            }
                        }
                    }
                    val presets = remember(walletType) { readWalletPresets(context, walletType) }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                        items(presets) { preset ->
                            OutlinedArusButton(onClick = { selectedPreset = preset.id; walletName = preset.name }, modifier = Modifier.fillMaxWidth()) {
                                Text((if (selectedPreset == preset.id) "✓  " else "") + preset.name)
                            }
                        }
                        item {
                            ArusTextField(value = walletName, onValueChange = { walletName = it }, placeholder = "Nama dompet", modifier = Modifier.fillMaxWidth())
                            ArusTextField(value = initialBalance, onValueChange = { initialBalance = it.filter(Char::isDigit) }, placeholder = "Saldo awal (Rp)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        }
                    }
                    var walletError by rememberSaveable { mutableStateOf<String?>(null) }
                    walletError?.let { Text(it, color = ArusTheme.colors.error) }
                    ArusButton(onClick = {
                        val balance = initialBalance.toLongOrNull()
                        if (walletName.isBlank() || (balance == null && existingWalletCount == 0)) {
                            walletError = "Isi nama dompet dan saldo awal yang valid."
                            return@ArusButton
                        }
                        if (existingWalletCount > 0 && (walletName.isBlank() || balance == null)) {
                            step = 2
                            return@ArusButton
                        }
                        scope.launch {
                        val preset = presets.find { it.id == selectedPreset }
                        withContext(Dispatchers.IO) {
                            if (walletName.isNotBlank() && balance != null) {
                                database.walletDao().insert(WalletEntity(walletName.trim(), walletType,
                                    preset?.icon ?: "account_balance_wallet", preset?.color ?: 0xFF607D8B.toInt(),
                                    balance, walletType != "CREDIT", false, null))
                            }
                        }
                            existingWalletCount = withContext(Dispatchers.IO) { database.walletDao().activeWalletCount() }
                            step = if (existingWalletCount > 0) 2 else 1
                        }
                    }, enabled = (walletName.isNotBlank() && initialBalance.toLongOrNull() != null) || existingWalletCount > 0, modifier = Modifier.fillMaxWidth()) { Text(if (existingWalletCount == 0) "Lanjut" else "Lanjut tanpa tambah", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                }
                2 -> {
                    Text("Pilih kategori bawaan", style = ArusTheme.typography.headline)
                    Text("Pilih kategori yang kamu pakai. Kamu bisa mengubahnya nanti.")
                    androidx.compose.foundation.rememberScrollState().let { scrollState ->
                        Column(Modifier.weight(1f).verticalScroll(scrollState)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                categories.filterNot { it.isSystem }.forEach { category ->
                                    val key = category.kind + ":" + category.name
                                    ArusButton(
                                        onClick = {
                                            if (selectedCategories.contains(key)) selectedCategories.remove(key)
                                            else selectedCategories.add(key)
                                        },
                                        kind = if (selectedCategories.contains(key)) ArusButtonKind.Secondary else ArusButtonKind.Outline,
                                        selected = selectedCategories.contains(key),
                                    ) { Text(category.name) }
                                }
                            }
                        }
                    }
                    ArusButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val selected = selectedCategories.toSet()
                                val db = database.openHelper.writableDatabase
                                db.execSQL("UPDATE categories SET isArchived = 1")
                                selected.forEach { key ->
                                    val split = key.indexOf(':')
                                    val kind = key.substring(0, split); val name = key.substring(split + 1)
                                    db.execSQL("UPDATE categories SET isArchived = 0 WHERE kind = ? AND name = ?", arrayOf(kind, name))
                                }
                            }
                            onComplete()
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Lanjut") }
                    OutlinedArusButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Lewati keamanan untuk nanti") }
                    OutlinedArusButton(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text("Tambah dompet lain") }
                }
                else -> {
                    Text("Keamanan", style = ArusTheme.typography.headline)
                    Text("Kamu bisa mengatur PIN dan biometrik nanti di Pengaturan.")
                    Spacer(Modifier.weight(1f))
                    ArusButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Lanjut ke Arus") }
                    OutlinedArusButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Lewati keamanan") }
                }
            }
        }
    }
}

private data class WalletPreset(val id: String, val name: String, val icon: String, val color: Int)

private fun readWalletPresets(context: android.content.Context, type: String): List<WalletPreset> {
    return try {
        val json = context.assets.open("wallet-presets.json").bufferedReader().use { it.readText() }
        val presets = org.json.JSONObject(json).getJSONArray("presets")
        (0 until presets.length()).map { presets.getJSONObject(it) }
            .filter { it.optString("type") == type }
            .map { WalletPreset(it.getString("id"), it.getString("name"), it.getString("icon"),
                it.optString("colorArgb", "FF607D8B").toLong(16).toInt()) }
    } catch (_: Exception) { emptyList() }
}
