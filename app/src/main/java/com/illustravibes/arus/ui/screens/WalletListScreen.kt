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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BalanceRepository
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusEmptyState
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.WalletCard
import com.illustravibes.arus.ui.designsystem.WalletCardUiModel
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * S-20 Dompet: Layar daftar dompet aktif dan terarsip.
 */
@Composable
fun WalletListScreen(
    database: ArusDatabase,
    onBackClick: () -> Unit,
    onAddWallet: () -> Unit,
    onEditWallet: (WalletEntity) -> Unit,
    onWalletDetail: (Long) -> Unit,
    onReconciliation: (WalletEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberArusHaptics()

    val wallets = remember { mutableStateListOf<WalletEntity>() }
    val archivedWallets = remember { mutableStateListOf<WalletEntity>() }
    val balances = remember { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<WalletEntity?>(null) }

    fun refresh() {
        scope.launch {
            val values = withContext(Dispatchers.IO) {
                val active = database.walletDao().activeWallets()
                val archived = database.walletDao().archivedWallets()
                val bMap = (active + archived).associate { w ->
                    w.id to (BalanceRepository(database).balance(w.id, System.currentTimeMillis()) ?: 0L)
                }
                Triple(active, archived, bMap)
            }
            wallets.clear()
            wallets.addAll(values.first)
            archivedWallets.clear()
            archivedWallets.addAll(values.second)
            balances.value = values.third
        }
    }

    LaunchedEffect(database) { refresh() }

    val displayedWallets = if (showArchived) archivedWallets else wallets

    fun moveWallet(wallet: WalletEntity, delta: Int) {
        val from = wallets.indexOf(wallet)
        val to = from + delta
        if (to !in wallets.indices) return
        scope.launch {
            withContext(Dispatchers.IO) {
                database.runInTransaction {
                    val other = wallets[to]
                    val currentOrder = wallet.sortOrder
                    wallet.sortOrder = other.sortOrder
                    other.sortOrder = currentOrder
                    database.walletDao().update(wallet)
                    database.walletDao().update(other)
                }
            }
            refresh()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background),
    ) {
        ArusTopBar(
            title = "Dompet",
            onBackClick = onBackClick,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            // Filter Toggle: Aktif vs Terarsip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ArusFilterChip(
                        label = "Aktif (${wallets.size})",
                        selected = !showArchived,
                        onClick = {
                            haptics.tick()
                            showArchived = false
                        },
                    )
                    ArusFilterChip(
                        label = "Terarsip (${archivedWallets.size})",
                        selected = showArchived,
                        onClick = {
                            haptics.tick()
                            showArchived = true
                        },
                    )
                }
            }

            if (displayedWallets.isEmpty()) {
                item {
                    ArusEmptyState(
                        title = if (showArchived) "Belum ada dompet terarsip" else "Belum ada dompet",
                        description = if (showArchived) "Dompet yang diarsipkan akan muncul di sini."
                        else "Tambahkan dompet bank, e-wallet, tunai, atau kredit untuk mulai mencatat keuangan.",
                        icon = ArusIconRegistry.WALLET,
                    )
                }
            } else {
                items(displayedWallets, key = { it.id }) { wallet ->
                    val bal = balances.value[wallet.id] ?: wallet.initialBalanceRupiah
                    val walletModel = WalletCardUiModel(
                        id = wallet.id,
                        name = wallet.name,
                        type = wallet.type,
                        balance = bal,
                        creditLimit = wallet.creditLimitRupiah,
                    )

                    ArusCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            haptics.click()
                            onWalletDetail(wallet.id)
                        },
                        contentPadding = 14.dp,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            WalletCard(
                                model = walletModel,
                                onClick = {
                                    haptics.click()
                                    onWalletDetail(wallet.id)
                                },
                                onReconciliationClick = {
                                    haptics.tick()
                                    onReconciliation(wallet)
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (!showArchived) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    ArusSecondaryButton(
                                        text = "Edit",
                                        onClick = { onEditWallet(wallet) },
                                        modifier = Modifier.weight(1f),
                                    )
                                    ArusSecondaryButton(
                                        text = "Arsip",
                                        onClick = { archiveTarget = wallet },
                                        modifier = Modifier.weight(1f),
                                    )
                                    val index = wallets.indexOf(wallet)
                                    ArusTertiaryButton(
                                        text = "↑",
                                        onClick = { moveWallet(wallet, -1) },
                                        enabled = index > 0,
                                    )
                                    ArusTertiaryButton(
                                        text = "↓",
                                        onClick = { moveWallet(wallet, 1) },
                                        enabled = index < wallets.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Bottom Action Button: Tambah Dompet
        if (!showArchived) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ArusTheme.colors.surface)
                    .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Medium),
            ) {
                ArusPrimaryButton(
                    text = "+ Tambah dompet",
                    onClick = {
                        haptics.tick()
                        onAddWallet()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // Confirmation Dialog: Archive Wallet
    archiveTarget?.let { target ->
        ArusDialog(
            title = "Arsipkan ${target.name}?",
            message = "Dompet tidak akan tersedia untuk pencatatan transaksi baru. Seluruh data riwayat tetap tersimpan.",
            confirmText = "Arsipkan",
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) { database.walletDao().archive(target.id) }
                    archiveTarget = null
                    haptics.confirm()
                    refresh()
                }
            },
            dismissText = "Batal",
            onDismiss = { archiveTarget = null },
            isDestructive = true,
        )
    }
}

