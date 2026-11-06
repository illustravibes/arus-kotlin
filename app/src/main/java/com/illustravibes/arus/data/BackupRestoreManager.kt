package com.illustravibes.arus.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal class BackupRestoreException(
    message: String,
    cause: Throwable,
    val recoveredDatabase: ArusDatabase?,
) : Exception(message, cause)

internal object BackupRestoreManager {
    const val BACKUP_MIME_TYPE = "application/vnd.arus.backup"
    private const val MAX_BACKUP_BYTES = 512L * 1024L * 1024L
    private const val MAX_ENTRIES = 20_000
    private const val MAX_EXPANDED_BYTES = 512L * 1024L * 1024L
    private val manifestMagic = byteArrayOf(0x41, 0x52, 0x55, 0x53, 0x4d, 0x45, 0x54, 0x41)

    fun createBackup(
        context: Context,
        database: ArusDatabase,
        password: CharArray,
        output: File,
        databaseName: String = DatabaseFactory.DATABASE_NAME,
        filesDirectory: File = context.filesDir,
    ) {
        val stage = File(context.cacheDir, "backup-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val zipFile = File(stage, "payload.zip")
        val encryptedFile = File(stage, "backup.arusbackup")
        var passphrase: ByteArray? = null
        try {
            checkpoint(database.openHelper.writableDatabase)
            val databaseFile = context.getDatabasePath(databaseName)
            check(databaseFile.isFile) { "Database Arus tidak ditemukan." }
            passphrase = DatabaseKeyStore(context).getOrCreatePassphrase()
            val attachments = database.attachmentDao().allAttachments()
            writePayload(filesDirectory, databaseFile, passphrase, attachments, zipFile)
            EncryptedBackupContainer.encrypt(zipFile, encryptedFile, password, ArusDatabase.CURRENT_VERSION)
            output.outputStream().use { target -> encryptedFile.inputStream().use { it.copyTo(target) } }
        } finally {
            passphrase?.fill(0)
            stage.deleteRecursively()
        }
    }

    fun restore(
        context: Context,
        currentDatabase: ArusDatabase,
        backupFile: File,
        password: CharArray,
        databaseName: String = DatabaseFactory.DATABASE_NAME,
        filesDirectory: File = context.filesDir,
    ): ArusDatabase {
        require(backupFile.isFile && backupFile.length() <= MAX_BACKUP_BYTES) { "File backup tidak ditemukan atau terlalu besar." }
        val stage = File(context.cacheDir, "restore-${UUID.randomUUID()}").apply { check(mkdirs()) }
        val zipFile = File(stage, "payload.zip")
        val extracted = File(stage, "extracted").apply { mkdirs() }
        val stageDatabaseName = "arus-restore-${UUID.randomUUID().toString().replace("-", "")}.db"
        val stageDatabaseFile = context.getDatabasePath(stageDatabaseName)
        var backupPassphrase: ByteArray? = null
        var previousPassphrase: ByteArray? = null
        var stagedDatabase: ArusDatabase? = null
        var currentClosed = false
        var targetDatabaseMoved = false
        var targetAttachmentsMoved = false
        var newKeyStored = false
        var reopenedDatabase: ArusDatabase? = null
        val targetDatabaseFile = context.getDatabasePath(databaseName)
        val targetDatabaseBackup = File(targetDatabaseFile.parentFile, "$databaseName.restore-old")
        val targetDatabaseNew = File(targetDatabaseFile.parentFile, "$databaseName.restore-new")
        val attachmentsDirectory = File(filesDirectory, "receipts")
        val attachmentsBackup = File(filesDirectory, "receipts.restore-old")
        val attachmentsNew = File(filesDirectory, "receipts.restore-new")

        try {
            val header = EncryptedBackupContainer.decrypt(backupFile, zipFile, password)
            if (header.schemaVersion > ArusDatabase.CURRENT_VERSION) {
                throw IllegalArgumentException("Perbarui aplikasi untuk memulihkan backup ini.")
            }
            val payload = extractPayload(zipFile, extracted)
            if (payload.schemaVersion != header.schemaVersion) throw IllegalArgumentException("Versi skema backup tidak cocok.")
            if (payload.attachmentCount != payload.attachmentFiles.size) throw IllegalArgumentException("Lampiran backup tidak lengkap.")
            backupPassphrase = payload.passphrase

            stageDatabaseFile.parentFile?.mkdirs()
            FileOutputStream(stageDatabaseFile).use { output ->
                FileInputStream(payload.databaseFile).use { input -> input.copyTo(output) }
                output.fd.sync()
            }
            stagedDatabase = DatabaseFactory.openWithPassphrase(context, stageDatabaseName, backupPassphrase)
            val stagedAttachments = stagedDatabase!!.attachmentDao().allAttachments()
            stagedAttachments.forEach { attachment ->
                val file = privateFile(File(extracted, "attachments"), attachment.filePath)
                check(file.isFile) { "Lampiran backup hilang: ${attachment.filePath}" }
            }
            checkpoint(stagedDatabase!!.openHelper.writableDatabase)
            stagedDatabase!!.close()
            stagedDatabase = null

            previousPassphrase = DatabaseKeyStore(context).getOrCreatePassphrase()
            check(!targetDatabaseBackup.exists() || targetDatabaseBackup.delete()) { "File restore lama tidak dapat dibersihkan." }
            check(!targetDatabaseNew.exists() || targetDatabaseNew.delete()) { "File restore sementara tidak dapat dibersihkan." }
            copyFile(stageDatabaseFile, targetDatabaseNew)
            check(!attachmentsNew.exists() || attachmentsNew.deleteRecursively()) { "Folder lampiran sementara tidak dapat dibersihkan." }
            val stagedReceiptDirectory = File(extracted, "attachments/receipts")
            if (stagedReceiptDirectory.exists()) copyDirectory(stagedReceiptDirectory, attachmentsNew)
            else check(attachmentsNew.mkdirs()) { "Folder lampiran sementara tidak dapat dibuat." }

            currentDatabase.close()
            currentClosed = true
            deleteDatabaseSidecars(targetDatabaseFile)
            if (targetDatabaseFile.exists()) {
                moveFile(targetDatabaseFile, targetDatabaseBackup)
                targetDatabaseMoved = true
            }
            if (attachmentsDirectory.exists()) {
                moveFile(attachmentsDirectory, attachmentsBackup)
                targetAttachmentsMoved = true
            }
            moveFile(targetDatabaseNew, targetDatabaseFile)
            moveFile(attachmentsNew, attachmentsDirectory)
            DatabaseKeyStore(context).replacePassphrase(backupPassphrase)
            newKeyStored = true
            reopenedDatabase = DatabaseFactory.open(context, databaseName)
            targetDatabaseBackup.delete()
            attachmentsBackup.deleteRecursively()
            return reopenedDatabase!!
        } catch (failure: Exception) {
            stagedDatabase?.close()
            reopenedDatabase?.close()
            var recoveryFailure: Exception? = null
            if (currentClosed) {
                runCatching {
                    if (targetDatabaseFile.exists()) targetDatabaseFile.delete()
                    if (targetDatabaseMoved && targetDatabaseBackup.exists()) moveFile(targetDatabaseBackup, targetDatabaseFile)
                    if (attachmentsDirectory.exists()) attachmentsDirectory.deleteRecursively()
                    if (targetAttachmentsMoved && attachmentsBackup.exists()) moveFile(attachmentsBackup, attachmentsDirectory)
                    if (newKeyStored && previousPassphrase != null)
                        DatabaseKeyStore(context).replacePassphrase(previousPassphrase)
                    reopenedDatabase = DatabaseFactory.open(context, databaseName)
                }.onFailure { recoveryFailure = it as? Exception ?: RuntimeException(it) }
            }
            val message = when (failure) {
                is javax.crypto.AEADBadTagException -> "Kata sandi salah atau file backup rusak. Data lokal tidak berubah."
                is IllegalArgumentException -> failure.message ?: "File backup tidak valid. Data lokal tidak berubah."
                else -> "Restore gagal. Data lokal sebelumnya dipulihkan."
            }
            val cause = if (recoveryFailure == null) failure else IOException(message, recoveryFailure)
            if (currentClosed) throw BackupRestoreException(message, cause, reopenedDatabase)
            throw IllegalArgumentException(message, failure)
        } finally {
            stagedDatabase?.close()
            backupPassphrase?.fill(0)
            previousPassphrase?.fill(0)
            targetDatabaseNew.delete()
            attachmentsNew.deleteRecursively()
            stage.deleteRecursively()
            deleteDatabaseSidecars(stageDatabaseFile)
        }
    }

    private data class Payload(
        val schemaVersion: Int,
        val attachmentCount: Int,
        val databaseFile: File,
        val passphrase: ByteArray,
        val attachmentFiles: Set<String>,
    )

    private fun writePayload(
        filesDirectory: File,
        databaseFile: File,
        passphrase: ByteArray,
        attachments: List<AttachmentEntity>,
        zipFile: File,
    ) {
        val attachmentFiles = attachments.associate { attachment ->
            val file = privateFile(filesDirectory, attachment.filePath)
            check(file.isFile) { "Lampiran tidak ditemukan: ${attachment.filePath}" }
            attachment.filePath to file
        }
        require(databaseFile.length() + attachmentFiles.values.sumOf(File::length) <= MAX_EXPANDED_BYTES) {
            "Backup lebih besar dari batas yang didukung."
        }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
            zip.putNextEntry(ZipEntry("metadata.bin"))
            DataOutputStream(zip).apply {
                write(manifestMagic)
                writeInt(1)
                writeInt(ArusDatabase.CURRENT_VERSION)
                writeLong(System.currentTimeMillis())
                writeInt(attachments.size)
                flush()
            }
            zip.closeEntry()
            putFile(zip, "database/arus.db", databaseFile)
            zip.putNextEntry(ZipEntry("database/passphrase"))
            zip.write(passphrase)
            zip.closeEntry()
            attachmentFiles.forEach { (relativePath, file) -> putFile(zip, "attachments/$relativePath", file) }
        }
    }

