package com.memora.manager.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class PasswordCodec {
    public String hash(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return toHex(encoded);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前环境不支持 SHA-256", ex);
        }
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(passwordHash)) {
            return false;
        }
        return hash(rawPassword).equals(passwordHash);
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            builder.append(String.format("%02x", current));
        }
        return builder.toString();
    }
}
