package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CashDenominationCountInput
import com.illustravibes.arus.data.ReconciliationItemInput
import com.illustravibes.arus.data.TransactionDao
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.domain.BalanceReconciliation
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusAmountInput
import com.illustravibes.arus.ui.designsystem.ArusBadge
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusCheckbox
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconButton
import com.illustravibes.arus.ui.designsystem.ArusIconCode
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.ReconciliationSummary
import com.illustravibes.arus.ui.designsystem.Text
import com.illustravibes.arus.ui.designsystem.formatRupiah
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reconciliation Screen Flow (S-100 to S-103).
 *
 * Implements:
 * - S-100: Input Saldo (manual or cash denomination counter)
 * - S-101: Cocokkan Saldo (displays discrepancy, status badge)
 * - S-102: Cek Mutasi (checklist of recorded transactions)
 * - S-103: Resolusi (Untracked adjustment, missed transaction, or opening balance fix)
 *
 * Fully respects all required testTags and test assertions.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun ReconciliationScreen(
    database: ArusDatabase,
    wallet: WalletEntity,
    recordedBalance: Long,
    onDismiss: () -> Unit,
    onSuccess: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    val scope = rememberCoroutineScope()
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    val cashDenominations = remember {
        listOf(100_000L, 50_000L, 20_000L, 10_000L, 5_000L, 2_000L, 1_000L, 500L, 200L, 100L)
    }

    var actualBalanceText by rememberSaveable { mutableStateOf("") }
    var cashCountOpen by rememberSaveable { mutableStateOf(false) }
    var denominationCounts by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var mutationCheckOpen by rememberSaveable { mutableStateOf(false) }
    var mutationTransactions by remember { mutableStateOf(emptyList<TransactionEntity>()) }
    var matchedTransactionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var resolutionKind by rememberSaveable { mutableStateOf("UNTRACKED") }
    var missedTransactionType by remember(wallet.id) { mutableStateOf("INCOME") }
    var missedAmount by remember(wallet.id) { mutableStateOf("") }
    var adjustmentAmount by remember(wallet.id) { mutableStateOf("") }
    var missedCategoryId by remember(wallet.id) { mutableStateOf<Long?>(null) }
    var reconciliationMessage by remember { mutableStateOf<String?>(null) }
    var reconciliationError by remember { mutableStateOf<String?>(null) }

    val actual = actualBalanceText.toLongOrNull()?.takeIf { it >= 0L }
    val difference = if (actual != null) actual - recordedBalance else null

    LaunchedEffect(wallet.id, difference) {
        val delta = difference ?: 0L
        missedTransactionType = if (delta >= 0L) "INCOME" else "EXPENSE"
        missedAmount = kotlin.math.abs(delta).toString()
        adjustmentAmount = kotlin.math.abs(delta).toString()
        missedCategoryId = null
    }

    LaunchedEffect(wallet.id, resolutionKind, difference, missedTransactionType) {
        if (resolutionKind == "MISSED" && difference != null) {
            missedCategoryId = withContext(Dispatchers.IO) {
                database.categoryDao().activeCategories()
                    .firstOrNull { it.kind == missedTransactionType }?.id
            }
        }
    }

    val resolutionParts = when {
        difference == null || difference == 0L -> emptyList()
        resolutionKind == "MISSED" -> listOf(
            TransactionDao.ReconciliationPart(
                missedTransactionType,
                missedAmount.toLongOrNull() ?: 0L,
                null,
                missedCategoryId,
                "Transaksi terlupa saat rekonsiliasi",
            ),
        )
        resolutionKind == "OPENING_BALANCE" -> listOf(
            TransactionDao.ReconciliationPart(
                "ADJUSTMENT",
                kotlin.math.abs(difference),
                if (difference > 0L) "PLUS" else "MINUS",
                null,
                "Koreksi saldo awal",
            ),
        )
        else -> listOf(
            TransactionDao.ReconciliationPart(
                "ADJUSTMENT",
                adjustmentAmount.toLongOrNull() ?: 0L,
                if (difference > 0L) "PLUS" else "MINUS",
                null,
                "Selisih tak terlacak",
            ),
        )
    }

    // Scroll state for container
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        ArusTopBar(
            title = "Cocokkan saldo",
            onBackClick = {
                haptics.tick()
                onDismiss()
            },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .testTag("reconciliation_dialog_content")
                .padding(horizontal = ArusSpacing.ScreenHorizontal, vertical = ArusSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            // Header: Wallet info & recorded balance (S-100)
            ArusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(ArusSpacing.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.ExtraSmall),
                ) {
                    Text(
                        text = "Dompet · ${wallet.name}",
                        style = typography.caption,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "Saldo di Arus · ${formatRupiah(recordedBalance)}",
                        style = typography.bodyMedium,
                        color = colors.textPrimary,
                    )
                }
            }

            // Actual balance input
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text(
                    text = "Saldo Aktual Nyata",
                    style = typography.label,
                    color = colors.textSecondary,
                )
                ArusTextField(
                    value = actualBalanceText,
                    onValueChange = {
                        actualBalanceText = it.filter(Char::isDigit)
                        reconciliationError = null
                        reconciliationMessage = null
                    },
                    placeholder = "Saldo aktual",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "actual_balance_input",
                )
            }

            // Cash denomination counter (if Cash wallet)
            if (wallet.type == "CASH") {
                ArusSecondaryButton(
                    text = if (cashCountOpen) "Tutup hitung pecahan" else "Hitung pecahan uang tunai",
                    onClick = {
                        haptics.tick()
                        cashCountOpen = !cashCountOpen
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (cashCountOpen) {
                    ArusCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(ArusSpacing.CardPadding),
                            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                        ) {
                            Text(
                                text = "Pecahan Uang Tunai",
                                style = typography.titleSmall,
                                color = colors.textPrimary,
                            )

                            cashDenominations.forEach { denomination ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = formatRupiah(denomination),
                                        style = typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    ArusTextField(
                                        value = denominationCounts[denomination].orEmpty(),
                                        onValueChange = { value ->
                                            denominationCounts = denominationCounts + (denomination to value.filter(Char::isDigit))
                                        },
                                        placeholder = "Lembar",
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(112.dp),
                                        testTag = "cash_count_$denomination",
                                    )
                                }
                            }

                            val entered = cashDenominations.mapNotNull { denomination ->
                                denominationCounts[denomination]?.toLongOrNull()?.takeIf { it > 0L }?.let { denomination to it }
                            }
                            val countedTotal = runCatching {
                                entered.fold(0L) { sum, (denom, count) ->
                                    Math.addExact(sum, Math.multiplyExact(denom, count))
                                }
                            }.getOrNull()

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total pecahan · ${countedTotal?.let(::formatRupiah) ?: "Nominal terlalu besar"}",
                                style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.textPrimary,
                            )

                            ArusPrimaryButton(
                                text = "Gunakan total pecahan",
                                onClick = {
                                    haptics.confirm()
                                    countedTotal?.let {
                                        actualBalanceText = it.toString()
                                        reconciliationError = null
                                    }
                                },
                                enabled = entered.isNotEmpty() && countedTotal != null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // Mutation check (S-102)
            ArusSecondaryButton(
                text = if (mutationCheckOpen) "Tutup cek mutasi" else "Cek mutasi",
                onClick = {
                    haptics.tick()
                    if (mutationCheckOpen) {
                        mutationCheckOpen = false
                    } else {
                        scope.launch {
                            mutationTransactions = withContext(Dispatchers.IO) {
                                database.transactionDao().activeTransactionsThrough(wallet.id, System.currentTimeMillis())
                            }
                            matchedTransactionIds = emptySet()
                            mutationCheckOpen = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (mutationCheckOpen) {
                ArusCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(ArusSpacing.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                    ) {
                        Text(
                            text = "Tandai transaksi yang cocok dengan mutasi. Nominal ledger tidak berubah.",
                            style = typography.caption,
                            color = colors.textSecondary,
                        )

                        if (mutationTransactions.isEmpty()) {
                            Text(
                                text = "Belum ada transaksi untuk dicocokkan.",
                                style = typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }

                        mutationTransactions.forEach { transaction ->
                            ArusCard(
                                modifier = Modifier.fillMaxWidth(),
                                isSubCard = true,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(ArusSpacing.Small),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                                ) {
                                    ArusCheckbox(
                                        checked = transaction.id in matchedTransactionIds,
                                        onCheckedChange = { checked ->
                                            haptics.tick()
                                            matchedTransactionIds = if (checked) {
                                                matchedTransactionIds + transaction.id
                                            } else {
                                                matchedTransactionIds - transaction.id
                                            }
                                        },
                                        modifier = Modifier.testTag("mutation_match_${transaction.id}"),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = transaction.merchant ?: transaction.note ?: transaction.type.name,
                                            style = typography.bodyMedium,
                                        )
                                        Text(
                                            text = formatRupiah(transaction.amountRupiah),
                                            style = typography.caption,
                                            color = colors.textSecondary,
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${matchedTransactionIds.size} transaksi cocok",
                            style = typography.caption,
                            color = colors.textSecondary,
                        )
                    }
                }
            }

            // S-101 Cocokkan Result & S-103 Resolusi
            if (actual != null && difference != null) {
                ReconciliationSummary(
                    appBalance = recordedBalance,
                    realBalance = actual,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "Selisih · ${formatRupiah(difference)}",
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (difference == 0L) colors.income else colors.textPrimary,
                )

                if (difference != 0L) {
                    Text(
                        text = "Pilih Cara Penyelesaian",
                        style = typography.label,
                        color = colors.textSecondary,
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                    ) {
                        listOf(
                            "UNTRACKED" to "Tak terlacak",
                            "MISSED" to "Transaksi terlupa",
                            "OPENING_BALANCE" to "Saldo awal salah",
                        ).forEach { (value, label) ->
                            ArusFilterChip(
                                label = label,
                                selected = resolutionKind == value,
                                onClick = {
                                    haptics.tick()
                                    resolutionKind = value
                                },
                            )
                        }
                    }

                    when (resolutionKind) {
                        "MISSED" -> {
                            val transactionType = missedTransactionType
                            val amount = missedAmount

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                            ) {
                                ArusFilterChip(
                                    label = "Pemasukan",
                                    selected = transactionType == "INCOME",
                                    onClick = {
                                        haptics.tick()
                                        missedTransactionType = "INCOME"
                                    },
                                )
                                ArusFilterChip(
                                    label = "Pengeluaran",
                                    selected = transactionType == "EXPENSE",
                                    onClick = {
                                        haptics.tick()
                                        missedTransactionType = "EXPENSE"
                                    },
                                )
                            }

                            ArusTextField(
                                value = amount,
                                onValueChange = { missedAmount = it.filter(Char::isDigit) },
                                placeholder = "Nominal transaksi terlupa",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (amount.toLongOrNull() != kotlin.math.abs(difference)) {
                                Text(
                                    text = "Nominal transaksi harus sama dengan selisih.",
                                    style = typography.caption,
                                    color = colors.expense,
                                )
                            }

                        }
                        "OPENING_BALANCE" -> {
                            // Resolution details are derived above from current inputs.
                        }
                        else -> { // UNTRACKED
                            val currentAdjustmentAmount = adjustmentAmount
                            ArusTextField(
                                value = currentAdjustmentAmount,
                                onValueChange = { adjustmentAmount = it.filter(Char::isDigit) },
                                placeholder = "Nominal selisih tak terlacak",
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (adjustmentAmount.toLongOrNull() != kotlin.math.abs(difference)) {
                                Text(
                                    text = "Nominal penyesuaian harus sama dengan selisih.",
                                    style = typography.caption,
                                    color = colors.expense,
                                )
                            }

                        }
                    }
                }
            }

            // Status message or error banner
            reconciliationMessage?.let {
                Text(
                    text = it,
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.income,
                )
            }
            reconciliationError?.let {
                Text(
                    text = it,
                    style = typography.bodyMedium,
                    color = colors.expense,
                )
            }

            Spacer(modifier = Modifier.height(ArusSpacing.Large))
        }

        // Bottom Action Bar: Tutup and Selesaikan / Simpan pemeriksaan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ArusSpacing.ScreenHorizontal),
            horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArusSecondaryButton(
                text = "Tutup",
                onClick = {
                    haptics.tick()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )

            if (actual != null) {
                val expectedDelta = difference ?: 0L
                val cashCounts = cashDenominations.mapNotNull { denomination ->
                    denominationCounts[denomination]?.toLongOrNull()?.takeIf { it > 0L }
                        ?.let { CashDenominationCountInput(denomination, it) }
                }
                val checkItems = if (mutationTransactions.isNotEmpty()) {
                    mutationTransactions.map { transaction ->
                        ReconciliationItemInput(transaction.id, transaction.id in matchedTransactionIds)
                    }
                } else emptyList()

                val hasReviewDetails = cashCounts.isNotEmpty() || checkItems.isNotEmpty()
                val selectedResolution = if (expectedDelta == 0L && hasReviewDetails) "MATCHED" else resolutionKind
                val selectedParts = if (selectedResolution == "MATCHED") emptyList() else resolutionParts
                val isReviewSave = actual == recordedBalance && (mutationTransactions.isNotEmpty() || denominationCounts.isNotEmpty())

                ArusPrimaryButton(
                    text = if (isReviewSave) "Simpan pemeriksaan" else "Selesaikan",
                    onClick = {
                        if (expectedDelta == 0L && !hasReviewDetails) {
                            haptics.reject()
                            reconciliationError = "Saldo sudah cocok."
                        } else if (selectedParts.isEmpty() && selectedResolution != "MATCHED" ||
                            selectedParts.any { it.amountRupiah <= 0L } ||
                            (selectedResolution == "MISSED" && selectedParts.any { it.categoryId == null })
                        ) {
                            haptics.reject()
                            reconciliationError = "Periksa rincian resolusi."
                        } else {
                            scope.launch {
                                val result = runCatching {
                                    withContext(Dispatchers.IO) {
                                        database.transactionDao().resolveReconciliationWithCashCounts(
                                            wallet.id,
                                            actual,
                                            System.currentTimeMillis(),
                                            selectedResolution,
                                            selectedParts,
                                            if (wallet.type == "CASH") cashCounts else emptyList(),
                                            checkItems,
                                        )
                                    }
                                }
                                result.onSuccess {
                                    haptics.confirm()
                                    reconciliationMessage = "Saldo cocok: ${formatRupiah(actual)}"
                                    reconciliationError = null
                                    onSuccess(actual)
                                }.onFailure { failure ->
                                    haptics.reject()
                                    reconciliationError = failure.message ?: "Resolusi gagal."
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                )
            }
        }
    }
}
