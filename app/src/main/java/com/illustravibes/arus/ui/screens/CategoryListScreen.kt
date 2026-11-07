package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusEmptyState
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusPrimaryButton
import com.illustravibes.arus.ui.designsystem.ArusSearchField
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSegmentedControl
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * S-40 Kategori: Layar daftar kategori pengeluaran dan pemasukan dengan pencarian,
 * aksi edit, arsip, naik urutan, dan hapus.
 */
@Composable
fun CategoryListScreen(
    database: ArusDatabase,
    onBackClick: () -> Unit,
    onAddCategory: (String) -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    refreshKey: Int = 0,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberArusHaptics()

    val categories = remember { mutableStateListOf<CategoryEntity>() }
    val wallets = remember { mutableStateListOf<WalletEntity>() }

    var kind by rememberSaveable { mutableStateOf("EXPENSE") }
    var search by rememberSaveable { mutableStateOf("") }
    var defaultWalletWalletToPick by remember { mutableStateOf<WalletEntity?>(null) }

    fun refresh() {
        scope.launch {
            val values = withContext(Dispatchers.IO) {
                database.categoryDao().allCategories() to database.walletDao().activeWallets()
            }
            categories.clear()
            categories.addAll(values.first)
            wallets.clear()
            wallets.addAll(values.second)
        }
    }

    LaunchedEffect(database, refreshKey) { refresh() }

    val orderedCategories = categories.filter { it.kind == kind && !it.isArchived }
    val filtered = orderedCategories.filter {
        search.isBlank() || it.name.contains(search.trim(), ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background),
    ) {
        ArusTopBar(
            title = "Kategori",
            onBackClick = onBackClick,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            // Segmented Kind Selector: Pengeluaran / Pemasukan
            item {
                ArusSegmentedControl(
                    options = listOf("Pengeluaran", "Pemasukan"),
                    selectedIndex = if (kind == "EXPENSE") 0 else 1,
                    onOptionSelected = { idx ->
                        kind = if (idx == 0) "EXPENSE" else "INCOME"
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Search Field
            item {
                ArusSearchField(
                    query = search,
                    onQueryChange = { search = it },
                    placeholder = "Cari kategori",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (filtered.isEmpty()) {
                item {
                    ArusEmptyState(
                        title = "Kategori tidak ditemukan",
                        description = if (search.isNotBlank()) "Tidak ada kategori yang cocok dengan pencarian."
                        else "Belum ada kategori untuk jenis ini.",
                        icon = ArusIconRegistry.CATEGORY,
                    )
                }
            } else {
                items(filtered, key = { it.id }) { cat ->
                    ArusCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = 12.dp,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val catColor = if (cat.colorArgb != 0) Color(cat.colorArgb) else ArusTheme.colors.muted
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ArusIcon(
                                        key = cat.icon ?: ArusIconRegistry.CATEGORY,
                                        tint = if (cat.colorArgb != 0) catColor else ArusTheme.colors.foreground,
                                        size = 18.dp,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    BasicText(
                                        text = cat.name + if (cat.isSystem) " · Sistem" else "",
                                        style = ArusTheme.typography.title,
                                    )
                                    if (cat.isArchived) {
                                        BasicText(
                                            text = "Diarsipkan",
                                            style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                                        )
                                    }
                                }
                            }

                            if (!cat.isSystem) {
                                ArusTertiaryButton(
                                    text = "Naik",
                                    enabled = orderedCategories.firstOrNull()?.id != cat.id,
                                    onClick = {
                                        val previous = orderedCategories.getOrNull(orderedCategories.indexOf(cat) - 1)
                                        if (previous != null) scope.launch {
                                            withContext(Dispatchers.IO) {
                                                database.runInTransaction {
                                                    database.categoryDao().updateSortOrder(cat.id, previous.sortOrder)
                                                    database.categoryDao().updateSortOrder(previous.id, cat.sortOrder)
                                                }
                                            }
                                            refresh()
                                        }
                                    },
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    ArusSecondaryButton(
                                        text = "Edit",
                                        onClick = {
                                            haptics.tick()
                                            onEditCategory(cat)
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    ArusSecondaryButton(
                                        text = "Arsip",
                                        onClick = {
                                            haptics.tick()
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    database.categoryDao().archive(cat.id)
                                                }
                                                refresh()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    ArusSecondaryButton(
                                        text = "Hapus",
                                        onClick = {
                                            haptics.warning()
                                            onDeleteCategory(cat)
                                        },
                                        destructive = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Wallet Default Categories Section
            item {
                Spacer(Modifier.height(8.dp))
                BasicText(
                    text = "Kategori default dompet",
                    style = ArusTheme.typography.title,
                )
            }

            val expenseCategories = categories.filter { it.kind == "EXPENSE" && !it.isArchived }
            items(wallets.filter { it.type != "CREDIT" }, key = { "wallet_default_${it.id}" }) { wallet ->
                val currentDefault = categories.firstOrNull { it.id == wallet.defaultExpenseCategoryId }
                ArusCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { defaultWalletWalletToPick = wallet },
                    contentPadding = 12.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            BasicText(text = wallet.name, style = ArusTheme.typography.title)
                            BasicText(
                                text = "Default: ${currentDefault?.name ?: "Pilih kategori"}",
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            )
                        }
                        ArusIcon(key = ArusIconRegistry.CHEVRON_RIGHT, size = 16.dp, tint = ArusTheme.colors.mutedForeground)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Bottom Add Category Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ArusTheme.colors.surface)
                .padding(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Medium),
        ) {
            ArusPrimaryButton(
                text = "+ Tambah kategori",
                onClick = {
                    haptics.tick()
                    onAddCategory(kind)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Default Category Picker for Wallet
    defaultWalletWalletToPick?.let { targetWallet ->
        CategoryPickerSheet(
            categories = categories.filter { it.kind == "EXPENSE" && !it.isArchived },
            selectedCategoryId = targetWallet.defaultExpenseCategoryId,
            onCategorySelected = { pickedCat ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        database.walletDao().updateDefaultExpenseCategory(targetWallet.id, pickedCat.id)
                    }
                    defaultWalletWalletToPick = null
                    refresh()
                }
            },
            onDismiss = { defaultWalletWalletToPick = null },
        )
    }
}

