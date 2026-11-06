package com.illustravibes.arus.ui

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.illustravibes.arus.data.ExportTransactionRow
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportFileWriterTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun writesCsvExcelAndPdfWithTransactionData() {
        val rows = listOf(ExportTransactionRow().apply {
            id = 3
            occurredAtEpochMillis = 1_000
            type = "EXPENSE"
            amountRupiah = 12_345
            walletName = "Tunai"
            categoryName = "Makan"
            merchant = "Warung, Bu Tini"
            note = "Nasi \"spesial\""
            verified = true
            hasAttachment = true
            splitSummary = "Makan: 12345"
        })
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 31)
        val csv = File(temporaryFolder.root, "arus.csv")
        val excel = File(temporaryFolder.root, "arus.xlsx")
        val pdf = File(temporaryFolder.root, "arus.pdf")

        ExportFileWriter.write(ExportFormat.CSV, rows, start, end, csv)
        ExportFileWriter.write(ExportFormat.EXCEL, rows, start, end, excel)
        ExportFileWriter.write(ExportFormat.PDF, rows, start, end, pdf)

        val csvText = csv.readText(Charsets.UTF_8)
        assertTrue(csvText.startsWith("\uFEFF"))
        assertTrue(csvText.contains("\"Warung, Bu Tini\""))
        assertTrue(csvText.contains("\"Nasi \"\"spesial\"\"\""))
        ZipFile(excel).use { archive ->
            val sheet = archive.getInputStream(archive.getEntry("xl/worksheets/sheet1.xml"))
                .bufferedReader().use { it.readText() }
            assertTrue(sheet.contains("Warung, Bu Tini"))
            assertTrue(sheet.contains("Nasi &quot;spesial&quot;"))
            assertTrue(sheet.contains("<v>12345</v>"))
        }
        assertTrue(pdf.length() > 100)
        assertEquals("%PDF-", pdf.inputStream().use { input -> ByteArray(5).also { input.read(it) }.decodeToString() })
    }

    @Test
    fun sharingUsesSystemChooserAndReadOnlyGrant() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        val uri = Uri.parse("content://com.example.documents/export.csv")

        val chooser = createExportShareIntent(resolver, uri, ExportFormat.CSV)

        assertEquals(android.content.Intent.ACTION_CHOOSER, chooser.action)
        val send = chooser.getParcelableExtra(android.content.Intent.EXTRA_INTENT) as? android.content.Intent
        assertEquals(android.content.Intent.ACTION_SEND, send?.action)
        assertEquals("text/csv", send?.type)
        assertTrue((send?.flags ?: 0) and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
