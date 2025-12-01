package com.db2api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.security.MessageDigest;
import java.util.Arrays;

@Service
public class EncryptionService {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EncryptionService.class);

    @Value("${app.encryption.secret:defaultSecretKey123}")
    private String secret;

    private SecretKeySpec secretKey;

    private void prepareSecreteKey() {
        try {
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            logger.error("Error while preparing key", e);
        }
    }

    public String encrypt(String strToEncrypt) {
        try {
            prepareSecreteKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            logger.error("Error while encrypting", e);
        }
        return null;
    }

    public String decrypt(String strToDecrypt) {
        try {
            prepareSecreteKey();
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            logger.error("Error while decrypting", e);
        }
        return null;
    }
}
