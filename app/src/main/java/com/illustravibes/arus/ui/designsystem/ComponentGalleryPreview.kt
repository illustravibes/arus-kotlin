package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Arus Component Gallery", showBackground = true, widthDp = 390, heightDp = 3600)
@Composable
fun ComponentGalleryPreview() {
    ArusTheme {
        val colors = ArusTheme.colors
        val typography = ArusTheme.typography

        var textValue by remember { mutableStateOf("Catatan makan siang") }
        var amountDigits by remember { mutableStateOf("65000") }
        var searchQuery by remember { mutableStateOf("") }
        var chipSelected by remember { mutableStateOf(true) }
        var switchState by remember { mutableStateOf(true) }
        var segmentedIndex by remember { mutableIntStateOf(0) }
        var checkRowChecked by remember { mutableStateOf(true) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(rememberScrollState())
                .padding(ArusSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(ArusSpacing.XLarge),
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(
                    text = "Arus Components",
                    style = typography.headlineLarge.copy(color = colors.foreground),
                )
                BasicText(
                    text = "Galeri komponen inti dan komponen domain keuangan.",
                    style = typography.bodyMedium.copy(color = colors.mutedForeground),
                )
            }

            // 1. Tombol (Buttons)
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("1. TOMBOL (PILL SHAPE, >= 48dp)", style = typography.label.copy(color = colors.mutedForeground))
                ArusPrimaryButton(
                    text = "Simpan Transaksi",
                    onClick = {},
                    leadingIcon = "check",
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    ArusSecondaryButton(
                        text = "Edit",
                        leadingIcon = "pen",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                    ArusTertiaryButton(
                        text = "Hapus",
                        destructive = true,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small),
                ) {
                    ArusIconButton(icon = "arrow-left", contentDescription = "Kembali", onClick = {})
                    ArusIconButton(icon = "magnifying-glass", contentDescription = "Cari", onClick = {})
                    ArusIconButton(icon = "gear", contentDescription = "Pengaturan", onClick = {})
                    ArusIconButton(icon = "share-nodes", contentDescription = "Bagikan", onClick = {})
                }
            }

            // 2. Input Fields
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("2. INPUT FORM & NOMINAL", style = typography.label.copy(color = colors.mutedForeground))
                ArusAmountInput(
                    value = amountDigits,
                    onValueChange = { amountDigits = it },
                    isExpense = true,
                )
                ArusSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                )
                ArusTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = "Tulis catatan...",
                    leadingIcon = "pen",
                )
            }

            // 3. Selection & Controls
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("3. SELEKSI & SAKLAR", style = typography.label.copy(color = colors.mutedForeground))
                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    ArusFilterChip(text = "Semua", selected = chipSelected, onClick = { chipSelected = !chipSelected })
                    ArusFilterChip(text = "Bulan ini", selected = !chipSelected, onClick = { chipSelected = !chipSelected })
                    ArusBadge(text = "TERVERIFIKASI")
                }
                ArusSegmentedControl(
                    options = listOf("Pengeluaran", "Pemasukan", "Transfer"),
                    selectedIndex = segmentedIndex,
                    onOptionSelected = { segmentedIndex = it },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BasicText("Aktifkan Haptic Feedback", style = typography.bodyLarge)
                    ArusSwitch(checked = switchState, onCheckedChange = { switchState = it })
                }
                ArusCheckRow(
                    text = "Konfirmasi transaksi rutin",
                    description = "Minta persetujuan sebelum memasukkan ke saldo",
                    checked = checkRowChecked,
                    onCheckedChange = { checkRowChecked = it },
                )
            }

            // 4. Kartu Saldo (Balance Card & Wallet)
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("4. KARTU SALDO & DOMPET", style = typography.label.copy(color = colors.mutedForeground))
                ArusBalanceCard(
                    netWorthAmount = 14250000,
                    assetsAmount = 15000000,
                    liabilitiesAmount = 750000,
                    onClick = {},
                )
                WalletCarousel(
                    wallets = listOf(
                        WalletCardUiModel(1, "BCA", "BANK", 5200000, "B", 0xFF1565C0.toInt()),
                        WalletCardUiModel(2, "GoPay", "EWALLET", 450000, "G", 0xFF00ACC1.toInt()),
                        WalletCardUiModel(3, "Dompet Tunai", "CASH", 120000, "T", 0xFF43A047.toInt()),
                        WalletCardUiModel(4, "Kartu Kredit Mandiri", "CREDIT", -750000, "M", 0xFFD32F2F.toInt(), creditLimit = 10000000, creditUsed = 750000),
                    ),
                    onWalletClick = {},
                )
            }

            // 5. Item Transaksi (12 Tipe)
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("5. ITEM TRANSAKSI (12 TIPE)", style = typography.label.copy(color = colors.mutedForeground))
                TransactionItem(
                    model = TransactionItemUiModel(
                        id = 1,
                        type = "EXPENSE",
                        amount = 45000,
                        title = "Nasi Padang Garuda",
                        categoryName = "Makan & Minum",
                        categoryIcon = "utensils",
                        walletName = "GoPay",
                        dateFormatted = "Hari ini, 12:30",
                        isVerified = true,
                    ),
                    onClick = {},
                )
                TransactionItem(
                    model = TransactionItemUiModel(
                        id = 2,
                        type = "INCOME",
                        amount = 8500000,
                        title = "Gaji Bulanan",
                        categoryName = "Gaji",
                        categoryIcon = "money-bill-wave",
                        walletName = "BCA",
                        dateFormatted = "25 Okt 2026",
                    ),
                    onClick = {},
                )
                TransactionItem(
                    model = TransactionItemUiModel(
                        id = 3,
                        type = "TRANSFER_OUT",
                        amount = 500000,
                        title = "Transfer BCA ke GoPay",
                        walletName = "BCA",
                        dateFormatted = "Kemarin, 09:15",
                    ),
                    onClick = {},
                )
            }

            // 6. Kategori & Anggaran
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("6. KATEGORI & ANGGARAN", style = typography.label.copy(color = colors.mutedForeground))
                CategoryItem(
                    model = CategoryItemUiModel(1, "Makan & Minum", "EXPENSE", "utensils", 0xFFFF9800.toInt()),
                    onClick = {},
                )
                CategoryItem(
                    model = CategoryItemUiModel(2, "Admin Bank", "SYSTEM", "building-columns", 0xFF607D8B.toInt(), isSystem = true),
                    onClick = {},
                )
                BudgetProgress(
                    spentAmount = 1800000,
                    limitAmount = 2000000,
                    categoryName = "Makan & Minum",
                    periodLabel = "1 Nov – 30 Nov",
                )
                IncomeExpenseSummary(
                    incomeAmount = 8500000,
                    expenseAmount = 3250000,
                    untrackedDifference = 15000,
                )
            }

            // 7. Utang-Piutang & Rekonsiliasi
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("7. UTANG-PIUTANG & REKONSILIASI", style = typography.label.copy(color = colors.mutedForeground))
                DebtItem(
                    model = DebtItemUiModel(
                        id = 1,
                        contactName = "Budi Santoso",
                        direction = "LENT",
                        principal = 150000,
                        remaining = 50000,
                        status = "PARTIAL",
                        dueDateFormatted = "10 Nov 2026",
                        note = "Patungan makan malam",
                    ),
                    onClick = {},
                )
                ReconciliationSummary(
                    appBalance = 5200000,
                    realBalance = 5185000,
                )
            }

            // 8. Navigation Bars
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("8. TOP BAR & BOTTOM NAVIGATION", style = typography.label.copy(color = colors.mutedForeground))
                ArusTopBar(title = "Riwayat Transaksi", onBackClick = {})
                Spacer(Modifier.height(8.dp))
                ArusBottomNavigation(
                    selectedTab = "Beranda",
                    onTabSelected = {},
                    onFabClick = {},
                )
            }

            // 9. States
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
                BasicText("9. KONDISI KHUSUS (EMPTY & ERROR)", style = typography.label.copy(color = colors.mutedForeground))
                ArusEmptyState(
                    title = "Belum Ada Transaksi",
                    description = "Mulai mencatat pengeluaran harianmu untuk mengontrol keuangan lebih baik.",
                    icon = "wallet",
                    actionText = "Tambah Transaksi",
                    onActionClick = {},
                )
            }
        }
    }
}

