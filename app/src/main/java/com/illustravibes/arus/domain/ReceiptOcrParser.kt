package com.illustravibes.arus.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

internal data class ReceiptOcrReview(
    val rawText: String,
    val totalRupiah: Long?,
    val totalConfidencePercent: Int?,
    val date: LocalDate?,
    val dateConfidencePercent: Int?,
    val merchant: String?,
    val merchantConfidencePercent: Int?,
    val items: List<ReceiptOcrItem>,
)

internal data class ReceiptOcrItem(
    val name: String,
    val amountRupiah: Long,
    val confidencePercent: Int,
)

internal object ReceiptOcrParser {
    private val amountPattern = Regex("(?i)(?:rp\\.?\\s*)?(\\d[\\d.,\\s]{0,17})")
    private val numericDatePattern = Regex("\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})\\b")
    private val isoDatePattern = Regex("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b")
    private val totalKeywords = listOf("grand total", "total pembayaran", "total bayar", "total", "jumlah", "tagihan", "harus dibayar")
    private val ignoredAmountKeywords = listOf("subtotal", "sub total", "pajak", "tax", "kembali", "change", "tunai", "cash", "diskon", "discount")
    private val monthDateFormatters = listOf(
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu").toFormatter(Locale.forLanguageTag("id-ID")).withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMMM uuuu").toFormatter(Locale.forLanguageTag("id-ID")).withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.SMART),
    )

    fun parse(rawText: String): ReceiptOcrReview {
        val lines = rawText.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        val datedLine = lines.firstNotNullOfOrNull { line -> parseDate(line)?.let { line to it } }
        val explicitTotal = lines.asReversed().firstNotNullOfOrNull { line ->
            val normalized = line.lowercase(Locale.ROOT)
            if ("subtotal" in normalized || "sub total" in normalized) null
            else if (totalKeywords.any(normalized::contains)) amount(line)?.let { line to it }
            else null
        }
        val fallbackTotal = if (explicitTotal == null) {
            lines.asSequence().filter { parseDate(it) == null && ignoredAmountKeywords.none(it.lowercase(Locale.ROOT)::contains) }
                .mapNotNull { line -> amount(line)?.let { line to it } }
                .maxByOrNull { it.second }
        } else null
        val total = explicitTotal ?: fallbackTotal
        val merchantLine = lines.firstOrNull { line ->
            val normalized = line.lowercase(Locale.ROOT)
            line.any(Char::isLetter) && parseDate(line) == null && amount(line) == null &&
                totalKeywords.none(normalized::contains) && ignoredAmountKeywords.none(normalized::contains) &&
                normalized !in setOf("struk", "nota", "receipt", "invoice")
        }
        val items = lines.mapNotNull { line ->
            val normalized = line.lowercase(Locale.ROOT)
            if (totalKeywords.any(normalized::contains) || ignoredAmountKeywords.any(normalized::contains) || parseDate(line) != null) return@mapNotNull null
            val value = amount(line) ?: return@mapNotNull null
            val name = amountPattern.findAll(line).lastOrNull()?.let { line.substring(0, it.range.first).trim().trimEnd('-', ':', '.') }.orEmpty()
            if (name.count(Char::isLetter) < 2) null else ReceiptOcrItem(name, value, 72)
        }
        return ReceiptOcrReview(
            rawText = rawText,
            totalRupiah = total?.second,
            totalConfidencePercent = total?.let { if (explicitTotal != null) 90 else 45 },
            date = datedLine?.second,
            dateConfidencePercent = datedLine?.let { if (numericDatePattern.containsMatchIn(it.first) || isoDatePattern.containsMatchIn(it.first)) 95 else 84 },
            merchant = merchantLine,
            merchantConfidencePercent = merchantLine?.let { 64 },
            items = items,
        )
    }

    fun normalizeMerchant(merchant: String): String = merchant.trim().replace(Regex("\\s+"), " ").uppercase(Locale.ROOT)

    private fun amount(line: String): Long? {
        val match = amountPattern.findAll(line).lastOrNull() ?: return null
        val digits = match.groupValues[1].filter(Char::isDigit)
        if (digits.length < 2) return null
        return digits.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun parseDate(line: String): LocalDate? {
        numericDatePattern.find(line)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let null
            val month = match.groupValues[2].toIntOrNull() ?: return@let null
            val yearValue = match.groupValues[3].toIntOrNull() ?: return@let null
            val year = if (yearValue < 100) 2000 + yearValue else yearValue
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }
        isoDatePattern.find(line)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return@let null
            val month = match.groupValues[2].toIntOrNull() ?: return@let null
            val day = match.groupValues[3].toIntOrNull() ?: return@let null
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }
        return monthDateFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(line, formatter) }.getOrNull()
        }
    }
}
