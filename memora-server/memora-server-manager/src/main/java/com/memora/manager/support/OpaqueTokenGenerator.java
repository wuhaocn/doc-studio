package com.memora.manager.support;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OpaqueTokenGenerator {
    public String generate(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
