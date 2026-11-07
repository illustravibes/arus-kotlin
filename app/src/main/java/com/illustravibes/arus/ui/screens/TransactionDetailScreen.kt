package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.TransactionEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.domain.TransactionType
import com.illustravibes.arus.ui.ArusFormatting
import com.illustravibes.arus.ui.designsystem.AmountKind
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/**
 * S-33 Detail Transaksi: Layar rincian transaksi dengan kartu nominal besar,
 * daftar atribut (dompet, kategori, tanggal, catatan, status), tombol Edit dan Hapus di bawah.
 */
@Composable
fun TransactionDetailScreen(
    database: ArusDatabase,
    transactionId: Long,
    onDismiss: () -> Unit,
    onEdit: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberArusHaptics()

    var transaction by remember(transactionId) { mutableStateOf<TransactionEntity?>(null) }
    var walletName by remember(transactionId) { mutableStateOf("") }
    var categoryName by remember(transactionId) { mutableStateOf("") }
    var confirmationOpen by rememberSaveable(transactionId) { mutableStateOf(false) }
    var warning by rememberSaveable(transactionId) { mutableStateOf<String?>(null) }
    var error by rememberSaveable(transactionId) { mutableStateOf<String?>(null) }

    LaunchedEffect(transactionId) {
        val data = withContext(Dispatchers.IO) {
            val dao = database.transactionDao()
            val row = dao.transactionById(transactionId)
            val wallet = row?.walletId?.let { database.walletDao().findById(it) }
            val category = row?.categoryId?.let { database.categoryDao().findById(it) }
            Triple(row, wallet, category)
        }
        transaction = data.first
        walletName = data.second?.name.orEmpty()
        categoryName = data.third?.name.orEmpty()
    }

    val row = transaction ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background),
    ) {
        ArusTopBar(
            title = "Detail transaksi",
            onBackClick = onDismiss,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            // Amount Card
            val kind = when (row.type) {
                TransactionType.INCOME -> AmountKind.INCOME
                TransactionType.EXPENSE -> AmountKind.EXPENSE
                else -> AmountKind.TRANSFER
            }

            ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BasicText(
                        text = row.type.name,
                        style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                    )
                    AmountText(
                        amount = row.amountRupiah,
                        kind = kind,
                        style = ArusTheme.typography.display,
                    )
                }
            }

            // Info Details
            ArusCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                    DetailRow(label = "Dompet", value = walletName)
                    if (categoryName.isNotBlank()) {
                        DetailRow(label = "Kategori", value = categoryName)
                    }
                    val dateFormatted = ArusFormatting.formatDateTime(
                        Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault())
                    )
                    DetailRow(label = "Tanggal", value = dateFormatted)
                    if (!row.note.isNullOrBlank()) {
                        DetailRow(label = "Catatan", value = row.note)
                    }
                    if (!row.merchant.isNullOrBlank()) {
                        DetailRow(label = "Merchant", value = row.merchant)
                    }
                    val statusText = when {
                        row.occurredAtEpochMillis > System.currentTimeMillis() -> "Terjadwal"
                        row.verifiedCheckpointId != null -> "Terverifikasi Checkpoint"
                        else -> "Efektif"
                    }
                    DetailRow(label = "Status", value = statusText)
                }
            }

            if (row.transferGroupId != null) {
                BasicText(
                    text = "Bagian transfer terkait.",
                    style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                )
            }

            warning?.let {
                BasicText(
                    text = it,
                    style = ArusTheme.typography.body.copy(color = ArusTheme.colors.warningText),
                )
            }

            error?.let {
                BasicText(
                    text = it,
                    style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error),
                )
            }
        }

        // Action Buttons at Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArusTheme.colors.surface)
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            if (row.type == TransactionType.INCOME || row.type == TransactionType.EXPENSE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onEdit != null) {
                        ArusPrimaryButton(
                            text = "Edit",
                            onClick = {
                                scope.launch {
                                    val special = withContext(Dispatchers.IO) {
                                        database.transactionDao().isSpecial(row.id) > 0
                                    }
                                    if (special) {
                                        warning = "Transaksi bagian operasi khusus tidak dapat diedit terpisah."
                                    } else {
                                        haptics.tick()
                                        onEdit(row.id)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    ArusSecondaryButton(
                        text = "Hapus",
                        onClick = {
                            scope.launch {
                                val protected = withContext(Dispatchers.IO) {
                                    database.transactionDao().isVerified(row.id) > 0 ||
                                        database.transactionDao().isSpecial(row.id) > 0
                                }
                                if (protected) {
                                    warning = "Transaksi terverifikasi atau bagian operasi khusus. Penghapusan akan ditolak atau perlu pemeriksaan ulang."
                                } else {
                                    confirmationOpen = true
                                }
                            }
                        },
                        destructive = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                ArusSecondaryButton(
                    text = "Tutup",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmationOpen) {
        ArusDialog(
            title = "Hapus transaksi?",
            message = "Saldo akan dihitung ulang. Penghapusan dapat diurungkan selama lima detik.",
            confirmText = "Hapus",
            onConfirm = {
                scope.launch {
                    val deletedAt = System.currentTimeMillis()
                    val result = withContext(Dispatchers.IO) {
                        database.transactionDao().softDeleteOrdinaryTrackingCheckpoint(row.id, deletedAt)
                    }
                    if (result == 1) {
                        confirmationOpen = false
                        haptics.confirm()
                        onDismiss()
                    } else {
                        error = "Transaksi tidak dapat dihapus."
                    }
                }
            },
            dismissText = "Batal",
            onDismiss = { confirmationOpen = false },
            isDestructive = true,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
        )
        BasicText(
            text = value,
            style = ArusTheme.typography.bodyLarge.copy(color = ArusTheme.colors.foreground),
        )
    }
}

