package com.illustravibes.arus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
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
import com.illustravibes.arus.data.ReportCategoryTotal
import com.illustravibes.arus.data.ReportTransactionRow
import com.illustravibes.arus.domain.BudgetPeriod
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import com.illustravibes.arus.ui.designsystem.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ReportScreen(database: ArusDatabase, periodStartDay: Int = 1) {
    var periodType by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var anchorDate by remember { mutableStateOf(LocalDate.now()) }
    var totals by remember { mutableStateOf<ReportTotals?>(null) }
    var expenseCategories by remember { mutableStateOf(emptyList<ReportCategoryTotal>()) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryTransactions by remember { mutableStateOf(emptyList<ReportTransactionRow>()) }
    val period = BudgetPeriod.containing(periodType, anchorDate, periodStartDay)

    LaunchedEffect(database, period.start, period.endExclusive) {
        val data = withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val startMillis = period.start.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = period.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
            database.transactionDao().reportTotals(
                startMillis, endMillis,
            ) to database.transactionDao().reportExpenseCategories(startMillis, endMillis)
        }
        totals = data.first
        expenseCategories = data.second
    }

    LaunchedEffect(database, selectedCategoryId, period.start, period.endExclusive) {
        val categoryId = selectedCategoryId ?: return@LaunchedEffect
        val transactions = withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            database.transactionDao().reportTransactionsForCategory(
                categoryId,
                period.start.atStartOfDay(zone).toInstant().toEpochMilli(),
                period.endExclusive.atStartOfDay(zone).toInstant().toEpochMilli(),
            )
        }
        selectedCategoryTransactions = transactions
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Laporan", style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.headline)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(BudgetPeriod.WEEKLY to "Minggu", BudgetPeriod.MONTHLY to "Bulan", BudgetPeriod.YEARLY to "Tahun").forEach { (type, label) ->
                ArusButton(onClick = { periodType = type }, selected = periodType == type) { Text(label) }
            }
        }
        ArusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ArusButton(onClick = {
                anchorDate = when (periodType) {
                    BudgetPeriod.WEEKLY -> anchorDate.minusWeeks(1)
                    BudgetPeriod.MONTHLY -> anchorDate.minusMonths(1)
                    else -> anchorDate.minusYears(1)
                }
                }, kind = com.illustravibes.arus.ui.designsystem.ArusButtonKind.Outline) { Text("‹") }
                Text("${ArusFormatting.formatDate(period.start)} – ${ArusFormatting.formatDate(period.endExclusive.minusDays(1))}")
                ArusButton(onClick = {
                anchorDate = when (periodType) {
                    BudgetPeriod.WEEKLY -> anchorDate.plusWeeks(1)
                    BudgetPeriod.MONTHLY -> anchorDate.plusMonths(1)
                    else -> anchorDate.plusYears(1)
                }
                }, enabled = !period.endExclusive.isAfter(LocalDate.now().plusDays(1)), kind = com.illustravibes.arus.ui.designsystem.ArusButtonKind.Outline) { Text("›") }
            }
        }
        totals?.let { report ->
            ArusCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val total = report.incomeRupiah + report.expenseRupiah
                    if (expenseCategories.isNotEmpty()) {
                        val categoryTotal = expenseCategories.sumOf { it.amountRupiah }.coerceAtLeast(1L)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            androidx.compose.foundation.Canvas(Modifier.size(108.dp)) {
                                val colors = listOf(Color(0xFFB8BEC4), Color(0xFFD3E6FA), Color(0xFFE8E8E8))
                                var startAngle = -90f
                                expenseCategories.forEachIndexed { index, category ->
                                    val sweep = 360f * category.amountRupiah / categoryTotal
                                    drawArc(colors[index % colors.size], startAngle, sweep, false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 18.dp.toPx()))
                                    startAngle += sweep
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                expenseCategories.forEach { category ->
                                    ArusButton(
                                        onClick = { selectedCategoryId = category.categoryId },
                                        kind = ArusButtonKind.Ghost,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("${category.categoryName} · ${category.amountRupiah * 100 / categoryTotal}%")
                                    }
                                }
                            }
                        }
                    } else Text("Belum ada rincian kategori pengeluaran pada periode ini.")
                    if (report.incomeRupiah > 0) ReportAmount("Pemasukan", report.incomeRupiah, report.incomeRupiah, total)
                    if (report.expenseRupiah > 0) ReportAmount("Pengeluaran", report.expenseRupiah, report.expenseRupiah, total)
                    Text("Selisih: ${formatRupiah(report.incomeRupiah - report.expenseRupiah)}")
                }
            }
        }
    }

    selectedCategoryId?.let { categoryId ->
        val categoryName = expenseCategories.firstOrNull { it.categoryId == categoryId }?.categoryName.orEmpty()
        ArusDialog(
            onDismissRequest = { selectedCategoryId = null; selectedCategoryTransactions = emptyList() },
            title = { Text(categoryName) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    Text("${ArusFormatting.formatDate(period.start)} sampai ${ArusFormatting.formatDate(period.endExclusive.minusDays(1))}")
                    if (selectedCategoryTransactions.isEmpty()) {
                        Text("Belum ada transaksi kategori ini pada periode tersebut.")
                    } else selectedCategoryTransactions.forEach { row ->
                        ArusCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(ArusSpacing.Small), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(row.merchant?.takeIf(String::isNotBlank) ?: row.note?.takeIf(String::isNotBlank) ?: row.type)
                                Text("${ArusFormatting.formatDate(java.time.Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate())} · ${row.walletName.orEmpty()}")
                                Text(formatRupiah(row.amountRupiah), style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.title)
                            }
                        }
                    }
                }
            },
            confirmButton = { OutlinedArusButton(onClick = { selectedCategoryId = null; selectedCategoryTransactions = emptyList() }) { Text("Tutup") } },
        )
    }
}

@Composable
private fun ReportAmount(label: String, amount: Long, value: Long, total: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(formatRupiah(amount))
        }
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(14.dp)) {
            drawRoundRect(Color(0xFFE8E8E8), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
            val fraction = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f
            drawRoundRect(Color(0xFFD3E6FA), size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
        }
    }
}
