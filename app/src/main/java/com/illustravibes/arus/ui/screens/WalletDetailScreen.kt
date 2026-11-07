package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BalanceRepository
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.ArusFormatting
import com.illustravibes.arus.ui.designsystem.AmountKind
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusEmptyState
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.TransactionItem
import com.illustravibes.arus.ui.designsystem.TransactionItemUiModel
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * S-22 Detail Dompet: Rincian saldo saat ini, saldo awal, aksi cocokkan saldo,
 * dan daftar transaksi riwayat khusus dompet ini.
 */
@Composable
fun WalletDetailScreen(
    database: ArusDatabase,
    walletId: Long,
    onDismiss: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onReconciliation: (WalletEntity) -> Unit,
    onEditWallet: (WalletEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    var wallet by remember(walletId) { mutableStateOf<WalletEntity?>(null) }
    var currentBalance by remember(walletId) { mutableStateOf<Long?>(null) }
    var transactions by remember(walletId) { mutableStateOf(emptyList<TransactionEntity>()) }
    val categoriesMap = remember { mutableStateMapOf<Long, CategoryEntity>() }

    LaunchedEffect(walletId, database) {
        val data = withContext(Dispatchers.IO) {
            val w = database.walletDao().findById(walletId)
            val bal = BalanceRepository(database).balance(walletId, System.currentTimeMillis()) ?: 0L
            val trxList = database.transactionDao().activeTransactionsThrough(walletId, System.currentTimeMillis())
            val cats = database.categoryDao().activeCategories().associateBy { it.id }
            Triple(w, bal, trxList) to cats
        }
        wallet = data.first.first
        currentBalance = data.first.second
        transactions = data.first.third
        categoriesMap.clear()
        categoriesMap.putAll(data.second)
    }

    val currentWallet = wallet ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background),
    ) {
        ArusTopBar(
            title = currentWallet.name,
            onBackClick = onDismiss,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            // Balance Card
            item {
                ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BasicText(
                            text = "Saldo saat ini · ${currentWallet.type}",
                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                        )
                        AmountText(
                            amount = currentBalance ?: currentWallet.initialBalanceRupiah,
                            style = ArusTheme.typography.display,
                        )
                        BasicText(
                            text = "Saldo awal · ${ArusFormatting.formatRupiah(currentWallet.initialBalanceRupiah)}",
                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                        )
                        if (currentWallet.type == "CREDIT") {
                            val limit = currentWallet.creditLimitRupiah ?: 0L
                            BasicText(
                                text = "Batas kredit · ${ArusFormatting.formatRupiah(limit)}" +
                                    (currentWallet.billingDay?.let { " · Tagihan tanggal $it" } ?: ""),
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ArusSecondaryButton(
                                text = "Cocokkan saldo",
                                onClick = {
                                    haptics.tick()
                                    onReconciliation(currentWallet)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ArusTertiaryButton(
                                text = "Edit dompet",
                                onClick = {
                                    haptics.tick()
                                    onEditWallet(currentWallet)
                                },
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                    }
                }
            }

            // Transactions Header
            item {
                BasicText(
                    text = "RIWAYAT TRANSAKSI (${transactions.size})",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
            }

            if (transactions.isEmpty()) {
                item {
                    ArusEmptyState(
                        title = "Belum ada transaksi",
                        description = "Belum ada transaksi yang menggunakan dompet ${currentWallet.name}.",
                        icon = ArusIconRegistry.RECEIPT,
                    )
                }
            } else {
                items(transactions, key = { it.id }) { trx ->
                    val category = trx.categoryId?.let { categoriesMap[it] }
                    val title = trx.merchant?.takeIf(String::isNotBlank)
                        ?: trx.note?.takeIf(String::isNotBlank)
                        ?: category?.name
                        ?: trx.type.name
                    val dateFormatted = ArusFormatting.formatDate(
                        Instant.ofEpochMilli(trx.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    )

                    TransactionItem(
                        model = TransactionItemUiModel(
                            id = trx.id,
                            type = trx.type.name,
                            amount = trx.amountRupiah,
                            title = title,
                            categoryName = category?.name,
                            categoryIcon = category?.icon,
                            dateFormatted = dateFormatted,
                            subtitle = dateFormatted,
                        ),
                        onClick = {
                            haptics.click()
                            onTransactionClick(trx.id)
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

