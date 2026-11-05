package com.illustravibes.arus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.ReportTotals
import com.illustravibes.arus.domain.BudgetPeriod
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ReportScreen(database: ArusDatabase) {
    var periodType by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
    var totals by remember { mutableStateOf<ReportTotals?>(null) }
    val period = BudgetPeriod.containing(periodType, anchorDate)

    LaunchedEffect(database, period.start, period.endExclusive) {
        totals = withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            database.transactionDao().reportTotals(
                period.start.atStartOfDay(zone).toInstant().toEpochMilli(),
                period.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli(),
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Laporan", style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.headline)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(BudgetPeriod.WEEKLY to "Minggu", BudgetPeriod.MONTHLY to "Bulan", BudgetPeriod.YEARLY to "Tahun").forEach { (type, label) ->
                ArusButton(onClick = { periodType = type }, selected = periodType == type) { Text(label) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArusButton(onClick = {
                anchorDate = when (periodType) {
                    BudgetPeriod.WEEKLY -> anchorDate.minusWeeks(1)
                    BudgetPeriod.MONTHLY -> anchorDate.minusMonths(1)
                    else -> anchorDate.minusYears(1)
                }
            }, kind = com.illustravibes.arus.ui.designsystem.ArusButtonKind.Outline) { Text("Sebelumnya") }
            ArusButton(onClick = { anchorDate = LocalDate.now() }, kind = com.illustravibes.arus.ui.designsystem.ArusButtonKind.Outline) { Text("Hari ini") }
            ArusButton(onClick = {
                anchorDate = when (periodType) {
                    BudgetPeriod.WEEKLY -> anchorDate.plusWeeks(1)
                    BudgetPeriod.MONTHLY -> anchorDate.plusMonths(1)
                    else -> anchorDate.plusYears(1)
                }
            }, kind = com.illustravibes.arus.ui.designsystem.ArusButtonKind.Outline) { Text("Berikutnya") }
        }
        Text("${period.start} – ${period.endExclusive.minusDays(1)}")
        totals?.let { report ->
            ArusCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pemasukan: ${formatRupiah(report.incomeRupiah)}")
                    Text("Pengeluaran: ${formatRupiah(report.expenseRupiah)}")
                    Text("Selisih: ${formatRupiah(report.incomeRupiah - report.expenseRupiah)}")
                }
            }
        }
    }
}
