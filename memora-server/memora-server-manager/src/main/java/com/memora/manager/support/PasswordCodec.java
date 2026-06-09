package com.memora.manager.support;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PasswordCodec {
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
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