    private fun extractPayload(zipFile: File, outputDirectory: File): Payload {
        var schemaVersion: Int? = null
        var attachmentCount: Int? = null
        var databaseFile: File? = null
        var passphrase: ByteArray? = null
        val attachmentFiles = mutableSetOf<String>()
        var totalExtracted = 0L
        var entryCount = 0
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "Backup berisi terlalu banyak berkas." }
                require(!entry.isDirectory) { "Folder tidak diizinkan di dalam backup." }
                val name = entry.name
                require(!name.startsWith('/') && !name.contains('\\') && name.split('/').none { it == ".." || it.isEmpty() }) {
                    "Jalur berkas backup tidak valid."
                }
                when {
                    name == "metadata.bin" -> {
                        val bytes = zip.readBounded(MAX_MANIFEST_BYTES)
                        DataInputStream(ByteArrayInputStream(bytes)).use { manifest ->
                            val magic = ByteArray(manifestMagic.size).also(manifest::readFully)
                            require(magic.contentEquals(manifestMagic)) { "Metadata backup tidak valid." }
                            require(manifest.readInt() == 1) { "Versi metadata backup tidak didukung." }
                            schemaVersion = manifest.readInt()
                            manifest.readLong()
                            attachmentCount = manifest.readInt()
                            require(manifest.available() == 0) { "Metadata backup memiliki data tambahan." }
                        }
                        totalExtracted += bytes.size
                    }
                    name == "database/arus.db" -> {
                        val file = File(outputDirectory, "database/arus.db")
                        file.parentFile?.mkdirs()
                        totalExtracted += zip.extractTo(file, MAX_EXPANDED_BYTES - totalExtracted)
                        databaseFile = file
                    }
                    name == "database/passphrase" -> {
                        val bytes = zip.readBounded(64)
                        require(bytes.size == 32) { "Kunci database backup tidak valid." }
                        passphrase = bytes
                        totalExtracted += bytes.size
                    }
                    name.startsWith("attachments/") -> {
                        val relative = name.removePrefix("attachments/")
                        require(relative.startsWith("receipts/")) { "Jalur lampiran backup tidak diizinkan." }
                        val file = privateFile(File(outputDirectory, "attachments"), relative)
                        file.parentFile?.mkdirs()
                        totalExtracted += zip.extractTo(file, MAX_EXPANDED_BYTES - totalExtracted)
                        attachmentFiles += relative
                    }
                    else -> throw IllegalArgumentException("Isi file backup tidak dikenali.")
                }
                require(totalExtracted <= MAX_EXPANDED_BYTES) { "Isi backup melebihi batas yang didukung." }
                zip.closeEntry()
            }
        }
        return Payload(
            requireNotNull(schemaVersion) { "Metadata backup hilang." },
            requireNotNull(attachmentCount) { "Jumlah lampiran backup hilang." },
            requireNotNull(databaseFile) { "Database backup hilang." },
            requireNotNull(passphrase) { "Kunci database backup hilang." },
            attachmentFiles,
        )
    }

    private fun checkpoint(database: SupportSQLiteDatabase) {
        database.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            if (cursor.moveToFirst() && cursor.getInt(0) != 0) throw IOException("Database sedang dipakai. Coba backup lagi.")
        }
    }

    private fun privateFile(root: File, relativePath: String): File {
        require(relativePath.isNotBlank() && !File(relativePath).isAbsolute && !relativePath.contains('\\')) {
            "Jalur lampiran tidak valid."
        }
        val base = root.canonicalFile
        val file = File(base, relativePath).canonicalFile
        require(file.path.startsWith(base.path + File.separator)) { "Jalur lampiran keluar dari folder privat." }
        return file
    }

    private fun putFile(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun ZipInputStream.readBounded(maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var readTotal = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            readTotal += count
            require(readTotal <= maxBytes) { "Berkas backup terlalu besar." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun ZipInputStream.extractTo(output: File, maxBytes: Long): Long {
        require(maxBytes > 0L) { "Backup melebihi batas ukuran." }
        var copied = 0L
        FileOutputStream(output).use { stream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = read(buffer)
                if (count < 0) break
                copied += count
                require(copied <= maxBytes) { "Berkas hasil dekompresi melebihi batas." }
                stream.write(buffer, 0, count)
            }
            stream.fd.sync()
        }
        return copied
    }

    private fun copyFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        FileInputStream(source).use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
    }

    private fun copyDirectory(source: File, target: File) {
        check(target.mkdirs() || target.isDirectory) { "Folder lampiran sementara tidak dapat dibuat." }
        source.listFiles().orEmpty().forEach { child ->
            val destination = File(target, child.name)
            if (child.isDirectory) copyDirectory(child, destination) else copyFile(child, destination)
        }
    }

    private fun moveFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun deleteDatabaseSidecars(databaseFile: File) {
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()
        File(databaseFile.path + "-journal").delete()
    }

    private const val MAX_MANIFEST_BYTES = 1024L
}
