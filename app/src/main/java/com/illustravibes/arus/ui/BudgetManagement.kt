package com.illustravibes.arus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BudgetEntity
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.domain.BudgetProgress
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun BudgetManagement(database: ArusDatabase) {
    val scope = rememberCoroutineScope()
    val budgets = remember { mutableStateListOf<BudgetEntity>() }
    val categories = remember { mutableStateListOf<CategoryEntity>() }
    var name by remember { androidx.compose.runtime.mutableStateOf("") }
    var amount by remember { androidx.compose.runtime.mutableStateOf("") }
    var selectedCategory by remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var categoryBudget by remember { androidx.compose.runtime.mutableStateOf(false) }
    var rollover by remember { androidx.compose.runtime.mutableStateOf(false) }
    var error by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var selectedPeriod by remember { mutableStateOf(com.illustravibes.arus.domain.BudgetPeriod.MONTHLY) }
    var threshold by remember { mutableStateOf("80") }
    var categoryPickerOpen by remember { mutableStateOf(false) }
    var showBudgetForm by remember { mutableStateOf(false) }

    suspend fun refresh() {
        val data = withContext(Dispatchers.IO) {
            database.budgetDao().advanceDueBudgets(LocalDate.now().toEpochDay())
            database.budgetDao().activeBudgets() to database.categoryDao().activeCategories("EXPENSE")
        }
        budgets.clear(); budgets.addAll(data.first)
        categories.clear(); categories.addAll(data.second)
    }
    LaunchedEffect(database) { refresh() }

    if (categoryPickerOpen) {
        com.illustravibes.arus.ui.designsystem.ArusDialog(
            onDismissRequest = { categoryPickerOpen = false },
            title = { Text("Pilih kategori pengeluaran") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        ArusButton(onClick = { selectedCategory = category.id; categoryPickerOpen = false }, selected = selectedCategory == category.id) {
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = { ArusButton(onClick = { categoryPickerOpen = false }) { Text("Tutup") } },
        )
    }

    LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Anggaran", style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.headline)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Anggaran berjalan", style = com.illustravibes.arus.ui.designsystem.ArusTheme.typography.title)
                ArusButton(onClick = { showBudgetForm = !showBudgetForm; categoryBudget = false; selectedCategory = null }, kind = com.illustravibes.arus.ui.designsystem.ArusButtonKind.Primary) { Text(if (showBudgetForm) "Batal" else "Tambah") }
            }
            if (budgets.isEmpty() && !showBudgetForm) {
                ArusCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Belum ada anggaran berjalan.")
                        Text("Tambah anggaran untuk memantau pengeluaran periode ini.")
                    }
                }
            }
            if (showBudgetForm) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArusButton(onClick = { categoryBudget = false; selectedCategory = null }, selected = !categoryBudget) { Text("Total") }
            ArusButton(onClick = { categoryBudget = true; selectedCategory = categories.firstOrNull()?.id }, selected = categoryBudget) { Text("Kategori") }
            }
            if (categoryBudget) {
                ArusButton(onClick = { categoryPickerOpen = true }) { Text(categories.firstOrNull { it.id == selectedCategory }?.name ?: "Pilih kategori") }
            }
            ArusTextField(name, { name = it }, placeholder = "Nama anggaran", singleLine = true)
            ArusTextField(amount, { amount = it.filter(Char::isDigit) }, placeholder = "Batas anggaran (Rp)", singleLine = true)
            ArusButton(onClick = { rollover = !rollover }) { Text(if (rollover) "Rollover aktif" else "Rollover nonaktif") }
            ArusButton(onClick = { selectedPeriod = com.illustravibes.arus.domain.BudgetPeriod.WEEKLY }, selected = selectedPeriod == com.illustravibes.arus.domain.BudgetPeriod.WEEKLY) { Text("Mingguan") }
            ArusButton(onClick = { selectedPeriod = com.illustravibes.arus.domain.BudgetPeriod.MONTHLY }, selected = selectedPeriod == com.illustravibes.arus.domain.BudgetPeriod.MONTHLY) { Text("Bulanan") }
            ArusButton(onClick = { selectedPeriod = com.illustravibes.arus.domain.BudgetPeriod.YEARLY }, selected = selectedPeriod == com.illustravibes.arus.domain.BudgetPeriod.YEARLY) { Text("Tahunan") }
            ArusTextField(threshold, { threshold = it.filter(Char::isDigit) }, placeholder = "Ambang peringatan (%)", singleLine = true)
            error?.let { Text(it, color = com.illustravibes.arus.ui.designsystem.ArusTheme.colors.error) }
            ArusButton(onClick = {
            val value = amount.toLongOrNull()
            val today = LocalDate.now()
            val period = com.illustravibes.arus.domain.BudgetPeriod.containing(selectedPeriod, today)
            val warningPercent = threshold.toIntOrNull()
            if (value == null || value <= 0 || (categoryBudget && selectedCategory == null) || warningPercent == null || warningPercent !in 50..99) {
                error = "Isi batas anggaran, ambang 50–99%, dan kategori yang valid."
            } else {
                val budget = BudgetEntity(name.ifBlank { if (categoryBudget) "Anggaran kategori" else "Anggaran total" },
                    if (categoryBudget) selectedCategory else null, value,
                    selectedPeriod,
                    period.start.toEpochDay(), period.endExclusive.toEpochDay(), rollover, 0, warningPercent, false)
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { database.budgetDao().insertValidated(budget) }
                        name = ""; amount = ""; error = null; showBudgetForm = false; refresh()
                    } catch (failure: Exception) {
                        error = failure.message ?: "Anggaran gagal disimpan."
                    }
                }
            }
            }) { Text("Simpan anggaran") }
            }
        }
        }
        items(budgets, key = { it.id }) { budget ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val progress = remember(budget.id, budgets.size) {
                    BudgetProgress.load(database.budgetDao(), budget, LocalDate.now(), ZoneId.systemDefault())
                }
                ArusCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(budget.name)
                        Text(
                            "${periodLabel(budget.periodType)} · ${ArusFormatting.formatDate(java.time.LocalDate.ofEpochDay(budget.periodStartEpochDay))} – ${ArusFormatting.formatDate(java.time.LocalDate.ofEpochDay(budget.periodEndEpochDay).minusDays(1))}",
                        )
                        Text(if (budget.categoryId == null) "Semua kategori" else "Kategori pengeluaran")
                        if (budget.rolloverEnabled) Text("Sisa dibawa: ${formatRupiah(progress.rolloverRupiah)}")
                        Text("Terpakai ${formatRupiah(progress.spentRupiah)} dari ${formatRupiah(progress.effectiveLimitRupiah)}")
                        val percent = if (progress.effectiveLimitRupiah > 0) (progress.spentRupiah.toDouble() * 100 / progress.effectiveLimitRupiah).toInt().coerceAtLeast(0) else 0
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Terpakai")
                            Text("$percent% · sisa ${formatRupiah((progress.effectiveLimitRupiah - progress.spentRupiah).coerceAtLeast(0))}")
                        }
                        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(16.dp)) {
                            drawRoundRect(androidx.compose.ui.graphics.Color(0xFFE8E8E8), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                            val fraction = (progress.spentRupiah.toFloat() / progress.effectiveLimitRupiah.coerceAtLeast(1)).coerceIn(0f, 1f)
                            drawRoundRect(androidx.compose.ui.graphics.Color(0xFFB8BEC4), size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                        }
                        if (progress.warningLevel >= budget.warningPercent) Text(if (progress.warningLevel == 100) "Batas anggaran tercapai" else "Mendekati batas anggaran")
                        progress.transactions.forEach { detail ->
                            Text("${ArusFormatting.formatDate(java.time.Instant.ofEpochMilli(detail.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate())} · ${detail.note ?: "Pengeluaran"} · ${formatRupiah(detail.amountRupiah)}")
                        }
                    }
                }
            }
        }
    }
}

private fun periodLabel(type: Int): String = when (type) {
    com.illustravibes.arus.domain.BudgetPeriod.WEEKLY -> "Mingguan"
    com.illustravibes.arus.domain.BudgetPeriod.MONTHLY -> "Bulanan"
    com.illustravibes.arus.domain.BudgetPeriod.YEARLY -> "Tahunan"
    else -> "Periode tidak dikenal"
}
