package com.illustravibes.arus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.ReconciliationCheckpointEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
internal fun CheckpointHistory(
    database: ArusDatabase,
    selectedWalletId: Long?,
    onWalletSelected: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var wallets by remember { mutableStateOf(emptyList<WalletEntity>()) }
    var checkpoints by remember { mutableStateOf(emptyList<ReconciliationCheckpointEntity>()) }
    var monthlyTotals by remember {
        mutableStateOf(emptyList<com.illustravibes.arus.data.MonthlyUntrackedTotal>())
    }
    LaunchedEffect(database) {
        wallets = withContext(Dispatchers.IO) { database.walletDao().allWallets() }
        if (selectedWalletId == null) wallets.firstOrNull()?.id?.let(onWalletSelected)
    }
    LaunchedEffect(database, selectedWalletId) {
        val wallet = selectedWalletId ?: return@LaunchedEffect
        val start = LocalDate.now().minusMonths(2).withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = LocalDate.now().plusMonths(1).withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val data = withContext(Dispatchers.IO) {
            database.reconciliationCheckpointDao().checkpointsForWallet(wallet) to
                database.reconciliationCheckpointDao().monthlyUntrackedTotals(wallet, start, end)
        }
        checkpoints = data.first
        monthlyTotals = data.second
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Text("Riwayat rekonsiliasi", style = ArusTheme.typography.headline)
        ArusButton(onClick = onBack, kind = ArusButtonKind.Outline) { Text("Kembali ke transaksi") }
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            wallets.forEach { wallet ->
                ArusButton(
                    onClick = { onWalletSelected(wallet.id) },
                    selected = selectedWalletId == wallet.id,
                    kind = ArusButtonKind.Outline,
                ) { Text(wallet.name) }
            }
        }
        if (monthlyTotals.isNotEmpty()) {
            ArusCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(ArusSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    Text("Tak terlacak per bulan", style = ArusTheme.typography.title)
                    val average = monthlyTotals.map { it.untrackedRupiah }.average().toLong()
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().height(150.dp),
                        horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        val maximum = monthlyTotals.maxOf { it.untrackedRupiah }.coerceAtLeast(1L)
                        val barColor = ArusTheme.colors.muted
                        monthlyTotals.forEach { total ->
                            val monthLabel = runCatching {
                                java.time.YearMonth.parse(total.month).month.getDisplayName(
                                    java.time.format.TextStyle.SHORT,
                                    Locale.forLanguageTag("id-ID"),
                                )
                            }.getOrDefault(total.month)
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(formatRupiah(total.untrackedRupiah))
                                androidx.compose.foundation.Canvas(
                                    Modifier.fillMaxWidth().weight(
                                        (total.untrackedRupiah.toFloat() / maximum).coerceAtLeast(0.08f),
                                    ),
                                ) { drawRect(barColor) }
                                Text(monthLabel)
                            }
                        }
                    }
                    ArusCard(Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Row(
                            Modifier.padding(ArusSpacing.Small).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Rata-rata tak terlacak")
                            Text("${formatRupiah(average)} / bulan")
                        }
                    }
                }
            }
        }
        Text("CHECKPOINT", style = ArusTheme.typography.label)
        if (checkpoints.isEmpty()) {
            Text("Belum ada checkpoint untuk dompet ini.", color = ArusTheme.colors.mutedForeground)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            items(checkpoints, key = { it.id }) { checkpoint ->
                ArusCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(ArusSpacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            ArusFormatting.formatDateTime(
                                java.time.Instant.ofEpochMilli(checkpoint.occurredAtEpochMillis)
                                    .atZone(ZoneId.systemDefault()),
                            ),
                        )
                        Text("Saldo tercatat · ${formatRupiah(checkpoint.recordedBalanceRupiah)}")
                        Text("Saldo aktual · ${formatRupiah(checkpoint.actualBalanceRupiah)}")
                        Text("Selisih · ${formatRupiah(checkpoint.differenceRupiah)}")
                        Text(checkpoint.resolution ?: "Tanpa resolusi")
                        if (checkpoint.needsRecheck) {
                            Text("Perlu rekonsiliasi ulang", color = ArusTheme.colors.error)
                        }
                    }
                }
            }
        }
    }
}
