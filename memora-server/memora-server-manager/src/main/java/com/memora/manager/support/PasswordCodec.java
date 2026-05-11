package com.memora.manager.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

@Component
public class PasswordCodec {
    private static final Pattern LEGACY_SHA256_PATTERN = Pattern.compile("^[a-f0-9]{64}$");

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public String hash(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            return "";
        }
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(passwordHash)) {
            return false;
        }
        if (isLegacyHash(passwordHash)) {
            return hashLegacySha256(rawPassword).equals(passwordHash);
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    public boolean needsRehash(String passwordHash) {
        return StringUtils.hasText(passwordHash) && isLegacyHash(passwordHash);
    }

    private boolean isLegacyHash(String passwordHash) {
        return LEGACY_SHA256_PATTERN.matcher(passwordHash).matches();
    }

    private String hashLegacySha256(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return toHex(encoded);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前环境不支持 SHA-256", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
