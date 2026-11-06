package com.illustravibes.arus.ui

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.illustravibes.arus.data.ExportTransactionRow
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal enum class ExportFormat(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV", "csv", "text/csv"),
    EXCEL("Excel (.xlsx)", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PDF("PDF", "pdf", "application/pdf"),
}

internal object ExportFileWriter {
    private val headers = listOf(
        "Tanggal", "ID", "Jenis", "Dompet", "Kategori", "Merchant", "Catatan",
        "Nominal (Rp)", "Terverifikasi", "Lampiran", "Rincian split",
    )

    fun write(format: ExportFormat, rows: List<ExportTransactionRow>, start: LocalDate, end: LocalDate, output: File) {
        when (format) {
            ExportFormat.CSV -> writeCsv(rows, start, end, output)
            ExportFormat.EXCEL -> writeXlsx(rows, start, end, output)
            ExportFormat.PDF -> writePdf(rows, start, end, output)
        }
    }

    private fun writeCsv(rows: List<ExportTransactionRow>, start: LocalDate, end: LocalDate, output: File) {
        output.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write('\uFEFF'.code)
            writer.appendLine("Arus · Ekspor keuangan · $start sampai $end")
            writer.appendLine(headers.joinToString(",", transform = ::csvField))
            rows.forEach { row -> writer.appendLine(exportValues(row).joinToString(",", transform = ::csvField)) }
        }
    }

    private fun writeXlsx(rows: List<ExportTransactionRow>, start: LocalDate, end: LocalDate, output: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zip ->
            putText(zip, "[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
            """.trimIndent())
            putText(zip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
            """.trimIndent())
            putText(zip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Transaksi" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
            """.trimIndent())
            putText(zip, "xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
            """.trimIndent())
            val sheet = buildString {
                append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
                appendXlsxRow(1, listOf("Arus · Ekspor keuangan · $start sampai $end"), numericColumns = emptySet())
                appendXlsxRow(2, headers, numericColumns = emptySet())
                rows.forEachIndexed { index, row ->
                    appendXlsxRow(index + 3, exportValues(row), numericColumns = setOf(2, 8))
                }
                append("</sheetData></worksheet>")
            }
            putText(zip, "xl/worksheets/sheet1.xml", sheet)
        }
    }

    private fun StringBuilder.appendXlsxRow(rowNumber: Int, values: List<String>, numericColumns: Set<Int>) {
        append("<row r=\"").append(rowNumber).append("\">")
        values.forEachIndexed { index, value ->
            val reference = cellReference(index + 1, rowNumber)
            if (index + 1 in numericColumns && value.isNotBlank()) {
                append("<c r=\"").append(reference).append("\"><v>").append(xmlEscape(value)).append("</v></c>")
            } else {
                append("<c r=\"").append(reference).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(xmlEscape(value)).append("</t></is></c>")
            }
        }
        append("</row>")
    }

    private fun writePdf(rows: List<ExportTransactionRow>, start: LocalDate, end: LocalDate, output: File) {
        val document = PdfDocument()
        val width = 595
        val height = 842
        val margin = 36f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 18f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.DKGRAY; textSize = 10f }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var y = 44f

        fun beginPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            y = 44f
        }

        fun drawWrapped(text: String, paint: Paint) {
            var remaining = text.ifBlank { "Tidak ada" }
            while (remaining.isNotEmpty()) {
                val chars = paint.breakText(remaining, true, width - margin * 2, null).coerceAtLeast(1)
                val line = remaining.take(chars)
                if (y + paint.textSize + 5 > height - margin) beginPage()
                page.canvas.drawText(line, margin, y, paint)
                y += paint.textSize + 5
                remaining = remaining.drop(chars).trimStart()
            }
        }

        try {
            drawWrapped("Arus · Ekspor keuangan", titlePaint)
            drawWrapped("Rentang $start sampai $end · ${rows.size} transaksi", bodyPaint)
            y += 12
            if (rows.isEmpty()) drawWrapped("Tidak ada transaksi pada rentang ini.", bodyPaint)
            rows.forEach { row ->
                if (y + 58 > height - margin) beginPage()
                drawWrapped("${dateOf(row)} · ${localizedType(row.type)} · ${formatRupiah(row.amountRupiah)}", titlePaint.apply { textSize = 12f })
                titlePaint.textSize = 18f
                drawWrapped("Dompet: ${row.walletName.orEmpty()} · Kategori: ${row.categoryName.orEmpty()}", bodyPaint)
                row.merchant?.takeIf(String::isNotBlank)?.let { drawWrapped("Merchant: $it", bodyPaint) }
                row.note?.takeIf(String::isNotBlank)?.let { drawWrapped("Catatan: $it", bodyPaint) }
                row.splitSummary?.takeIf(String::isNotBlank)?.let { drawWrapped("Split: $it", bodyPaint) }
                drawWrapped("Terverifikasi: ${if (row.verified) "Ya" else "Tidak"} · Lampiran: ${if (row.hasAttachment) "Ya" else "Tidak"}", bodyPaint)
                y += 10
            }
            document.finishPage(page)
            FileOutputStream(output).use { document.writeTo(it); it.fd.sync() }
        } finally {
            document.close()
        }
    }

    private fun exportValues(row: ExportTransactionRow): List<String> = listOf(
        dateOf(row), row.id.toString(), localizedType(row.type), row.walletName.orEmpty(),
        row.categoryName.orEmpty(), row.merchant.orEmpty(), row.note.orEmpty(), row.amountRupiah.toString(),
        if (row.verified) "Ya" else "Tidak", if (row.hasAttachment) "Ya" else "Tidak", row.splitSummary.orEmpty(),
    )

    private fun dateOf(row: ExportTransactionRow): String =
        Instant.ofEpochMilli(row.occurredAtEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

    private fun localizedType(type: String?): String = when (type) {
        "INCOME" -> "Pemasukan"
        "EXPENSE" -> "Pengeluaran"
        "TRANSFER_IN", "TRANSFER_OUT" -> "Transfer"
        "REPAYMENT_IN" -> "Pelunasan piutang"
        "DEBT_PAY_OUT" -> "Pembayaran utang"
        "LOAN_IN", "LOAN_OUT" -> "Pinjaman"
        "WRITE_OFF" -> "Penghapusan utang"
        "NETTING" -> "Netting"
        "ADJUSTMENT" -> "Penyesuaian saldo"
        else -> type.orEmpty()
    }

    private fun csvField(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.any { it == ',' || it == '\n' || it == '\r' || it == '"' }) "\"$escaped\"" else escaped
    }

    private fun xmlEscape(value: String): String = value
        .filter { it == '\t' || it == '\n' || it == '\r' || it >= ' ' }
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun cellReference(column: Int, row: Int): String {
        var current = column
        val letters = StringBuilder()
        while (current > 0) {
            val remainder = (current - 1) % 26
            letters.append(('A'.code + remainder).toChar())
            current = (current - 1) / 26
        }
        return letters.reverse().append(row).toString()
    }

    private fun putText(zip: ZipOutputStream, name: String, value: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(value.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
}
