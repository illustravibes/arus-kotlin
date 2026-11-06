package com.illustravibes.arus.ui

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.illustravibes.arus.data.ArusDatabase
import com.illustravibes.arus.data.BackupRestoreException
import com.illustravibes.arus.data.BackupRestoreManager
import com.illustravibes.arus.ui.designsystem.ArusButton
import com.illustravibes.arus.ui.designsystem.ArusCard
import com.illustravibes.arus.ui.designsystem.ArusDialog
import com.illustravibes.arus.ui.designsystem.ArusSpacing
import com.illustravibes.arus.ui.designsystem.ArusTextField
import com.illustravibes.arus.ui.designsystem.ArusTheme
import com.illustravibes.arus.ui.designsystem.OutlinedArusButton
import com.illustravibes.arus.ui.designsystem.Text
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RESTORE_FILE_LIMIT_BYTES = 512L * 1024L * 1024L

@Composable
internal fun BackupRestoreManagement(
    database: ArusDatabase,
    onDismiss: () -> Unit,
    onDatabaseReplaced: (ArusDatabase) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var backupPassword by remember { mutableStateOf("") }
    var backupPasswordConfirmation by remember { mutableStateOf("") }
    var restorePassword by remember { mutableStateOf("") }
    var selectedBackupUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBackupInfo by remember { mutableStateOf<String?>(null) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val createBackupPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupRestoreManager.BACKUP_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            val password = backupPassword.toCharArray()
            scope.launch {
                val tempFile = File(context.cacheDir, "arus-backup-${System.nanoTime()}.tmp")
                isWorking = true
                message = null
                error = null
                try {
                    withContext(Dispatchers.IO) {
                        BackupRestoreManager.createBackup(context, database, password, tempFile)
                        val output = context.contentResolver.openOutputStream(uri, "w")
                            ?: throw IllegalStateException("File tujuan tidak dapat dibuka.")
                        output.use { stream -> tempFile.inputStream().use { it.copyTo(stream) } }
                    }
                    message = "Backup terenkripsi berhasil dibuat. Simpan kata sandi dengan aman."
                    backupPassword = ""
                    backupPasswordConfirmation = ""
                } catch (failure: Exception) {
                    error = failure.message ?: "Backup tidak dapat dibuat."
                } finally {
                    password.fill('\u0000')
                    tempFile.delete()
                    isWorking = false
                }
            }
        }
    }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedBackupUri = uri
        selectedBackupInfo = uri?.let { describeDocument(context.contentResolver, it) }
        restorePassword = ""
        message = null
        error = null
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ArusSpacing.Medium),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
            OutlinedArusButton(onClick = onDismiss, enabled = !isWorking) { Text("‹ Kembali") }
            Text("Backup & Restore", style = ArusTheme.typography.headline)
        }
        Text("File backup berisi database terenkripsi dan foto lampiran. Pilih lokasi file di perangkat atau penyedia berkas.")
        message?.let { Text(it, style = ArusTheme.typography.body.copy(color = ArusTheme.colors.mutedForeground)) }
        error?.let { Text(it, style = ArusTheme.typography.body.copy(color = ArusTheme.colors.error)) }

        ArusCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(ArusSpacing.Medium), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Buat backup", style = ArusTheme.typography.title)
                Text("Gunakan kata sandi minimal 8 karakter. Arus tidak dapat memulihkan kata sandi yang hilang.")
                Text("Kata sandi backup", style = ArusTheme.typography.label)
                ArusTextField(
                    backupPassword,
                    { backupPassword = it; error = null },
                    placeholder = "Kata sandi backup",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Ulangi kata sandi", style = ArusTheme.typography.label)
                ArusTextField(
                    backupPasswordConfirmation,
                    { backupPasswordConfirmation = it; error = null },
                    placeholder = "Ulangi kata sandi",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                ArusButton(onClick = {
                    when {
                        backupPassword.length < 8 -> error = "Kata sandi backup harus minimal 8 karakter."
                        backupPassword != backupPasswordConfirmation -> error = "Kata sandi tidak sama."
                        else -> createBackupPicker.launch("arus-backup-${System.currentTimeMillis()}.arusbackup")
                    }
                }, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) { Text("Buat file backup") }
            }
        }

        ArusCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(ArusSpacing.Medium), verticalArrangement = Arrangement.spacedBy(ArusSpacing.Small)) {
                Text("Pulihkan backup", style = ArusTheme.typography.title)
                OutlinedArusButton(onClick = { restorePicker.launch(arrayOf("*/*")) }, enabled = !isWorking, modifier = Modifier.fillMaxWidth()) {
                    Text("Pilih file backup")
                }
                selectedBackupInfo?.let { Text(it) }
                selectedBackupUri?.let {
                    Text("Kata sandi backup", style = ArusTheme.typography.label)
                    ArusTextField(
                        restorePassword,
                        { restorePassword = it; error = null },
                        placeholder = "Kata sandi backup",
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ArusButton(
                        onClick = { showRestoreConfirmation = true },
                        enabled = !isWorking && restorePassword.length >= 8,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Pulihkan data") }
                }
            }
        }
        if (isWorking) Text("Memproses backup dengan aman…")
    }

    if (showRestoreConfirmation) ArusDialog(
        onDismissRequest = { if (!isWorking) showRestoreConfirmation = false },
        title = { Text("Ganti data lokal?") },
        text = { Text("Restore akan mengganti semua data lokal. Data lama hanya tergantikan setelah file, kata sandi, dan skema berhasil divalidasi.") },
        confirmButton = {
            ArusButton(onClick = {
                val uri = selectedBackupUri ?: return@ArusButton
                val password = restorePassword.toCharArray()
                showRestoreConfirmation = false
                isWorking = true
                error = null
                message = null
                scope.launch {
                    val tempFile = File(context.cacheDir, "arus-restore-${System.nanoTime()}.tmp")
                    try {
                        val restored = withContext(Dispatchers.IO) {
                            copyDocument(context.contentResolver, uri, tempFile)
                            BackupRestoreManager.restore(context, database, tempFile, password)
                        }
                        onDatabaseReplaced(restored)
                        selectedBackupUri = null
                        selectedBackupInfo = null
                        restorePassword = ""
                        message = "Data berhasil dipulihkan."
                    } catch (failure: Exception) {
                        if (failure is BackupRestoreException) failure.recoveredDatabase?.let(onDatabaseReplaced)
                        error = failure.message ?: "Restore tidak dapat diselesaikan."
                    } finally {
                        password.fill('\u0000')
                        tempFile.delete()
                        isWorking = false
                    }
                }
            }, enabled = !isWorking) { Text("Ganti data dan pulihkan") }
        },
        dismissButton = { OutlinedArusButton(onClick = { showRestoreConfirmation = false }, enabled = !isWorking) { Text("Batal") } },
    )
}

private fun describeDocument(contentResolver: ContentResolver, uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "File backup"
            val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
            val sizeLabel = if (size >= 0) "${size / 1024} KB" else "Ukuran tidak tersedia"
            return "$name · $sizeLabel"
        }
    }
    return "File backup dipilih"
}

private fun copyDocument(contentResolver: ContentResolver, uri: Uri, destination: File) {
    val input = contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("File backup tidak dapat dibuka.")
    try {
        input.use { source ->
            destination.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= RESTORE_FILE_LIMIT_BYTES) { "File backup melebihi batas 512 MB." }
                    output.write(buffer, 0, count)
                }
            }
        }
    } catch (failure: Exception) {
        destination.delete()
        throw failure
    }
}
