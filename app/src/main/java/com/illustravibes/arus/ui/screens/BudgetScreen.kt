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
import com.illustravibes.arus.data.BudgetEntity
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.domain.BudgetPeriod
import com.illustravibes.arus.ui.ArusFormatting
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusEmptyState
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.BudgetProgress as BudgetProgressComponent
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun budgetPeriodLabel(type: Int): String = when (type) {
    BudgetPeriod.WEEKLY -> "Mingguan"
    BudgetPeriod.MONTHLY -> "Bulanan"
    BudgetPeriod.YEARLY -> "Tahunan"
    else -> "Periode"
}

/**
 * S-50 Anggaran: Layar pemantauan anggaran pengeluaran aktif dengan bar progres
 * (normal, 80%+ warning, terlampaui), dan daftar transaksi terkait.
 */
@Composable
fun BudgetScreen(
    database: ArusDatabase,
    onBackClick: () -> Unit,
    onAddBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    val budgets = remember { mutableStateListOf<BudgetEntity>() }
    val categories = remember { mutableStateListOf<CategoryEntity>() }

    suspend fun refresh() {
        val data = withContext(Dispatchers.IO) {
            database.budgetDao().advanceDueBudgets(LocalDate.now().toEpochDay())
            database.budgetDao().activeBudgets() to database.categoryDao().activeCategories("EXPENSE")
        }
        budgets.clear()
        budgets.addAll(data.first)
        categories.clear()
        categories.addAll(data.second)
    }

    LaunchedEffect(database) { refresh() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background),
    ) {
        ArusTopBar(
            title = "Anggaran",
            onBackClick = onBackClick,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
        ) {
            if (budgets.isEmpty()) {
                item {
                    ArusEmptyState(
                        title = "Belum ada anggaran",
                        description = "Tetapkan batas anggaran bulanan atau per kategori untuk menjaga pengeluaran tetap terkendali.",
                        icon = ArusIconRegistry.PIGGY_BANK,
                    )
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    val progress = remember(budget.id, budgets.size) {
                        com.illustravibes.arus.domain.BudgetProgress.load(
                            database.budgetDao(), budget, LocalDate.now(), ZoneId.systemDefault()
                        )
                    }

                    val cat = categories.firstOrNull { it.id == budget.categoryId }
                    val label = cat?.name ?: "Semua Kategori"
                    val period = "${budgetPeriodLabel(budget.periodType)} · " +
                        "${ArusFormatting.formatDate(LocalDate.ofEpochDay(budget.periodStartEpochDay))} – " +
                        ArusFormatting.formatDate(LocalDate.ofEpochDay(budget.periodEndEpochDay).minusDays(1))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetProgressComponent(
                            spentAmount = progress.spentRupiah,
                            limitAmount = progress.effectiveLimitRupiah,
                            categoryName = budget.name.ifBlank { label },
                            periodLabel = period,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Transaksi yang masuk ke anggaran ini
                        if (progress.transactions.isNotEmpty()) {
                            ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BasicText(
                                        text = "Transaksi Periode Ini (${progress.transactions.size})",
                                        style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                                    )
                                    progress.transactions.take(5).forEach { detail ->
                                        val date = Instant.ofEpochMilli(detail.occurredAtEpochMillis)
                                            .atZone(ZoneId.systemDefault()).toLocalDate()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            BasicText(
                                                text = "${ArusFormatting.formatDate(date)} · ${detail.note ?: "Pengeluaran"}",
                                                style = ArusTheme.typography.bodySmall,
                                            )
                                            BasicText(
                                                text = "-${ArusFormatting.formatRupiah(detail.amountRupiah)}",
                                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.foreground),
                                            )
                                        }
                                    }
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

        // Bottom Add Budget Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArusTheme.colors.surface)
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Medium),
        ) {
            ArusPrimaryButton(
                text = "+ Buat anggaran",
                onClick = {
                    haptics.tick()
                    onAddBudget()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

