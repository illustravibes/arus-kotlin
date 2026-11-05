package com.illustravibes.arus.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.illustravibes.arus.data.PresetSeeder
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.BalanceRepository
import com.illustravibes.arus.data.TransactionDao
import com.illustravibes.arus.domain.TransactionDraft
import com.illustravibes.arus.domain.TransactionType
import com.illustravibes.arus.domain.BalanceReconciliation
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
import java.text.NumberFormat
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
        )
        setContent {
            ArusTheme {
                val context = LocalContext.current
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
                    } catch (_: Exception) { }
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
                    })
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
                var undoTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
                var undoDeletedAt by rememberSaveable { mutableStateOf<Long?>(null) }
                var undoDeadline by rememberSaveable { mutableStateOf(0L) }
                var showUndo by rememberSaveable { mutableStateOf(false) }
                var historySelected by rememberSaveable { mutableStateOf<Long?>(null) }
                var budgetManagementOpen by rememberSaveable { mutableStateOf(false) }
                var reportOpen by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(undoTransactionId, undoDeadline) {
                    if (undoTransactionId != null) {
                        showUndo = true
                        delay((undoDeadline - System.currentTimeMillis()).coerceAtLeast(0L))
                        showUndo = false
                        undoTransactionId = null
                        undoDeletedAt = null
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(ArusTheme.colors.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
                            ) {
                if (selectedDestination == "Rutin") RecurringManagement(activeDatabase)
                else if (selectedDestination == "Riwayat") TransactionHistory(activeDatabase, onSelect = { historySelected = it }, onTransfer = { transferOpen = true })
                                else if (selectedDestination == "Dompet") WalletManagement(database = activeDatabase)
                                else if (selectedDestination == "Lainnya") {
                                    Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                                        Text("Lainnya", style = ArusTheme.typography.headline)
                                        ArusButton(onClick = { lockSettings = true }, kind = ArusButtonKind.Outline) { Text("Pengaturan keamanan") }
                                         ArusButton(onClick = { budgetManagementOpen = !budgetManagementOpen }, kind = ArusButtonKind.Outline) { Text(if (budgetManagementOpen) "Tutup anggaran" else "Anggaran") }
                                         if (budgetManagementOpen) BudgetManagement(database = activeDatabase)
                                         ArusButton(onClick = { reportOpen = !reportOpen }, kind = ArusButtonKind.Outline) { Text(if (reportOpen) "Tutup laporan" else "Laporan") }
                                         if (reportOpen) ReportScreen(database = activeDatabase)
                                         ArusButton(onClick = { selectedDestination = "Rutin" }, kind = ArusButtonKind.Outline) { Text("Transaksi rutin") }
                                        Text("Kategori", style = ArusTheme.typography.headline)
                                        CategoryManagement(database = activeDatabase)
                                    }
                                }
                                else HomeOverview(activeDatabase,
                                    onNetWorth = { },
                                    onWallets = { selectedDestination = "Dompet" },
                                    onWallet = { _ -> selectedDestination = "Dompet" },
                                    onTransaction = { historySelected = it },
                                    onExpense = { transactionType = "EXPENSE" },
                                    onIncome = { transactionType = "INCOME" },
                                    onTransfer = { transferOpen = true },
                                )
                            }
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
                        ArusNavigationBar {
                            listOf("Beranda", "Riwayat", "Dompet", "Lainnya").forEach { destination ->
                                ArusNavigationItem(
                                    selected = selectedDestination == destination,
                                    onClick = { selectedDestination = destination },
                                    icon = {
                                        ArusIcon(when (destination) {
                                            "Beranda" -> ArusIconCode.HOUSE
                                            "Riwayat" -> ArusIconCode.CLOCK
                                            "Dompet" -> ArusIconCode.WALLET
                                            else -> ArusIconCode.ELLIPSIS
                                        })
                                    },
                                    label = { Text(destination) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                transactionType?.let { type ->
                    TransactionQuickEntry(
                        activeDatabase,
                        TransactionType.valueOf(type),
                        onDismiss = { transactionType = null },
                        onTypeChange = { transactionType = it.name },
                    ) { id, dismiss ->
                        undoTransactionId = id
                        undoDeletedAt = null
                        undoDeadline = System.currentTimeMillis() + 5_000
                        if (dismiss) transactionType = null
                    }
                }
                if (transferOpen) TransferEntry(activeDatabase, onDismiss = { transferOpen = false })
                historySelected?.let { id -> TransactionDetails(activeDatabase, id, onDismiss = { historySelected = null }) }
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
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag("id-ID")) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp).testTag("home_overview_list"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Arus", style = ArusTheme.typography.headline)
                ArusCard(modifier = Modifier.fillMaxWidth(), onClick = { detailOpen = true }, contentPadding = 20.dp) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Total kekayaan", style = ArusTheme.typography.body.copy(color = ArusTheme.colors.mutedForeground))
                        Text(formatRupiah(summary?.netWorthRupiah ?: 0L), style = ArusTheme.typography.display)
                        Text("Aset ${formatRupiah(summary?.assetsRupiah ?: 0L)} · Kewajiban ${formatRupiah(summary?.creditLiabilitiesRupiah ?: 0L)}", style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground))
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
                            ArusButton(onClick = { reconciliationWallet = wallet; actualBalance = ""; reconciliationError = null; resolutionParts = emptyList(); resolutionKind = "UNTRACKED" }, kind = ArusButtonKind.Ghost) { Text("Cocokkan saldo") }
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
                        Text("${transaction.type} · ${java.time.Instant.ofEpochMilli(transaction.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)}", style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground))
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
    }
    if (detailOpen) ArusDialog(onDismissRequest = { detailOpen = false }, title = { Text("Rincian net worth") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aset dompet: ${formatRupiah(summary?.assetsRupiah ?: 0L)}")
            Text("Kewajiban kredit: ${formatRupiah(summary?.creditLiabilitiesRupiah ?: 0L)}")
            Text("Net worth: ${formatRupiah(summary?.netWorthRupiah ?: 0L)}")
        }
    }, confirmButton = { OutlinedArusButton(onClick = { detailOpen = false }) { Text("Tutup") } })
    reconciliationWallet?.let { wallet ->
        val recorded = walletBalances[wallet.id] ?: 0L
        val actual = actualBalance.toLongOrNull()?.takeIf { it >= 0L }
        ArusDialog(onDismissRequest = { reconciliationWallet = null }, title = { Text("Cocokkan saldo") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dompet · ${wallet.name}")
                Text("Saldo di Arus · ${formatRupiah(recorded)}")
                ArusTextField(actualBalance, { actualBalance = it.filter(Char::isDigit) }, placeholder = "Saldo aktual", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().testTag("actual_balance_input"))
                if (actual != null) {
                    val difference = runCatching { BalanceReconciliation(recorded, actual).differenceRupiah }.getOrNull()
                    difference?.let { differenceRupiah ->
                        Text("Selisih · ${formatRupiah(differenceRupiah)}")
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("UNTRACKED" to "Tak terlacak", "MISSED" to "Transaksi terlupa", "OPENING_BALANCE" to "Saldo awal salah").forEach { (value, label) ->
                                ArusButton(onClick = { resolutionKind = value }, kind = if (resolutionKind == value) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(label) }
                            }
                        }
                        if (resolutionKind == "MISSED") {
                            var transactionType by remember(wallet.id, differenceRupiah) { mutableStateOf(if (differenceRupiah >= 0) "INCOME" else "EXPENSE") }
                            var amount by remember(wallet.id, differenceRupiah) { mutableStateOf(kotlin.math.abs(differenceRupiah).toString()) }
                            ArusButton(onClick = { transactionType = if (transactionType == "INCOME") "EXPENSE" else "INCOME" }, kind = ArusButtonKind.Outline) { Text(if (transactionType == "INCOME") "Pemasukan" else "Pengeluaran") }
                            ArusTextField(amount, { amount = it.filter(Char::isDigit) }, placeholder = "Nominal transaksi terlupa", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            if (amount.toLongOrNull() != kotlin.math.abs(differenceRupiah)) Text("Nominal transaksi harus sama dengan selisih.")
                            val categoryId = database.categoryDao().activeCategories().firstOrNull { it.kind == transactionType }?.id
                            resolutionParts = listOf(TransactionDao.ReconciliationPart(transactionType, amount.toLongOrNull() ?: 0L, null, categoryId, "Transaksi terlupa saat rekonsiliasi"))
                        } else if (resolutionKind == "OPENING_BALANCE") {
                            resolutionParts = if (differenceRupiah == 0L) emptyList() else listOf(TransactionDao.ReconciliationPart("ADJUSTMENT", kotlin.math.abs(differenceRupiah), if (differenceRupiah > 0) "PLUS" else "MINUS", null, "Koreksi saldo awal"))
                        } else {
                            var adjustmentAmount by remember(wallet.id, differenceRupiah) { mutableStateOf(kotlin.math.abs(differenceRupiah).toString()) }
                            ArusTextField(adjustmentAmount, { adjustmentAmount = it.filter(Char::isDigit) }, placeholder = "Nominal selisih tak terlacak", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                            if (adjustmentAmount.toLongOrNull() != kotlin.math.abs(differenceRupiah)) Text("Nominal penyesuaian harus sama dengan selisih.")
                            resolutionParts = if (differenceRupiah == 0L) emptyList() else listOf(TransactionDao.ReconciliationPart("ADJUSTMENT", adjustmentAmount.toLongOrNull() ?: 0L, if (differenceRupiah > 0) "PLUS" else "MINUS", null, "Selisih tak terlacak"))
                        }
                    }
                }
                reconciliationError?.let { Text(it) }
            }
        }, confirmButton = {
            OutlinedArusButton(onClick = { reconciliationWallet = null }) { Text("Tutup") }
            if (actual != null) OutlinedArusButton(onClick = {
                val expectedDelta = runCatching { BalanceReconciliation(recorded, actual).differenceRupiah }.getOrDefault(0L)
                if (expectedDelta == 0L) reconciliationError = "Saldo sudah cocok."
                else if (resolutionParts.isEmpty() || resolutionParts.any { it.amountRupiah <= 0L } || (resolutionKind == "MISSED" && resolutionParts.any { it.categoryId == null })) reconciliationError = "Periksa rincian resolusi."
                else scope.launch {
                    val result = runCatching { withContext(Dispatchers.IO) { database.transactionDao().resolveReconciliation(wallet.id, actual, System.currentTimeMillis(), resolutionKind, resolutionParts) } }
                    result.onSuccess {
                        walletBalances[wallet.id] = actual
                        latest.clear(); latest.addAll(withContext(Dispatchers.IO) { database.transactionDao().latestTransactions() })
                        reconciliationError = "Saldo cocok: ${formatRupiah(actual)}"
                    }.onFailure { reconciliationError = it.message ?: "Resolusi gagal." }
                }
            }) { Text("Selesaikan") }
        })
    }
}

