package com.mockify.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cryptographic service for API key generation and hashing
 * Uses HMAC-SHA256 for secure, deterministic key hashing
 */
@Service
@Slf4j
public class ApiKeyCryptoService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // Key format: mk_live_<32 bytes base64url>
    private static final String KEY_PREFIX_LIVE = "mk_live_";
    private static final String KEY_PREFIX_TEST = "mk_test_";
    private static final int KEY_ENTROPY_BYTES = 32; // 256 bits

    /**
     * Generate a new API key with cryptographically secure randomness
     *
     * @param isTestMode true for test keys (mk_test_), false for live keys (mk_live_)
     * @return newly generated API key string
     */
    public String generateApiKey(boolean isTestMode) {
        byte[] randomBytes = new byte[KEY_ENTROPY_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        // Use base64url encoding (URL-safe, no padding)
        String encodedKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String prefix = isTestMode ? KEY_PREFIX_TEST : KEY_PREFIX_LIVE;
        return prefix + encodedKey;
    }

    /**
     * Generate HMAC-SHA256 hash of API key
     * This is what gets stored in the database
     *
     * @param apiKey the raw API key to hash
     * @param secret HMAC secret (should be unique per organization for additional security)
     * @return hex-encoded HMAC-SHA256 hash
     */
    public String hashApiKey(String apiKey, String secret) {
        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(),
                    HMAC_ALGORITHM
            );
            hmac.init(secretKey);

            byte[] hashBytes = hmac.doFinal(apiKey.getBytes());

            // Return hex-encoded hash
            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to hash API key", e);
            throw new RuntimeException("Cryptographic error during key hashing", e);
        }
    }

    /**
     * Verify API key against stored hash
     *
     * @param apiKey the raw API key to verify
     * @param storedHash the hash stored in database
     * @param secret HMAC secret used during hashing
     * @return true if key matches hash
     */
    public boolean verifyApiKey(String apiKey, String storedHash, String secret) {
        String computedHash = hashApiKey(apiKey, secret);

        // Use constant-time comparison to prevent timing attacks
        return constantTimeEquals(computedHash, storedHash);
    }

    /**
     * Extract visible prefix from API key
     * Used for displaying keys to users (mk_live_****)
     *
     * @param apiKey full API key
     * @return prefix + first 4 chars of key
     */
    public String extractKeyPrefix(String apiKey) {
        if (apiKey == null || apiKey.length() < 12) {
            throw new IllegalArgumentException("Invalid API key format");
        }

        // Extract prefix (mk_live_ or mk_test_)
        String prefix = apiKey.startsWith(KEY_PREFIX_LIVE) ? KEY_PREFIX_LIVE : KEY_PREFIX_TEST;

        // Return prefix + first 4 chars after prefix
        int prefixEnd = prefix.length();
        return apiKey.substring(0, Math.min(prefixEnd + 4, apiKey.length()));
    }

    /**
     * Validate API key format
     *
     * @param apiKey key to validate
     * @return true if format is valid
     */
    public boolean isValidKeyFormat(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        // Must start with valid prefix
        if (!apiKey.startsWith(KEY_PREFIX_LIVE) && !apiKey.startsWith(KEY_PREFIX_TEST)) {
            return false;
        }

        // Must have reasonable length (prefix + base64url encoded 32 bytes ≈ 51 chars)
        if (apiKey.length() < 40 || apiKey.length() > 60) {
            return false;
        }

        return true;
    }

    /**
     * Constant-time string comparison to prevent timing attacks
     * Essential for secure hash verification
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * Generate organization-specific HMAC secret
     * In production, this should be stored securely
     * For now, we derive it from organization ID
     *
     * @param organizationId organization UUID
     * @param globalSecret application-level secret
     * @return organization-specific secret
     */
    public String generateOrgSecret(String organizationId, String globalSecret) {
        return hashApiKey(organizationId, globalSecret);
    }
}