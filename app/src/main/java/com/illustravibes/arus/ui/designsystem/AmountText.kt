package com.illustravibes.arus.ui.designsystem

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs

/**
 * Satu-satunya pemformat nominal uang Arus.
 * Menghormati aturan nominal:
 * - Pengeluaran = textPrimary dengan tanda minus (-Rp 45.000)
 * - Pemasukan = incomeText dengan tanda plus (+Rp 1.500.000)
 * - Transfer / Pelunasan / Pinjaman = textSecondary (Rp 500.000)
 * - Merah (expense) = khusus selisih negatif / saldo minus / over budget
 */
@Composable
fun AmountText(
    amount: Long,
    modifier: Modifier = Modifier,
    kind: AmountKind = AmountKind.NEUTRAL,
    style: AmountStyle = AmountStyle.STANDARD,
    useMono: Boolean = true,
    textStyle: TextStyle? = null,
    color: Color? = null,
    textAlign: TextAlign? = null,
) {
    val colors = ArusTheme.colors
    val typography = ArusTheme.typography

    // Tentukan warna berdasarkan aturan token
    val resolvedColor = color ?: when (kind) {
        AmountKind.EXPENSE -> colors.foreground
        AmountKind.INCOME -> colors.incomeText
        AmountKind.NEUTRAL, AmountKind.TRANSFER -> colors.mutedForeground
        AmountKind.DIFFERENCE -> if (amount < 0) colors.expense else colors.incomeText
        AmountKind.ALERT -> colors.expense
    }

    // Tentukan gaya teks dan font family
    val baseStyle = textStyle ?: if (useMono) typography.amountMono else typography.bodyLarge
    val finalStyle = baseStyle.copy(
        color = resolvedColor,
        textAlign = textAlign ?: baseStyle.textAlign,
    )

    // Tentukan tanda dan format
    val signedAmount = when (kind) {
        AmountKind.EXPENSE -> -abs(amount)
        AmountKind.INCOME -> abs(amount)
        else -> amount
    }
    val includeSign = kind == AmountKind.INCOME || (kind == AmountKind.DIFFERENCE && amount > 0)
    val formattedText = AmountFormatter.format(signedAmount, style = style, includeSign = includeSign)
    val spokenText = AmountFormatter.toSpokenText(signedAmount, kind)

    BasicText(
        text = formattedText,
        modifier = modifier.semantics {
            contentDescription = spokenText
        },
        style = finalStyle,
    )
}

@Composable
fun AmountText(
    amount: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    kind: AmountKind = AmountKind.NEUTRAL,
    color: Color? = null,
    textAlign: TextAlign? = null,
) = AmountText(
    amount = amount,
    modifier = modifier,
    kind = kind,
    style = AmountStyle.STANDARD,
    useMono = true,
    textStyle = style,
    color = color,
    textAlign = textAlign,
)

