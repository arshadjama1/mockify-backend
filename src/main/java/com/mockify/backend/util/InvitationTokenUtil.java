package com.mockify.backend.util;

import java.security.MessageDigest;
import java.util.HexFormat;

public final class InvitationTokenUtil {

    private InvitationTokenUtil() {}

    /**
     * Returns the SHA-256 hex digest of rawToken.
     * Deterministic — same input always produces the same 64-char hex string.
     * Use this for both storing and looking up invitation tokens.
     */
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}