package com.memora.manager.vo;

import lombok.Data;

@Data
public class ServiceRuntimeConfigVO {
    private AppConfig app;

    private AuthConfig auth;

    private FeatureConfig features;

    @Data
    public static class AppConfig {
        private String name;

        private String title;
    }

    @Data
    public static class AuthConfig {
        private String browserClientHeaderName;

        private String browserClientHeaderValue;

        private Boolean seedAccountLoginEnabled;

        private String sessionTransport;

        private String sessionCookieSameSite;

        private Boolean sessionCookieSecure;

        private Long sessionCookieMaxAgeSeconds;

        private Boolean refreshEnabled;
    }

    @Data
    public static class FeatureConfig {
        private Boolean ownerRegistrationEnabled;

        private Boolean inviteAcceptEnabled;

        private Boolean workspaceSwitchEnabled;

        private Boolean knowledgeBasePermissionEnabled;

        private Boolean publicShareEnabled;

        private Boolean publicSiteEnabled;

        private Boolean openApiEnabled;

        private Boolean auditExportEnabled;
    }
}
