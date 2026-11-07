package com.illustravibes.arus.ui.designsystem

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class AmountStyle {
    STANDARD,  // "Rp 1.250.000"
    COMPACT,   // "1,2 jt", "450 rb"
    PLAIN,     // "1.250.000" (tanpa "Rp")
}

enum class AmountKind {
    EXPENSE,     // -Rp 65.000 (textPrimary)
    INCOME,      // +Rp 1.500.000 (incomeText)
    NEUTRAL,     // Rp 500.000 (textSecondary)
    TRANSFER,    // Rp 500.000 (textSecondary, netral tanpa tanda)
    DIFFERENCE,  // Merah jika minus, Hijau jika surplus
    ALERT,       // Merah (expense) untuk saldo minus / budget jebol
}

object AmountFormatter {
    private val localeId = Locale.forLanguageTag("id-ID")

    /**
     * Format rupiah standar: "Rp 1.250.000", negatif "-Rp 65.000", positif "+Rp 1.500.000"
     */
    fun format(
        amount: Long,
        style: AmountStyle = AmountStyle.STANDARD,
        includeSign: Boolean = false,
        prefixRp: Boolean = true,
    ): String {
        return when (style) {
            AmountStyle.COMPACT -> formatCompact(amount, includeSign, prefixRp)
            AmountStyle.PLAIN -> formatNumber(abs(amount))
            AmountStyle.STANDARD -> {
                val formatted = formatNumber(abs(amount))
                val prefix = if (prefixRp) "Rp " else ""
                val sign = when {
                    amount < 0 -> "-"
                    includeSign && amount > 0 -> "+"
                    else -> ""
                }
                "$sign$prefix$formatted"
            }
        }
    }

    /**
     * Format ringkas untuk kartu / widget: "1,2 jt", "450 rb", "1,5 M"
     */
    fun formatCompact(
        amount: Long,
        includeSign: Boolean = false,
        prefixRp: Boolean = false,
    ): String {
        val sign = when {
            amount < 0 -> "-"
            includeSign && amount > 0 -> "+"
            else -> ""
        }
        val absVal = abs(amount)
        val prefix = if (prefixRp) "Rp " else ""

        val text = when {
            absVal >= 1_000_000_000L -> {
                val m = absVal.toDouble() / 1_000_000_000.0
                String.format(localeId, "%.1f M", m).replace(",0", "")
            }
            absVal >= 1_000_000L -> {
                val jt = absVal.toDouble() / 1_000_000.0
                String.format(localeId, "%.1f jt", jt).replace(",0", "")
            }
            absVal >= 1_000L -> {
                val rb = absVal.toDouble() / 1_000.0
                if (absVal % 1_000L == 0L) {
                    "${absVal / 1_000L} rb"
                } else {
                    String.format(localeId, "%.1f rb", rb).replace(",0", "")
                }
            }
            else -> absVal.toString()
        }

        return "$sign$prefix$text"
    }

    private fun formatNumber(number: Long): String {
        return NumberFormat.getNumberInstance(localeId).format(number)
    }

    /**
     * Membaca string yang ramah untuk screen-reader (TalkBack accessibility)
     */
    fun toSpokenText(amount: Long, kind: AmountKind): String {
        val sign = when (kind) {
            AmountKind.EXPENSE -> "Minus "
            AmountKind.INCOME -> "Plus "
            else -> if (amount < 0) "Minus " else ""
        }
        return "$sign${formatNumber(abs(amount))} rupiah"
    }
}

/**
 * Top-level convenience formatter used across Arus UI screens.
 */
fun formatRupiah(amount: Long): String = AmountFormatter.format(amount)


