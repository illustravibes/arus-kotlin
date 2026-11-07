package com.illustravibes.arus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.CategoryEntity
import com.illustravibes.arus.data.WalletEntity
import com.illustravibes.arus.ui.ArusFormatting
import com.illustravibes.arus.ui.designsystem.AmountText
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusEmptyState
import com.illustravibes.arus.ui.designsystem.ArusFilterChip
import com.illustravibes.arus.ui.designsystem.rememberArusHaptics
import com.illustravibes.arus.ui.designsystem.ArusIcon
import com.illustravibes.arus.ui.designsystem.ArusIconRegistry
import com.illustravibes.arus.ui.designsystem.ArusSearchField
import com.illustravibes.arus.ui.designsystem.ArusSecondaryButton
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTertiaryButton
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.ArusTopBar
import com.illustravibes.arus.ui.designsystem.TransactionItem
import com.illustravibes.arus.ui.designsystem.TransactionItemUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private fun String.toLocalDateOrNull(): LocalDate? = runCatching { LocalDate.parse(this) }.getOrNull()

/**
 * S-30 Riwayat: Layar riwayat transaksi dengan pencarian, filter chip (Semua, Pengeluaran, Pemasukan, Transfer),
 * grouping berdasarkan tanggal, dan TransactionItem.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionHistoryScreen(
    database: ArusDatabase,
    onSelect: (Long) -> Unit,
    onTransfer: () -> Unit,
    onCheckpoints: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberArusHaptics()
    var query by rememberSaveable { mutableStateOf("") }
    var walletId by rememberSaveable { mutableStateOf<Long?>(null) }
    var transactionType by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var minimumAmount by rememberSaveable { mutableStateOf("") }
    var maximumAmount by rememberSaveable { mutableStateOf("") }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var quickRange by rememberSaveable { mutableStateOf("") }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }

    var walletPickerOpen by rememberSaveable { mutableStateOf(false) }
    var categoryPickerOpen by rememberSaveable { mutableStateOf(false) }

    var wallets by remember { mutableStateOf(emptyList<WalletEntity>()) }
    var categories by remember { mutableStateOf(emptyList<CategoryEntity>()) }
    val categoriesMap = remember { mutableStateMapOf<Long, CategoryEntity>() }

    LaunchedEffect(database) {
        val filters = withContext(Dispatchers.IO) {
            database.walletDao().allWallets() to database.categoryDao().activeCategories()
        }
        wallets = filters.first
        categories = filters.second
        categoriesMap.clear()
        categories.forEach { categoriesMap[it.id] = it }
    }

    val today = LocalDate.now()
    val rangeStartDate = when (quickRange) {
        "Bulan ini" -> today.withDayOfMonth(1)
        "Tahun ini" -> today.withDayOfYear(1)
        else -> startDate.toLocalDateOrNull()
    }
    val rangeEndDate = when (quickRange) {
        "Bulan ini", "Tahun ini" -> today.plusDays(1)
        else -> endDate.toLocalDateOrNull()?.plusDays(1)
    }
    val start = rangeStartDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val end = rangeEndDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val minimum = minimumAmount.toLongOrNull()
    val maximum = maximumAmount.toLongOrNull()

    LaunchedEffect(startDate, endDate, quickRange, minimumAmount, maximumAmount, start, end, minimum, maximum) {
        dateError = when {
            quickRange.isBlank() && startDate.isNotBlank() && startDate.toLocalDateOrNull() == null -> "Tanggal awal tidak valid. Gunakan YYYY-MM-DD."
            quickRange.isBlank() && endDate.isNotBlank() && endDate.toLocalDateOrNull() == null -> "Tanggal akhir tidak valid. Gunakan YYYY-MM-DD."
            start != null && end != null && start >= end -> "Tanggal awal harus sebelum tanggal akhir."
            minimumAmount.isNotBlank() && minimum == null -> "Nominal minimum tidak valid."
            maximumAmount.isNotBlank() && maximum == null -> "Nominal maksimum tidak valid."
            minimum != null && maximum != null && minimum > maximum -> "Nominal minimum harus di bawah nominal maksimum."
            else -> null
        }
    }

    val filterIsValid = (quickRange.isNotBlank() || startDate.isBlank() || start != null) &&
        (quickRange.isNotBlank() || endDate.isBlank() || end != null) &&
        !(start != null && end != null && start >= end) && (minimumAmount.isBlank() || minimum != null) &&
        (maximumAmount.isBlank() || maximum != null) && !(minimum != null && maximum != null && minimum > maximum)

    val pager = remember(database, query, walletId, transactionType, categoryId, minimum, maximum, start, end, filterIsValid) {
        Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
            database.transactionDao().searchTransactionHistory(
                query.trim(), walletId, transactionType.ifBlank { null },
                categoryId, minimum, maximum, if (filterIsValid) start else Long.MAX_VALUE,
                if (filterIsValid) end else Long.MIN_VALUE,
            )
        }
    }

    val transactions = pager.flow.collectAsLazyPagingItems()
    val resultTotal = remember(transactions.itemSnapshotList.items) {
        transactions.itemSnapshotList.items.sumOf { it.amountRupiah }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArusTheme.colors.background),
    ) {
        ArusTopBar(
            title = "Transaksi",
            onBackClick = null,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = ArusSpacing.Large, vertical = ArusSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
        ) {
            // Search Input
            item {
                ArusSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Cari transaksi, catatan, merchant",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Quick Filter Chips (Semua, Pengeluaran, Pemasukan, Transfer)
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ArusFilterChip(
                        label = "Semua",
                        selected = transactionType.isEmpty(),
                        onClick = {
                            haptics.tick()
                            transactionType = ""
                        },
                    )
                    ArusFilterChip(
                        label = "Pengeluaran",
                        selected = transactionType == "EXPENSE",
                        onClick = {
                            haptics.tick()
                            transactionType = if (transactionType == "EXPENSE") "" else "EXPENSE"
                        },
                    )
                    ArusFilterChip(
                        label = "Pemasukan",
                        selected = transactionType == "INCOME",
                        onClick = {
                            haptics.tick()
                            transactionType = if (transactionType == "INCOME") "" else "INCOME"
                        },
                    )
                    ArusFilterChip(
                        label = "Transfer",
                        selected = transactionType == "TRANSFER_OUT",
                        onClick = {
                            haptics.tick()
                            transactionType = if (transactionType == "TRANSFER_OUT") "" else "TRANSFER_OUT"
                        },
                    )
                }
            }

            // Wallet, Category & Date Range filters
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ArusFilterChip(
                        label = wallets.firstOrNull { it.id == walletId }?.name ?: "Semua dompet",
                        selected = walletId != null,
                        onClick = {
                            haptics.tick()
                            walletPickerOpen = true
                        },
                    )
                    ArusFilterChip(
                        label = categories.firstOrNull { it.id == categoryId }?.name ?: "Semua kategori",
                        selected = categoryId != null,
                        onClick = {
                            haptics.tick()
                            categoryPickerOpen = true
                        },
                    )
                    listOf("" to "Semua tanggal", "Bulan ini" to "Bulan ini", "Tahun ini" to "Tahun ini").forEach { (value, label) ->
                        if (value.isNotEmpty()) {
                            ArusFilterChip(
                                label = label,
                                selected = quickRange == value,
                                onClick = {
                                    haptics.tick()
                                    quickRange = if (quickRange == value) "" else value
                                    if (quickRange.isNotBlank()) {
                                        startDate = ""
                                        endDate = ""
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Results count and total summary
            item {
                ArusCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            BasicText(
                                text = "${transactions.itemCount} transaksi ditemukan",
                                style = ArusTheme.typography.bodySmall.copy(color = ArusTheme.colors.mutedForeground),
                            )
                            AmountText(
                                amount = resultTotal,
                                style = ArusTheme.typography.title,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ArusTertiaryButton(text = "Rekonsiliasi", onClick = onCheckpoints)
                            ArusTertiaryButton(text = "Transfer", onClick = onTransfer)
                        }
                    }
                }
            }

            // Transactions list grouped by date
            if (transactions.itemCount == 0) {
                item {
                    ArusEmptyState(
                        title = "Tidak ada transaksi",
                        description = "Belum ada transaksi yang sesuai dengan filter atau kata kunci ini.",
                        icon = ArusIconRegistry.RECEIPT,
                        modifier = Modifier.padding(vertical = ArusSpacing.Large),
                    )
                }
            } else {
                items(count = transactions.itemCount, key = transactions.itemKey { it.id }) { index ->
                    val item = transactions[index] ?: return@items
                    val day = Instant.ofEpochMilli(item.occurredAtEpochMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    val isFirstInGroup = index == 0 || transactions.peek(index - 1)?.let {
                        Instant.ofEpochMilli(it.occurredAtEpochMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate() != day
                    } != false

                    if (isFirstInGroup) {
                        val headerText = when (day) {
                            today -> "HARI INI"
                            today.minusDays(1) -> "KEMARIN"
                            else -> ArusFormatting.formatDate(day).uppercase()
                        }
                        BasicText(
                            text = headerText,
                            style = ArusTheme.typography.label.copy(color = ArusTheme.colors.mutedForeground),
                            modifier = Modifier.padding(top = ArusSpacing.Medium, bottom = 4.dp),
                        )
                    }

                    val category = item.categoryId?.let { categoriesMap[it] }
                    val title = item.merchant?.takeIf(String::isNotBlank)
                        ?: item.note?.takeIf(String::isNotBlank)
                        ?: category?.name
                        ?: item.type.name

                    val subtitle = buildString {
                        val wallet = wallets.firstOrNull { it.id == item.walletId }
                        wallet?.name?.let { append(it) }
                        category?.name?.let {
                            if (isNotEmpty()) append(" · ")
                            append(it)
                        }
                    }

                    val dateStr = ArusFormatting.formatDate(day)
                    TransactionItem(
                        model = TransactionItemUiModel(
                            id = item.id,
                            type = item.type.name,
                            amount = item.amountRupiah,
                            title = title,
                            categoryName = category?.name,
                            categoryIcon = category?.icon,
                            dateFormatted = dateStr,
                            subtitle = subtitle,
                        ),
                        onClick = {
                            haptics.click()
                            onSelect(item.id)
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (categoryPickerOpen) {
        CategoryPickerSheet(
            categories = categories,
            selectedCategoryId = categoryId,
            onCategorySelected = { categoryId = it.id },
            onDismiss = { categoryPickerOpen = false },
        )
    }

    if (walletPickerOpen) {
        WalletPickerSheet(
            wallets = wallets,
            walletBalances = emptyMap(),
            selectedWalletId = walletId,
            onWalletSelected = { walletId = it.id },
            onDismiss = { walletPickerOpen = false },
        )
    }
}
