package com.memora.manager.support;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "memora.bootstrap.local-admin")
public class LocalAdminBootstrapProperties {
    private boolean enabled = true;

    private String username = "admin";

    private String password = "123456";

    private String displayName = "本地管理员";

    private String email = "admin@memora.local";

    private String tenantName = "Memora 默认工作区";

    private String tenantSlug = "memora-local";

    private String industry = "本地联调";

    private String planName = "TEAM";
}
