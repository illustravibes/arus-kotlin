package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BudgetEntity
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.domain.BudgetPeriod
import com.illustravibes.arus.ui.designsystem.ArusAmountInput
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusCheckRow
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSegmentedControl
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * S-51 Form Budget: Layar pembuatan anggaran baru (Total atau Kategori).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetFormScreen(
    database: ArusDatabase,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberArusHaptics()

    val categories = remember { mutableStateListOf<CategoryEntity>() }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Long?>(null) }
    var isCategoryBudget by remember { mutableStateOf(false) }
    var rollover by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(BudgetPeriod.MONTHLY) }
    var threshold by remember { mutableStateOf("80") }
    var error by remember { mutableStateOf<String?>(null) }
    var categoryPickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(database) {
        val cats = withContext(Dispatchers.IO) {
            database.categoryDao().activeCategories("EXPENSE")
        }
        categories.clear()
        categories.addAll(cats)
    }

    fun submit() {
        val value = amount.toLongOrNull()
        val today = LocalDate.now()
        val period = BudgetPeriod.containing(selectedPeriod, today)
        val warningPercent = threshold.toIntOrNull()

        if (value == null || value <= 0 || (isCategoryBudget && selectedCategory == null) ||
            warningPercent == null || warningPercent !in 50..99
        ) {
            error = "Isi batas anggaran, ambang 50–99%, dan kategori yang valid."
            haptics.reject()
            return
        }

        val budget = BudgetEntity(
            name.ifBlank { if (isCategoryBudget) "Anggaran Kategori" else "Anggaran Total" },
            if (isCategoryBudget) selectedCategory else null,
            value,
            selectedPeriod,
            period.start.toEpochDay(),
            period.endExclusive.toEpochDay(),
            rollover,
            0,
            warningPercent,
            false,
        )

        scope.launch {
            try {
                withContext(Dispatchers.IO) { database.budgetDao().insertValidated(budget) }
                haptics.confirm()
                onSaved()
            } catch (failure: Exception) {
                haptics.reject()
                error = failure.message ?: "Anggaran gagal disimpan."
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background)
            .imePadding(),
    ) {
        ArusTopBar(
            title = "Buat anggaran",
            onBackClick = onDismiss,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Large),
        ) {
            // Scope: Total vs Kategori
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                BasicText(
                    text = "Cakupan Anggaran",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                ArusSegmentedControl(
                    options = listOf("Semua Pengeluaran", "Per Kategori"),
                    selectedIndex = if (!isCategoryBudget) 0 else 1,
                    onOptionSelected = { idx ->
                        isCategoryBudget = idx == 1
                        if (isCategoryBudget && selectedCategory == null) {
                            selectedCategory = categories.firstOrNull()?.id
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (isCategoryBudget) {
                val cat = categories.firstOrNull { it.id == selectedCategory }
                ArusCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { categoryPickerOpen = true },
                    contentPadding = 14.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            BasicText(
                                text = "Kategori",
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            )
                            BasicText(
                                text = cat?.name ?: "Pilih kategori",
                                style = ArusTheme.typography.title,
                            )
                        }
                        ArusTertiaryButton(text = "Ubah", onClick = { categoryPickerOpen = true })
                    }
                }
            }

            // Budget Amount Input
            ArusAmountInput(
                value = amount,
                onValueChange = {
                    amount = it
                    error = null
                },
                showSign = false,
                label = "BATAS MAKSIMAL ANGGARAN",
                modifier = Modifier.fillMaxWidth(),
            )

            // Period Selection
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                BasicText(
                    text = "Periode Berulang",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        BudgetPeriod.WEEKLY to "Mingguan",
                        BudgetPeriod.MONTHLY to "Bulanan",
                        BudgetPeriod.YEARLY to "Tahunan",
                    ).forEach { (pType, label) ->
                        ArusFilterChip(
                            label = label,
                            selected = selectedPeriod == pType,
                            onClick = {
                                haptics.tick()
                                selectedPeriod = pType
                            },
                        )
                    }
                }
            }

            // Name Input
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Nama Anggaran (opsional)",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                ArusTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = if (isCategoryBudget) "Misal: Jatah Makan Bulanan" else "Misal: Anggaran Pengeluaran Mei",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Warning Threshold
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Ambang Peringatan (%)",
                    style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                )
                ArusTextField(
                    value = threshold,
                    onValueChange = { threshold = it.filter(Char::isDigit).take(2) },
                    placeholder = "Default 80%",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Rollover Option
            ArusCheckRow(
                text = "Rollover Anggaran",
                description = "Bawa sisa anggaran yang belum terpakai ke periode berikutnya.",
                checked = rollover,
                onCheckedChange = { rollover = it },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                BasicText(
                    text = it,
                    style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error),
                )
            }
        }

        // Bottom Action Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArusTheme.colors.surface)
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Medium),
        ) {
            ArusPrimaryButton(
                text = "Simpan anggaran",
                onClick = ::submit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (categoryPickerOpen) {
        CategoryPickerSheet(
            categories = categories,
            selectedCategoryId = selectedCategory,
            onCategorySelected = {
                selectedCategory = it.id
                categoryPickerOpen = false
            },
            onDismiss = { categoryPickerOpen = false },
        )
    }
}
