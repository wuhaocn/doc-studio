package com.memora.manager.support;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RuntimeAuthModeSupport {
    private final LocalAdminBootstrapProperties localAdminBootstrapProperties;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${memora.auth.allow-demo-token:false}")
    private Boolean allowDemoToken;

    public boolean isSeedAccountLoginEnabled() {
        return Boolean.TRUE.equals(allowDemoToken) || isLocalAdminBootstrapActive();
    }

    public boolean isLocalAdminBootstrapActive() {
        return localAdminBootstrapProperties.isEnabled() && isLocalFileH2Datasource();
    }

    private boolean isLocalFileH2Datasource() {
        return StringUtils.hasText(datasourceUrl) && datasourceUrl.startsWith("jdbc:h2:file:");
    }
}
