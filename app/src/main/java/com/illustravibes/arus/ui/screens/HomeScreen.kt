package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BalanceRepository
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.TransactionDao
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.ArusFormatting
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusBalanceCard
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusEmptyState
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusShapes
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.TransactionItem
import com.illustravibes.arus.ui.designsystem.TransactionItemUiModel
import com.illustravibes.arus.ui.designsystem.WalletCard
import com.illustravibes.arus.ui.designsystem.WalletCardUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * S-10 Beranda: Layar utama dengan Net Worth Card, Wallet Carousel, Aktivitas Terbaru,
 * dan Aksi Cepat.
 */
@Composable
fun HomeScreen(
    database: ArusDatabase,
    onNetWorthDetail: () -> Unit,
    onWallets: () -> Unit,
    onWallet: (Long) -> Unit,
    onTransaction: (Long) -> Unit,
    onExpense: () -> Unit,
    onIncome: () -> Unit,
    onTransfer: () -> Unit,
    onScanReceipt: () -> Unit,
    onReconciliation: (WalletEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    val wallets = remember { mutableStateListOf<WalletEntity>() }
    val walletBalances = remember { mutableStateMapOf<Long, Long>() }
    val latest = remember { mutableStateListOf<TransactionEntity>() }
    val categoriesMap = remember { mutableStateMapOf<Long, CategoryEntity>() }
    var summary by remember { mutableStateOf<BalanceRepository.FinancialSummary?>(null) }
    var detailOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(database) {
        val snapshot = withContext(Dispatchers.IO) {
            val activeWallets = database.walletDao().activeWallets()
            val balances = activeWallets.associate { wallet ->
                wallet.id to (BalanceRepository(database).balance(wallet.id, System.currentTimeMillis()) ?: 0L)
            }
            val activeCategories = database.categoryDao().activeCategories().associateBy { it.id }
            val latestTrx = database.transactionDao().latestTransactions()
            val finSummary = BalanceRepository(database).financialSummary(System.currentTimeMillis())
            Triple(activeWallets, latestTrx, finSummary) to (balances to activeCategories)
        }
        wallets.clear()
        wallets.addAll(snapshot.first.first)
        latest.clear()
        latest.addAll(snapshot.first.second)
        summary = snapshot.first.third
        walletBalances.clear()
        walletBalances.putAll(snapshot.second.first)
        categoriesMap.clear()
        categoriesMap.putAll(snapshot.second.second)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_overview_list"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Large),
    ) {
        // 1. Header (Brand & Motto)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicText(
                    text = "Arus",
                    style = ArusTheme.typography.display.copy(
                        fontWeight = FontWeight.Bold,
                        color = ArusTheme.colors.foreground,
                    ),
                )
                BasicText(
                    text = "Less noise. More control.",
                    style = ArusTheme.typography.bodySmall.copy(
                        color = ArusTheme.colors.mutedForeground,
                    ),
                )
            }
        }

        // 2. Net Worth Highlight Card (Dark surface, white text, no blue background)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                val assets = summary?.assetsRupiah ?: 0L
                val liabilities = (summary?.creditLiabilitiesRupiah ?: 0L) + (summary?.debtLiabilitiesRupiah ?: 0L)
                val netWorth = summary?.netWorthRupiah ?: (assets - liabilities)

                ArusBalanceCard(
                    netWorthAmount = netWorth,
                    assetsAmount = assets,
                    liabilitiesAmount = liabilities,
                    onClick = {
                        haptics.click()
                        detailOpen = true
                    },
                )
            }
        }

        // 3. Wallets Carousel Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BasicText(
                        text = "DOMPET",
                        style = ArusTheme.typography.label.copy(
                            color = ArusTheme.colors.mutedForeground,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    BasicText(
                        text = "Lihat semua ›",
                        style = ArusTheme.typography.label.copy(
                            color = ArusTheme.colors.foreground,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.clickable {
                            haptics.tick()
                            onWallets()
                        },
                    )
                }

                if (wallets.isEmpty()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ArusCard(modifier = Modifier.fillMaxWidth()) {
                            BasicText(
                                text = "Belum ada dompet",
                                style = ArusTheme.typography.title,
                            )
                            BasicText(
                                text = "Tambahkan dompet untuk mulai melihat saldo dan ringkasan.",
                                style = ArusTheme.typography.body.copy(color = ArusTheme.colors.mutedForeground),
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                            )
                            ArusPrimaryButton(
                                text = "Tambah dompet",
                                onClick = onWallets,
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(wallets, key = { it.id }) { wallet ->
                            val balance = walletBalances[wallet.id] ?: 0L
                            val walletModel = WalletCardUiModel(
                                id = wallet.id,
                                name = wallet.name,
                                type = wallet.type,
                                balance = balance,
                                creditLimit = wallet.creditLimitRupiah,
                            )
                            WalletCard(
                                model = walletModel,
                                onClick = {
                                    haptics.click()
                                    onWallet(wallet.id)
                                },
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Actions (Pill Buttons Row)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicText(
                    text = "AKSI CEPAT",
                    style = ArusTheme.typography.label.copy(
                        color = ArusTheme.colors.mutedForeground,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        ArusPrimaryButton(
                            text = "− Pengeluaran",
                            onClick = onExpense,
                            modifier = Modifier.height(42.dp),
                        )
                    }
                    item {
                        ArusSecondaryButton(
                            text = "+ Pemasukan",
                            onClick = onIncome,
                            modifier = Modifier.height(42.dp),
                        )
                    }
                    item {
                        ArusSecondaryButton(
                            text = "⇄ Transfer",
                            onClick = onTransfer,
                            modifier = Modifier.height(42.dp),
                        )
                    }
                    item {
                        ArusSecondaryButton(
                            text = "Scan struk",
                            onClick = onScanReceipt,
                            leadingIcon = "camera",
                            modifier = Modifier.height(42.dp),
                        )
                    }
                    item {
                        val firstWallet = wallets.firstOrNull()
                        ArusSecondaryButton(
                            text = "Cocokkan saldo",
                            onClick = {
                                if (firstWallet != null) onReconciliation(firstWallet) else onWallets()
                            },
                            leadingIcon = "sliders",
                            modifier = Modifier.height(42.dp),
                        )
                    }
                }
            }
        }

        // 5. Latest Transactions (Grouped Container, No Card Soup)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BasicText(
                        text = "AKTIVITAS TERBARU",
                        style = ArusTheme.typography.label.copy(
                            color = ArusTheme.colors.mutedForeground,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }

                if (latest.isEmpty()) {
                    ArusCard(modifier = Modifier.fillMaxWidth()) {
                        BasicText(
                            text = "Belum ada transaksi",
                            style = ArusTheme.typography.title,
                        )
                        BasicText(
                            text = "Catat transaksi pertamamu dengan tombol (+) di bawah.",
                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ArusShapes.Card)
                            .background(ArusTheme.colors.surface)
                            .border(BorderStroke(1.dp, ArusTheme.colors.border), ArusShapes.Card),
                    ) {
                        Column {
                            latest.take(6).forEachIndexed { index, trx ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(ArusTheme.colors.border.copy(alpha = 0.5f))
                                    )
                                }
                                val category = trx.categoryId?.let { categoriesMap[it] }
                                val title = trx.merchant?.takeIf(String::isNotBlank)
                                    ?: trx.note?.takeIf(String::isNotBlank)
                                    ?: category?.name
                                    ?: trx.type.name

                                val dateStr = ArusFormatting.formatDate(
                                    Instant.ofEpochMilli(trx.occurredAtEpochMillis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                )
                                val subtitle = buildString {
                                    category?.name?.let { append(it) }
                                    if (isNotEmpty()) append(" · ")
                                    append(dateStr)
                                }

                                TransactionItem(
                                    model = TransactionItemUiModel(
                                        id = trx.id,
                                        type = trx.type.name,
                                        amount = trx.amountRupiah,
                                        title = title,
                                        categoryName = category?.name,
                                        categoryIcon = category?.icon,
                                        dateFormatted = dateStr,
                                        subtitle = subtitle,
                                    ),
                                    isBordered = false,
                                    onClick = {
                                        haptics.click()
                                        onTransaction(trx.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Net Worth Detail Dialog
    if (detailOpen) {
        val assets = summary?.assetsRupiah ?: 0L
        val creditLiabilities = summary?.creditLiabilitiesRupiah ?: 0L
        val debtLiabilities = summary?.debtLiabilitiesRupiah ?: 0L
        val totalLiabilities = creditLiabilities + debtLiabilities

        ArusDialog(
            title = "Rincian Kekayaan Bersih",
            message = "Aset: ${ArusFormatting.formatRupiah(assets)}\n" +
                "Kewajiban Kartu Kredit: ${ArusFormatting.formatRupiah(creditLiabilities)}\n" +
                "Kewajiban Utang: ${ArusFormatting.formatRupiah(debtLiabilities)}\n" +
                "Total Kewajiban: ${ArusFormatting.formatRupiah(totalLiabilities)}",
            confirmText = "Tutup",
            onConfirm = { detailOpen = false },
            onDismiss = { detailOpen = false },
        )
    }
}
