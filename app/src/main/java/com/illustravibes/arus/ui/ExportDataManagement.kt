package com.illustravibes.arus.ui

import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import com.illustravibes.arus.ui.designsystem.Text
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ExportDataManagement(database: ArusDatabase, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    var startDateText by remember { mutableStateOf(today.withDayOfMonth(1).toString()) }
    var endDateText by remember { mutableStateOf(today.toString()) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var exportedUri by remember { mutableStateOf<Uri?>(null) }
    var exportedFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    fun writeExport(uri: Uri?, format: ExportFormat) {
        if (uri == null) return
        val start = runCatching { LocalDate.parse(startDateText) }.getOrNull()
        val endInclusive = runCatching { LocalDate.parse(endDateText) }.getOrNull()
        if (start == null || endInclusive == null || start.isAfter(endInclusive)) {
            error = "Pilih rentang tanggal yang valid."
            return
        }
        error = null
        message = null
        isExporting = true
        scope.launch {
            val temporary = File(context.cacheDir, "arus-export-${System.nanoTime()}.${format.extension}")
            try {
                val endExclusive = endInclusive.plusDays(1)
                withContext(Dispatchers.IO) {
                    val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endMillis = endExclusive.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val rows = database.transactionDao().exportTransactionsBetween(startMillis, endMillis)
                    ExportFileWriter.write(format, rows, start, endInclusive, temporary)
                    val output = context.contentResolver.openOutputStream(uri, "w")
                        ?: throw IllegalStateException("File tujuan tidak dapat dibuka.")
                    output.use { destination -> temporary.inputStream().use { it.copyTo(destination) } }
                    rows.size
                }.let { rowCount ->
                    exportedUri = uri
                    exportedFormat = format
                    message = "$rowCount transaksi diekspor ke ${format.extension.uppercase()}."
                }
            } catch (failure: Exception) {
                error = failure.message ?: "Ekspor tidak dapat dibuat."
            } finally {
                temporary.delete()
                isExporting = false
            }
        }
    }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType)) {
        writeExport(it, ExportFormat.CSV)
    }
    val excelPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.EXCEL.mimeType)) {
        writeExport(it, ExportFormat.EXCEL)
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)) {
        writeExport(it, ExportFormat.PDF)
    }

    fun showDatePicker(value: String, onDate: (LocalDate) -> Unit) {
        val initial = runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())
        DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) },
            initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
    }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small), verticalAlignment = Alignment.CenterVertically) {
            OutlinedArusButton(onClick = onDismiss, enabled = !isExporting) { Text("‹ Kembali") }
            Text("Ekspor data", style = ArusTheme.typography.headline)
        }
        Text("Pilih tanggal dan format. File dibuat di perangkat dan dapat dibagikan lewat aplikasi lain.")
        ArusCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(ArusSpacing.Medium), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Rentang tanggal · akhir inklusif", style = ArusTheme.typography.title)
                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    OutlinedArusButton(onClick = { showDatePicker(startDateText) { startDateText = it.toString(); error = null } },
                        enabled = !isExporting, modifier = Modifier.weight(1f)) { Text("Mulai · $startDateText") }
                    OutlinedArusButton(onClick = { showDatePicker(endDateText) { endDateText = it.toString(); error = null } },
                        enabled = !isExporting, modifier = Modifier.weight(1f)) { Text("Sampai · $endDateText") }
                }
                Text("Format file", style = ArusTheme.typography.title)
                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small), verticalAlignment = Alignment.CenterVertically) {
                    ExportFormat.entries.forEach { format ->
                        ArusButton(onClick = { selectedFormat = format },
                            kind = if (selectedFormat == format) ArusButtonKind.Secondary else ArusButtonKind.Outline,
                            selected = selectedFormat == format, enabled = !isExporting,
                            modifier = Modifier.weight(1f)) { Text(format.label) }
                    }
                }
                ArusButton(onClick = {
                    val prefix = "arus-${startDateText}-${endDateText}"
                    when (selectedFormat) {
                        ExportFormat.CSV -> csvPicker.launch("$prefix.csv")
                        ExportFormat.EXCEL -> excelPicker.launch("$prefix.xlsx")
                        ExportFormat.PDF -> pdfPicker.launch("$prefix.pdf")
                    }
                }, enabled = !isExporting) { Text("Simpan file ekspor") }
            }
        }
        error?.let { Text(it, style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error)) }
        message?.let { Text(it, style = ArusTheme.typography.body.copy(color = ArusTheme.colors.mutedForeground)) }
        if (isExporting) Text("Menyiapkan file ekspor…")
        val uri = exportedUri
        val format = exportedFormat
        if (uri != null && format != null) {
            ArusButton(onClick = {
                context.startActivity(createExportShareIntent(context.contentResolver, uri, format))
            }) { Text("Bagikan ekspor terakhir") }
        }
    }
}

internal fun createExportShareIntent(contentResolver: ContentResolver, uri: Uri, format: ExportFormat): Intent {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(contentResolver, "Ekspor Arus", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, "Bagikan file ekspor")
}
