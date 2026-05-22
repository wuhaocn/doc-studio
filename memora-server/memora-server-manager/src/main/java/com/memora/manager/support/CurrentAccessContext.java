package com.memora.manager.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.memora.common.exception.BusinessException;
import com.memora.manager.entity.UserSession;
import com.memora.manager.mapper.UserSessionMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
    private final OpaqueTokenCodec opaqueTokenCodec;
    private final BrowserSessionSupport browserSessionSupport;

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

    public Long getCurrentSessionIdOrNull() {
        AccessTokenPayload payload = resolveAccessTokenPayload();
        return payload == null ? null : payload.sessionId();
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

        String token = resolveBearerToken(request);
        if (!StringUtils.hasText(token)) {
            token = browserSessionSupport.resolveSessionCookie(request);
        }
        if (!StringUtils.hasText(token)) {
            return null;
        }

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

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private AccessTokenPayload resolveSessionAccessToken(String token) {
        String hashedToken = opaqueTokenCodec.hash(token);
        UserSession session = findActiveSessionByStoredToken(hashedToken);
        if (session == null) {
            session = findActiveSessionByStoredToken(token);
            if (session != null) {
                session.setAccessToken(hashedToken);
                userSessionMapper.updateById(session);
            }
        }
        if (session == null) {
            return null;
        }
        return new AccessTokenPayload(session.getTenantId(), session.getUserId(), session.getId(), token, true);
    }

    private UserSession findActiveSessionByStoredToken(String storedToken) {
        LambdaQueryWrapper<UserSession> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserSession::getAccessToken, storedToken)
            .eq(UserSession::getStatus, 1)
            .gt(UserSession::getExpiresAt, LocalDateTime.now())
            .last("LIMIT 1");
        return userSessionMapper.selectOne(queryWrapper);
    }

    private AccessTokenPayload resolveDemoAccessToken(String token) {
        String[] parts = token.substring(DEMO_TOKEN_PREFIX.length()).split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            return new AccessTokenPayload(Long.parseLong(parts[0]), Long.parseLong(parts[1]), null, token, false);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record AccessTokenPayload(Long tenantId, Long userId, Long sessionId, String accessToken, boolean sessionToken) {
    }
}
