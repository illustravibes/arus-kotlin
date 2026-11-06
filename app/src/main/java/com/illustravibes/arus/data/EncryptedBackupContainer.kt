package com.illustravibes.arus.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal data class EncryptedBackupMetadata(
    val formatVersion: Int,
    val schemaVersion: Int,
)

internal object EncryptedBackupContainer {
    private val MAGIC = byteArrayOf(0x41, 0x52, 0x55, 0x53, 0x42, 0x4b, 0x50, 0x31)
    private const val FORMAT_VERSION = 1
    private const val PBKDF2_ITERATIONS = 310_000
    private const val KEY_BYTES = 32
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val HEADER_BYTES = 8 + 4 + 4 + 4 + SALT_BYTES + NONCE_BYTES

    fun encrypt(input: File, output: File, password: CharArray, schemaVersion: Int) {
        require(password.size >= 8) { "Kata sandi backup harus memiliki minimal 8 karakter." }
        require(schemaVersion > 0)
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC)
            .putInt(FORMAT_VERSION)
            .putInt(schemaVersion)
            .putInt(PBKDF2_ITERATIONS)
            .put(salt)
            .put(nonce)
            .array()
        val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, nonce, PBKDF2_ITERATIONS)
        cipher.updateAAD(header)
        FileOutputStream(output).use { outputStream ->
            outputStream.write(header)
            FileInputStream(input).use { inputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = inputStream.read(buffer)
                    if (count < 0) break
                    cipher.update(buffer, 0, count)?.let(outputStream::write)
                }
            }
            cipher.doFinal()?.let(outputStream::write)
            outputStream.fd.sync()
        }
    }

    fun decrypt(input: File, output: File, password: CharArray): EncryptedBackupMetadata {
        require(input.length() > HEADER_BYTES + TAG_BITS / 8) { "File backup terlalu kecil atau rusak." }
        FileInputStream(input).use { inputStream ->
            val header = ByteArray(HEADER_BYTES)
            require(inputStream.read(header) == HEADER_BYTES) { "Header backup tidak lengkap." }
            val headerBuffer = ByteBuffer.wrap(header)
            val magic = ByteArray(MAGIC.size).also(headerBuffer::get)
            require(magic.contentEquals(MAGIC)) { "Format file backup tidak dikenali." }
            val formatVersion = headerBuffer.int
            val schemaVersion = headerBuffer.int
            val iterations = headerBuffer.int
            require(formatVersion == FORMAT_VERSION) { "Versi format backup tidak didukung." }
            require(schemaVersion > 0) { "Versi skema backup tidak valid." }
            require(iterations in 100_000..1_000_000) { "Parameter enkripsi backup tidak valid." }
            val salt = ByteArray(SALT_BYTES).also(headerBuffer::get)
            val nonce = ByteArray(NONCE_BYTES).also(headerBuffer::get)
            val cipher = cipher(Cipher.DECRYPT_MODE, password, salt, nonce, iterations)
            cipher.updateAAD(header)
            FileOutputStream(output).use { outputStream ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = inputStream.read(buffer)
                    if (count < 0) break
                    cipher.update(buffer, 0, count)?.let(outputStream::write)
                }
                cipher.doFinal()?.let(outputStream::write)
                outputStream.fd.sync()
            }
            return EncryptedBackupMetadata(formatVersion, schemaVersion)
        }
    }

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, nonce: ByteArray, iterations: Int): Cipher {
        val keySpec = PBEKeySpec(password, salt, iterations, KEY_BYTES * 8)
        val keyBytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(keySpec).encoded
        } finally {
            keySpec.clearPassword()
        }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            }
        } finally {
            keyBytes.fill(0)
        }
    }
}
