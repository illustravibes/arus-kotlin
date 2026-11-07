package com.illustravibes.arus.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// =========================================================================
// UI DATA MODELS (Sederhana, bebas dari ketergantungan Room Database)
// =========================================================================

data class TransactionItemUiModel(
    val id: Long,
    val type: String, // INCOME, EXPENSE, TRANSFER_OUT, TRANSFER_IN, dll.
    val amount: Long,
    val title: String,
    val categoryName: String? = null,
    val categoryIcon: String? = null,
    val walletName: String? = null,
    val dateFormatted: String,
    val isVerified: Boolean = false,
    val isScheduled: Boolean = false,
    val subtitle: String? = null,
)

data class WalletCardUiModel(
    val id: Long,
    val name: String,
    val type: String, // BANK, EWALLET, CASH, CREDIT
    val balance: Long,
    val initialChar: String = name.take(1).uppercase(),
    val colorArgb: Int = 0xFF18181B.toInt(),
    val creditLimit: Long? = null,
    val creditUsed: Long? = null,
    val billingDay: Int? = null,
    val isArchived: Boolean = false,
)

data class CategoryItemUiModel(
    val id: Long,
    val name: String,
    val kind: String, // INCOME, EXPENSE, SYSTEM
    val iconKey: String,
    val colorArgb: Int,
    val isSystem: Boolean = false,
    val isArchived: Boolean = false,
)

data class DebtItemUiModel(
    val id: Long,
    val contactName: String,
    val direction: String, // LENT (piutang) / BORROWED (utang)
    val principal: Long,
    val remaining: Long,
    val status: String, // OPEN, PARTIAL, PAID, WRITTEN_OFF
    val dueDateFormatted: String? = null,
    val note: String? = null,
)

data class GroupOrderShareUiModel(
    val id: Long,
    val contactName: String,
    val itemsAmount: Long,
    val feeAmount: Long,
    val totalAmount: Long,
    val isPaid: Boolean = false,
)

// =========================================================================
// 1. TRANSACTION ITEM (12 TIPE TRANSAKSI)
// =========================================================================

/**
 * Item Transaksi Arus: Mendukung 12 tipe transaksi ERD dengan format nominal,
 * ikon kategori, tanda status terverifikasi, dan label terjadwal.
 */
