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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.illustravibes.arus.ui.designsystem.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.LaunchedEffect
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
import com.illustravibes.arus.domain.TransactionType
import com.illustravibes.arus.domain.ReceiptOcrParser
import com.illustravibes.arus.domain.ReceiptOcrReview
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import com.illustravibes.arus.ui.designsystem.ArusNavigationBar
import com.illustravibes.arus.ui.designsystem.ArusNavigationItem
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconCode
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
import com.illustravibes.arus.ui.screens.WalletListScreen
import com.illustravibes.arus.ui.screens.WalletFormScreen
import com.illustravibes.arus.ui.screens.WalletDetailScreen
import com.illustravibes.arus.ui.screens.CategoryListScreen
import com.illustravibes.arus.ui.screens.CategoryFormScreen
import com.illustravibes.arus.ui.screens.DeleteCategoryDialog
import com.illustravibes.arus.ui.screens.BudgetScreen
import com.illustravibes.arus.ui.screens.BudgetFormScreen
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
                var walletDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
                var walletEditingId by rememberSaveable { mutableStateOf<Long?>(null) }
                var categoryEditingId by rememberSaveable { mutableStateOf<Long?>(null) }
                val walletEditing by androidx.compose.runtime.produceState<WalletEntity?>(null, activeDatabase, walletEditingId) {
                    value = withContext(Dispatchers.IO) { walletEditingId?.let { activeDatabase.walletDao().findById(it) } }
                }
                val categoryEditing by androidx.compose.runtime.produceState<CategoryEntity?>(null, activeDatabase, categoryEditingId) {
                    value = withContext(Dispatchers.IO) { categoryEditingId?.let { activeDatabase.categoryDao().findById(it) } }
                }
                var categoryInitialKind by rememberSaveable { mutableStateOf("EXPENSE") }
                var categoryDeleteSource by remember { mutableStateOf<CategoryEntity?>(null) }
                var categoryDeleteOptions by remember { mutableStateOf(emptyList<CategoryEntity>()) }
                var categoryRefreshKey by rememberSaveable { mutableStateOf(0) }
                var budgetRefreshKey by rememberSaveable { mutableStateOf(0) }
                var budgetFormOpen by rememberSaveable { mutableStateOf(false) }
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
                                else if (selectedDestination == "Dompet") WalletListScreen(
                                    database = activeDatabase,
                                    onBackClick = { selectedDestination = "Beranda" },
                                    onAddWallet = { walletEditingId = null; selectedDestination = "FormDompet" },
                                    onEditWallet = { walletEditingId = it.id; selectedDestination = "FormDompet" },
                                    onWalletDetail = { walletDetailId = it; selectedDestination = "DetailDompet" },
                                    onReconciliation = { reconciliationWallet = it },
                                )
                                else if (selectedDestination == "FormDompet") {
                                    val target = walletEditing?.takeIf { it.id == walletEditingId }
                                    if (walletEditingId == null || target != null) WalletFormScreen(
                                        database = activeDatabase,
                                        wallet = target,
                                        onDismiss = { walletEditingId = null; selectedDestination = "Dompet" },
                                        onSaved = { walletEditingId = null; selectedDestination = "Dompet" },
                                    ) else Text("Memuat dompet…")
                                }
                                else if (selectedDestination == "DetailDompet") {
                                    val walletId = walletDetailId
                                    if (walletId == null) selectedDestination = "Dompet"
                                    else WalletDetailScreen(
                                        database = activeDatabase,
                                        walletId = walletId,
                                        onDismiss = { walletDetailId = null; selectedDestination = "Dompet" },
                                        onTransactionClick = { historySelected = it },
                                        onReconciliation = { reconciliationWallet = it },
                                        onEditWallet = { walletEditingId = it.id; selectedDestination = "FormDompet" },
                                    )
                                }
                                else if (selectedDestination == "Kategori") CategoryListScreen(
                                    database = activeDatabase,
                                    onBackClick = { selectedDestination = "Lainnya" },
                                    refreshKey = categoryRefreshKey,
                                    onAddCategory = { kind -> categoryInitialKind = kind; categoryEditingId = null; selectedDestination = "FormKategori" },
                                    onEditCategory = { categoryEditingId = it.id; categoryInitialKind = it.kind; selectedDestination = "FormKategori" },
                                    onDeleteCategory = { category ->
                                        scope.launch {
                                            categoryDeleteOptions = withContext(Dispatchers.IO) { activeDatabase.categoryDao().allCategories() }
                                            categoryDeleteSource = category
                                        }
                                    },
                                )
                                else if (selectedDestination == "FormKategori") {
                                    val target = categoryEditing?.takeIf { it.id == categoryEditingId }
                                    if (categoryEditingId == null || target != null) CategoryFormScreen(
                                        database = activeDatabase,
                                        category = target,
                                        initialKind = categoryInitialKind,
                                        onDismiss = { categoryEditingId = null; selectedDestination = "Kategori" },
                                        onSaved = { categoryRefreshKey++; categoryEditingId = null; selectedDestination = "Kategori" },
                                    ) else Text("Memuat kategori…")
                                }
                                else if (selectedDestination == "Anggaran") {
                                    if (budgetFormOpen) BudgetFormScreen(
                                        database = activeDatabase,
                                        onDismiss = { budgetFormOpen = false },
                                        onSaved = { budgetRefreshKey++; budgetFormOpen = false },
                                    ) else BudgetScreen(
                                        database = activeDatabase,
                                        onBackClick = { selectedDestination = "Lainnya" },
                                        onAddBudget = { budgetFormOpen = true },
                                        refreshKey = budgetRefreshKey,
                                    )
                                }
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
                                else if (selectedDestination == "Lainnya") {
                                     Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                                        Text("Lainnya", style = ArusTheme.typography.headline)
                                        ArusButton(onClick = { lockSettings = true }, kind = ArusButtonKind.Outline) { Text("Pengaturan keamanan") }
                                         ArusButton(onClick = { selectedDestination = "Anggaran" }, kind = ArusButtonKind.Outline) { Text("Anggaran") }
                                         ArusButton(onClick = { selectedDestination = "Laporan" }, kind = ArusButtonKind.Outline) { Text("Laporan") }
                                          ArusButton(onClick = { selectedDestination = "Rutin" }, kind = ArusButtonKind.Outline) { Text("Transaksi rutin") }
                                          ArusButton(onClick = { selectedDestination = "Utang-Piutang" }, kind = ArusButtonKind.Outline) { Text("Utang-Piutang") }
                                           ArusButton(onClick = { selectedDestination = "Group Order" }, kind = ArusButtonKind.Outline) { Text("Group Order") }
                                          ArusButton(onClick = { selectedDestination = "Kategori" }, kind = ArusButtonKind.Outline) { Text("Kategori") }
                                           ArusButton(onClick = { selectedDestination = "Pengaturan" }, kind = ArusButtonKind.Outline) { Text("Pengaturan") }
                                           ArusButton(onClick = { selectedDestination = "Ekspor data" }, kind = ArusButtonKind.Outline) { Text("Ekspor data") }
                                           ArusButton(onClick = { selectedDestination = "Backup & Restore" }, kind = ArusButtonKind.Outline) { Text("Backup & Restore") }
                                     }
                                }
                                else HomeScreen(
                                    database = activeDatabase,
                                    onNetWorthDetail = { },
                                    onWallets = { selectedDestination = "Dompet" },
                                    onWallet = { walletDetailId = it; selectedDestination = "DetailDompet" },
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
                categoryDeleteSource?.let { source ->
                    DeleteCategoryDialog(
                        database = activeDatabase,
                        sourceCategory = source,
                        allCategories = categoryDeleteOptions,
                        onDismiss = { categoryDeleteSource = null },
                        onDeleted = { categoryRefreshKey++; categoryDeleteSource = null },
                    )
                }
                if (selectedDestination == "Laporan") {
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

internal fun formatRupiah(amount: Long): String = ArusFormatting.formatRupiah(amount)

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
