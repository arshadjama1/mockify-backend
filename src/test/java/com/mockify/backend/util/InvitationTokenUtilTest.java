package com.mockify.backend.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvitationTokenUtilTest {

    @Test
    void hash_is_deterministic_for_same_input() {
        String token = "fixed-token";
        String hash1 = InvitationTokenUtil.hash(token);
        String hash2 = InvitationTokenUtil.hash(token);

        assertEquals(hash1, hash2);
    }

    @Test
    void different_inputs_produce_different_hashes() {
        String hash1 = InvitationTokenUtil.hash("token-1");
        String hash2 = InvitationTokenUtil.hash("token-2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void hash_is_64_character_hex_string() {
        String hash = InvitationTokenUtil.hash("test");

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }
}