internal fun formatRupiah(amount: Long): String = "Rp ${NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(amount)}"

@Composable
private fun TransactionHistory(database: ArusDatabase, onSelect: (Long) -> Unit, onTransfer: () -> Unit) {
    val pager = remember(database) {
        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) { database.transactionDao().transactionHistory() }
    }
    val transactions = pager.flow.collectAsLazyPagingItems()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Riwayat", style = ArusTheme.typography.headline)
                ArusButton(onClick = onTransfer, kind = ArusButtonKind.Outline) { Text("Transfer") }
            }
        }
        items(count = transactions.itemCount, key = transactions.itemKey { it.id }) { index ->
            val item = transactions[index] ?: return@items
            val day = java.time.Instant.ofEpochMilli(item.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            if (index == 0 || transactions.peek(index - 1)?.let {
                    java.time.Instant.ofEpochMilli(it.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() != day
                } != false) {
                Text(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag("id-ID"))), style = ArusTheme.typography.title, modifier = Modifier.padding(top = ArusSpacing.Medium))
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
            listOf(row, wallet, category, database.walletDao().activeWallets(), database.categoryDao().activeCategories())
        }
        transaction = data[0] as? TransactionEntity
        walletName = (data[1] as? WalletEntity)?.name.orEmpty()
        categoryName = (data[2] as? CategoryEntity)?.name.orEmpty()
        wallets = data[3] as List<WalletEntity>
        categories = data[4] as List<CategoryEntity>
    }
    LaunchedEffect(transactionId) { reload() }
    val row = transaction
    if (row == null) return
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 760.dp).padding(ArusSpacing.Medium).verticalScroll(rememberScrollState()).clip(RoundedCornerShape(ArusRadius.ExtraLarge)).background(ArusTheme.colors.surface).border(1.dp, ArusTheme.colors.border, RoundedCornerShape(ArusRadius.ExtraLarge)).padding(ArusSpacing.Large), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
            Text("Rincian transaksi", style = ArusTheme.typography.headline)
            Text(formatRupiah(row.amountRupiah), style = ArusTheme.typography.display)
            ArusSegmentedButton(row.type.name, true, {}, Modifier.fillMaxWidth())
            Text(java.time.Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag("id-ID"))))
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
            date = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(row.occurredAtEpochMillis), ZoneId.systemDefault()).toString()
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

