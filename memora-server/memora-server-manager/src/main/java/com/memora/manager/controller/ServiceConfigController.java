package com.memora.manager.controller;

import com.memora.common.result.Result;
import com.memora.manager.support.BrowserSessionSupport;
import com.memora.manager.support.RuntimeAuthModeSupport;
import com.memora.manager.vo.ServiceRuntimeConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ServiceConfigController {
    private final BrowserSessionSupport browserSessionSupport;
    private final RuntimeAuthModeSupport runtimeAuthModeSupport;

    @Value("${spring.application.name:memora-server}")
    private String applicationName;

    @GetMapping("/services/config")
    public Result<ServiceRuntimeConfigVO> getRuntimeConfig() {
        ServiceRuntimeConfigVO config = new ServiceRuntimeConfigVO();

        ServiceRuntimeConfigVO.AppConfig app = new ServiceRuntimeConfigVO.AppConfig();
        app.setName("Memora");
        app.setTitle("Memora - 在线文档工作区");
        config.setApp(app);

        ServiceRuntimeConfigVO.AuthConfig auth = new ServiceRuntimeConfigVO.AuthConfig();
        auth.setBrowserClientHeaderName(BrowserSessionSupport.CLIENT_HEADER);
        auth.setBrowserClientHeaderValue(BrowserSessionSupport.WEB_APP_CLIENT);
        auth.setSeedAccountLoginEnabled(runtimeAuthModeSupport.isSeedAccountLoginEnabled());
        auth.setSessionTransport("HTTP_ONLY_COOKIE");
        auth.setSessionCookieSameSite(browserSessionSupport.getSameSite());
        auth.setSessionCookieSecure(browserSessionSupport.isSecure());
        auth.setSessionCookieMaxAgeSeconds(browserSessionSupport.getCookieMaxAgeSeconds());
        auth.setRefreshEnabled(true);
        config.setAuth(auth);

        ServiceRuntimeConfigVO.FeatureConfig features = new ServiceRuntimeConfigVO.FeatureConfig();
        features.setOwnerRegistrationEnabled(true);
        features.setInviteAcceptEnabled(true);
        features.setWorkspaceSwitchEnabled(true);
        features.setKnowledgeBasePermissionEnabled(true);
        features.setPublicShareEnabled(true);
        features.setPublicSiteEnabled(true);
        features.setOpenApiEnabled(true);
        features.setAuditExportEnabled(true);
        config.setFeatures(features);

        app.setName(resolveAppName());
        return Result.success(config);
    }

    private String resolveAppName() {
        return "memora-server".equalsIgnoreCase(applicationName) ? "Memora" : applicationName;
    }
}