@Composable
fun TransactionItem(
    model: TransactionItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBordered: Boolean = false,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    // Tentukan amount kind dan icon bawaan per tipe ERD
    val (amountKind, defaultIcon) = when (model.type) {
        "INCOME" -> AmountKind.INCOME to (model.categoryIcon ?: "money-bill-wave")
        "EXPENSE" -> AmountKind.EXPENSE to (model.categoryIcon ?: "utensils")
        "TRANSFER_OUT", "TRANSFER_IN" -> AmountKind.TRANSFER to "arrow-right-arrow-left"
        "REPAYMENT_IN" -> AmountKind.NEUTRAL to "hand-holding-dollar"
        "REFUND_OUT" -> AmountKind.NEUTRAL to "arrow-rotate-left"
        "LOAN_OUT" -> AmountKind.NEUTRAL to "hand-holding-dollar"
        "LOAN_IN" -> AmountKind.NEUTRAL to "hand-holding-dollar"
        "DEBT_PAY_OUT" -> AmountKind.NEUTRAL to "hand-holding-dollar"
        "WRITE_OFF" -> AmountKind.NEUTRAL to "heart"
        "NETTING" -> AmountKind.NEUTRAL to "arrows-rotate"
        "ADJUSTMENT" -> AmountKind.DIFFERENCE to "sliders"
        else -> AmountKind.NEUTRAL to "circle-question"
    }

    val shape = if (isBordered) ArusShapes.Card else RoundedCornerShape(12.dp)
    val rowModifier = if (isBordered) {
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(shape)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), shape)
            .springPress(interactionSource = interactionSource, enabled = true)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .springPress(interactionSource = interactionSource, enabled = true)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Lingkaran Ikon Kategori / Transaksi
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(colors.surfaceSecondary)
                .border(BorderStroke(1.dp, colors.border), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ArusIcon(key = defaultIcon, size = 17.dp, tint = colors.foreground)
        }

        Spacer(Modifier.width(12.dp))

        // Info Transaksi
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = model.title,
                    style = typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = colors.foreground,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (model.isVerified) {
                    Spacer(Modifier.width(6.dp))
                    ArusIcon(key = "circle-check", size = 14.dp, tint = colors.primary, contentDescription = "Terverifikasi")
                }
            }

            val meta = buildString {
                if (model.categoryName != null && model.type == "EXPENSE") append("${model.categoryName} · ")
                if (model.walletName != null) append("${model.walletName} · ")
                append(model.dateFormatted)
                if (model.isScheduled) append(" · Terjadwal")
            }
            BasicText(
                text = meta,
                style = typography.bodySmall.copy(color = colors.mutedForeground),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        // Nominal Rupiah (Menggunakan AmountText dengan font Mono)
        AmountText(
            amount = model.amount,
            kind = amountKind,
            style = AmountStyle.STANDARD,
            useMono = true,
            textStyle = typography.amountMono.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

// =========================================================================
// 2. WALLET CARD & CAROUSEL (BANK, EWALLET, CASH, CREDIT)
// =========================================================================

/**
 * Kartu Dompet Arus: Menampilkan inisial avatar berwarna, nama, saldo,
 * dan meteran limit khusus tipe CREDIT.
 */
@Composable
fun WalletCard(
    model: WalletCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReconciliationClick: (() -> Unit)? = null,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    ArusCard(
        modifier = modifier
            .width(156.dp)
            .defaultMinSize(minHeight = 104.dp),
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
    ) {
        // Baris Atas: Avatar Inisial & Tipe
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar Inisial
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(model.colorArgb))
                    .border(BorderStroke(1.dp, colors.border), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = model.initialChar,
                    style = typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }
            // Tipe Dompet Badge
            val typeLabel = when (model.type) {
                "BANK" -> "Bank"
                "EWALLET" -> "E-Wallet"
                "CASH" -> "Tunai"
                "CREDIT" -> "Kredit"
                else -> model.type
            }
            BasicText(
                text = typeLabel,
                style = typography.label.copy(color = colors.mutedForeground),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Nama Dompet
        BasicText(
            text = model.name,
            style = typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.foreground,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(4.dp))

        // Saldo Dompet
        AmountText(
            amount = model.balance,
            kind = AmountKind.NEUTRAL,
            useMono = false,
            textStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = colors.foreground),
        )

        if (onReconciliationClick != null) {
            Spacer(Modifier.height(6.dp))
            ArusTertiaryButton(
                text = "Cocokkan saldo",
                onClick = onReconciliationClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Khusus Kartu Kredit: Meteran Limit
        if (model.type == "CREDIT" && model.creditLimit != null && model.creditLimit > 0) {
            val used = model.creditUsed ?: 0L
            val limit = model.creditLimit
            val fraction = (used.toFloat() / limit).coerceIn(0f, 1f)

            Spacer(Modifier.height(6.dp))
            // Bar Limit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(ArusShapes.Pill)
                    .background(colors.surfaceSecondary),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(ArusShapes.Pill)
                        .background(if (fraction > 0.8f) colors.expense else colors.foreground),
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BasicText(
                    text = "Terpakai ${AmountFormatter.formatCompact(used, prefixRp = true)}",
                    style = typography.bodySmall.copy(color = colors.mutedForeground, fontSize = 9.sp),
                )
            }
        }
    }
}

/**
 * Carousel Dompet Horizontal Arus (Wireframe S-10): Kartu berikutnya sedikit terintip.
 */
@Composable
fun WalletCarousel(
    wallets: List<WalletCardUiModel>,
    onWalletClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(wallets, key = { it.id }) { wallet ->
            WalletCard(
                model = wallet,
                onClick = { onWalletClick(wallet.id) },
            )
        }
    }
}

// =========================================================================
// 3. CATEGORY ITEM
// =========================================================================

/**
 * Item Kategori Arus: Lingkaran warna pastel dengan ikon Font Awesome di tengah,
 * nama kategori, dan tanda gembok sistem jika kategori SYSTEM.
 */
@Composable
fun CategoryItem(
    model: CategoryItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(ArusShapes.Card)
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), ArusShapes.Card)
            .springPress(interactionSource = interactionSource, enabled = true)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Lingkaran Ikon Berwarna
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(model.colorArgb).copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, Color(model.colorArgb)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            ArusIcon(key = model.iconKey, size = 16.dp, tint = Color(model.colorArgb))
        }

        Spacer(Modifier.width(12.dp))

        // Nama Kategori & Status
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = model.name,
                    style = typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = colors.foreground,
                    ),
                )
                if (model.isSystem) {
                    Spacer(Modifier.width(6.dp))
                    ArusIcon(key = "lock", size = 12.dp, tint = colors.mutedForeground, contentDescription = "Kategori Sistem")
                }
            }
            if (model.isArchived) {
                BasicText(
                    text = "Diarsipkan",
                    style = typography.bodySmall.copy(color = colors.mutedForeground),
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}

// =========================================================================
// 4. BUDGET PROGRESS & SUMMARY
// =========================================================================

/**
 * Bar Progres Anggaran Arus: Normal (zinc/primary), 80%+ (warning amber),
 * terlampaui (expense merah).
 */
@Composable
fun BudgetProgress(
    spentAmount: Long,
    limitAmount: Long,
    categoryName: String,
    periodLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val fraction = if (limitAmount > 0) (spentAmount.toFloat() / limitAmount).coerceIn(0f, 1f) else 0f
    val percent = if (limitAmount > 0) ((spentAmount.toDouble() / limitAmount) * 100).toInt() else 0
    val isExceeded = spentAmount > limitAmount
    val isWarning = percent >= 80 && !isExceeded

    val barColor = when {
        isExceeded -> colors.expense
        isWarning -> colors.warning
        else -> colors.foreground
    }

    ArusCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = categoryName,
                style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.foreground),
            )
            BasicText(
                text = periodLabel,
                style = typography.bodySmall.copy(color = colors.mutedForeground),
            )
        }

        Spacer(Modifier.height(10.dp))

        // Progress Bar (Sudut Pill, 8dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(ArusShapes.Pill)
                .background(colors.surfaceSecondary),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(ArusShapes.Pill)
                    .background(barColor),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Rincian Terpakai vs Limit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val spentText = AmountFormatter.format(spentAmount)
            val limitText = AmountFormatter.format(limitAmount)
            BasicText(
                text = "$spentText dari $limitText",
                style = typography.bodySmall.copy(color = colors.mutedForeground),
            )

            val statusText = when {
                isExceeded -> "Lewat ${AmountFormatter.formatCompact(spentAmount - limitAmount, prefixRp = true)}"
                else -> "$percent%"
            }
            BasicText(
                text = statusText,
                style = typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = barColor,
                ),
            )
        }
    }
}