@Composable
private fun WalletManagement(database: ArusDatabase) {
    val wallets = remember { mutableStateListOf<com.illustravibes.arus.data.WalletEntity>() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<com.illustravibes.arus.data.WalletEntity?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var archiveTarget by remember { mutableStateOf<com.illustravibes.arus.data.WalletEntity?>(null) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    fun refresh() {
        scope.launch {
            val values = withContext(Dispatchers.IO) { database.walletDao().activeWallets() }
            wallets.clear(); wallets.addAll(values)
        }
    }
    LaunchedEffect(database) { refresh() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Text("Dompet", style = ArusTheme.typography.headline)
        ArusButton(onClick = { editing = null; formError = null; showForm = true }, kind = ArusButtonKind.Primary, modifier = Modifier.fillMaxWidth()) {
            ArusIcon(ArusIconCode.PLUS, fontSize = 14.sp)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Tambah dompet")
        }
        if (wallets.isEmpty()) ArusCard(Modifier.fillMaxWidth()) { Text("Belum ada dompet. Tambahkan dompet untuk mencatat saldo.", color = ArusTheme.colors.mutedForeground) }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            items(wallets, key = { it.id }) { wallet ->
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
        val wallet = wallets.find { it.id == id }
        if (wallet != null) ArusDialog(onDismissRequest = { detailId = null },
            title = { Text(wallet.name) },
            text = { Text("${wallet.type}\nSaldo awal: ${formatRupiah(wallet.initialBalanceRupiah)}\nTotal transaksi tercatat: ${formatRupiah(database.walletDao().transactionTotal(wallet.id))}") },
            confirmButton = { ArusButton(onClick = { detailId = null }) { Text("Tutup") } })
    }
}

private fun saveOrdinaryTransaction(database: ArusDatabase, draft: TransactionDraft, allowNegative: Boolean): Long {
    val entity = TransactionEntity(
        draft.type, draft.walletId, draft.amountRupiah, null, draft.categoryId,
        draft.occurredAtEpochMillis, draft.note?.trim()?.ifEmpty { null },
        draft.merchant?.trim()?.ifEmpty { null }, null,
    )
    return database.transactionDao().insertOrdinary(entity, allowNegative)
}

@Composable
private fun TransferEntry(database: ArusDatabase, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
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
        val values = withContext(Dispatchers.IO) { database.walletDao().activeWallets() }
        wallets.clear(); wallets.addAll(values)
        val lastId = context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).getLong("last_wallet_id", -1L)
        sourceId = wallets.firstOrNull { it.id == lastId }?.id ?: wallets.firstOrNull()?.id
        destinationId = wallets.firstOrNull { it.id != sourceId }?.id
    }
    val occurred = runCatching { LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().heightIn(max = 760.dp).verticalScroll(rememberScrollState()).padding(ArusSpacing.Medium).clip(RoundedCornerShape(ArusRadius.ExtraLarge)).background(ArusTheme.colors.surface).border(1.dp, ArusTheme.colors.border, RoundedCornerShape(ArusRadius.ExtraLarge)).padding(ArusSpacing.Large), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
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

@Composable
private fun TransactionQuickEntry(
    database: ArusDatabase,
    type: TransactionType,
    onDismiss: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onSaved: (Long, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val wallets = remember { mutableStateListOf<WalletEntity>() }
    val categories = remember { mutableStateListOf<CategoryEntity>() }
    var walletId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var negativeWarning by rememberSaveable { mutableStateOf(false) }
    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    var showMetadata by rememberSaveable { mutableStateOf(false) }
    var walletPickerOpen by rememberSaveable { mutableStateOf(false) }
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    LaunchedEffect(database, type) {
        val values = withContext(Dispatchers.IO) {
            database.walletDao().activeWallets() to database.categoryDao().activeCategories(
                if (type == TransactionType.INCOME) "INCOME" else "EXPENSE",
            )
        }
        wallets.clear(); wallets.addAll(values.first)
        categories.clear(); categories.addAll(values.second)
        val lastWalletId = context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).getLong("last_wallet_id", -1L)
        walletId = wallets.firstOrNull { it.id == lastWalletId }?.id ?: wallets.firstOrNull()?.id
        categoryId = categories.firstOrNull()?.id
        withContext(Dispatchers.Main) {
            withFrameNanos { }
            amountFocus.requestFocus()
            keyboard?.show()
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
            Text("Tambah transaksi", style = ArusTheme.typography.headline)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small), modifier = Modifier.fillMaxWidth()) {
                ArusSegmentedButton("Pengeluaran", type == TransactionType.EXPENSE, { onTypeChange(TransactionType.EXPENSE) }, Modifier.weight(1f))
                ArusSegmentedButton("Pemasukan", type == TransactionType.INCOME, { onTypeChange(TransactionType.INCOME) }, Modifier.weight(1f))
            }
            ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Column {
                    Text("Nominal", style = ArusTheme.typography.label, color = ArusTheme.colors.mutedForeground)
                    ArusTextField(amount, { amount = it.filter(Char::isDigit); error = null; negativeWarning = false }, placeholder = "Rp 0", singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().focusRequester(amountFocus).testTag("transaction_amount"))
                }
            }
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Sering dipakai", style = ArusTheme.typography.title, modifier = Modifier.weight(1f))
                ArusButton(onClick = { categoryPickerOpen = true }, kind = ArusButtonKind.Outline) { Text("Pilih kategori") }
            }
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                val quickCategories = if (showAllCategories) categories else categories.take(5)
                quickCategories.forEach { category ->
                    ArusButton(onClick = { categoryId = category.id }, kind = if (category.id == categoryId) ArusButtonKind.Secondary else ArusButtonKind.Outline) { Text(category.name) }
                }
                if (categories.size > 5) ArusButton(onClick = { showAllCategories = !showAllCategories }, kind = ArusButtonKind.Ghost) { Text(if (showAllCategories) "Tutup" else "Lainnya ›") }
            }
            ArusButton(onClick = { walletPickerOpen = true }, kind = ArusButtonKind.Outline, modifier = Modifier.fillMaxWidth()) {
                Text("Dompet: ${selectedWallet?.name ?: "Pilih dompet"}")
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ArusButton(onClick = {
                    val initial = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                    DatePickerDialog(context, { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }, kind = ArusButtonKind.Outline) { Text("Tanggal: $date") }
                ArusButton(onClick = { showMetadata = !showMetadata }, kind = ArusButtonKind.Ghost) { Text(if (showMetadata) "− Rincian" else "+ Catatan / merchant") }
            }
            if (showMetadata) {
                ArusTextField(note, { note = it }, placeholder = "Catatan (opsional)", singleLine = true, modifier = Modifier.fillMaxWidth())
                ArusTextField(merchant, { merchant = it }, placeholder = "Merchant (opsional)", singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (dateIsFuture) Text("Terjadwal · belum masuk saldo", color = ArusTheme.colors.mutedForeground)
            error?.let { Text(it, color = ArusTheme.colors.error) }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedArusButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Batal") }
                    ArusButton(onClick = { submitTransaction(scope, type, walletId, categoryId, amount, dateEpoch, note, merchant, negativeWarning, { negativeWarning = it }, { error = it }, database) { savedId ->
                    context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE).edit().putLong("last_wallet_id", walletId ?: -1L).apply()
                    amount = ""; note = ""; merchant = ""; error = null; negativeWarning = false
                    amountFocus.requestFocus(); keyboard?.show()
                    onSaved(savedId, false)
                } }, modifier = Modifier.weight(1f)) { Text("Simpan & tambah lagi") }
                ArusButton(onClick = { submitTransaction(scope, type, walletId, categoryId, amount, dateEpoch, note, merchant, negativeWarning, { negativeWarning = it }, { error = it }, database) { savedId ->
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
                saveOrdinaryTransaction(database, draft, negativeConfirmed)
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
private fun Onboarding(database: ArusDatabase, context: android.content.Context, onComplete: () -> Unit) {
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
                        Box(Modifier.fillMaxWidth().heightIn(min = 300.dp).clip(RoundedCornerShape(ArusRadius.Large)).background(ArusTheme.colors.surfaceVariant), contentAlignment = Alignment.Center) {
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
                    OutlinedArusButton(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) { Text("Lewati keamanan untuk nanti") }
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
