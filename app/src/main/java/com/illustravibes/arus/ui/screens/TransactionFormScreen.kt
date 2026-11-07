package com.illustravibes.arus.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BalanceRepository
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.TransactionDao
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.TransactionSplitDraft
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.domain.ReceiptOcrParser
import com.illustravibes.arus.domain.ReceiptOcrReview
import com.illustravibes.arus.domain.TransactionDraft
import com.illustravibes.arus.domain.TransactionSplitCalculator
import com.illustravibes.arus.domain.TransactionType
import com.illustravibes.arus.ui.ArusFormatting
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusAmountInput
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusChip
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSegmentedControl
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * Split item representation during editing.
 */
data class EditableSplitItem(
    val id: Long,
    val categoryId: Long?,
    val value: String,
)

/**
 * S-31 Form Transaksi: Layar input transaksi lengkap dengan top bar, segmented pengeluaran/pemasukan,
 * nominal besar di atas, kategori sering dipakai + picker sheet, dompet card + picker sheet,
 * tanggal, catatan, dan tombol Simpan pill di bawah.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionFormScreen(
    database: ArusDatabase,
    initialType: TransactionType,
    onDismiss: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    initialReceipt: ReceiptOcrReview? = null,
    receiptAttachmentId: Long? = null,
    onSaved: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = rememberArusHaptics()

    val wallets = remember { mutableStateListOf<WalletEntity>() }
    val walletBalances = remember { mutableStateMapOf<Long, Long>() }
    val categories = remember { mutableStateListOf<CategoryEntity>() }

    var currentType by rememberSaveable { mutableStateOf(initialType.name) }
    val type = TransactionType.valueOf(currentType)

    var walletId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var amountText by rememberSaveable { mutableStateOf(initialReceipt?.totalRupiah?.toString().orEmpty()) }
    var note by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf(initialReceipt?.merchant.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(initialReceipt?.date?.toString() ?: LocalDate.now().toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var negativeWarning by rememberSaveable { mutableStateOf(false) }
    var showAllCategories by rememberSaveable { mutableStateOf(false) }
    var showMetadata by rememberSaveable { mutableStateOf(initialReceipt?.merchant != null) }
    var showOcrText by rememberSaveable { mutableStateOf(false) }

    // Split state
    var splitEnabled by rememberSaveable { mutableStateOf(false) }
    var splitEditorOpen by rememberSaveable { mutableStateOf(false) }
    var splitValueMode by rememberSaveable { mutableStateOf("NOMINAL") }
    var splitEditorError by rememberSaveable { mutableStateOf<String?>(null) }
    var splitCategoryPickerRowId by rememberSaveable { mutableStateOf<Long?>(null) }
    val splitRows = remember { mutableStateListOf<EditableSplitItem>() }
    var nextSplitRowId by remember { mutableStateOf(1L) }
    var hintedCategoryId by remember { mutableStateOf<Long?>(null) }

    // Sheet pickers
    var walletPickerOpen by rememberSaveable { mutableStateOf(false) }
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }

    val amountFocus = remember { FocusRequester() }

    LaunchedEffect(database, currentType, initialReceipt) {
        val kindStr = if (type == TransactionType.INCOME) "INCOME" else "EXPENSE"
        val values = withContext(Dispatchers.IO) {
            val w = database.walletDao().activeWallets()
            val wb = w.associate { wallet ->
                wallet.id to (BalanceRepository(database).balance(wallet.id, System.currentTimeMillis()) ?: 0L)
            }
            val c = database.categoryDao().activeCategories(kindStr)
            Triple(w, wb, c)
        }
        wallets.clear()
        wallets.addAll(values.first)
        walletBalances.clear()
        walletBalances.putAll(values.second)
        categories.clear()
        categories.addAll(values.third)

        val lastWalletId = context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE)
            .getLong("last_wallet_id", -1L)
        walletId = wallets.firstOrNull { it.id == lastWalletId }?.id ?: wallets.firstOrNull()?.id

        val suggestion = withContext(Dispatchers.IO) {
            initialReceipt?.merchant?.let { merchantName ->
                database.merchantCategoryHintDao().suggestedCategoryId(ReceiptOcrParser.normalizeMerchant(merchantName))
            }
        }
        hintedCategoryId = categories.firstOrNull { it.id == suggestion }?.id
        categoryId = hintedCategoryId ?: categories.firstOrNull()?.id

        if (initialReceipt == null) {
            withContext(Dispatchers.Main) {
                withFrameNanos { }
                amountFocus.requestFocus()
                keyboard?.show()
            }
        }
    }

    fun beginSplitEditing() {
        if (!splitEnabled) {
            splitRows.clear()
            splitRows += EditableSplitItem(nextSplitRowId++, categoryId, amountText.filter(Char::isDigit))
            splitRows += EditableSplitItem(nextSplitRowId++, null, "")
            splitValueMode = "NOMINAL"
        }
        splitEditorError = null
        splitEditorOpen = true
    }

    fun changeSplitValueMode(mode: String) {
        if (mode == splitValueMode) return
        val total = amountText.toLongOrNull()
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
        splitRows.indices.forEach { index ->
            splitRows[index] = splitRows[index].copy(value = converted[index].toString())
        }
        splitValueMode = mode
        splitEditorError = null
    }

    fun transactionSplitDrafts(): List<TransactionSplitDraft>? {
        if (!splitEnabled) return null
        val total = amountText.toLongOrNull() ?: return null
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

    fun submit(dismissAfterSave: Boolean) {
        val splits = transactionSplitDrafts()
        if (splitEnabled && splits == null) {
            error = "Total split harus sama dengan nominal transaksi."
            return
        }
        val targetWalletId = walletId
        val targetCatId = categoryId
        val value = amountText.toLongOrNull()
        val occurred = dateEpoch

        if (targetWalletId == null || targetCatId == null || value == null || value <= 0 || occurred == null) {
            error = "Pilih dompet dan kategori, lalu isi nominal serta tanggal yang valid."
            return
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val wallet = database.walletDao().findById(targetWalletId) ?: error("Dompet tidak ditemukan.")
                    val balance = BalanceRepository(database).balance(targetWalletId, now) ?: 0L
                    val draft = TransactionDraft(
                        type, value, wallet.type, balance, occurred, now, targetCatId,
                        note.trim().ifEmpty { null }, merchant.trim().ifEmpty { null }, targetWalletId,
                    )
                    if (draft.requiresNegativeBalanceWarning && !negativeWarning) {
                        throw TransactionDao.InsufficientBalanceException()
                    }
                    database.runInTransaction(java.util.concurrent.Callable {
                        val entity = TransactionEntity(
                            draft.type, draft.walletId, draft.amountRupiah, null, draft.categoryId,
                            draft.occurredAtEpochMillis, draft.note, draft.merchant, null,
                        )
                        val transactionId = if (splits.isNullOrEmpty()) {
                            database.transactionDao().insertOrdinary(entity, negativeWarning)
                        } else {
                            database.transactionDao().insertOrdinaryWithSplits(entity, negativeWarning, splits)
                        }
                        if (receiptAttachmentId != null) {
                            check(database.attachmentDao().attach(receiptAttachmentId, "TRANSACTION", transactionId) == 1) {
                                "Lampiran struk tidak dapat ditautkan ke transaksi."
                            }
                        }
                        if (initialReceipt != null && merchant.isNotBlank()) {
                            database.merchantCategoryHintDao().recordConfirmedChoice(
                                ReceiptOcrParser.normalizeMerchant(merchant), targetCatId,
                            )
                        }
                        transactionId
                    })
                }
            }

            result.fold(
                onSuccess = { savedId ->
                    haptics.confirm()
                    context.getSharedPreferences("arus_transactions", android.content.Context.MODE_PRIVATE)
                        .edit().putLong("last_wallet_id", targetWalletId).apply()
                    if (dismissAfterSave) {
                        onSaved(savedId, true)
                    } else {
                        amountText = ""
                        note = ""
                        merchant = ""
                        error = null
                        negativeWarning = false
                        splitEnabled = false
                        splitRows.clear()
                        splitValueMode = "NOMINAL"
                        amountFocus.requestFocus()
                        keyboard?.show()
                        onSaved(savedId, false)
                    }
                },
                onFailure = { failure ->
                    haptics.reject()
                    if (failure is TransactionDao.InsufficientBalanceException) {
                        negativeWarning = true
                        error = "Saldo tidak cukup, saldo akan minus. Tetap simpan?"
                    } else {
                        error = failure.message ?: "Transaksi tidak dapat disimpan."
                    }
                },
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background)
            .imePadding(),
    ) {
        // 1. Top Bar: Hanya tombol kembali dan judul (tanpa aksi kanan)
        ArusTopBar(
            title = if (initialReceipt == null) "Tambah transaksi" else "Konfirmasi hasil scan",
            onBackClick = onDismiss,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            // OCR inspection if opened from receipt scanner
            if (initialReceipt != null) {
                ArusCard(modifier = Modifier.fillMaxWidth()) {
                    BasicText(
                        text = "Periksa semua hasil OCR sebelum menyimpan transaksi.",
                        style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                    )
                    ArusTertiaryButton(
                        text = if (showOcrText) "Sembunyikan teks OCR" else "Lihat teks OCR",
                        onClick = { showOcrText = !showOcrText },
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    if (showOcrText) {
                        BasicText(
                            text = initialReceipt.rawText.ifBlank { "Belum ada teks yang terbaca." },
                            style = ArusTheme.typography.bodySmall.copy(fontFamily = ArusTheme.typography.amountMono.fontFamily),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            // 2. Segmented Type Control: Pengeluaran / Pemasukan
            ArusSegmentedControl(
                options = listOf("Pengeluaran", "Pemasukan"),
                selectedIndex = if (type == TransactionType.EXPENSE) 0 else 1,
                onSelectIndex = { idx ->
                    val newType = if (idx == 0) TransactionType.EXPENSE else TransactionType.INCOME
                    currentType = newType.name
                    onTypeChange(newType)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // 3. Amount Input (Nominal paling atas sesuai wireframe & aturan form)
            ArusAmountInput(
                value = amountText,
                onValueChange = {
                    amountText = it
                    error = null
                    negativeWarning = false
                },
                placeholder = "Rp 0",
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocus),
                testTag = "transaction_amount_input",
            )

            // 4. Split Category or Standard Category Selector
            if (splitEnabled) {
                ArusCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            BasicText(text = "Pecah kategori", style = ArusTheme.typography.title)
                            BasicText(
                                text = "${splitRows.size} bagian · total harus sama dengan nominal",
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            )
                        }
                        ArusSecondaryButton(text = "Ubah split", onClick = ::beginSplitEditing)
                    }
                    val drafts = transactionSplitDrafts()
                    val splitAmount = drafts?.sumOf { it.amountRupiah } ?: 0L
                    BasicText(
                        text = if (drafts == null) "Split belum seimbang. Ubah pembagian sebelum menyimpan."
                        else "Total split · ${ArusFormatting.formatRupiah(splitAmount)}",
                        style = ArusTheme.typography.bodySmall.copy(
                            color = if (drafts == null) ArusTheme.colors.error else ArusTheme.colors.mutedForeground,
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        BasicText(
                            text = if (hintedCategoryId == categoryId && hintedCategoryId != null) "Saran merchant" else "Sering dipakai",
                            style = ArusTheme.typography.title,
                        )
                        ArusTertiaryButton(
                            text = "Pilih kategori ›",
                            onClick = {
                                haptics.tick()
                                categoryPickerOpen = true
                            },
                        )
                    }

                    // Quick category chips
                    val quickCategories = if (showAllCategories) categories else categories.take(5)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                    ) {
                        quickCategories.forEach { cat ->
                            ArusFilterChip(
                                label = cat.name,
                                selected = cat.id == categoryId,
                                onClick = {
                                    categoryId = cat.id
                                    error = null
                                },
                            )
                        }
                        if (categories.size > 5) {
                            ArusChip(
                                label = if (showAllCategories) "Tutup" else "Lainnya ›",
                                onClick = { showAllCategories = !showAllCategories },
                            )
                        }
                    }

                    // Pecah kategori button
                    ArusTertiaryButton(
                        text = "Pecah kategori",
                        onClick = ::beginSplitEditing,
                        leadingIconKey = ArusIconRegistry.SPLIT,
                    )
                }
            }

            // 5. Dompet Selector Row (Pill Card)
            ArusCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button },
                onClick = {
                    haptics.tick()
                    walletPickerOpen = true
                },
                contentPadding = 14.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ArusIcon(
                            iconKey = ArusIconRegistry.WALLET,
                            tint = ArusTheme.colors.foreground,
                            size = 18.dp,
                        )
                        Column {
                            BasicText(
                                text = "Dompet",
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            )
                            BasicText(
                                text = selectedWallet?.name ?: "Pilih dompet",
                                style = ArusTheme.typography.title,
                            )
                        }
                    }
                    ArusIcon(
                        iconKey = ArusIconRegistry.CHEVRON_RIGHT,
                        tint = ArusTheme.colors.mutedForeground,
                        size = 16.dp,
                    )
                }
            }

            // 6. Tanggal Selector Row
            ArusCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button },
                onClick = {
                    val initial = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() },
                        initial.year,
                        initial.monthValue - 1,
                        initial.dayOfMonth,
                    ).show()
                },
                contentPadding = 14.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ArusIcon(
                            iconKey = ArusIconRegistry.CALENDAR,
                            tint = ArusTheme.colors.foreground,
                            size = 18.dp,
                        )
                        Column {
                            BasicText(
                                text = "Tanggal",
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            )
                            BasicText(
                                text = if (date == LocalDate.now().toString()) "Hari ini ($date)" else date,
                                style = ArusTheme.typography.title,
                            )
                        }
                    }
                    ArusIcon(
                        iconKey = ArusIconRegistry.CHEVRON_RIGHT,
                        tint = ArusTheme.colors.mutedForeground,
                        size = 16.dp,
                    )
                }
            }

            // 7. Catatan & Merchant Expandable Row
            if (showMetadata) {
                ArusTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Catatan (opsional)",
                    singleLine = true,
                    leadingIcon = ArusIconRegistry.PEN,
                    modifier = Modifier.fillMaxWidth(),
                )
                ArusTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    placeholder = "Merchant / Toko (opsional)",
                    singleLine = true,
                    leadingIcon = ArusIconRegistry.STORE,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ArusTertiaryButton(
                    text = "+ Tambah catatan atau merchant",
                    onClick = { showMetadata = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (dateIsFuture) {
                BasicText(
                    text = "Terjadwal · belum masuk saldo saat ini",
                    style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                )
            }

            error?.let {
                BasicText(
                    text = it,
                    style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error),
                )
            }
        }

        // 8. Sticky Action Buttons at Bottom (above keyboard)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArusTheme.colors.surface)
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            ArusPrimaryButton(
                text = "Simpan",
                onClick = { submit(dismissAfterSave = true) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArusSecondaryButton(
                    text = "Simpan & tambah lagi",
                    onClick = { submit(dismissAfterSave = false) },
                    modifier = Modifier.weight(1f),
                )
                ArusTertiaryButton(
                    text = "Batal",
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.6f),
                )
            }
        }
    }

    // Modal Sheet: Pilih Kategori (S-37)
    if (categoryPickerOpen) {
        CategoryPickerSheet(
            categories = categories,
            selectedCategoryId = categoryId,
            onCategorySelected = {
                categoryId = it.id
                error = null
            },
            onDismiss = { categoryPickerOpen = false },
        )
    }

    // Modal Sheet: Pilih Dompet (S-38)
    if (walletPickerOpen) {
        WalletPickerSheet(
            wallets = wallets,
            walletBalances = walletBalances,
            selectedWalletId = walletId,
            onWalletSelected = {
                walletId = it.id
                error = null
            },
            onDismiss = { walletPickerOpen = false },
        )
    }

    // Modal Dialog: Pecah Kategori (Split Editor)
    if (splitEditorOpen) {
        ArusDialog(
            title = "Pecah kategori",
            message = "Bagi nominal transaksi ke beberapa kategori.",
            confirmText = "Simpan split",
            onConfirm = {
                val total = amountText.toLongOrNull()
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
            },
            dismissText = "Batal",
            onDismiss = { splitEditorOpen = false },
        )
    }
}
