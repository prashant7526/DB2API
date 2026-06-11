package com.db2api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EncryptionService}.
 * Verifies AES/GCM encryption and decryption round-trips, null handling,
 * and that different encryptions of the same plaintext produce different
 * ciphertexts (due to random IV).
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secret", "test-secret-key-for-unit-tests-256bit");
    }

    @Test
    void encrypt_shouldReturnNonEmptyBase64() {
        String result = encryptionService.encrypt("hello");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void decrypt_shouldReturnOriginalPlaintext() {
        String original = "my-secret-password-123!";
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_shouldProduceDifferentCiphertextsForSamePlaintext() {
        String plaintext = "same-input";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        // Due to random IV, same plaintext should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void decrypt_shouldHandleEmptyString() {
        String encrypted = encryptionService.encrypt("");
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals("", decrypted);
    }

    @Test
    void decrypt_shouldReturnNullForInvalidInput() {
        String result = encryptionService.decrypt("not-valid-base64!!!");
        assertNull(result);
    }

    @Test
    void encrypt_shouldHandleUnicodeCharacters() {
        String original = "密码テスト🔐";
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_shouldHandleLongStrings() {
        String original = "a".repeat(10_000);
        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void decrypt_shouldReturnNullForNullInput() {
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void encrypt_shouldReturnNullForNullInput() {
        assertNull(encryptionService.encrypt(null));
    }
}
