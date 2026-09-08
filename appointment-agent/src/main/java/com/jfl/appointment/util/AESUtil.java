package com.jfl.appointment.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {

    @Autowired
    private  static SecretKey secretKey;

    public AESUtil(@Value("${app.encryption.key}") String encryptionKey,SecretKey secretKey) {
        this.secretKey = new SecretKeySpec(
                Base64.getDecoder().decode(encryptionKey),
                "AES"
        );
        this.secretKey = secretKey;
    }

    public static String encrypt(String plainText) {
        try {
            byte[] iv = new byte[12];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            // Store IV + encrypted data together
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(buffer.array());

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt token", e);
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[12];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt token", e);
        }
    }
}
