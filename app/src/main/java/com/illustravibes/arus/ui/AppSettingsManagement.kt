package com.illustravibes.arus.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusButtonKind
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import com.illustravibes.arus.ui.designsystem.Text
import java.time.LocalDate

@Composable
internal fun AppSettingsManagement(
    themeMode: String,
    periodStartDay: Int,
    dateFormat: String,
    numberFormat: String,
    onThemeModeChange: (String) -> Unit,
    onPeriodStartDayChange: (Int) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onNumberFormatChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var choosePeriodStartDay by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            OutlinedArusButton(onClick = onDismiss) { Text("‹ Kembali") }
            Text("Pengaturan", style = ArusTheme.typography.headline)
        }
        ArusCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(ArusSpacing.Medium), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Tampilan", style = ArusTheme.typography.title)
                Text("Tema", style = ArusTheme.typography.label)
                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    listOf("SYSTEM" to "Sistem", "LIGHT" to "Terang", "DARK" to "Gelap").forEach { (mode, label) ->
                        ArusButton(onClick = { onThemeModeChange(mode) },
                            kind = if (themeMode == mode) ArusButtonKind.Secondary else ArusButtonKind.Outline,
                            selected = themeMode == mode) { Text(label) }
                    }
                }
                Text("Bahasa · Indonesia", style = ArusTheme.typography.body)
            }
        }
        ArusCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(ArusSpacing.Medium), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Periode dan format", style = ArusTheme.typography.title)
                Text("Awal periode bulanan · tanggal $periodStartDay")
                OutlinedArusButton(onClick = { choosePeriodStartDay = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ubah tanggal awal periode")
                }
                Text("Format tanggal", style = ArusTheme.typography.label)
                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    listOf("LOCAL" to "Lokal", "ISO" to "ISO").forEach { (format, label) ->
                        ArusButton(onClick = { onDateFormatChange(format) },
                            kind = if (dateFormat == format) ArusButtonKind.Secondary else ArusButtonKind.Outline,
                            selected = dateFormat == format, modifier = Modifier.weight(1f)) { Text(label) }
                    }
                }
                Text("Contoh · ${ArusFormatting.formatDate(LocalDate.of(2026, 10, 22))}")
                Text("Format angka", style = ArusTheme.typography.label)
                Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                    listOf("ID" to "1.234", "EN" to "1,234").forEach { (format, label) ->
                        ArusButton(onClick = { onNumberFormatChange(format) },
                            kind = if (numberFormat == format) ArusButtonKind.Secondary else ArusButtonKind.Outline,
                            selected = numberFormat == format, modifier = Modifier.weight(1f)) { Text(label) }
                    }
                }
                Text("Contoh · ${ArusFormatting.formatRupiah(1_234)}")
            }
        }
    }
    if (choosePeriodStartDay) ArusDialog(
        onDismissRequest = { choosePeriodStartDay = false },
        title = { Text("Awal periode bulanan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                    ArusButton(onClick = { onPeriodStartDayChange(day); choosePeriodStartDay = false },
                        kind = if (periodStartDay == day) ArusButtonKind.Secondary else ArusButtonKind.Outline,
                        selected = periodStartDay == day, modifier = Modifier.fillMaxWidth()) {
                        Text("Tanggal $day")
                    }
                }
            }
        },
        confirmButton = { OutlinedArusButton(onClick = { choosePeriodStartDay = false }) { Text("Tutup") } },
    )
}

internal object ArusFormatting {
    @Volatile private var numberFormat: String = "ID"
    @Volatile private var dateFormat: String = "LOCAL"

    fun update(numberFormat: String, dateFormat: String) {
        this.numberFormat = if (numberFormat == "EN") "EN" else "ID"
        this.dateFormat = if (dateFormat == "ISO") "ISO" else "LOCAL"
    }

    fun formatRupiah(amount: Long): String {
        val locale = if (numberFormat == "EN") java.util.Locale.US else java.util.Locale.forLanguageTag("id-ID")
        return "Rp ${java.text.NumberFormat.getNumberInstance(locale).format(amount)}"
    }

    fun formatDate(date: LocalDate): String = if (dateFormat == "ISO") {
        date.toString()
    } else {
        date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
            .withLocale(java.util.Locale.forLanguageTag("id-ID")))
    }

    fun formatDateTime(dateTime: java.time.ZonedDateTime): String =
        "${formatDate(dateTime.toLocalDate())} ${dateTime.format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT)
            .withLocale(java.util.Locale.forLanguageTag("id-ID")))}"
}
