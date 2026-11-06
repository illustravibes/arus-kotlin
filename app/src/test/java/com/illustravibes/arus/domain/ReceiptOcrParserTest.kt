package com.illustravibes.arus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ReceiptOcrParserTest {
    @Test
    fun extractsReceiptFieldsItemsAndConfidenceForReview() {
        val parsed = ReceiptOcrParser.parse(
            """WARUNG BUDI
                |22/10/2026
                |Nasi ayam 32.000
                |Es teh 8.000
                |TOTAL Rp 40.000
            """.trimMargin(),
        )

        assertEquals(40_000L, parsed.totalRupiah)
        assertEquals(90, parsed.totalConfidencePercent)
        assertEquals(LocalDate.of(2026, 10, 22), parsed.date)
        assertEquals(95, parsed.dateConfidencePercent)
        assertEquals("WARUNG BUDI", parsed.merchant)
        assertTrue(requireNotNull(parsed.merchantConfidencePercent) < 75)
        assertEquals(2, parsed.items.size)
        assertEquals(32_000L, parsed.items[0].amountRupiah)
        assertTrue(parsed.items.all { it.confidencePercent < 75 })
    }

    @Test
    fun missingFieldsStayNullAndFallbackAmountNeedsReview() {
        val parsed = ReceiptOcrParser.parse("TOKO KECIL\nBelanja harian\n17.000")

        assertEquals(17_000L, parsed.totalRupiah)
        assertEquals(45, parsed.totalConfidencePercent)
        assertEquals("TOKO KECIL", parsed.merchant)
        assertNotNull(parsed.rawText)
        assertEquals(null, parsed.date)
    }
}
