package com.memora;

import com.memora.manager.support.OpaqueTokenCodec;
import com.memora.manager.support.PasswordCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordCodecCompatibilityTest {
    private static final String LEGACY_SHA256_OF_123456 = "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";

    private final PasswordCodec passwordCodec = new PasswordCodec();
    private final OpaqueTokenCodec opaqueTokenCodec = new OpaqueTokenCodec();

    @Test
    void shouldMatchLegacySha256HashesAndMarkThemForUpgrade() {
        assertTrue(passwordCodec.matches("123456", LEGACY_SHA256_OF_123456));
        assertTrue(passwordCodec.needsRehash(LEGACY_SHA256_OF_123456));
    }

    @Test
    void shouldHashNewPasswordsWithBcrypt() {
        String encoded = passwordCodec.hash("12345678");
        assertTrue(passwordCodec.matches("12345678", encoded));
        assertFalse(passwordCodec.needsRehash(encoded));
        assertFalse(encoded.equals("12345678"));
        assertFalse(encoded.equals(LEGACY_SHA256_OF_123456));
    }

    @Test
    void shouldHashOpaqueTokensDeterministically() {
        String rawToken = "session:test-token";
        assertEquals(opaqueTokenCodec.hash(rawToken), opaqueTokenCodec.hash(rawToken));
    }
}
