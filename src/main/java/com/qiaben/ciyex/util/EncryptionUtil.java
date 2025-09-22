package com.qiaben.ciyex.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class EncryptionUtil {
    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int AES_KEY_SIZE = 256;   // bits

    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    public static byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] encrypted, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        return cipher.doFinal(encrypted);
    }

    public static SecretKey fromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }
}
