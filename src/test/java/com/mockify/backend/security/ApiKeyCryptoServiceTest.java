package com.mockify.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyCryptoServiceTest {

    private ApiKeyCryptoService cryptoService;
    private static final String TEST_SECRET = "test-secret-for-hashing";

    @BeforeEach
    void setUp() {
        cryptoService = new ApiKeyCryptoService();
    }

    @Test
    void testGenerateApiKey_LiveMode() {
        String key = cryptoService.generateApiKey(false);

        assertNotNull(key);
        assertTrue(key.startsWith("mk_live_"));
        assertTrue(key.length() >= 40 && key.length() <= 60);
    }

    @Test
    void testGenerateApiKey_TestMode() {
        String key = cryptoService.generateApiKey(true);

        assertNotNull(key);
        assertTrue(key.startsWith("mk_test_"));
        assertTrue(key.length() >= 40 && key.length() <= 60);
    }

    @Test
    void testGenerateApiKey_Uniqueness() {
        String key1 = cryptoService.generateApiKey(false);
        String key2 = cryptoService.generateApiKey(false);

        assertNotEquals(key1, key2);
    }

    @Test
    void testHashApiKey_Deterministic() {
        String apiKey = "mk_live_test1234567890";

        String hash1 = cryptoService.hashApiKey(apiKey, TEST_SECRET);
        String hash2 = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 = 64 hex chars
    }

    @Test
    void testHashApiKey_DifferentSecrets() {
        String apiKey = "mk_live_test1234567890";

        String hash1 = cryptoService.hashApiKey(apiKey, "secret1");
        String hash2 = cryptoService.hashApiKey(apiKey, "secret2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testHashApiKey_DifferentKeys() {
        String hash1 = cryptoService.hashApiKey("mk_live_key1", TEST_SECRET);
        String hash2 = cryptoService.hashApiKey("mk_live_key2", TEST_SECRET);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testVerifyApiKey_Success() {
        String apiKey = "mk_live_test1234567890";
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        assertTrue(cryptoService.verifyApiKey(apiKey, hash, TEST_SECRET));
    }

    @Test
    void testVerifyApiKey_Failure() {
        String apiKey = "mk_live_test1234567890";
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);
        String wrongKey = "mk_live_wrong_key";

        assertFalse(cryptoService.verifyApiKey(wrongKey, hash, TEST_SECRET));
    }

    @Test
    void testExtractKeyPrefix_LiveKey() {
        String apiKey = "mk_live_abcdefghijklmnop";
        String prefix = cryptoService.extractKeyPrefix(apiKey);

        assertEquals("mk_live_abcd", prefix);
    }

    @Test
    void testExtractKeyPrefix_TestKey() {
        String apiKey = "mk_test_abcdefghijklmnop";
        String prefix = cryptoService.extractKeyPrefix(apiKey);

        assertEquals("mk_test_abcd", prefix);
    }

    @Test
    void testExtractKeyPrefix_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            cryptoService.extractKeyPrefix("invalid");
        });
    }

    @Test
    void testIsValidFormat_GeneratedKey() {
        String key = cryptoService.generateApiKey(false);
        assertTrue(cryptoService.isValidKeyFormat(key));
    }

    @Test
    void testIsValidKeyFormat_LiveKey() {
        String validKey = "mk_live_" + "a".repeat(40);
        assertTrue(cryptoService.isValidKeyFormat(validKey));
    }

    @Test
    void testIsValidKeyFormat_TestKey() {
        String validKey = "mk_test_" + "a".repeat(40);
        assertTrue(cryptoService.isValidKeyFormat(validKey));
    }

    @Test
    void testIsValidKeyFormat_InvalidPrefix() {
        String invalidKey = "invalid_prefix_" + "a".repeat(40);
        assertFalse(cryptoService.isValidKeyFormat(invalidKey));
    }

    @Test
    void testIsValidKeyFormat_TooShort() {
        String shortKey = "mk_live_abc";
        assertFalse(cryptoService.isValidKeyFormat(shortKey));
    }

    @Test
    void testIsValidKeyFormat_TooLong() {
        String longKey = "mk_live_" + "a".repeat(100);
        assertFalse(cryptoService.isValidKeyFormat(longKey));
    }

    @Test
    void testIsValidKeyFormat_Null() {
        assertFalse(cryptoService.isValidKeyFormat(null));
    }

    @Test
    void testIsValidKeyFormat_Empty() {
        assertFalse(cryptoService.isValidKeyFormat(""));
    }

    @Test
    void testGenerateOrgSecret_Deterministic() {
        String orgId = "550e8400-e29b-41d4-a716-446655440000";

        String secret1 = cryptoService.generateOrgSecret(orgId, TEST_SECRET);
        String secret2 = cryptoService.generateOrgSecret(orgId, TEST_SECRET);

        assertEquals(secret1, secret2);
    }

    @Test
    void testGenerateOrgSecret_DifferentOrgs() {
        String orgId1 = "550e8400-e29b-41d4-a716-446655440000";
        String orgId2 = "660e8400-e29b-41d4-a716-446655440000";

        String secret1 = cryptoService.generateOrgSecret(orgId1, TEST_SECRET);
        String secret2 = cryptoService.generateOrgSecret(orgId2, TEST_SECRET);

        assertNotEquals(secret1, secret2);
    }

    @Test
    void testFullWorkflow_GenerateHashVerify() {
        // Generate key
        String apiKey = cryptoService.generateApiKey(false);

        // Hash it
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        // Verify it
        assertTrue(cryptoService.verifyApiKey(apiKey, hash, TEST_SECRET));

        // Verify wrong key fails
        String wrongKey = cryptoService.generateApiKey(false);
        assertFalse(cryptoService.verifyApiKey(wrongKey, hash, TEST_SECRET));
    }
}