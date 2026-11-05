package com.illustravibes.arus.data;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Wraps the SQLCipher passphrase with an Android Keystore AES key. */
public final class DatabaseKeyStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "arus_database_wrapper_v1";
    private static final String PREFS = "database_key_envelope";
    private static final String ENVELOPE = "wrapped_passphrase";
    private static final int PASSPHRASE_SIZE = 32;

    private final android.content.Context context;

    public DatabaseKeyStore(android.content.Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized byte[] getOrCreatePassphrase() throws Exception {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        String encoded = prefs.getString(ENVELOPE, null);
        if (encoded != null) return decrypt(android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP));

        byte[] passphrase = new byte[PASSPHRASE_SIZE];
        new SecureRandom().nextBytes(passphrase);
        byte[] encrypted = encrypt(passphrase);
        boolean saved = prefs.edit().putString(ENVELOPE,
                android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)).commit();
        Arrays.fill(encrypted, (byte) 0);
        if (!saved) {
            Arrays.fill(passphrase, (byte) 0);
            throw new IllegalStateException("Kunci database tidak dapat disimpan.");
        }
        return passphrase;
    }

    private byte[] encrypt(byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrapperKey());
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(4 + iv.length + ciphertext.length)
                .putInt(iv.length).put(iv).put(ciphertext).array();
    }

    private byte[] decrypt(byte[] envelope) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(envelope);
        int ivLength = buffer.getInt();
        if (ivLength != 12 || buffer.remaining() <= ivLength) {
            throw new IllegalStateException("Kunci database tersimpan rusak.");
        }
        byte[] iv = new byte[ivLength];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrapperKey(), new GCMParameterSpec(128, iv));
        return cipher.doFinal(ciphertext);
    }

    private SecretKey getOrCreateWrapperKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
