package com.memora.manager.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.UserSession;
import com.memora.manager.mapper.UserSessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CurrentAccessContext {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_PAYLOAD_ATTR = CurrentAccessContext.class.getName() + ".payload";
    private static final String DEMO_TOKEN_PREFIX = "demo:";
    private static final String SESSION_TOKEN_PREFIX = "session:";

    private final UserSessionMapper userSessionMapper;

    @Value("${memora.auth.allow-demo-token:false}")
    private boolean allowDemoToken;

    public Long getCurrentTenantId() {
        return requireAccessTokenPayload().tenantId();
    }

    public Long getCurrentTenantIdOrNull() {
        AccessTokenPayload payload = resolveAccessTokenPayload();
        return payload == null ? null : payload.tenantId();
    }

    public Long getCurrentUserId() {
        return requireAccessTokenPayload().userId();
    }

    public Long getCurrentUserIdOrNull() {
        AccessTokenPayload payload = resolveAccessTokenPayload();
        return payload == null ? null : payload.userId();
    }

    public String getCurrentAccessToken() {
        return requireAccessTokenPayload().accessToken();
    }

    public boolean isCurrentSessionToken() {
        return requireAccessTokenPayload().sessionToken();
    }

    private AccessTokenPayload requireAccessTokenPayload() {
        AccessTokenPayload tokenPayload = resolveAccessTokenPayload();
        if (tokenPayload == null) {
            throw new BusinessException(401, "当前请求未携带有效会话");
        }
        return tokenPayload;
    }

    private AccessTokenPayload resolveAccessTokenPayload() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }

        HttpServletRequest request = servletAttributes.getRequest();
        Object cachedPayload = request.getAttribute(ACCESS_TOKEN_PAYLOAD_ATTR);
        if (cachedPayload instanceof AccessTokenPayload accessTokenPayload) {
            return accessTokenPayload;
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isBlank() || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        AccessTokenPayload resolvedPayload;
        if (token.startsWith(SESSION_TOKEN_PREFIX)) {
            resolvedPayload = resolveSessionAccessToken(token);
        } else if (allowDemoToken && token.startsWith(DEMO_TOKEN_PREFIX)) {
            resolvedPayload = resolveDemoAccessToken(token);
        } else {
            resolvedPayload = null;
        }

        if (resolvedPayload != null) {
            request.setAttribute(ACCESS_TOKEN_PAYLOAD_ATTR, resolvedPayload);
        }
        return resolvedPayload;
    }

    private AccessTokenPayload resolveSessionAccessToken(String token) {
        LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSession::getAccessToken, token)
            .eq(UserSession::getStatus, 1)
            .gt(UserSession::getExpiresAt, LocalDateTime.now())
            .last("LIMIT 1");
        UserSession session = userSessionMapper.selectOne(queryWrapper);
        if (session == null) {
            return null;
        }
        return new AccessTokenPayload(session.getTenantId(), session.getUserId(), token, true);
    }

    private AccessTokenPayload resolveDemoAccessToken(String token) {
        String[] parts = token.substring(DEMO_TOKEN_PREFIX.length()).split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            return new AccessTokenPayload(Long.parseLong(parts[0]), Long.parseLong(parts[1]), token, false);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record AccessTokenPayload(Long tenantId, Long userId, String accessToken, boolean sessionToken) {
    }
}