/**
 * Ringkasan Pemasukan & Pengeluaran Arus (IncomeExpenseSummary).
 */
@Composable
fun IncomeExpenseSummary(
    incomeAmount: Long,
    expenseAmount: Long,
    untrackedDifference: Long? = null,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    ArusCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText("PEMASUKAN", style = typography.label.copy(color = colors.mutedForeground))
                AmountText(amount = incomeAmount, kind = AmountKind.INCOME, textStyle = typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText("PENGELUARAN", style = typography.label.copy(color = colors.mutedForeground))
                AmountText(amount = expenseAmount, kind = AmountKind.EXPENSE, textStyle = typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            }
        }
        if (untrackedDifference != null && untrackedDifference != 0L) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ArusShapes.Pill)
                    .background(colors.surfaceSecondary)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BasicText("Selisih tak terlacak", style = typography.bodySmall.copy(color = colors.mutedForeground))
                    AmountText(amount = untrackedDifference, kind = AmountKind.DIFFERENCE, style = AmountStyle.STANDARD, textStyle = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

// =========================================================================
// 5. FINANCIAL PERIOD SELECTOR
// =========================================================================

/**
 * Pemilih Periode Keuangan Arus (Anggaran / Laporan / Detail Kategori).
 */
@Composable
fun FinancialPeriodSelector(
    periodText: String,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    canGoNext: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    ArusCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArusIconButton(
                icon = "arrow-left",
                contentDescription = "Periode sebelumnya",
                onClick = onPreviousClick,
                size = 16.dp,
            )
            BasicText(
                text = periodText,
                style = typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.foreground,
                ),
            )
            ArusIconButton(
                icon = "arrow-right",
                contentDescription = "Periode berikutnya",
                onClick = onNextClick,
                enabled = canGoNext,
                size = 16.dp,
            )
        }
    }
}

// =========================================================================
// 6. DEBT ITEM (UTANG-PIUTANG DUA ARAH)
// =========================================================================

/**
 * Item Utang-Piutang Arus: Mendukung arah Piutang (LENT) dan Utang (BORROWED),
 * bar terbayar, dan label tanggal jatuh tempo.
 */
