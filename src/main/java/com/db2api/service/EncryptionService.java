package com.db2api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data such as database passwords
 * and client secrets. Uses AES/GCM/NoPadding for semantically secure encryption
 * with an initialization vector (IV) prepended to the ciphertext.
 */
@Service
public class EncryptionService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${app.encryption.secret:defaultSecretKey123}")
    private String secret;

    private SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Derives a 256-bit AES key from the configured secret using SHA-256.
     */
    private void prepareSecretKey() {
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 32); // 256-bit key for AES-256
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            logger.error("Error while preparing key", e);
        }
    }

    /**
     * Encrypts the given plaintext string using AES/GCM.
     * A random IV is generated for each encryption and prepended to the ciphertext
     * so it can be extracted during decryption.
     *
     * @param strToEncrypt the plaintext to encrypt
     * @return Base64-encoded ciphertext with IV prepended, or null on error
     */
    public String encrypt(String strToEncrypt) {
        if (strToEncrypt == null) {
            return null;
        }
        try {
            prepareSecretKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            byte[] encrypted = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            logger.error("Error while encrypting", e);
        }
        return null;
    }

    /**
     * Decrypts the given Base64-encoded ciphertext (with IV prepended) using AES/GCM.
     *
     * @param strToDecrypt the Base64-encoded ciphertext to decrypt
     * @return the decrypted plaintext, or null on error
     */
    public String decrypt(String strToDecrypt) {
        if (strToDecrypt == null) {
            return null;
        }
        try {
            prepareSecretKey();
            byte[] decoded = Base64.getDecoder().decode(strToDecrypt);

            // Extract IV from the beginning of the ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Error while decrypting", e);
        }
        return null;
    }
}