@Composable
fun DebtItem(
    model: DebtItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val isLent = model.direction == "LENT" // Orang berutang ke aku
    val paid = (model.principal - model.remaining).coerceAtLeast(0L)
    val fraction = if (model.principal > 0) (paid.toFloat() / model.principal).coerceIn(0f, 1f) else 1f

    ArusCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = model.contactName,
                    style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.foreground),
                )
                val directionLabel = if (isLent) "Piutang (dia berutang)" else "Utang (kamu berutang)"
                BasicText(
                    text = directionLabel,
                    style = typography.bodySmall.copy(color = colors.mutedForeground),
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AmountText(
                    amount = model.remaining,
                    kind = AmountKind.NEUTRAL,
                    textStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
                BasicText(
                    text = "Sisa dari ${AmountFormatter.formatCompact(model.principal, prefixRp = true)}",
                    style = typography.bodySmall.copy(color = colors.mutedForeground, fontSize = 11.sp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Progress Pelunasan
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(ArusShapes.Pill)
                .background(colors.surfaceSecondary),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(ArusShapes.Pill)
                    .background(if (isLent) colors.income else colors.primary),
            )
        }

        if (model.dueDateFormatted != null || model.note != null) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (model.dueDateFormatted != null) {
                    BasicText(
                        text = "Jatuh tempo: ${model.dueDateFormatted}",
                        style = typography.bodySmall.copy(color = colors.warningText, fontSize = 11.sp),
                    )
                }
                if (model.note != null) {
                    BasicText(
                        text = model.note,
                        style = typography.bodySmall.copy(color = colors.mutedForeground, fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// =========================================================================
// 7. SPLIT & GROUP ORDER ROWS
// =========================================================================

/**
 * Baris Split Transaksi (S-32): Kategori + nominal + tombol hapus.
 */
@Composable
fun SplitRow(
    categoryName: String,
    amountText: String,
    onCategoryClick: () -> Unit,
    onAmountChange: (String) -> Unit,
    onDeleteClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ArusShapes.Card)
            .background(colors.surfaceSecondary)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Tombol Pilih Kategori
        ArusChip(
            text = categoryName.ifBlank { "Pilih Kategori" },
            selected = false,
            onClick = onCategoryClick,
            modifier = Modifier.weight(1f),
        )
        // Input Nominal
        ArusTextField(
            value = amountText,
            onValueChange = onAmountChange,
            placeholder = "Rp 0",
            singleLine = true,
            modifier = Modifier.width(130.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        )
        // Tombol Hapus
        if (onDeleteClick != null) {
            ArusIconButton(
                icon = "trash",
                contentDescription = "Hapus bagian split",
                onClick = onDeleteClick,
                size = 16.dp,
                tint = colors.expense,
            )
        }
    }
}

/**
 * Baris Rincian Tagihan Group Order (S-85, S-86): Nama, rincian item, biaya bersama, total.
 */
@Composable
fun GroupOrderShareRow(
    model: GroupOrderShareUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    ArusCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText(
                    text = model.contactName,
                    style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = colors.foreground),
                )
                BasicText(
                    text = "Item ${AmountFormatter.formatCompact(model.itemsAmount, prefixRp = true)} · Biaya ${AmountFormatter.formatCompact(model.feeAmount, prefixRp = true)}",
                    style = typography.bodySmall.copy(color = colors.mutedForeground),
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AmountText(
                    amount = model.totalAmount,
                    kind = AmountKind.NEUTRAL,
                    useMono = true,
                    textStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
                val statusText = if (model.isPaid) "Lunas" else "Belum bayar"
                val statusColor = if (model.isPaid) colors.incomeText else colors.warningText
                BasicText(
                    text = statusText,
                    style = typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = statusColor),
                )
            }
        }
    }
}

/**
 * Ringkasan Rekonsiliasi (S-101, S-103): Saldo Arus, saldo asli, selisih.
 */
@Composable
fun ReconciliationSummary(
    appBalance: Long,
    realBalance: Long,
    modifier: Modifier = Modifier,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography
    val difference = realBalance - appBalance

    ArusCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText("Saldo di Arus · ${AmountFormatter.format(appBalance)}", style = typography.bodySmall.copy(color = colors.mutedForeground))
                AmountText(amount = appBalance, kind = AmountKind.NEUTRAL, textStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BasicText("Saldo Asli Aktual · ${AmountFormatter.format(realBalance)}", style = typography.bodySmall.copy(color = colors.mutedForeground))
                AmountText(amount = realBalance, kind = AmountKind.NEUTRAL, textStyle = typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ArusShapes.Pill)
                .background(if (difference == 0L) colors.surfaceSecondary else colors.errorSurface)
                .border(BorderStroke(1.dp, if (difference == 0L) colors.border else colors.expense), ArusShapes.Pill)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = if (difference == 0L) "Saldo Cocok" else "Selisih Saldo",
                    style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = if (difference == 0L) colors.incomeText else colors.expense),
                )
                AmountText(
                    amount = difference,
                    kind = AmountKind.DIFFERENCE,
                    textStyle = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
